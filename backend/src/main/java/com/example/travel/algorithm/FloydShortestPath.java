package com.example.travel.algorithm;

public class FloydShortestPath {
    public int[][] shortestDistances(int[][] matrix) {
        int[][] dist = new int[matrix.length][];
        for (int i = 0; i < matrix.length; i++) {
            dist[i] = matrix[i].clone();
        }
        for (int k = 0; k < dist.length; k++) {
            for (int i = 0; i < dist.length; i++) {
                for (int j = 0; j < dist.length; j++) {
                    if (dist[i][j] > dist[i][k] + dist[k][j]) {
                        dist[i][j] = dist[i][k] + dist[k][j];
                    }
                }
            }
        }
        return dist;
    }
}
