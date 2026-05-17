<template>
  <div class="chat-page">
    <div :class="['chat-container', { 'with-drawer': drawerOpen }]">

      <!--   侧边栏  -->
      <div class="sidebar">
        <div class="sidebar-header">
          <div class="logo-text">🎵 SYMPHONY</div>
          <p class="online-count">我的好友</p>
        </div>

        <div class="chat-types">
          <div class="type-tab active">
            <i class="fas fa-user"></i><span>学伴</span>
          </div>
          <div class="type-tab" @click="$router.push(`/chat/${userId}/public`)">
            <i class="fas fa-users"></i><span>公屏</span>
          </div>
        </div>

        <div class="channel-list">
          <div class="section-title">我的私聊</div>
          <div
              v-for="(chat, index) in privateChats"
              :key="chat.id"
              :class="['channel-item', { active: currentTargetId === chat.id }]"
              @click="switchChat(chat)"
          >
            <div class="channel-avatar" :style="{ background: getColor(index) }">
              {{ chat.username[0].toUpperCase() }}
            </div>
            <div class="channel-info">
              <div class="channel-name">
                {{ chat.username }}
                <span v-if="chat.isFriend" class="friend-badge">好友</span>
              </div>
              <div class="channel-preview">{{ chat.lastMessage || '开始聊天...' }}</div>
            </div>
            <span v-if="chat.unread" class="unread-dot"></span>
          </div>
          <div v-if="privateChats.length === 0" class="empty-tip">
            <i class="fas fa-user-plus"></i>
            <span>暂无私聊，去公屏认识朋友吧</span>
          </div>
        </div>
      </div>

      <!--  聊天主区   -->
      <div class="chat-main">
        <div class="chat-header">
          <div class="header-left">
            <h4 class="header-title">
              <i class="fas fa-user" style="color: #0039a6;"></i>
              {{ currentChat?.username || '请选择聊天' }}
            </h4>
            <span class="header-subtitle">
              {{ aiEnabled ? '🤖 AI辅助对话中' : '💬 普通对话模式' }}
            </span>
          </div>
          <div class="header-actions">
            <div v-if="currentChat?.isFriend" :class="['ai-status-badge', { active: aiEnabled }]">
              <i class="fas fa-robot"></i>
              <span>{{ aiEnabled ? 'AI ON' : 'AI OFF' }}</span>
            </div>
            <button class="gear-btn" @click="drawerOpen = !drawerOpen" :class="{ 'gear-active': drawerOpen }">
              <i :class="drawerOpen ? 'fas fa-times' : 'fas fa-sliders-h'"></i>
            </button>
          </div>
        </div>

        <div v-if="commonPoints.length > 0" class="common-points-bar">
          <i class="fas fa-fire"></i>
          你们两个都在
          <span v-for="(point, index) in commonPoints" :key="point">
            <strong>{{ point }}</strong><span v-if="index < commonPoints.length - 1">、</span>
          </span>
          这块知识点有错题哦，可以加强学习讨论 💪
          <i class="fas fa-times close-bar" @click="commonPoints = []"></i>
        </div>

        <div ref="messagesContainer" class="messages-container">
          <div v-if="loadingMore || !hasMore" class="load-indicator">
            <div v-if="loadingMore" class="loading-spinner">
              <div class="spinner"></div>
              <span>加载历史消息...</span>
            </div>
            <div v-else-if="!hasMore" class="no-more-hint">
              <i class="fas fa-check-circle"></i>
              已加载全部消息
            </div>
          </div>
          <div v-for="msg in messages" :key="msg.id" :class="['message', getClass(msg)]">
            <template v-if="msg.type === 'SYSTEM'">
              <div class="msg-bubble system-bubble">{{ msg.content }}</div>
            </template>
            <template v-else>
              <div class="msg-avatar" :style="{ background: msg.isAI ? '#111e71' : msg.isOwn ? 'var(--secondary)' : 'var(--primary)' }">
                {{ msg.isAI ? '🤖' : msg.from[0]?.toUpperCase() }}
              </div>
              <div class="msg-body">
                <div class="msg-info">
                  {{ msg.from }}
                  <span v-if="msg.isAI" class="ai-tag">AI</span>
                </div>
                <div class="msg-bubble">{{ msg.content }}</div>
                <div class="msg-time">{{ formatTime(msg.createdAt) }}</div>
              </div>
            </template>
          </div>

          <div v-if="messages.length === 0" class="empty-state">
            <div class="empty-icon">💬</div>
            <div class="empty-text">开始聊天吧！</div>
          </div>

          <div v-if="recommendText" class="recommend-bubble">
            <div class="recommend-bubble-icon">💡</div>
            <div class="recommend-bubble-content">
              <div class="recommend-bubble-title">今日学习建议</div>
              <div class="recommend-bubble-text">{{ recommendText }}</div>
            </div>
            <i class="fas fa-times" @click="closeRecommend" style="cursor:pointer;opacity:0.4;font-size:12px;flex-shrink:0;"></i>
          </div>
        </div>

        <div class="input-container">
          <div class="input-wrapper">
            <textarea
                ref="inputRef"
                v-model="inputMessage"
                class="input-field"
                :placeholder="aiEnabled ? '发送消息（AI会参与对话）' : '发送消息...'"
                rows="1"
                @input="autoResize"
                @keydown.enter.exact.prevent="sendMessage"
            ></textarea>
            <button class="send-btn" @click="sendMessage">
              <i class="fas fa-arrow-up"></i>
            </button>
          </div>
        </div>
      </div>

      <!-- 右侧功能抽屉 -->
      <transition name="drawer">
        <div v-if="drawerOpen" class="function-drawer">
          <div class="drawer-section">
            <div class="drawer-section-label">当前对话</div>
            <div class="drawer-chat-info" v-if="currentChat">
              <div class="dci-avatar" :style="{ background: getColor(0) }">{{ currentChat.username[0].toUpperCase() }}</div>
              <div>
                <div class="dci-name">{{ currentChat.username }}</div>
                <div class="dci-tag" v-if="currentChat.isFriend">好友</div>
              </div>
            </div>
          </div>

          <div class="drawer-section">
            <div class="drawer-section-label">AI 辅助</div>
            <div v-if="currentChat?.isFriend" class="drawer-toggle-row" @click="toggleAI">
              <div class="dtr-left">
                <div :class="['dtr-icon', { 'dtr-on': aiEnabled }]"><i class="fas fa-robot"></i></div>
                <div>
                  <div class="dtr-title">AI 辅助对话</div>
                  <div class="dtr-sub">{{ aiEnabled ? '已开启，AI会参与对话' : '已关闭' }}</div>
                </div>
              </div>
              <div :class="['toggle-switch', { on: aiEnabled }]">
                <div class="toggle-thumb"></div>
              </div>
            </div>
            <div v-else class="drawer-disabled">仅好友间可开启AI</div>
          </div>

          <div class="drawer-section">
            <div class="drawer-section-label">工具</div>
            <!--  改为 confirmSummarize，添加确认弹窗 -->
            <button class="drawer-action-btn" @click="confirmSummarize" :disabled="summarizing">
              <i class="fas fa-brain"></i>
              <div>
                <div class="dab-title">{{ summarizing ? '整理中...' : '整理知识点' }}</div>
                <div class="dab-sub">AI提取对话中的知识点</div>
              </div>
              <i class="fas fa-chevron-right dab-arrow"></i>
            </button>
            <!-- 编写协作文档 -->
            <button v-if="currentChat?.isFriend" class="drawer-action-btn" @click="openCollab(); drawerOpen = false">
              <i class="fas fa-file-alt"></i>
              <div>
                <div class="dab-title">编写协作文档</div>
                <div class="dab-sub">与对方共同编写文档</div>
              </div>
              <i class="fas fa-chevron-right dab-arrow"></i>
            </button>

            <!-- 查看历史协作画廊 -->
            <button v-if="currentChat?.isFriend" class="drawer-action-btn" @click="openGallery(); drawerOpen = false">
              <i class="fas fa-clock-rotate-left"></i>
              <div>
                <div class="dab-title">查看历史协作画廊</div>
                <div class="dab-sub">浏览过往协作文档</div>
              </div>
              <i class="fas fa-chevron-right dab-arrow"></i>
            </button>
          </div>

          <div class="drawer-section">
            <button class="drawer-back-btn" @click="$router.push('/home')">
              <i class="fas fa-arrow-left"></i> 返回首页
            </button>
          </div>
        </div>
      </transition>
    </div>

    <!--  协作文档弹窗  -->
    <transition name="collab-modal">
      <div v-if="collabVisible" class="collab-modal-overlay" @click.self="closeCollab">
        <div class="collab-modal">
          <div class="collab-modal-header">
            <div class="collab-modal-title">
              <div class="collab-title-icon"><i class="fas fa-file-alt"></i></div>
              <div>
                <div class="collab-title-text">协作文档</div>
                <div class="collab-title-sub">与 {{ currentChat?.username }} 实时协作</div>
              </div>
            </div>
            <div class="collab-header-right">
              <div class="collab-tab-switch">
                <button :class="['ctab', { active: collabView === 'edit' }]" @click="collabView = 'edit'">
                  <i class="fas fa-pen"></i> 编辑
                </button>
                <button :class="['ctab', { active: collabView === 'preview' }]" @click="collabView = 'preview'">
                  <i class="fas fa-eye"></i> 预览
                </button>
              </div>
              <button class="collab-icon-btn" @click="openGallery" title="查看历史">
                <i class="fas fa-clock-rotate-left"></i>
              </button>
              <button class="collab-save-pill" @click="saveCollab" :class="{ saving: collabSaving }">
                <i :class="collabSaving ? 'fas fa-spinner fa-spin' : 'fas fa-cloud-upload-alt'"></i>
                {{ collabSaving ? '保存中' : '保存' }}
              </button>
              <button class="collab-close-x" @click="closeCollab">
                <i class="fas fa-times"></i>
              </button>
            </div>
          </div>

          <div class="collab-toolbar" v-if="collabView === 'edit'">
            <button class="ctool" @click="insertMd('**', '**')" title="粗体"><b>B</b></button>
            <button class="ctool" @click="insertMd('*', '*')" title="斜体"><i>I</i></button>
            <button class="ctool" @click="insertMd('# ', '')" title="标题"><i class="fas fa-heading"></i></button>
            <button class="ctool" @click="insertMd('- ', '')" title="列表"><i class="fas fa-list-ul"></i></button>
            <button class="ctool" @click="insertMd('> ', '')" title="引用"><i class="fas fa-quote-left"></i></button>
            <button class="ctool" @click="insertMd('\`', '\`')" title="代码"><i class="fas fa-code"></i></button>
            <div class="ctool-divider"></div>
            <span class="collab-wordcount">{{ collabContent.length }} 字</span>
          </div>

          <div class="collab-body">
            <textarea
                v-if="collabView === 'edit'"
                ref="collabEditorRef"
                v-model="collabContent"
                class="collab-textarea"
                placeholder="开始编写协作文档...&#10;&#10;支持 Markdown 语法，例如：&#10;# 标题&#10;**粗体** *斜体*&#10;- 列表项"
                @input="onCollabInput"
            ></textarea>
            <div v-else class="collab-preview" v-html="renderMarkdown(collabContent)"></div>
          </div>

          <div class="collab-modal-footer">
            <div class="collab-status">
              <span class="collab-dot" :class="{ active: collabConnected }"></span>
              {{ collabConnected ? '实时同步中' : '离线模式' }}
            </div>
            <div class="collab-footer-right">
              <span class="collab-hint"><i class="fas fa-keyboard"></i> Ctrl+S 快速保存</span>
            </div>
          </div>
        </div>
      </div>
    </transition>

    <!-- 浮动 AI 小球  -->
    <div class="ai-ball-wrap">
      <transition name="ai-panel">
        <div v-if="aiPanelOpen" class="ai-float-panel">
          <div class="ai-panel-header">
            <div class="ai-panel-title">
              <div class="ai-panel-avatar">{{ currentAI?.emoji || '🤖' }}</div>
              <div>
                <div class="ai-panel-name">{{ currentAI?.name || 'AI助手' }}</div>
                <div class="ai-panel-sub">RAG 记忆</div>
              </div>
            </div>
            <div class="ai-panel-acts">
              <div class="ai-switcher" v-if="myAIs.length > 1">
                <button class="ai-switch-btn" @click="switcherOpen = !switcherOpen">
                  <i class="fas fa-exchange-alt"></i>
                </button>
                <div v-if="switcherOpen" class="ai-switch-menu">
                  <div
                      v-for="ai in myAIs"
                      :key="ai.id"
                      :class="['ai-switch-item', { active: currentAiId === ai.id }]"
                      @click="selectAI(ai)"
                  >
                    <span class="asi-emoji">{{ ai.emoji }}</span>
                    <span class="asi-name">{{ ai.name }}</span>
                  </div>
                </div>
              </div>
              <button class="ai-panel-close" @click="aiPanelOpen = false">
                <i class="fas fa-times"></i>
              </button>
            </div>
          </div>

          <div ref="aiMsgContainer" class="ai-panel-msgs">
            <div v-for="msg in aiMessages" :key="msg.id" :class="['ai-msg', msg.isOwn ? 'ai-msg-own' : 'ai-msg-other']">
              <template v-if="msg.type !== 'SYSTEM'">
                <div class="ai-msg-avatar" :style="{ background: msg.isOwn ? '#1a1a2e' : '#004cd5' }">
                  {{ msg.isOwn ? userInitial : (currentAI?.emoji || '🤖') }}
                </div>
                <div class="ai-msg-body">
                  <div class="ai-msg-bubble" :class="{ 'streaming': msg.streaming }">{{ msg.content }}</div>
                  <div class="ai-msg-time">{{ formatTime(msg.createdAt) }}</div>
                </div>
              </template>
            </div>
            <div v-if="aiMessages.length === 0" class="ai-empty">
              <div style="font-size:36px;margin-bottom:10px;">{{ currentAI?.emoji || '🤖' }}</div>
              <div style="font-size:13px;color:#888;">向AI提问吧</div>
            </div>
          </div>

          <div class="ai-panel-input">
            <textarea
                ref="aiInputRef"
                v-model="aiInputMsg"
                class="ai-input-field"
                placeholder="向AI提问..."
                rows="1"
                @input="autoResizeAI"
                @keydown.enter.exact.prevent="sendAIMsg"
            ></textarea>
            <button class="ai-send-btn" @click="sendAIMsg" :disabled="aiLoading">
              <i :class="aiLoading ? 'fas fa-spinner fa-spin' : 'fas fa-arrow-up'"></i>
            </button>
          </div>
        </div>
      </transition>

      <div
          :class="['ai-ball', { 'ball-open': aiPanelOpen, 'ball-pulse': !aiPanelOpen }]"
          @click="toggleAIPanel"
          :title="aiPanelOpen ? '关闭AI' : '打开AI助手'"
      >
        <div class="ball-inner">
          <span class="ball-emoji">{{ aiPanelOpen ? '✕' : (currentAI?.emoji || '🤖') }}</span>
        </div>
        <div class="ball-ring"></div>
        <div class="ball-ring ring2"></div>
      </div>
    </div>

    <!--  整理知识点确认弹窗 -->
    <transition name="modal">
      <div v-if="summarizeConfirmVisible" class="modal-overlay" @click.self="summarizeConfirmVisible = false">
        <div class="confirm-modal">
          <div class="confirm-icon">🧠</div>
          <div class="confirm-title">整理知识点</div>
          <div class="confirm-desc">
            AI将分析本次对话内容并提取知识点。<br>
            此操作会消耗 AI 额度，建议对话积累一定内容后再使用。
          </div>
          <div class="confirm-footer">
            <button class="btn-confirm" @click="doSummarize">确认整理</button>
            <button class="btn-skip" @click="summarizeConfirmVisible = false">取消</button>
          </div>
        </div>
      </div>
    </transition>

    <!-- 错题弹窗  -->
    <transition name="modal">
      <div v-if="mistakeVisible" class="modal-overlay" @click.self="mistakeVisible = false">
        <div class="mistake-modal">
          <div class="mistake-header">
            <h3>📝 检测到错题，要加入错题本吗？</h3>
            <button @click="mistakeVisible = false"><i class="fas fa-times"></i></button>
          </div>
          <div class="mistake-body">
            <div class="mistake-field">
              <label>题目</label>
              <textarea v-model="mistakeForm.question" placeholder="题目内容..." rows="3"></textarea>
            </div>
            <div class="mistake-field">
              <label>正确答案</label>
              <textarea v-model="mistakeForm.correctAnswer" placeholder="正确解法或答案..." rows="2"></textarea>
            </div>
            <div class="mistake-field">
              <label>错误原因</label>
              <textarea v-model="mistakeForm.wrongReason" placeholder="为什么做错了..." rows="2"></textarea>
            </div>
          </div>
          <div class="mistake-footer">
            <button class="btn-confirm" @click="submitMistake">加入错题本</button>
            <button class="btn-skip" @click="mistakeVisible = false">不需要</button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()
