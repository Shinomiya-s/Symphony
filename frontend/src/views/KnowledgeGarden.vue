<template>
  <div class="ds-viewport">
    <!-- 背景装饰 -->
    <div class="ds-grid-bg"></div>
    <div class="gold-accent accent-1"></div>
    <div class="gold-accent accent-2"></div>

    <nav class="ds-nav">
      <div class="ds-header-left">
        <div class="logo-wrapper">
          <div class="logo-deco"></div>
          <h1 class="ds-logo" @click="$router.push('/home')">
            <span class="line-italic">GARDEN</span>
            <span class="line-divider">/</span>
            <span class="line-normal">知识园区</span>
          </h1>
        </div>
        <p class="ds-subtitle">群体智慧 · 共同成长</p>
      </div>

      <div class="ds-header-right">
        <div class="ds-stat-block">
          <span class="ds-stat-label">TOTAL</span>
          <span class="ds-stat-num">{{ list.length }}</span>
          <span class="ds-stat-unit">ENTRIES</span>
        </div>
        <button class="ds-btn-outline" @click="$router.push('/home')">
          <span class="btn-text">返回</span>
          <span class="btn-arrow">→</span>
        </button>
      </div>
    </nav>

    <div class="ds-search-bar">
      <div class="search-wrapper">
        <div class="search-modes">
          <button
              v-for="mode in searchModes"
              :key="mode.value"
              :class="['search-mode-btn', { active: searchType === mode.value }]"
              @click="searchType = mode.value"
          >
            {{ mode.label }}
          </button>
        </div>

        <div class="search-input-group">
          <input
              v-model="searchKeyword"
              type="text"
              class="search-input"
              :placeholder="'搜索' + searchModes.find(m => m.value === searchType)?.label + '...'"
              @keyup.enter="handleSearch"
          />
          <button class="search-btn" @click="handleSearch">
            <i class="fas fa-search"></i>
          </button>
          <button v-if="searchKeyword" class="clear-btn" @click="handleClear">
            <i class="fas fa-times"></i>
          </button>
        </div>

        <!-- 排序按钮 -->
        <div class="sort-modes">
          <span class="sort-label">排序:</span>
          <button
              v-for="sort in sortModes"
              :key="sort.value"
              :class="['sort-btn', { active: sortBy === sort.value }]"
              @click="sortBy = sort.value; loadList()"
          >
            <i :class="sort.icon"></i>
            {{ sort.label }}
          </button>
        </div>
      </div>
    </div>
    <main class="ds-main-content">
      <!-- 创意图表区域 -->
      <section class="ds-charts-section">
        <div class="chart-wrapper chart-pie">
          <div class="chart-deco top-left"></div>
          <div class="ds-chart-box">
            <div class="ds-box-header">
              <span class="ds-box-index">01</span>
              <div class="box-title-wrapper">
                <span class="ds-box-title">错题分配</span>
                <span class="ds-box-subtitle">知识分布</span>
              </div>
            </div>
            <div ref="pieChartRef" class="echart-container"></div>
          </div>
          <div class="chart-deco bottom-right"></div>
        </div>

        <div class="chart-wrapper chart-bar">
          <div class="chart-deco top-right"></div>
          <div class="ds-chart-box">
            <div class="ds-box-header">
              <span class="ds-box-index">02</span>
              <div class="box-title-wrapper">
                <span class="ds-box-title">错题趋势</span>
                <span class="ds-box-subtitle">标签热度</span>
              </div>
            </div>
            <div ref="barChartRef" class="echart-container"></div>
          </div>
        </div>

        <div class="chart-wrapper chart-graph">
          <button class="chart-expand-btn" @click="openGraphModal">
            ⛶
          </button>
          <div class="chart-deco bottom-left"></div>
          <div class="ds-chart-box">
            <div class="ds-box-header">
              <span class="ds-box-index">03</span>
              <div class="box-title-wrapper">
                <span class="ds-box-title">树形图谱</span>
                <span class="ds-box-subtitle">知识图谱</span>
              </div>
            </div>
            <div ref="graphChartRef" class="echart-container"></div>
          </div>
          <div class="chart-deco top-right"></div>
        </div>
      </section>

      <!-- 列表区域 -->
      <section class="ds-list-section">
        <div class="ds-section-divider">
          <div class="divider-line"></div>
          <div class="divider-text">
            <span class="divider-icon">◆</span>
            <span>ARCHIVE ENTRIES</span>
            <span class="divider-icon">◆</span>
          </div>
          <div class="divider-line"></div>
        </div>

        <div class="ds-grid">
          <div
              v-for="(item, index) in list"
              :key="item.id"
              class="ds-card"
              :class="['card-variant-' + (index % 3)]"
              :style="{ animationDelay: index * 0.04 + 's' }"
              @click="openDetail(item)"
          >
            <div class="card-corner-deco"></div>
            <div class="card-gold-line"></div>

            <div class="card-top">
              <span class="ds-tag primary">{{ item.knowledgePoint || 'UNSORTED' }}</span>
              <span class="ds-weight">
                <i class="fas fa-heart"></i>
                <span class="weight-num">{{ item.likeCount || 0 }}</span>
              </span>
            </div>

            <h3 class="card-question">{{ item.question }}</h3>

            <div class="card-tags" v-if="item.tags">
              <span v-for="tag in item.tags.split(',')" :key="tag" class="ds-tag outline">
                {{ tag.trim() }}
              </span>
            </div>

            <div class="card-bottom">
              <span class="ds-user" @click.stop="openUserProfile(item.fromUserId)">
                <i class="fas fa-user-circle"></i>
                <span>{{ item.fromUsername }}</span>
              </span>
              <span class="ds-time">{{ formatTime(item.createdAt) }}</span>
            </div>
          </div>

          <div v-if="list.length === 0" class="ds-empty-state">
            <div class="empty-icon">◇</div>
            <p class="empty-text">NO DATA ARCHIVED</p>
            <p class="empty-sub">园区空空如也，等待播种</p>
          </div>
        </div>
      </section>
    </main>

    <!-- 详情弹窗 -->
    <transition name="fade-slide">
      <div v-if="detailVisible" class="ds-modal-overlay" @click.self="detailVisible = false">
        <div class="ds-modal detail-variant">
          <button class="ds-close-btn" @click="detailVisible = false">✕</button>

          <div class="detail-layout">
            <div class="detail-left">
              <div class="detail-gold-accent"></div>
              <div class="detail-kp-label">KNOWLEDGE POINT</div>
              <h2 class="detail-kp-title">{{ currentDetail?.knowledgePoint }}</h2>

              <div class="detail-meta-box" v-if="currentDetail?.tags">
                <div class="meta-label">TAGS</div>
                <div class="meta-tags">
                  <span v-for="tag in currentDetail.tags.split(',')" :key="tag" class="ds-tag outline">
                    {{ tag.trim() }}
                  </span>
                </div>
              </div>

              <div class="detail-meta-box">
                <div class="meta-label">CONTRIBUTOR</div>
                <div class="meta-user" @click="openUserProfile(currentDetail?.fromUserId)">
                  <span>{{ currentDetail?.fromUsername }}</span>
                  <span class="time-stamp">{{ formatTime(currentDetail?.createdAt) }}</span>
                </div>
              </div>
              <div class="detail-meta-box" v-if="related.length > 0">
                <div class="meta-label">RELATED</div>
                <div class="children-list">
                  <div
                      v-for="item in related"
                      :key="item.id"
                      class="child-item"
                      @click="openDetail(item)"
                  >
                    <span class="child-dot">◇</span>
                    <span class="child-name">{{ item.question.length > 30 ? item.question.slice(0, 30) + '...' : item.question }}</span>
                  </div>
                </div>
              </div>
              <div class="detail-meta-box">
                <div class="meta-label">LIKES</div>
                <button class="like-btn" :disabled="likeLoading" @click.stop="toggleLike(currentDetail)">
                  <i :class="isLiked ? 'fas fa-heart' : 'far fa-heart'"></i>
                  <span>{{ currentDetail?.likeCount || 0 }}</span>
                </button>
              </div>
            </div>

            <div class="detail-right">
              <div class="ds-content-block">
                <div class="block-title">
                  <span class="block-num">01</span>
                  <span>QUESTION</span>
                </div>
                <div class="block-text">{{ currentDetail?.question }}</div>
              </div>

              <div class="ds-content-block highlight">
                <div class="block-title">
                  <span class="block-num">02</span>
                  <span>CORRECT ANSWER</span>
                </div>
                <div class="block-text answer">{{ currentDetail?.correctAnswer }}</div>
              </div>

              <div class="ds-content-block">
                <div class="block-title">
                  <span class="block-num">03</span>
                  <span>AI ANALYSIS</span>
                </div>
                <div class="block-text">{{ currentDetail?.aiAnalysis }}</div>
              </div>

              <div class="ds-content-block dark">
                <div class="block-title">
                  <span class="block-num">04</span>
                  <span>STRATEGY</span>
                </div>
                <div class="block-text strategy">{{ currentDetail?.strategy }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </transition>

    <transition name="fade-slide">
      <div v-if="graphModalVisible" class="ds-modal-overlay" @click.self="closeGraphModal">
        <div class="ds-modal graph-variant">
          <button class="ds-close-btn" @click="closeGraphModal">✕</button>
          <div ref="graphFullRef" class="graph-full"></div>
        </div>
      </div>
    </transition>
    <!-- 用户Profile弹窗 -->
    <transition name="fade-slide">
      <div v-if="showProfileModal" class="ds-modal-overlay" @click.self="closeProfileModal">
        <div class="ds-modal profile-variant">
          <button class="ds-close-btn" @click="closeProfileModal">✕</button>

          <div class="profile-header">
            <div class="profile-deco"></div>
            <span>USER PROFILE</span>
          </div>

          <div class="profile-body">
            <div v-if="viewingUser" class="profile-hero">
              <div class="p-avatar" :style="{ background: getAvatarColor(viewingUser.username) }">
                {{ viewingUser.username?.[0]?.toUpperCase() || '?' }}
              </div>
              <h3 class="p-name">{{ viewingUser.username }}</h3>
              <p class="p-bio">{{ viewingUser.bio || 'This user is mysterious and left no bio.' }}</p>
            </div>

            <div v-else class="profile-loading">
              <span class="spinner"></span>
              <span>LOADING...</span>
            </div>

            <div class="profile-stats" v-if="viewingUser">
              <div class="p-stat">
                <span class="p-val">{{ viewingUser.mistakeCount || 0 }}</span>
                <span class="p-lbl">MISTAKES</span>
              </div>
              <div class="p-stat">
                <span class="p-val">{{ viewingUser.aiCount || 0 }}</span>
                <span class="p-lbl">AI CHATS</span>
              </div>
              <div class="p-stat">
                <span class="p-val">{{ viewingUser.days || 1 }}</span>
                <span class="p-lbl">DAYS</span>
              </div>
            </div>

            <div v-if="viewingUser?.isFriend" class="friend-badge">
              ✓ CONNECTED
            </div>
          </div>

          <div class="profile-actions">
            <button class="profile-btn secondary" @click="closeProfileModal">CLOSE</button>
            <button v-if="viewingUser?.isFriend" class="profile-btn primary" @click="goToPublicChat">
              CHAT →
            </button>
            <button v-else class="profile-btn primary" :disabled="requestSent || !viewingUser" @click="sendFriendRequest">
              {{ requestSent ? 'SENT' : 'ADD +' }}
            </button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'

const router = useRouter()
const userId = localStorage.getItem('currentUserId')

const list = ref([])
const detailVisible = ref(false)
const currentDetail = ref(null)
const pieChartRef = ref(null)
const barChartRef = ref(null)
const graphChartRef = ref(null)
const showProfileModal = ref(false)
const viewingUser = ref(null)
const requestSent = ref(false)
const graphModalVisible = ref(false)
const graphFullRef = ref(null)
let graphChartInstance = null
let fullGraphInstance = null
const searchKeyword = ref('')
const searchType = ref('title')
const searchModes = [
  { value: 'title', label: '题目' },
  { value: 'tag', label: '标签' },
  { value: 'user', label: '用户' }
]
const sortBy = ref('time')
const sortModes = [
  { value: 'time', label: '时间', icon: 'fas fa-clock' },
  { value: 'weight', label: '热度', icon: 'fas fa-fire' },
  { value: 'tag', label: '标签', icon: 'fas fa-tag' }
]
const isLiked = ref(false)

//打开知识条目详情
const openDetail = async (item) => {
  currentDetail.value = item
  related.value = []
  detailVisible.value = true
  const token = localStorage.getItem('token')

  //查点赞状态
  const likeRes = await fetch(`/api/garden/${item.id}/like`, {
    headers: { 'Authorization': 'Bearer ' + token }
  })
  const likeData = await likeRes.json()
  if (likeData.code === 200) isLiked.value = likeData.data

  //查相关推荐
  const res = await fetch(`/api/garden/${item.id}/related`, {
    headers: { 'Authorization': 'Bearer ' + token }
  })
  const data = await res.json()
  if (data.code === 200) related.value = data.data
}

const likeLoading = ref(false)

//点赞/取消点赞
const toggleLike = async (item) => {
  if (likeLoading.value) return  //请求进行中直接拦截，防重复
  likeLoading.value = true
  try {
    const token = localStorage.getItem('token')
    const res = await fetch(`/api/garden/${item.id}/like`, {
      method: 'POST',
      headers: { 'Authorization': 'Bearer ' + token }
    })
    const data = await res.json()
    if (data.code === 200) {
      isLiked.value = data.data
      //本地立刻更新点赞数，不等接口返回
      item.likeCount = Math.max(0, (item.likeCount || 0) + (data.data ? 1 : -1))
      currentDetail.value = { ...item }
    }
  } finally {
    likeLoading.value = false
  }
}

//加载知识条目列表
const loadList = async () => {
  const token = localStorage.getItem('token')

  let url = '/api/garden/list'
  const params = new URLSearchParams()
  //拼接搜索和排序参数
  if (searchKeyword.value) {
    params.append('keyword', searchKeyword.value)
    params.append('searchType', searchType.value)
  }
  params.append('sortBy', sortBy.value)

  if (params.toString()) {
    url += '?' + params.toString()
  }

  const res = await fetch(url, {
    headers: { 'Authorization': 'Bearer ' + token }
  })
  const data = await res.json()
  if (data.code === 200) {
    list.value = data.data
    //加载完后初始化图表
    nextTick(() => initCharts())
  }
}

// 搜索处理
const handleSearch = () => {
  loadList()
}

// 清空搜索
const handleClear = () => {
  searchKeyword.value = ''
  loadList()
}

//知识图谱全屏展示
const openGraphModal = () => {
  graphModalVisible.value = true

  nextTick(() => {
    fullGraphInstance = echarts.init(graphFullRef.value)
    fullGraphInstance.setOption(graphChartInstance.getOption())
  })
}

const closeGraphModal = () => {
  graphModalVisible.value = false

  if (fullGraphInstance) {
    fullGraphInstance.dispose()
    fullGraphInstance = null
  }
}

const getAvatarColor = (name) => {
  const colors = ['#1a1a1a', '#304f25', '#C9A84C', '#2c3e50', '#34495e']
  if (!name) return colors[0]
  return colors[name.charCodeAt(0) % colors.length]
}

const formatTime = (ts) => {
  if (!ts) return ''
  const d = new Date(ts)
  return `${d.getFullYear()}.${String(d.getMonth()+1).padStart(2,'0')}.${String(d.getDate()).padStart(2,'0')}`
}

//查看知识条目发布者资料
const openUserProfile = async (targetUserId) => {
  if (!targetUserId) return
  showProfileModal.value = true
  viewingUser.value = null
  requestSent.value = false
  try {
    //先用userId查username，再用username查详细资料
    const token = localStorage.getItem('token')
    const r1 = await fetch(`/api/user/id/${targetUserId}`, {
      headers: { 'Authorization': 'Bearer ' + token }
    })
    const d1 = await r1.json()
    if (d1.code === 200 && d1.data?.username) {
      const r2 = await fetch(`/api/user/profile/${d1.data.username}`, {
        headers: { 'Authorization': 'Bearer ' + token }
      })
      const d2 = await r2.json()
      if (d2.code === 200) {
        viewingUser.value = { ...d2.data, userId: d1.data.userId }
      }
    }
  } catch (e) {
    console.error('加载用户资料失败', e)
  }
}

const closeProfileModal = () => {
  showProfileModal.value = false
  viewingUser.value = null
}

//发好友申请
const sendFriendRequest = async () => {
  if (!viewingUser.value || viewingUser.value.isFriend || requestSent.value) return  //已是好友或已申请过
  try {
    const token = localStorage.getItem('token')
    const res = await fetch('/api/friends/request', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token },
      body: JSON.stringify({ targetUsername: viewingUser.value.username })
    })
    const data = await res.json()
    if (data.code === 200) requestSent.value = true
    else window.$toast.info(data.msg || '申请发送失败')
  } catch (e) {
    window.$toast.info('网络错误，请重试')
  }
}

