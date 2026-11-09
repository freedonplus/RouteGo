<script setup>
import { useRoute } from 'vue-router'
import { Odometer } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import SidebarItem from './SidebarItem.vue'
import BrandMark from './BrandMark.vue'

defineProps({
  collapsed: { type: Boolean, default: false },
})

const emit = defineEmits(['navigate'])
const auth = useAuthStore()
const route = useRoute()
</script>

<template>
  <div class="flex h-full flex-col bg-[#121212]">
    <div class="flex h-12 shrink-0 items-center gap-2 border-b border-[#2a2a2a] px-3">
      <BrandMark :size="20" class="shrink-0 text-white" />
      <template v-if="!collapsed">
        <span class="text-sm font-semibold tracking-tight text-white">RouteGo</span>
        <span class="ml-auto rounded bg-[#1f1f1f] px-1.5 py-0.5 text-[10px] text-[#94a3b8]">
          Admin
        </span>
      </template>
    </div>

    <nav class="min-h-0 flex-1 overflow-y-auto px-1 py-1" aria-label="主导航">
      <router-link
        to="/dashboard"
        class="group flex h-8 items-center gap-2 rounded px-2 text-xs transition-colors duration-150"
        :class="[
          route.path === '/dashboard'
            ? 'bg-[#1f1f1f] font-medium text-[#e89aaf]'
            : 'text-[#cbd5e1] hover:bg-[#1a1a1a] hover:text-white',
          collapsed ? 'justify-center' : '',
        ]"
        :title="collapsed ? '仪表盘' : undefined"
        @click="emit('navigate')"
      >
        <el-icon
          class="shrink-0"
          :class="route.path === '/dashboard' ? 'text-[#e89aaf]' : 'text-[#94a3b8]'"
        >
          <Odometer />
        </el-icon>
        <span v-if="!collapsed" class="truncate">仪表盘</span>
      </router-link>

      <div class="my-1 border-t border-[#2a2a2a]"></div>

      <SidebarItem
        v-for="item in auth.menus"
        :key="item.id"
        :item="item"
        :collapsed="collapsed"
        @navigate="emit('navigate')"
      />

      <p v-if="!auth.menus.length && !collapsed" class="px-2 py-3 text-[10px] text-[#6b7280]">
        暂无可用菜单
      </p>
    </nav>

    <div class="shrink-0 border-t border-[#2a2a2a] px-3 py-2">
      <p v-if="!collapsed" class="font-mono text-[10px] text-[#6b7280]">v1.0.0 · mock</p>
      <p v-else class="text-center font-mono text-[10px] text-[#6b7280]">v1</p>
    </div>
  </div>
</template>