let pollTimer = null

const userId = route.params.userId
const currentTargetId = ref(route.params.targetUserId)
const username = ref('')
const messages = ref([])
const inputMessage = ref('')
const aiEnabled = ref(true)
const messagesContainer = ref(null)
const inputRef = ref(null)
const summarizing = ref(false)
const privateChats = ref([])
const collabVisible = ref(false)
const drawerOpen = ref(true)
const collabView = ref('edit')
const collabSaving = ref(false)
const collabConnected = ref(true)
const collabEditorRef = ref(null)
const collabContent = ref('')
const commonPoints = ref([])
const recommendText = ref('')
const recommendId = ref('')
const mistakeVisible = ref(false)
const mistakeForm = ref({ question: '', correctAnswer: '', wrongReason: '', knowledgePoints: '' })
const summarizeConfirmVisible = ref(false)
let collabWs = null

const currentChat = computed(() => privateChats.value.find(c => c.id === currentTargetId.value))

const aiPanelOpen = ref(false)
const switcherOpen = ref(false)
const myAIs = ref([])
const currentAiId = ref('')
const aiInputMsg = ref('')
const aiLoading = ref(false)
const aiMsgContainer = ref(null)
const aiInputRef = ref(null)
const messagesByAI = ref({})
const abortControllers = ref({})

