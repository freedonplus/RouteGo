<script setup>
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import { Bell, MuteNotification, Plus, Refresh, Search, Setting } from '@element-plus/icons-vue'
import {
  assignTicket,
  deleteTicket,
  getTicketStatistics,
  pageTickets,
  setTicketPriority,
  setTicketStatus,
} from '@/api/operations'
import { pageUsers } from '@/api/system'
import { formatDateTime } from '@/utils/format'
import { isSoundOn, playNewMessageSound, setSoundOn, setTitleBadge } from '@/utils/notice'
import { createCsStream } from '@/utils/cs-stream'
import PageHeader from '@/components/PageHeader.vue'
import FilterBar from '@/components/FilterBar.vue'
import StatusTag from '@/components/StatusTag.vue'
import KpiCard from '@/components/KpiCard.vue'
import RemotePicker from '@/components/RemotePicker.vue'
import TicketFormDrawer from './TicketFormDrawer.vue'
import TicketChatDrawer from './TicketChatDrawer.vue'
import CsAssignRuleDrawer from './CsAssignRuleDrawer.vue'

const loading = ref(false)
const rows = ref([])
const stats = ref(null)
const filters = reactive({
  ticketNo: '',
  keyword: '',
  type: '',
  priority: '',
  status: '',
  handlerId: null,
  unhandled: '',
  unread: '',
})
const timeRange = ref([])
const page = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const formOpen = ref(false)
const chatOpen = ref(false)
const chatTicketId = ref(null)
const assignRuleOpen = ref(false)

/** 全局待回复总数：决定是否响铃、标题角标与顶部横幅 */
const unreadTotal = ref(0)
const soundOn = ref(isSoundOn())
let statsReady = false

const assignOpen = ref(false)
const assignTarget = ref(null)
const assignHandlerId = ref(null)
const assigning = ref(false)

const TYPE_OPTIONS = [
  { value: 1, label: '售前咨询' },
  { value: 2, label: '订单问题' },
  { value: 3, label: '退款售后' },
  { value: 4, label: '投诉建议' },
  { value: 5, label: '其他' },
]
const STATUS_LABELS = ['待处理', '处理中', '已解决', '已关闭']
const STATUS_TONES = ['warning', 'info', 'success', 'muted']

/** 状态机：与后端 TicketStateMachine 保持一致 */
const NEXT_STATUS = {
  0: [{ value: 1, label: '开始处理' }],
  1: [{ value: 2, label: '标记解决' }],
  2: [
    { value: 1, label: '重新打开' },
    { value: 3, label: '关闭工单' },
  ],
  3: [],
}

function fetchUsers(keyword) {
  return pageUsers({ username: keyword || undefined, pageNum: 1, pageSize: 20 })
}

function typeLabel(type) {
  return TYPE_OPTIONS.find((item) => item.value === Number(type))?.label || '—'
}

/**
 * 拉统计的同时做提醒：未读总数比上次多就响一声并弹通知。
 * 首次加载只记录基线，避免刚进页面就为历史未读响铃。
 */
async function loadStats() {
  try {
    const data = await getTicketStatistics()
    const next = Number(data.staffUnread || 0)
    const prev = unreadTotal.value
    stats.value = data
    unreadTotal.value = next
    setTitleBadge(next)
    if (statsReady && next > prev) {
      playNewMessageSound()
      ElNotification({
        title: '收到新的客户消息',
        message: `新增 ${next - prev} 条，当前共 ${next} 条待回复`,
        type: 'warning',
        duration: 6000,
        onClick: () => showUnreadOnly(),
      })
    }
    statsReady = true
  } catch {
    stats.value = null
  }
}

/** 一键筛出待回复的工单 */
function showUnreadOnly() {
  filters.unread = true
  page.pageNum = 1
  load()
}

function clearUnreadFilter() {
  filters.unread = ''
  page.pageNum = 1
  load()
}

function toggleSound() {
  soundOn.value = !soundOn.value
  setSoundOn(soundOn.value)
  if (soundOn.value) playNewMessageSound()
}

/** 未读行整行标红，一眼能看出哪几行等着回 */
function rowClassName({ row }) {
  return Number(row.staffUnread) > 0 ? 'cs-row-unread' : ''
}