const goToPublicChat = () => {
  const userId = localStorage.getItem('currentUserId')
  router.push(`/chat/${userId}/public`)
}

const related = ref([])

//加载完数据后初始化三个ECharts图表
const initCharts = () => {
  const kpCount = {}
  list.value.forEach(item => {
    const kp = item.knowledgePoint || '未分类'
    kpCount[kp] = (kpCount[kp] || 0) + 1
  })

  // 饼图 - 黑白金绿配色
  const pieChart = echarts.init(pieChartRef.value)
  pieChart.setOption({
    backgroundColor: 'transparent',
    color: ['#304f25','#C9A84C','#1a1a1a','#D4AF37','#2c5a3c','#A68944','#333','#4a6f4a'],
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c}次 ({d}%)',
      backgroundColor: '#1a1a1a',
      borderColor: '#C9A84C',
      borderWidth: 1,
      textStyle: { color: '#fff' }
    },
    series: [{
      type: 'pie',
      radius: ['45%', '75%'],
      center: ['50%', '45%'],
      data: Object.entries(kpCount).map(([name, value]) => ({ name, value })),
      label: { show: false },
      emphasis: {
        itemStyle: { shadowBlur: 15, shadowColor: 'rgba(201,168,76,0.5)' },
        label: { show: true, fontSize: 12, fontWeight: 'bold', color: '#1a1a1a' }
      }
    }]
  })

  const tagCount = {}
  list.value.forEach(item => {
    if (!item.tags) return
    item.tags.split(',').forEach(tag => {
      const t = tag.trim()
      if (t) tagCount[t] = (tagCount[t] || 0) + 1
    })
  })
  const tagEntries = Object.entries(tagCount).sort((a, b) => b[1] - a[1]).slice(0, 8)

  // 柱状图 - 金色渐变
  const barChart = echarts.init(barChartRef.value)
  barChart.setOption({
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#1a1a1a',
      borderColor: '#C9A84C',
      borderWidth: 1,
      textStyle: { color: '#fff' }
    },
    grid: { left: 15, right: 15, bottom: 50, top: 25, containLabel: true },
    xAxis: {
      type: 'category',
      data: tagEntries.map(e => e[0]),
      axisLabel: { color: '#1a1a1a', fontSize: 11, fontWeight: 600, rotate: 25 },
      axisLine: { lineStyle: { color: '#ddd' } }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#666', fontSize: 11 },
      splitLine: { lineStyle: { color: '#f0f0f0' } }
    },
    series: [{
      type: 'bar',
      data: tagEntries.map(e => e[1]),
      barMaxWidth: 40,
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0,0,0,1,[
          {offset:0, color:'#D4AF37'},
          {offset:0.5, color:'#C9A84C'},
          {offset:1, color:'#A68944'}
        ]),
        borderRadius: [6,6,0,0]
      },
      emphasis: {
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0,0,0,1,[
            {offset:0, color:'#FFD700'},
            {offset:1, color:'#DAA520'}
          ])
        }
      }
    }]
  })

  // 知识图谱 - 浅色节点配色，文字清晰可见
  const nodes = [], links = [], nodeSet = new Set()
  list.value.forEach(item => {
    const kp = item.knowledgePoint || '未分类'
    if (!nodeSet.has(kp)) {
      nodes.push({
        name: kp,
        symbolSize: 35 + (kpCount[kp]||1)*6,
        category: 0,
        itemStyle: {
          color: '#ffffff',
          borderColor: '#1a1a1a',
          borderWidth: 2,
          shadowBlur: 6,
          shadowColor: 'rgba(0,0,0,0.1)'
        },
        label: {
          color: '#1a1a1a',
          fontWeight: 700
        }
      })
      nodeSet.add(kp)
    }
    if (item.tags) {
      item.tags.split(',').forEach(tag => {
        const t = tag.trim()
        if (!t) return
        if (!nodeSet.has(t)) {
          nodes.push({
            name: t,
            symbolSize: 22,
            category: 1,
            itemStyle: {
              color: '#C9A84C',
              borderColor: '#1a1a1a',
              borderWidth: 2,
              shadowBlur: 6,
              shadowColor: 'rgba(201,168,76,0.3)'
            },
            label: {
              color: '#1a1a1a',
              fontWeight: 600
            }
          })
          nodeSet.add(t)
        }
        links.push({ source: kp, target: t })
      })
    }
  })
  graphChartInstance = echarts.init(graphChartRef.value)
  const graphChart = graphChartInstance
  graphChart.setOption({
    backgroundColor: 'transparent',

    tooltip: {
      trigger: 'item',
      backgroundColor: '#1a1a1a',
      borderColor: '#C9A84C',
      borderWidth: 1,
      textStyle: { color: '#fff' }
    },

    legend: [{
      data: [
        { name: '知识点', icon: 'circle', itemStyle: { color: '#ffffff' } },
        { name: '标签', icon: 'circle', itemStyle: { color: '#C9A84C' } }
      ],
      bottom: 5,
      textStyle: {
        color: '#1a1a1a',
        fontWeight: 700,
        fontSize: 11
      }
    }],

    series: [{
      type: 'graph',
      layout: 'force',

      data: nodes,
      links,

      categories: [
        {
          name: '知识点',
          itemStyle: {
            color: '#ffffff',
            borderColor: '#1a1a1a',
            borderWidth: 2
          }
        },
        {
          name: '标签',
          itemStyle: {
            color: '#C9A84C'
          }
        }
      ],
      roam: true,

      label: {
        show: true,
        fontSize: 11,
        color: '#1a1a1a',
        fontWeight: 700
      },

      force: {
        repulsion: 180,
        edgeLength: 110
      },

      lineStyle: {
        color: '#dcdcdc',
        width: 3,
        curveness: 0.25
      },

      emphasis: {
        focus: 'adjacency',

        itemStyle: {
          shadowBlur: 20,
          shadowColor: 'rgba(201,168,76,0.6)'
        },

        lineStyle: {
          width: 3,
          color: '#C9A84C'
        },

        label: {
          fontSize: 13,
          fontWeight: 'bold'
        }
      }
    }]
  })
}

