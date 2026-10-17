package com.example.travel.repository;

import com.example.travel.dto.DiaryRequest;
import com.example.travel.model.DiaryStats;
import com.example.travel.model.TravelDiary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Repository
public class DiaryRepository {
    private final JdbcTemplate jdbcTemplate;

    public DiaryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<TravelDiary> list(String destination, String keyword, String sort, int limit) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT id, title, destination, author_name, content, image_url, video_url, interest_tags,
                       view_count, rating, rating_count, compressed_content, create_time
                FROM travel_diary
                WHERE 1 = 1
                """);
        if (destination != null && !destination.isBlank()) {
            sql.append(" AND destination LIKE ?");
            args.add("%" + destination.trim() + "%");
        }
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (title LIKE ? OR content LIKE ? OR interest_tags LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
            args.add(like);
        }
        sql.append(orderBy(sort));
        sql.append(" LIMIT ?");
        args.add(Math.max(1, Math.min(limit <= 0 ? 20 : limit, 100)));
        return jdbcTemplate.query(sql.toString(), this::mapDiary, args.toArray());
    }

    public TravelDiary findById(long id) {
        return jdbcTemplate.queryForObject("""
                SELECT id, title, destination, author_name, content, image_url, video_url, interest_tags,
                       view_count, rating, rating_count, compressed_content, create_time
                FROM travel_diary
                WHERE id = ?
                """, this::mapDiary, id);
    }

    public List<TravelDiary> findByExactTitle(String title, int limit) {
        if (title == null || title.isBlank()) {
            return List.of();
        }
        return jdbcTemplate.query("""
                SELECT id, title, destination, author_name, content, image_url, video_url, interest_tags,
                       view_count, rating, rating_count, compressed_content, create_time
                FROM travel_diary
                WHERE title = ?
                ORDER BY view_count DESC, rating DESC, id DESC
                LIMIT ?
                """, this::mapDiary, title.trim(), Math.max(1, Math.min(limit <= 0 ? 20 : limit, 100)));
    }

    public List<TravelDiary> fullTextSearch(String keyword, String sort, int limit) {
        if (keyword == null || keyword.isBlank()) {
            return list(null, null, sort, limit);
        }
        int safeLimit = Math.max(1, Math.min(limit <= 0 ? 20 : limit, 100));
        try {
            List<TravelDiary> result = jdbcTemplate.query("""
                    SELECT id, title, destination, author_name, content, image_url, video_url, interest_tags,
                           view_count, rating, rating_count, compressed_content, create_time,
                           MATCH(title, content, interest_tags) AGAINST (? IN NATURAL LANGUAGE MODE) AS relevance
                    FROM travel_diary
                    WHERE MATCH(title, content, interest_tags) AGAINST (? IN NATURAL LANGUAGE MODE)
                    ORDER BY relevance DESC, view_count DESC, rating DESC, id DESC
                    LIMIT ?
                    """, this::mapDiary, keyword.trim(), keyword.trim(), safeLimit);
            return result.isEmpty() ? list(null, keyword, sort, safeLimit) : result;
        } catch (RuntimeException error) {
            return list(null, keyword, sort, safeLimit);
        }
    }

    public long create(DiaryRequest request, String compressedContent) {
        jdbcTemplate.update("""
                INSERT INTO travel_diary
                (title, destination, author_name, content, image_url, video_url, interest_tags, compressed_content)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                request.title().trim(),
                request.destination().trim(),
                blankToDefault(request.authorName(), "游客"),
                request.content().trim(),
                blankToDefault(request.imageUrl(), ""),
                blankToDefault(request.videoUrl(), ""),
                blankToDefault(request.interestTags(), ""),
                compressedContent);
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return id == null ? 0 : id;
    }

    public void increaseView(long id) {
        jdbcTemplate.update("UPDATE travel_diary SET view_count = view_count + 1 WHERE id = ?", id);
    }

    public void rate(long id, double newRating) {
        TravelDiary diary = findById(id);
        int nextCount = diary.ratingCount() + 1;
        double nextRating = ((diary.rating() * diary.ratingCount()) + newRating) / Math.max(1, nextCount);
        jdbcTemplate.update("UPDATE travel_diary SET rating = ?, rating_count = ? WHERE id = ?", round(nextRating), nextCount, id);
    }

    public DiaryStats stats() {
        return jdbcTemplate.queryForObject("""
                SELECT
                  COUNT(*) AS diary_count,
                  COALESCE(SUM(view_count), 0) AS total_views,
                  COALESCE(AVG(NULLIF(rating, 0)), 0) AS average_rating,
                  SUM(CASE WHEN compressed_content IS NOT NULL AND compressed_content <> '' THEN 1 ELSE 0 END) AS compressed_count
                FROM travel_diary
                """, (rs, rowNum) -> new DiaryStats(
                rs.getInt("diary_count"),
                rs.getInt("total_views"),
                round(rs.getDouble("average_rating")),
                rs.getInt("compressed_count")
        ));
    }

    private String orderBy(String sort) {
        String normalized = sort == null ? "hot" : sort.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "rating" -> " ORDER BY rating DESC, view_count DESC, id DESC";
            case "new" -> " ORDER BY create_time DESC, id DESC";
            default -> " ORDER BY view_count DESC, rating DESC, id DESC";
        };
    }

    private TravelDiary mapDiary(ResultSet rs, int rowNum) throws SQLException {
        return new TravelDiary(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("destination"),
                rs.getString("author_name"),
                rs.getString("content"),
                rs.getString("image_url"),
                rs.getString("video_url"),
                rs.getString("interest_tags"),
                rs.getInt("view_count"),
                rs.getDouble("rating"),
                rs.getInt("rating_count"),
                rs.getString("compressed_content"),
                rs.getTimestamp("create_time").toLocalDateTime()
        );
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
