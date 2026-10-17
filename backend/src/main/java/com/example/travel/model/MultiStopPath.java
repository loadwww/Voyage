package com.example.travel.model;

import java.util.List;

public record MultiStopPath(
        long areaId,
        long startNodeId,
        List<Long> targetNodeIds,
        String strategy,
        List<Long> visitOrder,
        double totalDistance,
        int totalTime,
        List<MapNode> nodes,
        List<MapEdge> edges,
        List<MapPathStep> steps
) {
}
