<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { findMenuByPath } from '@/utils/menu'

/**
 * 页面标题默认跟随「菜单名称」，而不是组件里写死的字面量。
 * 后台把菜单改名后，侧边栏、面包屑、浏览器标题都换了，唯独这里还是旧名，
 * 所以这里以菜单名优先，查不到菜单（登录页、错误页等）才退回调用方传的 title。
 */
const props = defineProps({
  title: { type: String, required: true },
  description: { type: String, default: '' },
  /** 少数页面（如仪表盘内部的子视图）不希望被菜单名覆盖时置 false */
  followMenu: { type: Boolean, default: true },
})

const auth = useAuthStore()
const route = useRoute()

const heading = computed(() => {
  if (!props.followMenu) return props.title
  return findMenuByPath(auth.menus, route.path)?.menuName || props.title
})
</script>

<template>
  <div
    class="flex flex-wrap items-center justify-between gap-2 border-b border-[#e2e8f0] bg-white px-4 py-2.5"
  >
    <div class="min-w-0">
      <h1 class="truncate text-sm font-semibold tracking-tight text-[#121212]">{{ heading }}</h1>
      <p v-if="description" class="mt-0.5 truncate text-[11px] text-[#64748b]">
        {{ description }}
      </p>
    </div>
    <div class="flex shrink-0 items-center gap-1">
      <slot name="actions" />
    </div>
  </div>
</template>
