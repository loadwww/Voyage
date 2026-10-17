package com.example.travel.service;

import com.example.travel.algorithm.FatigueCalculator;
import com.example.travel.algorithm.FloydShortestPath;
import com.example.travel.algorithm.TspDpPlanner;
import com.example.travel.model.DayPlan;
import com.example.travel.model.Demand;
import com.example.travel.model.ScoredSpot;
import com.example.travel.model.TravelPlan;
import com.example.travel.repository.SpotRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class PlanService {
    private final SpotRepository spotRepository;
    private final RecallService recallService;
    private final RhythmEngineService rhythmEngineService;
    private final FloydShortestPath floyd = new FloydShortestPath();
    private final TspDpPlanner tsp = new TspDpPlanner();
    private final FatigueCalculator fatigueCalculator = new FatigueCalculator();

    public PlanService(SpotRepository spotRepository, RecallService recallService, RhythmEngineService rhythmEngineService) {
        this.spotRepository = spotRepository;
        this.recallService = recallService;
        this.rhythmEngineService = rhythmEngineService;
    }

    public TravelPlan generate(Demand demand) {
        return generate(demand.normalize(), null, 1, null, null);
    }

    public TravelPlan generate(Demand demand, String parentPlanId, int versionNo, Integer mustSpotId, Integer removeSpotId) {
        Demand normalized = demand.normalize();
        List<ScoredSpot> selected = recallService.recall(normalized, mustSpotId, removeSpotId);
        selected = applyBudgetLimit(selected, normalized, mustSpotId);
        int[][] dist = floyd.shortestDistances(spotRepository.travelMinutes());
        List<DayPlan> days = splitIntoDays(selected, normalized, dist);
        int totalTicket = selected.stream().mapToInt(item -> item.spot().ticketPrice()).sum();
        int transportCost = days.stream().mapToInt(day -> (int) Math.round(day.travelMinutes() * transportCostRate(normalized))).sum();
        int foodAndLocal = selected.stream().mapToInt(item -> estimatedLocalSpend(item.spot(), normalized)).sum()
                + normalized.days() * dailyBaseSpend(normalized);
        int totalCost = totalTicket + transportCost + foodAndLocal;
        double totalHours = days.stream().mapToDouble(DayPlan::totalHours).sum();
        int match = selected.isEmpty()
                ? 0
                : (int) Math.round(selected.stream().mapToDouble(ScoredSpot::score).average().orElse(0) * 100);
        List<String> explanations = buildExplanations(normalized, selected, days, totalCost);
        return new TravelPlan(
                "PLAN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                normalized,
                selected,
                days,
                totalCost,
                round(totalHours),
                match,
                normalized.destination() + normalized.days() + "日动态节奏路线，共推荐 " + selected.size() + " 个主要景点，并按需插入恢复节点。",
                explanations,
                parentPlanId,
                versionNo
        );
    }

    private List<DayPlan> splitIntoDays(List<ScoredSpot> selected, Demand demand, int[][] dist) {
        List<List<ScoredSpot>> buckets = new ArrayList<>();
        for (int i = 0; i < demand.days(); i++) {
            buckets.add(new ArrayList<>());
        }
        List<ScoredSpot> orderedByScore = selected.stream()
                .sorted(Comparator.comparingDouble(ScoredSpot::score).reversed())
                .toList();
        for (ScoredSpot spot : orderedByScore) {
            List<ScoredSpot> target = buckets.stream()
                    .filter(bucket -> bucket.size() < demand.pace().getMaxSpots())
                    .min(Comparator.comparingInt(List::size))
                    .orElse(buckets.get(0));
            target.add(spot);
        }

        List<DayPlan> days = new ArrayList<>();
        for (int i = 0; i < buckets.size(); i++) {
            List<ScoredSpot> ordered = tsp.order(buckets.get(i), dist);
            RhythmEngineService.RhythmResult rhythm = rhythmEngineService.apply(demand, ordered, dist);
            String routeReason = ordered.isEmpty()
                    ? "当天保留自由探索或机动休息。"
                    : "先使用 Floyd-Warshall 与 TSP 优化访问顺序，再由节奏引擎检查状态转移并插入恢复节点。";
            days.add(new DayPlan(
                    i + 1,
                    ordered,
                    rhythm.nodes(),
                    rhythm.travelMinutes(),
                    rhythm.totalHours(),
                    fatigueCalculator.calculate(rhythm.fatigueRisk(), demand.pace()),
                    rhythm.finalState(),
                    rhythm.fatigueRisk(),
                    routeReason,
                    rhythm.interventions()
            ));
        }
        return days;
    }

    private List<ScoredSpot> applyBudgetLimit(List<ScoredSpot> selected, Demand demand, Integer mustSpotId) {
        int budgetLimit = demand.budget();
        int fixedDailyCost = demand.days() * dailyBaseSpend(demand);
        int softLimit = Math.max(80, budgetLimit - fixedDailyCost);
        int selectedCountLimit = budgetSpotLimit(demand);
        List<ScoredSpot> ordered = selected.stream()
                .sorted(Comparator.comparingDouble(this::budgetValue).reversed())
                .toList();

        List<ScoredSpot> result = new ArrayList<>();
        int estimatedCost = fixedDailyCost;
        for (ScoredSpot item : ordered) {
            boolean mustKeep = mustSpotId != null && item.spot().id() == mustSpotId;
            int itemCost = item.spot().ticketPrice() + estimatedLocalSpend(item.spot(), demand) + estimatedMoveCost(demand);
            boolean withinCount = result.size() < selectedCountLimit;
            boolean withinBudget = estimatedCost + itemCost <= fixedDailyCost + softLimit;
            if (mustKeep || (withinCount && withinBudget)) {
                result.add(item);
                estimatedCost += itemCost;
            }
        }

        int minimum = Math.min(selected.size(), minimumSpotCount(demand));
        for (ScoredSpot item : ordered) {
            if (result.size() >= minimum) {
                break;
            }
            if (result.stream().noneMatch(existing -> existing.spot().id() == item.spot().id())) {
                result.add(item);
            }
        }
        return result.stream()
                .sorted(Comparator.comparingDouble(ScoredSpot::score).reversed())
                .toList();
    }

    private int budgetSpotLimit(Demand demand) {
        double dayBudget = demand.budget() / Math.max(demand.days(), 1.0);
        if (demand.budget() < 500) {
            return 1;
        }
        if (demand.budget() < 900) {
            return Math.max(1, demand.days());
        }
        if (dayBudget < 450) {
            return demand.days() * 2;
        }
        if (dayBudget < 700) {
            return demand.days() * Math.min(3, demand.pace().getMaxSpots());
        }
        return Math.max(demand.days() * 2, demand.days() * demand.pace().getMaxSpots());
    }

    private int minimumSpotCount(Demand demand) {
        if (demand.budget() < 500) {
            return 1;
        }
        if (demand.budget() < 900) {
            return Math.max(1, demand.days());
        }
        return Math.max(1, demand.days() * 2);
    }

    private double budgetValue(ScoredSpot item) {
        int visitCost = item.spot().ticketPrice() + baseCategorySpend(item.spot());
        return item.score() / Math.max(visitCost, 30);
    }

    private int estimatedLocalSpend(com.example.travel.model.ScenicSpot spot, Demand demand) {
        double dayBudget = demand.budget() / Math.max(demand.days(), 1.0);
        double factor = demand.budget() < 500 ? 0.35 : dayBudget < 300 ? 0.55 : dayBudget < 450 ? 0.72 : dayBudget < 700 ? 0.92 : dayBudget > 1200 ? 1.28 : 1.0;
        return (int) Math.round(baseCategorySpend(spot) * factor);
    }

    private int baseCategorySpend(com.example.travel.model.ScenicSpot spot) {
        String category = spot.category() == null ? "" : spot.category();
        if (category.contains("美食")) {
            return 100;
        }
        if (category.contains("茶馆") || category.contains("咖啡")) {
            return 55;
        }
        if (category.contains("购物") || category.contains("商圈")) {
            return 120;
        }
        if (category.contains("博物馆") || category.contains("历史")) {
            return 45;
        }
        if (category.contains("公园") || category.contains("休闲")) {
            return 30;
        }
        return 35;
    }

    private int dailyBaseSpend(Demand demand) {
        double dayBudget = demand.budget() / Math.max(demand.days(), 1.0);
        if (demand.budget() < 500) {
            return 35;
        }
        if (dayBudget < 300) {
            return 65;
        }
        if (dayBudget < 450) {
            return 70;
        }
        if (dayBudget < 700) {
            return 110;
        }
        if (dayBudget > 1200) {
            return 190;
        }
        return 140;
    }

    private int estimatedMoveCost(Demand demand) {
        double dayBudget = demand.budget() / Math.max(demand.days(), 1.0);
        if (demand.budget() < 500) {
            return 8;
        }
        if (dayBudget < 300) {
            return 12;
        }
        return dayBudget < 450 ? 18 : 32;
    }

    private double transportCostRate(Demand demand) {
        double dayBudget = demand.budget() / Math.max(demand.days(), 1.0);
        if (demand.budget() < 500) {
            return 0.20;
        }
        if (dayBudget < 300) {
            return 0.35;
        }
        if (dayBudget < 450) {
            return 0.45;
        }
        if (dayBudget < 700) {
            return 0.62;
        }
        if (dayBudget > 1200) {
            return 0.95;
        }
        return 0.78;
    }

    private List<String> buildExplanations(Demand demand, List<ScoredSpot> selected, List<DayPlan> days, int totalCost) {
        List<String> result = new ArrayList<>();
        result.add("地图范围固定为成都主城区核心旅游圈，先通过城市与兴趣标签哈希索引召回候选景点。");
        result.add("候选排序使用 Top-K 优先队列，评分同时考虑兴趣、热度、预算和用户节奏画像。");
        result.add("路线顺序使用 Floyd-Warshall 交通代价矩阵与 TSP 状态压缩 DP，随后进入动态节奏控制。");
        long recoveryCount = days.stream().flatMap(day -> day.nodes().stream()).filter(node -> node.isRecoveryNode()).count();
        result.add("节奏引擎模拟 RELAXED、STABLE、FATIGUED、OVERLOADED 四种状态，本次共插入 " + recoveryCount + " 个恢复节点。");
        result.add("当前方案估算费用约 " + totalCost + " 元，用户预算为 " + demand.budget() + " 元，已按预算控制景点数量、餐饮消费和市内移动方式。");
        if (!selected.isEmpty()) {
            result.add("最高匹配景点是 " + selected.get(0).spot().name() + "，原因：" + selected.get(0).reason());
        }
        return result;
    }

    private double round(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
