<template>
  <div class="ds-viewport">
    <nav class="ds-nav">
      <div class="ds-logo" @click="$router.push('/')">SYMPHONY</div>
      <div class="ds-menu">
        <span class="ds-menu-item" @click="$router.push('/mistakes')">ARCHIVE / 错题</span>
        <span class="ds-menu-item" @click="goToChat">COMMUNITY / 聊天</span>
        <span class="ds-menu-item" @click="$router.push('/canvas')">STUDIO / 白板</span>
      </div>
      <div class="ds-auth">
        <div class="bell-wrapper" @click.stop="toggleNotifications">
          <i class="fas fa-bell ds-bell-icon"></i>
          <span v-if="friendRequests.length > 0" class="bell-badge">{{ friendRequests.length }}</span>
        </div>

        <div v-if="showNotifications" class="notification-dropdown">
          <div class="notif-header">
            <span class="notif-title">申请通知</span>
          </div>
          <div v-if="friendRequests.length > 0">
            <div v-for="req in friendRequests" :key="req.id" class="notif-item">
              <div class="notif-avatar" :style="{ background: req.color }">
                {{ req.name[0] }}
              </div>
              <div class="notif-info">
                <div class="notif-name">{{ req.name }}</div>
                <div class="notif-desc">{{ req.message }}</div>
              </div>
              <div class="notif-actions">
                <button class="notif-btn accept" @click="acceptRequest(req.id)">同意</button>
                <button class="notif-btn reject" @click="rejectRequest(req.id)">拒绝</button>
              </div>
            </div>
          </div>
          <div v-else class="notif-empty">
            <i class="fas fa-bell-slash"></i>
            <div>没有新的好友申请</div>
          </div>
        </div>

        <div @click="$router.push('/profile')" class="ds-user-click-area">
          <span class="ds-user-name">{{ userData.username || 'ACCOUNT' }}</span>
          <div class="ds-user-circle">{{ userInitial }}</div>
        </div>
      </div>
    </nav>

    <main class="ds-content-layer">
      <section class="ds-hero-minimal">
        <div class="hero-text-wrap">
          <h1 class="ds-main-title">
            <span class="line">LEARN IN </span>
            <span class="line-italic">HARMONY</span>
          </h1>
          <p class="ds-hero-desc">
            探索 AI 驱动的协作学习环境。<br>
            已有{{ userData.onlineFriends || 0 }}名好友。
          </p>
          <button class="ds-btn-hero" @click="goToChat">START A SESSION</button>
        </div>
      </section>

      <section class="ds-cards-deck">
        <div
            v-for="(f, i) in featureConfig"
            :key="i"
            class="ds-poker-wrapper"
            @mousemove="handleCardMove($event, i)"
            @mouseleave="resetCard(i)"
            @click="$router.push(f.path)"
            :style="{ '--card-theme': f.theme, '--card-text': f.textColor }"
        >
          <div class="ds-poker-card" :ref="el => cardRefs[i] = el">
            <div class="card-flashlight"></div>
            <div class="card-inner">
              <span class="card-index">0{{ i + 1 }}</span>
              <h3 class="card-label">{{ f.name }}</h3>
              <p class="card-caption">{{ f.desc }}</p>
              <div class="card-footer">EXPLORE MODULE →</div>
            </div>
          </div>
        </div>
      </section>
    </main>

    <div v-if="showReviewModal" class="modal-overlay" @click.self="showReviewModal = false">
      <div class="review-modal">
        <div class="review-header">
          <h2>待复习内容</h2>
        </div>
        <div class="review-list">
          <div v-for="item in reviewItems" :key="item.id" class="review-item">
            <div class="review-tag">{{ item.tag }}</div>
            <div class="review-content">{{ item.content }}</div>
          </div>
        </div>
        <div class="review-footer">
          <button class="review-go-btn" @click="goToReview">开始复习</button>
          <button class="review-close-btn" @click="showReviewModal = false">稍后再说</button>
        </div>
      </div>
    </div>

    <div v-if="showReportModal" class="modal-overlay" @click.self="showReportModal = false">
      <div class="report-modal">
        <div class="report-eyebrow">DAILY REPORT · 昨日总结</div>
        <h2 class="report-title">学习日报</h2>
        <div class="report-divider"></div>
        <div class="report-list">
          <div v-for="item in reportSummaries" :key="item.targetId" class="report-item">
            <div class="report-item-header">
              <span class="report-dot"></span>
              <span class="report-name">{{ item.targetName }}</span>
            </div>
            <p class="report-summary">{{ item.summary }}</p>
          </div>
        </div>
        <button class="report-close-btn" @click="closeReport">— 知道了 —</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()
