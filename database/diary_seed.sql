-- 旅游日记示例数据

USE personalized_travel;

TRUNCATE TABLE travel_diary;

INSERT INTO travel_diary
(id, title, destination, author_name, content, image_url, video_url, interest_tags, view_count, rating, rating_count, compressed_content)
VALUES
(1, '成都三日慢游：茶馆和巷子刚刚好', '成都', '林同学', '这次路线没有一味打卡，上午逛武侯祠和锦里，中午在茶馆坐了很久，下午再去宽窄巷子。最舒服的是每天都有休息点，走路压力不大。', 'https://images.unsplash.com/photo-1528127269322-539801943592?auto=format&fit=crop&w=900&q=80', '', '历史,美食,休闲', 128, 4.80, 25, ''),
(2, '在春熙路附近找咖啡休息点', '成都', '周同学', '春熙路和太古里人很多，如果一直逛会比较累。系统推荐了附近咖啡馆作为中途休息点，后面的建设路小吃街就没有那么疲惫。', 'https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=900&q=80', '', '购物,美食,休闲', 96, 4.50, 18, ''),
(3, '校园内部导航体验：从教学楼到食堂', '明德大学002校区', '王同学', '演示时我选择教学楼作为起点，食堂作为终点。距离最短和时间最短给出的路线不完全一样，能看出道路权重切换的效果。', 'https://images.unsplash.com/photo-1523580846011-d3a5bc25702b?auto=format&fit=crop&w=900&q=80', '', '校园,食堂,导航', 80, 4.30, 13, ''),
(4, '杜甫草堂适合慢慢看', '成都', '陈同学', '杜甫草堂的信息量比较高，不适合和太多博物馆连续安排。路线里加入人民公园作为缓冲，整体体验更自然。', 'https://images.unsplash.com/photo-1518005020951-eccb494ad742?auto=format&fit=crop&w=900&q=80', '', '历史,自然,休闲', 72, 4.70, 16, ''),
(5, '景区里找洗手间不能只看直线距离', '云锦山001景区', '赵同学', '附近设施查询按内部道路距离排序，比直接按经纬度直线距离更适合真实导航。演示时洗手间、便利店和服务台都能筛选。', 'https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?auto=format&fit=crop&w=900&q=80', '', '景区,服务设施,导航', 64, 4.40, 11, ''),
(6, '高效打卡也需要节奏缓冲', '成都', '李同学', '我选择了高效打卡，系统安排的景点更多，但在连续高强度节点之间仍然插入了短休息点。这样比单纯多排景点更可用。', 'https://images.unsplash.com/photo-1476514525535-07fb3b4ae5f1?auto=format&fit=crop&w=900&q=80', '', '拍照,购物,美食', 112, 4.60, 20, '');
