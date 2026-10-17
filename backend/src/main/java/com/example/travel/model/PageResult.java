package com.example.travel.model;

import java.util.List;

/**
 * 统一分页结果。
 */
public record PageResult<T>(
        List<T> items,
        long total,
        int page,
        int pageSize,
        int totalPages
) {
    public static <T> PageResult<T> of(List<T> items, long total, int page, int pageSize) {
        int safePageSize = Math.max(1, pageSize);
        int totalPages = (int) Math.ceil(total / (double) safePageSize);
        return new PageResult<>(items, total, page, safePageSize, totalPages);
    }
}
