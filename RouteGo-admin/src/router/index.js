import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { abortPendingRequests } from '@/api/request'
import AppLayout from '@/layouts/AppLayout.vue'
import LoginView from '@/views/login/LoginView.vue'
import DashboardView from '@/views/dashboard/DashboardView.vue'
import ForbiddenView from '@/views/error/ForbiddenView.vue'
import NotFoundView from '@/views/error/NotFoundView.vue'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: LoginView,
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
        component: DashboardView,
        meta: { title: '仪表盘' },
      },
      {
        // 所有业务页面都由后端菜单驱动：/catalog/route、/trade/order ...
        // 这里用一段受约束的动态段兜住，新增模块不用再改路由表。
        path: ':module([a-z]+)/:pathMatch(.*)*',
        name: 'dynamic-menu',
        // 兜底容器承载全部业务页面，懒加载避免打进首屏
        component: () => import('@/views/system/DynamicMenuView.vue'),
        meta: { title: '业务页面' },
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
    if (to.name === 'login' && !auth.loaded) {
      // The production API uses an HttpOnly cookie; try to restore that session.
      try {
        await auth.loadSession()
      } catch {
        auth.clear()
      }
    }
    if (auth.loaded && to.name === 'login') return { path: '/dashboard' }
    return true
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
  // 导航确认后取消上一页面遗留的请求：新页面的请求都在此之后发出，不会误伤
  abortPendingRequests()
})

export default router
