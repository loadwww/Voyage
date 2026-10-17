-- 个性化旅游系统数据库结构
-- MySQL 8.x / utf8mb4

CREATE DATABASE IF NOT EXISTS personalized_travel
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE personalized_travel;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS travel_plan_detail;
DROP TABLE IF EXISTS travel_plan;
DROP TABLE IF EXISTS travel_diary;
DROP TABLE IF EXISTS user_demand;
DROP TABLE IF EXISTS route_edge;
DROP TABLE IF EXISTS scenic_feature;
DROP TABLE IF EXISTS scenic_spot;
DROP TABLE IF EXISTS map_edge;
DROP TABLE IF EXISTS map_node;
DROP TABLE IF EXISTS map_area;
DROP TABLE IF EXISTS map_poi_raw;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE map_poi_raw (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  source_name VARCHAR(50) NOT NULL COMMENT '数据来源，如 amap / baidu',
  source_poi_id VARCHAR(100) COMMENT '地图平台原始 POI ID',
  raw_json JSON COMMENT '地图平台原始返回内容',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT='地图原始数据留档表';

CREATE TABLE map_area (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(120) NOT NULL,
  area_type VARCHAR(30) NOT NULL COMMENT 'scenic / campus',
  city VARCHAR(100) NOT NULL,
  district VARCHAR(100),
  address VARCHAR(255),
  longitude DECIMAL(10,6) NOT NULL,
  latitude DECIMAL(10,6) NOT NULL,
  description VARCHAR(500),
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_area_type (area_type),
  INDEX idx_area_city (city),
  INDEX idx_area_district (district)
) COMMENT='景区和校园区域表';

CREATE TABLE map_node (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  area_id BIGINT NOT NULL,
  name VARCHAR(120) NOT NULL,
  node_type VARCHAR(30) NOT NULL COMMENT 'building / facility / scenic',
  category VARCHAR(50) NOT NULL COMMENT '教学楼 / 食堂 / 洗手间 / 景点等',
  longitude DECIMAL(10,6) NOT NULL,
  latitude DECIMAL(10,6) NOT NULL,
  floor_count INT NOT NULL DEFAULT 1,
  open_time TIME,
  close_time TIME,
  service_tags VARCHAR(255),
  description VARCHAR(500),
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_node_area
    FOREIGN KEY (area_id) REFERENCES map_area(id)
    ON DELETE CASCADE,
  INDEX idx_node_area (area_id),
  INDEX idx_node_type (node_type),
  INDEX idx_node_category (category)
) COMMENT='景区和校园内部建筑物、景点与服务设施表';

CREATE TABLE map_edge (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  area_id BIGINT NOT NULL,
  from_node_id BIGINT NOT NULL,
  to_node_id BIGINT NOT NULL,
  distance DECIMAL(10,2) NOT NULL COMMENT '距离，米',
  travel_time INT NOT NULL COMMENT '步行通行分钟数',
  road_type VARCHAR(50) NOT NULL DEFAULT 'walkway',
  bidirectional TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_edge_area
    FOREIGN KEY (area_id) REFERENCES map_area(id)
    ON DELETE CASCADE,
  CONSTRAINT fk_edge_from_node
    FOREIGN KEY (from_node_id) REFERENCES map_node(id)
    ON DELETE CASCADE,
  CONSTRAINT fk_edge_to_node
    FOREIGN KEY (to_node_id) REFERENCES map_node(id)
    ON DELETE CASCADE,
  UNIQUE KEY uk_area_edge (area_id, from_node_id, to_node_id),
  INDEX idx_edge_area (area_id),
  INDEX idx_edge_from (from_node_id),
  INDEX idx_edge_to (to_node_id)
) COMMENT='景区和校园内部道路图边表';

CREATE TABLE scenic_spot (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  city VARCHAR(100) NOT NULL,
  district VARCHAR(100),
  address VARCHAR(255),
  longitude DECIMAL(10,6) NOT NULL,
  latitude DECIMAL(10,6) NOT NULL,
  category VARCHAR(50),
  ticket_price DECIMAL(10,2) NOT NULL DEFAULT 0,
  stay_duration INT NOT NULL COMMENT '建议停留分钟数',
  open_time TIME,
  close_time TIME,
  popularity DECIMAL(5,2) NOT NULL DEFAULT 0,
  description TEXT,
  is_recovery_node TINYINT NOT NULL DEFAULT 0 COMMENT '1 表示可作为中途休息/恢复节点',
  image_url VARCHAR(500),
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_spot_city (city),
  INDEX idx_spot_district (district),
  INDEX idx_spot_category (category),
  INDEX idx_spot_recovery (is_recovery_node)
) COMMENT='景点与中途休息点基础信息表';

CREATE TABLE scenic_feature (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  scenic_id BIGINT NOT NULL,
  history_score DECIMAL(5,2) NOT NULL DEFAULT 0,
  food_score DECIMAL(5,2) NOT NULL DEFAULT 0,
  nature_score DECIMAL(5,2) NOT NULL DEFAULT 0,
  shopping_score DECIMAL(5,2) NOT NULL DEFAULT 0,
  leisure_score DECIMAL(5,2) NOT NULL DEFAULT 0,
  photo_score DECIMAL(5,2) NOT NULL DEFAULT 0,
  physical_load DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '步行/体力压力，0~1',
  cognitive_load DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '信息量/参观压力，0~1',
  crowd_load DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '人流压力，0~1',
  queue_load DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '排队压力，0~1',
  recovery_value DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '放松恢复价值，0~1',
  tags VARCHAR(255) COMMENT '冗余标签，便于展示和检索',
  CONSTRAINT fk_feature_spot
    FOREIGN KEY (scenic_id) REFERENCES scenic_spot(id)
    ON DELETE CASCADE,
  UNIQUE KEY uk_feature_scenic (scenic_id)
) COMMENT='景点兴趣特征与旅行强度特征表';

CREATE TABLE route_edge (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  from_spot_id BIGINT NOT NULL,
  to_spot_id BIGINT NOT NULL,
  distance DECIMAL(10,2) NOT NULL COMMENT '距离，公里',
  travel_time INT NOT NULL COMMENT '预计通行分钟数',
  travel_cost DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '预计交通费用',
  transport_mode VARCHAR(20) NOT NULL DEFAULT 'walk' COMMENT 'walk / transit / taxi',
  CONSTRAINT fk_route_from
    FOREIGN KEY (from_spot_id) REFERENCES scenic_spot(id)
    ON DELETE CASCADE,
  CONSTRAINT fk_route_to
    FOREIGN KEY (to_spot_id) REFERENCES scenic_spot(id)
    ON DELETE CASCADE,
  UNIQUE KEY uk_route_pair_mode (from_spot_id, to_spot_id, transport_mode),
  INDEX idx_route_from (from_spot_id),
  INDEX idx_route_to (to_spot_id)
) COMMENT='景点之间路径关系表';

CREATE TABLE user_demand (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  raw_text TEXT,
  destination VARCHAR(100) NOT NULL DEFAULT '成都',
  days INT NOT NULL DEFAULT 3,
  budget DECIMAL(10,2) NOT NULL DEFAULT 1600,
  interests VARCHAR(255) COMMENT '逗号分隔兴趣，如 历史,美食,拍照',
  pace_type VARCHAR(50) NOT NULL DEFAULT 'balanced',
  special_constraints VARCHAR(255),
  walking_tolerance VARCHAR(50) NOT NULL DEFAULT 'medium',
  crowd_sensitivity VARCHAR(50) NOT NULL DEFAULT 'medium',
  rest_preference VARCHAR(50) NOT NULL DEFAULT 'need_noon_rest',
  comfort_preference VARCHAR(50) NOT NULL DEFAULT 'comfort',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_demand_destination (destination),
  INDEX idx_demand_create_time (create_time)
) COMMENT='用户原始需求与旅行风格偏好表';

CREATE TABLE travel_diary (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(160) NOT NULL,
  destination VARCHAR(100) NOT NULL,
  author_name VARCHAR(80) NOT NULL DEFAULT '游客',
  content TEXT NOT NULL,
  image_url VARCHAR(500),
  video_url VARCHAR(500),
  interest_tags VARCHAR(255),
  view_count INT NOT NULL DEFAULT 0,
  rating DECIMAL(3,2) NOT NULL DEFAULT 0,
  rating_count INT NOT NULL DEFAULT 0,
  compressed_content MEDIUMTEXT,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_diary_destination (destination),
  INDEX idx_diary_title (title),
  INDEX idx_diary_view (view_count),
  INDEX idx_diary_rating (rating),
  FULLTEXT KEY ft_diary_text (title, content, interest_tags)
) COMMENT='旅游日记管理表';

CREATE TABLE travel_plan (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  demand_id BIGINT,
  total_cost DECIMAL(10,2),
  total_time INT COMMENT '总时长，分钟',
  total_score DECIMAL(10,2),
  pace_type VARCHAR(50),
  summary TEXT,
  parent_plan_id BIGINT,
  version_no INT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_plan_demand
    FOREIGN KEY (demand_id) REFERENCES user_demand(id)
    ON DELETE SET NULL,
  CONSTRAINT fk_plan_parent
    FOREIGN KEY (parent_plan_id) REFERENCES travel_plan(id)
    ON DELETE SET NULL,
  INDEX idx_plan_demand (demand_id),
  INDEX idx_plan_parent (parent_plan_id)
) COMMENT='旅游方案版本表';

CREATE TABLE travel_plan_detail (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  plan_id BIGINT NOT NULL,
  day_no INT NOT NULL,
  scenic_id BIGINT NOT NULL,
  order_no INT NOT NULL,
  arrive_time TIME,
  leave_time TIME,
  fatigue_state_before VARCHAR(50),
  fatigue_state_after VARCHAR(50),
  fatigue_risk DECIMAL(5,2) NOT NULL DEFAULT 0,
  is_recovery_node TINYINT NOT NULL DEFAULT 0,
  reason VARCHAR(255),
  suggested_stay_minutes INT,
  CONSTRAINT fk_detail_plan
    FOREIGN KEY (plan_id) REFERENCES travel_plan(id)
    ON DELETE CASCADE,
  CONSTRAINT fk_detail_spot
    FOREIGN KEY (scenic_id) REFERENCES scenic_spot(id)
    ON DELETE RESTRICT,
  UNIQUE KEY uk_plan_day_order (plan_id, day_no, order_no),
  INDEX idx_detail_plan_day (plan_id, day_no),
  INDEX idx_detail_recovery (is_recovery_node)
) COMMENT='每日路线节点、状态变化与休息点解释表';