const userData = ref({ username: '', userId: '', onlineFriends: 0 })
const userInitial = computed(() => (userData.value?.username || '?')[0].toUpperCase())
const showNotifications = ref(false)
const friendRequests = ref([])
const showReviewModal = ref(false)
const reviewItems = ref([])
const cardRefs = ref([])
const showReportModal = ref(false)
const reportSummaries = ref([])
const reportIds = ref([])

//检查昨日学习报告
const checkDailyReport = async () => {
  try {
    const token = localStorage.getItem('token')
    const res = await axios.get('/api/report/today', {
      headers: { 'Authorization': 'Bearer ' + token }
    })
    if (res.data.code === 200 && res.data.data && res.data.data.length > 0) {
      reportIds.value = res.data.data.map(r => r.id)   // 存所有id
      reportSummaries.value = res.data.data             // 直接用，结构已是 {targetId, targetName, summary}
      setTimeout(() => { showReportModal.value = true }, 1000)
    }
  } catch (e) {}
}

//关闭报告，批量标记已读
const closeReport = async () => {
  showReportModal.value = false
  if (reportIds.value.length === 0) return
  try {
    const token = localStorage.getItem('token')
    await Promise.all(reportIds.value.map(id =>
        axios.put(`/api/report/${id}/read`, {}, {
          headers: { 'Authorization': 'Bearer ' + token }
        })
    ))
  } catch (e) {}
}

//功能卡片配置
const featureConfig = [
  { name: '智慧聊天', desc: 'AI 实时插话讨论与知识沉淀', path: '/chat', theme: '#004cd5', textColor: '#ffffff' },
  { name: '智能错题', desc: '遗忘曲线驱动的协作错题本', path: '/mistakes', theme: '#ddb638', textColor: '#ffffff' },
  { name: '知识园区', desc: '群体智慧汇聚的知识园区', path: '/garden', theme: '#304f25', textColor: '#ffffff' },
  { name: '绘画白板', desc: '无限协作白板与思维脑图', path: '/canvas', theme: '#656565', textColor: '#ffffff' },
  { name: '个人AI', desc: '基于 RAG 的个性化知识库', path: '/ai', theme: '#613125', textColor: '#ffffff' }
]

//3D卡片效果
const handleCardMove = (e, index) => {
  const card = cardRefs.value[index]
  if (!card) return
  const rect = card.getBoundingClientRect()
  const x = e.clientX - rect.left
  const y = e.clientY - rect.top
  const rotateX = (y - rect.height / 2) / -10
  const rotateY = (x - rect.width / 2) / 10
  card.style.transform = `perspective(1000px) rotateX(${rotateX}deg) rotateY(${rotateY}deg) scale3d(1.05, 1.05, 1.05)`
  card.style.setProperty('--mouse-x', `${x}px`);
  card.style.setProperty('--mouse-y', `${y}px`);
  card.classList.add('is-active')
}

//鼠标离开时恢复原位
const resetCard = (index) => {
  const card = cardRefs.value[index]
  if (card) {
    card.style.transform = `perspective(1000px) rotateX(0deg) rotateY(0deg) scale3d(1, 1, 1)`
    card.classList.remove('is-active')
  }
}

//点开通知面板时自动拉取申请列表
const toggleNotifications = async () => {
  showNotifications.value = !showNotifications.value
  if (showNotifications.value) await loadFriendRequests()
}
const loadFriendRequests = async () => {
  try {
    const token = localStorage.getItem('token')
    const res = await axios.get('/api/friends/requests/pending', { headers: { 'Authorization': 'Bearer ' + token } })
    if (res.data.code === 200) {
      const colors = ['#667eea', '#f093fb', '#4facfe', '#43e97b', '#fa709a']
      friendRequests.value = res.data.data.map((req, i) => ({
        id: req.id, name: req.fromUsername, message: '想要添加你为好友', color: colors[i % colors.length]
      }))
    }
  } catch (e) { console.error(e) }
}

