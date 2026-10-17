-- 景区/校园内部地图扩展表
-- 可单独导入，不会清空 scenic_spot / route_edge 等已有旅游数据。

USE personalized_travel;

CREATE TABLE IF NOT EXISTS map_area (
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

CREATE TABLE IF NOT EXISTS map_node (
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

CREATE TABLE IF NOT EXISTS map_edge (
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
