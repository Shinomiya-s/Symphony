<template>
  <div class="symphony-gallery">
    <!-- 导航栏 -->
    <nav class="gallery-nav">
      <button class="nav-back" @click="$router.back()">
        <i class="fas fa-arrow-left"></i>
        <span>返回</span>
      </button>
      <div class="nav-center">
        <div class="nav-brand">协作文档归档</div>
        <div v-if="userA && userB" class="nav-collaborators">
          <span class="collab-name">{{ userA }}</span>
          <span class="collab-sep">×</span>
          <span class="collab-name">{{ userB }}</span>
        </div>
      </div>
      <div class="nav-meta">
        <span v-if="docs.length > 0" class="doc-counter">{{ docs.length }} 篇文档</span>
        <div v-else style="width: 100px;"></div>
      </div>
    </nav>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <div class="search-container">
        <div class="search-input-group">
          <i class="fas fa-search search-icon"></i>
          <input
              v-model="searchKeyword"
              @input="handleSearch"
              @keyup.enter="handleSearch"
              type="text"
              placeholder="搜索正文或 AI 摘要..."
              class="search-input"
          />
          <button
              v-if="searchKeyword"
              @click="clearSearch"
              class="search-clear"
          >
            <i class="fas fa-times"></i>
          </button>
        </div>

        <div class="search-controls">
          <label class="control-label">排序：</label>
          <select v-model="sortBy" @change="handleSearch" class="sort-select">
            <option value="time">时间</option>
            <option value="length">字数</option>
          </select>

          <button @click="toggleSortOrder" class="sort-toggle">
            <i v-if="sortOrder === 'desc'" class="fas fa-sort-amount-down"></i>
            <i v-else class="fas fa-sort-amount-up"></i>
          </button>
        </div>
      </div>

      <!-- 搜索结果提示 -->
      <div v-if="isSearchMode" class="search-info">
        <span class="info-text">
          共找到 <strong>{{ searchResults.length }}</strong> 条结果
        </span>
        <button @click="clearSearch" class="info-clear-btn">
          返回日历视图
        </button>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <div class="loading-orbs">
        <div class="orb"></div>
        <div class="orb"></div>
        <div class="orb"></div>
      </div>
      <p class="loading-text">加载归档中...</p>
    </div>

    <!-- 空状态 -->
    <div v-else-if="docs.length === 0" class="empty-container">
      <div class="empty-icon">
        <i class="fas fa-archive"></i>
      </div>
      <div class="empty-title">暂无归档文档</div>
      <p class="empty-subtitle">开始协作并保存第一篇文档吧</p>
    </div>

    <!-- 主内容 -->
    <div v-else class="gallery-container">
      <!-- 左侧：日历 + 统计 -->
      <aside class="calendar-sidebar">
        <!-- 日历 -->
        <div v-if="!isSearchMode" class="calendar-widget">
          <div class="calendar-header">
            <button class="month-nav" @click="prevMonth">
              <i class="fas fa-chevron-left"></i>
            </button>
            <div class="calendar-title">
              {{ calYear }} 年 {{ String(calMonth + 1).padStart(2, '0') }} 月
            </div>
            <button class="month-nav" @click="nextMonth">
              <i class="fas fa-chevron-right"></i>
            </button>
          </div>

          <div class="calendar-weekdays">
            <span v-for="day in ['日','一','二','三','四','五','六']" :key="day">{{ day }}</span>
          </div>

          <div class="calendar-grid">
            <div
                v-for="(cell, idx) in calCells"
                :key="idx"
                :class="[
                  'calendar-cell',
                  { 'other-month': !cell.currentMonth },
                  { 'has-docs': cell.hasDoc },
                  { 'selected': cell.dateStr === selectedDate },
                  { 'today': cell.dateStr === todayStr }
                ]"
                @click="cell.hasDoc && selectDate(cell.dateStr)"
            >
              <span class="cell-date">{{ cell.day }}</span>
              <span v-if="cell.hasDoc" class="cell-indicator">
                {{ docsByDate[cell.dateStr]?.length }}
              </span>
            </div>
          </div>

          <div class="calendar-legend">
            <div class="legend-row">
              <span class="legend-marker active"></span>
              <span class="legend-label">有文档</span>
            </div>
            <div class="legend-row">
              <span class="legend-marker today"></span>
              <span class="legend-label">今天</span>
            </div>
          </div>
        </div>

        <!-- 统计面板 -->
        <div class="stats-panel">
          <div class="stat-item">
            <div class="stat-value">{{ displayDocs.length }}</div>
            <div class="stat-label">{{ isSearchMode ? '结果数' : '总计' }}</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ isSearchMode ? searchResults.length : Object.keys(docsByDate).length }}</div>
            <div class="stat-label">{{ isSearchMode ? '匹配数' : '活跃天' }}</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ displayDocs.filter(d => d.aiSummary).length }}</div>
            <div class="stat-label">已摘要</div>
          </div>
        </div>
      </aside>

      <!-- 右侧:文档列表 -->
      <main class="documents-main">
        <!-- 搜索模式 -->
        <template v-if="isSearchMode">
          <div class="documents-header">
            <div class="header-date">
              <i class="fas fa-search"></i>
              搜索结果
            </div>
            <div class="header-count">{{ searchResults.length }} 篇文档</div>
          </div>

          <div v-if="searchResults.length === 0" class="empty-search">
            <i class="fas fa-search"></i>
            <p>没有找到匹配的文档</p>
          </div>

          <div v-else class="documents-list">
            <div
                v-for="(doc, index) in searchResults"
                :key="doc.id"
                class="document-card"
                :style="{ '--index': index }"
                @click="openDoc(doc)"
            >
              <div class="card-header">
                <span class="card-time">
                  <i class="fas fa-clock"></i>
                  {{ formatDate(doc.createdAt) }}
                </span>
                <span class="card-length">
                  {{ doc.content?.length || 0 }} 字
                </span>
              </div>

              <div class="card-preview" v-html="highlightKeyword(getPreview(doc.content, 150))"></div>

              <div v-if="doc.aiSummary" class="card-ai-summary">
                <div class="summary-header">
                  <i class="fas fa-robot"></i>
                  AI 摘要
                </div>
                <div class="summary-content" v-html="highlightKeyword(getPreview(doc.aiSummary, 100))"></div>
              </div>

              <div class="card-footer">
                <button class="view-full-btn" @click.stop="openDoc(doc)">
                  查看全文
                  <i class="fas fa-arrow-right"></i>
                </button>
              </div>
            </div>
          </div>
        </template>

        <!-- 日历模式 -->
        <template v-else>
          <!-- 未选中日期 -->
          <div v-if="!selectedDate" class="select-prompt">
            <div class="prompt-icon">
              <i class="fas fa-calendar-alt"></i>
            </div>
            <div class="prompt-text">请选择日期</div>
            <div class="prompt-hint">点击日历上有文档的日期以查看内容</div>
          </div>

          <!-- 已选中日期 -->
          <template v-else>
            <div class="documents-header">
              <div class="header-date">
                <i class="fas fa-calendar-day"></i>
                {{ formatDateLabel(selectedDate) }}
              </div>
              <div class="header-count">{{ selectedDocs.length }} 篇文档</div>
            </div>

            <div class="documents-list">
              <div
                  v-for="(doc, index) in selectedDocs"
                  :key="doc.id"
                  class="document-card"
                  :style="{ '--index': index }"
                  @click="openDoc(doc)"
              >
                <div class="card-header">
                <span class="card-time">
                  <i class="fas fa-clock"></i>
                  {{ formatTime(doc.createdAt) }}
                </span>
                  <span class="card-length">
                  {{ doc.content?.length || 0 }} 字
                </span>
                </div>

                <div class="card-preview">{{ getPreview(doc.content, 150) }}</div>

                <div v-if="doc.aiSummary" class="card-ai-summary">
                  <div class="summary-header">
                    <i class="fas fa-robot"></i>
                    AI 摘要
                  </div>
                  <div class="summary-content">{{ getPreview(doc.aiSummary, 100) }}</div>
                </div>

                <div class="card-footer">
                  <button class="view-full-btn" @click.stop="openDoc(doc)">
                    查看全文
                    <i class="fas fa-arrow-right"></i>
                  </button>
                </div>
              </div>
            </div>
          </template>
        </template>
      </main>
    </div>

    <!-- 全文弹窗 -->
    <transition name="modal-fade">
      <div v-if="selectedDoc" class="modal-backdrop" @click.self="selectedDoc = null">
        <div class="modal-document">
          <header class="modal-doc-header">
            <div class="modal-doc-title">
              <i class="fas fa-file-alt"></i>
              {{ formatDate(selectedDoc.createdAt) }}
            </div>
            <button class="modal-close" @click="selectedDoc = null">
              <i class="fas fa-times"></i>
            </button>
          </header>

          <div v-if="selectedDoc.aiSummary" class="modal-ai-section">
            <div class="modal-ai-label">
              <i class="fas fa-robot"></i>
              AI 摘要
            </div>
            <div class="modal-ai-text">{{ selectedDoc.aiSummary }}</div>
          </div>

          <div class="modal-doc-body">
            <pre class="modal-doc-content">{{ selectedDoc.content }}</pre>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const roomId = route.params.roomId
