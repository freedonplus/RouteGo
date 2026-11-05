<script setup>
import { ref, watch } from 'vue'

/**
 * 远程搜索下拉：用于线路 / 会员 / 后台用户这类数据量大、不能一次性拉全的关联选择。
 * fetcher 需返回数组或 { records }，formatter 决定展示文案。
 */
const props = defineProps({
  modelValue: { type: [Number, String], default: null },
  fetcher: { type: Function, required: true },
  formatter: { type: Function, default: (row) => row.name || row.title || row.id },
  placeholder: { type: String, default: '输入关键字搜索' },
  initialLabel: { type: String, default: '' },
  clearable: { type: Boolean, default: true },
  disabled: { type: Boolean, default: false },
})
const emit = defineEmits(['update:modelValue', 'change'])

const options = ref([])
const loading = ref(false)

function toRows(result) {
  if (Array.isArray(result)) return result
  return result?.records || []
}

async function search(keyword) {
  loading.value = true
  try {
    const rows = toRows(await props.fetcher(keyword || ''))
    options.value = rows.map((row) => ({ value: row.id, label: props.formatter(row), raw: row }))
    // 已选值不在结果里时补一条，避免下拉框显示成裸 ID
    if (
      props.modelValue !== null &&
      props.modelValue !== undefined &&
      !options.value.some((item) => item.value === props.modelValue)
    ) {
      options.value.unshift({
        value: props.modelValue,
        label: props.initialLabel || `#${props.modelValue}`,
        raw: null,
      })
    }
  } catch {
    options.value = []
  } finally {
    loading.value = false
  }
}

watch(
  () => props.modelValue,
  (value) => {
    if (value === null || value === undefined) return
    if (options.value.some((item) => item.value === value)) return
    if (!props.initialLabel) return
    options.value = [{ value, label: props.initialLabel, raw: null }, ...options.value]
  },
  { immediate: true }
)

function onChange(value) {
  const hit = options.value.find((item) => item.value === value)
  emit('change', hit?.raw || null)
}

function onVisible(open) {
  if (open && options.value.length === 0) search('')
}
</script>

<template>
  <el-select
    :model-value="modelValue"
    filterable
    remote
    :remote-method="search"
    :loading="loading"
    :placeholder="placeholder"
    :clearable="clearable"
    :disabled="disabled"
    class="w-full"
    @update:model-value="emit('update:modelValue', $event)"
    @change="onChange"
    @visible-change="onVisible"
  >
    <el-option v-for="item in options" :key="item.value" :label="item.label" :value="item.value" />
  </el-select>
</template>
