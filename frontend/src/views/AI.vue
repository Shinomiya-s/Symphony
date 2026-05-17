<template>
  <div class="symphony-ai-page">
    <nav class="ai-nav">
      <button class="nav-return" @click="$router.push('/home')">
        <i class="fas fa-arrow-left"></i>
        <span>返回首页</span>
      </button>
      <div class="nav-logo">
        <span class="logo-icon">🎺</span>
        <span class="logo-text">SYMPHONY AI</span>
      </div>
      <div class="nav-spacer"></div>
    </nav>

    <div class="ai-container">
      <div class="metrics-panel">
        <div
            v-for="(stat, idx) in stats"
            :key="stat.label"
            class="metric-card"
            :style="{ '--metric-index': idx }"
        >
          <div class="metric-icon" :style="{ background: stat.color }">
            <i :class="stat.icon"></i>
          </div>
          <div class="metric-content">
            <div class="metric-value">{{ stat.value }}</div>
            <div class="metric-label">{{ stat.label }}</div>
          </div>
        </div>
      </div>

      <div class="ai-section">
        <header class="section-header">
          <div class="header-title-group">
            <h1 class="section-title">个人 AI 助理</h1>
            <p class="section-subtitle">{{ aiList.length }} 个活跃实例 / ACTIVE INSTANCES</p>
          </div>
          <button class="create-ai-btn" @click="createAI">
            <i class="fas fa-plus"></i>
            <span>新建助理</span>
          </button>
        </header>

        <div class="ai-grid">
          <div
              v-for="(ai, index) in aiList"
              :key="ai.id"
              class="ai-instance"
              :style="{ '--ai-index': index }"
          >
            <div class="instance-border"></div>

            <div class="instance-header">
              <div class="instance-avatar" :style="{ background: ai.gradient }">
                {{ ai.emoji }}
              </div>
              <div :class="['instance-status', ai.statusClass]">
                <span class="status-indicator"></span>
                <span class="status-text">{{ ai.statusText === 'Active' ? '运行中' : '离线' }}</span>
              </div>
            </div>

            <div class="instance-info">
              <h3 class="instance-name">{{ ai.name }}</h3>
              <p class="instance-desc">{{ ai.desc }}</p>
            </div>

            <div class="instance-metrics">
              <div class="instance-metric" v-if="ai.ownerId !== 'SYSTEM'">
                <span class="metric-num">{{ ai.conversations }}</span>
                <span class="metric-lbl">对话次数</span>
              </div>
              <div class="metric-divider" v-if="ai.ownerId !== 'SYSTEM'"></div>
              <div class="instance-metric" v-if="ai.ownerId !== 'SYSTEM'">
                <span class="metric-num">{{ ai.created }}</span>
                <span class="metric-lbl">创建日期</span>
              </div>
              <div v-if="ai.ownerId === 'SYSTEM'" class="system-ai-hint">
                <i class="fas fa-shield-alt"></i>
                系统预设助理
              </div>
            </div>

            <div class="instance-tags">
              <span v-for="tag in ai.tags" :key="tag" class="instance-tag">{{ tag }}</span>
            </div>

            <div class="instance-actions">
              <button class="action-primary" @click="chatWithAI(ai)">
                <i :class="ai.active ? 'fas fa-comment-dots' : 'fas fa-play'"></i>
                {{ ai.active ? '开始对话' : '激活助理' }}
              </button>
              <button class="action-secondary" @click="manageAI(ai)">
                <i class="fas fa-cog"></i>
                管理配置
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <transition name="modal-fade">
      <div v-if="showManageModal" class="modal-backdrop" @click.self="closeManageModal">
        <div class="modal-ai">
          <header class="modal-ai-header">
            <div class="modal-title-group">
              <i class="fas fa-robot"></i>
              <h2 class="modal-title">{{ isDefaultAI ? '助理详情' : '编辑助理配置' }}</h2>
            </div>
            <button class="modal-close" @click="closeManageModal">
              <i class="fas fa-times"></i>
            </button>
          </header>

          <div class="modal-ai-body">
            <div v-if="isDefaultAI" class="system-notice">
              <i class="fas fa-info-circle"></i>
              <span>系统预设助理不支持修改或删除</span>
            </div>

            <div class="config-field">
              <label class="field-label">
                <i class="fas fa-signature"></i>
                助理名称 / NAME
              </label>
              <input
                  v-model="editingAI.name"
                  type="text"
                  class="field-input"
                  placeholder="请输入助理名称"
                  :disabled="isDefaultAI"
              />
            </div>

            <div class="config-field">
              <label class="field-label">
                <i class="fas fa-align-left"></i>
                职能描述 / DESCRIPTION
              </label>
              <textarea
                  v-model="editingAI.desc"
                  class="field-textarea"
                  placeholder="简述该助理的核心功能与专长..."
                  rows="3"
                  :disabled="isDefaultAI"
              ></textarea>
            </div>

            <div class="config-field">
              <label class="field-label">
                <i class="fas fa-code"></i>
                系统提示词 / SYSTEM PROMPT
              </label>
              <textarea
                  v-model="editingAI.prompt"
                  class="field-textarea"
                  placeholder="在此定义助理的行为逻辑、人格设定与知识边界"
                  rows="5"
                  :disabled="isDefaultAI"
              ></textarea>
            </div>
          </div>

          <footer class="modal-ai-footer">
            <button
                v-if="!isDefaultAI"
                class="modal-action danger"
                @click="confirmDelete"
            >
              <i class="fas fa-trash-alt"></i>
              删除助理
            </button>
            <div class="footer-spacer"></div>
            <button class="modal-action secondary" @click="closeManageModal">
              取消
            </button>
            <button
                v-if="!isDefaultAI"
                class="modal-action primary"
                @click="saveAI"
            >
              <i class="fas fa-save"></i>
              保存修改
            </button>
          </footer>
        </div>
      </div>
    </transition>

    <transition name="modal-fade">
      <div v-if="showDeleteConfirm" class="modal-backdrop" @click.self="showDeleteConfirm = false">
        <div class="confirm-dialog">
          <div class="confirm-icon">
            <i class="fas fa-exclamation-triangle"></i>
          </div>
          <h3 class="confirm-title">确认删除助理</h3>
          <p class="confirm-text">
            您确定要永久移除 <strong>{{ editingAI.name }}</strong> 吗？
          </p>
          <p class="confirm-warning">此操作不可撤销，该助理的所有对话历史记录都将被一并清除。</p>
          <div class="confirm-actions">
            <button class="modal-action secondary" @click="showDeleteConfirm = false">
              点错了，返回
            </button>
            <button class="modal-action danger" @click="deleteAI">
              确认删除
            </button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import gsap from 'gsap'
