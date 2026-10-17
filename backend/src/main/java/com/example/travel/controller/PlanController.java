package com.example.travel.controller;

import com.example.travel.dto.ApiResponse;
import com.example.travel.dto.DemandRequest;
import com.example.travel.dto.ReplanRequest;
import com.example.travel.model.TravelPlan;
import com.example.travel.service.PlanService;
import com.example.travel.service.ReplanService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/plans")
public class PlanController {
    private final PlanService planService;
    private final ReplanService replanService;

    public PlanController(PlanService planService, ReplanService replanService) {
        this.planService = planService;
        this.replanService = replanService;
    }

    @PostMapping("/generate")
    public ApiResponse<TravelPlan> generate(@Valid @RequestBody DemandRequest request) {
        return ApiResponse.ok(planService.generate(request.toDemand()));
    }

    @PostMapping("/replan")
    public ApiResponse<TravelPlan> replan(@Valid @RequestBody ReplanRequest request) {
        return ApiResponse.ok(replanService.replan(request));
    }
}