//加载知识条目列表，列表加载完后自动初始化三个图表
onMounted(() => {
  loadList()
})
</script>

<style>
:root {
  --green: #304f25;
  --green-dark: #1e3516;
  --gold: #C9A84C;
  --gold-light: #D4AF37;
  --black: #1a1a1a;
  --white: #fdfdfc;
  --gray: #666;
  --gray-light: #ddd;
}
</style>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Cormorant+Garamond:ital,wght@1,400;1,700&family=Inter:wght@300;400;600;800;900&display=swap');

* { box-sizing: border-box; margin: 0; padding: 0; }
::selection { background: var(--gold); color: var(--black); }

.ds-viewport {
  min-height: 100vh;
  background: #f3f3ef;
  font-family: 'Inter', sans-serif;
  color: var(--black);
  position: relative;
  overflow-x: hidden;
  padding-bottom: 80px;
}

/* 背景装饰 */
.ds-grid-bg {
  position: fixed; inset: 0; pointer-events: none; z-index: 0;
  background-image:
      repeating-linear-gradient(0deg, rgba(201,168,76,0.03) 0px, transparent 1px, transparent 80px, rgba(201,168,76,0.03) 81px),
      repeating-linear-gradient(90deg, rgba(201,168,76,0.03) 0px, transparent 1px, transparent 80px, rgba(201,168,76,0.03) 81px);
}

