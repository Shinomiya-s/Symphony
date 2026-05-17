<template>
  <Toast />
  <router-view />
</template>

<script setup>
import Toast from '@/components/Toast.vue'
</script>

<style>
@import url('https://fonts.googleapis.com/css2?family=PingFang+SC&display=swap');

body {
  margin: 0;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
}
</style>