<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
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
</script>

<template>
  <div v-if="item.menuType === 'M'">
    <p
      v-if="!collapsed"
      class="px-2 pb-1 pt-2 text-[10px] font-medium uppercase tracking-wide text-[#6b7280]"
    >
      {{ item.menuName }}
    </p>
    <SidebarItem
      v-for="child in children"
      :key="child.id"
      :item="child"
      :collapsed="collapsed"
      :depth="depth + 1"
      @navigate="emit('navigate')"
    />
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
