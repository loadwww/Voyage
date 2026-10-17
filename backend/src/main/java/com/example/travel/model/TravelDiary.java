package com.example.travel.model;

import java.time.LocalDateTime;

public record TravelDiary(
        long id,
        String title,
        String destination,
        String authorName,
        String content,
        String imageUrl,
        String videoUrl,
        String interestTags,
        int viewCount,
        double rating,
        int ratingCount,
        String compressedContent,
        LocalDateTime createTime
) {
}
