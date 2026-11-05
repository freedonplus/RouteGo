<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { deleteConfig, pageConfigs } from '@/api/system'
import { formatDate } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'
import FilterBar from '@/components/FilterBar.vue'
import ConfigFormDrawer from './ConfigFormDrawer.vue'

const loading = ref(false)
const rows = ref([])
const filters = reactive({ configName: '', configKey: '' })
const page = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const formOpen = ref(false)
const formMode = ref('create')
const formConfigId = ref(null)

async function load() {
  loading.value = true
  try {
    const result = await pageConfigs({
      configName: filters.configName || undefined,
      configKey: filters.configKey || undefined,
      pageNum: page.pageNum,
      pageSize: page.pageSize,
    })
    rows.value = result.records || []
    page.total = Number(result.total || 0)
  } catch (error) {
    ElMessage.error(error.message || '参数加载失败')
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
  filters.configName = ''
  filters.configKey = ''
  page.pageNum = 1
  load()
}

function openCreate() {
  formMode.value = 'create'
  formConfigId.value = null
  formOpen.value = true
}

function openEdit(row) {
  formMode.value = 'edit'
  formConfigId.value = row.id
  formOpen.value = true
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确定删除参数 ${row.configKey} 吗？`, '删除参数', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  try {
    await deleteConfig(row.id)
    ElMessage.success('参数已删除')
    load()
  } catch (error) {
    ElMessage.error(error.message || '删除失败')
  }
}

onMounted(load)
</script>

<template>
  <div class="pb-4">
    <PageHeader title="参数配置" description="系统运行时参数，键创建后不可修改">
      <template #actions>
        <el-button size="small" :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
        <el-button v-perm="'admin:SysConfig:create'" size="small" type="primary" :icon="Plus" @click="openCreate">
          新增参数
        </el-button>
      </template>
    </PageHeader>

    <div class="rg-page space-y-2">
      <FilterBar>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">参数名称</span>
          <el-input v-model="filters.configName" size="small" placeholder="名称" clearable style="width: 140px" @keyup.enter="search" />
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">参数键</span>
          <el-input v-model="filters.configKey" size="small" placeholder="site.name" clearable style="width: 160px" class="font-mono" @keyup.enter="search" />
        </label>
        <template #actions>
          <el-button size="small" type="primary" :icon="Search" @click="search">查询</el-button>
          <el-button size="small" @click="resetFilters">重置</el-button>
        </template>
      </FilterBar>

      <div class="rg-card min-w-0">
        <div class="rg-scroll-x">
          <el-table v-loading="loading" :data="rows" size="small" border stripe row-key="id" empty-text="没有匹配的参数">
            <el-table-column label="ID" width="50">
              <template #default="{ row }">
                <span class="font-mono text-[11px] text-[#64748b]">{{ row.id }}</span>
              </template>
            </el-table-column>
            <el-table-column label="参数名称" min-width="140">
              <template #default="{ row }">
                <span class="truncate text-[11px] font-medium text-[#1e293b]">{{ row.configName }}</span>
              </template>
            </el-table-column>
            <el-table-column label="参数键" min-width="180">
              <template #default="{ row }">
                <span class="truncate font-mono text-[11px] text-[#a8435f]">{{ row.configKey }}</span>
              </template>
            </el-table-column>
            <el-table-column label="参数值" min-width="220">
              <template #default="{ row }">
                <span class="block truncate font-mono text-[11px] text-[#64748b]" :title="row.configValue">
                  {{ row.configValue }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="备注" min-width="140">
              <template #default="{ row }">
                <span class="truncate text-[11px] text-[#64748b]">{{ row.remark || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="创建时间" width="100">
              <template #default="{ row }">
                <span class="font-mono text-[11px] text-[#64748b]">{{ formatDate(row.createTime) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="112" fixed="right">
              <template #default="{ row }">
                <el-button v-perm="'admin:SysConfig:update'" link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
                <el-button v-perm="'admin:SysConfig:delete'" link type="danger" size="small" @click="remove(row)">删除</el-button>
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

    <ConfigFormDrawer v-model="formOpen" :mode="formMode" :config-id="formConfigId" @saved="load" />
  </div>
</template>
