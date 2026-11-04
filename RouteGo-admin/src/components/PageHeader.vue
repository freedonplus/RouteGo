<script setup>
defineProps({
  title: { type: String, required: true },
  description: { type: String, default: '' },
})
</script>

<template>
  <div
    class="flex flex-wrap items-center justify-between gap-2 border-b border-[#e2e8f0] bg-white px-4 py-2.5"
  >
    <div class="min-w-0">
      <h1 class="truncate text-sm font-semibold tracking-tight text-[#121212]">{{ title }}</h1>
      <p v-if="description" class="mt-0.5 truncate text-[11px] text-[#64748b]">
        {{ description }}
      </p>
    </div>
    <div class="flex shrink-0 items-center gap-1">
      <slot name="actions" />
    </div>
  </div>
</template>
