package com.example.travel.model;

public record AreaRecommendation(
        MapArea area,
        String typeLabel,
        int popularity,
        double rating,
        int matchScore,
        String reason
) {
}
