package com.example.travel.service;

import com.example.travel.repository.DiaryRepository;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * 浏览量计数服务：写路径 Redis INCR（内存操作），由定时任务批量落库，
 * 消除热点日记行上的行锁竞争。
 * <p>对账设计：Redis 只保存“距上次落库的增量”——
 * <pre>
 *   voyage:diary:view:pending:{id}  增量计数器
 *   voyage:diary:view:dirty         有待落库增量的 diaryId 集合
 * </pre>
 * DB 的 view_count 是基线，定时任务用 Lua 原子地“取走并清零”增量后执行
 * UPDATE view_count = view_count + ?；DB 写失败会把增量退回 Redis，不丢计数。
 * <p>降级：Redis 不可用时（本地开发 / 宕机）自动切换为进程内 LongAdder 缓冲，
 * 由同一个定时任务落库；应用启动与运行均不依赖 Redis 可用性。
 */
@Service
public class ViewCountService {
    private static final Logger log = LoggerFactory.getLogger(ViewCountService.class);

    private static final String PENDING_KEY_PREFIX = "voyage:diary:view:pending:";
    private static final String DIRTY_SET_KEY = "voyage:diary:view:dirty";
    private static final long REDIS_RETRY_INTERVAL_MS = 30_000;

    /**
     * 原子地取出增量并归零，保证“取数”和“清零”之间不会漏计并发的新浏览。
     */
    private static final DefaultRedisScript<Long> DRAIN_SCRIPT = new DefaultRedisScript<>("""
            local delta = redis.call('GET', KEYS[1])
            if delta and tonumber(delta) > 0 then
                redis.call('SET', KEYS[1], '0')
                return tonumber(delta)
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final DiaryRepository diaryRepository;
    private final MeterRegistry meterRegistry;

    private final ConcurrentHashMap<Long, LongAdder> localBuffer = new ConcurrentHashMap<>();
    private final AtomicBoolean redisUp = new AtomicBoolean(false);
    private final AtomicLong lastRedisAttempt = new AtomicLong(0);

    public ViewCountService(
            StringRedisTemplate redisTemplate,
            DiaryRepository diaryRepository,
            ObjectProvider<MeterRegistry> meterRegistryProvider
    ) {
        this.redisTemplate = redisTemplate;
        this.diaryRepository = diaryRepository;
        this.meterRegistry = meterRegistryProvider.getIfAvailable();
    }

    /**
     * 记录一次浏览。
     */
    public void increment(long diaryId) {
        if (redisAvailable()) {
            try {
                redisTemplate.opsForValue().increment(PENDING_KEY_PREFIX + diaryId);
                redisTemplate.opsForSet().add(DIRTY_SET_KEY, String.valueOf(diaryId));
                return;
            } catch (RuntimeException error) {
                markRedisDown(error);
            }
        }
        localBuffer.computeIfAbsent(diaryId, ignored -> new LongAdder()).increment();
    }

    /**
     * 当前尚未落库的浏览增量（用于详情页实时展示 DB 基线 + 增量）。
     */
    public long pending(long diaryId) {
        long local = localBuffer.getOrDefault(diaryId, new LongAdder()).sum();
        long redisPending = 0;
        if (redisAvailable()) {
            try {
                String value = redisTemplate.opsForValue().get(PENDING_KEY_PREFIX + diaryId);
                redisPending = value == null ? 0 : Long.parseLong(value);
            } catch (RuntimeException error) {
                markRedisDown(error);
            }
        }
        return local + redisPending;
    }

    /**
     * 定时批量落库：Redis 增量 + 本地降级增量。
     */
    @Scheduled(fixedDelayString = "${voyage.view-count.flush-interval-ms:15000}")
    public void flushAll() {
        flushLocalBuffer();
        if (redisAvailable()) {
            try {
                flushRedisDelta();
            } catch (RuntimeException error) {
                markRedisDown(error);
                flushLocalBuffer();
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Flushing view counters before shutdown...");
        flushAll();
    }

    private void flushRedisDelta() {
        Set<String> dirtyIds = redisTemplate.opsForSet().members(DIRTY_SET_KEY);
        if (dirtyIds == null || dirtyIds.isEmpty()) {
            return;
        }
        int flushed = 0;
        for (String rawId : dirtyIds) {
            long diaryId;
            try {
                diaryId = Long.parseLong(rawId);
            } catch (NumberFormatException error) {
                redisTemplate.opsForSet().remove(DIRTY_SET_KEY, rawId);
                continue;
            }
            String key = PENDING_KEY_PREFIX + diaryId;
            Long drained = redisTemplate.execute(DRAIN_SCRIPT, List.of(key));
            long delta = drained == null ? 0 : drained;
            if (delta <= 0) {
                redisTemplate.opsForSet().remove(DIRTY_SET_KEY, rawId);
                continue;
            }
            try {
                int rows = diaryRepository.addViewDelta(diaryId, delta);
                if (rows > 0) {
                    redisTemplate.opsForSet().remove(DIRTY_SET_KEY, rawId);
                    flushed += delta;
                } else {
                    // 日记已不存在：丢弃脏标记与增量
                    redisTemplate.delete(key);
                    redisTemplate.opsForSet().remove(DIRTY_SET_KEY, rawId);
                }
            } catch (DataAccessException error) {
                // DB 写失败：把增量退回 Redis，等待下个周期重试，不丢计数
                redisTemplate.opsForValue().increment(key, delta);
                log.warn("Flush view delta failed for diary {}, delta returned to redis: {}", diaryId, error.getMessage());
            }
        }
        if (flushed > 0) {
            log.info("Flushed {} view counts from redis to database", flushed);
            recordFlushed("redis", flushed);
        }
    }

    private void flushLocalBuffer() {
        if (localBuffer.isEmpty()) {
            return;
        }
        long flushed = 0;
        for (var entry : localBuffer.entrySet()) {
            long delta = entry.getValue().sumThenReset();
            if (delta <= 0) {
                continue;
            }
            try {
                if (diaryRepository.addViewDelta(entry.getKey(), delta) > 0) {
                    flushed += delta;
                }
            } catch (DataAccessException error) {
                // 落库失败：把增量加回本地缓冲，下个周期重试
                entry.getValue().add(delta);
                log.warn("Flush local view delta failed for diary {}: {}", entry.getKey(), error.getMessage());
            }
        }
        if (flushed > 0) {
            log.info("Flushed {} view counts from local buffer to database", flushed);
            recordFlushed("local", flushed);
        }
    }

    /**
     * 是否允许尝试 Redis：标记不可用后，每隔一个重试窗口再探活，避免每个请求都被超时拖慢。
     */
    private boolean redisAvailable() {
        if (redisUp.get()) {
            return true;
        }
        long now = System.currentTimeMillis();
        long last = lastRedisAttempt.get();
        return now - last > REDIS_RETRY_INTERVAL_MS && lastRedisAttempt.compareAndSet(last, now);
    }

    private void markRedisDown(RuntimeException error) {
        if (redisUp.compareAndSet(true, false)) {
            log.warn("Redis unavailable, falling back to in-process view counter: {}", error.getMessage());
        }
        lastRedisAttempt.set(System.currentTimeMillis());
    }

    private void recordFlushed(String source, long count) {
        if (meterRegistry != null) {
            meterRegistry.counter("voyage_view_flush_total", "source", source).increment(count);
        }
    }
}