const docs = ref([])
const loading = ref(true)
const selectedDate = ref(null)
const selectedDoc = ref(null)
const userA = ref('')
const userB = ref('')

// 搜索相关状态
const searchKeyword = ref('')
const searchResults = ref([])
const isSearchMode = ref(false)
const sortBy = ref('time')
const sortOrder = ref('desc')

const now = new Date()
const calYear = ref(now.getFullYear())
const calMonth = ref(now.getMonth())
const todayStr = `${now.getFullYear()}-${String(now.getMonth()+1).padStart(2,'0')}-${String(now.getDate()).padStart(2,'0')}`

//把文档按日期分组
const docsByDate = computed(() => {
  const map = {}
  docs.value.forEach(doc => {
    const d = new Date(doc.createdAt)
    //结果格式:{ "2026-04-01": [doc1, doc2], "2026-04-02": [doc3] }
    const key = `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`
    if (!map[key]) map[key] = []
    map[key].push(doc)
  })
  return map
})

//当前选中日期的文档列表
const selectedDocs = computed(() => {
  if (!selectedDate.value) return []
  return docsByDate.value[selectedDate.value] || []
})

//实际显示的文档列表
const displayDocs = computed(() => {
  //搜索模式显示搜索结果，否则显示全部
  return isSearchMode.value ? searchResults.value : docs.value
})

