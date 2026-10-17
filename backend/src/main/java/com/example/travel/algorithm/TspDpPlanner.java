package com.example.travel.algorithm;

import com.example.travel.model.ScoredSpot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TspDpPlanner {
    public List<ScoredSpot> order(List<ScoredSpot> spots, int[][] dist) {
        if (spots.size() <= 1) {
            return spots;
        }
        int n = spots.size();
        int stateCount = 1 << n;
        int[][] dp = new int[stateCount][n];
        int[][] prev = new int[stateCount][n];
        for (int mask = 0; mask < stateCount; mask++) {
            for (int i = 0; i < n; i++) {
                dp[mask][i] = Integer.MAX_VALUE / 4;
                prev[mask][i] = -1;
            }
        }
        for (int i = 0; i < n; i++) {
            dp[1 << i][i] = 20;
        }
        for (int mask = 0; mask < stateCount; mask++) {
            for (int last = 0; last < n; last++) {
                if ((mask & (1 << last)) == 0) {
                    continue;
                }
                for (int next = 0; next < n; next++) {
                    if ((mask & (1 << next)) != 0) {
                        continue;
                    }
                    int nextMask = mask | (1 << next);
                    int travel = travelBetween(spots.get(last), spots.get(next), dist);
                    int candidate = dp[mask][last] + travel;
                    if (candidate < dp[nextMask][next]) {
                        dp[nextMask][next] = candidate;
                        prev[nextMask][next] = last;
                    }
                }
            }
        }
        int fullMask = stateCount - 1;
        int bestLast = 0;
        for (int i = 1; i < n; i++) {
            if (dp[fullMask][i] < dp[fullMask][bestLast]) {
                bestLast = i;
            }
        }
        List<ScoredSpot> ordered = new ArrayList<>();
        int mask = fullMask;
        int current = bestLast;
        while (current >= 0) {
            ordered.add(spots.get(current));
            int previous = prev[mask][current];
            mask ^= 1 << current;
            current = previous;
        }
        Collections.reverse(ordered);
        return ordered;
    }

    public int travelMinutes(List<ScoredSpot> ordered, int[][] dist) {
        int total = 0;
        for (int i = 0; i < ordered.size(); i++) {
            if (i == 0) {
                total += 20;
            } else {
                total += travelBetween(ordered.get(i - 1), ordered.get(i), dist);
            }
        }
        return total;
    }

    private int travelBetween(ScoredSpot from, ScoredSpot to, int[][] dist) {
        return dist[from.spot().id() - 1][to.spot().id() - 1];
    }
}
