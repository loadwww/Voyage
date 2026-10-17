const API_BASE = "http://localhost:8080/api";
const PLAN_KEY = "voyage.currentPlan";

const interests = ["历史", "美食", "自然", "购物", "休闲", "拍照"];
const paceLabels = { relaxed: "轻松慢游", balanced: "均衡体验", compact: "高效打卡" };
const stateLabels = {
  RELAXED: "轻松",
  STABLE: "平稳",
  FATIGUED: "偏紧凑",
  OVERLOADED: "太累",
};

const mockSpots = [
  spot(1, "武侯祠", "武侯区", "历史文化", 50, 2.5, 96, ["历史", "拍照"], false, 0.56, 0.62, 0.58, 0.38, 0.1, "三国文化核心景点，适合作为成都人文路线起点。", 21, 58),
  spot(2, "锦里古街", "武侯区", "美食街区", 0, 1.8, 91, ["美食", "购物", "拍照"], false, 0.42, 0.32, 0.78, 0.5, 0.28, "紧邻武侯祠，适合衔接小吃与夜景，但节假日人流较多。", 25, 62),
  spot(3, "杜甫草堂", "青羊区", "历史文化", 50, 2.5, 92, ["历史", "自然", "休闲"], false, 0.48, 0.64, 0.42, 0.22, 0.24, "诗意园林与文化展陈结合，游览节奏相对舒缓。", 38, 45),
  spot(4, "宽窄巷子", "青羊区", "历史街区", 0, 2, 89, ["历史", "美食", "购物", "拍照"], false, 0.4, 0.36, 0.82, 0.44, 0.18, "成都老街巷代表，适合拍照和休闲，高峰时段会比较拥挤。", 52, 48),
  spot(5, "人民公园", "青羊区", "公园休闲", 0, 1.2, 86, ["休闲", "自然"], true, 0.16, 0.12, 0.28, 0.08, 0.88, "茶馆、湖边和慢步道很适合放慢节奏。", 48, 57),
  spot(6, "春熙路", "锦江区", "购物商圈", 0, 1.8, 94, ["购物", "美食", "拍照"], false, 0.42, 0.24, 0.86, 0.34, 0.16, "核心商圈，覆盖度高，但人流和停留时间容易增加疲惫感。", 68, 58),
  spot(7, "太古里", "锦江区", "休闲商圈", 0, 1.6, 93, ["购物", "美食", "休闲", "拍照"], true, 0.26, 0.18, 0.62, 0.2, 0.64, "开放式街区和餐饮密集，可作为购物路线里的短休息。", 73, 54),
  spot(8, "成都博物馆", "青羊区", "博物馆", 0, 2.2, 88, ["历史", "拍照"], false, 0.28, 0.78, 0.48, 0.3, 0.12, "展陈丰富，适合文化偏好用户，但连续参观会比较耗神。", 56, 42),
  spot(9, "文殊院", "青羊区", "历史文化", 0, 1.8, 84, ["历史", "休闲", "美食"], false, 0.32, 0.46, 0.36, 0.16, 0.3, "寺院街区节奏平稳，周边小吃适合轻松衔接。", 64, 34),
  spot(10, "东郊记忆", "成华区", "艺术街区", 0, 2, 82, ["拍照", "购物", "休闲"], false, 0.44, 0.38, 0.52, 0.18, 0.22, "工业风街区，适合拍照和文创体验。", 82, 39),
  spot(11, "鹤鸣茶社", "青羊区", "茶馆休息", 0, 0.9, 83, ["休闲", "美食"], true, 0.08, 0.08, 0.3, 0.06, 0.95, "人民公园内的代表性茶社，适合安排 40 分钟中途休息。", 47, 60),
  spot(12, "建设路小吃街", "成华区", "美食街区", 0, 1.5, 87, ["美食", "购物"], false, 0.36, 0.18, 0.76, 0.42, 0.2, "夜间美食选择丰富，适合收尾，但排队概率较高。", 77, 33),
  spot(13, "望平街咖啡带", "锦江区", "咖啡休息", 0, 1, 80, ["休闲", "美食"], true, 0.1, 0.1, 0.34, 0.08, 0.86, "河边咖啡与轻餐集中，适合跨区移动后的短暂停留。", 70, 44),
  spot(14, "青羊宫", "青羊区", "历史文化", 10, 1.5, 79, ["历史", "休闲"], false, 0.3, 0.42, 0.28, 0.1, 0.28, "文化氛围轻松，可和杜甫草堂、人民公园形成低压力路线。", 40, 54),
  spot(15, "奎星楼街", "青羊区", "美食街区", 0, 1.4, 85, ["美食", "休闲", "拍照"], false, 0.3, 0.2, 0.66, 0.34, 0.24, "餐饮与小店密集，适合晚间慢逛。", 50, 42),
];

const travelMinutes = [
  [0, 12, 28, 18, 24, 20, 30, 32, 16, 36, 14, 26, 34, 22, 20],
  [12, 0, 24, 14, 18, 16, 26, 28, 12, 34, 10, 24, 32, 18, 16],
  [28, 24, 0, 22, 20, 28, 36, 18, 26, 42, 26, 30, 44, 16, 22],
  [18, 14, 22, 0, 16, 14, 24, 26, 18, 32, 12, 20, 30, 16, 12],
  [24, 18, 20, 16, 0, 12, 18, 22, 24, 30, 18, 16, 26, 14, 16],
  [20, 16, 28, 14, 12, 0, 16, 24, 22, 26, 18, 14, 24, 18, 14],
  [30, 26, 36, 24, 18, 16, 0, 28, 34, 20, 28, 18, 18, 24, 22],
  [32, 28, 18, 26, 22, 24, 28, 0, 30, 38, 30, 26, 36, 20, 26],
  [16, 12, 26, 18, 24, 22, 34, 30, 0, 40, 14, 28, 36, 22, 20],
  [36, 34, 42, 32, 30, 26, 20, 38, 40, 0, 34, 22, 16, 32, 30],
  [14, 10, 26, 12, 18, 18, 28, 30, 14, 34, 0, 22, 32, 18, 14],
  [26, 24, 30, 20, 16, 14, 18, 26, 28, 22, 22, 0, 20, 18, 20],
  [34, 32, 44, 30, 26, 24, 18, 36, 36, 16, 32, 20, 0, 30, 28],
  [22, 18, 16, 16, 14, 18, 24, 20, 22, 32, 18, 18, 30, 0, 16],
  [20, 16, 22, 12, 16, 14, 22, 26, 20, 30, 14, 20, 28, 16, 0],
];

const app = {
  spots: [...mockSpots],
  selectedInterests: ["历史", "美食", "拍照"],
  rhythm: {
    pace: "balanced",
    walkingTolerance: "medium",
    crowdSensitivity: "medium",
    comfortPreference: "comfort",
  },
  plan: null,
  versions: [],
};

const $ = (selector) => document.querySelector(selector);
const $$ = (selector) => Array.from(document.querySelectorAll(selector));

function spot(id, name, district, category, ticketPrice, stayDuration, popularity, tags, isRecoveryNode, physicalLoad, cognitiveLoad, crowdLoad, queueLoad, recoveryValue, description, x, y) {
  return {
    id,
    name,
    city: "成都",
    district,
    category,
    ticketPrice,
    stayDuration,
    popularity,
    tags,
    isRecoveryNode,
    physicalLoad,
    cognitiveLoad,
    crowdLoad,
    queueLoad,
    recoveryValue,
    description,
    x,
    y,
    image: "https://images.unsplash.com/photo-1528127269322-539801943592?auto=format&fit=crop&w=900&q=80",
  };
}

async function init() {
  await loadSpots();
  app.plan = loadPlan();
  const page = document.body.dataset.page;
  if (page === "home") initHome();
  if (page === "planner") initPlanner();
  if (page === "result") initResult();
  if (page === "detail") initDetail();
  if (page === "adjust") initAdjust();
  if (page === "map") initMapDemo();
  if (page === "diary") initDiary();
}

async function loadSpots() {
  try {
    const response = await fetch(`${API_BASE}/spots?city=成都`);
    const body = await response.json();
    if (body.success && Array.isArray(body.data) && body.data.length) {
      app.spots = body.data.map((item, index) => ({ ...item, x: mockSpots[index]?.x || 50, y: mockSpots[index]?.y || 50 }));
    }
  } catch {
    app.spots = [...mockSpots];
  }
}

function initHome() {
  renderMiniMap("#home-map", null, { preview: true });
}

function initPlanner() {
  renderInterestChips();
  bindSegmented();
  renderPlannerMap();
  $("#planner-form").addEventListener("submit", (event) => {
    event.preventDefault();
    submitGenerate();
  });
  $("#load-demo")?.addEventListener("click", loadDemo);
  $("#parse-demand")?.addEventListener("click", parseDemand);
  initResizable(); // 初始化可调节分栏
}

function initResult() {
  ensurePlan();
  renderResultPage();
}

