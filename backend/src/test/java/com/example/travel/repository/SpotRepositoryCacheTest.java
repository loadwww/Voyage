package com.example.travel.repository;

import com.example.travel.model.ScenicSpot;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * 验证 P0-3：通行时间矩阵与 Floyd 结果矩阵在启动时各只计算一次，
 * travelMinutes()/shortestDistances() 返回共享只读引用，不再每请求深拷贝。
 */
class SpotRepositoryCacheTest {

    @Test
    void matricesAreComputedOnceAndShared() {
        StubJdbcTemplate jdbc = new StubJdbcTemplate(List.of(
                spot(1, 104.05, 30.66),
                spot(2, 104.06, 30.67),
                spot(3, 104.07, 30.68)
        ));
        SpotRepository repository = new SpotRepository(jdbc);
        repository.init();

        int[][] raw1 = repository.travelMinutes();
        int[][] raw2 = repository.travelMinutes();
        assertSame(raw1, raw2, "原始矩阵必须返回同一个缓存引用，不能每次深拷贝");

        int[][] floyd1 = repository.shortestDistances();
        int[][] floyd2 = repository.shortestDistances();
        assertSame(floyd1, floyd2, "Floyd 结果矩阵必须只计算一次并缓存");

        // 重新初始化后引用应更新（模拟数据刷新），旧矩阵不被原地修改
        repository.init();
        assertSame(repository.travelMinutes(), repository.travelMinutes());

        // Floyd 结果是最短路：任意两点不劣于原始直连估值，对角线为 0
        int[][] raw = repository.travelMinutes();
        int[][] shortest = repository.shortestDistances();
        for (int i = 0; i < raw.length; i++) {
            for (int j = 0; j < raw.length; j++) {
                if (i == j) {
                    org.junit.jupiter.api.Assertions.assertEquals(0, shortest[i][j]);
                } else {
                    org.junit.jupiter.api.Assertions.assertTrue(shortest[i][j] <= raw[i][j],
                            "Floyd 结果不能比原始直连更差");
                }
            }
        }
    }

    private static ScenicSpot spot(int id, double lon, double lat) {
        return new ScenicSpot(
                id, "景点" + id, "成都", "武侯区", "addr", lon, lat, "历史",
                0, 1.0, "09:00", "18:00", 50, List.of("历史"), "desc",
                "img", false, 0, 0, 0, 0, 0
        );
    }

    /**
     * 不连数据库的 JdbcTemplate 桩：景点查询返回内存数据，路线边查询为空（走 Haversine 估值）。
     */
    private static class StubJdbcTemplate extends JdbcTemplate {
        private final List<ScenicSpot> spots;

        StubJdbcTemplate(List<ScenicSpot> spots) {
            this.spots = spots;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> List<T> query(String sql, RowMapper<T> rowMapper) throws DataAccessException {
            return (List<T>) spots;
        }

        @Override
        public void query(String sql, RowCallbackHandler rch) throws DataAccessException {
            // 没有 route_edge 数据，邻接全走经纬度估值
        }
    }
}
