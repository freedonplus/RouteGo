<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { getOperLog, pageOperLogs } from '@/api/system'
import { formatDateTime } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'
import FilterBar from '@/components/FilterBar.vue'
import StatusTag from '@/components/StatusTag.vue'

const loading = ref(false)
const rows = ref([])
const filters = reactive({ title: '', operName: '', status: '', timeRange: [] })
const page = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const detailOpen = ref(false)
const detailLoading = ref(false)
const detail = ref(null)

async function load() {
  loading.value = true
  try {
    const params = {
      title: filters.title || undefined,
      operName: filters.operName || undefined,
      status: filters.status === '' ? undefined : filters.status,
      pageNum: page.pageNum,
      pageSize: page.pageSize,
    }
    if (filters.timeRange?.length === 2) {
      params.startTime = filters.timeRange[0]
      params.endTime = filters.timeRange[1]
    }
    const result = await pageOperLogs(params)
    rows.value = result.records || []
    page.total = Number(result.total || 0)
  } catch (error) {
    ElMessage.error(error.message || '操作日志加载失败')
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
  Object.assign(filters, { title: '', operName: '', status: '', timeRange: [] })
  page.pageNum = 1
  load()
}

async function openDetail(row) {
  detailOpen.value = true
  detailLoading.value = true
  detail.value = row
  try {
    detail.value = await getOperLog(row.id)
  } catch (error) {
    ElMessage.error(error.message || '详情加载失败')
  } finally {
    detailLoading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="pb-4">
    <PageHeader title="操作日志" description="只读日志，参数与结果已脱敏、截断">
      <template #actions>
        <el-button size="small" :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
      </template>
    </PageHeader>

    <div class="rg-page space-y-2">
      <FilterBar>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">标题</span>
          <el-input v-model="filters.title" size="small" placeholder="操作标题" clearable style="width: 130px" @keyup.enter="search" />
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">操作人</span>
          <el-input v-model="filters.operName" size="small" placeholder="账号" clearable style="width: 110px" class="font-mono" @keyup.enter="search" />
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">状态</span>
          <el-select v-model="filters.status" size="small" style="width: 88px">
            <el-option label="全部" value="" />
            <el-option label="成功" :value="1" />
            <el-option label="失败" :value="0" />
          </el-select>
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">时间范围</span>
          <el-date-picker
            v-model="filters.timeRange"
            type="datetimerange"
            size="small"
            value-format="YYYY-MM-DDTHH:mm:ss"
            start-placeholder="开始"
            end-placeholder="结束"
            style="width: 300px"
          />
        </label>
        <template #actions>
          <el-button size="small" type="primary" :icon="Search" @click="search">查询</el-button>
          <el-button size="small" @click="resetFilters">重置</el-button>
        </template>
      </FilterBar>

      <div class="rg-card min-w-0">
        <div class="rg-scroll-x">
          <el-table v-loading="loading" :data="rows" size="small" border stripe row-key="id" empty-text="没有匹配的日志">
            <el-table-column label="ID" width="54">
              <template #default="{ row }">
                <span class="font-mono text-[11px] text-[#64748b]">{{ row.id }}</span>
              </template>
            </el-table-column>
            <el-table-column label="标题" min-width="120">
              <template #default="{ row }">
                <span class="truncate text-[11px] font-medium text-[#1e293b]">{{ row.title }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作人" width="96">
              <template #default="{ row }">
                <span class="font-mono text-[11px]">{{ row.operName }}</span>
              </template>
            </el-table-column>
            <el-table-column label="请求" min-width="200">
              <template #default="{ row }">
                <span class="font-mono text-[10px] text-[#64748b]">
                  {{ row.requestMethod }} {{ row.operUrl }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="IP" width="112">
              <template #default="{ row }">
                <span class="font-mono text-[11px] text-[#64748b]">{{ row.operIp }}</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="66">
              <template #default="{ row }">
                <StatusTag :value="row.status" type="result" />
              </template>
            </el-table-column>
            <el-table-column label="耗时" width="66" align="right">
              <template #default="{ row }">
                <span class="font-mono text-[11px]">{{ row.costTime }}ms</span>
              </template>
            </el-table-column>
            <el-table-column label="操作时间" width="134">
              <template #default="{ row }">
                <span class="font-mono text-[11px] text-[#64748b]">{{ formatDateTime(row.operTime) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="60" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
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

    <el-drawer v-model="detailOpen" title="操作日志详情" size="520px" append-to-body>
      <div v-loading="detailLoading" class="space-y-2">
        <div class="grid grid-cols-2 gap-2">
          <div class="rounded border border-[#e2e8f0] px-2.5 py-1.5">
            <p class="rg-label">标题</p>
            <p class="mt-0.5 text-xs text-[#1e293b]">{{ detail?.title || '—' }}</p>
          </div>
          <div class="rounded border border-[#e2e8f0] px-2.5 py-1.5">
            <p class="rg-label">操作人</p>
            <p class="mt-0.5 font-mono text-xs text-[#1e293b]">{{ detail?.operName || '—' }}</p>
          </div>
          <div class="rounded border border-[#e2e8f0] px-2.5 py-1.5">
            <p class="rg-label">请求</p>
            <p class="mt-0.5 break-all font-mono text-[11px] text-[#1e293b]">
              {{ detail?.requestMethod }} {{ detail?.operUrl }}
            </p>
          </div>
          <div class="rounded border border-[#e2e8f0] px-2.5 py-1.5">
            <p class="rg-label">结果</p>
            <p class="mt-0.5 flex items-center gap-2">
              <StatusTag :value="detail?.status ?? 1" type="result" />
              <span class="font-mono text-[11px] text-[#64748b]">{{ detail?.costTime }}ms</span>
            </p>
          </div>
        </div>

        <div class="rounded border border-[#e2e8f0] px-2.5 py-1.5">
          <p class="rg-label">请求参数</p>
          <pre class="mt-1 max-h-40 overflow-auto break-all whitespace-pre-wrap font-mono text-[11px] text-[#1e293b]">{{ detail?.operParam || '—' }}</pre>
        </div>
        <div class="rounded border border-[#e2e8f0] px-2.5 py-1.5">
          <p class="rg-label">返回结果</p>
          <pre class="mt-1 max-h-40 overflow-auto break-all whitespace-pre-wrap font-mono text-[11px] text-[#1e293b]">{{ detail?.jsonResult || '—' }}</pre>
        </div>
        <div v-if="detail?.errorMsg" class="rounded border border-[#fecaca] bg-[#fef2f2] px-2.5 py-1.5">
          <p class="rg-label text-[#b91c1c]">错误信息</p>
          <pre class="mt-1 break-all whitespace-pre-wrap font-mono text-[11px] text-[#b91c1c]">{{ detail.errorMsg }}</pre>
        </div>
      </div>
    </el-drawer>
  </div>
</template>