.gold-accent {
  position: fixed; pointer-events: none; z-index: 0;
  background: radial-gradient(circle, rgba(212,175,55,0.15), transparent 70%);
}
.accent-1 { width: 400px; height: 400px; top: -100px; right: -100px; }
.accent-2 { width: 350px; height: 350px; bottom: -80px; left: -80px; }

/* 导航栏 */
.ds-nav {
  position: relative; z-index: 10;
  display: flex; justify-content: space-between; align-items: center;
  padding: 35px 60px 25px;
  border-bottom: 3px solid var(--black);
  background: var(--white);
}

.logo-wrapper { position: relative; }
.logo-deco {
  position: absolute; top: -8px; left: -8px;
  width: 4px; height: 4px;
  background: var(--gold);
  box-shadow:
      0 0 0 2px var(--gold),
      12px 0 0 0 var(--gold),
      0 12px 0 0 var(--gold);
}

.ds-logo {
  margin: 0; cursor: pointer;
  display: flex; align-items: baseline; gap: 12px;
  transition: transform 0.3s;
}
.ds-logo:hover { transform: translateX(2px); }
.line-italic {
  font-family: 'Cormorant Garamond', serif;
  font-size: 48px;
  font-style: italic;
  font-weight: 700;
  color: var(--green);
  letter-spacing: -1px;
}
.line-divider {
  font-size: 32px;
  color: var(--gold);
  font-weight: 300;
}
.line-normal {
  font-size: 16px;
  font-weight: 900;
  letter-spacing: 3px;
  color: var(--black);
}