//日历格子，固定42格
const calCells = computed(() => {
  const cells = []
  const firstDay = new Date(calYear.value, calMonth.value, 1)
  const lastDay = new Date(calYear.value, calMonth.value + 1, 0)
  const startWeekday = firstDay.getDay()
  //第一行补上月末尾几天
  for (let i = startWeekday - 1; i >= 0; i--) {
    const d = new Date(calYear.value, calMonth.value, -i)
    const dateStr = toDateStr(d)
    cells.push({ day: d.getDate(), dateStr, currentMonth: false, hasDoc: !!docsByDate.value[dateStr] })
  }
  //中间是本月
  for (let i = 1; i <= lastDay.getDate(); i++) {
    const d = new Date(calYear.value, calMonth.value, i)
    const dateStr = toDateStr(d)
    cells.push({ day: i, dateStr, currentMonth: true, hasDoc: !!docsByDate.value[dateStr] })
  }
  const remaining = 42 - cells.length
  //最后补下月开头几天
  for (let i = 1; i <= remaining; i++) {
    const d = new Date(calYear.value, calMonth.value + 1, i)
    const dateStr = toDateStr(d)
    cells.push({ day: d.getDate(), dateStr, currentMonth: false, hasDoc: !!docsByDate.value[dateStr] })
  }
  return cells
})

const toDateStr = (d) =>
    `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`

const prevMonth = () => {
  if (calMonth.value === 0) { calMonth.value = 11; calYear.value-- }
  else calMonth.value--
}
const nextMonth = () => {
  if (calMonth.value === 11) { calMonth.value = 0; calYear.value++ }
  else calMonth.value++
}
const selectDate = (dateStr) => { selectedDate.value = dateStr }
const openDoc = (doc) => { selectedDoc.value = doc }

const getPreview = (content, len = 200) =>
    content?.length > len ? content.substring(0, len) + '...' : (content || '')

