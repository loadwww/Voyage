package com.example.travel.controller;

import com.example.travel.dto.ApiResponse;
import com.example.travel.dto.MetaResponse;
import com.example.travel.model.PaceType;
import com.example.travel.repository.SpotRepository;
import com.example.travel.service.DemandParseService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/meta")
public class MetaController {
    private final DemandParseService demandParseService;
    private final SpotRepository spotRepository;

    public MetaController(DemandParseService demandParseService, SpotRepository spotRepository) {
        this.demandParseService = demandParseService;
        this.spotRepository = spotRepository;
    }

    @GetMapping
    public ApiResponse<MetaResponse> meta() {
        Map<String, String> paces = Arrays.stream(PaceType.values())
                .collect(Collectors.toMap(PaceType::name, PaceType::getLabel));
        return ApiResponse.ok(new MetaResponse(
                demandParseService.interests(),
                spotRepository.cities(),
                spotRepository.categories(),
                paces
        ));
    }
}
