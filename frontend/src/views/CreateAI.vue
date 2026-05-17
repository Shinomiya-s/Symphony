<template>
  <div class="create-ai-page">
    <!-- 导航栏 -->
    <nav class="navbar">
      <div class="nav-back" @click="$router.push('/ai')">
        <i class="fas fa-arrow-left"></i>
        <span>返回</span>
      </div>
    </nav>

    <!-- 主容器 -->
    <div class="main-container">
      <!-- 已创建提示 -->
      <div v-if="hasCreated" class="already-created">
        <div class="info-icon">ℹ️</div>
        <h2>你已经创建过AI助手了</h2>
        <p>每个用户只能创建一个学习AI助手</p>
        <button class="btn-back" @click="$router.push('/ai')">返回AI列表</button>
      </div>

      <!-- 创建表单 -->
      <div v-else class="form-container">
        <h2 class="page-title">创建你的学习AI助手</h2>
        <p class="page-desc">填写以下信息来定制专属于你的学习助手</p>

        <div class="form-section">
          <div class="form-group">
            <label class="form-label">AI名称 <span class="required">*</span></label>
            <input
                v-model="aiConfig.name"
                type="text"
                class="form-input"
                placeholder="例如：算法学习助手"
                maxlength="30"
            >
            <div class="input-hint">{{ aiConfig.name.length }}/30</div>
          </div>

          <div class="form-group">
            <label class="form-label">AI描述 <span class="required">*</span></label>
            <textarea
                v-model="aiConfig.description"
                class="form-textarea"
                placeholder="描述这个AI的主要功能和特点，例如：专注于算法和数据结构学习，帮助我理解各种算法原理..."
                maxlength="200"
                rows="4"
            ></textarea>
            <div class="input-hint">{{ aiConfig.description.length }}/200</div>
          </div>

          <div class="form-group">
            <label class="form-label">AI Prompt <span class="required">*</span></label>
            <textarea
                v-model="aiConfig.prompt"
                class="form-textarea"
                placeholder="定义AI的行为和回答风格，例如：你是一个专业的算法导师，擅长用通俗易懂的方式讲解复杂的算法概念。在回答时，请先解释原理，再给出代码示例..."
                maxlength="500"
                rows="6"
            ></textarea>
            <div class="input-hint">{{ aiConfig.prompt.length }}/500</div>
          </div>

          <button
              class="btn-create"
              @click="createAI"
              :disabled="!canCreate"
          >
            创建AI助手
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from "axios";

const router = useRouter()

//弃用
const hasCreated = ref(false)
const aiConfig = ref({
  name: '',
  description: '',
  prompt: ''
})

const canCreate = computed(() => {
  //三个字段都不为空才允许提交
  return aiConfig.value.name.trim().length > 0 &&
      aiConfig.value.description.trim().length > 0 &&
      aiConfig.value.prompt.trim().length > 0
})

//页面加载时检查是否已创建过
onMounted(() => {
  checkIfAlreadyCreated()
})

//检查是否已创建过,实际限制一个用户只能创建一个AI的逻辑在后端做的
const checkIfAlreadyCreated = async () => {
  // const response = await api.checkUserAi(userId)
  // hasCreated.value = response.hasAi
  hasCreated.value = false // 临时设置为false
}

//提交创建
const createAI = async () => {
  if (!canCreate.value) return     //字段不全直接return
  try {
    const payload = {
      name: aiConfig.value.name.trim(),
      description: aiConfig.value.description.trim(),
      prompt: aiConfig.value.prompt.trim(),
    }

    const res = await axios.post('/api/ai/createPersonalAi', payload, {
      headers: {
        // 从本地存储拿 Token，并加上 Bearer 前缀
        'Authorization': 'Bearer ' + localStorage.getItem('token')
      }
    })
    if (res.data.code === 200) {
      window.$toast.info('AI创建成功！')
      router.push('/ai')
    } else {
      // window.$toast.info('失败：' + res.data.message)
      window.$toast.info('只能创建一个自己的AI')
    }
  } catch (error) {
    window.$toast.info('创建失败：' + error.message)
  }
}
</script>
<style>
@import url('https://fonts.googleapis.com/css2?family=Cormorant+Garamond:wght@400;600&family=Inter:wght@300;400;500;600;700&display=swap');

:root {
  --symph-black: #2c3e50;
  --symph-white: #fdfdfc;
  --symph-gray: #7f8c8d;
  --symph-light-gray: #ecf0f1;
  --symph-blue: #4a90e2;
  --symph-gold: #d4af37;
  --symph-brown: #613125;
  --symph-border: rgba(0,0,0,0.06);
}
</style>