const currentAI = computed(() => myAIs.value.find(a => a.id === currentAiId.value))
const aiMessages = computed(() => messagesByAI.value[currentAiId.value] || [])
const userInitial = computed(() => username.value ? username.value[0].toUpperCase() : '?')
const loadingMore = ref(false)
const hasMore = ref(true)
let scrollHeightBeforeLoad = 0

//关闭建议弹窗
const closeRecommend = async () => {
  recommendText.value = ''
  localStorage.setItem('recommendClosedDate', new Date().toDateString())     //记录今天已关闭
  if (recommendId.value) {
    const token = localStorage.getItem('token')
    await fetch(`/api/report/${recommendId.value}/read`, {
      method: 'PUT',        //标记已读
      headers: { 'Authorization': 'Bearer ' + token }
    })
  }
}

const toggleAIPanel = () => {
  aiPanelOpen.value = !aiPanelOpen.value
  switcherOpen.value = false
  if (aiPanelOpen.value) nextTick(() => scrollAIToBottom())
}

const selectAI = (ai) => {
  currentAiId.value = ai.id
  switcherOpen.value = false
  if (!messagesByAI.value[ai.id]) loadAIHistory(ai.id)
  else nextTick(() => scrollAIToBottom())
}

const scrollAIToBottom = () => {
  if (aiMsgContainer.value) aiMsgContainer.value.scrollTop = aiMsgContainer.value.scrollHeight
}

const loadAIHistory = async (aiId) => {
  try {
    const token = localStorage.getItem('token')
    const res = await fetch(`/api/chat/ai/${aiId}/messages`, {
      headers: { 'Authorization': 'Bearer ' + token }
    })
    const data = await res.json()
    if (data.code === 200) {
      messagesByAI.value[aiId] = data.data.map(msg => ({
        id: msg.id,
        type: msg.role === 'user' ? 'USER' : 'ASSISTANT',
        content: msg.content,
        isOwn: msg.role === 'user',
        createdAt: msg.createdAt
      }))
      nextTick(() => scrollAIToBottom())
    }
  } catch (e) { console.error('AI历史加载失败', e) }
}

//加载AI列表
const loadMyAIs = async () => {
  try {
    const token = localStorage.getItem('token')
    const res = await fetch(`/api/ai/listPersonalAis/${userId}`, {
      headers: { 'Authorization': 'Bearer ' + token }
    })
    //拉取用户的所有AI，默认选第一个
    const data = await res.json()
    if (Array.isArray(data)) {
      myAIs.value = data.map(ai => ({
        id: ai.id, name: ai.name,
        emoji: ai.avatar || '🤖',
        desc: ai.description || ''
      }))
      if (myAIs.value.length > 0) {
        currentAiId.value = myAIs.value[0].id
        loadAIHistory(currentAiId.value)
      }
    }
  } catch (e) { console.error('AI列表加载失败', e) }
}

//发消息给AI
const sendAIMsg = async () => {
  const msg = aiInputMsg.value.trim()
  if (!msg || aiLoading.value || !currentAiId.value) return

  const msgs = ensureAIMsgs(currentAiId.value)
  //先把用户消息加入列表
  msgs.push({ id: Date.now(), type: 'USER', content: msg, isOwn: true, createdAt: new Date().toISOString() })
  aiInputMsg.value = ''
  if (aiInputRef.value) aiInputRef.value.style.height = '34px'
  aiLoading.value = true
  nextTick(() => scrollAIToBottom())
  //再调流式接口
  await sendAIStream(currentAiId.value, msg)
  aiLoading.value = false
}

const ensureAIMsgs = (aiId) => {
  if (!messagesByAI.value[aiId]) messagesByAI.value[aiId] = []
  return messagesByAI.value[aiId]
}

//流式接收AI回复
const sendAIStream = async (aiId, msg) => {
  const token = localStorage.getItem('token')
  if (abortControllers.value[aiId]) abortControllers.value[aiId].abort()
  const controller = new AbortController()
  abortControllers.value[aiId] = controller

  const aiMsgId = Date.now() + Math.random()
  //先显示 "..." 占位
  appendStreamingMsg(aiId, aiMsgId)

  try {
    const response = await fetch('/api/chat/ai/stream', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token },
      body: JSON.stringify({ aiId, message: msg }),
      signal: controller.signal
    })
    if (!response.ok) { updateStreamingMsg(aiId, aiMsgId, '⚠️ AI暂时不可用'); return }
    //逐行读SSE流
    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let fullText = ''
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      const chunk = decoder.decode(value, { stream: true })
      for (const line of chunk.split('\n')) {
        if (line.startsWith('data:')) {
          try {
            const jsonStr = line.slice(line.indexOf('{'))
            const data = JSON.parse(jsonStr)
            //解析token，累加到fullText
            if (data.token) { fullText += data.token; appendTokenToAI(aiId, aiMsgId, fullText) }
          } catch {}
        }
      }
    }
  } catch (e) {
    if (e.name === 'AbortError') return
    updateStreamingMsg(aiId, aiMsgId, '⚠️ 网络错误，请重试')
  }
}

const appendStreamingMsg = (aiId, msgId) => {
  const msgs = ensureAIMsgs(aiId)
  msgs.push({ id: msgId, type: 'ASSISTANT', content: '...', isOwn: false, streaming: true, createdAt: new Date().toISOString() })
  let dots = 1
  const timer = setInterval(() => {
    const msg = msgs.find(m => m.id === msgId)
    if (!msg || !msg.streaming) { clearInterval(timer); return }
    dots = dots >= 3 ? 1 : dots + 1
    msg.content = '.'.repeat(dots)
  }, 400)
  nextTick(() => scrollAIToBottom())
}

//打字机效果
const appendTokenToAI = (aiId, msgId, fullText) => {
  const msgs = messagesByAI.value[aiId]
  if (!msgs) return
  const msg = msgs.find(m => m.id === msgId)
  if (!msg) return
  msg.streaming = false
  msg.fullContent = fullText
  if (msg.typing) return
  msg.typing = true
  msg.content = ''
  let index = 0
  //收到完整文本后，每15ms显示一个字符
  const type = () => {
    if (index < msg.fullContent.length) {
      msg.content = msg.fullContent.slice(0, index + 1)
      index++
      if (document.hidden) { msg.content = msg.fullContent; msg.typing = false; return }
      setTimeout(type, 15)
    } else { msg.typing = false }
  }
  type()
  nextTick(() => scrollAIToBottom())
}

const updateStreamingMsg = (aiId, msgId, content) => {
  const msgs = messagesByAI.value[aiId]
  if (!msgs) return
  const msg = msgs.find(m => m.id === msgId)
  if (msg) { msg.content = content; msg.streaming = false; msg.typing = false }
}

const autoResizeAI = () => {
  if (aiInputRef.value) {
    aiInputRef.value.style.height = '34px'
    aiInputRef.value.style.height = Math.min(aiInputRef.value.scrollHeight, 80) + 'px'
  }
}

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
const autoResize = () => {
  if (inputRef.value) {
    inputRef.value.style.height = '34px'
    inputRef.value.style.height = Math.min(inputRef.value.scrollHeight, 100) + 'px'
  }
}
const scrollToBottom = () => {
  nextTick(() => { if (messagesContainer.value) messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight })
}

//往消息列表追加一条
const appendMsg = (type, content, from, isOwn = false, isAI = false, createdAt = null) => {
  messages.value.push({ id: Date.now() + Math.random(), type, content, from, isOwn, isAI, createdAt: createdAt || new Date().toISOString() })
  scrollToBottom()
}