async function load() {
  loading.value = true
  try {
    const result = await pageTickets({
      ticketNo: filters.ticketNo || undefined,
      keyword: filters.keyword || undefined,
      type: filters.type === '' ? undefined : filters.type,
      priority: filters.priority === '' ? undefined : filters.priority,
      status: filters.status === '' ? undefined : filters.status,
      handlerId: filters.handlerId || undefined,
      unhandled: filters.unhandled === '' ? undefined : filters.unhandled,
      unread: filters.unread === '' ? undefined : filters.unread,
      createTimeStart: timeRange.value?.[0] ? `${timeRange.value[0]} 00:00:00` : undefined,
      createTimeEnd: timeRange.value?.[1] ? `${timeRange.value[1]} 23:59:59` : undefined,
      pageNum: page.pageNum,
      pageSize: page.pageSize,
    })
    rows.value = result.records || []
    page.total = Number(result.total || 0)
  } catch (error) {
    ElMessage.error(error.message || '工单加载失败')
    rows.value = []
    page.total = 0
  } finally {
    loading.value = false
  }
}

function search() {
  page.pageNum = 1
  load()
}

function resetFilters() {
  Object.assign(filters, {
    ticketNo: '',
    keyword: '',
    type: '',
    priority: '',
    status: '',
    handlerId: null,
    unhandled: '',
    unread: '',
  })
  timeRange.value = []
  page.pageNum = 1
  load()
}

function openChat(row) {
  chatTicketId.value = row.id
  chatOpen.value = true
}

function openAssign(row) {
  assignTarget.value = row
  assignHandlerId.value = row.handlerId || null
  assignOpen.value = true
}

async function submitAssign() {
  if (!assignHandlerId.value) {
    ElMessage.warning('请选择处理人')
    return
  }
  assigning.value = true
  try {
    await assignTicket(assignTarget.value.id, assignHandlerId.value)
    ElMessage.success('已指派')
    assignOpen.value = false
    load()
    loadStats()
  } catch (error) {
    ElMessage.error(error.message || '指派失败')
  } finally {
    assigning.value = false
  }
}

async function changeStatus(row, status) {
  const needRemark = status === 2 || status === 3
  let remark = ''
  if (needRemark) {
    try {
      const { value } = await ElMessageBox.prompt(
        status === 2 ? '可以填写处理结果，会作为一条客服消息留痕。' : '可以填写关闭原因。',
        status === 2 ? '标记已解决' : '关闭工单',
        { inputPlaceholder: '选填', confirmButtonText: '确定', cancelButtonText: '取消' }
      )
      remark = value || ''
    } catch {
      return
    }
  }
  try {
    await setTicketStatus(row.id, status, remark || undefined)
    ElMessage.success('状态已更新')
    load()
    loadStats()
  } catch (error) {
    ElMessage.error(error.message || '状态更新失败')
  }
}

