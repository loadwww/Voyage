# Voyage 个性化旅游系统后端

当前版本先不接数据库，使用内存中的成都主城区核心旅游圈 POI 数据，完成前后端联调所需的接口、字段和节奏控制逻辑。

## 已实现能力

- `POST /api/demand/parse`：解析自然语言需求，输出结构化旅游需求和节奏画像。
- `GET /api/spots`、`GET /api/spots/{id}`：查询景点与恢复节点。
- `POST /api/plans/generate`：生成单人动态节奏路线。
- `POST /api/plans/replan`：根据预算、节奏、删除景点或一键调节动作生成新版本路线。
- `POST /api/group-plans/generate`：保留群体规划接口。
- `GET /api/meta`：返回兴趣、城市、类别、节奏类型等前端元数据。

## 本轮核心改造

- 地图范围固定为成都主城区核心旅游圈。
- `ScenicSpot` 增加行政区、地址、经纬度、门票、停留时长、恢复节点标记、体力/认知/拥挤/排队负担和恢复价值。
- `Demand` / `DemandRequest` 增加出游节奏、步行接受度、午休偏好、拥挤敏感度、舒适/覆盖偏好。
- 新增 `RhythmEngineService`，使用 `RELAXED`、`STABLE`、`FATIGUED`、`OVERLOADED` 四态模拟路线推进。
- `DayPlan` 增加 `nodes`，记录每个节点的到达/离开时间、状态变化、疲劳风险、恢复节点原因和建议停留时间。
- 重规划接口新增 `adjustmentType`，支持 `lighter`、`moreRecovery`、`lessWalking`、`shortenDay`。

## 运行

本机安装 Maven 后，在 `backend` 目录运行：

```bash
mvn spring-boot:run
```

默认地址：

```text
http://localhost:8080
```

## 示例请求

```bash
curl -X POST http://localhost:8080/api/plans/generate \
  -H "Content-Type: application/json" \
  -d "{\"destination\":\"成都\",\"days\":3,\"budget\":1600,\"interests\":[\"历史\",\"美食\",\"拍照\"],\"pace\":\"balanced\",\"walkingTolerance\":\"medium\",\"needNoonRest\":true,\"crowdSensitivity\":\"high\",\"comfortPreference\":\"comfort\"}"
```
