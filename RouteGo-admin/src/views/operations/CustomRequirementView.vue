<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  assignRequirement,
  deleteRequirement,
  pageRequirements,
  setRequirementStatus,
} from '@/api/operations'
import { pageUsers } from '@/api/system'
import { formatDate, formatMoney } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'
import FilterBar from '@/components/FilterBar.vue'
import StatusTag from '@/components/StatusTag.vue'
import RemotePicker from '@/components/RemotePicker.vue'
import RequirementFormDrawer from './RequirementFormDrawer.vue'
import RequirementDetailDrawer from './RequirementDetailDrawer.vue'
import QuoteFormDrawer from './QuoteFormDrawer.vue'

const loading = ref(false)
const rows = ref([])
const filters = reactive({ requirementNo: '', keyword: '', status: '', handlerId: null })
const departRange = ref([])
const page = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const formOpen = ref(false)
const formMode = ref('create')
const formRequirementId = ref(null)
const detailOpen = ref(false)
const detailRequirementId = ref(null)
const quoteOpen = ref(false)
const quoteRequirementId = ref(null)
const assignOpen = ref(false)
const assignTarget = ref(null)
const assignHandlerId = ref(null)
const assigning = ref(false)

const STATUS_LABELS = ['待跟进', '已报价', '已成交', '已关闭']
const STATUS_TONES = ['warning', 'info', 'success', 'muted']

function fetchUsers(keyword) {
  return pageUsers({ username: keyword || undefined, pageNum: 1, pageSize: 20 })
}

