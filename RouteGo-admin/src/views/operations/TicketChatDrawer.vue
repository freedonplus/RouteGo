<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { Delete, Picture } from '@element-plus/icons-vue'
import { getTicket, listMessages, sendMessage, setTicketStatus } from '@/api/operations'
import { uploadFile } from '@/api/system'
import { formatDateTime, resolveFileUrl } from '@/utils/format'
import { playNewMessageSound } from '@/utils/notice'
import StatusTag from '@/components/StatusTag.vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  ticketId: { type: [Number, String], default: null },
  /** 父级推送中心转来的最近一条事件，命中当前工单就立刻拉增量 */
  signal: { type: Object, default: null },
})
const emit = defineEmits(['update:modelValue', 'changed'])

const MAX_IMAGES = 9
/** 兜底轮询：正常情况下消息由 SSE 推送即时触发，这里只在推送断开时兜住 */
const POLL_INTERVAL = 15000

const loading = ref(false)
const sending = ref(false)
const uploading = ref(false)
const ticket = ref(null)
const messages = ref([])
const text = ref('')
const pendingImages = ref([])
const listRef = ref(null)
/** 本次打开后新收进来的客户消息数，用于顶部提示与「回到最新」气泡 */
const newIncoming = ref(0)
/** 不在底部时浮出「有新消息」按钮，避免客服看历史记录时被打断 */
const showJump = ref(false)
let pollTimer = null

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
const canSend = computed(() => canReply.value && (text.value.trim().length > 0 || pendingImages.value.length > 0))

async function scrollToBottom() {
  await nextTick()
  const el = listRef.value
  if (el) el.scrollTop = el.scrollHeight
}

function nearBottom() {
  const el = listRef.value
  if (!el) return true
  return el.scrollHeight - el.scrollTop - el.clientHeight < 80
}

async function load() {
  if (!props.ticketId) return
  loading.value = true
  try {
    const [detail, list] = await Promise.all([getTicket(props.ticketId), listMessages(props.ticketId)])
    ticket.value = detail
    messages.value = list || []
    newIncoming.value = 0
    showJump.value = false
    await scrollToBottom()
  } catch (error) {
    ElMessage.error(error.message || '工单加载失败')
    ticket.value = null
    messages.value = []
  } finally {
    loading.value = false
  }
}

/** 滚动位置变化时决定要不要浮出「有新消息」按钮 */
function onScroll() {
  if (nearBottom()) {
    showJump.value = false
  }
}

async function jumpToLatest() {
  await scrollToBottom()
  newIncoming.value = 0
  showJump.value = false
}

function countCustomerMessages(list) {
  return list.filter((item) => Number(item.senderType) === 1).length
}

/** 轮询增量：只在有新消息时重绘，避免把客服正在滚动的位置顶走 */
async function pollNewMessages() {
  if (!props.ticketId || loading.value || sending.value || uploading.value) return
  try {
    const list = (await listMessages(props.ticketId)) || []
    const lastOld = messages.value[messages.value.length - 1]?.id
    const lastNew = list[list.length - 1]?.id
    if (list.length === messages.value.length && lastOld === lastNew) return
    const stick = nearBottom()
    // 只把「客户新说的话」算作提示：自己刚发的回复、系统留痕不响铃
    const incoming = Math.max(0, countCustomerMessages(list) - countCustomerMessages(messages.value))
    messages.value = list
    if (incoming > 0) {
      playNewMessageSound()
      newIncoming.value += incoming
      if (!stick) showJump.value = true
    }
    if (stick) await scrollToBottom()
  } catch {
    // 轮询失败不打扰客服，下一轮会补上
  }
}

