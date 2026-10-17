# 校园/景区路线规划验收说明

本次补充的路线规划功能对应课程设计中的“旅游路线规划”要求。

## 已实现功能

1. 单目标最优线路
   - 接口：`GET /api/map/areas/{id}/path`
   - 参数：`fromNodeId`、`toNodeId`、`strategy`
   - 核心算法：Dijkstra 最短路径算法

2. 多目标参观并返回起点
   - 接口：`GET /api/map/areas/{id}/multi-path`
   - 参数：`startNodeId`、`targetNodeIds`、`strategy`
   - 核心算法：多点之间先用 Dijkstra 求最短路，再用状态压缩 DP 求参观顺序，最后回到起点

3. 导航图形界面
   - 页面：`frontend/map.html`
   - 展示：地图节点、道路边、路径高亮、路线步骤、附近设施、室内导航步骤

4. 路线策略
   - `strategy=distance`：距离最短
   - `strategy=time`：时间最短，按“真实速度 = 拥挤度 × 理想速度”计算
   - `strategy=transport`：交通工具最快，校园支持步行/自行车，景区支持步行/电瓶车，并按道路类型限制车辆通行

5. 室内导航模拟
   - 接口：`GET /api/map/areas/{id}/indoor-path`
   - 模拟流程：大门 -> 大厅 -> 电梯 -> 楼层 -> 房间

6. 美食推荐
   - 接口：`GET /api/map/areas/{id}/foods`
   - 参数：`fromNodeId`、`cuisine`、`keyword`、`sort`、`limit`
   - 支持菜系过滤：川菜、火锅、小吃、粤菜、面食、轻食、轻食咖啡
   - 支持模糊查询：美食名称、菜系、饭店/窗口名称、招牌菜
   - 支持排序：热度、评价、距离
   - 核心算法：内容模糊匹配 + Top-K 小堆，只维护前 10 个候选，不对全部美食做完整排序

## 演示入口

启动后端后，打开：

```text
frontend/map.html
```

演示顺序建议：

1. 先看数据规模：区域、节点、道路边、设施类型。
2. 选择一个校园或景区。
3. 演示“距离最短”和“时间最短”，说明时间策略考虑拥挤度。
4. 演示“交通工具最快”，说明自行车/电瓶车只能走允许的道路。
5. 输入多个目标点 ID，演示“多点参观并返回”。
6. 选择建筑物，输入目标房间，演示室内导航。
7. 在“美食推荐”中选择菜系和排序方式，输入关键词，演示前 10 个美食推荐。

## 本次补强项

1. 景区/校园推荐
   - 接口：`GET /api/map/recommend-areas`
   - 参数：`type`、`keyword`、`interest`、`sort`、`limit`
   - 页面：`frontend/map.html` 的“景区/校园推荐”
   - 算法说明：使用 Top-K 堆维护前 10 个区域，可按热度、评分、兴趣匹配切换，不需要完整排序所有区域。

2. 附近设施查询增强
   - 接口：`GET /api/map/areas/{id}/nearby`
   - 增强：支持设施类别输入、服务标签模糊匹配、道路距离范围 `radiusMeters`
   - 页面：`frontend/map.html` 的“附近设施”
   - 演示口径：结果按内部道路最短距离排序，不按直线距离排序。

3. 旅游日记检索增强
   - 精确标题接口：`GET /api/diaries/exact-title`
   - 全文检索接口：`GET /api/diaries/fulltext`
   - 页面：`frontend/diary.html`
   - 演示口径：标题精确查用于准确定位某篇日记；全文检索优先使用数据库 `FULLTEXT` 索引，异常或结果为空时回退到模糊查询。

4. 10 并发用户演示
   - 脚本：`database/performance_test_10_users.ps1`
   - 运行方式：
     ```powershell
     powershell -ExecutionPolicy Bypass -File database\performance_test_10_users.ps1
     ```
   - 演示口径：脚本同时启动 10 个用户任务，请求地图统计、区域推荐、日记统计、全文检索等接口，并输出成功数和平均耗时。