const formatDate = (dateStr) => {
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
}
const formatTime = (dateStr) => {
  const d = new Date(dateStr)
  return `${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
}
const formatDateLabel = (dateStr) => {
  const [y, m, day] = dateStr.split('-')
  return `${y} 年 ${m} 月 ${day} 日`
}

//调搜索接口
const handleSearch = async () => {
  if (!searchKeyword.value.trim()) {
    clearSearch()
    return
  }

  try {
    const token = localStorage.getItem('token')
    const params = new URLSearchParams({
      keyword: searchKeyword.value,
      sortBy: sortBy.value,
      sortOrder: sortOrder.value
    })

    const res = await fetch(`/api/collab/${roomId}/search?${params}`, {
      headers: { 'Authorization': 'Bearer ' + token }
    })
    const data = await res.json()

    if (data.code === 200) {
      searchResults.value = data.data
      isSearchMode.value = true
    }
  } catch (e) {
    console.error('搜索失败', e)
  }
}

//清空搜索，回到全部列表
const clearSearch = () => {
  searchKeyword.value = ''
  searchResults.value = []
  isSearchMode.value = false
}

//切换升降序
const toggleSortOrder = () => {
  sortOrder.value = sortOrder.value === 'desc' ? 'asc' : 'desc'
  if (isSearchMode.value) {
    handleSearch()
  }
}

//高亮搜索关键词
const highlightKeyword = (text) => {
  if (!searchKeyword.value || !text) return text
  const regex = new RegExp(`(${searchKeyword.value})`, 'gi')
  return text.replace(regex, '<mark class="highlight">$1</mark>')
}

//加载文档列表
const loadDocs = async () => {
  try {
    const token = localStorage.getItem('token')
    const res = await fetch(`/api/collab/${roomId}/list`, {
      headers: { 'Authorization': 'Bearer ' + token }
    })
    const data = await res.json()
    if (data.code === 200) {
      docs.value = data.data
      //默认选中最新文档所在的日期，日历也跳到那个月
      if (docs.value.length > 0) {
        const latest = new Date(docs.value[0].createdAt)
        selectedDate.value = toDateStr(latest)
        calYear.value = latest.getFullYear()
        calMonth.value = latest.getMonth()
      }
    }
  } catch (e) {
    console.error('加载失败', e)
  } finally {
    loading.value = false
  }
}

//从roomId解析出两个用户名
const loadUserNames = async () => {
  try {
    const token = localStorage.getItem('token')
    const [id1, id2] = roomId.split('_')
    const [res1, res2] = await Promise.all([
      fetch(`/api/user/id/${id1}`, { headers: { 'Authorization': 'Bearer ' + token } }),
      fetch(`/api/user/id/${id2}`, { headers: { 'Authorization': 'Bearer ' + token } })
    ])
    const [d1, d2] = await Promise.all([res1.json(), res2.json()])
    userA.value = d1.data?.username || id1
    userB.value = d2.data?.username || id2
  } catch (e) {
    const [id1, id2] = roomId.split('_')
    userA.value = id1; userB.value = id2
  }
}

//并发加载文档列表和用户名，互不依赖可以同时请求
onMounted(() => { loadDocs(); loadUserNames() })
</script>

<style>
@import url('https://fonts.googleapis.com/css2?family=Cormorant+Garamond:ital,wght@0,400;0,600;1,400;1,600&family=Inter:wght@300;400;500;600;700&display=swap');

:root {
  --symph-black: #2c3e50;
  --symph-white: #fdfdfc;
  --symph-gray: #7f8c8d;
  --symph-light-gray: #ecf0f1;
  --symph-blue: #4a90e2;
  --symph-blue-light: #64b5f6;
  --symph-blue-pale: #e3f2fd;
  --symph-blue-bg: #f0f8ff;
  --symph-border: rgba(0, 0, 0, 0.06);
  --symph-border-hover: rgba(74, 144, 226, 0.3);
  --symph-bg-subtle: #f8fafb;
  --symph-shadow-sm: 0 2px 8px rgba(0, 0, 0, 0.03);
  --symph-shadow-md: 0 4px 16px rgba(0, 0, 0, 0.05);
  --symph-shadow-lg: 0 8px 32px rgba(74, 144, 226, 0.12);
  --symph-shadow-hover: 0 12px 40px rgba(74, 144, 226, 0.15);
}
</style>

<style scoped>
* { margin: 0; padding: 0; box-sizing: border-box; }

.symphony-gallery {
  min-height: 100vh;
  background: linear-gradient(135deg, #fdfdfc 0%, #f0f8ff 40%, #e8f4f8 100%);
  font-family: 'Inter', -apple-system, sans-serif;
  color: var(--symph-black);
}

/*  搜索栏  */
.search-bar {
  background: var(--symph-white);
  border-bottom: 1px solid var(--symph-border);
  padding: 20px 60px;
}

.search-container {
  max-width: 1400px;
  margin: 0 auto;
  display: flex;
  gap: 20px;
  align-items: center;
}

.search-input-group {
  flex: 1;
  position: relative;
  display: flex;
  align-items: center;
  border: 1px solid var(--symph-border);
  border-radius: 10px;
  background: var(--symph-bg-subtle);
  transition: all 0.3s;
}

.search-input-group:focus-within {
  border-color: var(--symph-blue-light);
  background: var(--symph-white);
  box-shadow: 0 0 0 3px rgba(74, 144, 226, 0.1);
}

.search-icon {
  padding: 0 16px;
  color: var(--symph-gray);
  font-size: 14px;
}

.search-input {
  flex: 1;
  border: none;
  background: transparent;
  padding: 14px 16px 14px 0;
  font-size: 14px;
  font-family: inherit;
  outline: none;
  color: var(--symph-black);
}

.search-input::placeholder {
  color: var(--symph-gray);
  font-style: italic;
}

.search-clear {
  padding: 0 16px;
  background: transparent;
  border: none;
  color: var(--symph-gray);
  cursor: pointer;
  transition: all 0.3s;
  font-size: 14px;
}

.search-clear:hover {
  color: var(--symph-blue);
  transform: rotate(90deg);
}

.search-controls {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 18px;
  border: 1px solid var(--symph-border);
  border-radius: 10px;
  background: var(--symph-bg-subtle);
}

.control-label {
  font-size: 12px;
  letter-spacing: 0.5px;
  font-weight: 600;
  color: var(--symph-gray);
}

.sort-select {
  border: none;
  background: transparent;
  padding: 4px 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  outline: none;
  color: var(--symph-black);
  font-family: inherit;
}

.sort-toggle {
  width: 28px;
  height: 28px;
  border: 1px solid var(--symph-border);
  border-radius: 6px;
  background: transparent;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--symph-gray);
  font-size: 12px;
}

.sort-toggle:hover {
  background: var(--symph-white);
  border-color: var(--symph-blue-light);
  color: var(--symph-blue);
  transform: scale(1.05);
}

.search-info {
  max-width: 1400px;
  margin: 16px auto 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  background: linear-gradient(135deg, rgba(74, 144, 226, 0.05) 0%, rgba(0, 57, 166, 0.08) 100%);
  border: 1px solid rgba(74, 144, 226, 0.2);
  border-radius: 8px;
}

.info-text {
  font-size: 13px;
  color: var(--symph-gray);
}

.info-text strong {
  color: var(--symph-blue);
  font-weight: 700;
}

.info-clear-btn {
  padding: 6px 16px;
  background: transparent;
  border: 1px solid var(--symph-blue-light);
  border-radius: 6px;
  color: var(--symph-blue);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  font-family: inherit;
}

.info-clear-btn:hover {
  background: var(--symph-blue);
  color: var(--symph-white);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(74, 144, 226, 0.2);
}

:deep(.highlight) {
  background: linear-gradient(135deg, rgba(74, 144, 226, 0.2) 0%, rgba(0, 57, 166, 0.25) 100%);
  color: var(--symph-black);
  font-weight: 600;
  padding: 2px 4px;
  border-radius: 3px;
}

.empty-search {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 300px;
  gap: 16px;
  color: var(--symph-gray);
}

.empty-search i {
  font-size: 48px;
  opacity: 0.3;
}

/* ========== 导航栏 ========== */
.gallery-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 60px;
  border-bottom: 1px solid var(--symph-border);
  background: var(--symph-white);
}

.nav-back {
  display: flex;
  align-items: center;
  gap: 10px;
  background: transparent;
  border: 1px solid var(--symph-border);
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  font-family: inherit;
}

.nav-back:hover {
  border-color: var(--symph-border-hover);
  background: var(--symph-bg-subtle);
  box-shadow: var(--symph-shadow-sm);
  transform: translateY(-1px);
}

.nav-center {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.nav-brand {
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 2px;
  color: var(--symph-black);
}

.nav-collaborators {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 11px;
  letter-spacing: 1px;
}

.collab-name {
  font-weight: 600;
  color: var(--symph-gray);
}

.collab-sep {
  font-family: 'Cormorant Garamond', serif;
  font-style: italic;
  font-size: 16px;
  color: var(--symph-blue);
}

.nav-meta {
  width: 140px;
  display: flex;
  justify-content: flex-end;
}

.doc-counter {
  font-size: 12px;
  font-weight: 600;
  color: var(--symph-gray);
  padding: 6px 14px;
  border: 1px solid var(--symph-border);
  border-radius: 16px;
  background: var(--symph-bg-subtle);
}

/* 加载状态 */
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  gap: 30px;
}

.loading-orbs {
  display: flex;
  gap: 12px;
}

.orb {
  width: 12px;
  height: 12px;
  background: var(--symph-black);
  border-radius: 50%;
  animation: orbit 1.2s ease-in-out infinite;
}

.orb:nth-child(1) { animation-delay: 0s; }
.orb:nth-child(2) { animation-delay: 0.2s; }
.orb:nth-child(3) { animation-delay: 0.4s; }

@keyframes orbit {
  0%, 100% { transform: scale(0.8); opacity: 0.5; }
  50% { transform: scale(1.2); opacity: 1; }
}

.loading-text {
  font-size: 13px;
  letter-spacing: 1px;
  font-weight: 600;
  color: var(--symph-gray);
}

/* 空状态 */
.empty-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  gap: 20px;
}

.empty-icon { font-size: 60px; color: var(--symph-light-gray); }
.empty-title { font-size: 18px; font-weight: 600; letter-spacing: 1px; color: var(--symph-black); }
.empty-subtitle { font-size: 13px; color: var(--symph-gray); }

/* 主容器 */
.gallery-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 40px 60px;
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 60px;
}

/* 左侧边栏 */
.calendar-sidebar {
  position: sticky;
  top: 40px;
  height: fit-content;
  display: flex;
  flex-direction: column;
  gap: 30px;
}

.calendar-widget {
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid var(--symph-border);
  border-radius: 12px;
  padding: 24px;
  box-shadow: var(--symph-shadow-sm);
}

.calendar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.calendar-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--symph-black);
}

.month-nav {
  width: 32px;
  height: 32px;
  border: 1px solid var(--symph-border);
  border-radius: 8px;
  background: transparent;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: var(--symph-gray);
}

.month-nav:hover {
  background: var(--symph-bg-subtle);
  border-color: var(--symph-blue-light);
  color: var(--symph-blue);
  transform: scale(1.05);
}

.calendar-weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  margin-bottom: 10px;
}

.calendar-weekdays span {
  text-align: center;
  font-size: 11px;
  font-weight: 700;
  color: var(--symph-gray);
  padding: 8px 0;
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
  margin-bottom: 20px;
}

.calendar-cell {
  aspect-ratio: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
  cursor: default;
  transition: all 0.3s;
  border-radius: 8px;
  border: 1px solid transparent;
}

.calendar-cell.has-docs {
  cursor: pointer;
  border-color: var(--symph-border);
  background: rgba(255, 255, 255, 0.5);
}

.calendar-cell.has-docs:hover {
  border-color: var(--symph-blue-light);
  background: linear-gradient(135deg, rgba(74, 127, 193, 0.05) 0%, rgba(0, 57, 166, 0.08) 100%);
  transform: scale(1.08);
}

.calendar-cell.selected {
  background: linear-gradient(135deg, var(--symph-blue) 0%, var(--symph-blue-light) 100%);
  border-color: var(--symph-blue);
  box-shadow: 0 4px 12px rgba(0, 57, 166, 0.2);
}

.calendar-cell.selected .cell-date { color: var(--symph-white); font-weight: 700; }
.calendar-cell.selected .cell-indicator { background: var(--symph-white); color: var(--symph-black); }
.calendar-cell.today:not(.selected) { border-color: var(--symph-blue); }
.calendar-cell.today .cell-date { color: var(--symph-blue); font-weight: 700; }

.cell-date { font-size: 13px; font-weight: 500; color: var(--symph-black); }
.calendar-cell.other-month .cell-date { color: var(--symph-light-gray); }
.calendar-cell.has-docs:not(.selected) .cell-date { font-weight: 600; }

.cell-indicator {
  position: absolute;
  bottom: 4px;
  font-size: 9px;
  font-weight: 700;
  color: var(--symph-white);
  background: var(--symph-black);
  width: 14px;
  height: 14px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.calendar-legend {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-top: 16px;
  border-top: 1px solid var(--symph-border);
}

.legend-row { display: flex; align-items: center; gap: 10px; }

.legend-marker {
  width: 12px;
  height: 12px;
  border-radius: 3px;
  border: 1px solid var(--symph-border);
}

.legend-marker.active {
  background: linear-gradient(135deg, var(--symph-blue) 0%, var(--symph-blue-light) 100%);
  border-color: var(--symph-blue);
}

.legend-marker.today {
  border: 2px solid var(--symph-blue-light);
  background: rgba(74, 127, 193, 0.1);
}

.legend-label { font-size: 11px; font-weight: 600; color: var(--symph-gray); }

/* 统计面板 */
.stats-panel { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }

.stat-item {
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid var(--symph-border);
  border-radius: 10px;
  padding: 16px 12px;
  text-align: center;
  transition: all 0.3s;
  box-shadow: var(--symph-shadow-sm);
}

.stat-item:hover {
  border-color: var(--symph-blue-light);
  transform: translateY(-2px);
  box-shadow: var(--symph-shadow-md);
  background: rgba(74, 127, 193, 0.05);
}

.stat-value {
  font-size: 28px;
  font-weight: 300;
  background: linear-gradient(135deg, var(--symph-blue) 0%, var(--symph-blue-light) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 6px;
}

.stat-label { font-size: 10px; letter-spacing: 0.5px; font-weight: 700; color: var(--symph-gray); }

/* 右侧主内容 */
.documents-main { min-height: 500px; }

.select-prompt {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  gap: 20px;
}

.prompt-icon { font-size: 50px; color: var(--symph-light-gray); }
.prompt-text { font-size: 16px; font-weight: 600; letter-spacing: 1px; color: var(--symph-black); }
.prompt-hint { font-size: 12px; color: var(--symph-gray); }

/* 文档列表头部 */
.documents-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  margin-bottom: 24px;
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid var(--symph-border);
  border-radius: 12px;
  box-shadow: var(--symph-shadow-sm);
}

.header-date {
  font-size: 16px;
  font-weight: 600;
  color: var(--symph-black);
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-date i { color: var(--symph-blue); }

.header-count {
  font-size: 12px;
  font-weight: 700;
  color: var(--symph-blue);
  background: linear-gradient(135deg, rgba(74, 144, 226, 0.05) 0%, rgba(0, 57, 166, 0.08) 100%);
  padding: 6px 14px;
  border-radius: 16px;
  border: 1px solid rgba(74, 144, 226, 0.2);
}

/* 文档列表 */
.documents-list { display: flex; flex-direction: column; gap: 20px; }

.document-card {
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid var(--symph-border);
  border-radius: 12px;
  padding: 24px;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: var(--symph-shadow-sm);
  animation: cardFadeIn 0.5s ease-out backwards;
  animation-delay: calc(var(--index) * 0.08s);
  position: relative;
  overflow: hidden;
}

@keyframes cardFadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.document-card::before {
  content: '';
  position: absolute;
  top: 0; left: 0;
  width: 100%; height: 3px;
  background: linear-gradient(90deg, var(--symph-blue) 0%, var(--symph-blue-light) 100%);
  transform: scaleX(0);
  transform-origin: left;
  transition: transform 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.document-card:hover {
  border-color: var(--symph-blue-light);
  transform: translateY(-4px);
  box-shadow: var(--symph-shadow-lg);
  background: rgba(255, 255, 255, 0.95);
}

.document-card:hover::before { transform: scaleX(1); }

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
  padding-bottom: 10px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

.card-time {
  font-size: 12px;
  font-weight: 600;
  color: var(--symph-black);
  display: flex;
  align-items: center;
  gap: 6px;
}

.card-length { font-size: 12px; font-weight: 600; color: var(--symph-gray); }

.card-preview {
  font-size: 14px;
  line-height: 1.7;
  color: var(--symph-gray);
  margin-bottom: 16px;
  white-space: pre-wrap;
}

.card-ai-summary {
  margin-bottom: 16px;
  border-left: 3px solid var(--symph-blue-light);
  padding: 12px 16px;
  background: linear-gradient(135deg, rgba(74, 127, 193, 0.03) 0%, rgba(0, 57, 166, 0.05) 100%);
  border-radius: 0 8px 8px 0;
}

.summary-header {
  font-size: 10px;
  letter-spacing: 1px;
  font-weight: 700;
  color: var(--symph-blue);
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.summary-content { font-size: 13px; line-height: 1.6; color: var(--symph-gray); }

.card-footer {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
}

.view-full-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  border: 1px solid var(--symph-blue-light);
  border-radius: 8px;
  background: linear-gradient(135deg, rgba(74, 127, 193, 0.05) 0%, rgba(0, 57, 166, 0.08) 100%);
  color: var(--symph-blue);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  font-family: inherit;
}

.view-full-btn:hover {
  background: linear-gradient(135deg, var(--symph-blue) 0%, var(--symph-blue-light) 100%);
  color: var(--symph-white);
  border-color: var(--symph-blue);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 57, 166, 0.2);
}

/* 弹窗 */
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: linear-gradient(135deg, rgba(248, 249, 250, 0.92) 0%, rgba(245, 247, 250, 0.95) 100%);
  backdrop-filter: blur(12px);
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.modal-document {
  background: var(--symph-white);
  border: 1px solid var(--symph-border);
  border-radius: 16px;
  width: 100%;
  max-width: 900px;
  max-height: 85vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.12);
}

.modal-doc-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px 32px;
  border-bottom: 1px solid var(--symph-border);
}

.modal-doc-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  font-weight: 600;
  color: var(--symph-black);
}

.modal-doc-title i { color: var(--symph-blue); }

.modal-close {
  width: 36px;
  height: 36px;
  border: 1px solid var(--symph-border);
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.8);
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: var(--symph-gray);
}

.modal-close:hover {
  background: linear-gradient(135deg, var(--symph-blue) 0%, var(--symph-blue-light) 100%);
  color: var(--symph-white);
  border-color: var(--symph-blue);
  transform: rotate(90deg);
}

.modal-ai-section {
  padding: 24px 32px;
  background: linear-gradient(135deg, rgba(74, 127, 193, 0.04) 0%, rgba(0, 57, 166, 0.06) 100%);
  border-bottom: 1px solid var(--symph-border);
}

.modal-ai-label {
  font-size: 11px;
  letter-spacing: 1px;
  font-weight: 700;
  color: var(--symph-blue);
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.modal-ai-text { font-size: 14px; line-height: 1.7; color: var(--symph-gray); }

.modal-doc-body { flex: 1; overflow-y: auto; padding: 32px; }

.modal-doc-content {
  font-family: 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.9;
  color: var(--symph-black);
  white-space: pre-wrap;
  word-break: break-word;
}

/* 弹窗动画 */
.modal-fade-enter-active,
.modal-fade-leave-active { transition: all 0.3s ease; }

.modal-fade-enter-from,
.modal-fade-leave-to { opacity: 0; }

.modal-fade-enter-from .modal-document,
.modal-fade-leave-to .modal-document { transform: scale(0.95) translateY(20px); }

/* 响应式 */
@media (max-width: 1200px) {
  .gallery-container {
    grid-template-columns: 1fr;
    gap: 40px;
    padding: 30px 20px;
  }

  .calendar-sidebar {
    position: static;
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 20px;
  }

  .calendar-widget { grid-column: 1 / -1; }

  .search-bar { padding: 16px 20px; }
  .search-container { flex-direction: column; }
}

@media (max-width: 768px) {
  .gallery-nav { padding: 16px 20px; }
  .nav-brand { font-size: 14px; }
  .calendar-sidebar { grid-template-columns: 1fr; }
  .modal-document { max-width: 100%; }
}
</style>