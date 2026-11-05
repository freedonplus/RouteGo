<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTicket, listMessages, sendMessage, setTicketStatus } from '@/api/operations'
import { formatDateTime } from '@/utils/format'
import StatusTag from '@/components/StatusTag.vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  ticketId: { type: [Number, String], default: null },
})
const emit = defineEmits(['update:modelValue', 'changed'])

const loading = ref(false)
const sending = ref(false)
const ticket = ref(null)
const messages = ref([])
const text = ref('')
const listRef = ref(null)

const STATUS_LABELS = ['待处理', '处理中', '已解决', '已关闭']
const STATUS_TONES = ['warning', 'info', 'success', 'muted']

const NEXT_STATUS = {
  0: [{ value: 1, label: '开始处理' }],
  1: [{ value: 2, label: '标记解决' }],
  2: [
    { value: 1, label: '重新打开' },
    { value: 3, label: '关闭工单' },
  ],
  3: [],
}

const nextActions = computed(() => NEXT_STATUS[Number(ticket.value?.status)] || [])
const canReply = computed(() => ticket.value && Number(ticket.value.status) !== 3)

async function scrollToBottom() {
  await nextTick()
  const el = listRef.value
  if (el) el.scrollTop = el.scrollHeight
}

async function load() {
  if (!props.ticketId) return
  loading.value = true
  try {
    const [detail, list] = await Promise.all([getTicket(props.ticketId), listMessages(props.ticketId)])
    ticket.value = detail
    messages.value = list || []
    await scrollToBottom()
  } catch (error) {
    ElMessage.error(error.message || '工单加载失败')
    ticket.value = null
    messages.value = []
  } finally {
    loading.value = false
  }
}

watch(
  () => props.modelValue,
  (open) => {
    if (!open) return
    ticket.value = null
    messages.value = []
    text.value = ''
    load()
  }
)

async function submit() {
  const content = text.value.trim()
  if (!content) return
  sending.value = true
  try {
    await sendMessage({ ticketId: props.ticketId, content })
    text.value = ''
    await load()
    emit('changed')
  } catch (error) {
    ElMessage.error(error.message || '发送失败')
  } finally {
    sending.value = false
  }
}

async function changeStatus(status) {
  let remark = ''
  if (status === 2 || status === 3) {
    try {
      const { value } = await ElMessageBox.prompt(status === 2 ? '填写处理结果（选填）' : '填写关闭原因（选填）', '状态变更', {
        inputPlaceholder: '选填',
        confirmButtonText: '确定',
        cancelButtonText: '取消',
      })
      remark = value || ''
    } catch {
      return
    }
  }
  try {
    await setTicketStatus(props.ticketId, status, remark || undefined)
    ElMessage.success('状态已更新')
    await load()
    emit('changed')
  } catch (error) {
    ElMessage.error(error.message || '状态更新失败')
  }
}

function imageList(message) {
  return Array.isArray(message.images) ? message.images : []
}
</script>

<template>
  <el-drawer
    :model-value="modelValue"
    :title="ticket ? `${ticket.ticketNo} · ${ticket.title}` : '工单对话'"
    size="640px"
    append-to-body
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div v-loading="loading" class="flex h-full flex-col">
      <template v-if="ticket">
        <div class="mb-2 flex flex-wrap items-center gap-1">
          <StatusTag :value="ticket.status" :labels="STATUS_LABELS" :tone="STATUS_TONES[Number(ticket.status)]" />
          <span class="text-[10px] text-[#94a3b8]">
            {{ ticket.typeDesc }} · {{ ticket.priorityDesc }}优先级 · {{ ticket.handlerName || '未指派' }}
          </span>
          <div class="ml-auto flex gap-1">
            <el-button
              v-for="item in nextActions"
              :key="item.value"
              v-perm="'operations:CsTicket:updateStatus'"
              size="small"
              :type="item.value === 2 ? 'primary' : 'default'"
              @click="changeStatus(item.value)"
            >
              {{ item.label }}
            </el-button>
          </div>
        </div>

        <div class="mb-2 rounded bg-[#f8fafc] px-2 py-1.5 text-[11px] leading-5 text-[#475569]">
          <p class="whitespace-pre-line">{{ ticket.content }}</p>
          <p class="mt-1 font-mono text-[10px] text-[#94a3b8]">
            客户：{{ ticket.customerNickname || '未关联' }}
            <span v-if="ticket.customerPhone"> · {{ ticket.customerPhone }}</span>
            <span v-if="ticket.orderNo"> · 订单 {{ ticket.orderNo }}</span>
            <span> · 创建于 {{ formatDateTime(ticket.createTime) }}</span>
          </p>
        </div>

        <div ref="listRef" class="min-h-[240px] flex-1 space-y-2 overflow-y-auto rounded border border-[#e2e8f0] p-2">
          <el-empty v-if="!messages.length" description="还没有对话" :image-size="60" />
          <div
            v-for="message in messages"
            :key="message.id"
            class="flex"
            :class="Number(message.senderType) === 2 ? 'justify-end' : 'justify-start'"
          >
            <div class="max-w-[78%]">
              <p
                class="mb-0.5 text-[10px] text-[#94a3b8]"
                :class="Number(message.senderType) === 2 ? 'text-right' : ''"
              >
                {{ message.senderName || message.senderTypeDesc }} · {{ formatDateTime(message.createTime) }}
              </p>
              <div
                class="whitespace-pre-line rounded px-2 py-1.5 text-[11px] leading-5"
                :class="Number(message.senderType) === 2
                  ? 'bg-[#fdf1f5] text-[#7c2d45]'
                  : 'bg-[#f1f5f9] text-[#1e293b]'"
              >
                {{ message.content }}
                <div v-if="imageList(message).length" class="mt-1 flex flex-wrap gap-1">
                  <el-image
                    v-for="(url, index) in imageList(message)"
                    :key="index"
                    :src="url"
                    fit="cover"
                    class="h-14 w-14 rounded"
                    :preview-src-list="imageList(message)"
                    preview-teleported
                  />
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="mt-2">
          <el-input
            v-model="text"
            type="textarea"
            :rows="3"
            maxlength="5000"
            :disabled="!canReply"
            :placeholder="canReply ? '输入回复内容，Ctrl + Enter 发送' : '工单已关闭，无法回复'"
            @keydown.ctrl.enter.prevent="submit"
          />
          <div class="mt-1 flex items-center justify-between">
            <span class="text-[10px] text-[#94a3b8]">首次回复会把「待处理」工单自动转为「处理中」</span>
            <el-button
              v-perm="'operations:CsMessage:create'"
              size="small"
              type="primary"
              :loading="sending"
              :disabled="!canReply || !text.trim()"
              @click="submit"
            >
              发送回复
            </el-button>
          </div>
        </div>
      </template>
    </div>
  </el-drawer>
</template>
