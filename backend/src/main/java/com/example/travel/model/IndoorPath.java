package com.example.travel.model;

import java.util.List;

public record IndoorPath(
        long areaId,
        long buildingNodeId,
        String buildingName,
        String fromRoom,
        String toRoom,
        int totalTime,
        List<String> steps
) {
}
