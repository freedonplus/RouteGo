<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import { pageLoginLogs } from '@/api/system'
import { formatDateTime } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'
import FilterBar from '@/components/FilterBar.vue'
import StatusTag from '@/components/StatusTag.vue'

const loading = ref(false)
const rows = ref([])
const filters = reactive({ username: '', ip: '', status: '', timeRange: [] })
const page = reactive({ pageNum: 1, pageSize: 10, total: 0 })

async function load() {
  loading.value = true
  try {
    const params = {
      username: filters.username || undefined,
      ip: filters.ip || undefined,
      status: filters.status === '' ? undefined : filters.status,
      pageNum: page.pageNum,
      pageSize: page.pageSize,
    }
    if (filters.timeRange?.length === 2) {
      params.startTime = filters.timeRange[0]
      params.endTime = filters.timeRange[1]
    }
    const result = await pageLoginLogs(params)
    rows.value = result.records || []
    page.total = Number(result.total || 0)
  } catch (error) {
    ElMessage.error(error.message || '登录日志加载失败')
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
  Object.assign(filters, { username: '', ip: '', status: '', timeRange: [] })
  page.pageNum = 1
  load()
}

onMounted(load)
</script>

<template>
  <div class="pb-4">
    <PageHeader title="登录日志" description="登录成功与失败记录，只读">
      <template #actions>
        <el-button size="small" :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
      </template>
    </PageHeader>

    <div class="rg-page space-y-2">
      <FilterBar>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">用户名</span>
          <el-input v-model="filters.username" size="small" placeholder="账号" clearable style="width: 120px" class="font-mono" @keyup.enter="search" />
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">IP</span>
          <el-input v-model="filters.ip" size="small" placeholder="10.0.0.1" clearable style="width: 120px" class="font-mono" @keyup.enter="search" />
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
          <el-table v-loading="loading" :data="rows" size="small" border stripe row-key="id" empty-text="没有匹配的登录记录">
            <el-table-column label="ID" width="54">
              <template #default="{ row }">
                <span class="font-mono text-[11px] text-[#64748b]">{{ row.id }}</span>
              </template>
            </el-table-column>
            <el-table-column label="用户名" width="120">
              <template #default="{ row }">
                <span class="font-mono text-[11px] font-medium text-[#1e293b]">{{ row.username }}</span>
              </template>
            </el-table-column>
            <el-table-column label="IP" width="120">
              <template #default="{ row }">
                <span class="font-mono text-[11px] text-[#64748b]">{{ row.ip }}</span>
              </template>
            </el-table-column>
            <el-table-column label="归属地" min-width="150">
              <template #default="{ row }">
                <span class="truncate text-[11px] text-[#64748b]">{{ row.location || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="浏览器" min-width="130">
              <template #default="{ row }">
                <span class="truncate text-[11px] text-[#64748b]">{{ row.browser || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作系统" min-width="120">
              <template #default="{ row }">
                <span class="truncate text-[11px] text-[#64748b]">{{ row.os || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="66">
              <template #default="{ row }">
                <StatusTag :value="row.status" type="result" />
              </template>
            </el-table-column>
            <el-table-column label="消息" min-width="160">
              <template #default="{ row }">
                <span class="truncate text-[11px] text-[#64748b]">{{ row.msg || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="登录时间" width="140">
              <template #default="{ row }">
                <span class="font-mono text-[11px] text-[#64748b]">{{ formatDateTime(row.loginTime) }}</span>
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
