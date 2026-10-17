package com.example.travel.repository;

import com.example.travel.model.ScenicSpot;
import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Repository
public class SpotRepository {
    private static final String FALLBACK_IMAGE =
            "https://images.unsplash.com/photo-1530789253388-582c481c54b0?auto=format&fit=crop&w=900&q=80";

    private final JdbcTemplate jdbcTemplate;
    private final List<ScenicSpot> spots = new ArrayList<>();
    private final Map<Integer, ScenicSpot> byId = new HashMap<>();
    private final Map<String, List<ScenicSpot>> byCity = new HashMap<>();
    private final Map<String, List<ScenicSpot>> byCategory = new HashMap<>();
    private final Map<String, List<ScenicSpot>> byTag = new HashMap<>();
    private int[][] travelMinutes = new int[0][0];

    public SpotRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        spots.clear();
        byId.clear();
        byCity.clear();
        byCategory.clear();
        byTag.clear();

        spots.addAll(loadSpotsFromDatabase());
        indexSpots();
        travelMinutes = loadTravelMinutesFromDatabase();
    }

    public List<ScenicSpot> findAll() {
        return List.copyOf(spots);
    }

    public Optional<ScenicSpot> findById(int id) {
        return Optional.ofNullable(byId.get(id));
    }

    public List<ScenicSpot> recoveryNodes(String city) {
        return byCity.getOrDefault(city, List.of()).stream()
                .filter(ScenicSpot::isRecoveryNode)
                .toList();
    }

    public List<ScenicSpot> search(String city, String category, String keyword) {
        Collection<ScenicSpot> base = isAll(city)
                ? spots
                : byCity.getOrDefault(city.trim(), List.of());
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        return base.stream()
                .filter(spot -> isAll(category) || spot.category().equals(category.trim()))
                .filter(spot -> normalizedKeyword.isBlank() || searchableText(spot).contains(normalizedKeyword))
                .toList();
    }

    public List<String> cities() {
        return byCity.keySet().stream().sorted().toList();
    }

    public List<String> categories() {
        return byCategory.keySet().stream().sorted().toList();
    }

    public int[][] travelMinutes() {
        int[][] copy = new int[travelMinutes.length][];
        for (int i = 0; i < travelMinutes.length; i++) {
            copy[i] = travelMinutes[i].clone();
        }
        return copy;
    }

    public Set<ScenicSpot> recallByTags(String city, List<String> tags) {
        Set<ScenicSpot> result = new LinkedHashSet<>();
        for (String tag : tags) {
            byTag.getOrDefault(tag, List.of()).stream()
                    .filter(spot -> spot.city().equals(city))
                    .filter(spot -> !spot.isRecoveryNode())
                    .forEach(result::add);
        }
        if (result.isEmpty()) {
            byCity.getOrDefault(city, List.of()).stream()
                    .filter(spot -> !spot.isRecoveryNode())
                    .forEach(result::add);
        }
        return result;
    }

    private List<ScenicSpot> loadSpotsFromDatabase() {
        String sql = """
                SELECT
                  s.id,
                  s.name,
                  s.city,
                  s.district,
                  s.address,
                  s.longitude,
                  s.latitude,
                  s.category,
                  s.ticket_price,
                  s.stay_duration,
                  s.open_time,
                  s.close_time,
                  s.popularity,
                  s.description,
                  s.is_recovery_node,
                  s.image_url,
                  COALESCE(f.history_score, 0) AS history_score,
                  COALESCE(f.food_score, 0) AS food_score,
                  COALESCE(f.nature_score, 0) AS nature_score,
                  COALESCE(f.shopping_score, 0) AS shopping_score,
                  COALESCE(f.leisure_score, 0) AS leisure_score,
                  COALESCE(f.photo_score, 0) AS photo_score,
                  COALESCE(f.physical_load, 0) AS physical_load,
                  COALESCE(f.cognitive_load, 0) AS cognitive_load,
                  COALESCE(f.crowd_load, 0) AS crowd_load,
                  COALESCE(f.queue_load, 0) AS queue_load,
                  COALESCE(f.recovery_value, 0) AS recovery_value,
                  f.tags
                FROM scenic_spot s
                LEFT JOIN scenic_feature f ON f.scenic_id = s.id
                ORDER BY s.id
                """;
        return jdbcTemplate.query(sql, this::mapSpot);
    }

    private ScenicSpot mapSpot(ResultSet rs, int rowNum) throws SQLException {
        double stayHours = Math.max(0.5, rs.getInt("stay_duration") / 60.0);
        List<String> tags = parseTags(rs.getString("tags"));
        if (tags.isEmpty()) {
            tags = inferTags(rs);
        }

        return new ScenicSpot(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("city"),
                nullToEmpty(rs.getString("district")),
                nullToEmpty(rs.getString("address")),
                rs.getDouble("longitude"),
                rs.getDouble("latitude"),
                nullToEmpty(rs.getString("category")),
                rs.getBigDecimal("ticket_price").intValue(),
                stayHours,
                formatTime(rs.getString("open_time"), "09:00"),
                formatTime(rs.getString("close_time"), "21:00"),
                rs.getBigDecimal("popularity").intValue(),
                tags,
                nullToEmpty(rs.getString("description")),
                blankToDefault(rs.getString("image_url"), FALLBACK_IMAGE),
                rs.getInt("is_recovery_node") == 1,
                rs.getDouble("physical_load"),
                rs.getDouble("cognitive_load"),
                rs.getDouble("crowd_load"),
                rs.getDouble("queue_load"),
                rs.getDouble("recovery_value")
        );
    }

    private int[][] loadTravelMinutesFromDatabase() {
        int size = byId.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
        int[][] matrix = new int[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (i == j) {
                    matrix[i][j] = 0;
                    continue;
                }
                ScenicSpot from = byId.get(i + 1);
                ScenicSpot to = byId.get(j + 1);
                matrix[i][j] = from == null || to == null ? 45 : estimateTravelMinutes(from, to);
            }
        }

        String sql = """
                SELECT from_spot_id, to_spot_id, travel_time
                FROM route_edge
                WHERE from_spot_id IS NOT NULL AND to_spot_id IS NOT NULL
                """;
        jdbcTemplate.query(sql, rs -> {
            int from = rs.getInt("from_spot_id");
            int to = rs.getInt("to_spot_id");
            int minutes = Math.max(1, rs.getInt("travel_time"));
            if (from > 0 && to > 0 && from <= size && to <= size) {
                matrix[from - 1][to - 1] = minutes;
                matrix[to - 1][from - 1] = Math.min(matrix[to - 1][from - 1], minutes);
            }
        });
        return matrix;
    }

    private void indexSpots() {
        for (ScenicSpot spot : spots) {
            byId.put(spot.id(), spot);
            byCity.computeIfAbsent(spot.city(), ignored -> new ArrayList<>()).add(spot);
            byCategory.computeIfAbsent(spot.category(), ignored -> new ArrayList<>()).add(spot);
            for (String tag : spot.tags()) {
                byTag.computeIfAbsent(tag, ignored -> new ArrayList<>()).add(spot);
            }
        }
    }

    private String searchableText(ScenicSpot spot) {
        return String.join(" ", spot.name(), spot.city(), spot.district(), spot.category(), spot.description(), String.join(" ", spot.tags()))
                .toLowerCase(Locale.ROOT);
    }

    private boolean isAll(String value) {
        return value == null || value.isBlank() || "all".equalsIgnoreCase(value.trim());
    }

    private List<String> parseTags(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split("[,，、\\s]+"))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private List<String> inferTags(ResultSet rs) throws SQLException {
        List<String> tags = new ArrayList<>();
        addTagIfPositive(tags, "历史", rs.getDouble("history_score"));
        addTagIfPositive(tags, "美食", rs.getDouble("food_score"));
        addTagIfPositive(tags, "自然", rs.getDouble("nature_score"));
        addTagIfPositive(tags, "购物", rs.getDouble("shopping_score"));
        addTagIfPositive(tags, "休闲", rs.getDouble("leisure_score"));
        addTagIfPositive(tags, "拍照", rs.getDouble("photo_score"));
        if (tags.isEmpty()) {
            tags.add("休闲");
        }
        return tags;
    }

    private void addTagIfPositive(List<String> tags, String tag, double score) {
        if (score >= 0.2) {
            tags.add(tag);
        }
    }

    private int estimateTravelMinutes(ScenicSpot from, ScenicSpot to) {
        double distanceKm = haversineKm(from.latitude(), from.longitude(), to.latitude(), to.longitude());
        return Math.max(8, (int) Math.round(distanceKm / 22.0 * 60 + 8));
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double radius = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return radius * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private String formatTime(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.length() >= 5 ? value.substring(0, 5) : value;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
