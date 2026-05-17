<template>
  <div class="chat-page">
    <div class="chat-container">

      <!-- 侧边栏  -->
      <div class="sidebar">
        <div class="sidebar-header">
          <div class="logo-text">🎵 SYMPHONY</div>
          <p class="online-count">{{ onlineUsers.size }} 人在线</p>
        </div>

        <!-- 三种聊天类型切换 tab -->
        <div class="chat-types">
          <div class="type-tab" @click="goToPrivate">
            <i class="fas fa-user"></i><span>学伴</span>
          </div>
          <div class="type-tab active">
            <i class="fas fa-users"></i><span>公屏</span>
          </div>
        </div>

        <div class="channel-list">
          <!-- 公屏房间 -->
          <div class="section-title">公屏频道</div>
          <div class="channel-item active">
            <div class="channel-avatar" style="background: linear-gradient(135deg, #667EEA, #764BA2);">
              <i class="fas fa-hashtag"></i>
            </div>
            <div class="channel-info">
              <div class="channel-name">公共频道</div>
              <div class="channel-preview">开放式聊天室</div>
            </div>
          </div>

          <!-- 在线用户列表 -->
          <div class="section-title" style="margin-top: 10px;">在线用户</div>
          <div
              v-for="(user, index) in Array.from(onlineUsers)"
              :key="user"
              class="user-item"
              @click="openUserProfile(user)"
              title="点击查看资料 / 加好友"
          >
            <div class="user-avatar" :style="{ background: getColor(index) }">
              {{ user[0].toUpperCase() }}
            </div>
            <span class="user-name">{{ user }}</span>
            <i class="fas fa-chevron-right" style="font-size: 10px; color: var(--text-light); margin-left: auto;"></i>
          </div>
        </div>
      </div>

      <!--  聊天主区   -->
      <div class="chat-main">

        <div class="chat-header">
          <div class="header-left">
            <h4 class="header-title">
              <i class="fas fa-hashtag" style="color: var(--secondary);"></i>
              公共频道
            </h4>
            <span class="header-subtitle">{{ onlineUsers.size }} 人在线 · 输入 / 可查看指令</span>
          </div>
          <div class="header-actions">
            <button class="header-btn" @click="$router.push('/home')">
              <i class="fas fa-arrow-left"></i> 返回
            </button>
          </div>
        </div>

        <div ref="messagesContainer" class="messages-container">
          <div v-for="msg in messages" :key="msg.id" :class="['message', getClass(msg)]">
            <template v-if="msg.type === 'SYSTEM'">
              <div class="msg-bubble system-bubble">{{ msg.content }}</div>
            </template>
            <template v-else>
              <div class="msg-avatar" :style="{ background: msg.isOwn ? 'var(--secondary)' : 'var(--primary)' }">
                {{ msg.from[0]?.toUpperCase() }}
              </div>
              <div class="msg-body">
                <div class="msg-info">{{ msg.from }}</div>
                <div class="msg-bubble">{{ msg.content }}</div>
                <div class="msg-time">{{ formatTime(msg.createdAt) }}</div>
              </div>
            </template>
          </div>

          <div v-if="messages.length === 0" class="empty-state">
            <div class="empty-icon">💬</div>
            <div class="empty-text">暂无消息，快来发第一条吧！</div>
          </div>
        </div>

        <div class="input-container">
          <div class="input-wrapper">
            <textarea
                ref="inputRef"
                v-model="inputMessage"
                class="input-field"
                placeholder="聊点什么..."
                rows="1"
                @input="handleInput"
                @keydown.enter.exact.prevent="sendMessage"
            ></textarea>
            <button class="send-btn" @click="sendMessage">
              <i class="fas fa-arrow-up"></i>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!--  用户 Profile 弹窗  -->
    <div v-if="showProfileModal" class="modal-overlay" @click="closeModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3 class="modal-title">用户资料</h3>
          <i class="fas fa-times modal-close" @click="closeModal"></i>
        </div>
        <div class="modal-body">
          <div class="profile-info">
            <div class="profile-avatar" :style="{ background: getColor(0) }">
              {{ viewingUser?.username?.[0]?.toUpperCase() || '?' }}
            </div>
            <div class="profile-details">
              <div class="profile-name">{{ viewingUser?.username || '加载中...' }}</div>
              <div class="profile-bio">{{ viewingUser?.bio || '这个人很懒，什么都没写~' }}</div>
            </div>
          </div>
          <div class="profile-stats" v-if="viewingUser">
            <div class="stat-item">
              <div class="stat-value">{{ viewingUser.mistakeCount || 0 }}</div>
              <div class="stat-label">错题数</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">{{ viewingUser.aiCount || 0 }}</div>
              <div class="stat-label">AI数</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">{{ viewingUser.days || 1 }}</div>
              <div class="stat-label">在线天</div>
            </div>
          </div>
          <div v-if="viewingUser?.isFriend" class="already-friend">
            <i class="fas fa-check-circle"></i> 你们已经是好友了
          </div>
        </div>
        <div class="modal-footer">
          <button class="modal-btn cancel" @click="closeModal">关闭</button>
          <button v-if="viewingUser?.isFriend" class="modal-btn confirm" @click="goToPrivateChat">
            <i class="fas fa-comment"></i> 去聊天
          </button>
          <button v-else class="modal-btn confirm" :disabled="requestSent" @click="sendFriendRequest">
            <i class="fas fa-user-plus"></i>
            {{ requestSent ? '申请已发送' : '加好友' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

const userId = route.params.userId
const username = ref('')
const messages = ref([])
const onlineUsers = ref(new Set())
const inputMessage = ref('')
const showCmdPanel = ref(false)
const socket = ref(null)
const messagesContainer = ref(null)
const inputRef = ref(null)

const showProfileModal = ref(false)
const viewingUser = ref(null)
const requestSent = ref(false)

const getColor = (index) => {
  const colors = ['#FF6B6B', '#4ECDC4', '#45B7D1', '#FFA07A', '#98D8C8']
  return colors[index % colors.length]
}
const getClass = (msg) => {
  if (msg.type === 'SYSTEM') return 'system'
  if (msg.isOwn) return 'own'
  return 'other'
}
const formatTime = (ts) => {
  if (!ts) return ''
  const d = new Date(ts)
  return `${d.getHours()}:${String(d.getMinutes()).padStart(2, '0')}`
}
const scrollToBottom = () => {
  nextTick(() => { if (messagesContainer.value) messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight })
}
const appendMsg = (type, content, from = null, isOwn = false, createdAt = null) => {
  messages.value.push({ id: Date.now() + Math.random(), type, content, from: from || username.value, isOwn, createdAt: createdAt || new Date().toISOString() })
  scrollToBottom()
}

//已弃用
const handleInput = () => {
  showCmdPanel.value = inputMessage.value === '/'
  if (inputRef.value) {
    inputRef.value.style.height = '34px'
    inputRef.value.style.height = Math.min(inputRef.value.scrollHeight, 100) + 'px'
  }
}

//已弃用
const selectCmd = (cmd) => {
  inputMessage.value = cmd
  showCmdPanel.value = false
  inputRef.value?.focus()
}

//发消息
const sendMessage = () => {
  const msg = inputMessage.value.trim()
  if (!msg) return
  if (socket.value && socket.value.readyState === WebSocket.OPEN) {
    socket.value.send(msg)
    appendMsg('USER', msg, username.value, true)  //乐观更新，立刻显示在界面
  } else {
    appendMsg('SYSTEM', '连接已断开，请刷新页面')
  }
  inputMessage.value = ''
  showCmdPanel.value = false
  if (inputRef.value) inputRef.value.style.height = '34px'
}

//加载记录
const loadHistory = async () => {
  try {
    const token = localStorage.getItem('token')
    const res = await fetch('/api/chat/public/messages', { headers: { 'Authorization': 'Bearer ' + token } })
    const data = await res.json()
    if (data.code === 200) {
      messages.value = data.data.map(msg => ({ id: msg.id, type: 'USER', content: msg.content, from: msg.fromUsername, isOwn: msg.fromUserId === userId, createdAt: msg.createdAt }))
      scrollToBottom()
    }
  } catch (e) { console.error('加载历史消息失败', e) }
}

//建立连接
const initWebSocket = () => {
  const token = localStorage.getItem('token')
  const wsHost = import.meta.env.DEV ? 'localhost:8080' : window.location.host
  //token放URL参数传给后端验证
  socket.value = new WebSocket(`ws://${wsHost}/symphony?token=${token}`)
  socket.value.onopen = () => { appendMsg('SYSTEM', '✅ 已连接至聊天服务器') }
  socket.value.onmessage = (e) => { handleMsg(JSON.parse(e.data)) }
  socket.value.onerror = () => { appendMsg('SYSTEM', '⚠️ 连接失败，请检查网络') }
  socket.value.onclose = (event) => {
    //token过期时后端关闭连接，code=1008，跳转登录页
    if (event.code === 1008) {
      appendMsg('SYSTEM', 'Token已过期，请重新登录')
      localStorage.removeItem('token')
      setTimeout(() => router.push('/login'), 2000)
    } else { appendMsg('SYSTEM', '已断开连接') }
  }
}

const goToPrivateChat = () => {
  closeModal()
  router.push(`/chat/${userId}/UTOU/${viewingUser.value.userId}`)
}

//处理收到的消息
const handleMsg = (msg) => {
  const { messageType, content, createdAt } = msg
  const from = msg.fromName
  if (messageType === 'SYSTEM') {
    appendMsg('SYSTEM', content)
    if (content.includes('在线') || content.includes('Online')) {
      const match = content.match(/[:：](.+)/)
      if (match) {
        const names = match[1].trim().split(/[,，\s]+/).filter(n => n)
        onlineUsers.value.clear()
        names.forEach(n => onlineUsers.value.add(n.trim()))
      }
    }
    if (content.includes('进来了') || content.includes('加入')) {
      const name = content.replace(/进来了|加入了|entered|joined/g, '').trim()
      if (name) onlineUsers.value.add(name)
    }
    if (content.includes('离开了') || content.includes('退出')) {
      const name = content.replace(/离开了|退出了|left/g, '').trim()
      if (name) onlineUsers.value.delete(name)
    }
  } else if (messageType === 'CHAT' || messageType === 'USER') {
    const isOwn = from === username.value
    if (!isOwn) appendMsg('USER', content, from, false, createdAt)
  }
}

//跳转到和该用户的私聊
const goToPrivate = async () => {
  try {
    const token = localStorage.getItem('token')
    const res = await fetch('/api/chat/private/list', { headers: { 'Authorization': 'Bearer ' + token } })
    const data = await res.json()
    if (data.code === 200 && data.data.length > 0) router.push(`/chat/${userId}/UTOU/${data.data[0].userId}`)
    else window.$toast.info('暂无私聊记录，可以从在线用户列表中添加好友开始私聊')
  } catch { window.$toast.info('加载失败，请重试') }
}

//点击用户名查看资料
const openUserProfile = async (targetUsername) => {
  if (targetUsername === username.value) return //不能查自己
  //调接口拿用户信息
  showProfileModal.value = true
  viewingUser.value = null
  requestSent.value = false
  try {
    const token = localStorage.getItem('token')
    const res = await fetch(`/api/user/profile/${targetUsername}`, { headers: { 'Authorization': 'Bearer ' + token } })
    const data = await res.json()
    if (data.code === 200) viewingUser.value = data.data
  } catch (e) {
    viewingUser.value = { username: targetUsername, bio: '', isFriend: false }
  }
}

//发好友申请
const sendFriendRequest = async () => {
  if (!viewingUser.value || viewingUser.value.isFriend || requestSent.value) return    //已是好友或已申请过
  try {
    const token = localStorage.getItem('token')
    const res = await fetch('/api/friends/request', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token },
      body: JSON.stringify({ targetUsername: viewingUser.value.username })
    })
    const data = await res.json()
    if (data.code === 200) { requestSent.value = true; appendMsg('SYSTEM', `✅ 已向 ${viewingUser.value.username} 发送好友申请`) }
    else window.$toast.info(data.msg || '申请发送失败')
  } catch (e) { window.$toast.info('网络错误，请重试') }
}
const closeModal = () => { showProfileModal.value = false; viewingUser.value = null }