// 切换AI开关
const toggleAI = async () => {
  if (!currentChat.value?.isFriend) return
  const newVal = !aiEnabled.value
  aiEnabled.value = newVal
  appendMsg('SYSTEM', newVal ? '🤖 AI已重新接入对话' : '🔕 AI已临时关闭')
  //持久化到数据库
  try {
    const token = localStorage.getItem('token')
    await fetch('/api/chat/private/ai-toggle', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token },
      body: JSON.stringify({ targetUserId: currentTargetId.value, enabled: newVal })
    })
  } catch (e) { console.error('AI开关保存失败', e) }
}
const loadAiStatus = async (targetId) => {
  //从数据库查AI开关状态
  try {
    const token = localStorage.getItem('token')
    const res = await fetch(`/api/chat/private/ai-status?targetUserId=${targetId}`, {
      headers: { 'Authorization': 'Bearer ' + token }
    })
    const data = await res.json()
    if (data.code === 200) aiEnabled.value = data.data.aiEnabled
  } catch (e) { console.error('AI状态查询失败', e) }
}

//切换聊天对象
const switchChat = async (chat) => {
  currentTargetId.value = chat.id
  router.push(`/chat/${userId}/UTOU/${chat.id}`)
  hasMore.value = true
  loadingMore.value = false
  messages.value = []
  commonPoints.value = []
  recommendText.value = ''   // ← 加这行：先清空

  if (chat.isFriend) {
    await loadAiStatus(chat.id)
  } else {
    aiEnabled.value = false
  }
  loadHistory(chat.id)
  checkCommonPoints()
  checkRecommend()
}

const loadHistory = async (targetId) => {
  try {
    const token = localStorage.getItem('token')

    // ========== 改这里: 使用Redis缓存接口 ==========
    const res = await fetch(`/api/chat/private/${targetId}/messages/recent`, {
      headers: { 'Authorization': 'Bearer ' + token }
    })

    const data = await res.json()
    if (data.code === 200) {
      //调/messages/recent接口（Redis优先）
      //过滤掉已有的消息，避免重复
      const existingIds = new Set(messages.value.filter(m => !m.pending).map(m => m.id))
      messages.value = messages.value.filter(m => !m.pending)
      const newMsgs = data.data.filter(msg => !existingIds.has(msg.id)).map(msg => ({
        id: msg.id,
        type: msg.type === 1 ? 'AI_SUMMARY' : 'USER',
        content: msg.content,
        from: msg.fromUserId === 'AI' ? 'Symphony' : msg.fromUserId === userId ? username.value : currentChat.value?.username || '对方',
        isOwn: msg.fromUserId === userId,
        isAI: msg.fromUserId === 'AI',
        createdAt: msg.createdAt
      }))

      if (newMsgs.length > 0) {
        messages.value.push(...newMsgs)
        if (newMsgs.some(m => m.type === 'AI_SUMMARY')) summarizing.value = false
        const { scrollTop, scrollHeight, clientHeight } = messagesContainer.value
        if (scrollHeight - scrollTop - clientHeight < 50) scrollToBottom()
      }

      //判断是否还有更多
      if (newMsgs.length < 50) {
        hasMore.value = false
      }
    }
  } catch (e) {
    console.error('加载历史失败', e)
  }
}

//向上滚动加载更多
const loadMoreMessages = async () => {
  //防止重复加载
  if (loadingMore.value || !hasMore.value || messages.value.length === 0) {
    return
  }

  loadingMore.value = true

  try {
    //记录当前滚动高度
    if (messagesContainer.value) {
      scrollHeightBeforeLoad = messagesContainer.value.scrollHeight
    }

    //取列表最顶部那条消息的id作为游标
    const earliestMessageId = messages.value[0]?.id

    if (!earliestMessageId) {
      hasMore.value = false
      return
    }

    const token = localStorage.getItem('token')
    const res = await fetch(
        `/api/chat/private/${currentTargetId.value}/messages/more?beforeMessageId=${earliestMessageId}`,
        { headers: { 'Authorization': 'Bearer ' + token } }
    )

    const data = await res.json()

    if (data.code === 200) {
      const newMessages = data.data

      if (newMessages.length === 0) {
        hasMore.value = false
      } else {
        // 转换消息格式
        const formattedMsgs = newMessages.map(msg => ({
          id: msg.id,
          type: msg.type === 1 ? 'AI_SUMMARY' : 'USER',
          content: msg.content,
          from: msg.fromUserId === 'AI' ? 'Symphony' : msg.fromUserId === userId ? username.value : currentChat.value?.username || '对方',
          isOwn: msg.fromUserId === userId,
          isAI: msg.fromUserId === 'AI',
          createdAt: msg.createdAt
        }))

        //插到列表头部
        messages.value = [...formattedMsgs, ...messages.value]

        //恢复滚动位置
        await nextTick()
        restoreScrollPosition()

        // 少于100条说明到头了
        if (newMessages.length < 100) {
          hasMore.value = false
        }
      }
    }
  } catch (error) {
    console.error('加载更多失败:', error)
  } finally {
    loadingMore.value = false
  }
}

//加载更多后恢复滚动位置
const restoreScrollPosition = () => {
  if (messagesContainer.value) {
    const newScrollHeight = messagesContainer.value.scrollHeight
    //算出新增内容的高度，设置scrollTop防止页面跳动
    const addedHeight = newScrollHeight - scrollHeightBeforeLoad
    //保持用户原来的视野不变
    messagesContainer.value.scrollTop = addedHeight
  }
}

//滚动事件
const handleScroll = () => {
  const container = messagesContainer.value
  if (!container) return

  //滚动到顶部100px内时,触发加载更多
  if (container.scrollTop < 100) {
    loadMoreMessages()
  }
}

//点击整理按钮，先弹确认框
const confirmSummarize = () => {
  if (summarizing.value) return
  summarizeConfirmVisible.value = true
}

//确认后真正执行
const doSummarize = async () => {
  console.log('doSummarize called')
  summarizeConfirmVisible.value = false
  drawerOpen.value = false
  await summarize()
}

//调后端AI整理接口
const summarize = async () => {
  console.log('summarize called, summarizing =', summarizing.value)  // ← 第2步
  if (summarizing.value) return
  summarizing.value = true

  console.log('sending request to:', currentTargetId.value)          // ← 第3步
  try {
    const token = localStorage.getItem('token')
    console.log('token exists:', !!token)                             // ← 第4步
    const res = await fetch('/api/chat/private/ai/summarize', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token },
      body: JSON.stringify({ targetUserId: currentTargetId.value })
    })
    console.log('response status:', res.status)                       // ← 第5步
  } catch(e) {
    console.error('fetch error:', e)
  }
}

const loadPrivateChats = async () => {
  try {
    const token = localStorage.getItem('token')
    const res = await fetch('/api/chat/private/list', { headers: { 'Authorization': 'Bearer ' + token } })
    const data = await res.json()
    if (data.code === 200) {
      privateChats.value = data.data.map(chat => ({
        id: chat.userId, username: chat.username, isFriend: chat.isFriend || false,
        lastMessage: chat.lastMessage || '', unread: chat.unread || 0
      }))
    }
  } catch (e) { console.error('加载私聊列表失败', e) }
}
const sendMessage = async () => {
  const msg = inputMessage.value.trim()
  if (!msg || !currentTargetId.value) return
  //先乐观更新,pending为true则立刻显示
  messages.value.push({ id: Date.now() + Math.random(), type: 'USER', content: msg, from: username.value, isOwn: true, pending: true, createdAt: new Date().toISOString() })
  scrollToBottom()
  inputMessage.value = ''
  if (inputRef.value) inputRef.value.style.height = '34px'
  try {
    const token = localStorage.getItem('token')
    //再调接口
    await fetch('/api/chat/private/send', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token },
      body: JSON.stringify({ targetUserId: currentTargetId.value, message: msg, aiEnabled: aiEnabled.value })
    })
  } catch (e) { console.error('发送失败', e); appendMsg('SYSTEM', '⚠️ 消息发送失败，请重试') }
}

