package com.example.travel.dto;

import java.util.List;
import java.util.Map;

public record MetaResponse(
        List<String> interests,
        List<String> cities,
        List<String> categories,
        Map<String, String> paces
) {
}