function startPolling() {
  stopPolling()
  pollTimer = setInterval(pollNewMessages, POLL_INTERVAL)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

watch(
  () => props.signal,
  (payload) => {
    if (!props.modelValue || !payload) return
    // 推送是全客服广播，只有轮到自己正开着的这张工单才拉
    if (String(payload.ticketId) !== String(props.ticketId)) return
    pollNewMessages()
  }
)

watch(
  () => props.modelValue,
  (open) => {
    if (!open) {
      stopPolling()
      return
    }
    ticket.value = null
    messages.value = []
    text.value = ''
    pendingImages.value = []
    newIncoming.value = 0
    showJump.value = false
    load()
    startPolling()
  }
)

onBeforeUnmount(stopPolling)

/** 选图即上传：先把文件换成签名地址，发送时只提交地址 */
async function handleUpload(options) {
  if (pendingImages.value.length >= MAX_IMAGES) {
    ElMessage.warning(`单条消息最多 ${MAX_IMAGES} 张图片`)
    return
  }
  uploading.value = true
  try {
    const file = await uploadFile(options.file)
    pendingImages.value.push({ id: file.id, url: file.previewUrl })
  } catch (error) {
    ElMessage.error(error.message || '图片上传失败')
  } finally {
    uploading.value = false
  }
}

function removeImage(index) {
  pendingImages.value.splice(index, 1)
}

async function submit() {
  if (!canSend.value) return
  const content = text.value.trim()
  sending.value = true
  try {
    await sendMessage({
      ticketId: props.ticketId,
      content,
      images: pendingImages.value.map((item) => item.url),
    })
    text.value = ''
    pendingImages.value = []
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

/** 只发图片时后端存的是空文本，这里不渲染空行 */
function hasText(message) {
  return Boolean(message.content && message.content.trim())
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
          <span
            v-if="Number(ticket.priority) === 3"
            class="rounded bg-[#fef2f2] px-1.5 py-0.5 text-[10px] font-medium text-[#b91c1c] ring-1 ring-[#fecaca]"
          >
            高优先级
          </span>
          <span
            v-if="ticket.assignRuleId"
            class="rounded bg-[#eef6ff] px-1.5 py-0.5 text-[10px] text-[#1d4ed8]"
            title="由自动分配规则分给当前处理人"
          >
            自动分配
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

        <div class="relative min-h-0 flex-1">
          <div
            ref="listRef"
            class="h-full min-h-[240px] space-y-2 overflow-y-auto rounded border border-[#e2e8f0] p-2"
            @scroll="onScroll"
          >
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
                class="rounded px-2 py-1.5 text-[11px] leading-5"
                :class="Number(message.senderType) === 2
                  ? 'bg-[#fdf1f5] text-[#7c2d45]'
                  : 'bg-[#f1f5f9] text-[#1e293b]'"
              >
                <p v-if="hasText(message)" class="whitespace-pre-line">{{ message.content }}</p>
                <div v-if="imageList(message).length" class="mt-1 flex flex-wrap gap-1" :class="hasText(message) ? '' : 'mt-0'">
                  <el-image
                    v-for="(url, index) in imageList(message)"
                    :key="index"
                    :src="resolveFileUrl(url)"
                    fit="cover"
                    class="h-16 w-16 cursor-zoom-in rounded ring-1 ring-black/5"
                    :preview-src-list="imageList(message).map((item) => resolveFileUrl(item))"
                    :initial-index="index"
                    preview-teleported
                    lazy
                  />
                </div>
              </div>
            </div>
          </div>
          </div>

          <!-- 客服正在翻历史时飘一条提示，不强行把视图拽到底部 -->
          <div
            v-if="newIncoming > 0"
            class="pointer-events-none absolute inset-x-0 bottom-2 flex justify-center"
          >
            <button
              class="pointer-events-auto flex items-center gap-1 rounded-full bg-[#1d4ed8] px-3 py-1 text-[11px] font-medium text-white shadow-lg transition hover:bg-[#1e40af]"
              @click="jumpToLatest"
            >
              客户发来 {{ newIncoming }} 条新消息
              <span aria-hidden="true">↓</span>
            </button>
          </div>
        </div>

        <div class="mt-2">
          <div v-if="pendingImages.length" class="mb-1 flex flex-wrap gap-1">
            <div v-for="(image, index) in pendingImages" :key="image.url" class="relative">
              <el-image
                :src="resolveFileUrl(image.url)"
                fit="cover"
                class="h-14 w-14 rounded border border-[#e2e8f0]"
                :preview-src-list="[resolveFileUrl(image.url)]"
                preview-teleported
              />
              <el-button
                class="!absolute -right-1 -top-1"
                size="small"
                circle
                :icon="Delete"
                @click="removeImage(index)"
              />
            </div>
          </div>
          <el-input
            v-model="text"
            type="textarea"
            :rows="3"
            maxlength="5000"
            :disabled="!canReply"
            :placeholder="canReply ? '输入回复内容，Ctrl + Enter 发送；可直接粘贴或选择图片' : '工单已关闭，无法回复'"
            @keydown.ctrl.enter.prevent="submit"
          />
          <div class="mt-1 flex items-center justify-between">
            <div class="flex items-center gap-2">
              <el-upload
                :show-file-list="false"
                :http-request="handleUpload"
                :disabled="!canReply || pendingImages.length >= MAX_IMAGES"
                accept="image/png,image/jpeg,image/webp"
                multiple
              >
                <el-button size="small" :icon="Picture" :loading="uploading" :disabled="!canReply">
                  图片
                </el-button>
              </el-upload>
              <span class="text-[10px] text-[#94a3b8]">
                {{ pendingImages.length }}/{{ MAX_IMAGES }} · 支持 jpg/png/webp，单张 5MB 内
              </span>
            </div>
            <el-button
              v-perm="'operations:CsMessage:create'"
              size="small"
              type="primary"
              :loading="sending"
              :disabled="!canSend"
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
