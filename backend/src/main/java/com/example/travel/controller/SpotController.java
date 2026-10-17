package com.example.travel.controller;

import com.example.travel.dto.ApiResponse;
import com.example.travel.model.ScenicSpot;
import com.example.travel.repository.SpotRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/spots")
public class SpotController {
    private final SpotRepository spotRepository;

    public SpotController(SpotRepository spotRepository) {
        this.spotRepository = spotRepository;
    }

    @GetMapping
    public ApiResponse<List<ScenicSpot>> list(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.ok(spotRepository.search(city, category, keyword));
    }

    @GetMapping("/{id}")
    public ApiResponse<ScenicSpot> detail(@PathVariable int id) {
        return spotRepository.findById(id)
                .map(ApiResponse::ok)
                .orElseGet(() -> ApiResponse.fail("景点不存在"));
    }
}
