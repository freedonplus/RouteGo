<script setup>
import { computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowRight } from '@element-plus/icons-vue'
import { isGroupOpen, openGroup, toggleGroup } from '@/utils/sidebar-groups'
import { resolveMenuIcon } from './menu-icons'

defineOptions({ name: 'SidebarItem' })

const props = defineProps({
  item: { type: Object, required: true },
  collapsed: { type: Boolean, default: false },
  depth: { type: Number, default: 0 },
})

const emit = defineEmits(['navigate'])
const route = useRoute()

const isActive = computed(() => route.path === props.item.path)
const children = computed(() =>
  (props.item.children || []).filter((item) => item.menuType !== 'F')
)

const isGroup = computed(() => props.item.menuType === 'M')
// 根菜单（目录）的展开状态：默认展开，点击标题收起，再点重新展开
const open = computed(() => isGroupOpen(props.item.id))

function containsPath(node, path) {
  if (node.path && node.path === path) return true
  return (node.children || []).some((child) => containsPath(child, path))
}

// 当前页面落在该目录内时自动展开，避免路由跳转后「找不到自己在哪」；
// 仅在路由变化时触发，用户手动收起当前目录不会被立刻弹开
watch(
  () => route.path,
  (path) => {
    if (isGroup.value && containsPath(props.item, path)) openGroup(props.item.id)
  },
  { immediate: true }
)

function toggle() {
  toggleGroup(props.item.id)
}
</script>

<template>
  <div v-if="isGroup" :class="depth === 0 ? 'pt-1' : ''">
    <button
      v-if="!collapsed"
      type="button"
      class="group flex h-8 w-full items-center gap-2 rounded px-2 text-left text-xs font-medium text-[#94a3b8] transition-colors duration-150 hover:bg-[#1a1a1a] hover:text-white focus-visible:ring-1 focus-visible:ring-[#3b82f6]"
      :aria-expanded="open"
      :title="open ? `收起${item.menuName}` : `展开${item.menuName}`"
      @click="toggle"
    >
      <el-icon class="shrink-0">
        <component :is="resolveMenuIcon(item.icon)" />
      </el-icon>
      <span class="truncate">{{ item.menuName }}</span>
      <el-icon
        class="ml-auto shrink-0 text-[10px] text-[#6b7280] transition-transform duration-200 group-hover:text-[#cbd5e1]"
        :class="open ? 'rotate-90' : ''"
      >
        <ArrowRight />
      </el-icon>
    </button>

    <ElCollapseTransition>
      <div v-show="collapsed || open">
        <SidebarItem
          v-for="child in children"
          :key="child.id"
          :item="child"
          :collapsed="collapsed"
          :depth="depth + 1"
          @navigate="emit('navigate')"
        />
      </div>
    </ElCollapseTransition>
  </div>

  <router-link
    v-else
    :to="item.path"
    :title="collapsed ? item.menuName : undefined"
    class="group flex h-8 items-center gap-2 rounded px-2 text-xs transition-colors duration-150 focus-visible:ring-1 focus-visible:ring-[#3b82f6]"
    :class="[
      isActive
        ? 'bg-[#1f1f1f] font-medium text-[#e89aaf]'
        : 'text-[#cbd5e1] hover:bg-[#1a1a1a] hover:text-white',
      collapsed ? 'justify-center' : '',
    ]"
    :style="{ paddingLeft: collapsed ? undefined : `${8 + depth * 12}px` }"
    @click="emit('navigate')"
  >
    <el-icon class="shrink-0" :class="isActive ? 'text-[#e89aaf]' : 'text-[#94a3b8]'">
      <component :is="resolveMenuIcon(item.icon)" />
    </el-icon>
    <span v-if="!collapsed" class="truncate">{{ item.menuName }}</span>
  </router-link>
</template>
