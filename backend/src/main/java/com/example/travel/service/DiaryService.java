package com.example.travel.service;

import com.example.travel.dto.DiaryRequest;
import com.example.travel.exception.NotFoundException;
import com.example.travel.model.TravelDiary;
import com.example.travel.repository.DiaryRepository;
import com.example.travel.repository.IdempotencyRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

@Service
public class DiaryService {
    private final DiaryRepository diaryRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final ViewCountService viewCountService;

    public DiaryService(
            DiaryRepository diaryRepository,
            IdempotencyRepository idempotencyRepository,
            ViewCountService viewCountService
    ) {
        this.diaryRepository = diaryRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.viewCountService = viewCountService;
    }

    /**
     * 创建日记（事务 + 幂等）。
     * 携带 Idempotency-Key 时，同一键的重试返回首次创建结果，不会产生重复日记。
     */
    @Transactional
    public TravelDiary create(DiaryRequest request, String idempotencyKey) {
        String key = normalizeKey(idempotencyKey);
        if (key != null) {
            Long existing = idempotencyRepository.findReference(key);
            if (existing != null) {
                return diaryRepository.findById(existing);
            }
        }

        long id = diaryRepository.create(request, compress(request.content()));

        if (key != null) {
            try {
                idempotencyRepository.save(key, id);
            } catch (DuplicateKeyException duplicate) {
                // 并发请求抢先登记了同一个幂等键：返回对方的结果，保证只有一条日记
                Long winner = idempotencyRepository.findReference(key);
                if (winner != null) {
                    return diaryRepository.findById(winner);
                }
                throw duplicate;
            }
        }
        return diaryRepository.findById(id);
    }

    public TravelDiary detail(long id) {
        // 浏览量先进 Redis/本地缓冲，定时批量落库，不直接打热点行
        viewCountService.increment(id);
        TravelDiary diary = diaryRepository.findById(id);
        long pending = viewCountService.pending(id);
        return pending > 0 ? withViewCount(diary, diary.viewCount() + (int) pending) : diary;
    }

    public List<TravelDiary> recommend(String interest, String destination, int limit) {
        Set<String> interests = splitTags(interest);
        return diaryRepository.list(destination, null, "hot", 100).stream()
                .sorted(Comparator.comparingDouble((TravelDiary diary) -> recommendScore(diary, interests)).reversed())
                .limit(Math.max(1, Math.min(limit <= 0 ? 10 : limit, 30)))
                .toList();
    }

    /**
     * 原子评分：整条 UPDATE 在数据库行锁内完成，事务保证异常时不留中间状态。
     */
    @Transactional
    public void rate(long id, double rating) {
        double normalized = Math.max(1, Math.min(5, rating));
        int updated = diaryRepository.rate(id, normalized);
        if (updated == 0) {
            throw new NotFoundException("travel_diary", id);
        }
    }

    private TravelDiary withViewCount(TravelDiary diary, int viewCount) {
        return new TravelDiary(
                diary.id(),
                diary.title(),
                diary.destination(),
                diary.authorName(),
                diary.content(),
                diary.imageUrl(),
                diary.videoUrl(),
                diary.interestTags(),
                viewCount,
                diary.rating(),
                diary.ratingCount(),
                diary.compressedContent(),
                diary.createTime()
        );
    }

    private String normalizeKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }
        String key = idempotencyKey.trim();
        if (key.length() > 80) {
            throw new IllegalArgumentException("Idempotency-Key 长度不能超过 80");
        }
        return key;
    }

    private double recommendScore(TravelDiary diary, Set<String> interests) {
        long match = splitTags(diary.interestTags()).stream().filter(interests::contains).count();
        double interestScore = interests.isEmpty() ? 0.3 : match / Math.max(1.0, interests.size());
        return interestScore * 0.45 + Math.min(1, diary.viewCount() / 150.0) * 0.30 + (diary.rating() / 5.0) * 0.25;
    }

    private Set<String> splitTags(String raw) {
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(raw.split("[,，、\\s]+"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .collect(java.util.stream.Collectors.toSet());
    }

    private String compress(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(bytes)) {
                gzip.write(content.getBytes(StandardCharsets.UTF_8));
            }
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch (IOException error) {
            return "";
        }
    }
}