function initDetail() {
  ensurePlan();
  renderDetailPage();
}

function initAdjust() {
  ensurePlan();
  bindSegmented();
  renderAdjustPage();
}

async function initDiary() {
  await loadDiaryStats();
  await loadDiaries();
  $("#diary-search")?.addEventListener("click", loadDiaries);
  $("#diary-recommend")?.addEventListener("click", loadRecommendedDiaries);
  $("#diary-title-search")?.addEventListener("click", loadExactTitleDiaries);
  $("#diary-fulltext-search")?.addEventListener("click", loadFullTextDiaries);
  $("#aigc-generate")?.addEventListener("click", generateStandaloneAnimation);
  $("#aigc-image-file")?.addEventListener("change", previewUploadedAigcImage);
  $("#diary-form")?.addEventListener("submit", async (event) => {
    event.preventDefault();
    await createDiary();
  });
}

async function loadDiaryStats() {
  const stats = await getJson(`${API_BASE}/diaries/stats`, null);
  const container = $("#diary-stats");
  if (!container) return;
  if (!stats) {
    container.innerHTML = `<p class="muted-text">后端启动后可查看日记统计。</p>`;
    return;
  }
  container.innerHTML = `
    <div><span>日记数</span><strong>${stats.diaryCount}</strong></div>
    <div><span>浏览量</span><strong>${stats.totalViews}</strong></div>
    <div><span>平均评分</span><strong>${round(stats.averageRating)}</strong></div>
    <div><span>压缩存储</span><strong>${stats.compressedCount}</strong></div>
  `;
}

async function loadDiaries() {
  const destination = encodeURIComponent($("#diary-destination")?.value.trim() || "");
  const keyword = encodeURIComponent($("#diary-keyword")?.value.trim() || "");
  const sort = $("#diary-sort")?.value || "hot";
  const diaries = await getJson(`${API_BASE}/diaries?destination=${destination}&keyword=${keyword}&sort=${sort}&limit=30`, []);
  renderDiaries(diaries);
  await loadDiaryStats();
}

async function loadRecommendedDiaries() {
  const interest = encodeURIComponent($("#diary-interest")?.value || "");
  const destination = encodeURIComponent($("#diary-destination")?.value.trim() || "");
  const diaries = await getJson(`${API_BASE}/diaries/recommend?interest=${interest}&destination=${destination}&limit=10`, []);
  renderDiaries(diaries);
}

async function loadExactTitleDiaries() {
  const title = encodeURIComponent($("#diary-exact-title")?.value.trim() || "");
  if (!title) return;
  const diaries = await getJson(`${API_BASE}/diaries/exact-title?title=${title}&limit=20`, []);
  renderDiaries(diaries);
}

async function loadFullTextDiaries() {
  const keyword = encodeURIComponent($("#diary-fulltext-keyword")?.value.trim() || $("#diary-keyword")?.value.trim() || "");
  if (!keyword) return;
  const sort = $("#diary-sort")?.value || "hot";
  const diaries = await getJson(`${API_BASE}/diaries/fulltext?keyword=${keyword}&sort=${sort}&limit=30`, []);
  renderDiaries(diaries);
}

async function createDiary() {
  const payload = {
    title: $("#diary-title").value.trim(),
    destination: $("#diary-form-destination").value.trim(),
    authorName: $("#diary-author").value.trim(),
    content: $("#diary-content").value.trim(),
    imageUrl: $("#diary-image").value.trim(),
    videoUrl: "",
    interestTags: $("#diary-tags").value.trim(),
  };
  if (!payload.title || !payload.destination || !payload.content) return;
  await postJson(`${API_BASE}/diaries`, payload);
  $("#diary-keyword").value = "";
  $("#diary-destination").value = payload.destination;
  await loadDiaries();
}

function renderDiaries(diaries) {
  const container = $("#diary-list");
  if (!container) return;
  if (!diaries.length) {
    container.innerHTML = `<article class="insight-card"><p class="muted-text">没有找到日记，可以换个关键词或新写一篇。</p></article>`;
    return;
  }
  container.innerHTML = diaries.map(renderDiaryCard).join("");
  $$(".diary-card").forEach((card) => {
    card.addEventListener("click", async (event) => {
      const detailButton = event.target.closest("[data-open-detail]");
      if (event.target.closest("button") && !detailButton) return;
      const id = card.dataset.id;
      const diary = await getJson(`${API_BASE}/diaries/${id}`, null);
      if (diary) {
        showDiaryDetail(diary);
        await loadDiaryStats();
      }
    });
  });
  $$(".rate-buttons button[data-rating]").forEach((button) => {
    button.addEventListener("click", async () => {
      const id = button.closest(".diary-card").dataset.id;
      const rating = button.dataset.rating;
      const diary = await postJson(`${API_BASE}/diaries/${id}/rate?rating=${rating}`, {});
      if (diary?.success) {
        await loadDiaries();
      }
    });
  });
  $$(".rate-buttons button[data-open-animation]").forEach((button) => {
    button.addEventListener("click", async () => {
      const id = button.closest(".diary-card").dataset.id;
      const diary = await getJson(`${API_BASE}/diaries/${id}`, null);
      if (diary) showDiaryAnimation(diary);
    });
  });
}

function renderDiaryCard(diary) {
  const image = diary.imageUrl || "https://images.unsplash.com/photo-1528127269322-539801943592?auto=format&fit=crop&w=900&q=80";
  return `
    <article class="diary-card" data-id="${diary.id}">
      <div class="diary-image" style="background-image:url('${image}')"></div>
      <div class="diary-body">
        <div class="diary-head">
          <span>${diary.destination}</span>
          <strong>${round(diary.rating || 0)} 分</strong>
        </div>
        <h3>${diary.title}</h3>
        <p>${diary.content}</p>
        <div class="tag-row">
          ${(diary.interestTags || "").split(/[，,\s]+/).filter(Boolean).slice(0, 5).map((tag) => `<span class="mini-tag">${tag}</span>`).join("")}
        </div>
        <div class="diary-meta">
          <span>${diary.authorName || "游客"}</span>
          <span>${formatDateTime(diary.createTime)}</span>
          <span>浏览 ${diary.viewCount}</span>
          <span>${compressionText(diary)}</span>
        </div>
        <div class="rate-buttons">
          <button type="button" data-open-detail="true">查看全文</button>
          <button type="button" data-open-animation="true">生成动画</button>
          <button type="button" data-rating="5">5 分</button>
          <button type="button" data-rating="4">4 分</button>
          <button type="button" data-rating="3">3 分</button>
        </div>
      </div>
    </article>
  `;
}

function showDiaryDetail(diary) {
  let modal = $("#diary-detail-modal");
  if (!modal) {
    modal = document.createElement("div");
    modal.id = "diary-detail-modal";
    modal.className = "diary-modal";
    document.body.appendChild(modal);
  }
  const image = diary.imageUrl || "https://images.unsplash.com/photo-1528127269322-539801943592?auto=format&fit=crop&w=900&q=80";
  modal.innerHTML = `
    <div class="diary-modal-backdrop" data-close-diary="true"></div>
    <article class="diary-modal-card">
      <button class="diary-modal-close" type="button" data-close-diary="true">关闭</button>
      <div class="diary-modal-image" style="background-image:url('${image}')"></div>
      <div class="diary-modal-body">
        <p class="eyebrow">${diary.destination}</p>
        <h2>${escapeHtml(diary.title)}</h2>
        <div class="diary-meta">
          <span>${escapeHtml(diary.authorName || "游客")}</span>
          <span>${formatDateTime(diary.createTime)}</span>
          <span>浏览 ${diary.viewCount}</span>
          <span>评分 ${round(diary.rating || 0)}</span>
          <span>${compressionText(diary)}</span>
        </div>
        <div class="tag-row">
          ${(diary.interestTags || "").split(/[，,\s]+/).filter(Boolean).map((tag) => `<span class="mini-tag">${escapeHtml(tag)}</span>`).join("")}
        </div>
        ${renderCompressionPanel(diary)}
        <p class="diary-full-content">${escapeHtml(diary.content)}</p>
      </div>
    </article>
  `;
  modal.querySelectorAll("[data-close-diary]").forEach((item) => {
    item.addEventListener("click", () => modal.remove());
  });
}

