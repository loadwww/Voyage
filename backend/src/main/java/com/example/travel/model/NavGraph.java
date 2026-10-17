package com.example.travel.model;

import java.util.List;
import java.util.Map;

/**
 * 某个区域（景区/校园）的内存导航图：节点表 + 边表 + 邻接表。
 * 由地图数据库表一次性构建，构建后只读，可被多线程共享并放入缓存。
 */
public record NavGraph(
        MapArea area,
        Map<Long, MapNode> nodes,
        Map<Long, MapEdge> edgeById,
        Map<Long, List<Step>> adjacency
) {
    public record Step(long toNodeId, MapEdge edge) {
    }
}
