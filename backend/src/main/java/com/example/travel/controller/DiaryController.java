package com.example.travel.controller;

import com.example.travel.dto.ApiResponse;
import com.example.travel.dto.DiaryRequest;
import com.example.travel.model.DiaryStats;
import com.example.travel.model.TravelDiary;
import com.example.travel.repository.DiaryRepository;
import com.example.travel.service.DiaryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/diaries")
public class DiaryController {
    private final DiaryRepository diaryRepository;
    private final DiaryService diaryService;

    public DiaryController(DiaryRepository diaryRepository, DiaryService diaryService) {
        this.diaryRepository = diaryRepository;
        this.diaryService = diaryService;
    }

    @GetMapping
    public ApiResponse<List<TravelDiary>> list(
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "hot") String sort,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ApiResponse.ok(diaryRepository.list(destination, keyword, sort, limit));
    }

    @GetMapping("/exact-title")
    public ApiResponse<List<TravelDiary>> exactTitle(
            @RequestParam String title,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ApiResponse.ok(diaryRepository.findByExactTitle(title, limit));
    }

    @GetMapping("/fulltext")
    public ApiResponse<List<TravelDiary>> fullText(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "hot") String sort,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ApiResponse.ok(diaryRepository.fullTextSearch(keyword, sort, limit));
    }

    @PostMapping
    public ApiResponse<TravelDiary> create(@Valid @RequestBody DiaryRequest request) {
        return ApiResponse.ok(diaryService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<TravelDiary> detail(@PathVariable long id) {
        return ApiResponse.ok(diaryService.detail(id));
    }

    @PostMapping("/{id}/rate")
    public ApiResponse<TravelDiary> rate(@PathVariable long id, @RequestParam double rating) {
        diaryService.rate(id, rating);
        return ApiResponse.ok(diaryRepository.findById(id));
    }

    @GetMapping("/recommend")
    public ApiResponse<List<TravelDiary>> recommend(
            @RequestParam(required = false) String interest,
            @RequestParam(required = false) String destination,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ApiResponse.ok(diaryService.recommend(interest, destination, limit));
    }

    @GetMapping("/stats")
    public ApiResponse<DiaryStats> stats() {
        return ApiResponse.ok(diaryRepository.stats());
    }
}
