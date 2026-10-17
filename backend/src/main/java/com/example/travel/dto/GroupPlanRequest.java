package com.example.travel.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.List;

public record GroupPlanRequest(
        String groupName,
        String destination,
        @Min(1) @Max(7) Integer days,
        @Min(300) Integer budget,
        List<GroupMemberRequest> members,
        String constraints
) {
}
