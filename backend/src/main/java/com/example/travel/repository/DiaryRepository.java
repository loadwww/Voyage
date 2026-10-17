package com.example.travel.repository;

import com.example.travel.dto.DiaryRequest;
import com.example.travel.model.DiaryStats;
import com.example.travel.model.PageResult;
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
    private static final String COLUMNS = """
            id, title, destination, author_name, content, image_url, video_url, interest_tags,
            view_count, rating, rating_count, compressed_content, create_time
            """;

    private final JdbcTemplate jdbcTemplate;

    public DiaryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<TravelDiary> list(String destination, String keyword, String sort, int limit) {
        SqlWhere where = SqlWhere.filter(destination, keyword);
        String sql = "SELECT " + COLUMNS + " FROM travel_diary " + where.clause() + orderBy(sort) + " LIMIT ?";
        List<Object> args = new ArrayList<>(where.args());
        args.add(safeLimit(limit));
        return jdbcTemplate.query(sql, this::mapDiary, args.toArray());
    }

    /**
     * 分页查询日记（offset 分页）。
     */
    public PageResult<TravelDiary> page(String destination, String keyword, String sort, int page, int pageSize) {
        int safePage = Math.max(1, page);
        int safePageSize = Math.max(1, Math.min(pageSize <= 0 ? 20 : pageSize, 100));
        SqlWhere where = SqlWhere.filter(destination, keyword);

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM travel_diary " + where.clause(),
                Long.class,
                where.args().toArray()
        );
        long totalCount = total == null ? 0 : total;

        List<Object> args = new ArrayList<>(where.args());
        args.add(safePageSize);
        args.add((safePage - 1) * safePageSize);
        List<TravelDiary> items = jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM travel_diary " + where.clause() + orderBy(sort) + " LIMIT ? OFFSET ?",
                this::mapDiary,
                args.toArray()
        );
        return PageResult.of(items, totalCount, safePage, safePageSize);
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

    /**
     * 浏览量 +1 的兜底直写（热点行场景下由 Redis 缓冲 + 批量落库取代，见 ViewCountService）。
     */
    public void increaseView(long id) {
        jdbcTemplate.update("UPDATE travel_diary SET view_count = view_count + 1 WHERE id = ?", id);
    }

    /**
     * 定时任务批量回写浏览量增量。
     */
    public int addViewDelta(long id, long delta) {
        if (delta <= 0) {
            return 0;
        }
        return jdbcTemplate.update(
                "UPDATE travel_diary SET view_count = view_count + ? WHERE id = ?",
                (int) Math.min(Integer.MAX_VALUE, delta), id);
    }

    /**
     * 原子评分更新：单条 UPDATE 同时完成加权平均与计数递增。
     * <p>设计考虑：并发评分下"先 SELECT 再 UPDATE"的读-改-写两步会互相覆盖（丢失更新），
     * 因此直接用一条原子 UPDATE，整条语句在 InnoDB 行锁内完成，等价于一次 CAS。
     * <p>关键：MySQL 的 SET 子句从左到右求值，rating 必须写在 rating_count 之前，
     * 否则新评分会用到已自增的 count。
     *
     * @return 受影响行数，0 表示日记不存在
     */
    public int rate(long id, double newRating) {
        return jdbcTemplate.update("""
                UPDATE travel_diary
                SET rating = ROUND((rating * rating_count + ?) / (rating_count + 1), 2),
                    rating_count = rating_count + 1
                WHERE id = ?
                """, newRating, id);
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

    private int safeLimit(int limit) {
        return Math.max(1, Math.min(limit <= 0 ? 20 : limit, 100));
    }

    /**
     * 日记列表的公共过滤条件（list / page / count 复用，保证三处语义一致）。
     */
    private record SqlWhere(String clause, List<Object> args) {
        private static SqlWhere filter(String destination, String keyword) {
            StringBuilder clause = new StringBuilder("WHERE 1 = 1");
            List<Object> args = new ArrayList<>();
            if (destination != null && !destination.isBlank()) {
                clause.append(" AND destination LIKE ?");
                args.add("%" + destination.trim() + "%");
            }
            if (keyword != null && !keyword.isBlank()) {
                clause.append(" AND (title LIKE ? OR content LIKE ? OR interest_tags LIKE ?)");
                String like = "%" + keyword.trim() + "%";
                args.add(like);
                args.add(like);
                args.add(like);
            }
            return new SqlWhere(clause.toString(), args);
        }
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
