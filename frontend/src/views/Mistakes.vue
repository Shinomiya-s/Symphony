<template>
  <div class="symphony-mistakes">
    <!-- 导航栏 -->
    <nav class="symph-nav">
      <div class="nav-left" @click="$router.push('/home')">
        <i class="fas fa-arrow-left nav-arrow"></i>
        <span class="nav-text">返回</span>
      </div>
      <div class="nav-brand">SYMPHONY</div>
      <div class="nav-spacer"></div>
    </nav>

    <!-- 主容器 -->
    <div class="mistakes-container">
      <!-- 侧边栏 -->
      <aside class="sidebar-minimal">
        <div class="sidebar-section">
          <div class="section-label">视图</div>
          <div class="view-switcher">
            <button
                :class="['view-option', { active: currentView === 'personal' }]"
                @click="currentView = 'personal'"
            >
              <span class="view-num">01</span>
              <span class="view-name">个人</span>
            </button>
            <button
                :class="['view-option', { active: currentView === 'shared' }]"
                @click="currentView = 'shared'"
            >
              <span class="view-num">02</span>
              <span class="view-name">共享</span>
            </button>
          </div>
        </div>

        <div class="sidebar-section">
          <div class="section-label">过滤条件</div>
          <div class="filter-tags">
            <div
                v-for="tag in tags"
                :key="tag.name"
                :class="['filter-tag', { active: currentTag === tag.name }]"
                @click="currentTag = tag.name"
            >
              <span class="tag-name">{{ tag.name }}</span>
              <span class="tag-counter">{{ tag.count }}</span>
            </div>
          </div>
        </div>

        <div class="sidebar-section share-zone">
          <div class="section-label">COLLABORATE</div>
          <p class="share-desc">Share your archive with peers for collective growth</p>
        </div>
      </aside>

      <!-- 主内容区 -->
      <main class="content-stage">
        <header class="stage-header">
          <div class="header-title-group">
            <h1 class="stage-title">MISTAKE ARCHIVE</h1>
            <p class="stage-subtitle">{{ filteredMistakes.length }} 道错题 / {{ currentView === 'personal' ? '个人' : '共享' }}</p>
          </div>
          <div class="header-actions">
            <button class="action-pill" @click="showAIAnalysis">
              <i class="fas fa-robot"></i>
              <span>AI分析</span>
            </button>
            <button class="action-pill primary" @click="showAddMistake">
              <i class="fas fa-plus"></i>
              <span>添加错题</span>
            </button>
          </div>
        </header>

        <div class="mistakes-grid">
          <div
              v-for="(mistake, idx) in filteredMistakes"
              :key="mistake.id"
              class="mistake-tile"
              @click="viewMistake(mistake)"
              :style="{ '--delay': idx * 0.05 + 's' }"
          >
            <div class="tile-glow"></div>
            <div class="tile-header">
              <span class="tile-index">{{ String(idx + 1).padStart(2, '0') }}</span>
              <div class="tile-tags">
                <span v-for="tag in mistake.tags" :key="tag" class="micro-tag">{{ tag }}</span>
              </div>
            </div>
            <div class="tile-question">{{ mistake.question }}</div>
            <div class="tile-divider"></div>
            <div class="tile-analysis">
              <div class="analysis-block">
                <span class="analysis-label">错误原因</span>
                <p class="analysis-text">{{ mistake.wrongReason }}</p>
              </div>
              <div class="analysis-block">
                <span class="analysis-label">解决方案</span>
                <p class="analysis-text">{{ mistake.correctAnswer }}</p>
              </div>
            </div>
            <footer class="tile-footer">
              <span v-if="currentView === 'personal'" class="footer-item" @click.stop="toggleStatus(mistake)">
                <i :class="mistake.statusIcon"></i>
                {{ mistake.status }}
              </span>
                          <span class="footer-item">
                <i class="fas fa-calendar"></i>
                {{ mistake.date }}
              </span>
                          <span v-if="currentView === 'personal'" class="footer-item danger" @click.stop="deleteMistake(mistake.id)">
                <i class="fas fa-trash"></i>
                删除
              </span>
            </footer>
          </div>

          <div v-if="filteredMistakes.length === 0" class="empty-archive">
            <div class="empty-icon">
              <i class="fas fa-archive"></i>
            </div>
            <div class="empty-message">没有找到错题</div>
            <div class="empty-hint">开始补充你的错题吧</div>
          </div>
        </div>
      </main>
    </div>

    <!-- AI分析弹窗 -->
    <transition name="modal-fade">
      <div v-if="aiAnalysisVisible" class="modal-backdrop" @click="closeAIAnalysis">
        <div class="modal-symphony ai-analysis-modal" @click.stop>
          <header class="modal-header">
            <div class="modal-title-group">
              <i class="fas fa-robot modal-icon"></i>
              <h2 class="modal-title">智能分析</h2>
            </div>
            <button class="modal-close" @click="closeAIAnalysis">
              <i class="fas fa-times"></i>
            </button>
          </header>

          <div class="modal-content">
            <div v-if="aiAnalyzing" class="ai-thinking">
              <div class="thinking-orbs">
                <div class="orb"></div>
                <div class="orb"></div>
                <div class="orb"></div>
              </div>
              <p class="thinking-text">分析类型中...</p>
            </div>

            <div v-else class="analysis-results">
              <div class="stats-panel">
                <div class="stat-card">
                  <div class="stat-value">{{ aiAnalysis.totalMistakes }}</div>
                  <div class="stat-label">总错题</div>
                </div>
                <div class="stat-card">
                  <div class="stat-value">{{ aiAnalysis.weakPoints }}</div>
                  <div class="stat-label">薄弱知识点</div>
                </div>
                <div class="stat-card">
                  <div class="stat-value">{{ aiAnalysis.masteryRate }}%</div>
                  <div class="stat-label">可记忆点</div>
                </div>
              </div>

              <div class="insight-section">
                <h3 class="insight-title">核心问题</h3>
                <ul class="insight-list">
                  <li v-for="(problem, i) in aiAnalysis.mainProblems" :key="i">{{ problem }}</li>
                </ul>
              </div>

              <div class="insight-section">
                <h3 class="insight-title">推荐</h3>
                <ul class="insight-list suggestions">
                  <li v-for="(suggestion, i) in aiAnalysis.suggestions" :key="i">{{ suggestion }}</li>
                </ul>
              </div>

              <div class="insight-section">
                <h3 class="insight-title">个人回顾</h3>
                <div class="priority-tags">
                  <span v-for="tag in aiAnalysis.reviewTopics" :key="tag" class="priority-tag">
                    {{ tag }}
                  </span>
                </div>
              </div>

              <div class="insight-section">
                <h3 class="insight-title">知识点分布</h3>
                <div id="radarChart" style="width: 100%; height: 360px;"></div>
              </div>
            </div>
          </div>

          <footer class="modal-footer">
            <button class="modal-btn secondary" @click="closeAIAnalysis">关闭</button>
            <button class="modal-btn primary" @click="generatePractice">
              <i class="fas fa-file-alt"></i>
              生成练习题
            </button>
          </footer>
        </div>
      </div>
    </transition>

    <!-- 添加/编辑错题弹窗 -->
    <transition name="modal-fade">
      <div v-if="addMistakeVisible" class="modal-backdrop" @click="closeAddMistake">
        <div class="modal-symphony entry-modal" @click.stop>
          <header class="modal-header">
            <div class="modal-title-group">
              <i class="fas fa-plus-circle modal-icon"></i>
              <h2 class="modal-title">{{ newMistake.id ? 'EDIT ENTRY' : 'NEW ENTRY' }}</h2>
            </div>
            <button class="modal-close" @click="closeAddMistake">
              <i class="fas fa-times"></i>
            </button>
          </header>

          <div class="modal-content">
            <form class="entry-form" @submit.prevent="submitMistake">
              <div class="form-field">
                <label class="field-label">
                  错题题目
                  <span v-if="newMistake.id" style="font-weight:400;color:var(--symph-gray);letter-spacing:0;margin-left:8px;">不可修改</span>
                </label>
                <textarea
                    v-model="newMistake.question"
                    class="field-input"
                    placeholder="输入题目信息..."
                    rows="4"
                    :required="!newMistake.id"
                    :disabled="!!newMistake.id"
                    :style="newMistake.id ? 'opacity:0.45;cursor:not-allowed;resize:none;' : ''"
                ></textarea>
              </div>

              <div class="form-field">
                <label class="field-label">错误原因</label>
                <textarea
                    v-model="newMistake.wrongReason"
                    class="field-input"
                    placeholder="为什么会错这题?"
                    rows="3"
                    required
                ></textarea>
              </div>

              <div class="form-field">
                <label class="field-label">正确答案</label>
                <textarea
                    v-model="newMistake.correctAnswer"
                    class="field-input"
                    placeholder="正确答案是?"
                    rows="3"
                    required
                ></textarea>
              </div>

              <div class="form-field">
                <label class="field-label">额外笔记</label>
                <textarea
                    v-model="newMistake.notes"
                    class="field-input"
                    placeholder="任何提供辅助的想法..."
                    rows="2"
                ></textarea>
              </div>
            </form>
          </div>

          <footer class="modal-footer">
            <template v-if="newMistake.isOwner">
              <button
                  v-if="currentView === 'shared'"
                  class="modal-btn secondary"
                  @click="toggleShare"
              >
                <i class="fas fa-lock"></i>
                私有化
              </button>
              <button
                  v-else
                  class="modal-btn secondary"
                  @click="toggleShare"
              >
                <i :class="newMistake.isShared ? 'fas fa-lock' : 'fas fa-share-alt'"></i>
                {{ newMistake.isShared ? '私有化' : '共享' }}
              </button>
              <button class="modal-btn primary" @click="submitMistake">
                <i class="fas fa-check"></i>
                保存
              </button>
            </template>
            <template v-else>
              <div class="viewer-notice">
                <i class="fas fa-info-circle"></i>
                查看共享错题
              </div>
              <button class="modal-btn secondary" @click="closeAddMistake">关闭</button>
            </template>
          </footer>
        </div>
      </div>
    </transition>

    <!-- 练习题弹窗 -->
    <transition name="modal-fade">
      <div v-if="practiceVisible" class="modal-backdrop" @click="closePractice">
        <div class="modal-symphony practice-modal" @click.stop>
          <header class="modal-header">
            <div class="modal-title-group">
              <i class="fas fa-file-alt modal-icon"></i>
              <h2 class="modal-title">练习题</h2>
            </div>
            <button class="modal-close" @click="closePractice">
              <i class="fas fa-times"></i>
            </button>
          </header>

          <div class="modal-content">
            <div v-if="practiceLoading" class="ai-thinking">
              <div class="thinking-orbs">
                <div class="orb"></div>
                <div class="orb"></div>
                <div class="orb"></div>
              </div>
              <p class="thinking-text">生成对应练习题中...</p>
            </div>

            <div v-else class="practice-set">
              <div
                  v-for="(q, index) in practiceQuestions"
                  :key="index"
                  class="practice-question"
              >
                <div class="question-header">
                  <span class="question-num">Q{{ String(index + 1).padStart(2, '0') }}</span>
                  <span class="question-topic">{{ q.knowledgePoint }}</span>
                </div>
                <div class="question-body">{{ q.question }}</div>
                <button class="reveal-btn" @click="toggleAnswer(index)">
                  <i :class="showAnswers[index] ? 'fas fa-eye-slash' : 'fas fa-eye'"></i>
                  {{ showAnswers[index] ? '隐藏答案' : '展示答案' }}
                </button>
                <div v-if="showAnswers[index]" class="answer-panel">
                  <div class="answer-section">
                    <div class="answer-label">答案</div>
                    <p>{{ q.answer }}</p>
                  </div>
                  <div class="answer-section">
                    <div class="answer-label">解析</div>
                    <p>{{ q.explanation }}</p>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <footer class="modal-footer">
            <button class="modal-btn secondary" @click="closePractice">关闭</button>
            <button class="modal-btn primary" @click="generatePractice" :disabled="practiceLoading">
              <i class="fas fa-redo"></i>
              重新生成
            </button>
          </footer>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import gsap from 'gsap'
