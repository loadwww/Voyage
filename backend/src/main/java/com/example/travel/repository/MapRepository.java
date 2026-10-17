package com.example.travel.repository;

import com.example.travel.model.MapArea;
import com.example.travel.model.MapEdge;
import com.example.travel.model.MapNode;
import com.example.travel.model.MapStats;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class MapRepository {
    private final JdbcTemplate jdbcTemplate;

    public MapRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<MapArea> areas(String type, String keyword, int limit) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT id, name, area_type, city, district, address, longitude, latitude, description
                FROM map_area
                WHERE 1 = 1
                """);
        if (type != null && !type.isBlank() && !"all".equalsIgnoreCase(type)) {
            sql.append(" AND area_type = ?");
            args.add(type.trim());
        }
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (name LIKE ? OR city LIKE ? OR district LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
            args.add(like);
        }
        sql.append(" ORDER BY id LIMIT ?");
        args.add(Math.max(1, Math.min(limit <= 0 ? 50 : limit, 500)));
        return jdbcTemplate.query(sql.toString(), this::mapArea, args.toArray());
    }

    public MapArea area(long id) {
        return jdbcTemplate.queryForObject("""
                SELECT id, name, area_type, city, district, address, longitude, latitude, description
                FROM map_area
                WHERE id = ?
                """, this::mapArea, id);
    }

    public List<MapNode> nodes(long areaId, String type, String category) {
        List<Object> args = new ArrayList<>();
        args.add(areaId);
        StringBuilder sql = new StringBuilder("""
                SELECT id, area_id, name, node_type, category, longitude, latitude,
                       floor_count, open_time, close_time, service_tags, description
                FROM map_node
                WHERE area_id = ?
                """);
        if (type != null && !type.isBlank() && !"all".equalsIgnoreCase(type)) {
            sql.append(" AND node_type = ?");
            args.add(type.trim());
        }
        if (category != null && !category.isBlank() && !"all".equalsIgnoreCase(category)) {
            sql.append(" AND category = ?");
            args.add(category.trim());
        }
        sql.append(" ORDER BY id");
        return jdbcTemplate.query(sql.toString(), this::mapNode, args.toArray());
    }

    public List<MapEdge> edges(long areaId) {
        return jdbcTemplate.query("""
                SELECT id, area_id, from_node_id, to_node_id, distance, travel_time, road_type, bidirectional
                FROM map_edge
                WHERE area_id = ?
                ORDER BY id
                """, this::mapEdge, areaId);
    }

    public MapStats stats() {
        int areaCount = count("SELECT COUNT(*) FROM map_area");
        int nodeCount = count("SELECT COUNT(*) FROM map_node");
        int edgeCount = count("SELECT COUNT(*) FROM map_edge");
        int buildingCount = count("SELECT COUNT(*) FROM map_node WHERE node_type IN ('building', 'scenic')");
        int facilityCount = count("SELECT COUNT(*) FROM map_node WHERE node_type = 'facility'");
        Map<String, Integer> facilityTypes = new LinkedHashMap<>();
        jdbcTemplate.query("""
                SELECT category, COUNT(*) AS total
                FROM map_node
                WHERE node_type = 'facility'
                GROUP BY category
                ORDER BY total DESC, category
                """, (RowCallbackHandler) rs -> facilityTypes.put(rs.getString("category"), rs.getInt("total")));
        return new MapStats(areaCount, nodeCount, edgeCount, buildingCount, facilityCount, facilityTypes.size(), facilityTypes);
    }

    private int count(String sql) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class);
        return value == null ? 0 : value;
    }

    private MapArea mapArea(ResultSet rs, int rowNum) throws SQLException {
        return new MapArea(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("area_type"),
                rs.getString("city"),
                rs.getString("district"),
                rs.getString("address"),
                rs.getDouble("longitude"),
                rs.getDouble("latitude"),
                rs.getString("description")
        );
    }

    private MapNode mapNode(ResultSet rs, int rowNum) throws SQLException {
        return new MapNode(
                rs.getLong("id"),
                rs.getLong("area_id"),
                rs.getString("name"),
                rs.getString("node_type"),
                rs.getString("category"),
                rs.getDouble("longitude"),
                rs.getDouble("latitude"),
                rs.getInt("floor_count"),
                formatTime(rs.getString("open_time")),
                formatTime(rs.getString("close_time")),
                rs.getString("service_tags"),
                rs.getString("description")
        );
    }

    private MapEdge mapEdge(ResultSet rs, int rowNum) throws SQLException {
        return new MapEdge(
                rs.getLong("id"),
                rs.getLong("area_id"),
                rs.getLong("from_node_id"),
                rs.getLong("to_node_id"),
                rs.getDouble("distance"),
                rs.getInt("travel_time"),
                rs.getString("road_type"),
                rs.getInt("bidirectional") == 1
        );
    }

    private String formatTime(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.length() >= 5 ? value.substring(0, 5) : value;
    }
}
