<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  deleteNotice,
  pageNotices,
  publishNotice,
  setNoticeTop,
  unpublishNotice,
} from '@/api/system'
import { formatDate, formatDateTime } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'
import FilterBar from '@/components/FilterBar.vue'
import StatusTag from '@/components/StatusTag.vue'
import NoticeFormDrawer from './NoticeFormDrawer.vue'

const loading = ref(false)
const rows = ref([])
const filters = reactive({ title: '', type: '', status: '' })
const page = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const formOpen = ref(false)
const formMode = ref('create')
const formNoticeId = ref(null)

const typeMeta = {
  1: { label: '通知', cls: 'bg-[#eff6ff] text-[#1d4ed8]' },
  2: { label: '公告', cls: 'bg-[#fdf1f5] text-[#a8435f]' },
}

async function load() {
  loading.value = true
  try {
    const result = await pageNotices({
      title: filters.title || undefined,
      type: filters.type === '' ? undefined : filters.type,
      status: filters.status === '' ? undefined : filters.status,
      pageNum: page.pageNum,
      pageSize: page.pageSize,
    })
    rows.value = result.records || []
    page.total = Number(result.total || 0)
  } catch (error) {
    ElMessage.error(error.message || '公告加载失败')
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
  Object.assign(filters, { title: '', type: '', status: '' })
  page.pageNum = 1
  load()
}

function openCreate() {
  formMode.value = 'create'
  formNoticeId.value = null
  formOpen.value = true
}

function openEdit(row) {
  formMode.value = 'edit'
  formNoticeId.value = row.id
  formOpen.value = true
}

async function publish(row) {
  try {
    await publishNotice(row.id)
    ElMessage.success('已发布')
    load()
  } catch (error) {
    ElMessage.error(error.message || '发布失败')
  }
}

async function unpublish(row) {
  try {
    await unpublishNotice(row.id)
    ElMessage.success('已下架')
    load()
  } catch (error) {
    ElMessage.error(error.message || '下架失败')
  }
}

async function toggleTop(row) {
  try {
    await setNoticeTop(row.id, row.isTop !== 1)
    ElMessage.success(row.isTop === 1 ? '已取消置顶' : '已置顶')
    load()
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确定删除公告「${row.title}」吗？`, '删除公告', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  try {
    await deleteNotice(row.id)
    ElMessage.success('公告已删除')
    load()
  } catch (error) {
    ElMessage.error(error.message || '删除失败')
  }
}

onMounted(load)
</script>

<template>
  <div class="pb-4">
    <PageHeader title="公告管理" description="系统通知与公告，发布后写入发布时间">
      <template #actions>
        <el-button size="small" :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
        <el-button v-perm="'admin:SysNotice:create'" size="small" type="primary" :icon="Plus" @click="openCreate">
          新增公告
        </el-button>
      </template>
    </PageHeader>

    <div class="rg-page space-y-2">
      <FilterBar>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">标题</span>
          <el-input v-model="filters.title" size="small" placeholder="标题关键字" clearable style="width: 180px" @keyup.enter="search" />
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">类型</span>
          <el-select v-model="filters.type" size="small" style="width: 92px">
            <el-option label="全部" value="" />
            <el-option label="通知" :value="1" />
            <el-option label="公告" :value="2" />
          </el-select>
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">状态</span>
          <el-select v-model="filters.status" size="small" style="width: 92px">
            <el-option label="全部" value="" />
            <el-option label="草稿" :value="0" />
            <el-option label="已发布" :value="1" />
          </el-select>
        </label>
        <template #actions>
          <el-button size="small" type="primary" :icon="Search" @click="search">查询</el-button>
          <el-button size="small" @click="resetFilters">重置</el-button>
        </template>
      </FilterBar>

      <div class="rg-card min-w-0">
        <div class="rg-scroll-x">
          <el-table v-loading="loading" :data="rows" size="small" border stripe row-key="id" empty-text="没有匹配的公告">
            <el-table-column label="ID" width="50">
              <template #default="{ row }">
                <span class="font-mono text-[11px] text-[#64748b]">{{ row.id }}</span>
              </template>
            </el-table-column>
            <el-table-column label="标题" min-width="220">
              <template #default="{ row }">
                <div class="flex items-center gap-1">
                  <span v-if="row.isTop === 1" class="shrink-0 rounded bg-[#fdf1f5] px-1 text-[10px] text-[#a8435f]">置顶</span>
                  <span class="truncate text-[11px] font-medium text-[#1e293b]">{{ row.title }}</span>
                </div>
                <p class="truncate text-[10px] text-[#94a3b8]">{{ row.content }}</p>
              </template>
            </el-table-column>
            <el-table-column label="类型" width="66">
              <template #default="{ row }">
                <span class="rounded px-1.5 py-0.5 text-[10px]" :class="typeMeta[row.type]?.cls">
                  {{ typeMeta[row.type]?.label || row.type }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="76">
              <template #default="{ row }">
                <StatusTag :value="row.status" :labels="['草稿', '已发布']" :tone="row.status === 1 ? 'success' : 'muted'" />
              </template>
            </el-table-column>
            <el-table-column label="发布时间" width="134">
              <template #default="{ row }">
                <span class="font-mono text-[11px] text-[#64748b]">{{ formatDateTime(row.publishTime) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="创建时间" width="100">
              <template #default="{ row }">
                <span class="font-mono text-[11px] text-[#64748b]">{{ formatDate(row.createTime) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="230" fixed="right">
              <template #default="{ row }">
                <div class="flex items-center gap-0.5">
                  <el-button v-perm="'admin:SysNotice:update'" link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
                  <el-button
                    v-if="row.status !== 1"
                    v-perm="'admin:SysNotice:publish'"
                    link
                    type="primary"
                    size="small"
                    @click="publish(row)"
                  >
                    发布
                  </el-button>
                  <el-button
                    v-else
                    v-perm="'admin:SysNotice:unpublish'"
                    link
                    size="small"
                    @click="unpublish(row)"
                  >
                    下架
                  </el-button>
                  <el-button v-perm="'admin:SysNotice:setTop'" link size="small" @click="toggleTop(row)">
                    {{ row.isTop === 1 ? '取消置顶' : '置顶' }}
                  </el-button>
                  <el-button v-perm="'admin:SysNotice:delete'" link type="danger" size="small" @click="remove(row)">删除</el-button>
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

    <NoticeFormDrawer v-model="formOpen" :mode="formMode" :notice-id="formNoticeId" @saved="load" />
  </div>
</template>
