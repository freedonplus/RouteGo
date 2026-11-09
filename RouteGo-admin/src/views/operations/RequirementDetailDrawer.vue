<script setup>
import { ref, watch } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import {
  acceptQuote,
  getRequirement,
  invalidateQuote,
  listQuotes,
  setRequirementStatus,
} from '@/api/operations'
import { formatDate, formatDateTime, formatMoney } from '@/utils/format'
import StatusTag from '@/components/StatusTag.vue'
import QuoteFormDrawer from './QuoteFormDrawer.vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  requirementId: { type: [Number, String], default: null },
})
const emit = defineEmits(['update:modelValue', 'changed'])

const loading = ref(false)
const acting = ref(false)
const detail = ref(null)
const quotes = ref([])

const quoteOpen = ref(false)
const quoteId = ref(null)

const STATUS_LABELS = ['待跟进', '已报价', '已成交', '已关闭']
const STATUS_TONES = ['warning', 'info', 'success', 'muted']
const QUOTE_STATUS_LABELS = ['待确认', '已采纳', '已失效']
const QUOTE_STATUS_TONES = ['warning', 'success', 'muted']

async function load() {
  if (!props.requirementId) return
  loading.value = true
  try {
    const [req, list] = await Promise.all([getRequirement(props.requirementId), listQuotes(props.requirementId)])
    detail.value = req
    quotes.value = list || []
  } catch (error) {
    ElMessage.error(error.message || '需求详情加载失败')
    detail.value = null
    quotes.value = []
  } finally {
    loading.value = false
  }
}

watch(
  () => props.modelValue,
  (open) => {
    if (!open) return
    detail.value = null
    quotes.value = []
    load()
  }
)

async function act(fn, text) {
  acting.value = true
  try {
    await fn()
    ElMessage.success(text)
    await load()
    emit('changed')
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    acting.value = false
  }
}

function openQuote(quote) {
  quoteId.value = quote?.id || null
  quoteOpen.value = true
}

async function accept(quote) {
  try {
    await ElMessageBox.confirm(
      `采纳后需求置为「已成交」，其他报价自动失效。确认采纳 ${formatMoney(quote.amount)} 这一版吗？`,
      '采纳报价',
      { type: 'warning', confirmButtonText: '确认采纳', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  await act(() => acceptQuote(quote.id), '已采纳报价')
}

async function invalidate(quote) {
  try {
    await ElMessageBox.confirm('作废后该报价不可再采纳，确认吗？', '作废报价', {
      type: 'warning',
      confirmButtonText: '作废',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  await act(() => invalidateQuote(quote.id), '报价已作废')
}

async function changeStatus(status) {
  let remark = ''
  try {
    const { value } = await ElMessageBox.prompt(
      status === 2 ? '线下成交说明（成交渠道/金额）' : '关闭原因（选填）',
      status === 2 ? '标记线下成交' : '关闭需求',
      { inputPlaceholder: '选填', confirmButtonText: '确定', cancelButtonText: '取消' }
    )
    remark = value || ''
  } catch {
    return
  }
  await act(() => setRequirementStatus(props.requirementId, status, remark || undefined), '状态已更新')
}
</script>

<template>
  <el-drawer
    :model-value="modelValue"
    :title="detail ? `需求 ${detail.requirementNo}` : '定制需求详情'"
    size="680px"
    append-to-body
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div v-loading="loading" class="space-y-2">
      <template v-if="detail">
        <div class="flex flex-wrap items-center gap-1">
          <StatusTag :value="detail.status" :labels="STATUS_LABELS" :tone="STATUS_TONES[Number(detail.status)]" />
          <span class="text-[11px] text-[#64748b]">{{ detail.handlerName || '未指派' }}</span>
          <div class="ml-auto flex gap-1">
            <el-button
              v-if="Number(detail.status) < 2"
              v-perm="'operations:CustomQuote:create'"
              size="small"
              type="primary"
              :icon="Plus"
              @click="openQuote(null)"
            >
              新增报价
            </el-button>
            <el-button v-if="Number(detail.status) < 2" v-perm="'operations:CustomRequirement:updateStatus'" size="small" @click="changeStatus(2)">
              线下成交
            </el-button>
            <el-button v-if="Number(detail.status) < 2" v-perm="'operations:CustomRequirement:updateStatus'" size="small" @click="changeStatus(3)">
              关闭
            </el-button>
          </div>
        </div>

        <el-descriptions :column="2" size="small" border>
          <el-descriptions-item label="联系人">{{ detail.contactName }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">
            <span class="font-mono">{{ detail.contactPhone || '—' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="目的地">{{ detail.destination }}</el-descriptions-item>
          <el-descriptions-item label="出行日期">{{ detail.departDate || '待定' }}</el-descriptions-item>
          <el-descriptions-item label="行程">{{ detail.days || '—' }} 天 · {{ detail.peopleCount || '—' }} 人</el-descriptions-item>
          <el-descriptions-item label="预算">{{ formatMoney(detail.budget) }}</el-descriptions-item>
          <el-descriptions-item label="关联会员">{{ detail.customerNickname || '未关联' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDateTime(detail.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="偏好与要求" :span="2">
            <span class="whitespace-pre-line">{{ detail.preferences || '—' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detail.remark || '—' }}</el-descriptions-item>
        </el-descriptions>

        <p class="rg-section-title">报价版本（{{ quotes.length }}）</p>
        <el-empty v-if="!quotes.length" description="还没有报价，点右上角新增" :image-size="60" />
        <div v-else class="space-y-2">
          <div v-for="quote in quotes" :key="quote.id" class="rounded border border-[#e2e8f0] p-2">
            <div class="flex items-start justify-between gap-2">
              <div class="min-w-0">
                <p class="flex items-center gap-1 text-[11px] font-medium text-[#1e293b]">
                  <span class="font-mono text-[#a8435f]">{{ formatMoney(quote.amount) }}</span>
                  <StatusTag :value="quote.status" :labels="QUOTE_STATUS_LABELS" :tone="QUOTE_STATUS_TONES[Number(quote.status)]" />
                  <span v-if="quote.expired" class="rounded bg-[#fef2f2] px-1 text-[9px] text-[#b91c1c]">已过期</span>
                </p>
                <p class="mt-0.5 font-mono text-[10px] text-[#94a3b8]">
                  {{ quote.quoteNo }} · {{ formatDateTime(quote.createTime) }}
                  <span v-if="quote.validUntil"> · 有效至 {{ formatDate(quote.validUntil) }}</span>
                </p>
                <p v-if="quote.detail" class="mt-1 whitespace-pre-line text-[11px] text-[#64748b]">{{ quote.detail }}</p>
              </div>
              <div class="flex shrink-0 flex-col items-end gap-0.5">
                <el-button
                  v-if="Number(quote.status) === 0 && Number(detail.status) !== 2"
                  v-perm="'operations:CustomQuote:accept'"
                  link
                  type="primary"
                  size="small"
                  :loading="acting"
                  @click="accept(quote)"
                >
                  采纳
                </el-button>
                <el-button
                  v-if="Number(quote.status) === 0"
                  v-perm="'operations:CustomQuote:update'"
                  link
                  size="small"
                  @click="openQuote(quote)"
                >
                  改价
                </el-button>
                <el-button
                  v-if="Number(quote.status) === 0"
                  v-perm="'operations:CustomQuote:invalidate'"
                  link
                  type="danger"
                  size="small"
                  @click="invalidate(quote)"
                >
                  作废
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>

    <QuoteFormDrawer v-model="quoteOpen" :requirement-id="requirementId" :quote-id="quoteId" @saved="load" />
  </el-drawer>
</template>
