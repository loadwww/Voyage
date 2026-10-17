package com.example.travel.dto;

import java.util.List;

public record GroupMemberRequest(
        String name,
        List<String> interests,
        String pace,
        Integer budgetPreference
) {
}