import axios from "axios"

const router = useRouter()

const accountData = ref({
  userId: '',
  username: '',
  nickname: '',
  email: '',
  phone: '',
  bio: '',
  createdAt: '',
  mistakeCount: 0,
  aiCount: 0,
  totalConversations: 0,
  knowledgeMastery: 0,
  emailNotify: true,
  studyReminder: true,
  aiSuggestion: false,
  mistakePublic: false
})

//弹窗相关状态
const showManageModal = ref(false)
const showDeleteConfirm = ref(false)
const editingAI = ref({
  id: null,
  name: '',
  desc: '',
  prompt: '',
  tags: [],
  emoji: '',
  gradient: ''
})

//默认AI的ID列表（系统AI不能被编辑或删除）
const DEFAULT_AI_IDS = [1, 2]

//判断当前编辑的AI是否为默认AI
const isDefaultAI = computed(() => {
  return editingAI.value.ownerId === 'SYSTEM'
})

//格式化知识掌握度的工具函数
const formatMastery = (val) => {
  if (!val) return '0%';
  return Math.round(val) + '%';
}

//拉取个人信息
const loadUserInfo = async () => {
  try {
    const token = localStorage.getItem('token')
    const res = await fetch('/api/auth/profile', {
      method: 'GET',
      headers: { 'Authorization': 'Bearer ' + token }
    })
    const result = await res.json()
    if (result.code === 200) {
      accountData.value = { ...accountData.value, ...result.data }
    }
  } catch (e) {
    console.error('加载用户信息失败', e)
  }
}

//计算注册天数
const registrationDays = computed(() => {
  if (!accountData.value.createdAt) return 1;
  const start = new Date(accountData.value.createdAt);
  const now = new Date();
  const diffTime = Math.abs(now.getTime() - start.getTime());
  //用当前时间减去注册时间，转换成天数
  return Math.ceil(diffTime / (1000 * 60 * 60 * 24)) || 1;
});

