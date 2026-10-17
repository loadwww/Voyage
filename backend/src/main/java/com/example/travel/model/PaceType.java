package com.example.travel.model;

public enum PaceType {
    relaxed("轻松慢游", 6.0, 2, 0.78),
    balanced("舒适均衡", 8.0, 3, 1.0),
    compact("紧凑多玩", 9.5, 4, 1.18);

    private final String label;
    private final double maxHours;
    private final int maxSpots;
    private final double fatigueFactor;

    PaceType(String label, double maxHours, int maxSpots, double fatigueFactor) {
        this.label = label;
        this.maxHours = maxHours;
        this.maxSpots = maxSpots;
        this.fatigueFactor = fatigueFactor;
    }

    public String getLabel() {
        return label;
    }

    public double getMaxHours() {
        return maxHours;
    }

    public int getMaxSpots() {
        return maxSpots;
    }

    public double getFatigueFactor() {
        return fatigueFactor;
    }

    public static PaceType from(String value) {
        if (value == null || value.isBlank()) {
            return balanced;
        }
        String normalized = value.trim();
        for (PaceType paceType : values()) {
            if (paceType.name().equalsIgnoreCase(normalized) || paceType.label.equals(normalized)) {
                return paceType;
            }
        }
        return balanced;
    }
}