import { nextTick } from 'vue'
import * as echarts from 'echarts'

const router = useRouter()

const currentView = ref('personal')
const currentTag = ref('全部')

const practiceVisible = ref(false)
const practiceLoading = ref(false)
const practiceQuestions = ref([])
const showAnswers = ref([])

const mistakes = ref([])

//动态计算标签和计数
const tags = computed(() => {
  const tagMap = {}
  let totalCount = 0

  mistakes.value.forEach(mistake => {
    totalCount++
    if (mistake.tags && mistake.tags.length > 0) {
      mistake.tags.forEach(tag => {
        if (tagMap[tag]) {
          tagMap[tag]++
        } else {
          tagMap[tag] = 1
        }
      })
    }
  })

  //构建标签数组，"全部" 放在最前面
  const result = [{ name: '全部', count: totalCount }]

  //按计数降序排列其他标签
  Object.entries(tagMap)
      .sort((a, b) => b[1] - a[1])
      .forEach(([name, count]) => {
        result.push({ name, count })
      })

  return result
})

//按标签过滤错题
const filteredMistakes = computed(() => {
  if (currentTag.value === '全部') {
    return mistakes.value
  }
  return mistakes.value.filter(m => m.tags && m.tags.includes(currentTag.value))
})

//AI分析相关
const aiAnalysisVisible = ref(false)
const aiAnalyzing = ref(false)
const aiAnalysis = ref({
  totalMistakes: 0,
  weakPoints: 0,
  masteryRate: 0,
  mainProblems: [],
  suggestions: [],
  reviewTopics: []
})