//四个统计卡片的数据,直接绑定accountData里的字段,数据变了卡片自动更新
const stats = computed(() =>[
  {
    icon: 'fas fa-robot',
    color: '#2c3e50',
    value: accountData.value.aiCount || 2,
    label: '助理总数'
  },
  {
    icon: 'fas fa-comments',
    color: '#4a90e2',
    value: accountData.value.totalConversations||0,
    label: '对话记录'
  },
  {
    icon: 'fas fa-brain',
    color: '#d4af37',
    value: formatMastery(accountData.value.knowledgeMastery)||'0%',
    label: '知识掌握'
  },
  {
    icon: 'fas fa-fire',
    color: '#613125',
    value: registrationDays.value,
    label: '活跃天数'
  }
])

const aiList = ref([])

//拉取AI列表
const fetchMyAis = async () => {
  try {
    const userId=accountData.value.userId
    const res = await axios.get(`/api/ai/listPersonalAis/${userId}`, {
      headers: {
        'Authorization': 'Bearer ' + localStorage.getItem('token')
      }
    })
    aiList.value = res.data.map(item => ({
      ...item,
      id: item.id,
      ownerId: item.ownerId,
      desc: item.description || item.desc || '暂无描述',
      emoji: item.avatar || (item.name ? [...item.name][0] : '🤖'),
      conversations: item.dialogCount||0,
      gradient: item.gradient || 'linear-gradient(135deg, #2c3e50 0%, #4a90e2 100%)',
      created: (item.createdAt || '').split('T')[0],
      statusText: 'Active',
      statusClass: 'active',
      active: true
    }))
  } catch (error) {
    console.error('获取列表失败', error)
  }
}

//跳转创建AI页
const createAI = () => {
  router.push('/create-ai')
}

//跳转和AI对话
const chatWithAI = (ai) => {
  router.push({
    path: '/chat',
    query: {
      type: 'ai',
      aiId: ai.id
    }
  })
}

//点击管理按钮，打开弹窗
const manageAI = (ai) => {
  //把当前AI信息填入editingAI，弹出编辑弹窗
  editingAI.value = {
    id: ai.id,
    ownerId: ai.ownerId,
    name: ai.name,
    desc: ai.desc,
    prompt: ai.prompt || '',
    tags: [...(ai.tags || [])],
    emoji: ai.emoji,
    gradient: ai.gradient
  }
  showManageModal.value = true
}

const closeManageModal = () => {
  showManageModal.value = false
  newTag.value = ''
}

const confirmDelete = () => {
  showDeleteConfirm.value = true
}

//保存编辑
const saveAI = async () => {
  if (isDefaultAI.value) {
    window.$toast.info('系统预设AI不支持修改')
    return
  }//系统AI不能改

  //调接口更新，成功后同步更新本地aiList，不用重新拉接口
  if (!editingAI.value.name.trim()) {
    window.$toast.info('请输入AI名称')
    return
  }
  if (!editingAI.value.desc.trim()) {
    window.$toast.info('请输入AI描述')
    return
  }

  try {
    const token = localStorage.getItem('token')
    const res = await fetch('/api/ai/updateAi', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + token
      },
      body: JSON.stringify({
        id: editingAI.value.id,
        name: editingAI.value.name,
        description: editingAI.value.desc,
        prompt: editingAI.value.prompt,
        tags: editingAI.value.tags
      })
    })

    const result = await res.json()

    if (result.code === 200) {
      const aiIndex = aiList.value.findIndex(ai => ai.id === editingAI.value.id)
      if (aiIndex !== -1) {
        aiList.value[aiIndex] = {
          ...aiList.value[aiIndex],
          name: editingAI.value.name,
          desc: editingAI.value.desc,
          prompt: editingAI.value.prompt,
          tags: editingAI.value.tags
        }
      }

      window.$toast.info('保存成功！')
      closeManageModal()
    } else {
      window.$toast.info(result.message || '保存失败')
    }
  } catch (e) {
    console.error('保存AI失败', e)
    window.$toast.info('保存失败，请稍后重试')
  }
}

//删除AI
const deleteAI = async () => {
  if (isDefaultAI.value) {
    window.$toast.info('系统预设AI不支持删除')
    return
  }//系统AI不能删

  //删除成功后从本地aiList过滤掉，同时aiCount-1
  try {
    const token = localStorage.getItem('token')
    const res = await fetch(`/api/ai/delete/${editingAI.value.id}`, {
      method: 'POST',
      headers: {
        'Authorization': 'Bearer ' + token
      }
    })

    if (!res.ok) {
      throw new Error(`服务器响应异常: ${res.status}`);
    }

    const result = await res.json()

    if (result.code === 200) {
      aiList.value = aiList.value.filter(ai => ai.id !== editingAI.value.id)
      accountData.value.aiCount = Math.max(0, accountData.value.aiCount - 1)
      window.$toast.info('删除成功！')
      showDeleteConfirm.value = false
      closeManageModal()
    } else {
      window.$toast.info(result.message || '删除失败')
    }
  } catch (e) {
    console.error('删除AI失败', e)
    window.$toast.info('删除失败，请稍后重试')
  }
}