//验证登录->拉用户名->加载历史消息->建立WebSocket连接
onMounted(async () => {
  const token = localStorage.getItem('token')
  if (!token) { router.push('/login'); return }
  const res = await fetch('/api/auth/profile', { headers: { 'Authorization': 'Bearer ' + token } })
  const data = await res.json()
  if (data.code === 200) username.value = data.data.username
  else { router.push('/login'); return }
  await loadHistory()
  initWebSocket()
})
//页面销毁关闭websocket
onUnmounted(() => { if (socket.value) socket.value.close() })
</script>

<style>
:root {
  --primary:      #FF6B6B;
  --primary-dark: #EE5253;
  --secondary:    #0039a6;
  --secondary-h:  #004cd5;
  --gold:         #c9a84c;
  --gold-light:   #e8d08a;
  --bg:           #EEF1F8;
  --border:       #E8ECEF;
  --text:         #2D3436;
  --text-light:   #636E72;
}
</style>

<style scoped>
* { margin: 0; padding: 0; box-sizing: border-box; }

.chat-page {
  height: 100vh; background: var(--bg);
  display: flex; align-items: center; justify-content: center; padding: 20px;
  position: relative; overflow: hidden;
  box-shadow:
      inset 0 0 0 4px rgba(201,168,76,0.6),
      inset 0 0 0 8px rgba(232,208,138,0.3),
      inset 0 0 0 13px rgba(245,228,156,0.15);
}
.chat-page::before {
  content: ''; position: absolute; top: -80px; left: -60px;
  width: 320px; height: 320px; border-radius: 50%;
  border: 28px solid transparent;
  border-top-color: #0039a6; border-right-color: #004cd5;
  opacity: 0.18; transform: rotate(-30deg); pointer-events: none;
}
.chat-page::after {
  content: ''; position: absolute; bottom: -60px; right: -50px;
  width: 280px; height: 280px; border-radius: 50%;
  border: 22px solid transparent;
  border-bottom-color: #c9a84c; border-left-color: #e8d08a;
  opacity: 0.22; transform: rotate(20deg); pointer-events: none;
}