//调接口拿分析结果，解析AI文字
const showAIAnalysis = async () => {
  aiAnalysisVisible.value = true
  aiAnalyzing.value = true
  let radarData = []
  try {
    const token = localStorage.getItem('token')
    const res = await fetch('/api/mistakes/analysis', {
      headers: { 'Authorization': 'Bearer ' + token }
    })
    const data = await res.json()

    if (data.code === 200) {
      const { statistic, aiAnalysis: aiText, totalCount } = data.data

      const reviewTopics = statistic
          ? Object.keys(statistic).filter(k => k !== '总错题数' && statistic[k] > 0)
          : []

      radarData = reviewTopics
          .map(k => ({ label: k, value: statistic[k] }))
          .sort((a, b) => b.value - a.value)
          .slice(0, 8)

      const mainProblems = []
      const suggestions = []

      if (aiText && typeof aiText === 'string') {
        const lines = aiText.split('\n')
        let section = ''
        //按行解析AI返回的文字，识别"主要问题"和"学习建议"两个章节
        for (const line of lines) {
          const trimmed = line.trim()
          if (trimmed.includes('主要问题') || trimmed.includes('问题')) {
            section = 'problems'
          } else if (trimmed.includes('学习建议') || trimmed.includes('建议')) {
            section = 'suggestions'
          } else if (trimmed.startsWith('-') || trimmed.startsWith('•') || trimmed.startsWith('·')) {
            const content = trimmed.replace(/^[-•·]\s*/, '').trim()
            if (content) {
              if (section === 'problems') mainProblems.push(content)
              if (section === 'suggestions') suggestions.push(content)
            }
          }
        }
      }

      if (mainProblems.length === 0) mainProblems.push('暂无足够数据进行分析')
      if (suggestions.length === 0) suggestions.push('继续积累错题，获得更精准的学习建议')

      //普通立即执行函数，不用 computed
      const total = filteredMistakes.value.length
      const unmastered = filteredMistakes.value.filter(m => m.status !== '已掌握').length
      const masteryRate = total === 0 ? 0 : Math.round(((total - unmastered) / total) * 100)

      aiAnalysis.value = {
        totalMistakes: totalCount || 0,
        weakPoints: reviewTopics.length,
        masteryRate,
        mainProblems,
        suggestions,
        reviewTopics,
        radarData
      }
    }
  } catch (e) {
    console.error('AI分析请求失败', e)
    aiAnalysis.value = {
      totalMistakes: 0,
      weakPoints: 0,
      masteryRate: 0,
      mainProblems: ['分析失败，请稍后重试'],
      suggestions: ['请检查网络连接'],
      reviewTopics: []
    }
  } finally {
    aiAnalyzing.value = false
    await nextTick()
    const chartDom = document.getElementById('radarChart')
    if (chartDom && radarData.length > 0) {
      //分析完后用ECharts画雷达图，展示各知识点错题数
      const myChart = echarts.init(chartDom)
      myChart.setOption({
        radar: {
          indicator: radarData.map(d => ({
            name: d.label,
            max: Math.max(...radarData.map(r => r.value)) + 2
          })),
          shape: 'polygon',
          splitNumber: 4,
          axisName: { color: '#737373', fontSize: 11 },
          splitLine: { lineStyle: { color: 'rgba(0,0,0,0.08)' } },
          splitArea: { show: false },
          axisLine: { lineStyle: { color: 'rgba(0,0,0,0.1)' } }
        },
        series: [{
          type: 'radar',
          data: [{
            value: radarData.map(d => d.value),
            name: '错题数',
            areaStyle: { color: 'rgba(0,57,166,0.08)' },
            lineStyle: { color: '#0039a6', width: 2 },
            itemStyle: { color: '#0039a6' },
            symbol: 'circle',
            symbolSize: 6
          }]
        }],
        tooltip: { trigger: 'item' }
      })
    }
  }
}

