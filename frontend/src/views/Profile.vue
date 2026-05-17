<template>
  <div class="ds-viewport profile-theme">
    <nav class="ds-nav">
      <div class="nav-left">
        <div class="ds-logo" @click="$router.push('/home')">SYMPHONY</div>
        <div class="breadcrumb">/ ACCOUNT / 身份档案</div>
      </div>
      <div class="nav-right" @click="$router.push('/home')">
        <span class="ds-menu-item"><i class="fas fa-arrow-left"></i> RETURN</span>
      </div>
    </nav>

    <div class="ds-main-layout">
      <aside class="profile-card ds-id-badge">
        <div class="badge-header">IDENTITY</div>
        <div class="badge-body">
          <div class="ds-avatar-wrapper">
            <div class="ds-avatar">{{ userInitial }}</div>
            <div class="avatar-ring"></div>
          </div>
          <div class="profile-name">{{ accountData.username }}</div>
          <div class="profile-email">{{ accountData.email || 'NO EMAIL PROVIDED' }}</div>
        </div>

        <div class="ds-stats-grid">
          <div class="ds-stat">
            <div class="stat-v">{{ accountData.mistakeCount || 0 }}</div>
            <div class="stat-l">ARCHIVED</div>
          </div>
          <div class="ds-stat">
            <div class="stat-v">{{ accountData.aiCount || 0 }}</div>
            <div class="stat-l">AI SESSIONS</div>
          </div>
          <div class="ds-stat">
            <div class="stat-v">{{ registrationDays }}</div>
            <div class="stat-l">DAYS</div>
          </div>
        </div>

        <div class="ds-badge-actions">
          <button class="ds-btn-outline" @click="exportUserData">
            <i class="fas fa-download"></i> 导出信息
          </button>
          <button class="ds-btn-danger" @click="handleLogout">
            <i class="fas fa-sign-out-alt"></i> 登出
          </button>
        </div>
      </aside>

      <main class="ds-content-area">
        <section class="content-section ds-form-panel">
          <header class="panel-header">
            <h3 class="panel-title">
              <span class="title-index">01</span>
              基础简介
            </h3>
            <p class="panel-desc">管理你的基础识别信息</p>
          </header>

          <form @submit.prevent="saveProfile" class="ds-form">
            <div class="ds-form-row">
              <div class="ds-input-group">
                <label class="ds-label">USERNAME</label>
                <input type="text" class="ds-input" v-model="accountData.username" disabled>
              </div>
              <div class="ds-input-group">
                <label class="ds-label">NICKNAME</label>
                <input type="text" class="ds-input" v-model="accountData.nickname" placeholder="Your display name">
              </div>
            </div>

            <div class="ds-form-row">
              <div class="ds-input-group">
                <label class="ds-label">EMAIL ADDRESS</label>
                <input type="email" class="ds-input" v-model="accountData.email" placeholder="mail@example.com">
              </div>
              <div class="ds-input-group">
                <label class="ds-label">PHONE NUMBER</label>
                <input type="tel" class="ds-input" v-model="accountData.phone" placeholder="+86 ...">
              </div>
            </div>

            <div class="ds-input-group">
              <label class="ds-label">PERSONAL BIO</label>
              <textarea class="ds-input ds-textarea" rows="2" v-model="accountData.bio" placeholder="Describe your learning journey..."></textarea>
            </div>

            <div class="form-actions">
              <button type="submit" class="ds-btn-solid">保存更改 →</button>
            </div>
          </form>
        </section>

        <section class="content-section ds-form-panel">
          <header class="panel-header">
            <h3 class="panel-title">
              <span class="title-index">02</span>
              系统设置
            </h3>
            <p class="panel-desc">定制你的环境与 AI 交互行为</p>
          </header>

          <div class="ds-switches-list">
            <div class="ds-switch-item">
              <div class="switch-text">
                <div class="s-label">邮件提醒</div>
                <div class="s-desc">接收重要活动和更新的邮件通知</div>
              </div>
              <label class="ds-toggle">
                <input type="checkbox" v-model="accountData.emailNotify">
                <span class="ds-slider"></span>
              </label>
            </div>

            <div class="ds-switch-item">
              <div class="switch-text">
                <div class="s-label">学习提醒</div>
                <div class="s-desc">每天定时提醒你复习错题本内容</div>
              </div>
              <label class="ds-toggle">
                <input type="checkbox" v-model="accountData.studyReminder">
                <span class="ds-slider"></span>
              </label>
            </div>

            <div class="ds-switch-item">
              <div class="switch-text">
                <div class="s-label">AI建议</div>
                <div class="s-desc">允许 AI 主动分析学习数据并介入提供建议</div>
              </div>
              <label class="ds-toggle">
                <input type="checkbox" v-model="accountData.aiSuggestion">
                <span class="ds-slider"></span>
              </label>
            </div>

            <div class="ds-switch-item">
              <div class="switch-text">
                <div class="s-label">公有可见</div>
                <div class="s-desc">允许社区其他用户查看你的公开错题</div>
              </div>
              <label class="ds-toggle">
                <input type="checkbox" v-model="accountData.mistakePublic">
                <span class="ds-slider"></span>
              </label>
            </div>
          </div>
        </section>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { gsap } from 'gsap'

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
  emailNotify: true,
  studyReminder: true,
  aiSuggestion: false,
  mistakePublic: false
})