.chat-container {
  width: 100%; max-width: 1100px; height: calc(100vh - 40px); max-height: 900px;
  background: white; border-radius: 24px;
  display: grid; grid-template-columns: 280px 1fr;
  box-shadow:
      0 0 0 1.5px rgba(201,168,76,0.35),
      0 0 0 4px rgba(201,168,76,0.08),
      0 15px 40px rgba(0,0,0,0.10);
  outline: 1.5px solid rgba(201,168,76,0.18);
  overflow: hidden; position: relative; z-index: 1;
}

.chat-container::before {
  content: ''; position: absolute; top: 0; left: 0; right: 0; height: 3px;
  background: linear-gradient(90deg, #8c6d1f 0%, #e8d08a 25%, #f5e49c 50%, #c9a84c 75%, #8c6d1f 100%);
  z-index: 10; border-radius: 24px 24px 0 0;
}

.chat-container::after {
  content: ''; position: absolute; bottom: 0; left: 0; right: 0; height: 2px;
  background: linear-gradient(90deg, transparent, rgba(201,168,76,0.3) 20%, rgba(245,228,156,0.5) 50%, rgba(201,168,76,0.3) 80%, transparent);
  z-index: 10; border-radius: 0 0 24px 24px;
  pointer-events: none;
}

.sidebar { background: #F8F9FB; border-right: 1px solid var(--border); display: flex; flex-direction: column; }
.sidebar-header { padding: 20px; text-align: center; border-bottom: 1px solid rgba(0,0,0,0.03); }
.logo-text { font-size: 18px; font-weight: 900; color: var(--secondary); }
.online-count { font-size: 11px; color: var(--text-light); margin-top: 4px; }
.chat-types { padding: 10px; display: flex; flex-direction: column; gap: 6px; border-bottom: 1px solid var(--border); }
.type-tab { padding: 12px 15px; background: white; border-radius: 12px; font-size: 13px; font-weight: 700; color: var(--text); cursor: pointer; transition: all 0.3s; display: flex; align-items: center; gap: 10px; }
.type-tab:hover { background: #eef2ff; color: var(--secondary); }
.type-tab.active { background: #C9A84CFF; color: white; box-shadow: 0 2px 8px rgba(0,57,166,0.28); }
.channel-list { flex: 1; overflow-y: auto; padding: 10px; }
.section-title { font-size: 11px; font-weight: 800; color: var(--text-light); text-transform: uppercase; letter-spacing: 0.5px; padding: 10px 15px 8px; }
.channel-item {
  display: flex; align-items: center; padding: 12px; margin-bottom: 6px;
  background: white; border-radius: 12px; cursor: pointer; transition: all 0.3s;
  border: 2px solid transparent; position: relative;
}
.channel-item:hover { background: #eef2ff; border-color: rgba(0,57,166,0.15); }
.channel-item.active { background: linear-gradient(135deg,#eef2ff,#fefce8); border-color: rgba(201,168,76,0.35); }
.channel-item.active::before {
  content: ''; position: absolute; left: 0; top: 20%; bottom: 20%;
  width: 3px; border-radius: 2px;
  background: linear-gradient(180deg, var(--gold-light), var(--gold));
}
.channel-avatar { width: 36px; height: 36px; border-radius: 10px; color: white; display: flex; align-items: center; justify-content: center; font-size: 16px; flex-shrink: 0; margin-right: 12px; }
.channel-info { flex: 1; }
.channel-name { font-size: 13px; font-weight: 700; color: var(--text); margin-bottom: 2px; }
.channel-item.active .channel-name { color: var(--secondary); }
.channel-preview { font-size: 11px; color: var(--text-light); }
.user-item {
  display: flex; align-items: center; padding: 10px 12px; margin-bottom: 6px;
  background: white; border-radius: 10px; cursor: pointer; transition: all 0.3s;
  border: 1.5px solid transparent;
}
.user-item:hover { background: #eef2ff; border-color: rgba(0,57,166,0.2); transform: translateX(2px); }
.user-avatar { width: 28px; height: 28px; border-radius: 8px; color: white; display: flex; align-items: center; justify-content: center; font-weight: bold; font-size: 11px; flex-shrink: 0; margin-right: 10px; }
.user-name { font-size: 12px; font-weight: 600; color: var(--text); flex: 1; }


.chat-main { display: flex; flex-direction: column; height: 100%; overflow: hidden; }
.chat-header {
  padding: 15px 25px; border-bottom: 1px solid var(--border);
  display: flex; align-items: center; justify-content: space-between; flex-shrink: 0;
  position: relative;
}
.chat-header::after {
  content: ''; position: absolute; bottom: -1px; left: 0; right: 0; height: 1px;
  background: linear-gradient(90deg, transparent, rgba(201,168,76,0.4) 25%, rgba(245,228,156,0.65) 50%, rgba(201,168,76,0.4) 75%, transparent);
}
.header-left { flex: 1; }
.header-title { font-weight: 800; color: var(--text); font-size: 16px; display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.header-subtitle { font-size: 12px; color: var(--text-light); }
.header-actions { display: flex; gap: 10px; }
.header-btn { padding: 8px 16px; border: 2px solid var(--border); background: white; border-radius: 10px; font-size: 13px; font-weight: 700; color: var(--text); cursor: pointer; transition: all 0.3s; display: flex; align-items: center; gap: 6px; }
.header-btn:hover { border-color: var(--secondary); color: var(--secondary); }


.messages-container { flex: 1; overflow-y: auto; padding: 20px 25px; display: flex; flex-direction: column; gap: 18px; min-height: 0; }
.messages-container::-webkit-scrollbar { width: 4px; }
.messages-container::-webkit-scrollbar-thumb { background: rgba(0,0,0,0.15); border-radius: 10px; }
.message { display: flex; align-items: flex-start; gap: 12px; max-width: 85%; }
.message.own { align-self: flex-end; flex-direction: row-reverse; }
.message.other { align-self: flex-start; }
.message.system { align-self: center; }
.msg-avatar { width: 38px; height: 38px; border-radius: 12px; color: white; display: flex; align-items: center; justify-content: center; font-weight: bold; flex-shrink: 0; font-size: 16px; }
.msg-body { display: flex; flex-direction: column; gap: 4px; }
.message.own .msg-body { align-items: flex-end; }
.msg-info { font-size: 11px; color: var(--text-light); }
.msg-bubble { padding: 10px 15px; border-radius: 4px 15px 15px 15px; background: #F1F3F6; font-size: 14.5px; line-height: 1.5; color: var(--text); word-break: break-word; min-width: 40px; }
.message.own .msg-bubble { background: var(--secondary); color: white; border-radius: 15px 4px 15px 15px; box-shadow: 0 3px 12px rgba(0,57,166,0.2); }
.system-bubble { background: linear-gradient(135deg,#fffbeb,#fef9e7); color: #7a5c00; font-size: 11px; padding: 6px 14px; border-radius: 20px; font-weight: 600; border: 1px solid rgba(201,168,76,0.25); }
.msg-time { font-size: 10px; color: var(--text-light); }
.empty-state { text-align: center; padding: 60px 20px; color: var(--text-light); }
.empty-icon { font-size: 60px; margin-bottom: 15px; opacity: 0.3; }
.empty-text { font-size: 14px; }


.input-container { padding: 12px 25px 18px; background: white; flex-shrink: 0; position: relative; }
.input-container::before {
  content: ''; position: absolute; top: -1px; left: 0; right: 0; height: 1px;
  background: linear-gradient(90deg, transparent, rgba(201,168,76,0.4) 25%, rgba(245,228,156,0.65) 50%, rgba(201,168,76,0.4) 75%, transparent);
}
.command-panel {
  position: absolute; bottom: calc(100% + 5px); left: 25px; width: 220px;
  background: white; border-radius: 15px; padding: 8px;
  box-shadow: 0 8px 25px rgba(0,0,0,0.1); border: 1.5px solid rgba(0,57,166,0.25);
  z-index: 100; animation: pop 0.2s ease;
}
@keyframes pop { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
.command-item { padding: 9px 12px; cursor: pointer; border-radius: 10px; font-size: 13px; font-weight: 600; color: var(--text); display: flex; align-items: center; gap: 10px; transition: all 0.2s; }
.command-item:hover { background: #eef2ff; color: var(--secondary); }
.input-wrapper { display: flex; gap: 10px; align-items: center; background: #F1F3F6; padding: 5px 12px; border-radius: 25px; border: 1.5px solid transparent; transition: 0.3s; }
.input-wrapper:focus-within { background: white; border-color: rgba(0,57,166,0.25); box-shadow: 0 0 0 3px rgba(0,57,166,0.05); }
.input-field { flex: 1; min-height: 34px; max-height: 100px; background: transparent; border: none; outline: none; font-size: 14px; resize: none; line-height: 1.5; color: var(--text); font-family: inherit; }
.send-btn { width: 32px; height: 32px; border-radius: 50%; background: var(--secondary); color: white; border: none; cursor: pointer; transition: 0.2s; display: flex; align-items: center; justify-content: center; flex-shrink: 0; box-shadow: 0 2px 8px rgba(0,57,166,0.3); }
.send-btn:hover { background: var(--secondary-h); transform: scale(1.05); }


.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.45); display: flex; align-items: center; justify-content: center; z-index: 1000; animation: fadeIn 0.2s ease; }
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
.modal-content {
  background: white; border-radius: 20px; width: 90%; max-width: 400px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.18), 0 0 0 1px rgba(201,168,76,0.2);
  animation: slideUp 0.3s ease; overflow: hidden; position: relative;
}
.modal-content::before {
  content: ''; position: absolute; top: 0; left: 0; right: 0; height: 3px;
  background: linear-gradient(90deg, #8c6d1f, #e8d08a 30%, #f5e49c 50%, #c9a84c 70%, #8c6d1f);
}
@keyframes slideUp { from { opacity: 0; transform: translateY(30px); } to { opacity: 1; transform: translateY(0); } }
.modal-header { padding: 20px 25px; border-bottom: 1px solid var(--border); display: flex; align-items: center; justify-content: space-between; }
.modal-title { font-size: 18px; font-weight: 800; color: var(--text); }
.modal-close { cursor: pointer; color: var(--text-light); padding: 5px; transition: color 0.2s; }
.modal-close:hover { color: var(--primary); }
.modal-body { padding: 25px; }
.profile-info { display: flex; align-items: center; gap: 15px; margin-bottom: 20px; }
.profile-avatar { width: 70px; height: 70px; border-radius: 50%; color: white; display: flex; align-items: center; justify-content: center; font-size: 28px; font-weight: 900; flex-shrink: 0; box-shadow: 0 4px 15px rgba(0,0,0,0.15); }
.profile-name { font-size: 18px; font-weight: 800; color: var(--text); margin-bottom: 6px; }
.profile-bio { font-size: 13px; color: var(--text-light); line-height: 1.5; }
.profile-stats { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; background: var(--bg); border-radius: 12px; padding: 15px; margin-bottom: 15px; }
.stat-item { text-align: center; }
.stat-value { font-size: 18px; font-weight: 900; color: var(--secondary); }
.stat-label { font-size: 11px; color: var(--text-light); margin-top: 3px; }
.already-friend { text-align: center; padding: 10px; background: #eef2ff; color: var(--secondary); border-radius: 10px; font-size: 13px; font-weight: 700; border: 1px solid rgba(0,57,166,0.2); }
.modal-footer { padding: 15px 25px 20px; display: flex; gap: 12px; }
.modal-btn { flex: 1; padding: 12px; border: none; border-radius: 12px; font-size: 14px; font-weight: 700; cursor: pointer; transition: all 0.3s; display: flex; align-items: center; justify-content: center; gap: 6px; }
.modal-btn.cancel { background: var(--bg); color: var(--text); }
.modal-btn.cancel:hover { background: #E0E4E8; }
.modal-btn.confirm { background: var(--secondary); color: white; box-shadow: 0 4px 12px rgba(0,57,166,0.25); }
.modal-btn.confirm:hover:not(:disabled) { background: var(--secondary-h); transform: translateY(-2px); }
.modal-btn.confirm:disabled { opacity: 0.6; cursor: not-allowed; transform: none; }

@media (max-width: 800px) {
  .chat-container { grid-template-columns: 1fr; height: 100vh; border-radius: 0; }
  .sidebar { display: none; }
  .chat-page { padding: 0; }
}
</style>