const closeAIAnalysis = () => {
  aiAnalysisVisible.value = false
}

//根据薄弱知识点生成练习题
const generatePractice = async () => {
  closeAIAnalysis()
  practiceVisible.value = true
  practiceLoading.value = true
  practiceQuestions.value = []
  showAnswers.value = []

  try {
    const token = localStorage.getItem('token')
    const res = await fetch('/api/mistakes/practice', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + token
      },
      body: JSON.stringify({
        weakPoints: aiAnalysis.value.reviewTopics
      })
    })
    const data = await res.json()
    if (data.code === 200) {
      practiceQuestions.value = data.data
      showAnswers.value = new Array(data.data.length).fill(false)
    }
  } catch (e) {
    console.error('生成练习题失败', e)
  } finally {
    practiceLoading.value = false
  }
}

const closePractice = () => {
  practiceVisible.value = false
}

//展开/折叠答案
const toggleAnswer = (index) => {
  showAnswers.value[index] = !showAnswers.value[index]
}

// 添加错题相关
const addMistakeVisible = ref(false)
const newMistake = ref({
  id: null,
  question: '',
  wrongReason: '',
  correctAnswer: '',
  notes: ''
})

//加载错题列表
const loadMistakes = async () => {
  const token = localStorage.getItem('token')
  const res = await fetch(`/api/mistakes/list?view=${currentView.value}`, {
    headers: { 'Authorization': 'Bearer ' + token }
  })
  const data = await res.json()
  if (data.code === 200) {
    mistakes.value = data.data.map(m => ({
      id: m.id,
      creatorId: m.userId,
      question: m.question,
      wrongReason: m.wrongReason,
      correctAnswer: m.correctAnswer,
      notes: m.notes,
      isShared: m.isShared,
      tags: m.knowledgePoints ? m.knowledgePoints.split(',') : [],
      date: m.createdAt?.split('T')[0],
      retries: m.retries,
      status: m.status,
      statusIcon: m.status === '已掌握' ? 'fas fa-check-circle' : m.status === '复习中' ? 'fas fa-sync' : 'fas fa-clock'
    }))

    //动态计算标签统计
    loadTagStats()
  }
}

