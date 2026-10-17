package com.example.travel.service;

import com.example.travel.model.IndoorPath;
import com.example.travel.model.AreaRecommendation;
import com.example.travel.model.FoodRecommendation;
import com.example.travel.model.MapArea;
import com.example.travel.model.MapEdge;
import com.example.travel.model.MapNode;
import com.example.travel.model.MapPath;
import com.example.travel.model.MapPathStep;
import com.example.travel.model.MultiStopPath;
import com.example.travel.model.NearbyFacility;
import com.example.travel.repository.MapRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

@Service
public class MapNavigationService {
    private static final double WALK_SPEED = 75.0;
    private static final double BICYCLE_SPEED = 180.0;
    private static final double ELECTRIC_CART_SPEED = 240.0;
    private static final int MAX_MULTI_TARGETS = 8;

    private final MapRepository mapRepository;

    public MapNavigationService(MapRepository mapRepository) {
        this.mapRepository = mapRepository;
    }

    public MapPath shortestPath(long areaId, long fromNodeId, long toNodeId, String strategy) {
        Graph graph = loadGraph(areaId);
        ensureNodeInArea(graph, fromNodeId);
        ensureNodeInArea(graph, toNodeId);
        return buildPath(graph, fromNodeId, toNodeId, normalizeStrategy(strategy));
    }

    public MultiStopPath multiStopPath(long areaId, long startNodeId, List<Long> targetNodeIds, String strategy) {
        Graph graph = loadGraph(areaId);
        ensureNodeInArea(graph, startNodeId);

        String normalizedStrategy = normalizeStrategy(strategy);
        List<Long> targets = new ArrayList<>();
        for (Long id : targetNodeIds) {
            if (id != null && id != startNodeId && !targets.contains(id)) {
                targets.add(id);
            }
        }
        if (targets.isEmpty()) {
            throw new IllegalArgumentException("At least one target node is required.");
        }
        if (targets.size() > MAX_MULTI_TARGETS) {
            throw new IllegalArgumentException("Multi-stop planning supports at most " + MAX_MULTI_TARGETS + " targets.");
        }
        targets.forEach(id -> ensureNodeInArea(graph, id));

        List<Long> keyNodes = new ArrayList<>();
        keyNodes.add(startNodeId);
        keyNodes.addAll(targets);

        MapPath[][] pairPaths = new MapPath[keyNodes.size()][keyNodes.size()];
        for (int i = 0; i < keyNodes.size(); i += 1) {
            for (int j = 0; j < keyNodes.size(); j += 1) {
                if (i != j) {
                    pairPaths[i][j] = buildPath(graph, keyNodes.get(i), keyNodes.get(j), normalizedStrategy);
                }
            }
        }

        List<Integer> targetOrder = bestTargetOrder(pairPaths, normalizedStrategy, targets.size());
        List<Long> visitOrder = new ArrayList<>();
        visitOrder.add(startNodeId);
        for (int index : targetOrder) {
            visitOrder.add(targets.get(index));
        }
        visitOrder.add(startNodeId);

        List<MapNode> nodes = new ArrayList<>();
        List<MapEdge> edges = new ArrayList<>();
        List<MapPathStep> steps = new ArrayList<>();
        for (int i = 1; i < visitOrder.size(); i += 1) {
            int fromIndex = keyNodes.indexOf(visitOrder.get(i - 1));
            int toIndex = keyNodes.indexOf(visitOrder.get(i));
            MapPath segment = pairPaths[fromIndex][toIndex];
            if (nodes.isEmpty()) {
                nodes.addAll(segment.nodes());
            } else {
                nodes.addAll(segment.nodes().stream().skip(1).toList());
            }
            edges.addAll(segment.edges());
            steps.addAll(segment.steps());
        }

        return new MultiStopPath(
                areaId,
                startNodeId,
                targets,
                normalizedStrategy,
                visitOrder,
                round(edges.stream().mapToDouble(MapEdge::distance).sum()),
                steps.stream().mapToInt(MapPathStep::travelTime).sum(),
                nodes,
                edges,
                steps
        );
    }

