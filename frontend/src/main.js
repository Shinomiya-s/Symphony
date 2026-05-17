import '@excalidraw/excalidraw/index.css';
import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import router from './router'
import { useToast } from '@/composables/useToast'
const toast = useToast()
window.$toast = toast
createApp(App).use(router)
              .mount('#app')
