package com.example.travel.model;

public enum RhythmState {
    RELAXED("轻松", "relaxed"),
    STABLE("平稳", "stable"),
    FATIGUED("疲劳", "fatigued"),
    OVERLOADED("过载", "overloaded");

    private final String label;
    private final String level;

    RhythmState(String label, String level) {
        this.label = label;
        this.level = level;
    }

    public String getLabel() {
        return label;
    }

    public String getLevel() {
        return level;
    }
}