    public IndoorPath indoorPath(long areaId, long buildingNodeId, String fromRoom, String toRoom) {
        Graph graph = loadGraph(areaId);
        ensureNodeInArea(graph, buildingNodeId);
        MapNode building = graph.nodes.get(buildingNodeId);
        int fromFloor = clampFloor(parseFloor(fromRoom), building.floorCount());
        int toFloor = clampFloor(parseFloor(toRoom), building.floorCount());

        List<String> steps = new ArrayList<>();
        steps.add("Enter the building lobby from the main gate.");
        steps.add("Walk from the lobby to the elevator entrance, about 1 minute.");
        if (fromFloor != toFloor) {
            steps.add("Take the elevator from floor " + fromFloor + " to floor " + toFloor + ".");
        } else {
            steps.add("Stay on the same floor and skip the elevator.");
        }
        steps.add("Leave the elevator and follow the corridor to room " + safeRoom(toRoom) + ".");
        steps.add("Indoor demo path: gate -> elevator -> floor -> room.");

        int totalTime = 1 + Math.max(1, Math.abs(toFloor - fromFloor)) + 2;
        return new IndoorPath(areaId, buildingNodeId, building.name(), safeRoom(fromRoom), safeRoom(toRoom), totalTime, steps);
    }

    public List<NearbyFacility> nearbyFacilities(long areaId, long fromNodeId, String category, int limit, double radiusMeters) {
        Graph graph = loadGraph(areaId);
        ensureNodeInArea(graph, fromNodeId);
        SearchResult distanceResult = dijkstra(graph, fromNodeId, "distance");
        SearchResult timeResult = dijkstra(graph, fromNodeId, "time");

        int safeLimit = Math.max(1, Math.min(limit <= 0 ? 10 : limit, 50));
        double safeRadius = radiusMeters <= 0 ? Double.MAX_VALUE : radiusMeters;
        String categoryQuery = normalizeText(category);
        return graph.nodes.values().stream()
                .filter(node -> "facility".equals(node.nodeType()))
                .filter(node -> categoryQuery.isBlank()
                        || "all".equals(categoryQuery)
                        || normalizeText(node.category()).contains(categoryQuery)
                        || normalizeText(node.name()).contains(categoryQuery)
                        || normalizeText(node.serviceTags()).contains(categoryQuery))
                .filter(node -> distanceResult.distance.containsKey(node.id()))
                .map(node -> new NearbyFacility(
                        node,
                        round(distanceResult.distance.get(node.id())),
                        (int) Math.round(timeResult.distance.getOrDefault(node.id(), distanceResult.distance.get(node.id()) / WALK_SPEED)),
                        node.category(),
                        "Sorted by shortest road distance inside the area."
                ))
                .filter(item -> item.roadDistance() <= safeRadius)
                .sorted(Comparator.comparingDouble(NearbyFacility::roadDistance).thenComparing(item -> item.node().id()))
                .limit(safeLimit)
                .toList();
    }

    public List<AreaRecommendation> recommendAreas(String type, String keyword, String interest, String sort, int limit) {
        List<MapArea> areas = mapRepository.areas(type, keyword, 500);
        Comparator<AreaRecommendation> bestFirst = areaComparator(sort);
        PriorityQueue<AreaRecommendation> topK = new PriorityQueue<>(bestFirst.reversed());
        int safeLimit = Math.max(1, Math.min(limit <= 0 ? 10 : limit, 30));

        for (MapArea area : areas) {
            AreaRecommendation item = buildAreaRecommendation(area, keyword, interest, sort);
            if (topK.size() < safeLimit) {
                topK.add(item);
            } else if (bestFirst.compare(item, topK.peek()) < 0) {
                topK.poll();
                topK.add(item);
            }
        }

        List<AreaRecommendation> result = new ArrayList<>(topK);
        result.sort(bestFirst);
        return result;
    }