//打开协作文档，建立WebSocket连接
const openCollab = () => {
  collabVisible.value = true
  const roomId = userId < currentTargetId.value ? userId + '_' + currentTargetId.value : currentTargetId.value + '_' + userId
  const wsHost = import.meta.env.DEV ? 'localhost:8080' : window.location.host
  collabWs = new WebSocket(`ws://${wsHost}/ws/collab/${roomId}?userId=${userId}`)
  collabWs.onmessage = (event) => { collabContent.value = event.data }    //收到消息更新内容
  collabWs.onerror = (e) => { console.error('协作文档连接失败', e) }
}
//用户编辑时实时发送
const onCollabInput = () => { if (collabWs && collabWs.readyState === WebSocket.OPEN) collabWs.send(collabContent.value) }
//工具栏插入Markdown语法
const insertMd = (before, after) => {
  const el = collabEditorRef.value
  if (!el) return
  const start = el.selectionStart, end = el.selectionEnd
  const selected = collabContent.value.slice(start, end)
  const replacement = before + (selected || '文字') + after
  collabContent.value = collabContent.value.slice(0, start) + replacement + collabContent.value.slice(end)
  nextTick(() => { el.focus(); el.setSelectionRange(start + before.length, start + before.length + (selected || '文字').length) })
}
//Markdown渲染
const renderMarkdown = (text) => {
  if (!text) return '<p style="color:#bbb">预览区域（开始编写后显示）</p>'
  return text
      .replace(/^### (.+)$/gm, '<h3>$1</h3>')
      .replace(/^## (.+)$/gm, '<h2>$1</h2>')
      .replace(/^# (.+)$/gm, '<h1>$1</h1>')
      .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
      .replace(/\*(.+?)\*/g, '<em>$1</em>')
      .replace(/^> (.+)$/gm, '<blockquote>$1</blockquote>')
      .replace(/`(.+?)`/g, '<code>$1</code>')
      .replace(/^- (.+)$/gm, '<li>$1</li>')
      .replace(/\n/g, '<br>')
}
//关闭协作文档，断开WebSocket
const closeCollab = () => { collabVisible.value = false; if (collabWs) { collabWs.close(); collabWs = null } }
//手动保存文档到数据库
const saveCollab = async () => {
  if (!collabContent.value.trim()) return
  const token = localStorage.getItem('token')
  const roomId = userId < currentTargetId.value ? userId + '_' + currentTargetId.value : currentTargetId.value + '_' + userId
  await fetch('/api/collab/save', { method: 'POST', headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token }, body: JSON.stringify({ roomId, content: collabContent.value }) })
  window.$toast.info('保存成功！')
}
const openGallery = () => {
  const roomId = userId < currentTargetId.value ? userId + '_' + currentTargetId.value : currentTargetId.value + '_' + userId
  router.push(`/collab/gallery/${roomId}`)
}

// 查两人共同薄弱知识点
const checkCommonPoints = async () => {
  if (!currentTargetId.value) return  // ← 加这行
  const token = localStorage.getItem('token')
  const res = await fetch(`/api/mistakes/common-points?userId1=${userId}&userId2=${currentTargetId.value}`, {
    headers: { 'Authorization': 'Bearer ' + token }
  })
  const data = await res.json()
  if (data.code === 200 && data.data.length > 0) commonPoints.value = data.data
}

//检查今日学习建议
const checkRecommend = async () => {
  //今天已关闭过就不再弹
  const closedDate = localStorage.getItem('recommendClosedDate')
  if (closedDate === new Date().toDateString()) return
  const token = localStorage.getItem('token')
  const res = await fetch(`/api/report/recommend?targetUserId=${currentTargetId.value}`, { headers: { 'Authorization': 'Bearer ' + token } })
  const data = await res.json()
  if (data.code === 200 && data.data) {
    //拉取建议内容
    recommendText.value = data.data.content
    recommendId.value = data.data.id
  }
}

//提交错题到错题本
const submitMistake = async () => {
  const token = localStorage.getItem('token')
  await fetch('/api/mistakes/add', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token },
    body: JSON.stringify({
      question: mistakeForm.value.question,
      correctAnswer: mistakeForm.value.correctAnswer,
      wrongReason: mistakeForm.value.wrongReason
    })
  })
  mistakeVisible.value = false
}

//轮询检查AI检测到的错题建议
const checkMistakeSuggest = async () => {
  const token = localStorage.getItem('token')
  const res = await fetch(`/api/mistakes/suggest?targetUserId=${currentTargetId.value}`, { headers: { 'Authorization': 'Bearer ' + token } })
  //有建议就填入表单，弹出错题弹窗
  const data = await res.json()
  if (data.code === 200 && data.data) {
    const parsed = JSON.parse(data.data)
    mistakeForm.value.question = parsed.question || ''
    mistakeForm.value.correctAnswer = parsed.correctAnswer || ''
    mistakeForm.value.wrongReason = parsed.wrongReason || ''
    mistakeForm.value.knowledgePoints = parsed.knowledgePoints || ''
    mistakeVisible.value = true
  }
}

//页面加载流程:检查推荐→验证登录→拉用户名→加载好友列表→查AI状态→加载消息→启动轮询(每5秒刷新消息+检查错题建议)→查共同薄弱点→加载AI列表→注册滚动监听
onMounted(async () => {
  checkRecommend()
  const token = localStorage.getItem('token')
  if (!token) { router.push('/login'); return }
  const res = await fetch('/api/auth/profile', { headers: { 'Authorization': 'Bearer ' + token } })
  const data = await res.json()
  if (data.code === 200) username.value = data.data.username
  await loadPrivateChats()
  if (currentTargetId.value) {
    const chat = privateChats.value.find(c => c.id === currentTargetId.value)
    if (chat?.isFriend) {
      await loadAiStatus(currentTargetId.value)
    } else if (chat) {
      aiEnabled.value = false
    }
    await loadHistory(currentTargetId.value)
  }
  pollTimer = setInterval(() => {
    if (currentTargetId.value) { loadHistory(currentTargetId.value); checkMistakeSuggest() }
  }, 5000)
  await checkCommonPoints()
  await loadMyAIs()

  //注册滚动监听，滚到顶部触发加载更多
  if (messagesContainer.value) {
    messagesContainer.value.addEventListener('scroll', handleScroll)
  }
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)    //停止轮询
  for (const aiId in abortControllers.value) abortControllers.value[aiId].abort()    //中断所有AI请求
  //移除滚动监听
  if (messagesContainer.value) {
    messagesContainer.value.removeEventListener('scroll', handleScroll)
  }
})
</script>

<style>
:root {
  --primary: #C5A059;
  --primary-dark: #002D82;
  --secondary: #0039a6;
  --bg: #F0F2F5;
  --border: #E8ECEF;
  --text: #2D3436;
  --text-light: #636E72;
}
</style>

<style scoped>
* { margin: 0; padding: 0; box-sizing: border-box; }

.chat-page {
  height: 100vh; background: var(--bg);
  display: flex; align-items: center; justify-content: center;
  padding: 20px; transition: all 0.4s ease;
  box-shadow:
      inset 0 0 0 4px #0847b0,
      inset 0 0 0 8px #2f6fd3,
      inset 0 0 0 12px #5d84d6;
}
.chat-container {
  width: 100%; max-width: 1100px; height: calc(100vh - 40px); max-height: 900px;
  background: white; border-radius: 24px;
  display: grid; grid-template-columns: 280px 1fr;
  box-shadow: 0 15px 40px rgba(0,0,0,0.08); overflow: hidden;
  transition: grid-template-columns 0.32s cubic-bezier(0.4,0,0.2,1), max-width 0.32s cubic-bezier(0.4,0,0.2,1);
}
.chat-container.with-collab { max-width: 1500px; grid-template-columns: 280px 1fr 420px; }
.chat-container.with-drawer { max-width: 1400px; grid-template-columns: 280px 1fr 300px; }

