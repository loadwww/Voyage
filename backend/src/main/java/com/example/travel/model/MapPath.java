package com.example.travel.model;

import java.util.List;

public record MapPath(
        long areaId,
        long fromNodeId,
        long toNodeId,
        String strategy,
        double totalDistance,
        int totalTime,
        List<MapNode> nodes,
        List<MapEdge> edges,
        List<MapPathStep> steps
) {
}