    public List<FoodRecommendation> recommendFoods(
            long areaId,
            long fromNodeId,
            String cuisine,
            String keyword,
            String sort,
            int limit
    ) {
        Graph graph = loadGraph(areaId);
        ensureNodeInArea(graph, fromNodeId);
        SearchResult distanceResult = dijkstra(graph, fromNodeId, "distance");
        SearchResult timeResult = dijkstra(graph, fromNodeId, "time");
        Comparator<FoodRecommendation> bestFirst = foodComparator(sort);
        PriorityQueue<FoodRecommendation> topK = new PriorityQueue<>(bestFirst.reversed());
        int safeLimit = Math.max(1, Math.min(limit <= 0 ? 10 : limit, 30));

        for (MapNode node : graph.nodes.values()) {
            if (!isFoodFacility(node) || !distanceResult.distance.containsKey(node.id())) {
                continue;
            }
            FoodRecommendation item = buildFoodRecommendation(node, distanceResult, timeResult, cuisine, keyword, sort);
            if (item == null) {
                continue;
            }
            if (topK.size() < safeLimit) {
                topK.add(item);
            } else if (bestFirst.compare(item, topK.peek()) < 0) {
                topK.poll();
                topK.add(item);
            }
        }

        List<FoodRecommendation> result = new ArrayList<>(topK);
        result.sort(bestFirst);
        return result;
    }

    private AreaRecommendation buildAreaRecommendation(MapArea area, String keyword, String interest, String sort) {
        int popularity = 58 + (int) ((area.id() * 19) % 41);
        double rating = round(3.8 + ((area.id() * 11) % 12) / 10.0);
        int matchScore = areaMatchScore(area, keyword, interest);
        return new AreaRecommendation(
                area,
                "campus".equalsIgnoreCase(area.areaType()) ? "校园" : "景区",
                Math.min(99, popularity),
                Math.min(5.0, rating),
                matchScore,
                areaSortReason(sort)
        );
    }

    private Comparator<AreaRecommendation> areaComparator(String sort) {
        String normalized = sort == null ? "hot" : sort.trim().toLowerCase(Locale.ROOT);
        if ("rating".equals(normalized)) {
            return Comparator.comparingDouble(AreaRecommendation::rating).reversed()
                    .thenComparing(Comparator.comparingInt(AreaRecommendation::matchScore).reversed())
                    .thenComparing(Comparator.comparingInt(AreaRecommendation::popularity).reversed())
                    .thenComparing(item -> item.area().id());
        }
        if ("match".equals(normalized)) {
            return Comparator.comparingInt(AreaRecommendation::matchScore).reversed()
                    .thenComparing(Comparator.comparingInt(AreaRecommendation::popularity).reversed())
                    .thenComparing(Comparator.comparingDouble(AreaRecommendation::rating).reversed())
                    .thenComparing(item -> item.area().id());
        }
        return Comparator.comparingInt(AreaRecommendation::popularity).reversed()
                .thenComparing(Comparator.comparingDouble(AreaRecommendation::rating).reversed())
                .thenComparing(Comparator.comparingInt(AreaRecommendation::matchScore).reversed())
                .thenComparing(item -> item.area().id());
    }

    private int areaMatchScore(MapArea area, String keyword, String interest) {
        String query = normalizeText((keyword == null ? "" : keyword) + " " + (interest == null ? "" : interest));
        if (query.isBlank()) {
            return 60 + (int) ((area.id() * 7) % 30);
        }
        String text = normalizeText(area.name() + " " + area.areaType() + " " + area.city() + " " + area.district() + " " + area.address() + " " + area.description());
        int score = 40;
        for (String token : query.split("\\s+")) {
            if (!token.isBlank() && text.contains(token)) {
                score += 30;
            }
        }
        return Math.min(100, score);
    }

