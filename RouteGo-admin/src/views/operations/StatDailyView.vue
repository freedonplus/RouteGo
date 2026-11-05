<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { getStatSummary, getStatTrend, pageStats, rebuildStats } from '@/api/operations'
import { formatDate, formatMoney, formatNumber } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'
import KpiCard from '@/components/KpiCard.vue'

function daysAgo(days) {
  const date = new Date()
  date.setDate(date.getDate() - days)
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

function today() {
  return daysAgo(0)
}

const range = ref([daysAgo(6), today()])
const loading = ref(false)
const summary = ref(null)
const trend = ref([])
const rows = ref([])
const page = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const rebuildOpen = ref(false)
const rebuildRange = ref([daysAgo(6), today()])
const rebuilding = ref(false)

const maxSales = computed(() => {
  const values = trend.value.map((item) => Number(item.salesAmount || 0))
  return Math.max(1, ...values)
})

function barHeight(item) {
  const value = Number(item.salesAmount || 0)
  return `${Math.max(2, Math.round((value / maxSales.value) * 100))}%`
}

async function loadSummary() {
  if (!range.value?.[0] || !range.value?.[1]) return
  try {
    summary.value = await getStatSummary({ startDate: range.value[0], endDate: range.value[1] })
  } catch (error) {
    ElMessage.error(error.message || '汇总加载失败')
    summary.value = null
  }
}

async function loadTrend() {
  if (!range.value?.[0] || !range.value?.[1]) return
  try {
    trend.value = await getStatTrend({ startDate: range.value[0], endDate: range.value[1] })
  } catch {
    trend.value = []
  }
}

async function loadTable() {
  loading.value = true
  try {
    const result = await pageStats({
      startDate: range.value?.[0] || undefined,
      endDate: range.value?.[1] || undefined,
      pageNum: page.pageNum,
      pageSize: page.pageSize,
    })
    rows.value = result.records || []
    page.total = Number(result.total || 0)
  } catch (error) {
    ElMessage.error(error.message || '统计明细加载失败')
    rows.value = []
    page.total = 0
  } finally {
    loading.value = false
  }
}

function reload() {
  page.pageNum = 1
  loadSummary()
  loadTrend()
  loadTable()
}

function openRebuild() {
  rebuildRange.value = [daysAgo(6), today()]
  rebuildOpen.value = true
}

async function submitRebuild() {
  if (!rebuildRange.value?.[0] || !rebuildRange.value?.[1]) {
    ElMessage.warning('请选择日期区间')
    return
  }
  rebuilding.value = true
  try {
    const count = await rebuildStats({ startDate: rebuildRange.value[0], endDate: rebuildRange.value[1] })
    ElMessage.success(`已重算 ${count} 天`)
    rebuildOpen.value = false
    reload()
  } catch (error) {
    ElMessage.error(error.message || '重算失败')
  } finally {
    rebuilding.value = false
  }
}

onMounted(reload)
</script>

<template>
  <div class="pb-4">
    <PageHeader title="经营统计" description="按天汇总订单、支付、退款与新增会员；汇总直接聚合原始流水">
      <template #actions>
        <el-date-picker
          v-model="range"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="至"
          start-placeholder="开始"
          end-placeholder="结束"
          size="small"
          style="width: 224px"
          @change="reload"
        />
        <el-button size="small" :icon="Refresh" :loading="loading" @click="reload">刷新</el-button>
        <el-button v-perm="'operations:StatDaily:rebuild'" size="small" @click="openRebuild">重算统计</el-button>
      </template>
    </PageHeader>

    <div class="rg-page space-y-2">
      <div v-if="summary" class="grid grid-cols-2 gap-2 md:grid-cols-3 lg:grid-cols-6">
        <KpiCard label="销售额" :value="formatMoney(summary.salesAmount, { prefix: '' })" unit="元" />
        <KpiCard label="退款金额" :value="formatMoney(summary.refundAmount, { prefix: '' })" unit="元" invert />
        <KpiCard label="净收入" :value="formatMoney(summary.netAmount, { prefix: '' })" unit="元" />
        <KpiCard label="订单数" :value="formatNumber(summary.orderCount)" :hint="`已支付 ${formatNumber(summary.paidOrderCount)} 单`" />
        <KpiCard label="客单价" :value="formatMoney(summary.avgOrderAmount, { prefix: '' })" unit="元" />
        <KpiCard label="新增会员" :value="formatNumber(summary.newCustomerCount)" unit="人" />
      </div>

      <div class="rg-card px-3 py-2">
        <div class="flex items-center justify-between">
          <p class="rg-section-title !mb-0">销售趋势</p>
          <span class="font-mono text-[10px] text-[#94a3b8]">峰值 {{ formatMoney(maxSales, { prefix: '' }) }} 元</span>
        </div>
        <div v-if="trend.length" class="mt-2 flex h-36 items-end gap-1 overflow-x-auto">
          <div v-for="item in trend" :key="item.statDate" class="flex h-full min-w-[18px] flex-1 flex-col justify-end">
            <div
              class="w-full rounded-t bg-[#a8435f] transition-all"
              :style="{ height: barHeight(item) }"
              :title="`${item.statDate}：${formatMoney(item.salesAmount)}，${item.orderCount} 单`"
            />
          </div>
        </div>
        <el-empty v-else description="区间内还没有统计数据，可先点「重算统计」" :image-size="60" />
        <div v-if="trend.length" class="mt-1 flex justify-between font-mono text-[10px] text-[#94a3b8]">
          <span>{{ trend[0]?.statDate }}</span>
          <span>{{ trend[trend.length - 1]?.statDate }}</span>
        </div>
      </div>

      <div class="rg-card min-w-0">
        <div class="rg-scroll-x">
          <el-table v-loading="loading" :data="rows" size="small" border stripe row-key="id" empty-text="区间内没有统计记录">
            <el-table-column label="统计日期" width="106">
              <template #default="{ row }">
                <span class="font-mono text-[11px] font-medium text-[#1e293b]">{{ formatDate(row.statDate) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="订单数" width="76">
              <template #default="{ row }">
                <span class="font-mono text-[11px]">{{ row.orderCount }}</span>
              </template>
            </el-table-column>
            <el-table-column label="已支付" width="76">
              <template #default="{ row }">
                <span class="font-mono text-[11px]">{{ row.paidOrderCount }}</span>
              </template>
            </el-table-column>
            <el-table-column label="销售额" width="110">
              <template #default="{ row }">
                <span class="font-mono text-[11px] text-[#a8435f]">{{ formatMoney(row.salesAmount) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="退款" width="110">
              <template #default="{ row }">
                <span class="font-mono text-[11px] text-[#64748b]">{{ formatMoney(row.refundAmount) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="净收入" width="110">
              <template #default="{ row }">
                <span class="font-mono text-[11px] text-[#15803d]">{{ formatMoney(row.netAmount) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="客单价" width="110">
              <template #default="{ row }">
                <span class="font-mono text-[11px] text-[#64748b]">{{ formatMoney(row.avgOrderAmount) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="新增会员" width="86">
              <template #default="{ row }">
                <span class="font-mono text-[11px]">{{ row.newCustomerCount }}</span>
              </template>
            </el-table-column>
            <el-table-column label="更新时间" min-width="140">
              <template #default="{ row }">
                <span class="font-mono text-[10px] text-[#94a3b8]">{{ row.updateTime || '—' }}</span>
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
            @current-change="loadTable"
            @size-change="reload"
          />
        </div>
      </div>
    </div>

    <el-dialog v-model="rebuildOpen" title="重算经营统计" width="420px" append-to-body>
      <p class="mb-2 text-[11px] text-[#64748b]">
        按天覆盖写入，幂等可重复执行；单次最多 366 天，用于补跑漏掉的定时任务。
      </p>
      <el-date-picker
        v-model="rebuildRange"
        type="daterange"
        value-format="YYYY-MM-DD"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        size="small"
        style="width: 100%"
      />
      <template #footer>
        <div class="flex justify-end gap-1">
          <el-button size="small" @click="rebuildOpen = false">取消</el-button>
          <el-button size="small" type="primary" :loading="rebuilding" @click="submitRebuild">开始重算</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
