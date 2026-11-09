<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  /** [{ label, a, b }] */
  data: { type: Array, default: () => [] },
  nameA: { type: String, default: '系列 A' },
  nameB: { type: String, default: '' },
  colorA: { type: String, default: '#a8435f' },
  colorB: { type: String, default: '#3b82f6' },
  height: { type: Number, default: 220 },
})

const W = 680
const H = 240
const PAD = { top: 16, right: 14, bottom: 26, left: 40 }

const hoverIndex = ref(-1)
const svgRef = ref(null)

const maxValue = computed(() => {
  let max = 0
  props.data.forEach((item) => {
    max = Math.max(max, Number(item.a) || 0)
    if (props.nameB) max = Math.max(max, Number(item.b) || 0)
  })
  return Math.max(max, 1)
})

const niceMax = computed(() => {
  const raw = maxValue.value
  const magnitude = 10 ** Math.floor(Math.log10(raw))
  return Math.ceil(raw / magnitude) * magnitude
})

const plot = computed(() => ({
  x: PAD.left,
  y: PAD.top,
  w: W - PAD.left - PAD.right,
  h: H - PAD.top - PAD.bottom,
}))

function px(index) {
  const n = props.data.length
  if (n <= 1) return plot.value.x + plot.value.w / 2
  return plot.value.x + (plot.value.w * index) / (n - 1)
}

function py(value) {
  return plot.value.y + plot.value.h * (1 - value / niceMax.value)
}

const lineA = computed(() =>
  props.data.map((item, i) => `${px(i)},${py(item.a)}`).join(' ')
)
const lineB = computed(() =>
  props.data.map((item, i) => `${px(i)},${py(item.b)}`).join(' ')
)
const areaA = computed(() => {
  if (!props.data.length) return ''
  const first = props.data[0]
  return `M${px(0)},${py(first.a)} L${lineA.value} L${px(props.data.length - 1)},${py(0)} L${px(0)},${py(0)} Z`
})

const gridLines = computed(() => [0, 0.25, 0.5, 0.75, 1].map((ratio) => ({
  y: plot.value.y + plot.value.h * ratio,
  value: Math.round(niceMax.value * (1 - ratio)),
})))

const xLabels = computed(() => {
  const n = props.data.length
  const step = Math.max(1, Math.ceil(n / 6))
  return props.data
    .map((item, i) => ({ ...item, x: px(i), show: i % step === 0 || i === n - 1 }))
    .filter((item) => item.show)
})

const hoverPoint = computed(() => {
  if (hoverIndex.value < 0 || !props.data[hoverIndex.value]) return null
  const item = props.data[hoverIndex.value]
  return {
    ...item,
    x: px(hoverIndex.value),
    yA: py(item.a),
    yB: py(item.b),
  }
})

const tooltipStyle = computed(() => {
  if (!hoverPoint.value) return {}
  const ratio = hoverPoint.value.x / W
  return {
    left: `${Math.min(Math.max(ratio * 100, 12), 88)}%`,
  }
})

function onMove(event) {
  const svg = svgRef.value
  if (!svg || !props.data.length) return
  const rect = svg.getBoundingClientRect()
  const x = ((event.clientX - rect.left) / rect.width) * W
  const n = props.data.length
  const step = plot.value.w / Math.max(1, n - 1)
  hoverIndex.value = Math.min(n - 1, Math.max(0, Math.round((x - plot.value.x) / step)))
}

function onLeave() {
  hoverIndex.value = -1
}
</script>

<template>
  <div class="relative w-full" :style="{ height: `${height}px` }">
    <svg
      ref="svgRef"
      :viewBox="`0 0 ${W} ${H}`"
      class="block h-full w-full"
      @mousemove="onMove"
      @mouseleave="onLeave"
    >
      <defs>
        <linearGradient id="trend-area" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stop-color="#a8435f" stop-opacity="0.22" />
          <stop offset="100%" stop-color="#a8435f" stop-opacity="0.02" />
        </linearGradient>
      </defs>

      <!-- 网格与 Y 轴刻度 -->
      <g v-for="line in gridLines" :key="line.y">
        <line
          :x1="plot.x" :x2="plot.x + plot.w" :y1="line.y" :y2="line.y"
          stroke="#e2e8f0" stroke-width="1"
          :stroke-dasharray="line.value === 0 ? 'none' : '3 4'"
        />
        <text
          :x="plot.x - 6" :y="line.y + 3"
          text-anchor="end" class="fill-[#94a3b8]" font-size="9"
        >
          {{ line.value }}
        </text>
      </g>

      <!-- 面积 + 折线 -->
      <path :d="areaA" fill="url(#trend-area)" />
      <polyline :points="lineA" fill="none" :stroke="colorA" stroke-width="1.8" stroke-linejoin="round" />
      <polyline
        v-if="nameB"
        :points="lineB" fill="none" :stroke="colorB" stroke-width="1.5"
        stroke-linejoin="round" stroke-dasharray="4 3"
      />

      <!-- X 轴标签 -->
      <text
        v-for="item in xLabels" :key="item.label"
        :x="item.x" :y="H - 8"
        text-anchor="middle" class="fill-[#94a3b8]" font-size="9"
      >
        {{ item.label }}
      </text>

      <!-- 悬停指示 -->
      <g v-if="hoverPoint">
        <line
          :x1="hoverPoint.x" :x2="hoverPoint.x"
          :y1="plot.y" :y2="plot.y + plot.h"
          stroke="#a8435f" stroke-width="1" stroke-dasharray="2 3" opacity="0.6"
        />
        <circle :cx="hoverPoint.x" :cy="hoverPoint.yA" r="3.5" :fill="colorA" stroke="#fff" stroke-width="1.5" />
        <circle
          v-if="nameB" :cx="hoverPoint.x" :cy="hoverPoint.yB"
          r="3" :fill="colorB" stroke="#fff" stroke-width="1.5"
        />
      </g>
    </svg>

    <!-- 悬停浮层 -->
    <div
      v-if="hoverPoint"
      class="pointer-events-none absolute top-2 z-10 -translate-x-1/2 rounded border border-[#e2e8f0] bg-white/95 px-2 py-1.5 shadow-sm"
      :style="tooltipStyle"
    >
      <p class="font-mono text-[10px] text-[#94a3b8]">{{ hoverPoint.label }}</p>
      <p class="mt-0.5 flex items-center gap-1 text-[11px] text-[#1e293b]">
        <span class="h-1.5 w-1.5 rounded-full" :style="{ background: colorA }"></span>
        {{ nameA }}
        <span class="ml-1 font-mono font-semibold">{{ hoverPoint.a }}</span>
      </p>
      <p v-if="nameB" class="mt-0.5 flex items-center gap-1 text-[11px] text-[#1e293b]">
        <span class="h-1.5 w-1.5 rounded-full" :style="{ background: colorB }"></span>
        {{ nameB }}
        <span class="ml-1 font-mono font-semibold">{{ hoverPoint.b }}</span>
      </p>
    </div>
  </div>
</template>