    private String areaSortReason(String sort) {
        String normalized = sort == null ? "hot" : sort.trim().toLowerCase(Locale.ROOT);
        if ("rating".equals(normalized)) {
            return "Top-K 按评分维护前 10 个候选";
        }
        if ("match".equals(normalized)) {
            return "Top-K 按兴趣匹配维护前 10 个候选";
        }
        return "Top-K 按热度维护前 10 个候选";
    }

    private MapPath buildPath(Graph graph, long fromNodeId, long toNodeId, String strategy) {
        SearchResult result = dijkstra(graph, fromNodeId, strategy);
        if (!result.distance.containsKey(toNodeId)) {
            throw new IllegalArgumentException("Target node is unreachable.");
        }
        List<Long> nodeIds = restoreNodeIds(fromNodeId, toNodeId, result.previousNode);
        List<MapNode> pathNodes = nodeIds.stream().map(graph.nodes::get).toList();
        List<MapEdge> pathEdges = restoreEdges(nodeIds, result.previousEdge, graph.edgeById);
        List<MapPathStep> pathSteps = buildSteps(graph, nodeIds, pathEdges, strategy);
        return new MapPath(
                graph.area.id(),
                fromNodeId,
                toNodeId,
                strategy,
                round(pathEdges.stream().mapToDouble(MapEdge::distance).sum()),
                pathSteps.stream().mapToInt(MapPathStep::travelTime).sum(),
                pathNodes,
                pathEdges,
                pathSteps
        );
    }

    private List<Integer> bestTargetOrder(MapPath[][] pairPaths, String strategy, int targetCount) {
        int masks = 1 << targetCount;
        double[][] dp = new double[masks][targetCount];
        int[][] parent = new int[masks][targetCount];
        for (int mask = 0; mask < masks; mask += 1) {
            for (int i = 0; i < targetCount; i += 1) {
                dp[mask][i] = Double.MAX_VALUE;
                parent[mask][i] = -1;
            }
        }
        for (int i = 0; i < targetCount; i += 1) {
            dp[1 << i][i] = pathCost(pairPaths[0][i + 1], strategy);
        }
        for (int mask = 0; mask < masks; mask += 1) {
            for (int last = 0; last < targetCount; last += 1) {
                if (dp[mask][last] == Double.MAX_VALUE) {
                    continue;
                }
                for (int next = 0; next < targetCount; next += 1) {
                    if ((mask & (1 << next)) != 0) {
                        continue;
                    }
                    int nextMask = mask | (1 << next);
                    double candidate = dp[mask][last] + pathCost(pairPaths[last + 1][next + 1], strategy);
                    if (candidate < dp[nextMask][next]) {
                        dp[nextMask][next] = candidate;
                        parent[nextMask][next] = last;
                    }
                }
            }
        }

        int full = masks - 1;
        int bestLast = 0;
        double best = Double.MAX_VALUE;
        for (int last = 0; last < targetCount; last += 1) {
            double candidate = dp[full][last] + pathCost(pairPaths[last + 1][0], strategy);
            if (candidate < best) {
                best = candidate;
                bestLast = last;
            }
        }

        List<Integer> reversed = new ArrayList<>();
        int mask = full;
        int current = bestLast;
        while (current >= 0) {
            reversed.add(current);
            int previous = parent[mask][current];
            mask ^= 1 << current;
            current = previous;
        }
        List<Integer> ordered = new ArrayList<>();
        for (int i = reversed.size() - 1; i >= 0; i -= 1) {
            ordered.add(reversed.get(i));
        }
        return ordered;
    }

    private double pathCost(MapPath path, String strategy) {
        return "distance".equals(strategy) ? path.totalDistance() : path.totalTime();
    }

