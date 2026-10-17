package com.example.travel.dto;

import jakarta.validation.constraints.NotBlank;

public record DiaryRequest(
        @NotBlank String title,
        @NotBlank String destination,
        String authorName,
        @NotBlank String content,
        String imageUrl,
        String videoUrl,
        String interestTags
) {
}
