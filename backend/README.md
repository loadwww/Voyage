# Voyage 后端

Spring Boot 3.3 + Java 21 + MySQL 的智能旅行规划服务，分层为 Controller → Service → Repository（JdbcTemplate 手写 SQL + RowMapper），出入参使用 record DTO 与统一 `ApiResponse<T>`。

## 运行

```powershell
# 数据库连接通过环境变量注入（也可用 docker-compose 起本地 MySQL + Redis）
$env:MYSQL_PASSWORD="你的密码"
mvn spring-boot.run
# 或
java -jar target/travel-system-0.0.1-SNAPSHOT.jar
```

默认端口 `8081`（可用 `SERVER_PORT` 覆盖）。Redis 为可选依赖：未启动时浏览量计数自动降级为进程内缓冲，定时落库，功能不受影响。

- 接口文档：http://localhost:8081/swagger-ui.html
- 健康检查 / 指标：`/actuator/health`、`/actuator/metrics`

## 测试

```powershell
mvn test
```

覆盖 Floyd、TSP 状态压缩 DP、Top-K 优先队列、疲劳度计算、评分并发原子性，以及矩阵缓存只计算一次的契约。
