package com.example.travel.algorithm;

import com.example.travel.model.FatigueLevel;
import com.example.travel.model.PaceType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FatigueCalculatorTest {
    private final FatigueCalculator calculator = new FatigueCalculator();

    @Test
    void classifiesBalancedPaceBoundaries() {
        // balanced 节奏因子 = 1.0，阈值 38 / 68 / 88
        assertEquals("low", calculator.calculate(37, PaceType.balanced).level());
        assertEquals("medium", calculator.calculate(38, PaceType.balanced).level());
        assertEquals("medium", calculator.calculate(67, PaceType.balanced).level());
        assertEquals("high", calculator.calculate(68, PaceType.balanced).level());
        assertEquals("high", calculator.calculate(87, PaceType.balanced).level());
        assertEquals("critical", calculator.calculate(88, PaceType.balanced).level());
    }

    @Test
    void relaxedPaceReducesFatigueByFactor() {
        // relaxed 因子 0.78：50 * 0.78 = 39 -> medium
        FatigueLevel level = calculator.calculate(50, PaceType.relaxed);
        assertEquals("medium", level.level());
        assertEquals(39.0, level.score(), 0.001);
    }

    @Test
    void compactPaceAmplifiesFatigue() {
        // compact 因子 1.18：60 * 1.18 = 70.8 -> high
        FatigueLevel level = calculator.calculate(60, PaceType.compact);
        assertEquals("high", level.level());
        assertEquals(70.8, level.score(), 0.001);
    }
}
