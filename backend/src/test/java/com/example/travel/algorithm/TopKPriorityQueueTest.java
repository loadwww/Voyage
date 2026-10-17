package com.example.travel.algorithm;

import com.example.travel.model.ScoredSpot;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TopKPriorityQueueTest {
    private final TopKPriorityQueue topK = new TopKPriorityQueue();

    @Test
    void keepsHighestKScoresSortedDesc() {
        List<ScoredSpot> candidates = List.of(
                scored(1, 0.2, 10),
                scored(2, 0.9, 80),
                scored(3, 0.5, 40),
                scored(4, 0.95, 90),
                scored(5, 0.7, 70)
        );

        List<ScoredSpot> result = topK.select(candidates, 3);

        assertEquals(List.of(4, 2, 5), result.stream().map(item -> item.spot().id()).toList());
    }

    @Test
    void tieBreaksByPopularityDesc() {
        List<ScoredSpot> candidates = List.of(
                scored(1, 0.8, 30),
                scored(2, 0.8, 90),
                scored(3, 0.8, 60)
        );

        List<ScoredSpot> result = topK.select(candidates, 2);

        assertEquals(List.of(2, 3), result.stream().map(item -> item.spot().id()).toList());
    }

    @Test
    void kLargerThanCandidatesReturnsAll() {
        List<ScoredSpot> candidates = List.of(
                scored(1, 0.3, 10),
                scored(2, 0.6, 20)
        );

        List<ScoredSpot> result = topK.select(candidates, 5);

        assertEquals(2, result.size());
        assertTrue(result.get(0).score() >= result.get(1).score());
    }

    private ScoredSpot scored(int id, double score, int popularity) {
        return new ScoredSpot(SpotFixtures.spot(id, popularity), score, "reason-" + id);
    }
}
