-- 旅游日记扩展表
-- 可单独导入，不会清空已有景点、地图和路线数据。

USE personalized_travel;

CREATE TABLE IF NOT EXISTS travel_diary (
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
