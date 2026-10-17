package com.example.travel.model;

import java.util.Map;

public record MapStats(
        int areaCount,
        int nodeCount,
        int edgeCount,
        int buildingCount,
        int facilityCount,
        int facilityTypeCount,
        Map<String, Integer> facilityTypes
) {
}
