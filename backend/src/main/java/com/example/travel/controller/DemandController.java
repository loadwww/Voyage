package com.example.travel.controller;

import com.example.travel.dto.ApiResponse;
import com.example.travel.dto.ParseTextRequest;
import com.example.travel.model.Demand;
import com.example.travel.service.DemandParseService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/demand")
public class DemandController {
    private final DemandParseService demandParseService;

    public DemandController(DemandParseService demandParseService) {
        this.demandParseService = demandParseService;
    }

    @PostMapping("/parse")
    public ApiResponse<Demand> parse(@RequestBody ParseTextRequest request) {
        return ApiResponse.ok(demandParseService.parse(request.text()));
    }
}
