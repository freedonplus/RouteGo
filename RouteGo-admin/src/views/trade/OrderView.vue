<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { pageCustomers } from '@/api/customer'
import {
  cancelOrder,
  deleteOrder,
  pageOrders,
  payOrder,
  startOrder,
} from '@/api/trade'
import { formatDateTime, formatMoney } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'
import FilterBar from '@/components/FilterBar.vue'
import StatusTag from '@/components/StatusTag.vue'
import RemotePicker from '@/components/RemotePicker.vue'
import OrderFormDrawer from './OrderFormDrawer.vue'
import OrderDetailDrawer from './OrderDetailDrawer.vue'

const loading = ref(false)
const rows = ref([])
const filters = reactive({ orderNo: '', customerId: null, status: '', payStatus: '', source: '' })
const departRange = ref([])
const page = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const formOpen = ref(false)
const detailOpen = ref(false)
const detailOrderId = ref(null)

const STATUS_LABELS = ['待支付', '已支付', '出行中', '已完成', '已取消', '退款中', '已退款']
const STATUS_TONES = ['warning', 'success', 'info', 'success', 'muted', 'warning', 'danger']

function statusTone(status) {
  return STATUS_TONES[Number(status)] || 'muted'
}

function fetchCustomers(keyword) {
  return pageCustomers({ keyword: keyword || undefined, pageNum: 1, pageSize: 20 })
}

