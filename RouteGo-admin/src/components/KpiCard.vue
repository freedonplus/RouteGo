<script setup>
import { computed } from 'vue'

const props = defineProps({
  label: { type: String, required: true },
  value: { type: [Number, String], default: 0 },
  unit: { type: String, default: '' },
  delta: { type: Number, default: null },
  hint: { type: String, default: '' },
  invert: { type: Boolean, default: false },
  /** 可选图标组件（el-icon 内使用） */
  icon: { type: [Object, Function], default: null },
  /** 可选迷你趋势数据（数字数组） */
  spark: { type: Array, default: () => [] },
  sparkColor: { type: String, default: '#a8435f' },
})

const hasDelta = computed(() => props.delta !== null && props.delta !== undefined)
const positive = computed(() =>
  props.invert ? Number(props.delta) <= 0 : Number(props.delta) >= 0
)
const deltaText = computed(() => {
  const value = Math.abs(Number(props.delta) || 0)
  return Number.isInteger(value) ? String(value) : value.toFixed(1)
})

const SPARK_W = 100
const SPARK_H = 22

const sparkPoints = computed(() => {
  const data = props.spark.filter((n) => Number.isFinite(Number(n)))
  if (data.length < 2) return ''
  const min = Math.min(...data)
  const max = Math.max(...data)
  const span = max - min || 1
  return data
    .map((n, i) => {
      const x = (SPARK_W * i) / (data.length - 1)
      const y = SPARK_H - ((Number(n) - min) / span) * (SPARK_H - 4) - 2
      return `${x.toFixed(1)},${y.toFixed(1)}`
    })
    .join(' ')
})
</script>

<template>
  <div class="rg-card flex min-w-0 flex-col px-3 py-2.5">
    <div class="flex items-start justify-between gap-2">
      <p class="rg-label">{{ label }}</p>
      <span
        v-if="icon"
        class="flex h-6 w-6 shrink-0 items-center justify-center rounded"
        :style="{ background: `${sparkColor}14`, color: sparkColor }"
      >
        <el-icon :size="13"><component :is="icon" /></el-icon>
      </span>
    </div>
    <div class="mt-1 flex items-baseline gap-1">
      <span class="font-mono text-lg font-semibold leading-6 text-[#121212]">{{ value }}</span>
      <span v-if="unit" class="text-[10px] text-[#94a3b8]">{{ unit }}</span>
      <span
        v-if="hasDelta"
        class="ml-auto inline-flex items-center gap-0.5 font-mono text-[10px]"
        :class="positive ? 'text-[#15803d]' : 'text-[#b91c1c]'"
      >
        <span aria-hidden="true">{{ positive ? '↑' : '↓' }}</span>
        {{ deltaText }}
      </span>
    </div>
    <svg v-if="sparkPoints" viewBox="0 0 100 22" class="mt-1.5 h-6 w-full" preserveAspectRatio="none">
      <polyline
        :points="sparkPoints"
        fill="none"
        :stroke="sparkColor"
        stroke-width="1.6"
        stroke-linejoin="round"
        stroke-linecap="round"
        vector-effect="non-scaling-stroke"
      />
    </svg>
    <p v-if="hint" class="mt-0.5 text-[10px] text-[#94a3b8]" :class="{ 'mt-1': sparkPoints }">
      {{ hint }}
    </p>
  </div>
</template>
