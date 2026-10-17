package com.example.travel.model;

public record MapPathStep(
        long fromNodeId,
        long toNodeId,
        String fromName,
        String toName,
        String roadType,
        String transportMode,
        double distance,
        int travelTime,
        double congestion,
        String note
) {
}
