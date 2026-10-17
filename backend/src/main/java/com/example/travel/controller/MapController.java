package com.example.travel.controller;

import com.example.travel.dto.ApiResponse;
import com.example.travel.model.AreaRecommendation;
import com.example.travel.model.FoodRecommendation;
import com.example.travel.model.IndoorPath;
import com.example.travel.model.MapArea;
import com.example.travel.model.MapEdge;
import com.example.travel.model.MapNode;
import com.example.travel.model.MapPath;
import com.example.travel.model.MapStats;
import com.example.travel.model.MultiStopPath;
import com.example.travel.model.NearbyFacility;
import com.example.travel.repository.MapRepository;
import com.example.travel.service.MapNavigationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/map")
public class MapController {
    private final MapRepository mapRepository;
    private final MapNavigationService mapNavigationService;

    public MapController(MapRepository mapRepository, MapNavigationService mapNavigationService) {
        this.mapRepository = mapRepository;
        this.mapNavigationService = mapNavigationService;
    }

    @GetMapping("/areas")
    public ApiResponse<List<MapArea>> areas(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ApiResponse.ok(mapRepository.areas(type, keyword, limit));
    }

    @GetMapping("/recommend-areas")
    public ApiResponse<List<AreaRecommendation>> recommendAreas(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String interest,
            @RequestParam(defaultValue = "hot") String sort,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ApiResponse.ok(mapNavigationService.recommendAreas(type, keyword, interest, sort, limit));
    }

    @GetMapping("/areas/{id}")
    public ApiResponse<MapArea> area(@PathVariable long id) {
        return ApiResponse.ok(mapRepository.area(id));
    }

    @GetMapping("/areas/{id}/nodes")
    public ApiResponse<List<MapNode>> nodes(
            @PathVariable long id,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category
    ) {
        return ApiResponse.ok(mapRepository.nodes(id, type, category));
    }

    @GetMapping("/areas/{id}/edges")
    public ApiResponse<List<MapEdge>> edges(@PathVariable long id) {
        return ApiResponse.ok(mapRepository.edges(id));
    }

    @GetMapping("/areas/{id}/path")
    public ApiResponse<MapPath> path(
            @PathVariable long id,
            @RequestParam long fromNodeId,
            @RequestParam long toNodeId,
            @RequestParam(defaultValue = "distance") String strategy
    ) {
        return ApiResponse.ok(mapNavigationService.shortestPath(id, fromNodeId, toNodeId, strategy));
    }

    @GetMapping("/areas/{id}/multi-path")
    public ApiResponse<MultiStopPath> multiPath(
            @PathVariable long id,
            @RequestParam long startNodeId,
            @RequestParam List<Long> targetNodeIds,
            @RequestParam(defaultValue = "distance") String strategy
    ) {
        return ApiResponse.ok(mapNavigationService.multiStopPath(id, startNodeId, targetNodeIds, strategy));
    }

    @GetMapping("/areas/{id}/indoor-path")
    public ApiResponse<IndoorPath> indoorPath(
            @PathVariable long id,
            @RequestParam long buildingNodeId,
            @RequestParam(defaultValue = "入口") String fromRoom,
            @RequestParam(defaultValue = "305") String toRoom
    ) {
        return ApiResponse.ok(mapNavigationService.indoorPath(id, buildingNodeId, fromRoom, toRoom));
    }

    @GetMapping("/areas/{id}/nearby")
    public ApiResponse<List<NearbyFacility>> nearby(
            @PathVariable long id,
            @RequestParam long fromNodeId,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") double radiusMeters
    ) {
        return ApiResponse.ok(mapNavigationService.nearbyFacilities(id, fromNodeId, category, limit, radiusMeters));
    }

    @GetMapping("/areas/{id}/foods")
    public ApiResponse<List<FoodRecommendation>> foods(
            @PathVariable long id,
            @RequestParam long fromNodeId,
            @RequestParam(required = false) String cuisine,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "hot") String sort,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ApiResponse.ok(mapNavigationService.recommendFoods(id, fromNodeId, cuisine, keyword, sort, limit));
    }

    @GetMapping("/stats")
    public ApiResponse<MapStats> stats() {
        return ApiResponse.ok(mapRepository.stats());
    }
}
