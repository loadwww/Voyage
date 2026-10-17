# 个性化旅游系统数据库说明

本目录先生成数据库结构和示例数据，不改后端接库逻辑。当前后端仍可继续使用内存数据，后续接 MySQL 时可以按这些表做 Entity / Mapper / Repository。

## 文件

- `schema.sql`：数据库、表、主键、外键、索引。
- `seed.sql`：成都主城区核心旅游圈示例数据。
- `amap_fetch_generate.mjs`：调用高德 Web 服务 API 拉取真实 POI，并生成可导入 SQL。
- `map_schema.sql`：景区/校园内部地图扩展表，可单独导入，不清空已有旅游数据。
- `generate_area_map_data.mjs`：批量生成景区/校园、内部建筑物、服务设施和道路边数据。
- `diary_schema.sql`：旅游日记扩展表，可单独导入。
- `diary_seed.sql`：旅游日记演示数据。
- `generate_diary_seed.mjs`：批量生成更多旅游日记，便于演示搜索、排序和推荐。
- `generated/`：脚本输出目录。

## 覆盖范围

示例数据以成都主城区为范围，包含：

- 武侯区、青羊区、锦江区、成华区等核心旅游片区；
- 30 个景点/中途休息点；
- 其中 `is_recovery_node = 1` 的地点可作为中途休息点；
- 景点之间核心路径关系；
- 1 条用户需求、1 个示例方案、3 天路线详情。

验收扩展数据包含：

- 200 个景区/校园区域；
- 每个区域 20 个核心建筑物/景点；
- 每个区域 50 个服务设施；
- 每个区域约 100 条内部道路边；
- 服务设施类型不少于 10 种。

## 核心表

- `scenic_spot`：景点与中途休息点基础信息。
- `scenic_feature`：兴趣分、强度压力、排队压力、放松价值。
- `route_edge`：景点之间距离、时间、费用和交通方式。
- `user_demand`：用户输入和旅行风格偏好。
- `travel_plan`：方案版本。
- `travel_plan_detail`：每日路线、节点顺序、到离时间、节奏变化和休息点解释。
- `map_area`：景区/校园区域。
- `map_node`：区域内部建筑物、景点、教学楼、办公楼、宿舍楼和服务设施。
- `map_edge`：区域内部道路图边。
- `travel_diary`：旅游日记、浏览量、评分、兴趣标签和压缩正文。

## 推荐导入顺序

```bash
mysql -u root -p < schema.sql
mysql -u root -p personalized_travel < seed.sql
```

如果已有 `personalized_travel` 和高德数据，只想增加景区/校园内部地图表，推荐导入：

```powershell
mysql --default-character-set=utf8mb4 -u root -p -e "source database/map_schema.sql"
```

## 生成景区/校园内部地图验收数据

```powershell
node database/generate_area_map_data.mjs
mysql --default-character-set=utf8mb4 -u root -p -e "source database/generated/area_map_data.sql"
```

生成结果默认满足：

- `map_area = 200`
- `map_node = 14000`
- `map_edge = 20000`

导入后检查数量：

```powershell
mysql --default-character-set=utf8mb4 -u root -p -e "USE personalized_travel; SELECT COUNT(*) AS area_count FROM map_area; SELECT COUNT(*) AS node_count FROM map_node; SELECT COUNT(*) AS edge_count FROM map_edge; SELECT category, COUNT(*) AS count FROM map_node WHERE node_type = 'facility' GROUP BY category;"
```

后端验收接口：

- `GET /api/map/stats`
- `GET /api/map/areas`
- `GET /api/map/areas/{id}/nodes`
- `GET /api/map/areas/{id}/edges`
- `GET /api/map/areas/{id}/path?fromNodeId=1&toNodeId=20&strategy=distance`
- `GET /api/map/areas/{id}/path?fromNodeId=1&toNodeId=20&strategy=time`
- `GET /api/map/areas/{id}/nearby?fromNodeId=1&category=洗手间&limit=10`

其中：

- `/path` 使用 Dijkstra 算法在内部道路图上求最短路径，`strategy=distance` 表示距离最短，`strategy=time` 表示时间最短。
- `/nearby` 使用内部道路最短距离排序附近服务设施，不使用直线距离，适合演示“场所查询”的查找和排序算法。

## 导入旅游日记演示数据

```powershell
mysql --default-character-set=utf8mb4 -u root -p -e "source database/diary_schema.sql"
node database/generate_diary_seed.mjs
mysql --default-character-set=utf8mb4 -u root -p -e "source database/generated/diary_seed_80.sql"
```

后端日记接口：

- `GET /api/diaries`
- `POST /api/diaries`
- `GET /api/diaries/{id}`
- `POST /api/diaries/{id}/rate?rating=5`
- `GET /api/diaries/recommend?interest=历史`
- `GET /api/diaries/stats`

前端演示页：

- `frontend/diary.html`

可演示能力：

- 日记新增和统一管理；
- 浏览日记时增加浏览量；
- 按热度、评分、最新排序；
- 按目的地、标题和正文关键词检索；
- 按兴趣标签、浏览量和评分进行推荐；
- 使用 GZIP + Base64 保存 `compressed_content`，用于说明无损压缩存储思路。
- 前端提供“生成动画”按钮，根据日记照片、标题、目的地和兴趣标签生成旅行短片预览，用于演示 AIGC 旅行影像生成流程。

## 使用高德 API 生成真实数据

先在高德开放平台创建 **Web 服务 API** 类型 Key，然后运行：

```powershell
$env:AMAP_KEY="你的高德Web服务Key"
node database/amap_fetch_generate.mjs
```

脚本会输出：

- `database/generated/amap_raw_poi.json`
- `database/generated/amap_scenic_spot.sql`
- `database/generated/amap_route_edge.sql`
- `database/generated/amap_all.sql`

导入真实数据：

```bash
mysql -u root -p personalized_travel < database/generated/amap_all.sql
```

可选环境变量：

- `AMAP_MAX_POIS`：最多保留多少个 POI，默认 `40`
- `AMAP_ROUTE_NODE_LIMIT`：参与路径边生成的节点数量，默认 `25`
- `AMAP_PAGE_LIMIT`：每个关键词翻页数，默认 `1`
- `AMAP_REQUEST_DELAY_MS`：POI 查询间隔，默认 `1000`
- `AMAP_ROUTE_DELAY_MS`：路线查询间隔，默认 `1200`
- `AMAP_MAX_RETRY`：高德限流后的重试次数，默认 `6`

如果出现 `CUQPS_HAS_EXCEEDED_THE_LIMIT 10021`，说明高德接口被限流。先等 1-2 分钟，再用更慢参数重跑：

```powershell
$env:AMAP_REQUEST_DELAY_MS="1500"
$env:AMAP_ROUTE_DELAY_MS="1800"
$env:AMAP_PAGE_LIMIT="1"
$env:AMAP_ROUTE_NODE_LIMIT="20"
node database/amap_fetch_generate.mjs
```

## 与前后端字段关系

前端和后端使用驼峰字段，数据库使用下划线字段：

- `ticketPrice` -> `ticket_price`
- `stayDuration` -> `stay_duration`
- `isRecoveryNode` -> `is_recovery_node`
- `physicalLoad` -> `physical_load`
- `cognitiveLoad` -> `cognitive_load`
- `crowdLoad` -> `crowd_load`
- `queueLoad` -> `queue_load`
- `recoveryValue` -> `recovery_value`
- `walkingTolerance` -> `walking_tolerance`
- `crowdSensitivity` -> `crowd_sensitivity`
- `comfortPreference` -> `comfort_preference`