//注册天数，和AI列表页一样的算法
const registrationDays = computed(() => {
  if (!accountData.value.createdAt) return 1;
  const start = new Date(accountData.value.createdAt);
  const now = new Date();
  const diffTime = Math.abs(now.getTime() - start.getTime());
  return Math.ceil(diffTime / (1000 * 60 * 60 * 24)) || 1;
});

//用户名首字母大写，用于头像显示
const userInitial = computed(() => {
  return accountData.value.username ? accountData.value.username[0].toUpperCase() : '?'
})

//导出个人数据为JSON文件
const exportUserData = () => {
  const data = JSON.stringify(accountData.value, null, 2);
  const blob = new Blob([data], { type: 'application/json' });
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `SYMPHONY_User_${accountData.value.username}.json`;
  link.click();
  window.URL.revokeObjectURL(url);
};

//拉取用户信息
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

//保存资料和偏好设置
const saveProfile = async () => {
  try {
    const token = localStorage.getItem('token')
    const res = await fetch('/api/auth/update', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + token
      },
      body: JSON.stringify(accountData.value)
    })
    const result = await res.json()
    if (result.code === 200) {
      window.$toast.info('✨ 资料与偏好设置已保存！')
      loadUserInfo()
    } else {
      window.$toast.info('保存失败：' + result.msg)
    }
  } catch (e) {
    console.error('保存出错', e)
    window.$toast.info('网络请求失败')
  }
}

//退出登录
const handleLogout = () => {
  if (confirm('确定要退出登录吗？')) {
    localStorage.removeItem('token')
    router.push('/login')
  }
}