.ds-subtitle {
  font-size: 10px;
  letter-spacing: 5px;
  color: var(--gray);
  text-transform: uppercase;
  font-weight: 600;
  margin-top: 8px;
  padding-left: 2px;
}

.ds-header-right { display: flex; align-items: center; gap: 30px; }

.ds-stat-block {
  display: flex;
  gap: 8px;
  padding: 8px 18px;
  align-items: center;
  justify-content: center;
  border: 2px solid var(--black);
  background: var(--white);
  position: relative;
}
.ds-stat-block::before {
  content: '';
  position: absolute; top: -4px; right: -4px;
  width: 100%; height: 100%;
  border: 2px solid var(--gold);
  z-index: -1;
}
.ds-stat-label { font-size: 9px; font-weight: 800; letter-spacing: 1px; color: var(--gray); }
.ds-stat-num {
  font-family: 'Cormorant Garamond', serif;
  font-size: 32px;
  font-style: italic;
  font-weight: 700;
  color: var(--green);
  line-height: 1;
}
.ds-stat-unit { font-size: 9px; font-weight: 800; letter-spacing: 1px; color: var(--gray); }

.ds-btn-outline {
  background: var(--black); border: none;
  padding: 14px 28px; color: var(--white);
  font-family: 'Inter', sans-serif;
  font-size: 11px; font-weight: 800; letter-spacing: 2px;
  cursor: pointer; display: flex; align-items: center; gap: 10px;
  position: relative; overflow: hidden;
  transition: all 0.4s;
}
.ds-btn-outline::before {
  content: '';
  position: absolute; top: 0; left: -100%; width: 100%; height: 100%;
  background: var(--gold);
  transition: left 0.4s;
}
.ds-btn-outline:hover::before { left: 0; }
.ds-btn-outline:hover { color: var(--black); }
.btn-text, .btn-arrow { position: relative; z-index: 1; }
.btn-arrow { transition: transform 0.3s; }
.ds-btn-outline:hover .btn-arrow { transform: translateX(4px); }

/* 图表区域 */
.ds-main-content { position: relative; z-index: 10; padding: 0 60px; max-width: 1600px; margin: 0 auto; }

.ds-charts-section {
  display: grid;
  grid-template-columns: 0.9fr 0.9fr 1.2fr;
  gap: 25px;
  margin-top: 50px;
}

.chart-wrapper { position: relative; }

.chart-deco {
  position: absolute;
  width: 30px; height: 30px;
  border: 3px solid var(--gold);
  pointer-events: none;
}
.chart-deco.top-left { top: -10px; left: -10px; border-right: none; border-bottom: none; }
.chart-deco.top-right { top: -10px; right: -10px; border-left: none; border-bottom: none; }
.chart-deco.bottom-left { bottom: -10px; left: -10px; border-right: none; border-top: none; }
.chart-deco.bottom-right { bottom: -10px; right: -10px; border-left: none; border-top: none; }

