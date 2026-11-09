<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  acceptQuote,
  deleteQuote,
  invalidateQuote,
  pageQuotes,
  pageRequirements,
} from '@/api/operations'
import { formatDate, formatDateTime, formatMoney } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'
import FilterBar from '@/components/FilterBar.vue'
import StatusTag from '@/components/StatusTag.vue'
import RemotePicker from '@/components/RemotePicker.vue'
import QuoteFormDrawer from './QuoteFormDrawer.vue'

const loading = ref(false)
const rows = ref([])
const filters = reactive({ requirementId: null, quoteNo: '', status: '', expiredOnly: '' })
const page = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const formOpen = ref(false)
const editingQuoteId = ref(null)

const STATUS_LABELS = ['待确认', '已采纳', '已失效']
const STATUS_TONES = ['warning', 'success', 'muted']

function fetchRequirements(keyword) {
  return pageRequirements({ keyword: keyword || undefined, pageNum: 1, pageSize: 20 })
}

async function load() {
  loading.value = true
  try {
    const result = await pageQuotes({
      requirementId: filters.requirementId || undefined,
      quoteNo: filters.quoteNo || undefined,
      status: filters.status === '' ? undefined : filters.status,
      expiredOnly: filters.expiredOnly === '' ? undefined : filters.expiredOnly,
      pageNum: page.pageNum,
      pageSize: page.pageSize,
    })
    rows.value = result.records || []
    page.total = Number(result.total || 0)
  } catch (error) {
    ElMessage.error(error.message || '报价加载失败')
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
  Object.assign(filters, { requirementId: null, quoteNo: '', status: '', expiredOnly: '' })
  page.pageNum = 1
  load()
}

function openCreate() {
  editingQuoteId.value = null
  formOpen.value = true
}

function openEdit(row) {
  editingQuoteId.value = row.id
  formOpen.value = true
}

async function accept(row) {
  try {
    await ElMessageBox.confirm(
      `采纳后需求转为已成交，同需求其他待确认报价失效。确认采纳 ${formatMoney(row.amount)} 吗？`,
      '采纳报价',
      { type: 'warning', confirmButtonText: '确认采纳', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await acceptQuote(row.id)
    ElMessage.success('已采纳')
    load()
  } catch (error) {
    ElMessage.error(error.message || '采纳失败')
  }
}

async function invalidate(row) {
  try {
    await ElMessageBox.confirm('作废后不可再采纳，确认吗？', '作废报价', {
      type: 'warning',
      confirmButtonText: '作废',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  try {
    await invalidateQuote(row.id)
    ElMessage.success('已作废')
    load()
  } catch (error) {
    ElMessage.error(error.message || '作废失败')
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确定删除报价 ${row.quoteNo} 吗？`, '删除报价', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  try {
    await deleteQuote(row.id)
    ElMessage.success('报价已删除')
    load()
  } catch (error) {
    ElMessage.error(error.message || '删除失败')
  }
}

onMounted(load)
</script>

<template>
  <div class="pb-4">
    <PageHeader title="定制报价" description="同一需求可多版报价，采纳一版后其余自动失效">
      <template #actions>
        <el-button size="small" :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
        <el-button v-perm="'operations:CustomQuote:create'" size="small" type="primary" :icon="Plus" @click="openCreate">
          新建报价
        </el-button>
      </template>
    </PageHeader>

    <div class="rg-page space-y-2">
      <FilterBar>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">需求单</span>
          <div style="width: 200px">
            <RemotePicker
              v-model="filters.requirementId"
              :fetcher="fetchRequirements"
              :formatter="(row) => `${row.requirementNo} · ${row.destination}`"
              placeholder="搜索需求单号/目的地"
            />
          </div>
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">报价单号</span>
          <el-input v-model="filters.quoteNo" size="small" placeholder="QT..." clearable style="width: 140px" class="font-mono" @keyup.enter="search" />
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">状态</span>
          <el-select v-model="filters.status" size="small" style="width: 100px">
            <el-option label="全部" value="" />
            <el-option v-for="(label, index) in STATUS_LABELS" :key="index" :label="label" :value="index" />
          </el-select>
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">有效期</span>
          <el-select v-model="filters.expiredOnly" size="small" style="width: 110px">
            <el-option label="全部" value="" />
            <el-option label="仅已过期" :value="true" />
          </el-select>
        </label>
        <template #actions>
          <el-button size="small" type="primary" :icon="Search" @click="search">查询</el-button>
          <el-button size="small" @click="resetFilters">重置</el-button>
        </template>
      </FilterBar>

      <div class="rg-card min-w-0">
        <div class="rg-scroll-x">
          <el-table v-loading="loading" :data="rows" size="small" border stripe row-key="id" empty-text="没有匹配的报价">
            <el-table-column label="报价单号" width="150">
              <template #default="{ row }">
                <span class="font-mono text-[11px] font-medium text-[#1e293b]">{{ row.quoteNo }}</span>
              </template>
            </el-table-column>
            <el-table-column label="需求单" min-width="170">
              <template #default="{ row }">
                <span class="font-mono text-[11px] text-[#64748b]">{{ row.requirementNo || `#${row.requirementId}` }}</span>
              </template>
            </el-table-column>
            <el-table-column label="金额" width="110">
              <template #default="{ row }">
                <span class="font-mono text-[11px] font-medium text-[#a8435f]">{{ formatMoney(row.amount) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="明细" min-width="200">
              <template #default="{ row }">
                <span class="block truncate text-[11px] text-[#64748b]" :title="row.detail">{{ row.detail || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="86">
              <template #default="{ row }">
                <StatusTag :value="row.status" :labels="STATUS_LABELS" :tone="STATUS_TONES[Number(row.status)]" />
              </template>
            </el-table-column>
            <el-table-column label="有效期至" width="106">
              <template #default="{ row }">
                <span class="font-mono text-[11px]" :class="row.expired ? 'text-[#b91c1c]' : 'text-[#64748b]'">
                  {{ row.validUntil ? formatDate(row.validUntil) : '—' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="创建时间" width="134">
              <template #default="{ row }">
                <span class="font-mono text-[11px] text-[#64748b]">{{ formatDateTime(row.createTime) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <template v-if="Number(row.status) === 0">
                  <el-button v-perm="'operations:CustomQuote:accept'" link type="primary" size="small" @click="accept(row)">采纳</el-button>
                  <el-button v-perm="'operations:CustomQuote:update'" link size="small" @click="openEdit(row)">改价</el-button>
                  <el-button v-perm="'operations:CustomQuote:invalidate'" link type="danger" size="small" @click="invalidate(row)">作废</el-button>
                </template>
                <el-button v-perm="'operations:CustomQuote:delete'" link type="danger" size="small" @click="remove(row)">删除</el-button>
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

    <QuoteFormDrawer v-model="formOpen" :quote-id="editingQuoteId" @saved="load" />
  </div>
</template>
