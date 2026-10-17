package com.example.travel.service;

import com.example.travel.model.Demand;
import com.example.travel.model.ItineraryNode;
import com.example.travel.model.RhythmState;
import com.example.travel.model.ScoredSpot;
import com.example.travel.model.ScenicSpot;
import com.example.travel.repository.SpotRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class RhythmEngineService {
    private final SpotRepository spotRepository;

    public RhythmEngineService(SpotRepository spotRepository) {
        this.spotRepository = spotRepository;
    }

    public RhythmResult apply(Demand demand, List<ScoredSpot> ordered, int[][] dist) {
        List<ItineraryNode> nodes = new ArrayList<>();
        List<String> interventions = new ArrayList<>();
        double risk = startRisk(demand);
        int currentMinute = 9 * 60;
        int travelMinutes = 20;
        int consecutiveHighLoad = 0;

        for (int i = 0; i < ordered.size(); i++) {
            ScoredSpot item = ordered.get(i);
            ScenicSpot spot = item.spot();
            int transfer = i == 0 ? 20 : dist[ordered.get(i - 1).spot().id() - 1][spot.id() - 1];

            if (shouldRecover(demand, spot, risk, transfer, consecutiveHighLoad)) {
                ScoredSpot recovery = nearestRecovery(demand.destination(), spot, dist, nodes);
                if (recovery != null) {
                    int recoveryTransfer = i == 0 ? 15 : dist[ordered.get(i - 1).spot().id() - 1][recovery.spot().id() - 1];
                    currentMinute += recoveryTransfer;
                    RhythmState beforeRecovery = toState(risk);
                    risk = Math.max(16, risk - recovery.spot().recoveryValue() * 45);
                    ItineraryNode recoveryNode = new ItineraryNode(
                            recovery,
                            time(currentMinute),
                            time(currentMinute + recoveryMinutes(demand)),
                            beforeRecovery,
                            toState(risk),
                            round(risk),
                            true,
                            "检测到连续高负担或长距离转移，插入恢复节点降低节奏压力。",
                            recoveryMinutes(demand)
                    );
                    nodes.add(recoveryNode);
                    interventions.add("在 " + spot.name() + " 前插入 " + recovery.spot().name() + " 作为恢复节点。");
                    currentMinute += recoveryMinutes(demand);
                    travelMinutes += recoveryTransfer;
                    consecutiveHighLoad = 0;
                    transfer = dist[recovery.spot().id() - 1][spot.id() - 1];
                }
            }

            currentMinute += transfer;
            RhythmState before = toState(risk);
            risk += loadDelta(demand, spot, transfer);
            if (isHighLoad(spot)) {
                consecutiveHighLoad += 1;
            } else {
                consecutiveHighLoad = 0;
            }
            int stayMinutes = (int) Math.round(spot.stayDuration() * 60);
            nodes.add(new ItineraryNode(
                    item,
                    time(currentMinute),
                    time(currentMinute + stayMinutes),
                    before,
                    toState(risk),
                    round(risk),
                    spot.isRecoveryNode(),
                    nodeReason(spot, before, toState(risk)),
                    stayMinutes
            ));
            currentMinute += stayMinutes;
            travelMinutes += transfer;
        }

        double totalHours = (currentMinute - 9 * 60) / 60.0;
        RhythmState finalState = nodes.isEmpty() ? RhythmState.RELAXED : nodes.get(nodes.size() - 1).stateAfter();
        return new RhythmResult(nodes, travelMinutes, round(totalHours), finalState, (int) Math.min(100, Math.round(risk)), interventions);
    }

    private boolean shouldRecover(Demand demand, ScenicSpot next, double risk, int transfer, int consecutiveHighLoad) {
        if (demand.needNoonRest() && risk > 48) {
            return true;
        }
        if (risk + loadDelta(demand, next, transfer) >= 72) {
            return true;
        }
        if (consecutiveHighLoad >= 1 && isHighLoad(next)) {
            return true;
        }
        return transfer >= 28 && isHighLoad(next);
    }

    private ScoredSpot nearestRecovery(String city, ScenicSpot target, int[][] dist, List<ItineraryNode> existing) {
        return spotRepository.recoveryNodes(city).stream()
                .filter(spot -> existing.stream().noneMatch(node -> node.item().spot().id() == spot.id()))
                .min(Comparator.comparingInt(spot -> dist[target.id() - 1][spot.id() - 1]))
                .map(spot -> new ScoredSpot(spot, 0.8, "恢复价值 " + Math.round(spot.recoveryValue() * 100) + "%，适合降低当前疲劳状态。"))
                .orElse(null);
    }

    private double loadDelta(Demand demand, ScenicSpot spot, int transfer) {
        if (spot.isRecoveryNode()) {
            return -spot.recoveryValue() * 35;
        }
        double base = spot.physicalLoad() * 22 + spot.cognitiveLoad() * 16 + spot.crowdLoad() * 14 + spot.queueLoad() * 12 + transfer * 0.18;
        if ("low".equals(demand.walkingTolerance())) {
            base += spot.physicalLoad() * 14;
        }
        if ("high".equals(demand.crowdSensitivity())) {
            base += (spot.crowdLoad() + spot.queueLoad()) * 10;
        }
        if ("coverage".equals(demand.comfortPreference())) {
            base *= 0.88;
        }
        if (demand.pace().name().equals("relaxed")) {
            base *= 1.10;
        }
        return base;
    }

    private boolean isHighLoad(ScenicSpot spot) {
        return spot.physicalLoad() + spot.cognitiveLoad() + spot.crowdLoad() + spot.queueLoad() >= 2.05;
    }

    private double startRisk(Demand demand) {
        return switch (demand.pace()) {
            case relaxed -> 16;
            case balanced -> 22;
            case compact -> 28;
        };
    }

    private int recoveryMinutes(Demand demand) {
        return demand.needNoonRest() || "comfort".equals(demand.comfortPreference()) ? 45 : 35;
    }

    private RhythmState toState(double risk) {
        if (risk < 34) {
            return RhythmState.RELAXED;
        }
        if (risk < 62) {
            return RhythmState.STABLE;
        }
        if (risk < 86) {
            return RhythmState.FATIGUED;
        }
        return RhythmState.OVERLOADED;
    }

    private String nodeReason(ScenicSpot spot, RhythmState before, RhythmState after) {
        if (spot.isRecoveryNode()) {
            return "恢复节点，用于降低连续移动后的疲劳风险。";
        }
        if (after.ordinal() > before.ordinal()) {
            return "该节点带来一定体力、认知或拥挤负担，状态从 " + before.name() + " 转为 " + after.name() + "。";
        }
        return "该节点负担可控，状态保持在 " + after.name() + "。";
    }

    private String time(int minuteOfDay) {
        int normalized = minuteOfDay % (24 * 60);
        return String.format("%02d:%02d", normalized / 60, normalized % 60);
    }

    private double round(double value) {
        return Math.round(value * 10) / 10.0;
    }

    public record RhythmResult(
            List<ItineraryNode> nodes,
            int travelMinutes,
            double totalHours,
            RhythmState finalState,
            int fatigueRisk,
            List<String> interventions
    ) {
    }
}
