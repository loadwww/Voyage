package com.example.travel.controller;

import com.example.travel.dto.ApiResponse;
import com.example.travel.dto.GroupPlanRequest;
import com.example.travel.model.TravelPlan;
import com.example.travel.service.GroupPlanService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/group-plans")
public class GroupPlanController {
    private final GroupPlanService groupPlanService;

    public GroupPlanController(GroupPlanService groupPlanService) {
        this.groupPlanService = groupPlanService;
    }

    @PostMapping("/generate")
    public ApiResponse<TravelPlan> generate(@Valid @RequestBody GroupPlanRequest request) {
        return ApiResponse.ok(groupPlanService.generate(request));
    }
}