    private FoodRecommendation buildFoodRecommendation(
            MapNode node,
            SearchResult distanceResult,
            SearchResult timeResult,
            String cuisineFilter,
            String keyword,
            String sort
    ) {
        String cuisine = cuisineFor(node);
        String restaurantName = node.name();
        String windowName = windowNameFor(node);
        String dish = signatureDishFor(cuisine, node.id());
        if (!matchesCuisine(cuisine, cuisineFilter)) {
            return null;
        }
        int matchScore = fuzzyMatchScore(node, cuisine, windowName, dish, keyword);
        if (matchScore < 0) {
            return null;
        }
        double distance = round(distanceResult.distance.get(node.id()));
        int travelTime = (int) Math.round(timeResult.distance.getOrDefault(node.id(), distance / WALK_SPEED));
        int popularity = 60 + (int) ((node.id() * 17) % 38);
        double rating = round(3.7 + ((node.id() * 13) % 14) / 10.0);
        return new FoodRecommendation(
                node,
                cuisine,
                restaurantName,
                windowName,
                dish,
                Math.min(99, popularity),
                Math.min(5.0, rating),
                distance,
                travelTime,
                matchScore,
                sortReason(sort)
        );
    }

    private Comparator<FoodRecommendation> foodComparator(String sort) {
        String normalized = sort == null ? "hot" : sort.trim().toLowerCase(Locale.ROOT);
        if ("distance".equals(normalized)) {
            return Comparator.comparingDouble(FoodRecommendation::roadDistance)
                    .thenComparing(Comparator.comparingDouble(FoodRecommendation::rating).reversed())
                    .thenComparing(Comparator.comparingInt(FoodRecommendation::popularity).reversed())
                    .thenComparing(item -> item.node().id());
        }
        if ("rating".equals(normalized)) {
            return Comparator.comparingDouble(FoodRecommendation::rating).reversed()
                    .thenComparing(Comparator.comparingInt(FoodRecommendation::popularity).reversed())
                    .thenComparingDouble(FoodRecommendation::roadDistance)
                    .thenComparing(item -> item.node().id());
        }
        return Comparator.comparingInt(FoodRecommendation::popularity).reversed()
                .thenComparing(Comparator.comparingDouble(FoodRecommendation::rating).reversed())
                .thenComparingDouble(FoodRecommendation::roadDistance)
                .thenComparing(item -> item.node().id());
    }

    private String sortReason(String sort) {
        String normalized = sort == null ? "hot" : sort.trim().toLowerCase(Locale.ROOT);
        if ("distance".equals(normalized)) {
            return "top-k heap by road distance";
        }
        if ("rating".equals(normalized)) {
            return "top-k heap by rating";
        }
        return "top-k heap by popularity";
    }

    private boolean isFoodFacility(MapNode node) {
        if (!"facility".equals(node.nodeType())) {
            return false;
        }
        String text = normalizeText(node.name() + " " + node.category() + " " + node.serviceTags() + " " + node.description());
        return text.contains("\u996d\u5e97")
                || text.contains("\u98df\u5802")
                || text.contains("\u5496\u5561")
                || text.contains("\u4fbf\u5229\u5e97")
                || text.contains("\u8d85\u5e02")
                || text.contains("\u5546\u5e97")
                || text.contains("\u5c0f\u5403")
                || text.contains("\u9910")
                || text.contains("\u00e9\u00a5")
                || text.contains("\u00e9\u00a3")
                || text.contains("\u00e5\u2019")
                || text.contains("\u00e4\u00be")
                || text.contains("\u00e8\u00b6")
                || text.contains("\u00e5\u2022");
    }

    private boolean matchesCuisine(String cuisine, String cuisineFilter) {
        String filter = normalizeText(cuisineFilter);
        return filter.isBlank() || "all".equals(filter) || normalizeText(cuisine).contains(filter);
    }

    private int fuzzyMatchScore(MapNode node, String cuisine, String windowName, String dish, String keyword) {
        String query = normalizeText(keyword);
        if (query.isBlank()) {
            return 0;
        }
        List<String> fields = List.of(node.name(), node.category(), node.serviceTags(), node.description(), cuisine, windowName, dish);
        int best = Integer.MAX_VALUE;
        for (String field : fields) {
            String value = normalizeText(field);
            if (value.contains(query)) {
                return 100;
            }
            best = Math.min(best, levenshtein(value, query));
        }
        int tolerance = Math.max(1, Math.min(3, query.length() / 3 + 1));
        return best <= tolerance ? 80 - best : -1;
    }

