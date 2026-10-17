package com.example.travel.algorithm;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 评分并发问题的可复现验证（对应 P0-1）。
 * <p>旧实现是“Java 端读旧值 -> 计算 -> 整体写回”的读-改-写；
 * 新实现是一条原子 UPDATE（等价于在行锁上做 CAS）。
 * 这里在内存中分别模拟两种策略，证明：旧策略丢更新，CAS 策略不丢。
 */
class AtomicRatingConcurrencyTest {
    private static final int THREADS = 32;

    @Test
    void readModifyWriteLosesConcurrentRatings() throws Exception {
        class Row {
            double rating = 0.0;
            int count = 0;
        }
        Row row = new Row();
        CyclicBarrier allRead = new CyclicBarrier(THREADS);
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch done = new CountDownLatch(THREADS);

        try {
            for (int i = 0; i < THREADS; i++) {
                pool.submit(() -> {
                    try {
                        double oldRating = row.rating;
                        int oldCount = row.count;
                        // 所有线程都读到初始快照后再写回，复现并发交错
                        allRead.await();
                        row.rating = round2((oldRating * oldCount + 5.0) / Math.max(1, oldCount + 1));
                        row.count = oldCount + 1;
                    } catch (Exception ignored) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }
            assertTrue(done.await(10, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }

        // 32 个并发请求全部基于 count=0 的快照写回，最终 count 只剩 1
        assertEquals(1, row.count, "读-改-写必然丢更新");
    }

    @Test
    void casUpdateKeepsEveryConcurrentRating() throws Exception {
        record Rating(double rating, int count) {
        }
        AtomicReference<Rating> row = new AtomicReference<>(new Rating(0.0, 0));
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(THREADS);

        try {
            for (int i = 0; i < THREADS; i++) {
                Future<?> future = pool.submit(() -> {
                    try {
                        start.await();
                        while (true) {
                            // 模拟原子 SQL：SET rating = (rating*count + ?)/(count+1), count = count+1
                            Rating current = row.get();
                            Rating next = new Rating(
                                    round2((current.rating() * current.count() + 5.0) / (current.count() + 1)),
                                    current.count() + 1
                            );
                            if (row.compareAndSet(current, next)) {
                                break;
                            }
                        }
                    } catch (InterruptedException error) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            assertTrue(done.await(10, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }

        Rating finalRow = row.get();
        assertEquals(THREADS, finalRow.count(), "CAS 原子更新一次都不能丢");
        assertEquals(5.0, finalRow.rating(), 0.001, "全部评 5 分，平均分收敛到 5.0");
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
