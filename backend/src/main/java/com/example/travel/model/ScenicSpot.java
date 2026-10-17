package com.example.travel.model;

import java.util.List;

public record ScenicSpot(
        int id,
        String name,
        String city,
        String district,
        String address,
        double longitude,
        double latitude,
        String category,
        int ticketPrice,
        double stayDuration,
        String openTime,
        String closeTime,
        int popularity,
        List<String> tags,
        String description,
        String image,
        boolean isRecoveryNode,
        double physicalLoad,
        double cognitiveLoad,
        double crowdLoad,
        double queueLoad,
        double recoveryValue
) {
}