    private String cuisineFor(MapNode node) {
        String category = normalizeText(node.category() + " " + node.name());
        if (category.contains("\u5496\u5561") || category.contains("\u00e5\u2019")) {
            return "\u8f7b\u98df\u5496\u5561";
        }
        if (category.contains("\u98df\u5802") || category.contains("\u00e9\u00a3")) {
            String[] campus = {"\u5ddd\u83dc", "\u9762\u98df", "\u76d6\u996d", "\u7ca4\u83dc", "\u7d20\u98df"};
            return campus[(int) (node.id() % campus.length)];
        }
        String[] cuisines = {"\u5ddd\u83dc", "\u706b\u9505", "\u5c0f\u5403", "\u7ca4\u83dc", "\u9762\u98df", "\u8f7b\u98df"};
        return cuisines[(int) (node.id() % cuisines.length)];
    }

    private String windowNameFor(MapNode node) {
        if (normalizeText(node.category()).contains("\u98df\u5802")) {
            char zone = (char) ('A' + (int) (node.id() % 5));
            return zone + "-window-" + (node.id() % 12 + 1);
        }
        return "shop-" + (node.id() % 20 + 1);
    }

    private String signatureDishFor(String cuisine, long id) {
        Map<String, String[]> dishes = Map.of(
                "\u5ddd\u83dc", new String[]{"\u9ebb\u5a46\u8c46\u8150", "\u5bab\u4fdd\u9e21\u4e01", "\u6c34\u716e\u725b\u8089"},
                "\u706b\u9505", new String[]{"\u9e33\u9e2f\u9505", "\u6bdb\u809a", "\u9ec4\u5589"},
                "\u5c0f\u5403", new String[]{"\u949f\u6c34\u997a", "\u62c5\u62c5\u9762", "\u51b0\u7c89"},
                "\u7ca4\u83dc", new String[]{"\u70e7\u9e45\u996d", "\u80a0\u7c89", "\u867e\u997a"},
                "\u9762\u98df", new String[]{"\u725b\u8089\u9762", "\u6742\u9171\u9762", "\u62cc\u9762"},
                "\u8f7b\u98df", new String[]{"\u6c99\u62c9", "\u4e09\u660e\u6cbb", "\u996d\u56e2"},
                "\u8f7b\u98df\u5496\u5561", new String[]{"\u62ff\u94c1", "\u9999\u8349\u62ff\u94c1", "\u8d1d\u679c"}
        );
        String[] selected = dishes.getOrDefault(cuisine, dishes.get("\u5c0f\u5403"));
        return selected[(int) (id % selected.length)];
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private int levenshtein(String source, String target) {
        if (source.isBlank()) {
            return target.length();
        }
        if (target.isBlank()) {
            return 0;
        }
        int[] previous = new int[target.length() + 1];
        int[] current = new int[target.length() + 1];
        for (int j = 0; j <= target.length(); j += 1) {
            previous[j] = j;
        }
        for (int i = 1; i <= source.length(); i += 1) {
            current[0] = i;
            for (int j = 1; j <= target.length(); j += 1) {
                int cost = source.charAt(i - 1) == target.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(
                        Math.min(current[j - 1] + 1, previous[j] + 1),
                        previous[j - 1] + cost
                );
            }
            int[] temp = previous;
            previous = current;
            current = temp;
        }
        return previous[target.length()];
    }

    private Graph loadGraph(long areaId) {
        MapArea area = mapRepository.area(areaId);
        List<MapNode> nodes = mapRepository.nodes(areaId, null, null);
        List<MapEdge> edges = mapRepository.edges(areaId);
        Map<Long, MapNode> nodeMap = new HashMap<>();
        for (MapNode node : nodes) {
            nodeMap.put(node.id(), node);
        }
        Map<Long, MapEdge> edgeById = new HashMap<>();
        Map<Long, List<Step>> adjacency = new HashMap<>();
        for (MapEdge edge : edges) {
            edgeById.put(edge.id(), edge);
            adjacency.computeIfAbsent(edge.fromNodeId(), ignored -> new ArrayList<>()).add(new Step(edge.toNodeId(), edge));
            if (edge.bidirectional()) {
                adjacency.computeIfAbsent(edge.toNodeId(), ignored -> new ArrayList<>()).add(new Step(edge.fromNodeId(), edge));
            }
        }
        return new Graph(area, nodeMap, edgeById, adjacency);
    }

    private SearchResult dijkstra(Graph graph, long startNodeId, String strategy) {
        Map<Long, Double> distance = new HashMap<>();
        Map<Long, Long> previousNode = new HashMap<>();
        Map<Long, Long> previousEdge = new HashMap<>();
        Set<Long> visited = new HashSet<>();
        PriorityQueue<State> queue = new PriorityQueue<>(Comparator.comparingDouble(State::distance));
        distance.put(startNodeId, 0.0);
        queue.add(new State(startNodeId, 0.0));

        while (!queue.isEmpty()) {
            State current = queue.poll();
            if (!visited.add(current.nodeId())) {
                continue;
            }
            for (Step step : graph.adjacency.getOrDefault(current.nodeId(), List.of())) {
                double nextDistance = current.distance() + edgeWeight(graph.area.areaType(), step.edge(), strategy);
                double known = distance.getOrDefault(step.toNodeId(), Double.MAX_VALUE);
                if (nextDistance < known) {
                    distance.put(step.toNodeId(), nextDistance);
                    previousNode.put(step.toNodeId(), current.nodeId());
                    previousEdge.put(step.toNodeId(), step.edge().id());
                    queue.add(new State(step.toNodeId(), nextDistance));
                }
            }
        }
        return new SearchResult(distance, previousNode, previousEdge);
    }

    private List<MapPathStep> buildSteps(Graph graph, List<Long> nodeIds, List<MapEdge> edges, String strategy) {
        List<MapPathStep> steps = new ArrayList<>();
        for (int i = 0; i < edges.size(); i += 1) {
            MapEdge edge = edges.get(i);
            MapNode from = graph.nodes.get(nodeIds.get(i));
            MapNode to = graph.nodes.get(nodeIds.get(i + 1));
            TransportChoice choice = chooseTransport(graph.area.areaType(), edge, strategy);
            steps.add(new MapPathStep(
                    from.id(),
                    to.id(),
                    from.name(),
                    to.name(),
                    edge.roadType(),
                    choice.mode(),
                    round(edge.distance()),
                    choice.time(),
                    choice.congestion(),
                    stepNote(strategy, choice.mode(), choice.congestion())
            ));
        }
        return steps;
    }

    private double edgeWeight(String areaType, MapEdge edge, String strategy) {
        if ("distance".equals(strategy)) {
            return edge.distance();
        }
        return chooseTransport(areaType, edge, strategy).time();
    }

    private TransportChoice chooseTransport(String areaType, MapEdge edge, String strategy) {
        double congestion = congestion(edge);
        if ("transport".equals(strategy)) {
            TransportChoice walk = travelTime("walk", edge.distance(), WALK_SPEED, congestion);
            TransportChoice vehicle = null;
            if ("campus".equalsIgnoreCase(areaType) && bicycleAllowed(edge.roadType())) {
                vehicle = travelTime("bicycle", edge.distance(), BICYCLE_SPEED, congestion);
            }
            if (!"campus".equalsIgnoreCase(areaType) && electricCartAllowed(edge.roadType())) {
                vehicle = travelTime("electric_cart", edge.distance(), ELECTRIC_CART_SPEED, congestion);
            }
            return vehicle != null && vehicle.time() < walk.time() ? vehicle : walk;
        }
        return travelTime("walk", edge.distance(), WALK_SPEED, congestion);
    }

    private TransportChoice travelTime(String mode, double distance, double idealSpeed, double congestion) {
        int minutes = Math.max(1, (int) Math.ceil(distance / Math.max(1.0, idealSpeed * congestion)));
        return new TransportChoice(mode, minutes, round(congestion));
    }

    private boolean bicycleAllowed(String roadType) {
        return Set.of("main_road", "greenway", "covered_walkway").contains(safeRoadType(roadType));
    }

    private boolean electricCartAllowed(String roadType) {
        return Set.of("main_road", "greenway").contains(safeRoadType(roadType));
    }

    private String safeRoadType(String roadType) {
        return roadType == null ? "" : roadType.trim().toLowerCase(Locale.ROOT);
    }

    private double congestion(MapEdge edge) {
        double base = 0.58 + ((edge.id() * 37) % 38) / 100.0;
        String roadType = safeRoadType(edge.roadType());
        if ("stairs".equals(roadType)) {
            base -= 0.08;
        }
        if ("main_road".equals(roadType)) {
            base += 0.04;
        }
        return Math.max(0.35, Math.min(1.0, base));
    }

    private String stepNote(String strategy, String mode, double congestion) {
        if ("distance".equals(strategy)) {
            return "shortest road distance";
        }
        if ("transport".equals(strategy)) {
            return mode + " selected with congestion-aware travel time";
        }
        return "real speed = congestion x ideal speed";
    }

    private List<Long> restoreNodeIds(long fromNodeId, long toNodeId, Map<Long, Long> previousNode) {
        List<Long> reversed = new ArrayList<>();
        long current = toNodeId;
        reversed.add(current);
        while (current != fromNodeId) {
            Long previous = previousNode.get(current);
            if (previous == null) {
                throw new IllegalArgumentException("Target node is unreachable.");
            }
            current = previous;
            reversed.add(current);
        }
        List<Long> ordered = new ArrayList<>();
        for (int i = reversed.size() - 1; i >= 0; i -= 1) {
            ordered.add(reversed.get(i));
        }
        return ordered;
    }

    private List<MapEdge> restoreEdges(List<Long> nodeIds, Map<Long, Long> previousEdge, Map<Long, MapEdge> edgeById) {
        List<MapEdge> edges = new ArrayList<>();
        for (int i = 1; i < nodeIds.size(); i += 1) {
            Long edgeId = previousEdge.get(nodeIds.get(i));
            if (edgeId != null && edgeById.containsKey(edgeId)) {
                edges.add(edgeById.get(edgeId));
            }
        }
        return edges;
    }

    private void ensureNodeInArea(Graph graph, long nodeId) {
        if (!graph.nodes.containsKey(nodeId)) {
            throw new IllegalArgumentException("Node does not exist in current area: " + nodeId);
        }
    }

    private String normalizeStrategy(String strategy) {
        if (strategy == null || strategy.isBlank()) {
            return "distance";
        }
        String normalized = strategy.trim().toLowerCase(Locale.ROOT);
        if ("time".equals(normalized) || "transport".equals(normalized)) {
            return normalized;
        }
        return "distance";
    }

    private int parseFloor(String room) {
        if (room == null || room.isBlank()) {
            return 1;
        }
        String digits = room.replaceAll("\\D+", "");
        if (digits.isBlank()) {
            return 1;
        }
        return Math.max(1, Character.digit(digits.charAt(0), 10));
    }

    private int clampFloor(int floor, int floorCount) {
        return Math.min(Math.max(1, floor), Math.max(1, floorCount));
    }

    private String safeRoom(String value) {
        return value == null || value.isBlank() ? "entrance" : value.trim();
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private record Graph(MapArea area, Map<Long, MapNode> nodes, Map<Long, MapEdge> edgeById, Map<Long, List<Step>> adjacency) {
    }

    private record Step(long toNodeId, MapEdge edge) {
    }

    private record State(long nodeId, double distance) {
    }

    private record SearchResult(Map<Long, Double> distance, Map<Long, Long> previousNode, Map<Long, Long> previousEdge) {
    }

    private record TransportChoice(String mode, int time, double congestion) {
    }
}
