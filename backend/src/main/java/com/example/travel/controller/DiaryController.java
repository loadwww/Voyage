package com.example.travel.controller;

import com.example.travel.dto.ApiResponse;
import com.example.travel.dto.DiaryRequest;
import com.example.travel.model.DiaryStats;
import com.example.travel.model.PageResult;
import com.example.travel.model.TravelDiary;
import com.example.travel.repository.DiaryRepository;
import com.example.travel.service.DiaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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

    @Operation(summary = "日记列表", description = "兼容旧版 limit（返回数组）；传入 page 时返回分页对象 PageResult")
    @GetMapping
    public ApiResponse<?> list(
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "hot") String sort,
            @RequestParam(required = false) Integer limit,
            @Parameter(description = "页码，从 1 开始；传入即启用分页响应") @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize
    ) {
        if (page != null) {
            PageResult<TravelDiary> result = diaryRepository.page(
                    destination, keyword, sort, page, pageSize == null ? 20 : pageSize);
            return ApiResponse.ok(result);
        }
        return ApiResponse.ok(diaryRepository.list(destination, keyword, sort, limit == null ? 20 : limit));
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

    @Operation(summary = "发布日记", description = "可选请求头 Idempotency-Key 防止重复提交")
    @PostMapping
    public ApiResponse<TravelDiary> create(
            @Valid @RequestBody DiaryRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return ApiResponse.ok(diaryService.create(request, idempotencyKey));
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