function showDiaryAnimation(diary) {
  let modal = $("#diary-animation-modal");
  if (!modal) {
    modal = document.createElement("div");
    modal.id = "diary-animation-modal";
    modal.className = "diary-modal";
    document.body.appendChild(modal);
  }
  const image = diary.imageUrl || "https://images.unsplash.com/photo-1528127269322-539801943592?auto=format&fit=crop&w=900&q=80";
  const captions = buildAnimationCaptions(diary);
  modal.innerHTML = `
    <div class="diary-modal-backdrop" data-close-animation="true"></div>
    <article class="diary-modal-card animation-card">
      <button class="diary-modal-close" type="button" data-close-animation="true">关闭</button>
      <div class="aigc-animation-stage">
        <div class="aigc-photo" style="background-image:url('${image}')"></div>
        <div class="aigc-caption">
          <p class="eyebrow">AIGC Travel Animation</p>
          <h2>${escapeHtml(diary.title)}</h2>
          <span>${escapeHtml(captions[0])}</span>
          <span>${escapeHtml(captions[1])}</span>
          <span>${escapeHtml(captions[2])}</span>
        </div>
      </div>
      <div class="diary-modal-body">
        <h2>照片生成旅行动画</h2>
        <p class="muted-text">演示逻辑：系统读取日记图片、标题、目的地和兴趣标签，生成分镜文案，并对照片做自动运镜、缩放和字幕叠加，用于展示 AIGC 旅行影像生成流程。</p>
        <div class="compression-panel">
          <div><span>输入</span><strong>日记照片 + 文本</strong></div>
          <div><span>生成</span><strong>3 段分镜</strong></div>
          <div><span>效果</span><strong>自动运镜动画</strong></div>
        </div>
      </div>
    </article>
  `;
  modal.querySelectorAll("[data-close-animation]").forEach((item) => {
    item.addEventListener("click", () => modal.remove());
  });
}

function previewUploadedAigcImage(event) {
  const file = event.target.files?.[0];
  if (!file) return;
  const reader = new FileReader();
  reader.onload = () => {
    $("#aigc-image-url").value = reader.result;
  };
  reader.readAsDataURL(file);
}

function generateStandaloneAnimation() {
  const place = $("#aigc-place")?.value.trim() || "旅行照片";
  const style = $("#aigc-style")?.value.trim() || "旅行记录";
  const imageUrl = $("#aigc-image-url")?.value.trim() || "https://images.unsplash.com/photo-1528127269322-539801943592?auto=format&fit=crop&w=900&q=80";
  showDiaryAnimation({
    title: `${place} · 旅行照片动画`,
    destination: place,
    content: `根据上传照片识别旅行场景，结合${style}生成运镜、字幕和分镜节奏。`,
    imageUrl,
    interestTags: style,
    authorName: "AIGC 演示",
    viewCount: 0,
    rating: 5,
    compressedContent: "",
  });
}

function buildAnimationCaptions(diary) {
  const tags = (diary.interestTags || "").split(/[，,\s]+/).filter(Boolean);
  return [
    `${diary.destination} · ${tags.slice(0, 2).join(" / ") || "旅行记录"}`,
    diary.content.slice(0, 38) + (diary.content.length > 38 ? "..." : ""),
    "生成慢推、转场和字幕，形成旅行短片预览",
  ];
}

function renderCompressionPanel(diary) {
  const info = compressionInfo(diary);
  return `
    <div class="compression-panel">
      <div><span>原文大小</span><strong>${info.originalBytes} B</strong></div>
      <div><span>压缩后</span><strong>${info.compressedBytes} B</strong></div>
      <div><span>压缩率</span><strong>${info.ratio}%</strong></div>
      <div><span>算法</span><strong>GZIP 无损</strong></div>
    </div>
  `;
}

function compressionText(diary) {
  const info = compressionInfo(diary);
  return diary.compressedContent ? `压缩率 ${info.ratio}%` : "压缩字段未生成";
}