//同意申请
const acceptRequest = async (id) => {
  const token = localStorage.getItem('token')
  await axios.post('/api/friends/request/handle',
      { requestId: id, accept: true },
      { headers: { 'Authorization': 'Bearer ' + token } })
  loadFriendRequests()
}
//拒绝申请
const rejectRequest = async (id) => {
  const token = localStorage.getItem('token')
  await axios.post('/api/friends/request/handle',
      { requestId: id, accept: false },
      { headers: { 'Authorization': 'Bearer ' + token } })
  loadFriendRequests()
}

//加载用户信息的方法
const loadUserInfo = async () => {
  try {
    const token = localStorage.getItem('token')
    if (!token) { router.push('/login'); return }   //没有token直接跳登录页
    const res = await axios.get('/api/auth/profile', { headers: { 'Authorization': 'Bearer ' + token } })
    if (res.data.code === 200) {
      userData.value = { username: res.data.data.username, userId: res.data.data.userId, onlineFriends: res.data.data.friendCount || 0 }
    }
  } catch (e) { router.push('/login') }
}

//检查今日待复习错题
const checkTodayReview = async () => {
  try {
    const token = localStorage.getItem('token')
    const res = await fetch('/api/mistakes/review/today', { headers: { 'Authorization': 'Bearer ' + token } })
    const reviewData = await res.json()
    if (reviewData.code === 200 && reviewData.data.length > 0) {
      reviewItems.value = reviewData.data.map(item => ({
        id: item.id, tag: item.subject || '知识点', content: item.question, knowledge: item.knowledgePoint
      }))
      setTimeout(() => { if (reviewItems.value.length > 0) showReviewModal.value = true }, 2000)
    }
  } catch (e) {}
}
const goToChat = () => { if (userData.value.userId) router.push(`/chat/${userData.value.userId}/public`) }
const goToReview = () => { router.push('/mistakes') }

