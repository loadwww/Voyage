package com.example.travel.model;

public record MapArea(
        long id,
        String name,
        String areaType,
        String city,
        String district,
        String address,
        double longitude,
        double latitude,
        String description
) {
}
