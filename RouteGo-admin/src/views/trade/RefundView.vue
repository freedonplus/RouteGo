<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { auditRefund, pageOrders, pageRefunds } from '@/api/trade'
import { formatDateTime, formatMoney } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'
import FilterBar from '@/components/FilterBar.vue'
import StatusTag from '@/components/StatusTag.vue'
import RemotePicker from '@/components/RemotePicker.vue'

const loading = ref(false)
const rows = ref([])
const filters = reactive({ refundNo: '', orderId: null, status: '' })
const timeRange = ref([])
const page = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const auditOpen = ref(false)
const auditSaving = ref(false)
const auditTarget = ref(null)
const auditForm = reactive({ approved: true, auditRemark: '' })

const STATUS_LABELS = ['待审核', '已通过', '已驳回', '已退款']
const STATUS_TONES = ['warning', 'info', 'danger', 'success']

function fetchOrders(keyword) {
  return pageOrders({ orderNo: keyword || undefined, pageNum: 1, pageSize: 20 })
}

async function load() {
  loading.value = true
  try {
    const result = await pageRefunds({
      refundNo: filters.refundNo || undefined,
      orderId: filters.orderId || undefined,
      status: filters.status === '' ? undefined : filters.status,
      applyTimeBegin: timeRange.value?.[0] ? `${timeRange.value[0]} 00:00:00` : undefined,
      applyTimeEnd: timeRange.value?.[1] ? `${timeRange.value[1]} 23:59:59` : undefined,
      pageNum: page.pageNum,
      pageSize: page.pageSize,
    })
    rows.value = result.records || []
    page.total = Number(result.total || 0)
  } catch (error) {
    ElMessage.error(error.message || '退款单加载失败')
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
  Object.assign(filters, { refundNo: '', orderId: null, status: '' })
  timeRange.value = []
  page.pageNum = 1
  load()
}

function openAudit(row, approved) {
  auditTarget.value = row
  auditForm.approved = approved
  auditForm.auditRemark = ''
  auditOpen.value = true
}

async function submitAudit() {
  if (!auditForm.approved && !auditForm.auditRemark.trim()) {
    ElMessage.warning('驳回时请填写审核意见')
    return
  }
  auditSaving.value = true
  try {
    await auditRefund(auditTarget.value.id, { ...auditForm })
    ElMessage.success(auditForm.approved ? '退款已通过' : '退款已驳回')
    auditOpen.value = false
    load()
  } catch (error) {
    ElMessage.error(error.message || '审核失败')
  } finally {
    auditSaving.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="pb-4">
    <PageHeader title="退款管理" description="通过后订单转为已退款并回补库存；驳回则回到已支付" />

    <div class="rg-page space-y-2">
      <FilterBar>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">退款单号</span>
          <el-input v-model="filters.refundNo" size="small" placeholder="RF..." clearable style="width: 150px" class="font-mono" @keyup.enter="search" />
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">订单</span>
          <div style="width: 200px">
            <RemotePicker
              v-model="filters.orderId"
              :fetcher="fetchOrders"
              :formatter="(row) => `${row.orderNo}（${row.statusDesc}）`"
              placeholder="搜索订单号"
            />
          </div>
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">状态</span>
          <el-select v-model="filters.status" size="small" style="width: 100px">
            <el-option label="全部" value="" />
            <el-option v-for="(label, index) in STATUS_LABELS" :key="index" :label="label" :value="index" />
          </el-select>
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">申请时间</span>
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
          <el-table v-loading="loading" :data="rows" size="small" border stripe row-key="id" empty-text="没有匹配的退款单">
            <el-table-column label="退款单号" width="150">
              <template #default="{ row }">
                <span class="font-mono text-[11px] font-medium text-[#1e293b]">{{ row.refundNo }}</span>
              </template>
            </el-table-column>
            <el-table-column label="订单号" width="150">
              <template #default="{ row }">
                <span class="font-mono text-[11px] text-[#64748b]">{{ row.orderNo || `#${row.orderId}` }}</span>
              </template>
            </el-table-column>
            <el-table-column label="金额" width="100">
              <template #default="{ row }">
                <span class="font-mono text-[11px] font-medium text-[#a8435f]">{{ formatMoney(row.amount) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="86">
              <template #default="{ row }">
                <StatusTag :value="row.status" :labels="STATUS_LABELS" :tone="STATUS_TONES[Number(row.status)]" />
              </template>
            </el-table-column>
            <el-table-column label="退款原因" min-width="180">
              <template #default="{ row }">
                <span class="block truncate text-[11px] text-[#1e293b]" :title="row.reason">{{ row.reason }}</span>
                <span v-if="row.auditRemark" class="block truncate text-[10px] text-[#94a3b8]">审核意见：{{ row.auditRemark }}</span>
              </template>
            </el-table-column>
            <el-table-column label="申请时间" width="134">
              <template #default="{ row }">
                <span class="font-mono text-[11px] text-[#64748b]">{{ formatDateTime(row.applyTime) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="审核时间" width="134">
              <template #default="{ row }">
                <span class="font-mono text-[11px] text-[#64748b]">{{ formatDateTime(row.auditTime) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <template v-if="Number(row.status) === 0">
                  <el-button v-perm="'trade:RefundRecord:audit'" link type="primary" size="small" @click="openAudit(row, true)">通过</el-button>
                  <el-button v-perm="'trade:RefundRecord:audit'" link type="danger" size="small" @click="openAudit(row, false)">驳回</el-button>
                </template>
                <span v-else class="text-[10px] text-[#94a3b8]">已处理</span>
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

    <el-dialog v-model="auditOpen" :title="auditForm.approved ? '通过退款' : '驳回退款'" width="440px" append-to-body>
      <div v-if="auditTarget" class="mb-2 rounded bg-[#f8fafc] p-2 text-[11px] text-[#475569]">
        <p>退款单：<span class="font-mono">{{ auditTarget.refundNo }}</span></p>
        <p>退款金额：<span class="font-mono text-[#a8435f]">{{ formatMoney(auditTarget.amount) }}</span></p>
        <p>申请原因：{{ auditTarget.reason }}</p>
      </div>
      <el-form :model="auditForm" label-position="top" size="small">
        <el-form-item :label="auditForm.approved ? '审核意见（可选）' : '驳回原因（必填）'">
          <el-input v-model="auditForm.auditRemark" type="textarea" :rows="3" maxlength="255" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="flex justify-end gap-1">
          <el-button size="small" @click="auditOpen = false">取消</el-button>
          <el-button size="small" type="primary" :loading="auditSaving" @click="submitAudit">确认</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