const loadTagStats = () => {
  //统计所有知识点
  const tagCountMap = {}
  let totalCount = 0

  mistakes.value.forEach(mistake => {
    totalCount++
    mistake.tags.forEach(tag => {
      if (tag && tag.trim()) {
        tagCountMap[tag] = (tagCountMap[tag] || 0) + 1
      }
    })
  })

  // 构建标签列表
  const newTags = [{ name: '全部', count: totalCount }]

  // 按数量排序，取前10个
  Object.entries(tagCountMap)
      .sort((a, b) => b[1] - a[1])
      .slice(0, 10)
      .forEach(([name, count]) => {
        newTags.push({ name, count })
      })

  tags.value = newTags
}

const showAddMistake = () => {
  newMistake.value = {
    id: null,
    question: '',
    wrongReason: '',
    correctAnswer: '',
    notes: '',
    isOwner: true
  }
  addMistakeVisible.value = true
}

const closeAddMistake = () => {
  addMistakeVisible.value = false
  newMistake.value = {
    question: '',
    tags: [],
    wrongReason: '',
    correctAnswer: '',
    notes: ''
  }
}

//切换复习状态
const toggleStatus = async (mistake) => {
  const statusMap = {
    '待复习': '复习中',
    '复习中': '已掌握',
    '已掌握': '待复习'
  }
  const newStatus = statusMap[mistake.status]
  const token = localStorage.getItem('token')
  await fetch(`/api/mistakes/${mistake.id}/status`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer ' + token
    },
    body: JSON.stringify({ status: newStatus })
  })
  mistake.status = newStatus
  mistake.statusIcon = newStatus === '已掌握' ? 'fas fa-check-circle' : newStatus === '复习中' ? 'fas fa-sync' : 'fas fa-clock'
}

//删除错题
const deleteMistake = async (id) => {
  if (!confirm('确定删除这道错题吗？')) return
  const token = localStorage.getItem('token')
  const res = await fetch(`/api/mistakes/${id}`, {
    method: 'DELETE',
    headers: { 'Authorization': 'Bearer ' + token }
  })
  const data = await res.json()
  if (data.code === 200) {
    await loadMistakes()
  }
}

//新增或编辑错题
const submitMistake = async () => {
  if (!newMistake.value.question.trim()) { window.$toast.info('请输入题目内容'); return }
  if (!newMistake.value.wrongReason.trim()) { window.$toast.info('请输入错误原因'); return }
  if (!newMistake.value.correctAnswer.trim()) { window.$toast.info('请输入正确思路'); return }

  const token = localStorage.getItem('token')
  //用id判断是新增还是编辑
  const isEdit = !!newMistake.value.id
  const url = isEdit ? `/api/mistakes/${newMistake.value.id}` : '/api/mistakes/add'
  const method = isEdit ? 'PUT' : 'POST'

  const res = await fetch(url, {
    method,
    headers: {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer ' + token
    },
    body: JSON.stringify({
      question: newMistake.value.question,
      wrongReason: newMistake.value.wrongReason,
      correctAnswer: newMistake.value.correctAnswer,
      notes: newMistake.value.notes
    })
  })
  const data = await res.json()
  if (data.code === 200) {
    await loadMistakes()
    closeAddMistake()
  }
}

//点击查看错题
const viewMistake = (mistake) => {
  //把错题数据填入表单，打开弹窗
  //判断是否是自己的错题且只有自己的才能编辑删除
  const currentUserId = localStorage.getItem('currentUserId')
  newMistake.value = {
    id: mistake.id,
    question: mistake.question,
    wrongReason: mistake.wrongReason,
    correctAnswer: mistake.correctAnswer,
    notes: mistake.notes || '',
    isShared: mistake.isShared || 0,
    creatorId: mistake.creatorId,
    isOwner: String(mistake.creatorId) === String(currentUserId)
  }
  addMistakeVisible.value = true
}

