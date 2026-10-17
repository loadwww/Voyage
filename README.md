# Voyage · 智能旅行规划系统

基于 Spring Boot 3.3 / Java 21 / MySQL 的个性化行程规划全栈项目：自然语言需求解析 → 兴趣召回排序 → Floyd + TSP 动态规划路线优化 → 四态节奏引擎（疲劳干预）→ 景区/校园地图导航（Dijkstra + 多目标 TSP）→ 旅游日记（全文检索 + GZIP 压缩存储）。

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Spring Boot 3.3、Java 21、JdbcTemplate、MySQL 8 |
| 缓存/计数 | Caffeine（本地图缓存）、Redis 7（浏览量计数，可降级） |
| 可观测 | Actuator + Micrometer（缓存命中率、落库计数） |
| 接口文档 | springdoc-openapi（Swagger UI） |
| 前端 | 原生 HTML / CSS / JavaScript |
| 压测 | k6 |

## 核心算法

- **Floyd-Warshall** 全源最短路：景点通行时间矩阵启动时预计算一次（O(n³) 不再按请求重跑）。
- **TSP 状态压缩 DP**：`dp[mask][i]` 求每日景点最优访问顺序；多目标导航同款 DP 求途经点顺序。
- **四态节奏引擎**：RELAXED / STABLE / FATIGUED / OVERLOADED 状态机，按疲劳风险动态插入恢复节点。
- **Top-K 最小堆**：召回排序只维护 K 个候选，避免全量排序。
- **Dijkstra + 拥堵感知权重**：步行 / 单车 / 摆渡车多策略路径规划。

## 工程化改进（并发 / 缓存 / 健壮性）

1. **评分原子更新**：`rating` 由“读-改-写”改为单条原子 SQL（利用 MySQL SET 子句从左到右求值的语义），消除并发丢更新；`AtomicRatingConcurrencyTest` 可复现旧逻辑丢更新、CAS/原子 SQL 不丢。
2. **浏览量计数**：Redis `INCR` 缓冲 + Lua 原子“取走归零” + 定时批量落库，消除热点行锁竞争；Redis 不可用时自动降级为进程内 LongAdder 缓冲，停机前 flush，不丢计数。
3. **矩阵缓存**：通行时间矩阵与 Floyd 结果矩阵启动时各算一次，返回只读共享引用，去掉每请求 O(n²) 深拷贝。
4. **导航图缓存**：`GraphCacheService` 基于 Caffeine 按 areaId 缓存节点/边/邻接表（TTL + 手动失效），命中率通过 `/actuator/metrics/voyageGraphCacheTotal` 观测。
5. **健壮性**：写操作 `@Transactional`；`Idempotency-Key` 请求头幂等防重放；`@RestControllerAdvice` 统一异常与错误码；日记列表 offset 分页（旧 `limit` 调用保持兼容）。
6. **质量验证**：16 个单元测试（算法 + 并发 + 缓存契约），k6 压测脚本输出 QPS/P95/P99。

## 快速开始

```powershell
# 1. 起基础设施（MySQL + Redis）
docker compose up -d
# 导入数据：依次执行 database/schema.sql、database/seed.sql、database/upgrade_engineering.sql
#    以及 database/ 下的地图与日记脚本（见 database/README.md）

# 2. 构建并启动后端（默认 8081）
cd backend
mvn package -DskipTests
$env:MYSQL_PASSWORD="voyage123"   # 与 docker-compose 中一致
java -jar target/travel-system-0.0.1-SNAPSHOT.jar

# 3. 前端直接用浏览器打开 frontend/index.html
```

环境变量（均有默认值）：`SERVER_PORT`、`MYSQL_URL`、`MYSQL_USER`、`MYSQL_PASSWORD`、`REDIS_HOST`、`REDIS_PORT`。

- Swagger UI：http://localhost:8081/swagger-ui.html
- 健康检查：http://localhost:8081/actuator/health

## 压测

```powershell
k6 run --env BASE_URL=http://localhost:8081 --env AREA_ID=1 scripts/benchmark/plan-load.js
# 首轮后再跑一次，voyageGraphCacheTotal 中 miss 归零、只剩 hit
```

## 目录结构

```
backend/    Spring Boot 服务（algorithm / controller / service / repository / dto / model）
frontend/   原生前端页面
database/   建表、种子数据与升级脚本
scripts/    压测脚本
docker-compose.yml
```

## License

MIT