//验证登录->拉用户信息->检查今日复习->检查每日报告
//点击通知面板外部时自动关闭
onMounted(() => {
  loadUserInfo(); checkTodayReview(); checkDailyReport()
  document.addEventListener('click', (e) => { if (showNotifications.value && !e.target.closest('.ds-auth')) showNotifications.value = false })
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Cormorant+Garamond:ital,wght@1,400;1,600&family=Inter:wght@300;400;600&display=swap');

:root {
  --ds-bg: #fdfdfc;
  --ds-text: #1a1a1a;
  --ds-text-muted: #737373;
  --ds-border: rgba(0, 0, 0, 0.1);
  --ds-blue-bright: #004cd5;
}

.ds-viewport { width: 100%; height: 100vh; overflow: hidden; background-color: #fdfdfc; display: flex; flex-direction: column; }


.ds-nav { display: flex; justify-content: space-between; align-items: center; padding: 20px 60px; background: #fff; border-bottom: 1px solid var(--ds-border); z-index: 100; }
.ds-logo { font-family: 'Cormorant Garamond', serif; font-size: 24px; letter-spacing: 4px; cursor: pointer; font-weight: 600; }
.ds-menu { display: flex; gap: 30px; }
.ds-menu-item { font-size: 11px; letter-spacing: 1px; color: var(--ds-text-muted); cursor: pointer; transition: 0.3s; }
.ds-auth { position: relative; display: flex; align-items: center; gap: 20px; }
.ds-user-click-area { display: flex; align-items: center; gap: 12px; cursor: pointer; }
.ds-user-name { font-size: 11px; letter-spacing: 2px; }
.ds-user-circle { width: 32px; height: 32px; border: 1px solid var(--ds-border); border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 12px; }
.bell-wrapper { position: relative; cursor: pointer; }
.ds-bell-icon { font-size: 16px; color: var(--ds-text-muted); transition: color 0.3s; }
.bell-badge { position: absolute; top: -6px; right: -6px; min-width: 16px; height: 16px; background: var(--ds-blue-bright); color: white; border-radius: 10px; font-size: 10px; font-weight: 700; display: flex; align-items: center; justify-content: center; padding: 0 3px; box-shadow: 0 0 12px rgba(0, 76, 213, 0.4); }


.notification-dropdown { position: absolute; top: 50px; right: 0; width: 340px; background: #ffffff; border-radius: 2px; border: 1px solid rgba(0, 0, 0, 0.1); box-shadow: 0 20px 60px rgba(0,0,0,0.1); z-index: 200; overflow: hidden; animation: dropDown 0.3s cubic-bezier(0.7, 0, 0.3, 1); }
@keyframes dropDown { from { opacity: 0; transform: translateY(-10px); } to { opacity: 1; transform: translateY(0); } }
.notif-header { padding: 20px 24px; border-bottom: 1px solid rgba(0, 0, 0, 0.1); }
.notif-title { font-size: 11px; font-weight: 600; letter-spacing: 2px; color: #1a1a1a; }
.notif-item { display: flex; align-items: center; gap: 14px; padding: 18px 24px; border-bottom: 1px solid rgba(0, 0, 0, 0.1); transition: background 0.2s; }
.notif-item:hover { background: rgba(0, 0, 0, 0.03); }
.notif-avatar { width: 40px; height: 40px; border-radius: 50%; color: white; display: flex; align-items: center; justify-content: center; font-size: 14px; font-weight: 600; flex-shrink: 0; }
.notif-info { flex: 1; }
.notif-name { font-size: 13px; font-weight: 600; color: #1a1a1a; margin-bottom: 4px; }
.notif-desc { font-size: 11px; color: #737373; letter-spacing: 0.5px; }
.notif-actions { display: flex; gap: 8px; flex-direction: column; }
.notif-btn { padding: 6px 16px; border: none; border-radius: 2px; font-size: 10px; font-weight: 600; letter-spacing: 1px; cursor: pointer; transition: all 0.3s; text-transform: uppercase; }
.notif-btn.accept { background: #1a1a1a; color: #ffffff; }
.notif-btn.accept:hover { background: var(--ds-blue-bright); transform: translateY(-1px); box-shadow: 0 4px 12px rgba(0, 76, 213, 0.3); }
.notif-btn.reject { background: transparent; color: #737373; border: 1px solid rgba(0, 0, 0, 0.1); }
.notif-btn.reject:hover { border-color: #1a1a1a; color: #1a1a1a; }
.notif-empty { padding: 40px; text-align: center; color: #737373; font-size: 12px; display: flex; flex-direction: column; align-items: center; gap: 12px; }


.ds-content-layer {
  flex: 1; display: flex; flex-direction: column;
  justify-content: space-evenly; align-items: center; /* 关键：水平居中 */
  padding: 0 60px;
}


.ds-hero-minimal { text-align: center; width: 100%; }
.hero-text-wrap {
  display: flex; flex-direction: column; align-items: center; /* 关键：内部元素居中 */
}

.ds-main-title {
  font-size: clamp(40px, 10vh, 120px); line-height: 0.85; margin: 0; font-weight: 300;
  display: flex; flex-direction: column; align-items: center;
}
.ds-main-title .line-italic {
  font-family: 'Cormorant Garamond', serif; font-style: italic; color: #0039a6;
  padding-left: 0;
  margin-top: 10px;
}

.ds-hero-desc { font-size: 14px; color: #666; margin: 25px 0; line-height: 1.6; }
.ds-btn-hero { background: #1a1a1a; color: #fff; border: none; padding: 14px 40px; font-size: 11px; letter-spacing: 2px; cursor: pointer; transition: 0.3s; }
.ds-btn-hero:hover { background: #0039a6; transform: scale(1.05); }


.ds-cards-deck { display: flex; justify-content: center; gap: 20px; perspective: 2000px; width: 100%; }
.ds-poker-wrapper { flex: 1; max-width: 220px; height: 320px; cursor: pointer; }
.ds-poker-card { position: relative; width: 100%; height: 100%; background: #fff; border: 1px solid rgba(0,0,0,0.06); border-radius: 8px; padding: 25px; transition: transform 0.1s ease-out, background 0.4s, color 0.4s, box-shadow 0.4s; box-shadow: 0 10px 30px rgba(0,0,0,0.04); display: flex; flex-direction: column; overflow: hidden; transform-style: preserve-3d; }
.ds-poker-card.is-active { background: var(--card-theme); color: var(--card-text); box-shadow: 0 30px 60px rgba(0,0,0,0.2); }
.card-flashlight { position: absolute; top: 0; left: 0; width: 100%; height: 100%; pointer-events: none; z-index: 1; opacity: 0; background: radial-gradient(circle 150px at var(--mouse-x) var(--mouse-y), rgba(255, 255, 255, 0.3) 0%, transparent 80%); }
.ds-poker-card.is-active .card-flashlight { opacity: 1; }
.card-inner { z-index: 2; height: 100%; display: flex; flex-direction: column; }
.card-index { font-family: 'Cormorant Garamond', serif; font-style: italic; font-size: 26px; color: #0039a6; transition: 0.4s; }
.ds-poker-card.is-active .card-index { color: inherit; opacity: 0.8; }
.card-label { font-size: 19px; font-weight: 600; margin: 15px 0 10px; }
.card-caption { font-size: 11px; color: #888; line-height: 1.6; flex: 1; }
.card-footer { font-size: 10px; font-weight: 700; padding-top: 15px; border-top: 1px solid rgba(0,0,0,0.05); }


.modal-overlay { position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(255, 255, 255, 0.9); z-index: 1000; display: flex; align-items: center; justify-content: center; backdrop-filter: blur(8px); }
.review-modal { background: #fff; border: 1px solid #eee; padding: 40px; width: 500px; box-shadow: 0 20px 60px rgba(0,0,0,0.1); }
.review-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 25px; }
.review-item { background: #f9f9f9; padding: 18px; border: 1px solid #eee; margin-bottom: 12px; }
.review-tag { font-size: 10px; background: #1a1a1a; color: #fff; display: inline-block; padding: 2px 8px; margin-bottom: 8px; }
.review-knowledge { font-size: 11px; color: #0039a6; margin-top: 10px; font-style: italic; }
.review-footer { display: flex; gap: 10px; margin-top: 25px; }
.review-go-btn { flex: 2; background: #1a1a1a; color: #fff; padding: 14px; border: none; cursor: pointer; font-size: 12px; }
.review-close-btn { flex: 1; background: transparent; border: 1px solid #ddd; padding: 14px; cursor: pointer; font-size: 12px; }

.report-modal {
  background: #fff;
  width: 480px;
  padding: 52px 56px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}
.report-eyebrow {
  font-size: 10px;
  letter-spacing: 3px;
  color: #aaa;
  margin-bottom: 16px;
}
.report-title {
  font-family: 'Cormorant Garamond', serif;
  font-style: italic;
  font-size: 38px;
  font-weight: 400;
  color: #0039a6;
  margin: 0 0 20px;
}
.report-divider {
  width: 40px;
  height: 1px;
  background: #ddd;
  margin-bottom: 32px;
}
.report-list {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 24px;
  margin-bottom: 40px;
}
.report-item {
  text-align: left;
}
.report-item-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.report-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #0039a6;
  flex-shrink: 0;
}
.report-name {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 2px;
  color: #1a1a1a;
}
.report-summary {
  font-size: 13px;
  color: #555;
  line-height: 1.8;
  margin: 0;
  padding-left: 13px;
  border-left: 1px solid #eee;
}
.report-close-btn {
  background: none;
  border: none;
  font-size: 11px;
  letter-spacing: 3px;
  color: #aaa;
  cursor: pointer;
  transition: color 0.3s;
}
.report-close-btn:hover {
  color: #1a1a1a;
}
.report-list {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 24px;
  margin-bottom: 40px;
  max-height: 360px;
  overflow-y: auto;
  padding-right: 8px;
}

.report-list::-webkit-scrollbar {
  width: 3px;
}
.report-list::-webkit-scrollbar-track {
  background: transparent;
}
.report-list::-webkit-scrollbar-thumb {
  background: #ddd;
  border-radius: 2px;
}
</style>