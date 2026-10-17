package com.example.travel.model;

public record MapEdge(
        long id,
        long areaId,
        long fromNodeId,
        long toNodeId,
        double distance,
        int travelTime,
        String roadType,
        boolean bidirectional
) {
}