async function load() {
  loading.value = true
  try {
    const result = await pageOrders({
      orderNo: filters.orderNo || undefined,
      customerId: filters.customerId || undefined,
      status: filters.status === '' ? undefined : filters.status,
      payStatus: filters.payStatus === '' ? undefined : filters.payStatus,
      source: filters.source === '' ? undefined : filters.source,
      departDateBegin: departRange.value?.[0] || undefined,
      departDateEnd: departRange.value?.[1] || undefined,
      pageNum: page.pageNum,
      pageSize: page.pageSize,
    })
    rows.value = result.records || []
    page.total = Number(result.total || 0)
  } catch (error) {
    ElMessage.error(error.message || '订单加载失败')
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
  Object.assign(filters, { orderNo: '', customerId: null, status: '', payStatus: '', source: '' })
  departRange.value = []
  page.pageNum = 1
  load()
}

function openCreate() {
  formOpen.value = true
}

function openDetail(row) {
  detailOrderId.value = row.id
  detailOpen.value = true
}

async function pay(row) {
  try {
    await ElMessageBox.confirm(
      `确认收款 ${formatMoney(row.payAmount)} 吗？确认后订单进入已支付并累加线路销量。`,
      '收款确认',
      { type: 'warning', confirmButtonText: '确认收款', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await payOrder(row.id, { payType: 1, remark: '后台收款' })
    ElMessage.success('收款成功')
    load()
  } catch (error) {
    ElMessage.error(error.message || '收款失败')
  }
}

async function cancel(row) {
  try {
    const { value } = await ElMessageBox.prompt('请填写取消原因，取消后班期库存会自动回补。', '取消订单', {
      inputPlaceholder: '如：客户临时有事',
      inputValidator: (text) => (text && text.trim() ? true : '请填写取消原因'),
      confirmButtonText: '取消订单',
      cancelButtonText: '返回',
    })
    await cancelOrder(row.id, value.trim())
    ElMessage.success('订单已取消')
    load()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || '取消失败')
    }
  }
}

async function start(row) {
  try {
    await startOrder(row.id)
    ElMessage.success('订单已进入出行中')
    load()
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确定删除订单 ${row.orderNo} 吗？`, '删除订单', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  try {
    await deleteOrder(row.id)
    ElMessage.success('订单已删除')
    load()
  } catch (error) {
    ElMessage.error(error.message || '删除失败')
  }
}

onMounted(load)
</script>

<template>
  <div class="pb-4">
    <PageHeader title="订单列表" description="待支付可收款或取消；已支付可开始行程；退款请走退款流程">
      <template #actions>
        <el-button size="small" :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
        <el-button v-perm="'trade:Orders:create'" size="small" type="primary" :icon="Plus" @click="openCreate">
          后台代录
        </el-button>
      </template>
    </PageHeader>

    <div class="rg-page space-y-2">
      <FilterBar>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">订单号</span>
          <el-input v-model="filters.orderNo" size="small" placeholder="RG..." clearable style="width: 150px" class="font-mono" @keyup.enter="search" />
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">会员</span>
          <div style="width: 190px">
            <RemotePicker
              v-model="filters.customerId"
              :fetcher="fetchCustomers"
              :formatter="(row) => `${row.nickname || '未命名'}（${row.phone}）`"
              placeholder="搜索昵称/手机号"
            />
          </div>
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">订单状态</span>
          <el-select v-model="filters.status" size="small" style="width: 100px">
            <el-option label="全部" value="" />
            <el-option v-for="(label, index) in STATUS_LABELS" :key="index" :label="label" :value="index" />
          </el-select>
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">支付状态</span>
          <el-select v-model="filters.payStatus" size="small" style="width: 96px">
            <el-option label="全部" value="" />
            <el-option label="未支付" :value="0" />
            <el-option label="已支付" :value="1" />
            <el-option label="已退款" :value="2" />
          </el-select>
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">来源</span>
          <el-select v-model="filters.source" size="small" style="width: 96px">
            <el-option label="全部" value="" />
            <el-option label="后台代录" :value="1" />
            <el-option label="微信小程序" :value="2" />
          </el-select>
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">出行日期</span>
          <el-date-picker
            v-model="departRange"
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
          <el-table v-loading="loading" :data="rows" size="small" border stripe row-key="id" empty-text="没有匹配的订单">
            <el-table-column label="订单号" width="150">
              <template #default="{ row }">
                <p class="font-mono text-[11px] font-medium text-[#1e293b]">{{ row.orderNo }}</p>
                <p class="font-mono text-[10px] text-[#94a3b8]">{{ formatDateTime(row.createTime) }}</p>
              </template>
            </el-table-column>
            <el-table-column label="会员" min-width="150">
              <template #default="{ row }">
                <p class="truncate text-[11px] text-[#1e293b]">{{ row.customerNickname || `#${row.customerId}` }}</p>
                <p class="font-mono text-[10px] text-[#94a3b8]">{{ row.customerPhone }}</p>
              </template>
            </el-table-column>
            <el-table-column label="线路" min-width="190">
              <template #default="{ row }">
                <p class="truncate text-[11px] text-[#1e293b]">{{ row.routeTitle || `#${row.routeId}` }}</p>
                <p class="font-mono text-[10px] text-[#94a3b8]">{{ row.departDate }} 出发 · {{ row.travelerCount }} 人</p>
              </template>
            </el-table-column>
            <el-table-column label="金额" width="100">
              <template #default="{ row }">
                <p class="font-mono text-[11px] font-medium text-[#a8435f]">{{ formatMoney(row.payAmount) }}</p>
                <p v-if="Number(row.discountAmount) > 0" class="font-mono text-[10px] text-[#94a3b8]">
                  优惠 {{ formatMoney(row.discountAmount) }}
                </p>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <StatusTag :value="row.status" :labels="STATUS_LABELS" :tone="statusTone(row.status)" />
              </template>
            </el-table-column>
            <el-table-column label="支付" width="84">
              <template #default="{ row }">
                <span class="text-[11px] text-[#64748b]">{{ row.payStatusDesc || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="来源" width="80">
              <template #default="{ row }">
                <span class="text-[11px] text-[#64748b]">{{ row.sourceDesc || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="210" fixed="right">
              <template #default="{ row }">
                <div class="flex flex-wrap items-center gap-0.5">
                  <el-button v-perm="'trade:Orders:detail'" link type="primary" size="small" @click="openDetail(row)">详情</el-button>
                  <el-button
                    v-if="Number(row.status) === 0"
                    v-perm="'trade:Orders:pay'"
                    link
                    type="primary"
                    size="small"
                    @click="pay(row)"
                  >
                    收款
                  </el-button>
                  <el-button
                    v-if="Number(row.status) === 0"
                    v-perm="'trade:Orders:cancel'"
                    link
                    size="small"
                    @click="cancel(row)"
                  >
                    取消
                  </el-button>
                  <el-button
                    v-if="Number(row.status) === 1"
                    v-perm="'trade:Orders:startTravel'"
                    link
                    type="primary"
                    size="small"
                    @click="start(row)"
                  >
                    开始行程
                  </el-button>
                  <el-button v-perm="'trade:Orders:delete'" link type="danger" size="small" @click="remove(row)">删除</el-button>
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

    <OrderFormDrawer v-model="formOpen" @saved="load" />
    <OrderDetailDrawer v-model="detailOpen" :order-id="detailOrderId" @changed="load" />
  </div>
</template>
