import { defineStore } from 'pinia'
import {
  getCaptcha as getCaptchaApi,
  getMe,
  login as loginApi,
  logout as logoutApi,
} from '@/api/auth'
import { getUserMenuTree } from '@/api/system'
import { TOKEN_KEY, USER_KEY } from '@/api/request'

export function hasPermission(perms, needed) {
  if (!needed || (Array.isArray(needed) && needed.length === 0)) return true
  const list = Array.isArray(perms) ? perms : []
  if (list.includes('*')) return true
  if (Array.isArray(needed)) return needed.some((item) => list.includes(item))
  return list.includes(needed)
}

function readStoredUser() {
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: readStoredUser(),
    menus: [],
    loaded: false,
  }),

  getters: {
    permissions: (state) => state.user?.permissions || [],
    hasPerm: (state) => (perm) => hasPermission(state.user?.permissions || [], perm),
    displayName: (state) => state.user?.nickname || state.user?.username || '未登录',
  },

  actions: {
    async fetchCaptcha() {
      return getCaptchaApi()
    },

    async login(payload) {
      const result = await loginApi(payload)
      this.token = result.tokenValue
      localStorage.setItem(TOKEN_KEY, result.tokenValue)
      await this.loadSession()
      return result
    },

    async loadSession() {
      const [me, menus] = await Promise.all([getMe(), getUserMenuTree()])
      this.user = me
      this.menus = menus || []
      this.loaded = true
      localStorage.setItem(USER_KEY, JSON.stringify(me))
      if (!this.token) {
        this.token = localStorage.getItem(TOKEN_KEY) || ''
      }
      return me
    },

    async logout() {
      try {
        await logoutApi()
      } catch {
        // 本地会话仍需清理
      } finally {
        this.clear()
      }
    },

    clear() {
      this.token = ''
      this.user = null
      this.menus = []
      this.loaded = false
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    },
  },
})
