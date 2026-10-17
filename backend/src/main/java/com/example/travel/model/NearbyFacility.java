package com.example.travel.model;

public record NearbyFacility(
        MapNode node,
        double roadDistance,
        int travelTime,
        String category,
        String reason
) {
}
