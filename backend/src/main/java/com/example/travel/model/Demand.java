package com.example.travel.model;

import java.util.List;

public record Demand(
        String rawText,
        String destination,
        int days,
        int budget,
        List<String> interests,
        PaceType pace,
        String constraints,
        String walkingTolerance,
        boolean needNoonRest,
        String crowdSensitivity,
        String comfortPreference
) {
    public Demand normalize() {
        String normalizedDestination = destination == null || destination.isBlank() ? "成都" : destination.trim();
        int normalizedDays = Math.max(1, Math.min(days <= 0 ? 3 : days, 7));
        int normalizedBudget = Math.max(300, budget <= 0 ? 1600 : budget);
        List<String> normalizedInterests = interests == null || interests.isEmpty() ? List.of("历史") : interests;
        return new Demand(
                rawText == null ? "" : rawText.trim(),
                normalizedDestination,
                normalizedDays,
                normalizedBudget,
                normalizedInterests,
                pace == null ? PaceType.balanced : pace,
                constraints == null ? "" : constraints.trim(),
                normalizeLevel(walkingTolerance, "medium"),
                needNoonRest,
                normalizeLevel(crowdSensitivity, "medium"),
                comfortPreference == null || comfortPreference.isBlank() ? "comfort" : comfortPreference.trim()
        );
    }

    private String normalizeLevel(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.trim().toLowerCase();
        return switch (normalized) {
            case "low", "medium", "high" -> normalized;
            default -> fallback;
        };
    }
}
