package com.example.travel.algorithm;

import com.example.travel.model.ScoredSpot;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class TopKPriorityQueue {
    public List<ScoredSpot> select(List<ScoredSpot> candidates, int k) {
        PriorityQueue<ScoredSpot> queue = new PriorityQueue<>(Comparator.comparingDouble(ScoredSpot::score));
        for (ScoredSpot candidate : candidates) {
            queue.offer(candidate);
            if (queue.size() > k) {
                queue.poll();
            }
        }
        return queue.stream()
                .sorted(Comparator.comparingDouble(ScoredSpot::score).reversed()
                        .thenComparing(item -> item.spot().popularity(), Comparator.reverseOrder()))
                .toList();
    }
}
