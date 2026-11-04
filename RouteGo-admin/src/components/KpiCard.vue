<script setup>
import { computed } from 'vue'

const props = defineProps({
  label: { type: String, required: true },
  value: { type: [Number, String], default: 0 },
  unit: { type: String, default: '' },
  delta: { type: Number, default: null },
  hint: { type: String, default: '' },
  invert: { type: Boolean, default: false },
})

const hasDelta = computed(() => props.delta !== null && props.delta !== undefined)
const positive = computed(() =>
  props.invert ? Number(props.delta) <= 0 : Number(props.delta) >= 0
)
const deltaText = computed(() => {
  const value = Math.abs(Number(props.delta) || 0)
  return Number.isInteger(value) ? String(value) : value.toFixed(1)
})
</script>

<template>
  <div class="rg-card px-3 py-2.5">
    <p class="rg-label">{{ label }}</p>
    <div class="mt-1 flex items-baseline gap-1">
      <span class="font-mono text-base font-semibold leading-5 text-[#121212]">{{ value }}</span>
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
    <p v-if="hint" class="mt-0.5 text-[10px] text-[#94a3b8]">{{ hint }}</p>
  </div>
</template>
