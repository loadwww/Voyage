package com.example.travel.model;

public record FoodRecommendation(
        MapNode node,
        String cuisine,
        String restaurantName,
        String windowName,
        String signatureDish,
        int popularity,
        double rating,
        double roadDistance,
        int travelTime,
        int matchScore,
        String sortReason
) {
}
