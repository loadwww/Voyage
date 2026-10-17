package com.example.travel.model;

import java.util.List;

public record DayPlan(
        int dayNo,
        List<ScoredSpot> spots,
        List<ItineraryNode> nodes,
        int travelMinutes,
        double totalHours,
        FatigueLevel fatigue,
        RhythmState rhythmState,
        int fatigueRisk,
        String routeReason,
        List<String> interventions
) {
}
