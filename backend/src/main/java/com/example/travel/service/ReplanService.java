package com.example.travel.service;

import com.example.travel.dto.ReplanRequest;
import com.example.travel.model.Demand;
import com.example.travel.model.PaceType;
import com.example.travel.model.TravelPlan;
import org.springframework.stereotype.Service;

@Service
public class ReplanService {
    private final PlanService planService;

    public ReplanService(PlanService planService) {
        this.planService = planService;
    }

    public TravelPlan replan(ReplanRequest request) {
        Demand base = request.demand().toDemand();
        String adjustmentType = request.adjustmentType() == null ? "" : request.adjustmentType();
        PaceType nextPace = request.newPace() == null ? base.pace() : PaceType.from(request.newPace());
        String nextWalking = base.walkingTolerance();
        boolean nextRest = base.needNoonRest();
        String nextComfort = base.comfortPreference();

        if ("lighter".equals(adjustmentType)) {
            nextPace = PaceType.relaxed;
            nextWalking = "low";
            nextComfort = "comfort";
            nextRest = true;
        } else if ("lessWalking".equals(adjustmentType)) {
            nextWalking = "low";
        } else if ("moreRecovery".equals(adjustmentType)) {
            nextRest = true;
            nextComfort = "comfort";
        } else if ("shortenDay".equals(adjustmentType)) {
            nextPace = PaceType.relaxed;
        }

        Demand adjusted = new Demand(
                base.rawText(),
                base.destination(),
                base.days(),
                request.newBudget() == null ? base.budget() : request.newBudget(),
                base.interests(),
                nextPace,
                base.constraints(),
                nextWalking,
                nextRest,
                base.crowdSensitivity(),
                nextComfort
        ).normalize();
        int nextVersion = request.versionNo() == null ? 2 : request.versionNo() + 1;
        return planService.generate(adjusted, request.parentPlanId(), nextVersion, request.mustSpotId(), request.removeSpotId());
    }
}
