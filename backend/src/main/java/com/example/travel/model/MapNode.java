package com.example.travel.model;

public record MapNode(
        long id,
        long areaId,
        String name,
        String nodeType,
        String category,
        double longitude,
        double latitude,
        int floorCount,
        String openTime,
        String closeTime,
        String serviceTags,
        String description
) {
}
