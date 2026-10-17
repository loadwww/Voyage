package com.example.travel.model;

public record ItineraryNode(
        ScoredSpot item,
        String arriveTime,
        String leaveTime,
        RhythmState stateBefore,
        RhythmState stateAfter,
        double fatigueRisk,
        boolean isRecoveryNode,
        String reason,
        int suggestedStayMinutes
) {
}
