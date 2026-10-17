// k6 压测脚本：行程规划 + 地图导航（覆盖 Floyd 矩阵缓存与 Caffeine 图缓存）
//
// 用法：
//   k6 run --env BASE_URL=http://localhost:8081 --env AREA_ID=1 scripts/benchmark/plan-load.js
//
// 关注指标：
//   - http_req_failed                 错误率
//   - http_req_duration p(95)/p(99)   接口延迟（改造前后对比）
//   - 应用侧缓存命中：GET /actuator/metrics/voyageGraphCacheTotal
//     （首次压测后再跑一轮，miss 应基本归零，只剩 hit）

import http from 'k6/http';
import { check, sleep } from 'k6';

const baseUrl = __ENV.BASE_URL || 'http://localhost:8081';
const areaId = __ENV.AREA_ID || '1';
const fromNode = __ENV.FROM_NODE || '1';
const toNode = __ENV.TO_NODE || '50';

export const options = {
  stages: [
    { duration: '30s', target: 20 },  // 爬坡
    { duration: '1m', target: 50 },   // 稳定负载
    { duration: '30s', target: 0 },   // 回落
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'],
    'http_req_duration{endpoint:plan}': ['p(95)<800'],
    'http_req_duration{endpoint:path}': ['p(95)<500'],
  },
};

const planPayload = JSON.stringify({
  destination: '成都',
  days: 3,
  budget: 1600,
  interests: ['历史', '美食'],
  pace: 'balanced',
  needNoonRest: true,
  walkingTolerance: 'medium',
  crowdSensitivity: 'medium',
  comfortPreference: 'comfort',
});

export default function () {
  const planHeaders = { headers: { 'Content-Type': 'application/json' } };
  const planRes = http.post(`${baseUrl}/api/plans/generate`, planPayload, {
    ...planHeaders,
    tags: { endpoint: 'plan' },
  });
  check(planRes, {
    'plan status 200': (r) => r.status === 200,
    'plan success body': (r) => {
      try { return JSON.parse(r.body).success === true; } catch { return false; }
    },
  });

  const pathRes = http.get(
    `${baseUrl}/api/map/areas/${areaId}/path?fromNodeId=${fromNode}&toNodeId=${toNode}&strategy=time`,
    { tags: { endpoint: 'path' } }
  );
  check(pathRes, {
    'path status 200': (r) => r.status === 200,
    'path success body': (r) => {
      try { return JSON.parse(r.body).success === true; } catch { return false; }
    },
  });

  sleep(1);
}
