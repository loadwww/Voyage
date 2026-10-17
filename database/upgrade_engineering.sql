-- ============================================================
-- Voyage 工程化改造升级脚本（在已有 personalized_travel 库上执行）
-- 幂性：全部使用 IF NOT EXISTS，可重复执行
-- ============================================================

USE personalized_travel;

-- 写操作幂等键表（Idempotency-Key 防重放）
CREATE TABLE IF NOT EXISTS idempotency_record (
  idempotency_key VARCHAR(80) PRIMARY KEY COMMENT '客户端生成的幂等键（UUID）',
  response_ref   BIGINT       NOT NULL COMMENT '首次请求创建的资源 id（如 travel_diary.id）',
  create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_idempotency_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='写操作幂等记录表';
