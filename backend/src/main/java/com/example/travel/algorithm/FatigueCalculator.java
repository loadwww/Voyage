package com.example.travel.algorithm;

import com.example.travel.model.FatigueLevel;
import com.example.travel.model.PaceType;

public class FatigueCalculator {
    public FatigueLevel calculate(double riskScore, PaceType pace) {
        double score = riskScore * pace.getFatigueFactor();
        if (score < 38) {
            return new FatigueLevel("轻松", "low", round(score));
        }
        if (score < 68) {
            return new FatigueLevel("平稳", "medium", round(score));
        }
        if (score < 88) {
            return new FatigueLevel("疲劳", "high", round(score));
        }
        return new FatigueLevel("过载", "critical", round(score));
    }

    private double round(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