//GSAP入场动画
function runAnimation() {
  gsap.to('.metric-card', {
    opacity: 1,
    y: 0,
    duration: 0.35,
    stagger: 0.06,
    ease: "power3.out"
  })

  gsap.to('.ai-instance', {
    opacity: 1,
    y: 0,
    duration: 0.35,
    stagger: 0.07,
    ease: "power3.out",
    delay: 0.15
  })
}

//先拉用户信息->再拉AI列表->DOM渲染完毕->跑入场动画
onMounted(async () => {
  await loadUserInfo()
  await fetchMyAis()

  await nextTick()   // 等待DOM渲染

  runAnimation()
})
</script>

<style>
@import url('https://fonts.googleapis.com/css2?family=Cormorant+Garamond:ital,wght@0,400;0,600;1,400;1,600&family=Inter:wght@300;400;500;600;700&display=swap');

:root {
  --symph-black: #2c3e50;
  --symph-white: #fdfdfc;
  --symph-gray: #7f8c8d;
  --symph-light-gray: #ecf0f1;
  --symph-blue: #4a90e2;
  --symph-gold: #d4af37;
  --symph-brown: #613125;
  --symph-border: rgba(0, 0, 0, 0.06);
  --symph-shadow: rgba(0, 0, 0, 0.04);
}
</style>

<style scoped>
* { margin: 0; padding: 0; box-sizing: border-box; }

.symphony-ai-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #fdfdfc 0%, #f8fafb 100%);
  font-family: 'Inter', -apple-system, sans-serif;
  color: var(--symph-black);
}

/* 导航栏 */
.ai-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 60px;
  border-bottom: 1px solid var(--symph-border);
  background: var(--symph-white);
}

.nav-return {
  display: flex;
  align-items: center;
  gap: 10px;
  background: transparent;
  border: 1px solid var(--symph-border);
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 10px;
  letter-spacing: 1.5px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.3s;
  font-family: inherit;
}

.nav-return:hover {
  border-color: var(--symph-blue);
  background: rgba(74, 144, 226, 0.05);
  color: var(--symph-blue);
}

.nav-logo {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-icon {
  font-family: 'Cormorant Garamond', serif;
  font-size: 24px;
  color: var(--symph-gold);
  font-style: italic;
}

.logo-text {
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 3px;
  color: var(--symph-black);
}

.nav-spacer {
  width: 140px;
}

/* 主容器 */
.ai-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 40px 60px;
}

/* 统计面板 */
.metrics-panel {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 20px;
  margin-bottom: 50px;
}

.metric-card {
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid var(--symph-border);
  border-radius: 12px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 20px;
  transition: all 0.3s;
}

@keyframes metricFadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.metric-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  border-color: var(--symph-blue);
}

.metric-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
}

.metric-content {
  flex: 1;
}

.metric-value {
  font-size: 28px;
  font-weight: 300;
  color: var(--symph-black);
  margin-bottom: 4px;
}

.metric-label {
  font-size: 10px;
  letter-spacing: 1.5px;
  font-weight: 700;
  color: var(--symph-gray);
}

/* AI列表区域 */
.ai-section {
  margin-top: 40px;
}

.section-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 32px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--symph-border);
}

.header-title-group {
  flex: 1;
}

.section-title {
  font-size: 28px;
  font-weight: 300;
  letter-spacing: 2px;
  color: var(--symph-black);
  margin-bottom: 6px;
}

.section-subtitle {
  font-size: 12px;
  letter-spacing: 1px;
  font-weight: 500;
  color: var(--symph-gray);
}

.create-ai-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 28px;
  background: linear-gradient(135deg, var(--symph-black) 0%, var(--symph-blue) 100%);
  color: var(--symph-white);
  border: none;
  border-radius: 8px;
  font-size: 11px;
  letter-spacing: 1.5px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.3s;
  font-family: inherit;
}

.create-ai-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(74, 144, 226, 0.3);
}

/* AI网格 */
.ai-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 24px;
}

