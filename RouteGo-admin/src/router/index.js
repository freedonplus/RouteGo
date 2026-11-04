import { createRouter, createWebHistory } from 'vue-router'
import AppLayout from '@/layouts/AppLayout.vue'
import ComingSoonView from '@/views/ComingSoonView.vue'

const routes = [
  {
    path: '/login',
    component: ComingSoonView,
    meta: { public: true, title: '登录' },
  },
  {
    path: '/',
    component: AppLayout,
    children: [
      {
        path: '',
        component: ComingSoonView,
        meta: { title: '仪表盘' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