function compressionInfo(diary) {
  const originalBytes = new Blob([diary.content || ""]).size;
  const compressedBytes = diary.compressedContent
    ? Math.max(1, Math.round(String(diary.compressedContent).length * 0.75))
    : 0;
  const ratio = originalBytes && compressedBytes
    ? Math.max(1, Math.round((1 - compressedBytes / originalBytes) * 100))
    : 0;
  return { originalBytes, compressedBytes, ratio };
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function formatDateTime(value) {
  if (!value) return "刚刚";
  const normalized = Array.isArray(value)
    ? new Date(value[0], (value[1] || 1) - 1, value[2] || 1, value[3] || 0, value[4] || 0)
    : new Date(value);
  if (Number.isNaN(normalized.getTime())) {
    return String(value).replace("T", " ").slice(0, 16);
  }
  return `${normalized.getFullYear()}-${String(normalized.getMonth() + 1).padStart(2, "0")}-${String(normalized.getDate()).padStart(2, "0")} ${String(normalized.getHours()).padStart(2, "0")}:${String(normalized.getMinutes()).padStart(2, "0")}`;
}

async function initMapDemo() {
  const state = {
    areas: [],
    area: null,
    nodes: [],
    edges: [],
    path: null,
    nearby: [],
    foods: [],
    areaRecommendations: [],
    indoor: null,
  };

  await loadMapStats();
  state.areas = await getJson(`${API_BASE}/map/areas?limit=200`, []);
  const areaSelect = $("#map-area-select");
  areaSelect.innerHTML = state.areas.map((area) => `<option value="${area.id}">${area.name} · ${area.areaType === "campus" ? "校园" : "景区"}</option>`).join("");

  areaSelect.addEventListener("change", async () => {
    await loadAreaMap(state, Number(areaSelect.value));
  });
  $("#path-distance")?.addEventListener("click", () => loadMapPath(state, "distance"));
  $("#path-time")?.addEventListener("click", () => loadMapPath(state, "time"));
  $("#path-transport")?.addEventListener("click", () => loadMapPath(state, "transport"));
  $("#multi-distance")?.addEventListener("click", () => loadMultiStopPath(state, "distance"));
  $("#multi-time")?.addEventListener("click", () => loadMultiStopPath(state, "time"));
  $("#multi-transport")?.addEventListener("click", () => loadMultiStopPath(state, "transport"));
  $("#indoor-search")?.addEventListener("click", () => loadIndoorPath(state));
  $("#nearby-search")?.addEventListener("click", () => loadNearbyFacilities(state));
  $("#food-search")?.addEventListener("click", () => loadFoodRecommendations(state));
  $("#area-recommend-search")?.addEventListener("click", () => loadAreaRecommendations(state));

  if (state.areas.length) {
    await loadAreaMap(state, state.areas[0].id);
    await loadAreaRecommendations(state);
  } else {
    renderMapDemoError("后端暂时没有返回区域数据，请确认服务已启动并导入 area_map_data.sql。");
  }
}

async function loadMapStats() {
  const stats = await getJson(`${API_BASE}/map/stats`, null);
  const container = $("#map-stats");
  if (!container) return;
  if (!stats) {
    container.innerHTML = `<p class="muted-text">后端未连接，启动服务后可查看数据规模。</p>`;
    return;
  }
  container.innerHTML = `
    <div><span>区域</span><strong>${stats.areaCount}</strong></div>
    <div><span>节点</span><strong>${stats.nodeCount}</strong></div>
    <div><span>道路边</span><strong>${stats.edgeCount}</strong></div>
    <div><span>设施类型</span><strong>${stats.facilityTypeCount}</strong></div>
  `;
}

async function loadAreaMap(state, areaId) {
  state.area = state.areas.find((area) => Number(area.id) === Number(areaId)) || null;
  state.nodes = await getJson(`${API_BASE}/map/areas/${areaId}/nodes`, []);
  state.edges = await getJson(`${API_BASE}/map/areas/${areaId}/edges`, []);
  state.path = null;
  state.nearby = [];
  state.foods = [];
  state.indoor = null;

  const fromSelect = $("#map-from-node");
  const toSelect = $("#map-to-node");
  const buildingSelect = $("#indoor-building-node");
  const coreNodes = state.nodes.filter((nodeItem) => nodeItem.nodeType !== "facility");
  const buildingNodes = state.nodes.filter((nodeItem) => ["building", "scenic"].includes(nodeItem.nodeType));
  const allOptions = state.nodes.map((nodeItem) => `<option value="${nodeItem.id}">${nodeItem.name} · ${nodeItem.category}</option>`).join("");
  fromSelect.innerHTML = allOptions;
  toSelect.innerHTML = allOptions;
  if (buildingSelect) {
    buildingSelect.innerHTML = buildingNodes.map((nodeItem) => `<option value="${nodeItem.id}">${nodeItem.name} · ${nodeItem.category}</option>`).join("");
  }
  fromSelect.value = String(coreNodes[0]?.id || state.nodes[0]?.id || "");
  toSelect.value = String(coreNodes[Math.min(19, coreNodes.length - 1)]?.id || state.nodes[Math.min(19, state.nodes.length - 1)]?.id || "");
  if (buildingSelect) {
    buildingSelect.value = String(buildingNodes[0]?.id || coreNodes[0]?.id || state.nodes[0]?.id || "");
  }
  $("#map-target-nodes").value = coreNodes.slice(4, 7).map((nodeItem) => nodeItem.id).join(",");

  $("#map-demo-title").textContent = state.area ? state.area.name : "内部道路图";
  $("#map-demo-subtitle").textContent = `${state.nodes.length} 个节点 · ${state.edges.length} 条道路边`;
  $("#path-result").innerHTML = `<p class="muted-text">选择起点和目标后，可以演示距离最短、时间最短或交通工具最快路线。</p>`;
  $("#advanced-path-result").innerHTML = `<p class="muted-text">这里展示多点参观顺序、返回起点结果，以及室内导航步骤。</p>`;
  $("#area-recommend-result").innerHTML = `<p class="muted-text">可按热度、评分或兴趣匹配推荐前 10 个景区/校园。</p>`;
  $("#food-result").innerHTML = `<p class="muted-text">选择菜系、排序方式或关键词后，可推荐前 10 个美食点。</p>`;
  $("#nearby-result").innerHTML = `<p class="muted-text">选择设施类别后，可以按内部道路距离查询最近设施。</p>`;
  renderCampusMap(state);
}

async function loadMapPath(state, strategy) {
  const areaId = $("#map-area-select").value;
  const fromNodeId = $("#map-from-node").value;
  const toNodeId = $("#map-to-node").value;
  state.path = await getJson(`${API_BASE}/map/areas/${areaId}/path?fromNodeId=${fromNodeId}&toNodeId=${toNodeId}&strategy=${strategy}`, null);
  state.indoor = null;
  renderCampusMap(state);
  renderPathResult(state.path);
}

async function loadMultiStopPath(state, strategy) {
  const areaId = $("#map-area-select").value;
  const startNodeId = $("#map-from-node").value;
  const targetNodeIds = ($("#map-target-nodes").value || "").split(",").map((item) => item.trim()).filter(Boolean).join(",");
  if (!targetNodeIds) {
    $("#advanced-path-result").innerHTML = `<p class="muted-text">请先填写至少一个目标点 ID。</p>`;
    return;
  }
  state.path = await getJson(`${API_BASE}/map/areas/${areaId}/multi-path?startNodeId=${startNodeId}&targetNodeIds=${targetNodeIds}&strategy=${strategy}`, null);
  state.indoor = null;
  renderCampusMap(state);
  renderPathResult(state.path);
  renderAdvancedPathResult(state.path, "multi");
}

async function loadIndoorPath(state) {
  const areaId = $("#map-area-select").value;
  const buildingNodeId = $("#indoor-building-node").value;
  const fromRoom = encodeURIComponent($("#indoor-from-room").value || "入口");
  const toRoom = encodeURIComponent($("#indoor-to-room").value || "305");
  state.indoor = await getJson(`${API_BASE}/map/areas/${areaId}/indoor-path?buildingNodeId=${buildingNodeId}&fromRoom=${fromRoom}&toRoom=${toRoom}`, null);
  renderAdvancedPathResult(state.indoor, "indoor");
}

async function loadNearbyFacilities(state) {
  const areaId = $("#map-area-select").value;
  const fromNodeId = $("#map-from-node").value;
  const typedCategory = $("#facility-category-keyword")?.value.trim();
  const category = encodeURIComponent(typedCategory || $("#facility-category").value || "all");
  const limit = Number($("#facility-limit").value) || 10;
  const radiusMeters = Number($("#facility-radius")?.value) || 800;
  state.nearby = await getJson(`${API_BASE}/map/areas/${areaId}/nearby?fromNodeId=${fromNodeId}&category=${category}&limit=${limit}&radiusMeters=${radiusMeters}`, []);
  renderCampusMap(state);
  renderNearbyResult(state.nearby);
}

async function loadAreaRecommendations(state) {
  const type = encodeURIComponent($("#area-recommend-type")?.value || "all");
  const keyword = encodeURIComponent($("#area-recommend-keyword")?.value.trim() || "");
  const interest = encodeURIComponent($("#area-recommend-interest")?.value.trim() || "");
  const sort = encodeURIComponent($("#area-recommend-sort")?.value || "hot");
  state.areaRecommendations = await getJson(`${API_BASE}/map/recommend-areas?type=${type}&keyword=${keyword}&interest=${interest}&sort=${sort}&limit=10`, []);
  renderAreaRecommendationResult(state.areaRecommendations);
}

async function loadFoodRecommendations(state) {
  const areaId = $("#map-area-select").value;
  const fromNodeId = $("#map-from-node").value;
  const cuisine = encodeURIComponent($("#food-cuisine").value || "");
  const keyword = encodeURIComponent($("#food-keyword").value || "");
  const sort = encodeURIComponent($("#food-sort").value || "hot");
  state.foods = await getJson(`${API_BASE}/map/areas/${areaId}/foods?fromNodeId=${fromNodeId}&cuisine=${cuisine}&keyword=${keyword}&sort=${sort}&limit=10`, []);
  renderCampusMap(state);
  renderFoodResult(state.foods);
}

function renderCampusMap(state) {
  const container = $("#campus-map");
  if (!container) return;
  if (!state.nodes.length) {
    renderMapDemoError("没有可展示的节点数据。");
    return;
  }

  const bounds = nodeBounds(state.nodes);
  const pathEdgeIds = new Set((state.path?.edges || []).map((edge) => Number(edge.id)));
  const pathNodeIds = new Set((state.path?.nodes || []).map((nodeItem) => Number(nodeItem.id)));
  const nearbyNodeIds = new Set((state.nearby || []).map((item) => Number(item.node.id)));
  const foodNodeIds = new Set((state.foods || []).map((item) => Number(item.node.id)));
  const visibleEdges = state.edges.slice(0, 160);

  container.innerHTML = `
    <div class="campus-canvas">
      ${visibleEdges.map((edge) => renderCampusEdge(edge, state.nodes, bounds, pathEdgeIds.has(Number(edge.id)))).join("")}
      ${state.nodes.map((nodeItem) => renderCampusNode(nodeItem, bounds, pathNodeIds.has(Number(nodeItem.id)), nearbyNodeIds.has(Number(nodeItem.id)) || foodNodeIds.has(Number(nodeItem.id)))).join("")}
      <div class="map-legend campus-legend">
        <span><i></i>建筑/景点</span>
        <span><i class="facility-dot"></i>服务设施</span>
        <span><i class="path-dot"></i>路径/推荐</span>
      </div>
    </div>
  `;
}

function renderPathResult(path) {
  const container = $("#path-result");
  if (!container) return;
  if (!path) {
    container.innerHTML = `<p class="muted-text">没有找到可达路线，请换一个起终点。</p>`;
    return;
  }
  const steps = path.steps || [];
  container.innerHTML = `
    <div class="path-summary">
      <span>${strategyLabel(path.strategy)}</span>
      <strong>${Math.round(path.totalDistance)} 米 · ${path.totalTime} 分钟</strong>
    </div>
    <ol class="path-steps">
      ${steps.length ? steps.map((step) => `
        <li>
          <strong>${step.fromName} → ${step.toName}</strong>
          <span>${transportLabel(step.transportMode)} · ${roadTypeLabel(step.roadType)} · ${Math.round(step.distance)} 米 · ${step.travelTime} 分钟 · 拥挤度 ${step.congestion}</span>
          <small>${step.note}</small>
        </li>
      `).join("") : path.nodes.map((nodeItem) => `<li><strong>${nodeItem.name}</strong><span>${nodeItem.category}</span></li>`).join("")}
    </ol>
  `;
}

function renderAdvancedPathResult(result, type) {
  const container = $("#advanced-path-result");
  if (!container) return;
  if (!result) {
    container.innerHTML = `<p class="muted-text">暂时没有生成结果，请重新选择节点。</p>`;
    return;
  }
  if (type === "indoor") {
    container.innerHTML = `
      <div class="path-summary">
        <span>室内导航模拟</span>
        <strong>${result.buildingName} · ${result.totalTime} 分钟</strong>
      </div>
      <ol class="path-steps">
        ${result.steps.map((step) => `<li><strong>${step}</strong><span>${result.fromRoom} → ${result.toRoom}</span></li>`).join("")}
      </ol>
    `;
    return;
  }
  const namesById = new Map((result.nodes || []).map((nodeItem) => [Number(nodeItem.id), nodeItem.name]));
  container.innerHTML = `
    <div class="path-summary">
      <span>多点途经并返回起点 · ${strategyLabel(result.strategy)}</span>
      <strong>${Math.round(result.totalDistance)} 米 · ${result.totalTime} 分钟</strong>
    </div>
    <ol class="path-steps">
      ${(result.visitOrder || []).map((nodeId, index) => `
        <li>
          <strong>${index + 1}. ${namesById.get(Number(nodeId)) || `节点 ${nodeId}`}</strong>
          <span>${index === 0 ? "当前位置" : index === result.visitOrder.length - 1 ? "返回起点" : "途经目标点"}</span>
        </li>
      `).join("")}
    </ol>
  `;
}

function renderFoodResult(items) {
  const container = $("#food-result");
  if (!container) return;
  if (!items.length) {
    container.innerHTML = `<p class="muted-text">没有找到匹配的美食，可以换个菜系或关键词。</p>`;
    return;
  }
  container.innerHTML = `
    <div class="path-summary">
      <span>Top-K 推荐算法</span>
      <strong>维护前 ${items.length} 个候选，不做全量排序</strong>
    </div>
    <ol class="facility-list food-list">
      ${items.map((item, index) => `
        <li>
          <strong>${index + 1}. ${item.restaurantName}</strong>
          <span>${item.cuisine} · ${item.signatureDish} · ${item.windowName}</span>
          <span>热度 ${item.popularity} · 评分 ${item.rating} · ${Math.round(item.roadDistance)} 米 · ${item.travelTime} 分钟</span>
          <small>${item.sortReason} · 模糊匹配分 ${item.matchScore}</small>
        </li>
      `).join("")}
    </ol>
  `;
}

function renderAreaRecommendationResult(items) {
  const container = $("#area-recommend-result");
  if (!container) return;
  if (!items.length) {
    container.innerHTML = `<p class="muted-text">没有找到匹配的景区或校园，可以换个关键词。</p>`;
    return;
  }
  container.innerHTML = `
    <div class="path-summary">
      <span>推荐 Top 10</span>
      <strong>热度、评分、兴趣匹配可切换</strong>
    </div>
    <ol class="facility-list">
      ${items.map((item, index) => `
        <li>
          <strong>${index + 1}. ${item.area.name}</strong>
          <span>${item.typeLabel} · ${item.area.city}${item.area.district ? " · " + item.area.district : ""}</span>
          <span>热度 ${item.popularity} · 评分 ${item.rating} · 匹配 ${item.matchScore}</span>
          <small>${item.reason}</small>
        </li>
      `).join("")}
    </ol>
  `;
}

function renderNearbyResult(items) {
  const container = $("#nearby-result");
  if (!container) return;
  if (!items.length) {
    container.innerHTML = `<p class="muted-text">当前范围内没有找到该类设施。</p>`;
    return;
  }
  container.innerHTML = `
    <ol class="facility-list">
      ${items.map((item) => `
        <li>
          <strong>${item.node.name}</strong>
          <span>${item.category} · ${Math.round(item.roadDistance)} 米 · ${item.travelTime} 分钟</span>
        </li>
      `).join("")}
    </ol>
  `;
}

function strategyLabel(strategy) {
  if (strategy === "time") return "时间最短：按拥挤度修正后的真实速度";
  if (strategy === "transport") return "交通工具最快：步行 + 可用车辆混合";
  return "距离最短：道路距离最小";
}

function roadTypeLabel(roadType) {
  const labels = {
    walkway: "步道",
    main_road: "主路",
    branch_road: "支路",
    greenway: "绿道",
    covered_walkway: "连廊",
    stairs: "台阶",
  };
  return labels[roadType] || roadType || "道路";
}

function transportLabel(mode) {
  const labels = {
    walk: "步行",
    bicycle: "自行车",
    electric_cart: "电瓶车",
  };
  return labels[mode] || mode || "步行";
}

function ensurePlan() {
  if (!app.plan) {
    app.plan = generateMockPlan(defaultDemand());
    savePlan(app.plan);
  }
}

function bindSegmented() {
  $$(".segmented").forEach((group) => {
    group.addEventListener("click", (event) => {
      const button = event.target.closest("button");
      if (!button) return;
      group.querySelectorAll("button").forEach((item) => item.classList.toggle("active", item === button));
      app.rhythm[group.dataset.group] = button.dataset.value;
      renderPlannerMap();
    });
  });
}

function renderInterestChips() {
  const container = $("#interest-chips");
  if (!container) return;
  container.innerHTML = interests
    .map((interest) => `<button type="button" class="chip ${app.selectedInterests.includes(interest) ? "active" : ""}" data-interest="${interest}">${interest}</button>`)
    .join("");

  $$("#interest-chips .chip").forEach((chip) => {
    chip.addEventListener("click", () => {
      const set = new Set(app.selectedInterests);
      set.has(chip.dataset.interest) ? set.delete(chip.dataset.interest) : set.add(chip.dataset.interest);
      app.selectedInterests = Array.from(set);
      chip.classList.toggle("active");
      renderPlannerMap();
    });
  });
}

function renderPlannerMap() {
  const demand = collectDemand({ quiet: true });
  const previewSpots = app.spots
    .filter((item) => item.city === "成都")
    .map((item) => ({ spot: item, score: scoreSpot(item, demand), reason: reasonSpot(item, demand) }))
    .sort((a, b) => b.score - a.score)
    .slice(0, 8);
  renderMiniMap("#planner-map", { days: [{ nodes: previewSpots.map((item, index) => node(item, 540 + index * 70, 50, "RELAXED", "STABLE", 28, isRecovery(item.spot), item.spot.description)) }] }, { preview: true });
  const list = $("#planner-preview-list");
  if (!list) return;
  list.innerHTML = previewSpots
    .map((item) => `<li><strong>${item.spot.name}</strong><span>${isRecovery(item.spot) ? "适合作为中途休息" : item.reason}</span></li>`)
    .join("");
}

function renderResultPage() {
  const plan = app.plan;
  const restCount = allNodes(plan).filter((item) => item.isRecoveryNode).length;
  $("#result-summary").innerHTML = `
    <div class="summary-card wide">
      <p class="eyebrow">Route Overview</p>
      <h1>${plan.demand.destination} ${plan.demand.days} 日路线</h1>
      <p>${plan.summary}</p>
      <div class="summary-metrics">
        <div><span>预计花费</span><strong>${Math.round(plan.totalCost)} 元</strong></div>
        <div><span>总时长</span><strong>${round(plan.totalHours)} 小时</strong></div>
        <div><span>整体节奏</span><strong>${paceLabels[plan.demand.pace]}</strong></div>
        <div><span>中途休息</span><strong>${restCount} 处</strong></div>
      </div>
    </div>
  `;
  renderMiniMap("#result-map", plan);
  $("#result-days").innerHTML = plan.days.map(renderDaySummary).join("");
  $("#result-insights").innerHTML = renderInsights(plan);
}

function renderDaySummary(day) {
  const restCount = (day.nodes || []).filter((item) => item.isRecoveryNode).length;
  return `
    <article class="route-card">
      <div class="route-card-head">
        <div>
          <p>Day ${day.dayNo}</p>
          <h3>${dayTitle(day)}</h3>
        </div>
        <span class="state-pill ${stateClass(day.rhythmState)}">${stateLabels[day.rhythmState]}</span>
      </div>
      <div class="day-tip">
        <span>${riskText(day.fatigueRisk)}</span>
        ${riskBar(day.fatigueRisk)}
      </div>
      <p>${restCount ? `安排了 ${restCount} 个中途休息点，让当天不至于一路赶。` : "当天路线比较顺，不需要额外休息点。"}</p>
      <div class="route-actions">
        <a class="secondary-link" href="detail.html?day=${day.dayNo}">查看当天详情</a>
      </div>
    </article>
  `;
}

function renderDetailPage() {
  const dayNo = Number(new URLSearchParams(location.search).get("day")) || 1;
  const day = app.plan.days.find((item) => item.dayNo === dayNo) || app.plan.days[0];
  $("#detail-heading").innerHTML = `
    <p class="eyebrow">Day ${day.dayNo}</p>
    <h1>${dayTitle(day)}</h1>
    <p>${riskText(day.fatigueRisk)}，全程约 ${round(day.totalHours)} 小时，交通约 ${Math.round(day.travelMinutes)} 分钟。</p>
  `;
  renderMiniMap("#detail-map", { ...app.plan, days: [day] });
  $("#detail-timeline").innerHTML = (day.nodes || []).map(renderTimelineNode).join("");
  $("#detail-explain").innerHTML = `
    <article class="insight-card">
      <h3>为什么这样安排</h3>
      <p>${day.interventions?.length ? day.interventions.join(" ") : "当天景点之间移动比较顺，所以以路线连贯和游玩体验为主。"}</p>
      ${riskBar(day.fatigueRisk)}
    </article>
  `;
}

function renderTimelineNode(nodeItem) {
  const item = nodeItem.item?.spot || nodeItem.spot || {};
  return `
    <article class="timeline-card ${nodeItem.isRecoveryNode ? "rest" : ""}">
      <time>${nodeItem.arriveTime}<span>${nodeItem.leaveTime}</span></time>
      <div>
        <div class="node-title">
          <h3>${item.name}</h3>
          <span>${nodeItem.isRecoveryNode ? "中途休息点" : item.category || "景点"}</span>
        </div>
        <p>${nodeItem.isRecoveryNode ? restReason(nodeItem) : naturalReason(nodeItem)}</p>
        <em>${stateLabels[nodeItem.stateBefore]} 到 ${stateLabels[nodeItem.stateAfter]} · ${riskText(nodeItem.fatigueRisk)}</em>
      </div>
    </article>
  `;
}

function renderAdjustPage() {
  const plan = app.plan;
  renderMiniMap("#adjust-map", plan);
  $("#adjust-summary").innerHTML = `
    <h1>把路线调到更适合你</h1>
    <p>当前是 ${paceLabels[plan.demand.pace]}，共 ${plan.days.length} 天。你可以直接放慢节奏、增加休息点，或删掉不想去的地方。</p>
  `;
  const selectedOptions = plan.selected.map((item) => `<option value="${item.spot.id}">${item.spot.name}</option>`).join("");
  $("#adjust-form").innerHTML = `
    <div class="quick-actions large">
      <button type="button" data-action="lighter">改轻松一点</button>
      <button type="button" data-action="moreRecovery">增加休息点</button>
      <button type="button" data-action="lessWalking">减少步行</button>
      <button type="button" data-action="shortenDay">缩短当天路线</button>
    </div>
    <label>
      预算
      <input id="adjust-budget" type="number" min="300" step="100" value="${plan.demand.budget}" />
    </label>
    <label>
      旅行风格
      <select id="adjust-pace">
        <option value="relaxed" ${plan.demand.pace === "relaxed" ? "selected" : ""}>轻松慢游</option>
        <option value="balanced" ${plan.demand.pace === "balanced" ? "selected" : ""}>均衡体验</option>
        <option value="compact" ${plan.demand.pace === "compact" ? "selected" : ""}>高效打卡</option>
      </select>
    </label>
    <label>
      删除一个不想去的地方
      <select id="remove-spot"><option value="">不删除</option>${selectedOptions}</select>
    </label>
    <button class="primary-btn" type="submit">重新生成路线</button>
  `;
  $$(".quick-actions button").forEach((button) => button.addEventListener("click", () => submitReplan(button.dataset.action)));
  $("#adjust-form").addEventListener("submit", (event) => {
    event.preventDefault();
    submitReplan("");
  });
}

function renderInsights(plan) {
  return `
    <section class="insight-card">
      <p class="eyebrow">Why This Route</p>
      <h3>为什么这样安排</h3>
      <div class="insight-list">
        ${plan.days.map((day) => `<p><strong>Day ${day.dayNo}</strong>${day.interventions?.length ? day.interventions.join(" ") : "当天移动比较顺，不需要额外加休息。"}</p>`).join("")}
      </div>
    </section>
  `;
}

function renderMiniMap(selector, plan, options = {}) {
  const container = $(selector);
  if (!container) return;
  const nodes = plan ? allNodes(plan) : mockSpots.slice(0, 8).map((item, index) => node({ spot: item, score: 0.7, reason: item.description }, 540 + index * 50, 45, "RELAXED", "STABLE", 30, item.isRecoveryNode, item.description));
  container.innerHTML = `
    <div class="map-canvas">
      <div class="map-river"></div>
      <div class="map-road road-a"></div>
      <div class="map-road road-b"></div>
      ${nodes.map((nodeItem, index) => renderMapPin(nodeItem, index, options)).join("")}
      <div class="map-legend">
        <span><i></i>景点</span>
        <span><i class="rest-dot"></i>中途休息</span>
      </div>
    </div>
  `;
}

function renderMapPin(nodeItem, index) {
  const item = nodeItem.item?.spot || nodeItem.spot || {};
  const x = item.x || 50;
  const y = item.y || 50;
  return `
    <button class="map-pin ${nodeItem.isRecoveryNode || isRecovery(item) ? "rest" : ""}" style="left:${x}%;top:${y}%;" type="button" title="${item.name}">
      <span>${index + 1}</span>
      <strong>${item.name}</strong>
    </button>
  `;
}

function loadDemo() {
  $("#rawText").value = "我想去成都玩 3 天，预算 1600，喜欢历史、美食和拍照，希望不要太累，中午能安排茶馆休息，也不想排太久队。";
  $("#destination").value = "成都";
  $("#days").value = 3;
  $("#budget").value = 1600;
  $("#constraints").value = "中午休息，避免连续安排太累的地方";
  app.selectedInterests = ["历史", "美食", "拍照"];
  app.rhythm = { pace: "balanced", walkingTolerance: "medium", crowdSensitivity: "high", comfortPreference: "comfort" };
  $("#needNoonRest").checked = true;
  syncSegmented();
  renderInterestChips();
  renderPlannerMap();
}

async function parseDemand() {
  const text = $("#rawText").value.trim();
  if (!text) return;
  try {
    const body = await postJson(`${API_BASE}/demand/parse`, { text });
    if (body.success) {
      applyParsedDemand(body.data);
      return;
    }
  } catch {
    // Backend is optional for static preview.
  }
  applyParsedDemand(localParse(text));
}

function localParse(text) {
  const matchBudget = text.match(/预算\s*(\d+)|(\d+)\s*元/);
  return {
    destination: "成都",
    days: Number((text.match(/(\d+)\s*天/) || [])[1]) || 3,
    budget: Number(matchBudget?.[1] || matchBudget?.[2]) || 1600,
    interests: interests.filter((item) => text.includes(item)),
    pace: text.includes("轻松") || text.includes("不累") || text.includes("慢") ? "relaxed" : text.includes("多玩") || text.includes("打卡") || text.includes("紧凑") ? "compact" : "balanced",
    walkingTolerance: text.includes("少走路") ? "low" : "medium",
    crowdSensitivity: text.includes("排队") || text.includes("拥挤") || text.includes("人多") ? "high" : "medium",
    comfortPreference: text.includes("多玩") || text.includes("打卡") ? "coverage" : "comfort",
    needNoonRest: text.includes("午休") || text.includes("休息") || text.includes("茶馆") || text.includes("咖啡"),
    constraints: text,
  };
}

function applyParsedDemand(data) {
  $("#destination").value = data.destination || "成都";
  $("#days").value = data.days || 3;
  $("#budget").value = data.budget || 1600;
  $("#constraints").value = data.constraints || $("#constraints").value;
  app.selectedInterests = data.interests?.length ? data.interests : app.selectedInterests;
  app.rhythm.pace = data.pace || app.rhythm.pace;
  app.rhythm.walkingTolerance = data.walkingTolerance || app.rhythm.walkingTolerance;
  app.rhythm.crowdSensitivity = data.crowdSensitivity || app.rhythm.crowdSensitivity;
  app.rhythm.comfortPreference = data.comfortPreference || app.rhythm.comfortPreference;
  $("#needNoonRest").checked = Boolean(data.needNoonRest);
  syncSegmented();
  renderInterestChips();
  renderPlannerMap();
}

function syncSegmented() {
  $$(".segmented").forEach((group) => {
    group.querySelectorAll("button").forEach((button) => button.classList.toggle("active", app.rhythm[group.dataset.group] === button.dataset.value));
  });
}

function collectDemand(options = {}) {
  const fallback = defaultDemand();
  return {
    rawText: $("#rawText")?.value.trim() || fallback.rawText,
    destination: $("#destination")?.value.trim() || "成都",
    days: Number($("#days")?.value) || fallback.days,
    budget: Number($("#budget")?.value) || fallback.budget,
    interests: app.selectedInterests.length ? app.selectedInterests : ["历史"],
    pace: app.rhythm.pace,
    constraints: $("#constraints")?.value.trim() || "",
    walkingTolerance: app.rhythm.walkingTolerance,
    needNoonRest: $("#needNoonRest")?.checked ?? true,
    crowdSensitivity: app.rhythm.crowdSensitivity,
    comfortPreference: app.rhythm.comfortPreference,
    quiet: options.quiet,
  };
}

async function submitGenerate() {
  const demand = collectDemand();
  try {
    const body = await postJson(`${API_BASE}/plans/generate`, demand);
    app.plan = body.success ? normalizeRemotePlan(body.data) : generateMockPlan(demand);
  } catch {
    app.plan = generateMockPlan(demand);
  }
  savePlan(app.plan);
  location.href = "result.html";
}

async function submitReplan(adjustmentType = "") {
  const demand = app.plan?.demand || defaultDemand();
  const payload = {
    demand,
    parentPlanId: app.plan?.parentPlanId || app.plan?.id || null,
    versionNo: app.plan?.versionNo || 1,
    newBudget: Number($("#adjust-budget")?.value) || demand.budget,
    newPace: $("#adjust-pace")?.value || demand.pace,
    adjustmentType,
    removeSpotId: Number($("#remove-spot")?.value) || null,
  };
  try {
    const body = await postJson(`${API_BASE}/plans/replan`, payload);
    app.plan = body.success ? normalizeRemotePlan(body.data) : generateMockPlan({ ...payload.demand, budget: payload.newBudget, pace: payload.newPace }, payload);
  } catch {
    app.plan = generateMockPlan({ ...payload.demand, budget: payload.newBudget, pace: payload.newPace }, payload);
  }
  savePlan(app.plan);
  location.href = "result.html";
}

function normalizeRemotePlan(plan) {
  return {
    ...plan,
    days: (plan.days || []).map((day) => ({
      ...day,
      rhythmState: day.rhythmState || stateFromRisk(day.fatigueRisk || day.fatigue?.score || 30),
      fatigueRisk: day.fatigueRisk ?? Math.round(day.fatigue?.score || 30),
      nodes: (day.nodes || []).map((nodeItem) => {
        const spotItem = nodeItem.item?.spot || nodeItem.spot || {};
        const local = mockSpots.find((item) => item.name === spotItem.name || item.id === spotItem.id);
        return { ...nodeItem, item: { ...(nodeItem.item || {}), spot: { ...spotItem, x: local?.x || 50, y: local?.y || 50 } } };
      }),
    })),
  };
}

async function postJson(url, payload) {
  const response = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
  return response.json();
}

function generateMockPlan(inputDemand, options = {}) {
  const demand = normalizeDemand(inputDemand, options);
  const candidates = app.spots
    .filter((item) => item.city === demand.destination && !isRecovery(item) && item.id !== Number(options.removeSpotId))
    .map((item) => ({ spot: item, score: scoreSpot(item, demand), reason: reasonSpot(item, demand) }))
    .sort((a, b) => b.score - a.score || b.spot.popularity - a.spot.popularity);
  const maxSpots = demand.pace === "compact" ? 4 : demand.pace === "relaxed" ? 2 : 3;
  const selected = selectMockSpotsByBudget(candidates, demand, maxSpots, options);
  const days = Array.from({ length: demand.days }, (_, index) => ({ dayNo: index + 1, items: [] }));
  selected.forEach((item, index) => {
    if (days[index % days.length].items.length < maxSpots) days[index % days.length].items.push(item);
  });

  const plannedDays = days.map((day) => buildMockDay(day, demand));
  const totalTicket = selected.reduce((sum, item) => sum + item.spot.ticketPrice, 0);
  const localSpend = selected.reduce((sum, item) => sum + estimatedMockLocalSpend(item.spot, demand), 0);
  const transportCost = plannedDays.reduce((sum, day) => sum + Math.round(day.travelMinutes * mockTransportRate(demand)), 0);
  const totalCost = totalTicket + localSpend + transportCost + demand.days * mockDailyBaseSpend(demand);
  const totalHours = plannedDays.reduce((sum, day) => sum + day.totalHours, 0);
  const match = selected.length ? Math.round((selected.reduce((sum, item) => sum + item.score, 0) / selected.length) * 100) : 0;
  const restCount = plannedDays.flatMap((day) => day.nodes).filter((nodeItem) => nodeItem.isRecoveryNode).length;
  return {
    id: `ROUTE-${Date.now().toString().slice(-5)}`,
    demand,
    selected,
    days: plannedDays,
    totalCost,
    totalHours: round(totalHours),
    match,
    summary: `${demand.destination}${demand.days}日路线：安排 ${selected.length} 个主要地点，穿插 ${restCount} 个中途休息点。`,
    explanations: [
      "优先把想玩的地方串起来，再根据步行、排队和当天节奏插入休息。",
      `当前预算为 ${demand.budget} 元，已按${budgetTierLabel(demand)}控制景点数量、门票、餐饮和交通估算。`,
    ],
    parentPlanId: options.parentPlanId || null,
    versionNo: (options.versionNo || 0) + 1,
  };
}

function selectMockSpotsByBudget(candidates, demand, maxSpots, options = {}) {
  const targetCount = mockBudgetSpotLimit(demand, maxSpots);
  const fixedCost = demand.days * mockDailyBaseSpend(demand);
  const softLimit = Math.max(80, demand.budget - fixedCost);
  const ordered = [...candidates].sort((a, b) => budgetValue(b, demand) - budgetValue(a, demand));
  const selected = [];
  let estimatedCost = 0;

  ordered.forEach((item) => {
    if (selected.length >= targetCount) return;
    const cost = estimatedMockSpotCost(item.spot, demand);
    const mustKeep = Number(options.mustSpotId) === item.spot.id;
    if (mustKeep || estimatedCost + cost <= softLimit) {
      selected.push(item);
      estimatedCost += cost;
    }
  });

  const minimum = Math.min(candidates.length, mockMinimumSpotCount(demand));
  ordered.forEach((item) => {
    if (selected.length >= minimum) return;
    if (!selected.some((existing) => existing.spot.id === item.spot.id)) selected.push(item);
  });

  return selected.sort((a, b) => b.score - a.score || b.spot.popularity - a.spot.popularity);
}

function mockBudgetSpotLimit(demand, maxSpots) {
  const dayBudget = demand.budget / Math.max(demand.days, 1);
  if (demand.budget < 500) return 1;
  if (demand.budget < 900) return Math.max(1, demand.days);
  if (dayBudget < 450) return Math.max(1, demand.days * 2);
  if (dayBudget < 700) return demand.days * Math.min(3, maxSpots);
  if (dayBudget > 1200) return demand.days * maxSpots + Math.max(1, demand.days - 1);
  return Math.max(demand.days * 2, demand.days * maxSpots - 1);
}

function mockMinimumSpotCount(demand) {
  if (demand.budget < 500) return 1;
  if (demand.budget < 900) return Math.max(1, demand.days);
  return Math.max(1, demand.days * 2);
}

function budgetValue(item, demand) {
  return item.score / Math.max(estimatedMockSpotCost(item.spot, demand), 30);
}

function estimatedMockSpotCost(item, demand) {
  return item.ticketPrice + estimatedMockLocalSpend(item, demand) + mockMoveCost(demand);
}

function estimatedMockLocalSpend(item, demand) {
  const dayBudget = demand.budget / Math.max(demand.days, 1);
  const factor = demand.budget < 500 ? 0.35 : dayBudget < 300 ? 0.55 : dayBudget < 450 ? 0.72 : dayBudget < 700 ? 0.92 : dayBudget > 1200 ? 1.28 : 1;
  return Math.round(baseMockSpend(item) * factor);
}

function baseMockSpend(item) {
  const text = `${item.category || ""} ${(item.tags || []).join(" ")}`;
  if (text.includes("美食")) return 100;
  if (text.includes("茶馆") || text.includes("咖啡")) return 55;
  if (text.includes("购物") || text.includes("商圈")) return 120;
  if (text.includes("博物馆") || text.includes("历史")) return 45;
  if (text.includes("公园") || text.includes("休闲")) return 30;
  return 35;
}

function mockDailyBaseSpend(demand) {
  const dayBudget = demand.budget / Math.max(demand.days, 1);
  if (demand.budget < 500) return 35;
  if (dayBudget < 300) return 65;
  if (dayBudget < 450) return 70;
  if (dayBudget < 700) return 110;
  if (dayBudget > 1200) return 190;
  return 140;
}

function mockMoveCost(demand) {
  const dayBudget = demand.budget / Math.max(demand.days, 1);
  if (demand.budget < 500) return 8;
  if (dayBudget < 300) return 12;
  return dayBudget < 450 ? 18 : 32;
}

function mockTransportRate(demand) {
  const dayBudget = demand.budget / Math.max(demand.days, 1);
  if (demand.budget < 500) return 0.2;
  if (dayBudget < 300) return 0.35;
  if (dayBudget < 450) return 0.45;
  if (dayBudget < 700) return 0.62;
  if (dayBudget > 1200) return 0.95;
  return 0.78;
}

function budgetTierLabel(demand) {
  const dayBudget = demand.budget / Math.max(demand.days, 1);
  if (demand.budget < 500) return "极简预算";
  if (demand.budget < 900) return "经济型预算";
  if (dayBudget < 450) return "经济型预算";
  if (dayBudget < 700) return "均衡型预算";
  if (dayBudget > 1200) return "舒适型预算";
  return "标准预算";
}

function buildMockDay(day, demand) {
  let risk = demand.pace === "relaxed" ? 16 : demand.pace === "compact" ? 28 : 22;
  let minute = 9 * 60;
  let travel = 20;
  const nodes = [];
  const interventions = [];
  const ordered = [...day.items].sort((a, b) => a.spot.id - b.spot.id);

  ordered.forEach((item, index) => {
    const transfer = index === 0 ? 20 : travelMinutes[ordered[index - 1].spot.id - 1][item.spot.id - 1];
    if (risk + loadDelta(item.spot, demand, transfer) > 72 || (index > 0 && highLoad(ordered[index - 1].spot) && highLoad(item.spot))) {
      const recovery = nearestRecovery(item.spot, nodes);
      if (recovery) {
        const before = stateFromRisk(risk);
        minute += 12;
        risk = Math.max(16, risk - recovery.recoveryValue * 45);
        nodes.push(node({ spot: recovery, score: 0.8, reason: "这里适合喝茶、散步或坐一会儿，让后半天更舒服。" }, minute, 40, before, stateFromRisk(risk), risk, true, "前面安排较密，先在这里放慢一点。"));
        minute += 40;
        interventions.push(`在 ${item.spot.name} 前加了 ${recovery.name}，避免连续游玩太赶。`);
        travel += 12;
      }
    }
    minute += transfer;
    const before = stateFromRisk(risk);
    risk += loadDelta(item.spot, demand, transfer);
    const stay = Math.round(item.spot.stayDuration * 60);
    nodes.push(node(item, minute, stay, before, stateFromRisk(risk), risk, false, displaySpotReason(item.spot, demand)));
    minute += stay;
    travel += transfer;
  });

  return {
    dayNo: day.dayNo,
    spots: ordered,
    nodes,
    travelMinutes: travel,
    totalHours: round((minute - 9 * 60) / 60),
    fatigue: { label: stateLabels[stateFromRisk(risk)], level: risk > 86 ? "critical" : risk > 68 ? "high" : risk > 38 ? "medium" : "low", score: round(risk) },
    rhythmState: stateFromRisk(risk),
    fatigueRisk: Math.min(100, Math.round(risk)),
    routeReason: "按照相邻距离和当天强度重新平衡路线。",
    interventions,
  };
}

function normalizeDemand(input, options = {}) {
  const demand = { ...input };
  if (options.adjustmentType === "lighter") {
    demand.pace = "relaxed";
    demand.walkingTolerance = "low";
    demand.needNoonRest = true;
    demand.comfortPreference = "comfort";
  }
  if (options.adjustmentType === "moreRecovery") demand.needNoonRest = true;
  if (options.adjustmentType === "lessWalking") demand.walkingTolerance = "low";
  if (options.adjustmentType === "shortenDay") demand.pace = "relaxed";
  if (options.newPace) demand.pace = options.newPace;
  return demand;
}

function node(item, arrive, stayMinutes, before, after, risk, isRecoveryNode, reason) {
  return {
    item,
    arriveTime: time(arrive),
    leaveTime: time(arrive + stayMinutes),
    stateBefore: before,
    stateAfter: after,
    fatigueRisk: round(risk),
    isRecoveryNode,
    reason,
    suggestedStayMinutes: stayMinutes,
  };
}

function defaultDemand() {
  return {
    rawText: "",
    destination: "成都",
    days: 3,
    budget: 1600,
    interests: ["历史", "美食", "拍照"],
    pace: "balanced",
    constraints: "",
    walkingTolerance: "medium",
    needNoonRest: true,
    crowdSensitivity: "medium",
    comfortPreference: "comfort",
  };
}

function savePlan(plan) {
  localStorage.setItem(PLAN_KEY, JSON.stringify(plan));
}

function loadPlan() {
  try {
    return JSON.parse(localStorage.getItem(PLAN_KEY));
  } catch {
    return null;
  }
}

function allNodes(plan) {
  return (plan?.days || []).flatMap((day) => day.nodes || []);
}

function scoreSpot(item, demand) {
  const matched = demand.interests.filter((interest) => item.tags?.includes(interest)).length / Math.max(demand.interests.length, 1);
  const budget = mockBudgetScore(item, demand);
  let penalty = 0;
  if (demand.walkingTolerance === "low") penalty += item.physicalLoad * 0.1;
  if (demand.crowdSensitivity === "high") penalty += (item.crowdLoad + item.queueLoad) * 0.08;
  if (demand.comfortPreference === "comfort") penalty += (item.physicalLoad + item.cognitiveLoad + item.crowdLoad + item.queueLoad) * 0.03;
  return Math.max(0.2, Math.min(0.99, matched * 0.48 + (item.popularity / 100) * 0.18 + budget * 0.26 - penalty));
}

function reasonSpot(item, demand) {
  const matched = demand.interests.filter((interest) => item.tags?.includes(interest));
  const budgetText = mockBudgetScore(item, demand) >= 0.8 ? "预计花费符合预算" : "预计花费偏高，已降低排序";
  return matched.length ? `符合你想看的 ${matched.join("、")}，${budgetText}。` : `作为同城补充地点，${budgetText}。`;
}

function mockBudgetScore(item, demand) {
  const dayBudget = demand.budget / Math.max(demand.days, 1);
  const visitCost = estimatedMockSpotCost(item, demand);
  if (visitCost <= dayBudget * 0.18) return 1;
  if (visitCost <= dayBudget * 0.3) return 0.82;
  if (visitCost <= dayBudget * 0.45) return 0.62;
  return 0.36;
}

function displaySpotReason(item, demand) {
  if (item.crowdLoad > 0.72 && demand.crowdSensitivity === "high") return "这里人气高，所以没有连续安排太多同类地点。";
  if (item.cognitiveLoad > 0.68) return "这里适合慢慢看，后面会留出缓冲时间。";
  if (item.physicalLoad > 0.52 && demand.walkingTolerance === "low") return "这里会有一些步行，路线会在前后安排得更松。";
  return "和前后地点顺路，游玩强度比较可控。";
}

function loadDelta(item, demand, transfer) {
  let value = item.physicalLoad * 22 + item.cognitiveLoad * 16 + item.crowdLoad * 14 + item.queueLoad * 12 + transfer * 0.18;
  if (demand.walkingTolerance === "low") value += item.physicalLoad * 14;
  if (demand.crowdSensitivity === "high") value += (item.crowdLoad + item.queueLoad) * 10;
  if (demand.pace === "relaxed") value *= 1.1;
  return value;
}

function nearestRecovery(target, existingNodes) {
  return app.spots
    .filter((item) => isRecovery(item) && !existingNodes.some((nodeItem) => nodeItem.item.spot.id === item.id))
    .sort((a, b) => travelMinutes[target.id - 1][a.id - 1] - travelMinutes[target.id - 1][b.id - 1])[0];
}

function highLoad(item) {
  return item.physicalLoad + item.cognitiveLoad + item.crowdLoad + item.queueLoad >= 2.05;
}

function stateFromRisk(risk) {
  if (risk < 34) return "RELAXED";
  if (risk < 62) return "STABLE";
  if (risk < 86) return "FATIGUED";
  return "OVERLOADED";
}

function dayTitle(day) {
  const names = (day.nodes || []).filter((nodeItem) => !nodeItem.isRecoveryNode).slice(0, 2).map((nodeItem) => nodeItem.item?.spot?.name).filter(Boolean);
  return names.length ? names.join(" + ") : "自由探索";
}

function restReason(nodeItem) {
  return nodeItem.reason || "这里适合坐下来休息，让后面的行程不那么赶。";
}

function naturalReason(nodeItem) {
  return nodeItem.reason || "和前后地点顺路，适合安排在这个时间段。";
}

function riskText(value = 0) {
  if (value < 38) return "今天比较轻松";
  if (value < 68) return "节奏平稳";
  if (value < 86) return "略微偏赶";
  return "建议放慢一点";
}

function riskBar(value) {
  return `<div class="risk-bar"><span style="width:${Math.min(100, Math.max(0, value))}%"></span><strong>${Math.round(value)}%</strong></div>`;
}

function stateClass(value) {
  return String(value || "").toLowerCase();
}

function isRecovery(item) {
  return Boolean(item?.isRecoveryNode ?? item?.recoveryNode);
}

function time(minute) {
  const value = minute % (24 * 60);
  return `${String(Math.floor(value / 60)).padStart(2, "0")}:${String(value % 60).padStart(2, "0")}`;
}

function round(value) {
  return Math.round(value * 10) / 10;
}

// ===== 可调节分栏功能 =====
function initResizable() {
  const resizer = document.getElementById('resizer');
  const leftPanel = document.getElementById('left-panel');
  const container = document.getElementById('resizable-section');
  
  if (!resizer || !leftPanel || !container) return;
  
  let isResizing = false;
  let startX = 0;
  let startLeftWidth = 0;
  
  resizer.addEventListener('mousedown', function(e) {
    e.preventDefault();
    isResizing = true;
    startX = e.clientX;
    startLeftWidth = leftPanel.offsetWidth;
    resizer.classList.add('resizing');
    document.body.style.cursor = 'col-resize';
    document.body.style.userSelect = 'none';
    
    const overlay = document.createElement('div');
    overlay.className = 'resizing-overlay';
    overlay.id = 'resizing-overlay';
    document.body.appendChild(overlay);
  });
  
  document.addEventListener('mousemove', function(e) {
    if (!isResizing) return;
    const deltaX = e.clientX - startX;
    let newWidth = startLeftWidth + deltaX;
    const containerWidth = container.offsetWidth;
    
    if (newWidth < 300) newWidth = 300;
    if (newWidth > containerWidth - 400) newWidth = containerWidth - 400;
    
    leftPanel.style.flex = '0 0 ' + newWidth + 'px';
  });
  
  document.addEventListener('mouseup', function() {
    if (!isResizing) return;
    isResizing = false;
    resizer.classList.remove('resizing');
    document.body.style.cursor = '';
    document.body.style.userSelect = '';
    
    const overlay = document.getElementById('resizing-overlay');
    if (overlay) overlay.remove();
  });
  
  // 双击重置
  resizer.addEventListener('dblclick', function() {
    leftPanel.style.flex = '0 0 430px';
  });
}

init();
