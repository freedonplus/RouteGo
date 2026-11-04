<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowDown,
  Bell,
  Expand,
  Fold,
  Lock,
  Search,
  SwitchButton,
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { getPublishedNotices } from '@/api/system'
import { formatDateTime } from '@/utils/format'
import { menuTrail } from '@/utils/menu'
import CommandPalette from './CommandPalette.vue'
import ChangePasswordDialog from './ChangePasswordDialog.vue'

const props = defineProps({
  collapsed: { type: Boolean, default: false },
})
const emit = defineEmits(['toggle'])

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const paletteOpen = ref(false)
const passwordOpen = ref(false)
const notices = ref([])
const noticeLoading = ref(false)

const trail = computed(() => {
  if (route.path === '/dashboard') return [{ menuName: '仪表盘' }]
  return menuTrail(auth.menus, route.path)
})

const initial = computed(() => (auth.displayName || 'A').slice(0, 1).toUpperCase())

async function loadNotices() {
  if (noticeLoading.value) return
  noticeLoading.value = true
  try {
    notices.value = await getPublishedNotices()
  } catch {
    notices.value = []
  } finally {
    noticeLoading.value = false
  }
}

function isTypingTarget(target) {
  if (!target) return false
  const tag = target.tagName
  return tag === 'INPUT' || tag === 'TEXTAREA' || target.isContentEditable
}

function onKeydown(event) {
  const key = event.key.toLowerCase()
  if ((event.ctrlKey || event.metaKey) && key === 'k') {
    event.preventDefault()
    paletteOpen.value = true
    return
  }
  if (event.key === '/' && !isTypingTarget(event.target)) {
    event.preventDefault()
    paletteOpen.value = true
  }
}

async function handleCommand(command) {
  if (command === 'password') {
    passwordOpen.value = true
    return
  }
  if (command === 'logout') {
    await auth.logout()
    ElMessage.success('已退出登录')
    router.replace('/login')
  }
}

onMounted(() => window.addEventListener('keydown', onKeydown))
onBeforeUnmount(() => window.removeEventListener('keydown', onKeydown))
</script>

<template>
  <header
    class="flex h-12 shrink-0 items-center gap-2 border-b border-[#e2e8f0] bg-white px-3"
  >
    <button
      type="button"
      class="flex h-7 w-7 shrink-0 items-center justify-center rounded text-[#64748b] transition-colors duration-150 hover:bg-[#f8fafc] hover:text-[#1e293b] focus-visible:ring-1 focus-visible:ring-[#3b82f6] active:scale-95"
      :aria-label="collapsed ? '展开侧栏' : '收起侧栏'"
      @click="emit('toggle')"
    >
      <el-icon><component :is="collapsed ? Expand : Fold" /></el-icon>
    </button>

    <nav class="flex min-w-0 items-center gap-1 text-xs text-[#64748b]" aria-label="面包屑">
      <template v-for="(item, index) in trail" :key="`${item.menuName}-${index}`">
        <span v-if="index" class="text-[#cbd5e1]">/</span>
        <span
          class="truncate"
          :class="index === trail.length - 1 ? 'font-medium text-[#1e293b]' : 'text-[#64748b]'"
        >
          {{ item.menuName }}
        </span>
      </template>
    </nav>

    <div class="ml-auto flex shrink-0 items-center gap-1">
      <button
        type="button"
        class="flex h-7 items-center gap-1.5 rounded border border-[#e2e8f0] bg-white px-2 text-[11px] text-[#64748b] transition-colors duration-150 hover:bg-[#f8fafc] hover:text-[#1e293b] focus-visible:ring-1 focus-visible:ring-[#3b82f6]"
        aria-label="搜索页面"
        @click="paletteOpen = true"
      >
        <el-icon><Search /></el-icon>
        <span class="hidden md:inline">搜索</span>
        <kbd class="hidden rounded border border-[#e2e8f0] px-1 font-mono text-[10px] md:inline">
          Ctrl K
        </kbd>
      </button>

      <el-popover trigger="click" placement="bottom-end" :width="320" @show="loadNotices">
        <template #reference>
          <button
            type="button"
            class="relative flex h-7 w-7 items-center justify-center rounded text-[#64748b] transition-colors duration-150 hover:bg-[#f8fafc] hover:text-[#1e293b] focus-visible:ring-1 focus-visible:ring-[#3b82f6]"
            aria-label="公告"
          >
            <el-icon><Bell /></el-icon>
            <span
              v-if="notices.length"
              class="absolute right-1 top-1 h-1.5 w-1.5 rounded-full bg-[#e89aaf]"
            ></span>
          </button>
        </template>
        <div>
          <div class="flex items-center justify-between border-b border-[#e2e8f0] pb-1.5">
            <span class="text-xs font-semibold text-[#1e293b]">公告</span>
            <span class="font-mono text-[10px] text-[#94a3b8]">{{ notices.length }} 条</span>
          </div>
          <p v-if="noticeLoading" class="py-4 text-center text-[11px] text-[#94a3b8]">加载中…</p>
          <p v-else-if="!notices.length" class="py-4 text-center text-[11px] text-[#94a3b8]">
            暂无公告
          </p>
          <ul v-else class="divide-y divide-[#e2e8f0]">
            <li v-for="notice in notices" :key="notice.id" class="py-2">
              <div class="flex items-center gap-1">
                <span
                  v-if="notice.isTop"
                  class="rounded bg-[#fdf1f5] px-1 text-[10px] text-[#a8435f]"
                >
                  置顶
                </span>
                <span class="truncate text-xs font-medium text-[#1e293b]">{{ notice.title }}</span>
              </div>
              <p class="mt-0.5 line-clamp-2 text-[11px] text-[#64748b]">{{ notice.content }}</p>
              <p class="mt-0.5 font-mono text-[10px] text-[#94a3b8]">
                {{ formatDateTime(notice.publishTime) }}
              </p>
            </li>
          </ul>
        </div>
      </el-popover>

      <el-dropdown trigger="click" @command="handleCommand">
        <button
          type="button"
          class="flex h-7 items-center gap-1.5 rounded px-1.5 transition-colors duration-150 hover:bg-[#f8fafc] focus-visible:ring-1 focus-visible:ring-[#3b82f6]"
          aria-label="账号菜单"
        >
          <span
            class="flex h-5 w-5 items-center justify-center rounded bg-[#e89aaf] text-[10px] font-semibold text-[#121212]"
          >
            {{ initial }}
          </span>
          <span class="hidden max-w-[120px] truncate text-xs text-[#1e293b] md:inline">
            {{ auth.displayName }}
          </span>
          <el-icon class="text-[#94a3b8]"><ArrowDown /></el-icon>
        </button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="password">
              <el-icon><Lock /></el-icon>
              修改密码
            </el-dropdown-item>
            <el-dropdown-item command="logout" divided>
              <el-icon><SwitchButton /></el-icon>
              退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <CommandPalette v-model="paletteOpen" />
    <ChangePasswordDialog v-model="passwordOpen" />
  </header>
</template>
