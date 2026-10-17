package com.example.travel.service;

import com.example.travel.model.MapArea;
import com.example.travel.model.MapEdge;
import com.example.travel.model.MapNode;
import com.example.travel.model.NavGraph;
import com.example.travel.repository.MapRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 按 areaId 缓存区域导航图。
 * <p>改造前：MapNavigationService 的 5 个入口每次请求都查 3 次库（area/nodes/edges）并重建邻接表；
 * 改造后：同一区域只构建一次，后续请求直接命中内存，TTL 到期或手动失效后重建。
 * <p>景点/地图数据是低频变更的静态数据，选择本地缓存（Caffeine）而非分布式缓存：
 * 单实例足够、零网络开销；多实例部署时各实例持有相同的缓存副本，天然最终一致。
 */
@Service
public class GraphCacheService {
    private static final Logger log = LoggerFactory.getLogger(GraphCacheService.class);

    private final MapRepository mapRepository;
    private final MeterRegistry meterRegistry;
    private final Cache<Long, NavGraph> graphCache;

    public GraphCacheService(
            MapRepository mapRepository,
            ObjectProvider<MeterRegistry> meterRegistryProvider,
            @Value("${voyage.cache.graph.ttl-minutes:10}") long ttlMinutes,
            @Value("${voyage.cache.graph.max-size:200}") long maxSize
    ) {
        this.mapRepository = mapRepository;
        this.meterRegistry = meterRegistryProvider.getIfAvailable();
        this.graphCache = Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofMinutes(ttlMinutes))
                .maximumSize(maxSize)
                .recordStats()
                .build();
    }

    /**
     * 获取区域导航图：命中缓存直接返回，未命中则查库构建并回填。
     */
    public NavGraph getGraph(long areaId) {
        NavGraph cached = graphCache.getIfPresent(areaId);
        if (cached != null) {
            recordCacheResult("hit");
            return cached;
        }
        recordCacheResult("miss");
        NavGraph graph = graphCache.get(areaId, this::loadGraph);
        log.debug("NavGraph loaded for areaId={}, nodes={}", areaId, graph.nodes().size());
        return graph;
    }

    /**
     * 地图数据变更后手动失效（管理端调用）。
     */
    public void invalidate(long areaId) {
        graphCache.invalidate(areaId);
    }

    private NavGraph loadGraph(long areaId) {
        MapArea area = mapRepository.area(areaId);
        List<MapNode> nodes = mapRepository.nodes(areaId, null, null);
        List<MapEdge> edges = mapRepository.edges(areaId);

        Map<Long, MapNode> nodeMap = new HashMap<>(nodes.size() * 2);
        for (MapNode node : nodes) {
            nodeMap.put(node.id(), node);
        }
        Map<Long, MapEdge> edgeById = new HashMap<>(edges.size() * 2);
        Map<Long, List<NavGraph.Step>> adjacency = new HashMap<>();
        for (MapEdge edge : edges) {
            edgeById.put(edge.id(), edge);
            adjacency.computeIfAbsent(edge.fromNodeId(), ignored -> new ArrayList<>())
                    .add(new NavGraph.Step(edge.toNodeId(), edge));
            if (edge.bidirectional()) {
                adjacency.computeIfAbsent(edge.toNodeId(), ignored -> new ArrayList<>())
                        .add(new NavGraph.Step(edge.fromNodeId(), edge));
            }
        }
        return new NavGraph(area, nodeMap, edgeById, adjacency);
    }

    private void recordCacheResult(String result) {
        if (meterRegistry != null) {
            meterRegistry.counter("voyage_graph_cache_total", "result", result).increment();
        }
    }
}
