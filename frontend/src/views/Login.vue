<script setup lang="ts">
import {ref} from "vue";
import RainbowParticles from '../components/RainbowParticles.vue'
import axios from 'axios'
import { useRouter } from "vue-router";
const formData = ref({
  username: '',
  phone: '',
  password: ''
})
const router = useRouter();
const handleSubmit = async() => {
  const url = isRegister.value ? '/api/auth/register' : '/api/auth/login'
  try {
    const res = await axios.post(url, formData.value)

    if (res.data.code === 200) {
      if (isRegister.value) {
        //注册成功，后端只返回字符串，不存token
        window.$toast.info('注册成功！')
        isRegister.value = false
        setTimeout(() => router.push('/login'), 1500)
      } else {
        //登录成功才存token
        const token = res.data.data.token
        localStorage.setItem('token', token)
        localStorage.setItem('currentUserId', res.data.data.user.userId)
        window.$toast.info('登录成功！')
        router.push('/home')
      }
    } else {
      window.$toast.info('失败：' + res.data.msg)
    }
  } catch (error: any) {
    if (error.response?.status === 403) {
      window.$toast.info('失败：手机号格式不正确或注册过于频繁')
    } else {
      window.$toast.info('网络错误，请重试')
    }
  }
}
const isRegister = ref(false)
</script>

<template>
  <div class="page">
    <div class="left-section">
      <div class="bgcolor">
        <div class="mainTitle">
          <RainbowParticles />
        </div>
      </div>
    </div>
    <div class="right-section">
      <Transition name="fade-slide" mode="out-in">
        <div class="auth-container" :key="isRegister ? 'register' : 'login'" :class="{ register: isRegister }">
          <svg width="48" height="52" viewBox="0 0 48 52" xmlns="http://www.w3.org/2000/svg" class="logo">
            <line x1="10" y1="18" x2="38" y2="18" stroke="#004cd5" stroke-width="2" opacity="0.5"/>
            <line x1="10" y1="26" x2="38" y2="26" stroke="#004cd5" stroke-width="2" opacity="0.5"/>
            <line x1="10" y1="34" x2="38" y2="34" stroke="#004cd5" stroke-width="2" opacity="0.5"/>
            <path d="M32 12 C32 8, 16 8, 16 18 C16 26, 32 26, 32 34 C32 44, 16 44, 16 40"
                  fill="none" stroke="#004cd5" stroke-width="4.5" stroke-linecap="round"/>
          </svg>
          <h2 class="formTitle">  {{ isRegister ? '创建账号' : '欢迎回来' }}</h2>
          <p class="subtitle"> {{ isRegister ? '注册进入彩色世界' : '登录进入彩色世界' }}</p>
          <form @submit.prevent="handleSubmit" id="authform" :class="{ register: isRegister }">
            <div class="input-group" v-if="isRegister">
              <label class="input-label">用户名</label>
              <div class="input-wrapper">
                <i class="fas fa-user"></i>
                <input v-model="formData.username" type="text" placeholder="你的名字" required>
              </div>
            </div>
            <div class="input-group">
              <label class="input-label">手机号</label>
              <div class="input-wrapper">
                <i class="fas fa-phone"></i>
                <input v-model="formData.phone" type="text" id="phoneNumber" placeholder="输入手机号" required>
              </div>
            </div>
            <div class="input-group">
              <label class="input-label">密码</label>
              <div class="input-wrapper">
                <i class="fas fa-lock"></i>
                <input v-model="formData.password" type="password" id="password" placeholder="输入密码" required>
              </div>
            </div>
            <button type="submit" class="btn">
              <span class="btn_inner">
                <span class="btn_label">  {{ isRegister ? '注册' : '登录' }}</span>
              </span>
            </button>
          </form>
          <div class="switch-mode">
            <span>{{ isRegister ? '已有账号' : '还没有账号?' }}
              <span class="actionText" @click="isRegister=!isRegister">
                {{ isRegister ? '立即登录' : '立即注册' }}
              </span>
            </span>
          </div>
        </div>
      </Transition>
    </div>
  </div>
</template>

<style scoped>
.page{
  display: flex;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
}
.left-section {
  width: 66.5%;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
}
.bgcolor {
  width: 100%;
  height: 100%;
  position: relative;
}
.mainTitle {
  width: 100%;
  height: 100%;
  position: relative;
}
.right-section {
  width: 480px;
  padding: 60px;
  display: flex;
  justify-content: center;
  align-items: center;
}
.auth-container{
  display: flex;
  width: 360px;
  height: 471px;
  flex-direction: column;
}
.auth-container.register {
  height: 564px;
}
.logo{
  width: 48px;
  height: 52px;
  margin-bottom: 20px;
}
.formTitle{
  font-size: 32px;
  margin-bottom: 8px;
  color: #2c3e50;
}
.subtitle{
  margin-bottom: 30px;
  color: #8b9798;
}
#authform{
  display: flex;
  flex-direction: column;
  width: 360px;
  height: 248px;
}

#authform.register{
  height: 341px;
}
.input-group{
  width: 360px;
  height: 72px;
  margin-bottom: 20px;
}
.input-label{
  display: block;
  width:360px;
  height: 17.33px;
  font-size: 15px;
  font-weight: 550;
  margin-bottom: 8px;
}
.input-wrapper{
  width: 360px;
  height: 47.33px;
  background: #f8f9fa;
  border: 2px solid #E8E8E8;
  display: flex;
  align-items: center;
  border-radius: 8px;
  padding-left: 12px;
}
.fas{
  width: 16px;
  height: 16px;
  color: #95a5a6;
}
.input-wrapper input {
  width: 292px;
  height: 15.3px;
  border: 2px;
  padding: 0 16px; /* 内边距 */
  background: #f8f9fa;
  display: inline-block;
  border: none;
  flex: 1;
  outline: none;
}
.btn{
  display: flex;
  width: 360px;
  height: 58px;
  border:0.1px solid #004cd5;
  border-radius: 50px;
  margin-top: 5px;
  background-color: #004cd5;
  justify-content: center;
}
.btn_inner{
  display: flex;
  align-items: center;
}
.btn_label{
  font-size: 17px;
  color: white;
  font-weight: bold;
}

.switch-mode{
  display: flex;
  font-size: 13.5px;
  font-weight: bold;
  width: 360px;
  height:20px;
  margin-top: 25px;
  justify-content: center;
  cursor: default;
}

.actionText{
  color: #004cd5;
}

.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.3s ease;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(20px);  /* 从下方20px淡入 */
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-20px);  /* 向上20px淡出 */
}
</style>