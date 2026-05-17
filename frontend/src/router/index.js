import { createRouter, createWebHistory } from 'vue-router'
import Login from "../views/Login.vue";

const routes = [
    { path: '/', redirect: '/home' },
    { path: '/login', name: 'Login', component: Login },
    {
        path: '/home',
        name: 'Home',
        component: () => import('../views/Home.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/mistakes',
        name: 'Mistakes',
        component: () => import('../views/Mistakes.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/canvas/:boardId?',
        name: 'Canvas',
        component: () => import('../views/Canvas.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/ai',
        name: 'AI',
        component: () => import('../views/AI.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/profile',
        name: 'Profile',
        component: () => import('../views/Profile.vue'),
        meta: { requiresAuth: true }
    },

    //聊天路由

    // 默认跳转,/chat 直接访问时
    {
        path: '/chat',
        redirect: () => {
            const userId = localStorage.getItem('currentUserId')
            return userId ? `/chat/${userId}/public` : '/login'
        }
    },
    {
        path: '/chat/:userId',
        redirect: (to) => `/chat/${to.params.userId}/public`
    },

    // 2. 私聊 → ChatPrivate.vue
    {
        path: '/chat/:userId/UTOU/:targetUserId',
        name: 'ChatPrivate',
        component: () => import('../views/chat/ChatPrivate.vue'),
        meta: { requiresAuth: true }
    },

    // 3. 公屏 → ChatPublic.vue
    {
        path: '/chat/:userId/public',
        name: 'ChatPublic',
        component: () => import('../views/chat/ChatPublic.vue'),
        meta: { requiresAuth: true }
    },

    {
        path: '/create-ai',
        name: 'Create',
        component: () => import('../views/CreateAI.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/collab/gallery/:roomId',
        name: 'CollabGallery',
        component: () => import('../views/CollabGallery.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/garden',
        name: 'KnowledgeGarden',
        component: () => import('../views/KnowledgeGarden.vue'),
        meta: { requiresAuth: true }
    }
]

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL || '/'),
    routes
})

router.beforeEach((to, from, next) => {
    const token = localStorage.getItem('token')
    if (to.meta.requiresAuth && !token) {
        next('/login')
    } else if (to.path === '/login' && token) {
        next('/home')
    } else {
        next()
    }
})

export default router