.ai-instance {
  position: relative;
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid var(--symph-border);
  border-radius: 12px;
  padding: 28px;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
}

@keyframes instanceFadeIn {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.instance-border {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 3px;
  background: linear-gradient(90deg, var(--symph-gold) 0%, var(--symph-blue) 50%, var(--symph-brown) 100%);
  transform: scaleX(0);
  transform-origin: left;
  transition: transform 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.ai-instance:hover {
  transform: translateY(-6px);
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.12);
  border-color: var(--symph-blue);
}

.ai-instance:hover .instance-border {
  transform: scaleX(1);
}

.instance-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 20px;
}

.instance-avatar {
  width: 64px;
  height: 64px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: white;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}

.instance-status {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 9px;
  letter-spacing: 1px;
  font-weight: 700;
}

.instance-status.active {
  background: rgba(74, 144, 226, 0.1);
  color: var(--symph-blue);
  border: 1px solid rgba(74, 144, 226, 0.2);
}

.status-indicator {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--symph-blue);
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.6; transform: scale(0.9); }
}

.instance-info {
  margin-bottom: 20px;
}

.instance-name {
  font-size: 20px;
  font-weight: 600;
  letter-spacing: 0.5px;
  color: var(--symph-black);
  margin-bottom: 8px;
}

.instance-desc {
  font-size: 13px;
  line-height: 1.6;
  color: var(--symph-gray);
}

.instance-metrics {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: rgba(0, 0, 0, 0.02);
  border-radius: 8px;
  margin-bottom: 16px;
}

.instance-metric {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.metric-num {
  font-size: 18px;
  font-weight: 600;
  color: var(--symph-black);
}

.metric-lbl {
  font-size: 9px;
  letter-spacing: 1px;
  font-weight: 700;
  color: var(--symph-gray);
}

.metric-divider {
  width: 1px;
  height: 30px;
  background: var(--symph-border);
}

.instance-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 20px;
}

.instance-tag {
  padding: 6px 12px;
  background: rgba(212, 175, 55, 0.1);
  border: 1px solid rgba(212, 175, 55, 0.2);
  border-radius: 6px;
  font-size: 10px;
  letter-spacing: 0.5px;
  font-weight: 600;
  color: var(--symph-gold);
}

.instance-actions {
  display: flex;
  gap: 12px;
}

.action-primary,
.action-secondary {
  flex: 1;
  padding: 12px;
  border-radius: 8px;
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

.action-primary {
  background: var(--symph-black);
  color: var(--symph-white);
  border: 1px solid var(--symph-black);
}

.action-primary:hover {
  background: var(--symph-blue);
  border-color: var(--symph-blue);
  transform: translateY(-2px);
}

.action-secondary {
  background: transparent;
  color: var(--symph-black);
  border: 1px solid var(--symph-border);
}

.action-secondary:hover {
  border-color: var(--symph-black);
  background: rgba(0, 0, 0, 0.02);
}

/* 弹窗 */
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.modal-ai {
  background: var(--symph-white);
  border: 1px solid var(--symph-border);
  border-radius: 16px;
  width: 100%;
  max-width: 700px;
  max-height: 85vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 40px 100px rgba(0, 0, 0, 0.15);
}

.modal-ai-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28px 32px;
  border-bottom: 1px solid var(--symph-border);
}

.modal-title-group {
  display: flex;
  align-items: center;
  gap: 12px;
}

.modal-title-group i {
  font-size: 20px;
  color: var(--symph-blue);
}

.modal-title {
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 2px;
  color: var(--symph-black);
}

.modal-close {
  width: 36px;
  height: 36px;
  border: 1px solid var(--symph-border);
  background: transparent;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: var(--symph-gray);
}

.modal-close:hover {
  background: var(--symph-black);
  color: var(--symph-white);
  border-color: var(--symph-black);
}

.modal-ai-body {
  flex: 1;
  overflow-y: auto;
  padding: 32px;
}

.system-notice {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  background: rgba(212, 175, 55, 0.1);
  border-left: 3px solid var(--symph-gold);
  border-radius: 0 8px 8px 0;
  margin-bottom: 28px;
  font-size: 13px;
  font-weight: 500;
  color: var(--symph-brown);
}

.system-notice i {
  font-size: 18px;
}

.config-field {
  margin-bottom: 24px;
}

.field-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11px;
  letter-spacing: 1.5px;
  font-weight: 700;
  color: var(--symph-black);
  margin-bottom: 10px;
}

