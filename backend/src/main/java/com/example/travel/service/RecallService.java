package com.example.travel.service;

import com.example.travel.algorithm.TopKPriorityQueue;
import com.example.travel.model.Demand;
import com.example.travel.model.ScoredSpot;
import com.example.travel.model.ScenicSpot;
import com.example.travel.repository.SpotRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RecallService {
    private final SpotRepository spotRepository;
    private final TopKPriorityQueue topK = new TopKPriorityQueue();

    public RecallService(SpotRepository spotRepository) {
        this.spotRepository = spotRepository;
    }

    public List<ScoredSpot> recall(Demand demand, Integer mustSpotId, Integer removeSpotId) {
        List<ScoredSpot> scored = spotRepository.recallByTags(demand.destination(), demand.interests()).stream()
                .filter(spot -> removeSpotId == null || spot.id() != removeSpotId)
                .map(spot -> score(spot, demand, mustSpotId))
                .toList();

        int targetCount = budgetAwareTargetCount(demand);
        int count = Math.min(scored.size(), targetCount);
        List<ScoredSpot> selected = new ArrayList<>(topK.select(scored, Math.max(1, count)));

        if (mustSpotId != null && selected.stream().noneMatch(item -> item.spot().id() == mustSpotId)) {
            spotRepository.findById(mustSpotId)
                    .filter(spot -> spot.city().equals(demand.destination()))
                    .filter(spot -> !spot.isRecoveryNode())
                    .map(spot -> score(spot, demand, mustSpotId))
                    .ifPresent(selected::add);
        }
        return selected;
    }

    private ScoredSpot score(ScenicSpot spot, Demand demand, Integer mustSpotId) {
        long matchCount = demand.interests().stream().filter(spot.tags()::contains).count();
        double interestScore = matchCount / Math.max(demand.interests().size(), 1.0);
        double popularityScore = spot.popularity() / 100.0;
        double budgetScore = budgetScore(spot, demand);
        double loadPenalty = loadPenalty(spot, demand);
        double mustBoost = mustSpotId != null && spot.id() == mustSpotId ? 0.3 : 0.0;
        double score = Math.min(0.99, interestScore * 0.50 + popularityScore * 0.20 + budgetScore * 0.24 - loadPenalty + mustBoost);
        String reason = explainSpot(spot, demand, matchCount, budgetScore, loadPenalty, mustBoost);
        return new ScoredSpot(spot, round(Math.max(0.18, score)), reason);
    }

    private int budgetAwareTargetCount(Demand demand) {
        int base = Math.max(demand.days() * 2, demand.days() * demand.pace().getMaxSpots() - 1);
        double dayBudget = demand.budget() / Math.max(demand.days(), 1.0);
        if (demand.budget() < 500) {
            return 1;
        }
        if (demand.budget() < 900) {
            return Math.max(1, demand.days());
        }
        if (dayBudget < 450) {
            return Math.min(base, demand.days() * 2);
        }
        if (dayBudget < 700) {
            return Math.min(base, demand.days() * Math.min(3, demand.pace().getMaxSpots()));
        }
        if (dayBudget > 1200 && demand.pace().getMaxSpots() >= 3) {
            return base + Math.max(1, demand.days() - 1);
        }
        return base;
    }

    private double budgetScore(ScenicSpot spot, Demand demand) {
        double dayBudget = demand.budget() / Math.max(demand.days(), 1.0);
        double visitCost = estimatedVisitCost(spot);
        if (demand.budget() < 500) {
            return visitCost <= Math.max(80, demand.budget() * 0.55) ? 0.95 : 0.28;
        }
        if (visitCost <= dayBudget * 0.18) {
            return 1.0;
        }
        if (visitCost <= dayBudget * 0.30) {
            return 0.82;
        }
        if (visitCost <= dayBudget * 0.45) {
            return 0.62;
        }
        return 0.36;
    }

    private int estimatedVisitCost(ScenicSpot spot) {
        String category = spot.category() == null ? "" : spot.category();
        int localSpend = 35;
        if (category.contains("美食")) {
            localSpend = 100;
        } else if (category.contains("茶馆") || category.contains("咖啡")) {
            localSpend = 55;
        } else if (category.contains("购物") || category.contains("商圈")) {
            localSpend = 120;
        } else if (category.contains("博物馆") || category.contains("历史")) {
            localSpend = 45;
        } else if (category.contains("公园") || category.contains("休闲")) {
            localSpend = 30;
        }
        return spot.ticketPrice() + localSpend;
    }

    private double loadPenalty(ScenicSpot spot, Demand demand) {
        double penalty = 0.0;
        if ("low".equals(demand.walkingTolerance())) {
            penalty += spot.physicalLoad() * 0.10;
        }
        if ("high".equals(demand.crowdSensitivity())) {
            penalty += (spot.crowdLoad() + spot.queueLoad()) * 0.08;
        }
        if ("comfort".equals(demand.comfortPreference())) {
            penalty += (spot.physicalLoad() + spot.cognitiveLoad() + spot.crowdLoad() + spot.queueLoad()) * 0.03;
        }
        return penalty;
    }

    private String explainSpot(ScenicSpot spot, Demand demand, long matchCount, double budgetScore, double loadPenalty, double mustBoost) {
        if (mustBoost > 0) {
            return spot.name() + "被设置为必去点，系统优先保留，并在节奏引擎中校验体力和拥挤负担。";
        }
        String interestPart = matchCount > 0
                ? "匹配 " + matchCount + " 个兴趣标签"
                : "作为同城补充景点提升路线完整度";
        String budgetPart = budgetScore >= 0.82 ? "预计花费符合预算" : "预计花费偏高，已降低排序";
        String loadPart = loadPenalty > 0.08 ? "已因用户节奏偏好降低排序" : "节奏负担可控";
        return interestPart + "，" + budgetPart + "，" + loadPart + "。";
    }

    private double round(double value) {
        return Math.round(value * 1000) / 1000.0;
    }
}
