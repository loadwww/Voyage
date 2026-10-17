package com.example.travel.model;

import java.util.List;

public record TravelPlan(
        String id,
        Demand demand,
        List<ScoredSpot> selected,
        List<DayPlan> days,
        int totalCost,
        double totalHours,
        int match,
        String summary,
        List<String> explanations,
        String parentPlanId,
        int versionNo
) {
}