.ds-chart-box {
  background: #ffffff;
  border: 2px solid var(--black);
  padding: 25px 20px 20px;
  transition: all 0.3s;
  height: 100%;
  box-shadow: 0 2px 0 rgba(0,0,0,0.05);
}

.chart-wrapper:hover .ds-chart-box {
  transform: translate(-3px, -3px);

  box-shadow:
      5px 5px 0 var(--gold),
      0 8px 20px rgba(0,0,0,0.06);
}

.ds-box-header {
  display: flex; align-items: center; gap: 15px;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 2px solid var(--black);
}

.ds-box-index {
  font-family: 'Cormorant Garamond', serif;
  font-size: 24px;
  font-style: italic;
  font-weight: 700;
  color: var(--gold);
  min-width: 35px;
}

.box-title-wrapper { display: flex; flex-direction: column; gap: 2px; }
.ds-box-title {
  font-size: 13px;
  font-weight: 900;
  letter-spacing: 2px;
  color: var(--black);
}
.ds-box-subtitle {
  font-size: 10px;
  color: var(--gray);
  letter-spacing: 1px;
}

.echart-container { width: 100%; height: 260px; }

/* 列表区域 */
.ds-list-section {
  margin-top: 70px;
  padding: 40px;
  background: #f8f8f5;
  border: 2px solid var(--black);
}

.ds-section-divider {
  display: flex;
  align-items: center;
  gap: 30px;
  margin-bottom: 45px;
}

.divider-line {
  flex: 1;
  height: 2px;
  background: linear-gradient(90deg, transparent, var(--gold) 20%, var(--gold) 80%, transparent);
}

.divider-text {
  font-family: 'Cormorant Garamond', serif;
  font-size: 26px;
  font-style: italic;
  font-weight: 700;
  color: var(--green);
  letter-spacing: 3px;
  display: flex;
  align-items: center;
  gap: 20px;
}

.divider-icon {
  font-size: 12px;
  color: var(--gold);
}

.ds-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(330px, 1fr));
  gap: 30px;
}

.ds-card {
  background: #ffffff;
  border: 2px solid var(--black);
  padding: 28px 24px;
  cursor: pointer;
  position: relative;
  overflow: hidden;

  transition:
      transform 0.25s ease,
      box-shadow 0.25s ease,
      border-color 0.25s ease;

  box-shadow:
      0 2px 0 rgba(0,0,0,0.05),
      inset 0 1px 0 rgba(255,255,255,0.6);

  animation: fadeUp 0.6s ease-out both;
}

.ds-card > * {
  position: relative;
  z-index: 2;
}

/* 扫光效果 */
.ds-card::after {
  content: '';
  position: absolute;
  top: 0;
  left: -120%;
  width: 80%;
  height: 100%;

  background: linear-gradient(
      90deg,
      transparent,
      rgba(255,255,255,0.6),
      transparent
  );

  transform: skewX(-20deg);
  transition: left 0.6s;
}

.ds-card:hover::after {
  left: 120%;
}

.card-corner-deco {
  position: absolute;
  top: -2px;
  right: -2px;
  width: 0;
  height: 0;
  border-bottom: 22px solid var(--gold);
  border-right: 22px solid transparent;
  transition: all 0.3s;
}

/* 顶部金线 */
.card-gold-line {
  position: absolute;
  top: 0;
  left: 0;

  height: 3px;
  width: 60%;

  background: linear-gradient(
      90deg,
      #D4AF37,
      #FFD700,
      #C9A84C
  );

  transform-origin: left;
  transform: scaleX(0.7);
  transition: transform 0.3s ease;
}

/* hover状态 */
.ds-card:hover {
  transform: translate(-4px, -4px);

  box-shadow:
      4px 4px 0 var(--black),
      0 12px 24px rgba(0,0,0,0.08);

  border-color: var(--gold);
}

.ds-card:hover .card-corner-deco {
  border-bottom-width: 35px;
  border-right-width: 35px;
}

.ds-card:hover .card-gold-line {
  transform: scaleX(1);
}

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 18px;
}

.ds-tag {
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 1.5px;
  padding: 5px 12px;
  text-transform: uppercase;
}

.ds-tag.primary {
  background: var(--green);
  color: var(--white);
}

.ds-tag.outline {
  border: 1px solid #C9A84C;
  color: #1a1a1a;
  background: rgba(201,168,76,0.08);
}

.ds-card:hover .ds-tag.outline {
  border-color: var(--gold);
  color: var(--gold);
}

.ds-weight {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  font-weight: 900;
  color: var(--gold);
}
.weight-num {
  font-family: 'Cormorant Garamond', serif;
  font-size: 16px;
  font-style: italic;
}

.card-question {
  font-size: 16px;
  font-weight: 600;
  line-height: 1.6;
  color: var(--black);
  margin-bottom: 18px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  transition: color 0.25s;
}

.card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 20px;
  min-height: 28px;
}

.card-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 16px;
  border-top: 2px solid var(--black);
  font-size: 11px;
  font-weight: 700;
}

.ds-user {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--green);
  cursor: pointer;
  transition: all 0.3s;
}

.ds-user:hover {
  color: var(--gold);
  text-decoration: underline;
  text-underline-offset: 3px;
}

.ds-time {
  color: var(--gray);
  letter-spacing: 0.5px;
}

.ds-empty-state {
  grid-column: 1/-1;
  text-align: center;
  padding: 120px 40px;
}

.empty-icon {
  font-size: 60px;
  color: var(--gold);
  margin-bottom: 20px;
  font-family: 'Cormorant Garamond', serif;
}