//切换公开/私密
const toggleShare = async () => {
  const newSharedStatus = currentView.value === 'shared' ? 0 : (newMistake.value.isShared ? 0 : 1)

  const token = localStorage.getItem('token')
  try {
    const res = await fetch(`/api/mistakes/${newMistake.value.id}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + token
      },
      body: JSON.stringify({ isShared: newSharedStatus })
    })

    if (res.ok) {
      newMistake.value.isShared = newSharedStatus
      await loadMistakes()

      if (currentView.value === 'shared') {
        closeAddMistake()
      }
    }
  } catch (error) {
    console.error("更新共享状态失败", error)
  }
}

//切换personal/shared视图时重新加载，并检查当前标签是否还存在
watch(currentView, () => {
  loadMistakes()
  // 切换视图后，如果当前选中的标签在新视图中不存在，则重置为"全部"
  setTimeout(() => {
    const tagExists = tags.value.some(t => t.name === currentTag.value)
    if (!tagExists) {
      currentTag.value = '全部'
    }
  }, 100)
})

//加载错题->侧边栏从左滑入->内容区从下淡入->错题卡片依次入场
onMounted(() => {
  loadMistakes()

  gsap.from('.sidebar-minimal', {
    x: -60,
    opacity: 0,
    duration: 0.8,
    ease: "power3.out"
  })

  gsap.from('.content-stage', {
    y: 40,
    opacity: 0,
    duration: 0.8,
    ease: "power3.out",
    delay: 0.1
  })

  gsap.from('.mistake-tile', {
    y: 30,
    opacity: 0,
    duration: 0.6,
    stagger: 0.08,
    ease: "power2.out",
    delay: 0.3
  })
})
</script>

<style>
@import url('https://fonts.googleapis.com/css2?family=Cormorant+Garamond:ital,wght@0,400;0,600;1,400;1,600&family=Inter:wght@300;400;500;600;700&display=swap');

:root {
  --symph-black: #1a1a1a;
  --symph-white: #fdfdfc;
  --symph-gray: #737373;
  --symph-light-gray: #e5e5e5;
  --symph-blue: #0039a6;
  --symph-border: rgba(0, 0, 0, 0.1);
  --symph-shadow: rgba(0, 0, 0, 0.04);
}
</style>

<style scoped>
* { margin: 0; padding: 0; box-sizing: border-box; }

.symphony-mistakes {
  min-height: 100vh;
  background: var(--symph-white);
  font-family: 'Inter', -apple-system, sans-serif;
  color: var(--symph-black);
}

/* 导航栏 */
.symph-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 60px;
  border-bottom: 1px solid var(--symph-border);
  background: var(--symph-white);
}

.nav-left {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  transition: opacity 0.3s;
}

.nav-left:hover {
  opacity: 0.6;
}

.nav-arrow {
  font-size: 14px;
  color: var(--symph-gray);
}

.nav-text {
  font-size: 11px;
  letter-spacing: 2px;
  font-weight: 600;
  color: var(--symph-gray);
}

.nav-brand {
  font-family: 'Cormorant Garamond', serif;
  font-size: 24px;
  font-weight: 600;
  letter-spacing: 4px;
  color: var(--symph-black);
}

.nav-spacer {
  width: 80px;
}

/* 主容器 */
.mistakes-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 40px 60px;
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: 60px;
}

/* 侧边栏 */
.sidebar-minimal {
  position: sticky;
  top: 40px;
  height: fit-content;
}

.sidebar-section {
  margin-bottom: 40px;
}

.section-label {
  font-size: 10px;
  letter-spacing: 2px;
  font-weight: 700;
  color: var(--symph-gray);
  margin-bottom: 16px;
}

.view-switcher {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.view-option {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border: 1px solid var(--symph-border);
  background: transparent;
  cursor: pointer;
  transition: all 0.3s;
  font-family: inherit;
}

.view-option:hover {
  border-color: var(--symph-black);
}

.view-option.active {
  background: var(--symph-black);
  border-color: var(--symph-black);
}

.view-num {
  font-family: 'Cormorant Garamond', serif;
  font-style: italic;
  font-size: 18px;
  color: var(--symph-blue);
  transition: color 0.3s;
}

.view-option.active .view-num {
  color: var(--symph-white);
}

.view-name {
  font-size: 11px;
  letter-spacing: 1.5px;
  font-weight: 600;
  color: var(--symph-black);
  transition: color 0.3s;
}

.view-option.active .view-name {
  color: var(--symph-white);
}

.filter-tags {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.filter-tag {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  border-left: 2px solid transparent;
  cursor: pointer;
  transition: all 0.3s;
}

.filter-tag:hover {
  border-left-color: var(--symph-blue);
  background: rgba(0, 57, 166, 0.03);
}

.filter-tag.active {
  border-left-color: var(--symph-black);
  background: rgba(0, 0, 0, 0.03);
}

.tag-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--symph-black);
}

.tag-counter {
  font-size: 11px;
  color: var(--symph-gray);
  font-weight: 600;
}

.share-zone {
  padding-top: 40px;
  border-top: 1px solid var(--symph-border);
}

.share-desc {
  font-size: 11px;
  line-height: 1.6;
  color: var(--symph-gray);
  margin-bottom: 16px;
}

.share-invite-btn {
  width: 100%;
  padding: 12px;
  border: 1px solid var(--symph-black);
  background: transparent;
  font-size: 10px;
  letter-spacing: 1.5px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-family: inherit;
}

.share-invite-btn:hover {
  background: var(--symph-black);
  color: var(--symph-white);
}

/* 主内容区 */
.content-stage {
  min-height: 600px;
}

.stage-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 40px;
}

.header-title-group {
  flex: 1;
}

.stage-title {
  font-size: 36px;
  font-weight: 300;
  letter-spacing: 2px;
  margin-bottom: 8px;
}

.stage-subtitle {
  font-size: 11px;
  letter-spacing: 1px;
  color: var(--symph-gray);
  font-weight: 500;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.action-pill {
  padding: 10px 20px;
  border: 1px solid var(--symph-border);
  background: transparent;
  font-size: 10px;
  letter-spacing: 1.5px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: inherit;
}

.action-pill:hover {
  border-color: var(--symph-black);
}

.action-pill.primary {
  background: var(--symph-black);
  color: var(--symph-white);
  border-color: var(--symph-black);
}

.action-pill.primary:hover {
  background: var(--symph-blue);
  border-color: var(--symph-blue);
  transform: translateY(-2px);
  box-shadow: 0 10px 30px rgba(0, 57, 166, 0.2);
}

/* 错题网格 */
.mistakes-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(380px, 1fr));
  gap: 24px;
}

.mistake-tile {
  position: relative;
  border: 1px solid var(--symph-border);
  background: var(--symph-white);
  padding: 24px;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  animation: fadeInUp 0.6s ease-out backwards;
  animation-delay: var(--delay);
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.mistake-tile::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 2px;
  background: var(--symph-blue);
  transform: scaleX(0);
  transform-origin: left;
  transition: transform 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.mistake-tile:hover {
  border-color: var(--symph-black);
  transform: translateY(-4px);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.1);
}

.mistake-tile:hover::before {
  transform: scaleX(1);
}

.tile-glow {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: radial-gradient(circle at var(--mouse-x, 50%) var(--mouse-y, 50%), rgba(0, 57, 166, 0.05) 0%, transparent 50%);
  opacity: 0;
  transition: opacity 0.3s;
  pointer-events: none;
}

.mistake-tile:hover .tile-glow {
  opacity: 1;
}

.tile-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.tile-index {
  font-family: 'Cormorant Garamond', serif;
  font-style: italic;
  font-size: 28px;
  color: var(--symph-blue);
}

.tile-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.micro-tag {
  font-size: 9px;
  letter-spacing: 1px;
  font-weight: 700;
  color: var(--symph-gray);
  padding: 4px 8px;
  border: 1px solid var(--symph-border);
}

.tile-question {
  font-size: 15px;
  font-weight: 500;
  line-height: 1.6;
  color: var(--symph-black);
  margin-bottom: 16px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.tile-divider {
  height: 1px;
  background: var(--symph-border);
  margin: 16px 0;
}

.tile-analysis {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 16px;
}

.analysis-block {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.analysis-label {
  font-size: 9px;
  letter-spacing: 1.5px;
  font-weight: 700;
  color: var(--symph-gray);
}

.analysis-text {
  font-size: 13px;
  line-height: 1.6;
  color: var(--symph-gray);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.tile-footer {
  display: flex;
  align-items: center;
  gap: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--symph-border);
  font-size: 10px;
  color: var(--symph-gray);
}

.footer-item {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  transition: color 0.3s;
  letter-spacing: 0.5px;
}

.footer-item:hover {
  color: var(--symph-black);
}

.footer-item.danger:hover {
  color: #d32f2f;
}

/* 空状态 */
.empty-archive {
  grid-column: 1 / -1;
  text-align: center;
  padding: 100px 20px;
}

.empty-icon {
  font-size: 60px;
  color: var(--symph-light-gray);
  margin-bottom: 20px;
}

.empty-message {
  font-size: 16px;
  letter-spacing: 2px;
  font-weight: 600;
  color: var(--symph-black);
  margin-bottom: 8px;
}

.empty-hint {
  font-size: 12px;
  color: var(--symph-gray);
}

/* 弹窗基础样式 */
.modal-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.modal-symphony {
  background: var(--symph-white);
  border: 1px solid var(--symph-border);
  max-width: 700px;
  width: 100%;
  max-height: 85vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 40px 100px rgba(0, 0, 0, 0.15);
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 30px 40px;
  border-bottom: 1px solid var(--symph-border);
}

.modal-title-group {
  display: flex;
  align-items: center;
  gap: 12px;
}

.modal-icon {
  font-size: 20px;
  color: var(--symph-blue);
}

.modal-title {
  font-size: 18px;
  letter-spacing: 2px;
  font-weight: 600;
  color: var(--symph-black);
}

.modal-close {
  width: 36px;
  height: 36px;
  border: 1px solid var(--symph-border);
  background: transparent;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}

.modal-close:hover {
  background: var(--symph-black);
  color: var(--symph-white);
  border-color: var(--symph-black);
}

.modal-content {
  flex: 1;
  overflow-y: auto;
  padding: 40px;
}

.modal-footer {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  padding: 24px 40px;
  border-top: 1px solid var(--symph-border);
}

.modal-btn {
  padding: 12px 28px;
  border: 1px solid var(--symph-border);
  background: transparent;
  font-size: 10px;
  letter-spacing: 1.5px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: inherit;
}

.modal-btn:hover {
  border-color: var(--symph-black);
}

.modal-btn.primary {
  background: var(--symph-black);
  color: var(--symph-white);
  border-color: var(--symph-black);
}

.modal-btn.primary:hover {
  background: var(--symph-blue);
  border-color: var(--symph-blue);
}

.modal-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* AI 思考动画 */
.ai-thinking {
  text-align: center;
  padding: 80px 20px;
}

.thinking-orbs {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-bottom: 30px;
}

.orb {
  width: 12px;
  height: 12px;
  background: var(--symph-black);
  border-radius: 50%;
  animation: orbit 1.2s ease-in-out infinite;
}

.orb:nth-child(1) {
  animation-delay: 0s;
}

.orb:nth-child(2) {
  animation-delay: 0.2s;
}

.orb:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes orbit {
  0%, 100% {
    transform: scale(0.8);
    opacity: 0.5;
  }
  50% {
    transform: scale(1.2);
    opacity: 1;
  }
}

.thinking-text {
  font-size: 11px;
  letter-spacing: 2px;
  font-weight: 600;
  color: var(--symph-gray);
}

/* AI 分析结果 */
.analysis-results {
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.stats-panel {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.stat-card {
  border: 1px solid var(--symph-border);
  padding: 24px;
  text-align: center;
  transition: all 0.3s;
}

.stat-card:hover {
  border-color: var(--symph-blue);
  transform: translateY(-4px);
}

.stat-value {
  font-size: 36px;
  font-weight: 300;
  color: var(--symph-black);
  margin-bottom: 8px;
}

.stat-label {
  font-size: 10px;
  letter-spacing: 1.5px;
  font-weight: 700;
  color: var(--symph-gray);
}

.insight-section {
  border-left: 2px solid var(--symph-border);
  padding-left: 24px;
}

.insight-title {
  font-size: 11px;
  letter-spacing: 2px;
  font-weight: 700;
  color: var(--symph-black);
  margin-bottom: 16px;
}

.insight-list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.insight-list li {
  padding-left: 24px;
  position: relative;
  font-size: 13px;
  line-height: 1.6;
  color: var(--symph-gray);
}

.insight-list li::before {
  content: '—';
  position: absolute;
  left: 0;
  color: var(--symph-blue);
  font-weight: 700;
}

.priority-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.priority-tag {
  padding: 8px 16px;
  border: 1px solid var(--symph-black);
  font-size: 11px;
  letter-spacing: 1px;
  font-weight: 600;
  color: var(--symph-black);
  transition: all 0.3s;
}

.priority-tag:hover {
  background: var(--symph-black);
  color: var(--symph-white);
}

/* 表单样式 */
.entry-form {
  display: flex;
  flex-direction: column;
  gap: 28px;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.field-label {
  font-size: 10px;
  letter-spacing: 2px;
  font-weight: 700;
  color: var(--symph-black);
}

.field-input {
  width: 100%;
  padding: 16px;
  border: 1px solid var(--symph-border);
  background: transparent;
  font-size: 14px;
  font-family: inherit;
  line-height: 1.6;
  color: var(--symph-black);
  resize: vertical;
  transition: all 0.3s;
}

.field-input:focus {
  outline: none;
  border-color: var(--symph-black);
}

.field-input::placeholder {
  color: var(--symph-gray);
  opacity: 0.5;
}

.viewer-notice {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11px;
  letter-spacing: 1px;
  color: var(--symph-gray);
}

/* 练习题样式 */
.practice-set {
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.practice-question {
  border: 1px solid var(--symph-border);
  padding: 24px;
}

.question-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.question-num {
  font-family: 'Cormorant Garamond', serif;
  font-style: italic;
  font-size: 24px;
  color: var(--symph-blue);
}

.question-topic {
  font-size: 10px;
  letter-spacing: 1.5px;
  font-weight: 700;
  color: var(--symph-gray);
  padding: 4px 12px;
  border: 1px solid var(--symph-border);
}

.question-body {
  font-size: 14px;
  line-height: 1.7;
  color: var(--symph-black);
  margin-bottom: 20px;
}

.reveal-btn {
  padding: 10px 20px;
  border: 1px solid var(--symph-border);
  background: transparent;
  font-size: 10px;
  letter-spacing: 1.5px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: inherit;
  margin-bottom: 16px;
}

.reveal-btn:hover {
  background: var(--symph-black);
  color: var(--symph-white);
  border-color: var(--symph-black);
}

.answer-panel {
  border-top: 1px solid var(--symph-border);
  padding-top: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  animation: slideDown 0.3s ease-out;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.answer-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.answer-label {
  font-size: 10px;
  letter-spacing: 2px;
  font-weight: 700;
  color: var(--symph-black);
}

.answer-section p {
  font-size: 13px;
  line-height: 1.7;
  color: var(--symph-gray);
}

/* 弹窗动画 */
.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: all 0.3s ease;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}

.modal-fade-enter-from .modal-symphony,
.modal-fade-leave-to .modal-symphony {
  transform: scale(0.95) translateY(20px);
}

/* 响应式 */
@media (max-width: 1200px) {
  .mistakes-container {
    grid-template-columns: 1fr;
    gap: 40px;
  }

  .sidebar-minimal {
    position: static;
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 20px;
  }

  .sidebar-section {
    margin-bottom: 0;
  }

  .share-zone {
    border-top: none;
    padding-top: 0;
  }
}

@media (max-width: 768px) {
  .symph-nav {
    padding: 16px 20px;
  }

  .mistakes-container {
    padding: 20px;
  }

  .sidebar-minimal {
    grid-template-columns: 1fr;
  }

  .mistakes-grid {
    grid-template-columns: 1fr;
  }

  .stage-header {
    flex-direction: column;
    gap: 20px;
  }

  .modal-symphony {
    max-width: 100%;
  }

  .modal-header,
  .modal-content,
  .modal-footer {
    padding: 20px;
  }

  .stats-panel {
    grid-template-columns: 1fr;
  }
}
</style>