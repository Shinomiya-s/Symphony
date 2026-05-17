import { ref } from 'vue'
//弹窗
const toasts = ref([])
let idCounter = 0

export function useToast() {
    function show(message, type = 'info', duration = 3000) {
        const id = ++idCounter
        toasts.value.push({ id, message, type })
        setTimeout(() => {
            toasts.value = toasts.value.filter(t => t.id !== id)
        }, duration)
    }

    return {
        toasts,
        success: (msg) => show(msg, 'success'),
        error:   (msg) => show(msg, 'error'),
        warning: (msg) => show(msg, 'warning'),
        info:    (msg) => show(msg, 'info'),
    }
}