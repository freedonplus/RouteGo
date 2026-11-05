<script setup>
import { computed } from 'vue'

const props = defineProps({
  /** [{ name, value, color }] */
  items: { type: Array, default: () => [] },
  totalLabel: { type: String, default: '总计' },
  unit: { type: String, default: '' },
})

const RADIUS = 54
const CIRCUMFERENCE = 2 * Math.PI * RADIUS

const total = computed(() =>
  props.items.reduce((sum, item) => sum + (Number(item.value) || 0), 0)
)

const segments = computed(() => {
  let offset = 0
  return props.items.map((item) => {
    const value = Number(item.value) || 0
    const fraction = total.value > 0 ? value / total.value : 0
    const segment = {
      ...item,
      percent: total.value > 0 ? Math.round((value / total.value) * 1000) / 10 : 0,
      dash: `${fraction * CIRCUMFERENCE} ${CIRCUMFERENCE}`,
      offset: -offset,
    }
    offset += fraction * CIRCUMFERENCE
    return segment
  })
})
</script>

<template>
  <div class="flex flex-col items-center gap-3 px-3 py-3 sm:flex-row sm:items-center">
    <div class="relative shrink-0">
      <svg viewBox="0 0 140 140" class="h-32 w-32">
        <circle cx="70" cy="70" :r="RADIUS" fill="none" stroke="#f1f5f9" stroke-width="14" />
        <g transform="rotate(-90 70 70)">
          <circle
            v-for="seg in segments" :key="seg.name"
            cx="70" cy="70" :r="RADIUS" fill="none"
            :stroke="seg.color" stroke-width="14" stroke-linecap="butt"
            :stroke-dasharray="seg.dash" :stroke-dashoffset="seg.offset"
          />
        </g>
        <text x="70" y="66" text-anchor="middle" class="fill-[#121212]" font-size="18" font-weight="600">
          {{ total }}
        </text>
        <text x="70" y="82" text-anchor="middle" class="fill-[#94a3b8]" font-size="9">
          {{ totalLabel }}{{ unit }}
        </text>
      </svg>
    </div>
    <ul class="min-w-0 flex-1 space-y-1.5">
      <li v-for="seg in segments" :key="seg.name" class="flex items-center gap-2">
        <span class="h-2 w-2 shrink-0 rounded-sm" :style="{ background: seg.color }"></span>
        <span class="min-w-0 flex-1 truncate text-[11px] text-[#475569]">{{ seg.name }}</span>
        <span class="font-mono text-[11px] font-medium text-[#1e293b]">{{ seg.value }}</span>
        <span class="w-10 text-right font-mono text-[10px] text-[#94a3b8]">{{ seg.percent }}%</span>
      </li>
    </ul>
  </div>
</template>
