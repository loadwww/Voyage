package com.example.travel.algorithm;

import com.example.travel.model.ScoredSpot;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TspDpPlannerTest {
    private final TspDpPlanner tsp = new TspDpPlanner();

    @Test
    void ordersSpotsAlongCheapestChain() {
        List<ScoredSpot> spots = List.of(
                new ScoredSpot(SpotFixtures.spot(1), 0.9, "r1"),
                new ScoredSpot(SpotFixtures.spot(2), 0.8, "r2"),
                new ScoredSpot(SpotFixtures.spot(3), 0.7, "r3")
        );
        // 1 <-> 3 很远，2 在中间，最优链为 1 -> 2 -> 3（或反向）
        int[][] dist = {
                {0, 1, 100},
                {1, 0, 1},
                {100, 1, 0}
        };

        List<ScoredSpot> ordered = tsp.order(spots, dist);

        assertEquals(3, ordered.size());
        assertEquals(2, ordered.get(1).spot().id(), "中间点必须是距离最短链上的景点 2");
        assertTrue(ordered.get(0).spot().id() == 1 && ordered.get(2).spot().id() == 3
                || ordered.get(0).spot().id() == 3 && ordered.get(2).spot().id() == 1);
    }

    @Test
    void travelMinutesMatchesOrderedEdges() {
        List<ScoredSpot> spots = List.of(
                new ScoredSpot(SpotFixtures.spot(1), 0.9, "r1"),
                new ScoredSpot(SpotFixtures.spot(2), 0.8, "r2"),
                new ScoredSpot(SpotFixtures.spot(3), 0.7, "r3")
        );
        int[][] dist = {
                {0, 5, 20},
                {5, 0, 7},
                {20, 7, 0}
        };

        List<ScoredSpot> ordered = tsp.order(spots, dist);

        // 首景点固定 20 分钟到店开销，之后按相邻矩阵累加
        int expected = 20 + dist[ordered.get(0).spot().id() - 1][ordered.get(1).spot().id() - 1]
                + dist[ordered.get(1).spot().id() - 1][ordered.get(2).spot().id() - 1];
        assertEquals(expected, tsp.travelMinutes(ordered, dist));
    }

    @Test
    void returnsInputDirectlyForSingleSpot() {
        List<ScoredSpot> spots = List.of(new ScoredSpot(SpotFixtures.spot(1), 0.9, "r1"));
        int[][] dist = {{0}};

        List<ScoredSpot> ordered = tsp.order(spots, dist);

        assertEquals(1, ordered.size());
        assertEquals(1, ordered.get(0).spot().id());
    }
}