async function load() {
  loading.value = true
  try {
    const result = await pageRequirements({
      requirementNo: filters.requirementNo || undefined,
      keyword: filters.keyword || undefined,
      status: filters.status === '' ? undefined : filters.status,
      handlerId: filters.handlerId || undefined,
      departDateStart: departRange.value?.[0] || undefined,
      departDateEnd: departRange.value?.[1] || undefined,
      pageNum: page.pageNum,
      pageSize: page.pageSize,
    })
    rows.value = result.records || []
    page.total = Number(result.total || 0)
  } catch (error) {
    ElMessage.error(error.message || '定制需求加载失败')
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
  Object.assign(filters, { requirementNo: '', keyword: '', status: '', handlerId: null })
  departRange.value = []
  page.pageNum = 1
  load()
}

function openCreate() {
  formMode.value = 'create'
  formRequirementId.value = null
  formOpen.value = true
}

function openEdit(row) {
  formMode.value = 'edit'
  formRequirementId.value = row.id
  formOpen.value = true
}

function openDetail(row) {
  detailRequirementId.value = row.id
  detailOpen.value = true
}

function openQuote(row) {
  quoteRequirementId.value = row.id
  quoteOpen.value = true
}

function openAssign(row) {
  assignTarget.value = row
  assignHandlerId.value = row.handlerId || null
  assignOpen.value = true
}

async function submitAssign() {
  if (!assignHandlerId.value) {
    ElMessage.warning('请选择跟进人')
    return
  }
  assigning.value = true
  try {
    await assignRequirement(assignTarget.value.id, assignHandlerId.value)
    ElMessage.success('已指派')
    assignOpen.value = false
    load()
  } catch (error) {
    ElMessage.error(error.message || '指派失败')
  } finally {
    assigning.value = false
  }
}

async function changeStatus(row, status) {
  let remark
  try {
    const { value } = await ElMessageBox.prompt(
      status === 2 ? '线下成交会把需求置为已成交，请填写成交说明。' : '填写关闭原因（选填）。',
      status === 2 ? '标记线下成交' : '关闭需求',
      { inputPlaceholder: status === 2 ? '成交金额/渠道' : '选填', confirmButtonText: '确定', cancelButtonText: '取消' }
    )
    remark = value || ''
  } catch {
    return
  }
  try {
    await setRequirementStatus(row.id, status, remark || undefined)
    ElMessage.success('状态已更新')
    load()
  } catch (error) {
    ElMessage.error(error.message || '状态更新失败')
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确定删除需求 ${row.requirementNo} 吗？`, '删除需求', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  try {
    await deleteRequirement(row.id)
    ElMessage.success('需求已删除')
    load()
  } catch (error) {
    ElMessage.error(error.message || '删除失败')
  }
}

onMounted(load)
</script>

<template>
  <div class="pb-4">
    <PageHeader title="定制需求" description="客户提交的定制需求，报价后自动进入已报价，采纳报价即成交">
      <template #actions>
        <el-button size="small" :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
        <el-button v-perm="'operations:CustomRequirement:create'" size="small" type="primary" :icon="Plus" @click="openCreate">
          录入需求
        </el-button>
      </template>
    </PageHeader>

    <div class="rg-page space-y-2">
      <FilterBar>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">需求单号</span>
          <el-input v-model="filters.requirementNo" size="small" placeholder="CR..." clearable style="width: 140px" class="font-mono" @keyup.enter="search" />
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">关键字</span>
          <el-input v-model="filters.keyword" size="small" placeholder="联系人/目的地" clearable style="width: 150px" @keyup.enter="search" />
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">状态</span>
          <el-select v-model="filters.status" size="small" style="width: 96px">
            <el-option label="全部" value="" />
            <el-option v-for="(label, index) in STATUS_LABELS" :key="index" :label="label" :value="index" />
          </el-select>
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">跟进人</span>
          <div style="width: 150px">
            <RemotePicker
              v-model="filters.handlerId"
              :fetcher="fetchUsers"
              :formatter="(row) => row.nickname || row.username"
              placeholder="搜索跟进人"
            />
          </div>
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
          <el-table v-loading="loading" :data="rows" size="small" border stripe row-key="id" empty-text="没有匹配的需求">
            <el-table-column label="需求" min-width="200">
              <template #default="{ row }">
                <p class="truncate text-[11px] font-medium text-[#1e293b]">{{ row.destination }}</p>
                <p class="font-mono text-[10px] text-[#94a3b8]">{{ row.requirementNo }}</p>
              </template>
            </el-table-column>
            <el-table-column label="联系人" min-width="140">
              <template #default="{ row }">
                <p class="truncate text-[11px] text-[#1e293b]">{{ row.contactName }}</p>
                <p class="font-mono text-[10px] text-[#94a3b8]">{{ row.contactPhone }}</p>
              </template>
            </el-table-column>
            <el-table-column label="计划" width="150">
              <template #default="{ row }">
                <p class="font-mono text-[11px] text-[#64748b]">{{ row.departDate || '日期待定' }}</p>
                <p class="text-[10px] text-[#94a3b8]">{{ row.days || '—' }} 天 · {{ row.peopleCount || '—' }} 人</p>
              </template>
            </el-table-column>
            <el-table-column label="预算" width="100">
              <template #default="{ row }">
                <span class="font-mono text-[11px] text-[#a8435f]">{{ formatMoney(row.budget) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="报价" width="140">
              <template #default="{ row }">
                <p class="text-[11px] text-[#1e293b]">
                  {{ row.quoteCount || 0 }} 版
                  <span v-if="row.latestQuoteAmount" class="ml-1 font-mono text-[#a8435f]">
                    {{ formatMoney(row.latestQuoteAmount) }}
                  </span>
                </p>
                <p v-if="row.latestQuoteStatus !== null && row.latestQuoteStatus !== undefined" class="text-[10px] text-[#94a3b8]">
                  {{ ['待确认', '已采纳', '已失效'][Number(row.latestQuoteStatus)] }}
                </p>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <StatusTag :value="row.status" :labels="STATUS_LABELS" :tone="STATUS_TONES[Number(row.status)]" />
              </template>
            </el-table-column>
            <el-table-column label="跟进人" width="96">
              <template #default="{ row }">
                <span class="text-[11px]" :class="row.handlerName ? 'text-[#1e293b]' : 'text-[#cbd5e1]'">
                  {{ row.handlerName || '未指派' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="创建时间" width="100">
              <template #default="{ row }">
                <span class="font-mono text-[11px] text-[#64748b]">{{ formatDate(row.createTime) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="290" fixed="right">
              <template #default="{ row }">
                <div class="flex flex-wrap items-center gap-0.5">
                  <el-button v-perm="'operations:CustomRequirement:detail'" link type="primary" size="small" @click="openDetail(row)">详情</el-button>
                  <el-button
                    v-if="Number(row.status) < 2"
                    v-perm="'operations:CustomQuote:create'"
                    link
                    type="primary"
                    size="small"
                    @click="openQuote(row)"
                  >
                    报价
                  </el-button>
                  <el-button v-perm="'operations:CustomRequirement:update'" link size="small" @click="openEdit(row)">编辑</el-button>
                  <el-button v-perm="'operations:CustomRequirement:assign'" link size="small" @click="openAssign(row)">指派</el-button>
                  <el-dropdown v-if="Number(row.status) < 2" trigger="click">
                    <el-button v-perm="'operations:CustomRequirement:updateStatus'" link size="small">状态</el-button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item @click="changeStatus(row, 2)">线下成交</el-dropdown-item>
                        <el-dropdown-item @click="changeStatus(row, 3)">关闭需求</el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                  <el-button v-perm="'operations:CustomRequirement:delete'" link type="danger" size="small" @click="remove(row)">删除</el-button>
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

    <RequirementFormDrawer v-model="formOpen" :mode="formMode" :requirement-id="formRequirementId" @saved="load" />
    <RequirementDetailDrawer v-model="detailOpen" :requirement-id="detailRequirementId" @changed="load" />
    <QuoteFormDrawer v-model="quoteOpen" :requirement-id="quoteRequirementId" @saved="load" />

    <el-dialog v-model="assignOpen" title="指派跟进人" width="420px" append-to-body>
      <RemotePicker
        v-model="assignHandlerId"
        :fetcher="fetchUsers"
        :formatter="(row) => `${row.nickname || row.username}（${row.username}）`"
        placeholder="搜索后台用户"
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
