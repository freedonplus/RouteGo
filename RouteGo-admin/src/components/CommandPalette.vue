<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { navItems } from '@/utils/menu'
import { resolveMenuIcon } from './menu-icons'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
})
const emit = defineEmits(['update:modelValue'])

const auth = useAuthStore()
const router = useRouter()
const query = ref('')
const active = ref(0)
const inputRef = ref()

const items = computed(() => navItems(auth.menus))
const filtered = computed(() => {
  const keyword = query.value.trim().toLowerCase()
  if (!keyword) return items.value
  return items.value.filter(
    (item) =>
      item.menuName.toLowerCase().includes(keyword) ||
      String(item.path || '').toLowerCase().includes(keyword)
  )
})

watch(
  () => props.modelValue,
  async (open) => {
    if (!open) return
    query.value = ''
    active.value = 0
    await nextTick()
    inputRef.value?.focus()
  }
)

watch(filtered, () => {
  active.value = 0
})

function close() {
  emit('update:modelValue', false)
}

function choose(item) {
  if (!item) return
  router.push(item.path)
  close()
}

function onKeydown(event) {
  if (event.key === 'ArrowDown') {
    event.preventDefault()
    active.value = (active.value + 1) % Math.max(filtered.value.length, 1)
  } else if (event.key === 'ArrowUp') {
    event.preventDefault()
    active.value =
      (active.value - 1 + Math.max(filtered.value.length, 1)) % Math.max(filtered.value.length, 1)
  } else if (event.key === 'Enter') {
    event.preventDefault()
    choose(filtered.value[active.value])
  } else if (event.key === 'Escape') {
    close()
  }
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    width="520px"
    append-to-body
    :show-close="false"
    class="rg-command-dialog"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div class="flex items-center gap-2 border-b border-[#e2e8f0] px-3 py-2">
      <el-icon class="text-[#94a3b8]"><Search /></el-icon>
      <input
        ref="inputRef"
        v-model="query"
        class="h-6 w-full border-0 bg-transparent text-xs text-[#1e293b] outline-none placeholder:text-[#94a3b8]"
        placeholder="搜索页面，Enter 跳转"
        @keydown="onKeydown"
      />
      <kbd class="rounded border border-[#e2e8f0] px-1 font-mono text-[10px] text-[#94a3b8]">
        Esc
      </kbd>
    </div>

    <div class="max-h-[320px] overflow-y-auto py-1">
      <button
        v-for="(item, index) in filtered"
        :key="item.id"
        type="button"
        class="flex w-full items-center gap-2 px-3 py-2 text-left text-xs transition-colors duration-150"
        :class="
          index === active
            ? 'bg-[#fdf1f5] text-[#a8435f]'
            : 'text-[#1e293b] hover:bg-[#f8fafc]'
        "
        @mouseenter="active = index"
        @click="choose(item)"
      >
        <el-icon class="shrink-0"><component :is="resolveMenuIcon(item.icon)" /></el-icon>
        <span class="truncate">{{ item.menuName }}</span>
        <span class="ml-auto shrink-0 font-mono text-[10px] text-[#94a3b8]">{{ item.path }}</span>
      </button>
      <p v-if="!filtered.length" class="px-3 py-6 text-center text-xs text-[#94a3b8]">
        无匹配页面
      </p>
    </div>
  </el-dialog>
</template>
