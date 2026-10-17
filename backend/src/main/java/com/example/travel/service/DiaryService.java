package com.example.travel.service;

import com.example.travel.dto.DiaryRequest;
import com.example.travel.model.TravelDiary;
import com.example.travel.repository.DiaryRepository;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

@Service
public class DiaryService {
    private final DiaryRepository diaryRepository;

    public DiaryService(DiaryRepository diaryRepository) {
        this.diaryRepository = diaryRepository;
    }

    public TravelDiary create(DiaryRequest request) {
        long id = diaryRepository.create(request, compress(request.content()));
        return diaryRepository.findById(id);
    }

    public TravelDiary detail(long id) {
        diaryRepository.increaseView(id);
        return diaryRepository.findById(id);
    }

    public List<TravelDiary> recommend(String interest, String destination, int limit) {
        Set<String> interests = splitTags(interest);
        return diaryRepository.list(destination, null, "hot", 100).stream()
                .sorted(Comparator.comparingDouble((TravelDiary diary) -> recommendScore(diary, interests)).reversed())
                .limit(Math.max(1, Math.min(limit <= 0 ? 10 : limit, 30)))
                .toList();
    }

    public void rate(long id, double rating) {
        double normalized = Math.max(1, Math.min(5, rating));
        diaryRepository.rate(id, normalized);
    }

    private double recommendScore(TravelDiary diary, Set<String> interests) {
        long match = splitTags(diary.interestTags()).stream().filter(interests::contains).count();
        double interestScore = interests.isEmpty() ? 0.3 : match / Math.max(1.0, interests.size());
        return interestScore * 0.45 + Math.min(1, diary.viewCount() / 150.0) * 0.30 + (diary.rating() / 5.0) * 0.25;
    }

    private Set<String> splitTags(String raw) {
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(raw.split("[,，、\\s]+"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .collect(java.util.stream.Collectors.toSet());
    }

    private String compress(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(bytes)) {
                gzip.write(content.getBytes(StandardCharsets.UTF_8));
            }
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch (IOException error) {
            return "";
        }
    }
}