<style scoped>

*{
  margin:0;
  padding:0;
  box-sizing:border-box;
}

.create-ai-page{
  min-height:100vh;
  background:linear-gradient(135deg,#fdfdfc 0%,#f8fafb 100%);
  font-family:'Inter',sans-serif;
  color:var(--symph-black);
}


.navbar{
  display:flex;
  align-items:center;
  padding:20px 60px;
  border-bottom:1px solid var(--symph-border);
  background:var(--symph-white);
}

.nav-back{
  display:flex;
  align-items:center;
  gap:10px;
  border:1px solid var(--symph-border);
  padding:10px 20px;
  border-radius:8px;
  font-size:10px;
  font-weight:700;
  letter-spacing:1.5px;
  cursor:pointer;
  transition:all .25s;
}

.nav-back:hover{
  border-color:var(--symph-blue);
  color:var(--symph-blue);
  background:rgba(74,144,226,0.05);
}


.main-container{
  max-width:900px;
  margin:0 auto;
  padding:60px 40px;
}

.form-container {
  background: white;
  border-radius: 20px;
  padding: 40px;

  border: 1px solid rgba(0,0,0,0.06);

  box-shadow:
      0 10px 30px rgba(0,0,0,0.08),
      0 2px 6px rgba(0,0,0,0.05);

  transition: all 0.25s ease;
}

.form-container:hover {
  transform: translateY(-2px);
  box-shadow:
      0 16px 40px rgba(0,0,0,0.10),
      0 4px 12px rgba(0,0,0,0.06);
}
@keyframes fadeUp{
  from{
    opacity:0;
    transform:translateY(20px);
  }
  to{
    opacity:1;
    transform:translateY(0);
  }
}

.page-title{
  font-size:30px;
  font-weight:300;
  letter-spacing:2px;
  text-align:center;
  margin-bottom:10px;
}

.page-desc{
  text-align:center;
  font-size:13px;
  color:var(--symph-gray);
  margin-bottom:40px;
}

.form-group{
  margin-bottom:28px;
}

.form-label{
  display:block;
  font-size:11px;
  font-weight:700;
  letter-spacing:1.5px;
  margin-bottom:10px;
}

.required{
  color:var(--symph-gold);
}

.form-input,
.form-textarea{
  width:100%;
  padding:14px 16px;
  border:1px solid var(--symph-border);
  border-radius:8px;
  font-size:14px;
  font-family:inherit;
  background:var(--symph-white);
  transition:.25s;
}

.form-input:focus,
.form-textarea:focus{
  outline:none;
  border-color:var(--symph-blue);
  box-shadow:0 0 0 3px rgba(74,144,226,0.1);
}

.form-textarea{
  resize:vertical;
  line-height:1.6;
}

.input-hint{
  font-size:11px;
  color:var(--symph-gray);
  margin-top:6px;
  text-align:right;
}


.btn-create{
  width:100%;
  margin-top:10px;
  padding:14px;
  border:none;
  border-radius:8px;
  font-size:11px;
  font-weight:700;
  letter-spacing:1.5px;
  cursor:pointer;
  background:linear-gradient(
      135deg,
      var(--symph-black) 0%,
      var(--symph-blue) 100%
  );
  color:var(--symph-white);
  transition:.25s;
}

.btn-create:hover:not(:disabled){
  transform:translateY(-2px);
  box-shadow:0 10px 25px rgba(74,144,226,.3);
}

.btn-create:disabled{
  opacity:.5;
  cursor:not-allowed;
}


.already-created{
  text-align:center;
  padding:60px;
  background:rgba(255,255,255,0.8);
  border:1px solid var(--symph-border);
  border-radius:14px;
}

.info-icon{
  font-size:56px;
  margin-bottom:20px;
}

.already-created h2{
  font-size:22px;
  font-weight:600;
  margin-bottom:10px;
}

.already-created p{
  font-size:13px;
  color:var(--symph-gray);
  margin-bottom:28px;
}

.btn-back{
  padding:12px 30px;
  border:none;
  border-radius:8px;
  font-size:11px;
  font-weight:700;
  letter-spacing:1.5px;
  background:var(--symph-black);
  color:white;
  cursor:pointer;
  transition:.25s;
}

.btn-back:hover{
  background:var(--symph-blue);
}


@media(max-width:768px){

  .navbar{
    padding:16px 20px;
  }

  .main-container{
    padding:30px 20px;
  }

  .form-container{
    padding:30px 24px;
  }

}
</style>