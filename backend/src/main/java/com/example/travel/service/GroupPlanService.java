package com.example.travel.service;

import com.example.travel.dto.GroupMemberRequest;
import com.example.travel.dto.GroupPlanRequest;
import com.example.travel.model.Demand;
import com.example.travel.model.PaceType;
import com.example.travel.model.TravelPlan;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class GroupPlanService {
    private final PlanService planService;

    public GroupPlanService(PlanService planService) {
        this.planService = planService;
    }

    public TravelPlan generate(GroupPlanRequest request) {
        List<GroupMemberRequest> members = request.members() == null ? List.of() : request.members();
        List<String> interests = mergeInterests(members);
        PaceType pace = mergePace(members);
        int budget = request.budget() == null ? averageBudget(members) : request.budget();
        Demand demand = new Demand(
                "群体出游：" + (request.groupName() == null ? "未命名小组" : request.groupName()),
                request.destination(),
                request.days() == null ? 3 : request.days(),
                budget,
                interests,
                pace,
                request.constraints(),
                "medium",
                true,
                "medium",
                "comfort"
        ).normalize();
        return planService.generate(demand);
    }

    private List<String> mergeInterests(List<GroupMemberRequest> members) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (GroupMemberRequest member : members) {
            if (member.interests() == null) {
                continue;
            }
            for (String interest : member.interests()) {
                counts.merge(interest, 1, Integer::sum);
            }
        }
        if (counts.isEmpty()) {
            return List.of("历史", "美食", "拍照");
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .map(Map.Entry::getKey)
                .limit(4)
                .toList();
    }

    private PaceType mergePace(List<GroupMemberRequest> members) {
        double average = members.stream()
                .map(GroupMemberRequest::pace)
                .map(PaceType::from)
                .mapToInt(Enum::ordinal)
                .average()
                .orElse(PaceType.balanced.ordinal());
        return PaceType.values()[(int) Math.round(average)];
    }

    private int averageBudget(List<GroupMemberRequest> members) {
        return (int) members.stream()
                .filter(member -> member.budgetPreference() != null)
                .mapToInt(GroupMemberRequest::budgetPreference)
                .average()
                .orElse(1600);
    }
}