.field-label i {
  color: var(--symph-blue);
}

.field-input,
.field-textarea {
  width: 100%;
  padding: 14px 16px;
  border: 1px solid var(--symph-border);
  border-radius: 8px;
  font-size: 14px;
  font-family: inherit;
  color: var(--symph-black);
  background: var(--symph-white);
  transition: all 0.3s;
}

.field-input:focus,
.field-textarea:focus {
  outline: none;
  border-color: var(--symph-blue);
  box-shadow: 0 0 0 3px rgba(74, 144, 226, 0.1);
}

.field-input:disabled,
.field-textarea:disabled {
  background: rgba(0, 0, 0, 0.02);
  color: var(--symph-gray);
  cursor: not-allowed;
}

.field-textarea {
  resize: vertical;
  line-height: 1.6;
}

.tags-config {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.tags-display {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.config-tag {
  padding: 8px 16px;
  background: linear-gradient(135deg, var(--symph-blue) 0%, var(--symph-gold) 100%);
  color: var(--symph-white);
  border-radius: 6px;
  font-size: 11px;
  letter-spacing: 0.5px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}

.tag-remove {
  cursor: pointer;
  opacity: 0.8;
  transition: opacity 0.2s;
}

.tag-remove:hover {
  opacity: 1;
}

.modal-ai-footer {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 24px 32px;
  border-top: 1px solid var(--symph-border);
}

.footer-spacer {
  flex: 1;
}

.modal-action {
  padding: 12px 24px;
  border-radius: 8px;
  font-size: 11px;
  letter-spacing: 1.5px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.3s;
  border: none;
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: inherit;
}

.modal-action.primary {
  background: var(--symph-black);
  color: var(--symph-white);
}

.modal-action.primary:hover {
  background: var(--symph-blue);
  transform: translateY(-1px);
}

.modal-action.secondary {
  background: rgba(0, 0, 0, 0.05);
  color: var(--symph-black);
}

.modal-action.secondary:hover {
  background: rgba(0, 0, 0, 0.1);
}

.modal-action.danger {
  background: var(--symph-brown);
  color: var(--symph-white);
}

.modal-action.danger:hover {
  background: #4a1f19;
  transform: translateY(-1px);
}

/* 确认对话框 */
.confirm-dialog {
  background: var(--symph-white);
  border: 1px solid var(--symph-border);
  border-radius: 16px;
  padding: 40px;
  width: 100%;
  max-width: 480px;
  text-align: center;
}

.confirm-icon {
  width: 72px;
  height: 72px;
  margin: 0 auto 24px;
  background: var(--symph-brown);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  color: var(--symph-white);
}

.confirm-title {
  font-size: 20px;
  font-weight: 600;
  letter-spacing: 1.5px;
  color: var(--symph-black);
  margin-bottom: 16px;
}

.confirm-text {
  font-size: 14px;
  line-height: 1.6;
  color: var(--symph-gray);
  margin-bottom: 12px;
}

.confirm-warning {
  font-size: 13px;
  line-height: 1.6;
  color: var(--symph-brown);
  font-weight: 500;
  margin-bottom: 28px;
}

.confirm-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.system-ai-hint {
  width: 100%;
  text-align: center;
  font-size: 11px;
  color: var(--symph-gray);
  letter-spacing: 1px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.metric-card,
.ai-instance {
  opacity: 0;
}

.modal-fade-enter-from .modal-ai,
.modal-fade-leave-to .modal-ai,
.modal-fade-enter-from .confirm-dialog,
.modal-fade-leave-to .confirm-dialog {
  transform: scale(0.95) translateY(20px);
}

/* 响应式 */
@media (max-width: 1200px) {
  .ai-container {
    padding: 30px 40px;
  }

  .ai-grid {
    grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  }
}

@media (max-width: 768px) {
  .ai-nav {
    padding: 16px 20px;
  }

  .nav-logo {
    flex-direction: column;
    gap: 4px;
  }

  .logo-text {
    font-size: 12px;
  }

  .ai-container {
    padding: 20px;
  }

  .section-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .create-ai-btn {
    width: 100%;
  }

  .ai-grid {
    grid-template-columns: 1fr;
  }

  .modal-ai {
    max-width: 100%;
    max-height: 90vh;
  }

  .modal-ai-footer {
    flex-direction: column;
  }

  .footer-spacer {
    display: none;
  }

  .modal-action {
    width: 100%;
  }
}
</style>