async function changePriority(row, priority) {
  try {
    await setTicketPriority(row.id, priority)
    ElMessage.success('优先级已调整')
    load()
    loadStats()
  } catch (error) {
    ElMessage.error(error.message || '调整失败')
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确定删除工单 ${row.ticketNo} 吗？对话消息会一并清理。`, '删除工单', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  try {
    await deleteTicket(row.id)
    ElMessage.success('工单已删除')
    load()
    loadStats()
  } catch (error) {
    ElMessage.error(error.message || '删除失败')
  }
}

let stream = null
let fallbackTimer = null
let reloadTimer = null
/** connecting 建连中，online 实时推送，offline 断线重连，unsupported 环境不支持 */
const streamStatus = ref('connecting')
/** 最近一条推送事件，转给对话抽屉触发即时拉取 */
const chatSignal = ref(null)

/** 推送断了才启用的兜底轮询：30 秒一次，只为不漏消息，恢复推送后立刻停 */
function startFallbackPolling() {
  if (fallbackTimer) return
  fallbackTimer = setInterval(loadStats, 30000)
}

function stopFallbackPolling() {
  if (fallbackTimer) {
    clearInterval(fallbackTimer)
    fallbackTimer = null
  }
}

/** 推送可能连着来几条，列表刷新做 800ms 节流，避免把接口刷爆 */
function scheduleReload() {
  if (reloadTimer) return
  reloadTimer = setTimeout(() => {
    reloadTimer = null
    load()
  }, 800)
}

function applyUnread(raw) {
  const prev = unreadTotal.value
  const value = Number(raw || 0)
  unreadTotal.value = value
  setTitleBadge(value)
  // KPI 卡上的待回复数跟着推送走，不必再单独发一次统计请求
  if (stats.value) stats.value = { ...stats.value, staffUnread: value }
  return { prev, value }
}

function handleStreamEvent(kind, payload) {
  if (kind === 'hello') {
    applyUnread(payload.staffUnread)
    // 建连即拿到当前未读，等价于做了一次统计，后续不再为历史未读响铃
    statsReady = true
    return
  }

  const { prev, value } = applyUnread(payload.staffUnread)
  chatSignal.value = payload
  const fromCustomer = payload.type === 'message' && Number(payload.senderType) === 1
  // 正开着这个工单的对话时，抽屉里自己会提示，列表页不再重复响铃
  const inCurrentChat = chatOpen.value && String(payload.ticketId) === String(chatTicketId.value)

  if (fromCustomer && value > prev && !inCurrentChat) {
    playNewMessageSound()
    ElNotification({
      title: '收到新的客户消息',
      message: payload.preview
        ? `${payload.preview}（共 ${value} 条待回复）`
        : `当前共 ${value} 条待回复`,
      type: 'warning',
      duration: 6000,
      onClick: () => showUnreadOnly(),
    })
  }
  scheduleReload()
}

function startStream() {
  stream = createCsStream({
    onEvent: handleStreamEvent,
    onStatus: (status) => {
      streamStatus.value = status
      if (status === 'online') {
        stopFallbackPolling()
      } else {
        startFallbackPolling()
      }
    },
  })
}

onMounted(() => {
  loadStats()
  load()
  startStream()
})

onBeforeUnmount(() => {
  if (stream) stream.close()
  if (reloadTimer) {
    clearTimeout(reloadTimer)
    reloadTimer = null
  }
  stopFallbackPolling()
  setTitleBadge(0)
})
</script>

<template>
  <div class="pb-4">
    <PageHeader title="客服工单" description="0 待处理 → 1 处理中 → 2 已解决 → 3 已关闭；已关闭为终态">
      <template #actions>
        <span
          class="flex items-center gap-1 text-[11px] text-[#64748b]"
          :title="
            streamStatus === 'online'
              ? '推送长连接已建立，客户消息秒级到达'
              : '推送未连上，暂用 30 秒轮询兜底，恢复后自动切回'
          "
        >
          <span
            class="h-1.5 w-1.5 rounded-full"
            :class="
              streamStatus === 'online'
                ? 'bg-[#22c55e]'
                : streamStatus === 'connecting'
                  ? 'bg-[#f59e0b]'
                  : 'bg-[#ef4444]'
            "
          ></span>
          {{ streamStatus === 'online' ? '实时' : streamStatus === 'connecting' ? '连接中' : '轮询' }}
        </span>
        <el-button
          size="small"
          :icon="soundOn ? Bell : MuteNotification"
          :type="soundOn ? 'default' : 'info'"
          :title="soundOn ? '新消息提示音已开启，点击关闭' : '提示音已关闭，点击开启'"
          @click="toggleSound"
        >
          {{ soundOn ? '提示音开' : '提示音关' }}
        </el-button>
        <el-button
          v-perm="'operations:CsAssignRule:page'"
          size="small"
          :icon="Setting"
          @click="assignRuleOpen = true"
        >
          分配规则
        </el-button>
        <el-button size="small" :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
        <el-button v-perm="'operations:CsTicket:create'" size="small" type="primary" :icon="Plus" @click="formOpen = true">
          新建工单
        </el-button>
      </template>
    </PageHeader>

    <div class="rg-page space-y-2">
      <!-- 待回复横幅：比表里的角标更早被注意到，点一下直接筛出这些工单 -->
      <div
        v-if="unreadTotal > 0"
        class="flex flex-wrap items-center gap-2 rounded border border-[#fecaca] bg-[#fff5f5] px-3 py-2"
      >
        <span class="relative flex h-2.5 w-2.5 shrink-0">
          <span class="absolute inline-flex h-full w-full animate-ping rounded-full bg-[#ef4444] opacity-75"></span>
          <span class="relative inline-flex h-2.5 w-2.5 rounded-full bg-[#ef4444]"></span>
        </span>
        <span class="text-[12px] font-medium text-[#b91c1c]">
          客户有 {{ unreadTotal }} 条消息等待回复
        </span>
        <span class="text-[11px] text-[#ef4444]/80">每 15 秒自动刷新，有新消息会响铃提醒</span>
        <div class="ml-auto flex gap-1">
          <el-button size="small" type="danger" plain @click="showUnreadOnly">只看待回复</el-button>
          <el-button v-if="filters.unread === true" size="small" @click="clearUnreadFilter">取消筛选</el-button>
        </div>
      </div>

      <div v-if="stats" class="grid grid-cols-2 gap-2 md:grid-cols-4 lg:grid-cols-7">
        <KpiCard label="工单总数" :value="stats.total ?? 0" />
        <KpiCard label="待处理" :value="stats.pending ?? 0" hint="含未指派" />
        <KpiCard label="处理中" :value="stats.processing ?? 0" />
        <KpiCard label="已解决" :value="stats.resolved ?? 0" />
        <KpiCard label="未指派" :value="stats.unhandled ?? 0" hint="需要尽快认领" />
        <KpiCard label="待回复消息" :value="stats.staffUnread ?? 0" hint="客户已发、客服未读" />
        <KpiCard label="高优待处理" :value="stats.urgentPending ?? 0" hint="优先级为高" />
      </div>

      <FilterBar>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">工单号</span>
          <el-input v-model="filters.ticketNo" size="small" placeholder="TK..." clearable style="width: 140px" class="font-mono" @keyup.enter="search" />
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">关键字</span>
          <el-input v-model="filters.keyword" size="small" placeholder="标题/内容" clearable style="width: 140px" @keyup.enter="search" />
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">类型</span>
          <el-select v-model="filters.type" size="small" style="width: 104px">
            <el-option label="全部" value="" />
            <el-option v-for="item in TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">优先级</span>
          <el-select v-model="filters.priority" size="small" style="width: 88px">
            <el-option label="全部" value="" />
            <el-option label="低" :value="1" />
            <el-option label="中" :value="2" />
            <el-option label="高" :value="3" />
          </el-select>
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">状态</span>
          <el-select v-model="filters.status" size="small" style="width: 96px">
            <el-option label="全部" value="" />
            <el-option v-for="(label, index) in STATUS_LABELS" :key="index" :label="label" :value="index" />
          </el-select>
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">处理人</span>
          <div style="width: 150px">
            <RemotePicker
              v-model="filters.handlerId"
              :fetcher="fetchUsers"
              :formatter="(row) => row.nickname || row.username"
              placeholder="搜索客服"
            />
          </div>
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">认领状态</span>
          <el-select v-model="filters.unhandled" size="small" style="width: 100px">
            <el-option label="全部" value="" />
            <el-option label="仅未指派" :value="true" />
          </el-select>
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">待回复</span>
          <el-select v-model="filters.unread" size="small" style="width: 100px">
            <el-option label="全部" value="" />
            <el-option label="仅待回复" :value="true" />
          </el-select>
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">创建时间</span>
          <el-date-picker
            v-model="timeRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始"
            end-placeholder="结束"
            size="small"
            style="width: 224px"
          />
        </label>
        <template #actions>
          <el-button size="small" type="primary" :icon="Search" @click="search">查询</el-button>
          <el-button size="small" @click="resetFilters">重置</el-button>
        </template>
      </FilterBar>

      <div class="rg-card min-w-0">
        <div class="rg-scroll-x">
          <el-table
            v-loading="loading"
            :data="rows"
            size="small"
            border
            stripe
            row-key="id"
            empty-text="没有匹配的工单"
            :row-class-name="rowClassName"
          >
            <el-table-column label="工单" min-width="220">
              <template #default="{ row }">
                <div class="flex items-start gap-1">
                  <span
                    v-if="Number(row.priority) === 3"
                    class="mt-0.5 shrink-0 rounded bg-[#fef2f2] px-1 text-[9px] text-[#b91c1c]"
                  >
                    高
                  </span>
                  <div class="min-w-0">
                    <p class="truncate text-[11px] font-medium text-[#1e293b]">{{ row.title }}</p>
                    <p class="flex items-center gap-1 font-mono text-[10px] text-[#94a3b8]">
                      <span class="truncate">{{ row.ticketNo }} · {{ typeLabel(row.type) }} · {{ row.messageCount || 0 }} 条对话</span>
                      <span
                        v-if="Number(row.staffUnread) > 0"
                        class="inline-flex shrink-0 animate-pulse items-center gap-0.5 rounded-full bg-[#ef4444] px-1.5 text-[10px] font-medium leading-4 text-white shadow-sm ring-2 ring-white"
                        :title="`客户有 ${row.staffUnread} 条待回复消息`"
                      >
                        {{ Number(row.staffUnread) > 99 ? '99+' : row.staffUnread }} 条待回
                      </span>
                    </p>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="客户 / 订单" min-width="160">
              <template #default="{ row }">
                <p class="truncate text-[11px] text-[#1e293b]">{{ row.customerNickname || '—' }}</p>
                <p class="truncate text-[10px] text-[#94a3b8]">
                  <span v-if="row.lastMessage">{{ row.lastMessage }}</span>
                  <span v-else class="font-mono">{{ row.orderNo || row.customerPhone || '未关联' }}</span>
                </p>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <StatusTag :value="row.status" :labels="STATUS_LABELS" :tone="STATUS_TONES[Number(row.status)]" />
              </template>
            </el-table-column>
            <el-table-column label="处理人" width="100">
              <template #default="{ row }">
                <span class="text-[11px]" :class="row.handlerName ? 'text-[#1e293b]' : 'text-[#cbd5e1]'">
                  {{ row.handlerName || '未指派' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="创建时间" width="134">
              <template #default="{ row }">
                <span class="font-mono text-[11px] text-[#64748b]">{{ formatDateTime(row.createTime) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="250" fixed="right">
              <template #default="{ row }">
                <div class="flex flex-wrap items-center gap-0.5">
                  <el-button v-perm="'operations:CsTicket:detail'" link type="primary" size="small" @click="openChat(row)">对话</el-button>
                  <el-button v-perm="'operations:CsTicket:assign'" link size="small" @click="openAssign(row)">指派</el-button>
                  <el-dropdown v-if="NEXT_STATUS[Number(row.status)]?.length" trigger="click">
                    <el-button v-perm="'operations:CsTicket:updateStatus'" link size="small">状态</el-button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item
                          v-for="item in NEXT_STATUS[Number(row.status)]"
                          :key="item.value"
                          @click="changeStatus(row, item.value)"
                        >
                          {{ item.label }}
                        </el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                  <el-dropdown trigger="click">
                    <el-button v-perm="'operations:CsTicket:updatePriority'" link size="small">优先级</el-button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item @click="changePriority(row, 1)">低</el-dropdown-item>
                        <el-dropdown-item @click="changePriority(row, 2)">中</el-dropdown-item>
                        <el-dropdown-item @click="changePriority(row, 3)">高</el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                  <el-button v-perm="'operations:CsTicket:delete'" link type="danger" size="small" @click="remove(row)">删除</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <div class="flex flex-wrap items-center justify-between gap-2 border-t border-[#e2e8f0] px-3 py-2">
          <span class="font-mono text-[10px] text-[#94a3b8]">共 {{ page.total }} 条</span>
          <el-pagination
            v-model:current-page="page.pageNum"
            v-model:page-size="page.pageSize"
            :total="page.total"
            :page-sizes="[10, 20, 50, 100]"
            layout="sizes, prev, pager, next"
            size="small"
            @current-change="load"
            @size-change="search"
          />
        </div>
      </div>
    </div>

    <TicketFormDrawer v-model="formOpen" @saved="() => { load(); loadStats() }" />
    <TicketChatDrawer
      v-model="chatOpen"
      :ticket-id="chatTicketId"
      :signal="chatSignal"
      @changed="() => { load(); loadStats() }"
    />
    <CsAssignRuleDrawer v-model="assignRuleOpen" />

    <el-dialog v-model="assignOpen" title="指派处理人" width="420px" append-to-body>
      <p class="mb-2 text-[11px] text-[#64748b]">待处理的工单被指派后自动进入「处理中」。</p>
      <RemotePicker
        v-model="assignHandlerId"
        :fetcher="fetchUsers"
        :formatter="(row) => `${row.nickname || row.username}（${row.username}）`"
        placeholder="搜索客服账号"
      />
      <template #footer>
        <div class="flex justify-end gap-1">
          <el-button size="small" @click="assignOpen = false">取消</el-button>
          <el-button size="small" type="primary" :loading="assigning" @click="submitAssign">确认指派</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
/* 未读行：浅红底 + 左侧红条，扫一眼就知道哪几行要回 */
:deep(.el-table .cs-row-unread > td.el-table__cell) {
  background-color: #fff6f6 !important;
}

:deep(.el-table .cs-row-unread > td.el-table__cell:first-child) {
  box-shadow: inset 3px 0 0 #ef4444;
}

:deep(.el-table .cs-row-unread:hover > td.el-table__cell) {
  background-color: #ffecec !important;
}
</style>
