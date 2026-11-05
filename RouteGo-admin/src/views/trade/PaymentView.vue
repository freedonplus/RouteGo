<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { pageOrders, pagePayments } from '@/api/trade'
import { formatDateTime, formatMoney } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'
import FilterBar from '@/components/FilterBar.vue'
import StatusTag from '@/components/StatusTag.vue'
import RemotePicker from '@/components/RemotePicker.vue'

const loading = ref(false)
const rows = ref([])
const filters = reactive({ payNo: '', orderId: null, payType: '', status: '' })
const timeRange = ref([])
const page = reactive({ pageNum: 1, pageSize: 10, total: 0 })

function fetchOrders(keyword) {
  return pageOrders({ orderNo: keyword || undefined, pageNum: 1, pageSize: 20 })
}

async function load() {
  loading.value = true
  try {
    const result = await pagePayments({
      payNo: filters.payNo || undefined,
      orderId: filters.orderId || undefined,
      payType: filters.payType === '' ? undefined : filters.payType,
      status: filters.status === '' ? undefined : filters.status,
      payTimeBegin: timeRange.value?.[0] ? `${timeRange.value[0]} 00:00:00` : undefined,
      payTimeEnd: timeRange.value?.[1] ? `${timeRange.value[1]} 23:59:59` : undefined,
      pageNum: page.pageNum,
      pageSize: page.pageSize,
    })
    rows.value = result.records || []
    page.total = Number(result.total || 0)
  } catch (error) {
    ElMessage.error(error.message || '支付流水加载失败')
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
  Object.assign(filters, { payNo: '', orderId: null, payType: '', status: '' })
  timeRange.value = []
  page.pageNum = 1
  load()
}

onMounted(load)
</script>

<template>
  <div class="pb-4">
    <PageHeader title="支付流水" description="订单收款记录，支付流水只读，异常处理走退款流程">
      <template #actions>
        <el-button size="small" :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
      </template>
    </PageHeader>

    <div class="rg-page space-y-2">
      <FilterBar>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">支付单号</span>
          <el-input v-model="filters.payNo" size="small" placeholder="PAY..." clearable style="width: 150px" class="font-mono" @keyup.enter="search" />
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
          <span class="rg-label">支付方式</span>
          <el-select v-model="filters.payType" size="small" style="width: 104px">
            <el-option label="全部" value="" />
            <el-option label="模拟支付" :value="1" />
            <el-option label="微信支付" :value="2" />
          </el-select>
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">结果</span>
          <el-select v-model="filters.status" size="small" style="width: 92px">
            <el-option label="全部" value="" />
            <el-option label="成功" :value="1" />
            <el-option label="失败" :value="0" />
          </el-select>
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">支付时间</span>
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
          <el-table v-loading="loading" :data="rows" size="small" border stripe row-key="id" empty-text="没有匹配的支付流水">
            <el-table-column label="支付单号" width="160">
              <template #default="{ row }">
                <span class="font-mono text-[11px] font-medium text-[#1e293b]">{{ row.payNo }}</span>
              </template>
            </el-table-column>
            <el-table-column label="订单号" width="150">
              <template #default="{ row }">
                <span class="font-mono text-[11px] text-[#64748b]">{{ row.orderNo || `#${row.orderId}` }}</span>
              </template>
            </el-table-column>
            <el-table-column label="方式" width="90">
              <template #default="{ row }">
                <span class="text-[11px] text-[#64748b]">{{ row.payTypeDesc }}</span>
              </template>
            </el-table-column>
            <el-table-column label="金额" width="100">
              <template #default="{ row }">
                <span class="font-mono text-[11px] font-medium text-[#a8435f]">{{ formatMoney(row.amount) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="结果" width="76">
              <template #default="{ row }">
                <StatusTag :value="row.status" :labels="['失败', '成功']" :tone="row.status === 1 ? 'success' : 'danger'" />
              </template>
            </el-table-column>
            <el-table-column label="第三方交易号" min-width="180">
              <template #default="{ row }">
                <span class="font-mono text-[10px] text-[#94a3b8]">{{ row.transactionId || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="支付时间" width="134">
              <template #default="{ row }">
                <span class="font-mono text-[11px] text-[#64748b]">{{ formatDateTime(row.payTime) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="创建时间" width="134">
              <template #default="{ row }">
                <span class="font-mono text-[11px] text-[#64748b]">{{ formatDateTime(row.createTime) }}</span>
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
  </div>
</template>