.empty-text {
  font-size: 16px;
  font-weight: 900;
  letter-spacing: 3px;
  color: var(--black);
  margin-bottom: 8px;
}

.empty-sub {
  font-size: 13px;
  color: var(--gray);
}

/* 弹窗 */
.ds-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(26,26,26,0.75);
  backdrop-filter: blur(6px);
  z-index: 999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.ds-modal {
  background: var(--white);
  border: 3px solid var(--black);
  position: relative;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 15px 15px 0 rgba(201,168,76,0.3);
}

.ds-close-btn {
  position: absolute;
  top: 20px;
  right: 24px;
  width: 36px;
  height: 36px;
  background: var(--black);
  border: none;
  color: var(--white);
  font-size: 20px;
  cursor: pointer;
  transition: all 0.3s;
  z-index: 10;
}

.ds-close-btn:hover {
  background: var(--gold);
  color: var(--black);
  transform: rotate(90deg);
}

/* 详情弹窗 */
.detail-variant {
  width: 100%;
  max-width: 1100px;
  padding: 0;
}

.detail-layout {
  display: flex;
  min-height: 650px;
}

.detail-left {
  width: 38%;
  background: linear-gradient(135deg, #f5f9f5 0%, #e8f5e9 100%);
  color: var(--black);
  padding: 60px 45px;
  display: flex;
  flex-direction: column;
  position: relative;
  border-right: 3px solid var(--black);
}

.detail-gold-accent {
  position: absolute;
  top: 0;
  left: 0;
  width: 5px;
  height: 80px;
  background: var(--gold);
}

.detail-kp-label {
  font-size: 10px;
  color: var(--gold);
  letter-spacing: 3px;
  font-weight: 800;
  margin-bottom: 20px;
}

.detail-kp-title {
  font-family: 'Cormorant Garamond', serif;
  font-size: 38px;
  font-style: italic;
  font-weight: 700;
  line-height: 1.2;
  margin-bottom: auto;
  color: var(--green);
}

.detail-meta-box {
  margin-top: 40px;
  padding-top: 20px;
  border-top: 2px solid var(--green);
}

.meta-label {
  font-size: 9px;
  color: var(--gold);
  letter-spacing: 2px;
  font-weight: 800;
  margin-bottom: 12px;
}

.meta-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.meta-tags .ds-tag.outline {
  color: var(--green);
  border-color: var(--green);
  background: rgba(48,79,37,0.1);
}

.meta-user {
  cursor: pointer;
  font-size: 14px;
  font-weight: 700;
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: color 0.3s;
}

.meta-user:hover {
  color: var(--green);
}

.time-stamp {
  font-size: 10px;
  font-weight: 400;
  color: var(--gray);
}

.detail-right {
  width: 62%;
  padding: 60px 50px;
  display: flex;
  flex-direction: column;
  gap: 35px;
  overflow-y: auto;

  background: #f6f6f2;
}

.ds-content-block .block-title {
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 2px;
  color: var(--gray);
  margin-bottom: 14px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.block-num {
  font-family: 'Cormorant Garamond', serif;
  font-size: 18px;
  font-style: italic;
  color: var(--gold);
}

.ds-content-block.highlight .block-title {
  color: var(--green);
}

.ds-content-block.highlight .block-num {
  color: var(--green);
}

.block-text {
  font-size: 15px;
  line-height: 1.8;
  color: var(--black);
  padding: 16px 20px;
  background: #fafafa;
  border-left: 3px solid var(--gray-light);
}

.block-text.answer {
  font-family: 'Cormorant Garamond', serif;
  font-size: 19px;
  font-style: italic;
  color: var(--green);
  background: #f5f9f5;
  border-left-color: var(--green);
  font-weight: 600;
}

.block-text.strategy {
  background: #fffef8;
  border-left-color: var(--gold);
  font-size: 14px;
}

/* Profile弹窗 */
.profile-variant {
  width: 100%;
  max-width: 440px;
  padding: 0;
}

.profile-header {
  padding: 25px 30px;
  background: var(--black);
  color: var(--white);
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 3px;
  display: flex;
  align-items: center;
  gap: 12px;
  position: relative;
}

.profile-deco {
  width: 5px;
  height: 5px;
  background: var(--gold);
  box-shadow:
      0 0 0 2px var(--gold),
      10px 0 0 0 var(--gold);
}

.profile-body {
  padding: 40px 30px;
}

.profile-hero {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 30px;
}

.p-avatar {
  width: 90px;
  height: 90px;
  border: 3px solid var(--black);
  color: var(--white);
  font-size: 36px;
  font-family: 'Cormorant Garamond', serif;
  font-style: italic;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
  position: relative;
}

.p-avatar::after {
  content: '';
  position: absolute;
  top: -6px;
  right: -6px;
  width: 100%;
  height: 100%;
  border: 3px solid var(--gold);
  z-index: -1;
}

.p-name {
  font-size: 26px;
  font-weight: 900;
  margin-bottom: 10px;
  color: var(--black);
}

.p-bio {
  font-size: 13px;
  color: var(--gray);
  line-height: 1.6;
  text-align: center;
}

.profile-loading {
  padding: 50px 0;
  font-size: 12px;
  letter-spacing: 2px;
  color: var(--gray);
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.spinner {
  display: inline-block;
  width: 20px;
  height: 20px;
  border: 3px solid var(--gray-light);
  border-top-color: var(--gold);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.profile-stats {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 12px;
  border: 2px solid var(--black);
  padding: 25px 0;
  margin-bottom: 25px;
}

.p-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  border-right: 1px solid var(--gray-light);
}

.p-stat:last-child {
  border-right: none;
}

.p-val {
  font-family: 'Cormorant Garamond', serif;
  font-size: 28px;
  font-style: italic;
  font-weight: 700;
  color: var(--gold);
}

.p-lbl {
  font-size: 9px;
  font-weight: 900;
  letter-spacing: 1.5px;
  color: var(--gray);
}

.friend-badge {
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 2px;
  color: var(--green);
  background: rgba(48,79,37,0.1);
  padding: 10px 0;
  margin-bottom: 25px;
  border: 2px dashed var(--green);
  text-align: center;
}

.profile-actions {
  padding: 20px 30px 30px;
  display: flex;
  gap: 12px;
  border-top: 2px solid var(--black);
}

.profile-btn {
  flex: 1;
  padding: 15px 0;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 2px;
  cursor: pointer;
  border: 2px solid var(--black);
  transition: all 0.3s;
}

.profile-btn.secondary {
  background: transparent;
  color: var(--black);
}

.profile-btn.secondary:hover {
  background: var(--black);
  color: var(--white);
}

.profile-btn.primary {
  background: var(--gold);
  color: var(--black);
}

.profile-btn.primary:hover:not(:disabled) {
  background: var(--black);
  color: var(--gold);
  border-color: var(--gold);
}

.profile-btn.primary:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* 过渡动画 */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.fade-slide-enter-from,
.fade-slide-leave-to {
  opacity: 0;
  transform: scale(0.96) translateY(20px);
}

/* 响应式 */
@media (max-width: 1200px) {
  .ds-charts-section {
    grid-template-columns: 1fr 1fr;
  }
  .chart-graph {
    grid-column: 1 / -1;
  }
}

@media (max-width: 900px) {
  .ds-charts-section {
    grid-template-columns: 1fr;
  }
  .detail-layout {
    flex-direction: column;
  }
  .detail-left,
  .detail-right {
    width: 100%;
  }
  .ds-nav,
  .ds-main-content {
    padding-left: 30px;
    padding-right: 30px;
  }
}

.chart-expand-btn{
  position:absolute;
  right:12px;
  top:12px;
  width:34px;
  height:34px;
  border:2px solid var(--black);
  background:#fff;
  font-size:16px;
  cursor:pointer;
  z-index:5;
  transition:all 0.25s;
}

.chart-expand-btn:hover{
  background:var(--gold);
  color:#000;
  transform:scale(1.05);
}

.graph-variant{
  width:90vw;
  height:90vh;
  padding:30px;
}

.graph-full{
  width:100%;
  height:100%;
}

/* 搜索栏 */
.ds-search-bar {
  position: relative;
  z-index: 10;
  background: var(--white);
  border-bottom: 2px solid var(--black);
  padding: 25px 60px;
}

.search-wrapper {
  max-width: 1600px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  gap: 25px;
}

.search-modes {
  display: flex;
  gap: 8px;
  border: 2px solid var(--black);
  padding: 4px;
  background: #f8f8f5;
}

.search-mode-btn {
  padding: 10px 20px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 1px;
  background: transparent;
  border: none;
  color: var(--gray);
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
}

.search-mode-btn.active {
  background: var(--black);
  color: var(--white);
}

.search-mode-btn:not(.active):hover {
  color: var(--gold);
}

.search-input-group {
  flex: 1;
  display: flex;
  border: 2px solid var(--black);
  background: var(--white);
  position: relative;
}

.search-input-group::after {
  content: '';
  position: absolute;
  top: -6px;
  right: -6px;
  width: 100%;
  height: 100%;
  border: 2px solid var(--gold);
  z-index: -1;
  pointer-events: none;
}

.search-input {
  flex: 1;
  padding: 14px 20px;
  font-size: 14px;
  border: none;
  outline: none;
  font-family: 'Inter', sans-serif;
  background: transparent;
}

.search-input::placeholder {
  color: var(--gray);
  font-size: 13px;
}

.search-btn,
.clear-btn {
  width: 50px;
  border: none;
  background: transparent;
  border-left: 2px solid var(--black);
  color: var(--black);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
}

.search-btn:hover {
  background: var(--gold);
  color: var(--black);
}

.clear-btn:hover {
  background: #f44336;
  color: var(--white);
}

@media (max-width: 900px) {
  .ds-search-bar {
    padding: 20px 30px;
  }

  .search-wrapper {
    flex-direction: column;
    align-items: stretch;
  }

  .search-modes {
    justify-content: center;
  }
}
/* 排序按钮 */
.sort-modes {
  display: flex;
  align-items: center;
  gap: 8px;
  border: 2px solid var(--black);
  padding: 4px;
  background: #f8f8f5;
}

.sort-label {
  font-size: 11px;
  font-weight: 700;
  color: var(--gray);
  padding: 0 8px;
  letter-spacing: 1px;
}

.sort-btn {
  padding: 10px 16px;
  font-size: 11px;
  font-weight: 700;
  background: transparent;
  border: none;
  color: var(--gray);
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 6px;
}

.sort-btn.active {
  background: var(--gold);
  color: var(--black);
}

.sort-btn:not(.active):hover {
  color: var(--black);
  background: rgba(201,168,76,0.2);
}

.sort-btn i {
  font-size: 10px;
}

.children-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.child-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border: 1px solid var(--green);
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  color: var(--green);
  transition: all 0.2s;
}

.child-item:hover {
  background: var(--green);
  color: var(--white);
}

.child-dot {
  font-size: 8px;
  color: var(--gold);
}

.like-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  border: 2px solid var(--green);
  background: transparent;
  color: var(--green);
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.3s;
}
.like-btn:hover, .like-btn.liked {
  background: var(--green);
  color: var(--white);
}

@media (max-width: 900px) {
  .search-wrapper {
    flex-direction: column;
    align-items: stretch;
  }

  .search-modes,
  .sort-modes {
    justify-content: center;
  }
}
@media (max-width: 600px) {
  .ds-nav {
    flex-direction: column;
    align-items: flex-start;
    gap: 20px;
  }
  .ds-header-right {
    width: 100%;
    justify-content: space-between;
  }
}
</style>