//拉用户信息+GSAP动画
onMounted(() => {
  loadUserInfo()
  gsap.to('.profile-card', { opacity: 1, y: 0, duration: 0.8, ease: "power3.out" })
  gsap.to('.content-section', { opacity: 1, y: 0, duration: 0.8, stagger: 0.15, ease: "power3.out", delay: 0.2 })
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Cormorant+Garamond:ital,wght@1,400;1,600&family=Inter:wght@300;400;600;700&display=swap');

/* 主题变量设定 */
.profile-theme {
  --ds-bg: #fdfdfc;
  --ds-black: #1a1a1a;
  --ds-blue: #004cd5;
  --ds-gold: #d4af37;
  --ds-text: #1a1a1a;
  --ds-text-muted: #888888;
  --ds-border: rgba(0, 0, 0, 0.1);
  --ds-border-light: rgba(0, 0, 0, 0.05);
  font-family: 'Inter', sans-serif;
  color: var(--ds-text);
  min-height: 100vh;
  background: var(--ds-bg);
}

/* 导航栏  */
.ds-nav {
  display: flex; justify-content: space-between; align-items: center;
  padding: 20px 60px; background: rgba(253,253,252,0.9);
  backdrop-filter: blur(10px); border-bottom: 1px solid var(--ds-border);
  position: sticky; top: 0; z-index: 100;
}
.nav-left { display: flex; align-items: baseline; gap: 15px; }
.nav-right { cursor: pointer; }
.ds-logo { font-family: 'Cormorant Garamond', serif; font-size: 24px; letter-spacing: 4px; font-weight: 600; cursor: pointer; }
.breadcrumb { font-size: 10px; letter-spacing: 2px; color: var(--ds-text-muted); text-transform: uppercase; }
.ds-menu-item { font-size: 11px; letter-spacing: 1px; font-weight: 600; transition: color 0.3s; }
.ds-menu-item:hover { color: var(--ds-blue); }

/* 页面网格布局 */
.ds-main-layout {
  max-width: 1200px; margin: 60px auto; padding: 0 40px;
  display: grid; grid-template-columns: 320px 1fr; gap: 60px;
  align-items: start;
}

/*  左侧：铭牌卡片  */
.ds-id-badge {
  background: white; color: var(--ds-text);
  padding: 40px 30px; position: sticky; top: 100px;
  box-shadow: 0 20px 50px rgba(0,0,0,0.05);
  border: 1px solid var(--ds-border-light);
  opacity: 0; transform: translateY(30px);
}
.badge-header {
  font-size: 10px; letter-spacing: 4px; color: var(--ds-text-muted);
  border-bottom: 1px solid var(--ds-border-light); padding-bottom: 15px;
  margin-bottom: 30px; text-align: center; font-weight: 700;
}
.badge-body { text-align: center; margin-bottom: 40px; }
.ds-avatar-wrapper { position: relative; width: 80px; height: 80px; margin: 0 auto 20px; }
.ds-avatar {
  width: 100%; height: 100%; border-radius: 50%; color: white; /* 确保文字是白色 */
  background: var(--ds-blue); display: flex; align-items: center; justify-content: center;
  font-family: 'Cormorant Garamond', serif; font-size: 32px; font-style: italic;
  position: relative; z-index: 2;
}
.avatar-ring {
  position: absolute; top: -5px; left: -5px; right: -5px; bottom: -5px;
  border: 1px solid rgba(0,0,0,0.08); border-radius: 50%; z-index: 1;
}
.profile-name { font-size: 20px; font-weight: 600; letter-spacing: 1px; margin-bottom: 5px; }
.profile-email { font-size: 11px; color: var(--ds-text-muted); font-weight: 400; }

.ds-stats-grid {
  display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px;
  padding: 20px 0; border-top: 1px solid var(--ds-border-light);
  border-bottom: 1px solid var(--ds-border-light); margin-bottom: 30px;
}
.ds-stat { text-align: center; }
.stat-v { font-family: 'Cormorant Garamond', serif; font-size: 26px; color: var(--ds-blue); font-style: italic; }
.stat-l { font-size: 9px; letter-spacing: 1px; color: var(--ds-text-muted); margin-top: 4px; font-weight: 600; }

.ds-badge-actions { display: flex; flex-direction: column; gap: 12px; }
.ds-btn-outline, .ds-btn-danger {
  width: 100%; padding: 14px; background: transparent;
  font-size: 10px; font-weight: 700; letter-spacing: 2px;
  cursor: pointer; transition: 0.3s; display: flex; align-items: center; justify-content: center; gap: 8px;
}
.ds-btn-outline { border: 1px solid var(--ds-border); color: var(--ds-black); }
.ds-btn-outline:hover { background: var(--ds-black); color: white; }
.ds-btn-danger { border: 1px solid rgba(255, 71, 87, 0.3); color: #ff4757; }
.ds-btn-danger:hover { background: #ff4757; color: white; }
/* 右侧：表单面板  */
.ds-content-area { display: flex; flex-direction: column; gap: 40px; }
.ds-form-panel {
  background: white; padding: 40px 50px;
  border: 1px solid var(--ds-border-light);
  opacity: 0; transform: translateY(30px);
}
.panel-header { margin-bottom: 40px; }
.panel-title {
  font-family: 'Cormorant Garamond', serif; font-size: 28px;
  font-weight: 600; display: flex; align-items: center; gap: 15px; margin-bottom: 8px;
}
.title-index { font-size: 14px; color: var(--ds-blue); font-style: italic; }
.panel-desc { font-size: 12px; color: var(--ds-text-muted); letter-spacing: 1px; }

.ds-form { display: flex; flex-direction: column; gap: 25px; }
.ds-form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 30px; }
.ds-input-group { display: flex; flex-direction: column; gap: 8px; }
.ds-label { font-size: 10px; font-weight: 700; letter-spacing: 1px; color: var(--ds-text-muted); }
.ds-input {
  width: 100%; padding: 12px 0; border: none; border-bottom: 1px solid var(--ds-border);
  background: transparent; font-size: 14px; color: var(--ds-text);
  font-family: 'Inter', sans-serif; transition: all 0.3s; border-radius: 0;
}
.ds-input:focus { outline: none; border-bottom-color: var(--ds-blue); box-shadow: 0 1px 0 var(--ds-blue); }
.ds-input:disabled { color: #aaa; border-bottom-style: dashed; }
.ds-textarea { resize: vertical; min-height: 40px; }

.form-actions { margin-top: 20px; display: flex; justify-content: flex-end; }
.ds-btn-solid {
  background: var(--ds-black); color: white; border: none;
  padding: 16px 32px; font-size: 11px; font-weight: 700; letter-spacing: 2px;
  cursor: pointer; transition: 0.3s;
}
.ds-btn-solid:hover { background: var(--ds-blue); transform: translateX(5px); }

.ds-switches-list { display: flex; flex-direction: column; gap: 30px; }
.ds-switch-item { display: flex; justify-content: space-between; align-items: center; padding-bottom: 30px; border-bottom: 1px solid var(--ds-border-light); }
.ds-switch-item:last-child { border-bottom: none; padding-bottom: 0; }
.switch-text { flex: 1; padding-right: 20px; }
.s-label { font-size: 14px; font-weight: 600; margin-bottom: 6px; }
.s-desc { font-size: 12px; color: var(--ds-text-muted); line-height: 1.5; }

.ds-toggle { position: relative; display: inline-block; width: 44px; height: 24px; flex-shrink: 0; }
.ds-toggle input { opacity: 0; width: 0; height: 0; }
.ds-slider {
  position: absolute; cursor: pointer; top: 0; left: 0; right: 0; bottom: 0;
  background-color: #e0e0e0; transition: .4s; border-radius: 24px;
}
.ds-slider:before {
  position: absolute; content: ""; height: 16px; width: 16px;
  left: 4px; bottom: 4px; background-color: white; transition: .4s; border-radius: 50%;
  box-shadow: 0 2px 4px rgba(0,0,0,0.2);
}
input:checked + .ds-slider { background-color: var(--ds-blue); }
input:checked + .ds-slider:before { transform: translateX(20px); }

/* 响应式调整 */
@media (max-width: 900px) {
  .ds-main-layout { grid-template-columns: 1fr; padding: 0 20px; gap: 40px; }
  .ds-id-badge { position: static; }
  .ds-form-row { grid-template-columns: 1fr; gap: 25px; }
}
</style>