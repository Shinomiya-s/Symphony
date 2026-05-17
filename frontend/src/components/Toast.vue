<template>
  <Teleport to="body">
    <div class="toast-container">
      <TransitionGroup name="toast">
        <div
            v-for="item in toasts"
            :key="item.id"
            class="toast-item"
            :class="item.type"
        >
          <span class="toast-icon">
            <span v-if="item.type === 'success'">✓</span>
            <span v-else-if="item.type === 'error'">✕</span>
            <span v-else-if="item.type === 'warning'">⚠</span>
            <span v-else>ℹ</span>
          </span>
          <span class="toast-msg">{{ item.message }}</span>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<script setup>
import { useToast } from '@/composables/useToast'
const { toasts } = useToast()
</script>

<style scoped>
.toast-container {
  position: fixed;
  top: 24px;
  right: 24px;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  gap: 10px;
  pointer-events: none;
}

.toast-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 18px;
  border-radius: 6px;
  font-family: 'Inter', sans-serif;
  font-size: 13px;
  letter-spacing: 0.01em;
  background: #ffffff;
  border: 1px solid #e0e0e0;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  min-width: 220px;
  max-width: 360px;
  pointer-events: all;
}

.toast-item.success { border-left: 3px solid #4a7c59; color: #2d4a38; }
.toast-item.error   { border-left: 3px solid #a04444; color: #5c2020; }
.toast-item.warning { border-left: 3px solid #a07c30; color: #5c4010; }
.toast-item.info    { border-left: 3px solid #4a6a8a; color: #2a3a50; }

.toast-icon { font-size: 14px; flex-shrink: 0; }

/* 动画 */
.toast-enter-active { transition: all 0.28s ease; }
.toast-leave-active { transition: all 0.22s ease; }
.toast-enter-from   { opacity: 0; transform: translateX(20px); }
.toast-leave-to     { opacity: 0; transform: translateX(20px); }
</style>