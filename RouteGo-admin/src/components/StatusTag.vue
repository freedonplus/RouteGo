<script setup>
import { computed } from 'vue'

const props = defineProps({
  value: { type: [Number, String], default: 1 },
  type: { type: String, default: 'status' },
  labels: { type: Array, default: null },
  tone: { type: String, default: '' },
})

const variants = {
  status: { 1: ['启用', 'success'], 0: ['禁用', 'muted'] },
  result: { 1: ['成功', 'success'], 0: ['失败', 'danger'] },
  visible: { 1: ['显示', 'info'], 0: ['隐藏', 'muted'] },
  default: { 1: ['是', 'success'], 0: ['否', 'muted'] },
}

const state = computed(() => {
  const table = variants[props.type] || variants.default
  const key = Number(props.value)
  const [fallbackLabel, fallbackTone] = table[key] || ['未知', 'muted']
  const label = props.labels?.[key] || fallbackLabel
  const tone = props.tone || fallbackTone
  return { label, tone }
})
</script>

<template>
  <span class="rg-status" :class="`rg-status--${state.tone}`">{{ state.label }}</span>
</template>
