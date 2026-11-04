import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import AppLayout from '@/layouts/AppLayout.vue'
import ComingSoonView from '@/views/ComingSoonView.vue'
import DynamicMenuView from '@/views/system/DynamicMenuView.vue'
import ForbiddenView from '@/views/error/ForbiddenView.vue'
import NotFoundView from '@/views/error/NotFoundView.vue'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: ComingSoonView,
    meta: { public: true, title: '登录' },
  },
  {
    path: '/',
    component: AppLayout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: ComingSoonView,
        meta: { title: '仪表盘' },
      },
      {
        path: 'system/:pathMatch(.*)*',
        name: 'dynamic-menu',
        component: DynamicMenuView,
        meta: { title: '系统管理' },
      },
    ],
  },
  {
    path: '/403',
    name: 'forbidden',
    component: ForbiddenView,
    meta: { title: '无权限' },
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: NotFoundView,
    meta: { public: true, title: '页面不存在' },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()

  if (to.meta.public) {
    if (auth.token && to.name === 'login') return { path: '/dashboard' }
    return true
  }

  if (!auth.token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  if (!auth.loaded) {
    try {
      await auth.loadSession()
    } catch {
      auth.clear()
      return { path: '/login', query: { redirect: to.fullPath } }
    }
  }

  return true
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · RouteGo Admin` : 'RouteGo Admin'
})

export default router