.sidebar { background: #F8F9FB; border-right: 1px solid var(--border); display: flex; flex-direction: column; }
.sidebar-header { padding: 20px; text-align: center; border-bottom: 1px solid rgba(0,0,0,0.03); }
.logo-text { font-size: 18px; font-weight: 900; color: #cd9d25; }
.online-count { font-size: 11px; color: var(--text-light); margin-top: 4px; }
.chat-types { padding: 10px; display: flex; flex-direction: column; gap: 6px; border-bottom: 1px solid var(--border); }
.type-tab { padding: 12px 15px; background: white; border-radius: 12px; font-size: 13px; font-weight: 700; color: var(--text); cursor: pointer; transition: all 0.3s; display: flex; align-items: center; gap: 10px; }
.type-tab:hover { background: #F0FAF9; color: var(--secondary);}
.type-tab.active { background: #1772dc; color: white; box-shadow: 0 2px 8px rgba(78,205,196,0.3); }
.channel-list { flex: 1; overflow-y: auto; padding: 10px; }
.section-title { font-size: 11px; font-weight: 800; color: var(--text-light); text-transform: uppercase; letter-spacing: 0.5px; padding: 10px 15px 8px; }
.channel-item { display: flex; align-items: center; padding: 12px; margin-bottom: 6px; background: white; border-radius: 12px; cursor: pointer; transition: all 0.3s; border: 2px solid transparent; position: relative; }
.channel-item:hover { background: #F0FAF9; border-color: var(--secondary); transform: translateX(2px); }
.channel-item.active { background: #e8f0ff; color: #0039a6; border-color: #0039a6; }
.channel-avatar { width: 36px; height: 36px; border-radius: 10px; color: white; display: flex; align-items: center; justify-content: center; font-size: 16px; font-weight: bold; flex-shrink: 0; margin-right: 12px; }
.channel-info { flex: 1; min-width: 0; }
.channel-name { font-size: 13px; font-weight: 700; color: var(--text); margin-bottom: 2px; display: flex; align-items: center; gap: 6px; }
.channel-preview { font-size: 11px; color: var(--text-light); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.channel-item.active .channel-name { color: #0039a6; }
.channel-item.active .channel-preview { color: rgba(0, 57, 166, 0.7); }
.friend-badge { font-size: 9px; background: #FFF3CD; color: #856404; padding: 2px 5px; border-radius: 4px; }
.unread-dot { width: 8px; height: 8px; border-radius: 50%; background: var(--primary); flex-shrink: 0; }
.empty-tip { padding: 20px; text-align: center; color: var(--text-light); font-size: 12px; display: flex; flex-direction: column; gap: 8px; align-items: center; }

.chat-main { display: flex; flex-direction: column; height: 100%; overflow: hidden; }
.chat-header { padding: 15px 25px; border-bottom: 1px solid var(--border); display: flex; align-items: center; justify-content: space-between; flex-shrink: 0; }
.header-left { flex: 1; }
.header-title { font-weight: 800; color: var(--text); font-size: 16px; display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.header-subtitle { font-size: 12px; color: var(--text-light); }
.header-actions { display: flex; gap: 10px; }
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
.msg-info { font-size: 11px; color: var(--text-light); display: flex; align-items: center; gap: 6px; }
.ai-tag { background: #0039a6; color: white; padding: 2px 6px; border-radius: 4px; font-size: 9px; font-weight: 700; }
.msg-bubble { padding: 10px 15px; font-size: 14.5px; line-height: 1.5; word-break: break-word; white-space: pre-wrap; transition: all 0.2s ease; }
.message.other .msg-bubble { background: rgba(236, 230, 218, 0.34); color: var(--text); border: 1px solid rgba(0, 0, 0, 0.06); border-radius: 4px 16px 16px 16px; }
.message.own .msg-bubble { background: #2a64c3; color: #FFFFFF; border-radius: 16px 4px 16px 16px; box-shadow: 0 4px 12px rgba(0, 57, 166, 0.15); }
.system-bubble { background: #E9ECEF; color: #495057; font-size: 11px; padding: 6px 14px; border-radius: 20px; font-weight: 600; }
.msg-time { font-size: 10px; color: var(--text-light); }
.empty-state { text-align: center; padding: 60px 20px; color: var(--text-light); }
.empty-icon { font-size: 60px; margin-bottom: 15px; opacity: 0.3; }
.empty-text { font-size: 14px; }
.input-container { padding: 12px 25px 18px; background: white; flex-shrink: 0; }
.input-wrapper { display: flex; gap: 10px; align-items: center; background: #F1F3F6; padding: 5px 12px; border-radius: 25px; border: 1.5px solid transparent; transition: 0.3s; }
.input-wrapper:focus-within { background: white; border-color: var(--secondary); }
.input-field { flex: 1; min-height: 34px; max-height: 100px; background: transparent; border: none; outline: none; font-size: 14px; resize: none; line-height: 1.5; color: var(--text); font-family: inherit; }
.send-btn { width: 32px; height: 32px; border-radius: 50%; background: #0039a6; color: white; border: none; cursor: pointer; transition: 0.2s; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.send-btn:hover { background: #002D82; transform: scale(1.05); }
.common-points-bar { background: linear-gradient(to bottom, #2f6fd3 0%, #3f7ce0 50%, #5a93ee 100%); border-top: 1px solid #ae9ce1; border-bottom: 1px solid #dac090; padding: 10px 20px; font-size: 13px; font-weight: 600; color: white; display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.close-bar { margin-left: auto; cursor: pointer; opacity: 0.6; }
.recommend-bubble { display: flex; align-items: flex-start; gap: 12px; background: linear-gradient(135deg, #EBF5FB, #D6EAF8); border: 2px solid #AED6F1; border-radius: 18px; padding: 14px 18px; }
.recommend-bubble-icon { font-size: 24px; flex-shrink: 0; }
.recommend-bubble-content { flex: 1; }
.recommend-bubble-title { font-size: 11px; font-weight: 800; color: #1A5276; margin-bottom: 5px; text-transform: uppercase; letter-spacing: 1px; }
.recommend-bubble-text { font-size: 14px; color: #1A5276; line-height: 1.6; }

/* ===== 浮动AI小球 ===== */
.ai-ball-wrap { position: fixed; bottom: 32px; right: 32px; z-index: 999; display: flex; flex-direction: column; align-items: flex-end; gap: 16px; }
.ai-ball { width: 58px; height: 58px; border-radius: 50%; background: linear-gradient(135deg, #0039a6 0%, #004cd5 100%); cursor: pointer; display: flex; align-items: center; justify-content: center; box-shadow: 0 8px 28px rgba(0,57,166,0.55); transition: all 0.35s cubic-bezier(0.34, 1.56, 0.64, 1); position: relative; user-select: none; }
.ai-ball:hover { transform: scale(1.1); box-shadow: 0 12px 36px rgba(0,57,166,0.7); }
.ai-ball.ball-open { background: linear-gradient(135deg, #636E72, #2D3436); box-shadow: 0 8px 24px rgba(0,0,0,0.3); }
.ball-inner { z-index: 2; }
.ball-emoji { font-size: 26px; display: block; transition: all 0.3s; }
.ball-ring { position: absolute; inset: -6px; border-radius: 50%; border: 2px solid rgba(0,76,213,0.45); animation: ballRing 2.4s ease-out infinite; }
.ring2 { animation-delay: 1.2s; }
.ball-open .ball-ring { display: none; }
@keyframes ballRing { 0% { transform: scale(1); opacity: 0.7; } 100% { transform: scale(1.6); opacity: 0; } }
.ball-pulse .ball-inner { animation: ballBounce 3s ease-in-out infinite; }
@keyframes ballBounce { 0%, 100% { transform: translateY(0); } 50% { transform: translateY(-4px); } }
.ai-float-panel { width: 360px; height: 520px; background: white; border-radius: 20px; box-shadow: 0 20px 60px rgba(0,0,0,0.22), 0 0 0 1.5px rgba(0,57,166,0.18); display: flex; flex-direction: column; overflow: hidden; }
.ai-panel-header { padding: 16px 18px; background: linear-gradient(160deg, #0039a6 0%, #004cd5 60%, #1a1a2e 100%); display: flex; align-items: center; justify-content: space-between; flex-shrink: 0; }
.ai-panel-title { display: flex; align-items: center; gap: 12px; }
.ai-panel-avatar { width: 36px; height: 36px; border-radius: 12px; background: rgba(255,255,255,0.2); display: flex; align-items: center; justify-content: center; font-size: 18px; }
.ai-panel-name { font-size: 15px; font-weight: 800; color: white; }
.ai-panel-sub { font-size: 11px; color: rgba(255,255,255,0.65); margin-top: 1px; }
.ai-panel-acts { display: flex; align-items: center; gap: 8px; }
.ai-panel-close { width: 28px; height: 28px; border-radius: 50%; background: rgba(255,255,255,0.15); border: none; color: white; cursor: pointer; font-size: 12px; transition: all 0.2s; display: flex; align-items: center; justify-content: center; }
.ai-panel-close:hover { background: rgba(255,255,255,0.3); }
.ai-switcher { position: relative; }
.ai-switch-btn { width: 28px; height: 28px; border-radius: 50%; background: rgba(255,255,255,0.15); border: none; color: white; cursor: pointer; font-size: 11px; transition: all 0.2s; display: flex; align-items: center; justify-content: center; }
.ai-switch-btn:hover { background: rgba(255,255,255,0.3); }
.ai-switch-menu { position: absolute; right: 0; top: 36px; background: white; border-radius: 12px; box-shadow: 0 8px 24px rgba(0,0,0,0.15); overflow: hidden; z-index: 10; min-width: 160px; }
.ai-switch-item { display: flex; align-items: center; gap: 10px; padding: 11px 14px; cursor: pointer; transition: background 0.2s; font-size: 13px; color: var(--text); }
.ai-switch-item:hover { background: #f0f4ff; }
.ai-switch-item.active { background: #e8eeff; color: #0039a6; font-weight: 700; }
.asi-emoji { font-size: 16px; }
.ai-panel-msgs { flex: 1; overflow-y: auto; padding: 14px 16px; display: flex; flex-direction: column; gap: 12px; min-height: 0; }
.ai-panel-msgs::-webkit-scrollbar { width: 3px; }
.ai-panel-msgs::-webkit-scrollbar-thumb { background: rgba(0,0,0,0.12); border-radius: 10px; }
.ai-empty { display: flex; flex-direction: column; align-items: center; justify-content: center; flex: 1; opacity: 0.5; padding: 40px 0; }
.ai-msg { display: flex; align-items: flex-start; gap: 8px; max-width: 90%; }
.ai-msg-own { align-self: flex-end; flex-direction: row-reverse; }
.ai-msg-other { align-self: flex-start; }
.ai-msg-avatar { width: 28px; height: 28px; border-radius: 8px; color: white; display: flex; align-items: center; justify-content: center; font-size: 13px; font-weight: bold; flex-shrink: 0; }
.ai-msg-body { display: flex; flex-direction: column; gap: 3px; }
.ai-msg-own .ai-msg-body { align-items: flex-end; }
.ai-msg-bubble { padding: 9px 13px; border-radius: 4px 14px 14px 14px; background: #F1F3F6; font-size: 13.5px; line-height: 1.55; color: var(--text); word-break: break-word; white-space: pre-wrap; max-width: 260px; }
.ai-msg-own .ai-msg-bubble { background: linear-gradient(160deg, #0039a6 0%, #004cd5 60%, #1a1a2e 100%); color: white; border-radius: 14px 4px 14px 14px; }
.ai-msg-bubble.streaming { opacity: 0.7; }
.ai-msg-time { font-size: 10px; color: var(--text-light); }
.ai-panel-input { padding: 10px 14px 14px; border-top: 1px solid var(--border); display: flex; align-items: center; gap: 8px; background: white; flex-shrink: 0; }
.ai-input-field { flex: 1; min-height: 34px; max-height: 80px; background: #F1F3F6; border: 1.5px solid transparent; outline: none; border-radius: 18px; font-size: 13px; resize: none; line-height: 1.5; color: var(--text); font-family: inherit; padding: 8px 14px; transition: all 0.2s; }
.ai-input-field:focus { background: white; border-color: #004cd5; }
.ai-send-btn { width: 34px; height: 34px; border-radius: 50%; background: linear-gradient(160deg, #0039a6 0%, #004cd5 60%, #1a1a2e 100%); color: white; border: none; cursor: pointer; transition: all 0.2s; display: flex; align-items: center; justify-content: center; flex-shrink: 0; font-size: 13px; }
.ai-send-btn:hover:not(:disabled) { transform: scale(1.08); box-shadow: 0 4px 14px rgba(0,57,166,0.45); }
.ai-send-btn:disabled { opacity: 0.55; cursor: not-allowed; }
.ai-panel-enter-active { animation: panelPop 0.35s cubic-bezier(0.34, 1.56, 0.64, 1); }
.ai-panel-leave-active { animation: panelPop 0.2s ease reverse; }
@keyframes panelPop { from { opacity: 0; transform: scale(0.85) translateY(20px); transform-origin: bottom right; } to { opacity: 1; transform: scale(1) translateY(0); transform-origin: bottom right; } }

/* ===== 弹窗通用 ===== */
.modal-overlay { position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal-enter-active { animation: modalIn 0.25s cubic-bezier(0.34,1.3,0.64,1); }
.modal-leave-active { animation: modalIn 0.18s ease reverse; }
@keyframes modalIn { from { opacity: 0; transform: scale(0.92); } to { opacity: 1; transform: scale(1); } }

/* Bug 3: 确认弹窗样式 */
.confirm-modal { background: white; border-radius: 20px; padding: 32px 28px; width: 90%; max-width: 400px; display: flex; flex-direction: column; align-items: center; gap: 16px; box-shadow: 0 20px 60px rgba(0,0,0,0.15), 0 0 0 1px rgba(0,57,166,0.08); text-align: center; }
.confirm-icon { font-size: 48px; line-height: 1; }
.confirm-title { font-size: 18px; font-weight: 900; color: var(--text); }
.confirm-desc { font-size: 13.5px; color: var(--text-light); line-height: 1.7; }
.confirm-footer { display: flex; gap: 10px; width: 100%; margin-top: 4px; }

/* Bug 7: 错题弹窗 */
.mistake-modal { background: white; border-radius: 20px; padding: 28px; width: 90%; max-width: 500px; display: flex; flex-direction: column; gap: 20px; box-shadow: 0 20px 60px rgba(0,0,0,0.15), 0 0 0 1px rgba(0,57,166,0.08); }
.mistake-header { display: flex; align-items: center; justify-content: space-between; }
.mistake-header h3 { font-size: 16px; font-weight: 800; color: var(--text); }
.mistake-header button { width: 32px; height: 32px; border: none; background: var(--bg); border-radius: 50%; cursor: pointer; }
.mistake-body { display: flex; flex-direction: column; gap: 14px; }
.mistake-field { display: flex; flex-direction: column; gap: 6px; }
.mistake-field label { font-size: 12px; font-weight: 700; color: var(--text-light); }
.mistake-field textarea, .mistake-field input { border: 1.5px solid var(--border); border-radius: 10px; padding: 10px 14px; font-size: 14px; color: var(--text); font-family: inherit; resize: none; outline: none; transition: border-color 0.2s; }
.mistake-field textarea:focus, .mistake-field input:focus { border-color: var(--secondary); }
.mistake-footer { display: flex; gap: 10px; }
.btn-confirm { flex: 1; background: var(--secondary); color: white; border: none; padding: 12px; border-radius: 12px; font-size: 14px; font-weight: 700; cursor: pointer; }
.btn-skip { padding: 12px 20px; border: 2px solid var(--border); background: none; border-radius: 12px; font-size: 14px; font-weight: 700; color: var(--text-light); cursor: pointer; }

/* ===== Header ===== */
.ai-status-badge { display: flex; align-items: center; gap: 5px; padding: 5px 12px; border-radius: 20px; font-size: 11px; font-weight: 800; letter-spacing: 0.5px; background: #f0f0f0; color: #aaa; border: 1.5px solid #e0e0e0; transition: all 0.3s; user-select: none; }
.ai-status-badge.active { background: #e8f0ff; color: #0039a6; border-color: #b8ccff; }
.gear-btn { width: 38px; height: 38px; border-radius: 12px; border: 2px solid var(--border); background: white; color: var(--text); cursor: pointer; font-size: 15px; display: flex; align-items: center; justify-content: center; transition: all 0.3s; }
.gear-btn:hover { border-color: #0039a6; color: #0039a6; }
.gear-btn.gear-active { background: #0039a6; border-color: #0039a6; color: white; }

/* ===== 功能抽屉 ===== */
.function-drawer { width: 300px; border-left: 1px solid var(--border); background: #f8f9fb; display: flex; flex-direction: column; padding: 20px 16px; gap: 20px; overflow-y: auto; flex-shrink: 0; height: 100%; }
.drawer-enter-active { animation: drawerSlide 0.28s cubic-bezier(0.34,1.4,0.64,1); }
.drawer-leave-active { animation: drawerSlide 0.2s ease reverse; }
@keyframes drawerSlide { from { opacity: 0; transform: translateX(30px); } to { opacity: 1; transform: translateX(0); } }
.drawer-section { display: flex; flex-direction: column; gap: 8px; }
.drawer-section-label { font-size: 10px; font-weight: 900; color: var(--text-light); text-transform: uppercase; letter-spacing: 1.5px; padding: 0 4px; }
.drawer-chat-info { display: flex; align-items: center; gap: 12px; background: white; border-radius: 14px; padding: 12px 14px; border: 1.5px solid var(--border); }
.dci-avatar { width: 38px; height: 38px; border-radius: 10px; color: white; display: flex; align-items: center; justify-content: center; font-size: 16px; font-weight: 900; flex-shrink: 0; }
.dci-name { font-size: 14px; font-weight: 800; color: var(--text); }
.dci-tag { font-size: 10px; font-weight: 700; color: #0039a6; background: #e8f0ff; padding: 2px 6px; border-radius: 6px; margin-top: 3px; display: inline-block; }
.drawer-toggle-row { display: flex; align-items: center; justify-content: space-between; background: white; border-radius: 14px; padding: 12px 14px; border: 1.5px solid var(--border); cursor: pointer; transition: all 0.2s; }
.drawer-toggle-row:hover { border-color: #0039a6; }
.dtr-left { display: flex; align-items: center; gap: 12px; }
.dtr-icon { width: 36px; height: 36px; border-radius: 10px; background: #f0f0f0; display: flex; align-items: center; justify-content: center; font-size: 15px; color: #aaa; transition: all 0.3s; flex-shrink: 0; }
.dtr-icon.dtr-on { background: #e8f0ff; color: #0039a6; }
.dtr-title { font-size: 13px; font-weight: 700; color: var(--text); }
.dtr-sub { font-size: 11px; color: var(--text-light); margin-top: 2px; }
.toggle-switch { width: 40px; height: 22px; border-radius: 11px; background: #ddd; position: relative; transition: background 0.3s; flex-shrink: 0; }
.toggle-switch.on { background: #0039a6; }
.toggle-thumb { position: absolute; top: 3px; left: 3px; width: 16px; height: 16px; border-radius: 50%; background: white; transition: transform 0.3s; box-shadow: 0 1px 4px rgba(0,0,0,0.2); }
.toggle-switch.on .toggle-thumb { transform: translateX(18px); }
.drawer-action-btn { display: flex; align-items: center; gap: 12px; background: white; border: 1.5px solid var(--border); border-radius: 14px; padding: 12px 14px; width: 100%; cursor: pointer; transition: all 0.2s; text-align: left; }
.drawer-action-btn:hover:not(:disabled) { border-color: #0039a6; background: #f4f7ff; }
.drawer-action-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.drawer-action-btn > i:first-child { width: 36px; height: 36px; border-radius: 10px; background: #f0f0f0; display: flex; align-items: center; justify-content: center; font-size: 15px; color: #0039a6; flex-shrink: 0; }
.dab-title { font-size: 13px; font-weight: 700; color: var(--text); }
.dab-sub { font-size: 11px; color: var(--text-light); margin-top: 2px; }
.dab-arrow { color: #ccc; font-size: 11px; margin-left: auto; }
.drawer-disabled { font-size: 12px; color: #bbb; text-align: center; padding: 10px; background: white; border-radius: 12px; border: 1.5px dashed #e0e0e0; }
.drawer-back-btn { width: 100%; padding: 12px; border-radius: 12px; border: 1.5px solid var(--border); background: white; font-size: 13px; font-weight: 700; color: var(--text-light); cursor: pointer; transition: all 0.2s; display: flex; align-items: center; justify-content: center; gap: 8px; }
.drawer-back-btn:hover { border-color: var(--primary); color: var(--primary); }

/* ===== 协作文档弹窗 ===== */
.collab-modal-overlay { position: fixed; inset: 0; z-index: 800; background: rgba(10,20,50,0.45); backdrop-filter: blur(6px); display: flex; align-items: center; justify-content: center; padding: 24px; }
.collab-modal-enter-active { animation: collabIn 0.32s cubic-bezier(0.34,1.3,0.64,1); }
.collab-modal-leave-active { animation: collabIn 0.2s ease reverse; }
@keyframes collabIn { from { opacity: 0; transform: scale(0.94) translateY(20px); } to { opacity: 1; transform: scale(1) translateY(0); } }
.collab-modal { width: 100%; max-width: 860px; height: 82vh; max-height: 760px; background: white; border-radius: 24px; display: flex; flex-direction: column; box-shadow: 0 32px 80px rgba(0,30,100,0.18), 0 0 0 1px rgba(0,57,166,0.08); overflow: hidden; }
.collab-modal-header { padding: 18px 22px; border-bottom: 1px solid #eef0f5; display: flex; align-items: center; justify-content: space-between; background: white; flex-shrink: 0; }
.collab-modal-title { display: flex; align-items: center; gap: 12px; }
.collab-title-icon { width: 40px; height: 40px; border-radius: 12px; background: linear-gradient(135deg, #0039a6, #004cd5); color: white; display: flex; align-items: center; justify-content: center; font-size: 16px; flex-shrink: 0; }
.collab-title-text { font-size: 16px; font-weight: 800; color: #1a1a2e; }
.collab-title-sub { font-size: 11px; color: #8a90a0; margin-top: 2px; }
.collab-header-right { display: flex; align-items: center; gap: 10px; }
.collab-tab-switch { display: flex; background: #f0f2f7; border-radius: 10px; padding: 3px; gap: 2px; }
.ctab { padding: 6px 14px; border: none; background: transparent; border-radius: 8px; font-size: 12px; font-weight: 700; color: #8a90a0; cursor: pointer; transition: all 0.2s; display: flex; align-items: center; gap: 5px; }
.ctab.active { background: white; color: #0039a6; box-shadow: 0 1px 6px rgba(0,0,0,0.08); }
.collab-icon-btn { width: 34px; height: 34px; border: 1.5px solid #e8ecf2; border-radius: 10px; background: white; color: #8a90a0; cursor: pointer; font-size: 13px; display: flex; align-items: center; justify-content: center; transition: all 0.2s; }
.collab-icon-btn:hover { border-color: #0039a6; color: #0039a6; }
.collab-save-pill { padding: 7px 16px; border-radius: 10px; border: none; background: linear-gradient(135deg, #0039a6, #004cd5); color: white; font-size: 12px; font-weight: 700; cursor: pointer; display: flex; align-items: center; gap: 6px; transition: all 0.2s; box-shadow: 0 3px 10px rgba(0,57,166,0.3); }
.collab-save-pill:hover { transform: translateY(-1px); box-shadow: 0 5px 14px rgba(0,57,166,0.4); }
.collab-save-pill.saving { opacity: 0.75; cursor: not-allowed; transform: none; }
.collab-close-x { width: 34px; height: 34px; border: 1.5px solid #e8ecf2; border-radius: 10px; background: white; color: #8a90a0; cursor: pointer; font-size: 14px; display: flex; align-items: center; justify-content: center; transition: all 0.2s; }
.collab-close-x:hover { border-color: #ff4757; color: #ff4757; background: #fff5f5; }
.collab-toolbar { padding: 8px 22px; border-bottom: 1px solid #eef0f5; display: flex; align-items: center; gap: 4px; background: #fafbfd; flex-shrink: 0; }
.ctool { width: 30px; height: 30px; border: none; border-radius: 8px; background: transparent; color: #5a6070; cursor: pointer; font-size: 13px; font-weight: 700; display: flex; align-items: center; justify-content: center; transition: all 0.15s; }
.ctool:hover { background: #e8f0ff; color: #0039a6; }
.ctool-divider { width: 1px; height: 18px; background: #e0e4ec; margin: 0 6px; }
.collab-wordcount { font-size: 11px; color: #aab; margin-left: auto; }
.collab-body { flex: 1; min-height: 0; position: relative; }
.collab-textarea { width: 100%; height: 100%; padding: 24px 28px; border: none; outline: none; resize: none; font-size: 15px; line-height: 1.8; color: #2d3142; font-family: 'SF Mono', 'Fira Code', 'Consolas', monospace; background: white; box-sizing: border-box; }
.collab-textarea::placeholder { color: #c0c4d0; line-height: 2; }
.collab-preview { width: 100%; height: 100%; padding: 24px 32px; overflow-y: auto; font-size: 15px; line-height: 1.9; color: #2d3142; box-sizing: border-box; }
.collab-preview h1 { font-size: 22px; font-weight: 900; margin: 16px 0 8px; color: #1a1a2e; }
.collab-preview h2 { font-size: 18px; font-weight: 800; margin: 14px 0 6px; color: #1a1a2e; }
.collab-preview h3 { font-size: 15px; font-weight: 700; margin: 12px 0 5px; }
.collab-preview strong { color: #0039a6; }
.collab-preview em { color: #636e8a; }
.collab-preview blockquote { border-left: 3px solid #0039a6; padding-left: 14px; color: #636e8a; margin: 10px 0; }
.collab-preview code { background: #f0f2f8; padding: 2px 7px; border-radius: 5px; font-size: 13px; color: #0039a6; }
.collab-preview li { margin: 4px 0 4px 20px; }
.collab-modal-footer { padding: 10px 22px; border-top: 1px solid #eef0f5; display: flex; align-items: center; justify-content: space-between; background: #fafbfd; flex-shrink: 0; }
.collab-status { display: flex; align-items: center; gap: 7px; font-size: 12px; color: #8a90a0; font-weight: 600; }
.collab-dot { width: 7px; height: 7px; border-radius: 50%; background: #ddd; }
.collab-dot.active { background: #2ed573; box-shadow: 0 0 6px rgba(46,213,115,0.6); }
.collab-hint { font-size: 11px; color: #bbc; display: flex; align-items: center; gap: 5px; }
/* ========== 加载指示器样式 ========== */
.load-indicator {
  padding: 12px 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: var(--text-light);
}

.loading-spinner {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #0039a6;
  font-weight: 600;
}

.spinner {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(0, 57, 166, 0.2);
  border-top-color: #0039a6;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.no-more-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #999;
  font-size: 11px;
  font-weight: 600;
  padding: 8px 16px;
  background: rgba(0, 0, 0, 0.02);
  border-radius: 20px;
}

.no-more-hint i {
  color: #4CAF50;
  font-size: 10px;
}
@media (max-width: 800px) {
  .chat-container { grid-template-columns: 1fr; height: 100vh; border-radius: 0; }
  .sidebar { display: none; }
  .chat-page { padding: 0; }
  .ai-float-panel { width: calc(100vw - 40px); }
  .function-drawer { position: fixed; right: 0; top: 0; height: 100vh; z-index: 500; box-shadow: -8px 0 30px rgba(0,0,0,0.1); }
  .collab-modal { width: 100vw; height: 100vh; border-radius: 0; }
}
</style>