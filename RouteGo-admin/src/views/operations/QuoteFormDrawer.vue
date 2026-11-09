<script setup>
import { computed, reactive, ref, watch } from 'vue'
import {
  createQuote,
  getQuote,
  pageRequirements,
  updateQuote,
} from '@/api/operations'
import RemotePicker from '@/components/RemotePicker.vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  requirementId: { type: [Number, String], default: null },
  quoteId: { type: [Number, String], default: null },
})
const emit = defineEmits(['update:modelValue', 'saved'])

const formRef = ref()
const saving = ref(false)
const requirementLabel = ref('')
const isEdit = computed(() => Boolean(props.quoteId))

const form = reactive({
  requirementId: null,
  amount: null,
  detail: '',
  validUntil: '',
})

const rules = {
  requirementId: [{ required: true, message: '请选择需求单', trigger: 'change' }],
  amount: [{ required: true, message: '请填写报价金额', trigger: 'blur' }],
}

function fetchRequirements(keyword) {
  return pageRequirements({ keyword: keyword || undefined, pageNum: 1, pageSize: 20 })
}

watch(
  () => props.modelValue,
  async (open) => {
    if (!open) return
    Object.assign(form, { requirementId: props.requirementId || null, amount: null, detail: '', validUntil: '' })
    requirementLabel.value = ''
    formRef.value?.clearValidate()
    if (!props.quoteId) return
    try {
      const detail = await getQuote(props.quoteId)
      requirementLabel.value = detail.requirementNo || ''
      Object.assign(form, {
        requirementId: detail.requirementId,
        amount: Number(detail.amount),
        detail: detail.detail || '',
        validUntil: detail.validUntil || '',
      })
    } catch (error) {
      ElMessage.error(error.message || '报价加载失败')
    }
  }
)

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  const payload = { ...form }
  if (payload.validUntil === '') payload.validUntil = null
  try {
    if (isEdit.value) {
      await updateQuote(props.quoteId, { amount: payload.amount, detail: payload.detail, validUntil: payload.validUntil })
      ElMessage.success('报价已更新')
    } else {
      await createQuote(payload)
      ElMessage.success('报价已提交')
    }
    emit('saved')
    emit('update:modelValue', false)
  } catch (error) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    :title="isEdit ? '修改报价' : '新建报价'"
    width="520px"
    append-to-body
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="small">
      <el-form-item v-if="!requirementId" label="需求单" prop="requirementId">
        <RemotePicker
          v-model="form.requirementId"
          :fetcher="fetchRequirements"
          :formatter="(row) => `${row.requirementNo} · ${row.destination}`"
          :initial-label="requirementLabel"
          placeholder="搜索需求单号/目的地"
        />
      </el-form-item>
      <div class="grid grid-cols-2 gap-2">
        <el-form-item label="报价金额（元）" prop="amount">
          <el-input-number v-model="form.amount" :min="0.01" :precision="2" :step="500" class="w-full" controls-position="right" />
        </el-form-item>
        <el-form-item label="有效期至">
          <el-date-picker v-model="form.validUntil" type="date" value-format="YYYY-MM-DD" class="!w-full" placeholder="可留空" />
        </el-form-item>
      </div>
      <el-form-item label="报价明细">
        <el-input v-model="form.detail" type="textarea" :rows="5" placeholder="含大交通、住宿标准、门票、用车、餐食等" />
      </el-form-item>
      <p class="text-[10px] leading-4 text-[#94a3b8]">
        报价后需求自动进入「已报价」；采纳一版后，同需求其他待确认报价会全部失效。
      </p>
    </el-form>
    <template #footer>
      <div class="flex justify-end gap-1">
        <el-button size="small" @click="emit('update:modelValue', false)">取消</el-button>
        <el-button size="small" type="primary" :loading="saving" @click="submit">
          {{ isEdit ? '保存修改' : '提交报价' }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>
