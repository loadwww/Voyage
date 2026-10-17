package com.example.travel.algorithm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FloydShortestPathTest {
    private static final int INF = 1_000_000;

    private final FloydShortestPath floyd = new FloydShortestPath();

    @Test
    void findsShorterIndirectPath() {
        int[][] matrix = {
                {0, 5, INF},
                {INF, 0, 3},
                {INF, INF, 0}
        };

        int[][] dist = floyd.shortestDistances(matrix);

        // 0 -> 2 无直连，绕行 0 -> 1 -> 2 = 8
        assertEquals(8, dist[0][2]);
        assertEquals(5, dist[0][1]);
        assertEquals(3, dist[1][2]);
    }

    @Test
    void picksMinimumAcrossMultipleHops() {
        int[][] matrix = {
                {0, 1, INF, 10},
                {INF, 0, 1, INF},
                {INF, INF, 0, 1},
                {INF, INF, INF, 0}
        };

        int[][] dist = floyd.shortestDistances(matrix);

        // 0 -> 3 直达 10；路径 0 -> 1 -> 2 -> 3 = 3
        assertEquals(3, dist[0][3]);
        assertEquals(2, dist[0][2]);
    }

    @Test
    void diagonalStaysZeroAndDirectEdgeWins() {
        int[][] matrix = {
                {0, 2, 9},
                {2, 0, 4},
                {9, 4, 0}
        };

        int[][] dist = floyd.shortestDistances(matrix);

        for (int i = 0; i < 3; i++) {
            assertEquals(0, dist[i][i]);
        }
        // 0 -> 2 直达 9，绕行 0 -> 1 -> 2 = 6，取更小值
        assertEquals(6, dist[0][2]);
        assertEquals(2, dist[0][1]);
    }

    @Test
    void doesNotMutateInputMatrix() {
        int[][] matrix = {
                {0, 100, INF},
                {INF, 0, 1},
                {INF, INF, 0}
        };

        floyd.shortestDistances(matrix);

        // 输入矩阵保持原样（PlanService 复用共享缓存矩阵的前提）
        assertEquals(INF, matrix[0][2]);
    }
}
