<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  deletePermission,
  getGroupedPermissions,
  pagePermissions,
  setPermissionStatus,
} from '@/api/system'
import { useAuthStore } from '@/stores/auth'
import PageHeader from '@/components/PageHeader.vue'
import FilterBar from '@/components/FilterBar.vue'
import PermissionTable from './PermissionTable.vue'
import PermissionFormDrawer from './PermissionFormDrawer.vue'

const auth = useAuthStore()
const viewMode = ref('list')
const loading = ref(false)
const rows = ref([])
const groups = ref([])
const filters = reactive({ permissionName: '', permissionCode: '', module: '', status: '' })
const page = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const formOpen = ref(false)
const formMode = ref('create')
const formPermissionId = ref(null)

async function loadList() {
  loading.value = true
  try {
    const result = await pagePermissions({
      permissionName: filters.permissionName || undefined,
      permissionCode: filters.permissionCode || undefined,
      module: filters.module || undefined,
      status: filters.status === '' ? undefined : filters.status,
      pageNum: page.pageNum,
      pageSize: page.pageSize,
    })
    rows.value = result.records || []
    page.total = Number(result.total || 0)
  } catch (error) {
    ElMessage.error(error.message || '权限点加载失败')
    rows.value = []
    page.total = 0
  } finally {
    loading.value = false
  }
}

async function loadGrouped() {
  loading.value = true
  try {
    groups.value = (await getGroupedPermissions()) || []
  } catch (error) {
    ElMessage.error(error.message || '权限点加载失败')
    groups.value = []
  } finally {
    loading.value = false
  }
}

function refresh() {
  if (viewMode.value === 'list') loadList()
  else loadGrouped()
}

function search() {
  page.pageNum = 1
  loadList()
}

async function refreshSession() {
  try {
    await auth.loadSession()
  } catch {
    // 会话失效时拦截器会跳转登录
  }
}

async function onSaved() {
  refresh()
  await refreshSession()
}

function resetFilters() {
  Object.assign(filters, { permissionName: '', permissionCode: '', module: '', status: '' })
  page.pageNum = 1
  loadList()
}

watch(viewMode, () => refresh())

function openCreate() {
  formMode.value = 'create'
  formPermissionId.value = null
  formOpen.value = true
}

function openEdit(row) {
  formMode.value = 'edit'
  formPermissionId.value = row.id
  formOpen.value = true
}

async function toggleStatus(row) {
  const next = row.status === 1 ? 0 : 1
  const action = next === 1 ? '启用' : '停用'
  try {
    await setPermissionStatus(row.id, next)
    ElMessage.success(`已${action} ${row.permissionName}`)
    refresh()
    await refreshSession()
  } catch (error) {
    ElMessage.error(error.message || `${action}失败`)
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除权限点 ${row.permissionCode} 吗？`,
      '删除权限点',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await deletePermission(row.id)
    ElMessage.success('权限点已删除')
    refresh()
    await refreshSession()
  } catch (error) {
    ElMessage.error(error.message || '删除失败')
  }
}

function onSizeChange() {
  page.pageNum = 1
  loadList()
}

onMounted(loadList)
</script>

<template>
  <div class="pb-4">
    <PageHeader title="权限点" description="接口级权限码，按模块分组维护">
      <template #actions>
        <el-radio-group v-model="viewMode" size="small">
          <el-radio-button value="list">列表</el-radio-button>
          <el-radio-button value="group">分组</el-radio-button>
        </el-radio-group>
        <el-button size="small" :icon="Refresh" :loading="loading" @click="refresh">刷新</el-button>
        <el-button
          v-perm="'admin:SysPermission:create'"
          size="small"
          type="primary"
          :icon="Plus"
          @click="openCreate"
        >
          新增权限点
        </el-button>
      </template>
    </PageHeader>

    <div class="rg-page space-y-2">
      <FilterBar v-if="viewMode === 'list'">
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">权限名称</span>
          <el-input
            v-model="filters.permissionName"
            size="small"
            placeholder="名称"
            clearable
            style="width: 140px"
            @keyup.enter="search"
          />
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">权限码</span>
          <el-input
            v-model="filters.permissionCode"
            size="small"
            placeholder="admin:SysUser:page"
            clearable
            style="width: 180px"
            class="font-mono"
            @keyup.enter="search"
          />
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">模块</span>
          <el-input
            v-model="filters.module"
            size="small"
            placeholder="system"
            clearable
            style="width: 110px"
            class="font-mono"
            @keyup.enter="search"
          />
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">状态</span>
          <el-select v-model="filters.status" size="small" style="width: 96px">
            <el-option label="全部" value="" />
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </label>
        <template #actions>
          <el-button size="small" type="primary" :icon="Search" @click="search">查询</el-button>
          <el-button size="small" @click="resetFilters">重置</el-button>
        </template>
      </FilterBar>

      <div v-if="viewMode === 'list'" class="rg-card min-w-0">
        <div class="rg-scroll-x">
          <PermissionTable
            :data="rows"
            :loading="loading"
            @edit="openEdit"
            @toggle="toggleStatus"
            @remove="remove"
          />
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
            @current-change="loadList"
            @size-change="onSizeChange"
          />
        </div>
      </div>

      <div v-else class="rg-card min-w-0">
        <div
          v-for="(group, index) in groups"
          :key="group.module"
          :class="index ? 'border-t border-[#e2e8f0]' : ''"
        >
          <div class="flex flex-wrap items-center gap-2 bg-[#f8fafc] px-3 py-1.5">
            <span class="rg-section-title">{{ group.module }}</span>
            <span class="font-mono text-[10px] text-[#94a3b8]">
              {{ group.permissions.length }} 项
            </span>
            <span class="ml-auto text-[10px] text-[#94a3b8]">按模块分组 · 全部权限点</span>
          </div>
          <div class="rg-scroll-x">
            <PermissionTable
              :data="group.permissions"
              :show-module="false"
              :loading="loading"
              @edit="openEdit"
              @toggle="toggleStatus"
              @remove="remove"
            />
          </div>
        </div>
        <p v-if="!groups.length && !loading" class="px-3 py-6 text-center text-xs text-[#94a3b8]">
          暂无权限点
        </p>
      </div>
    </div>

    <PermissionFormDrawer
      v-model="formOpen"
      :mode="formMode"
      :permission-id="formPermissionId"
      @saved="onSaved"
    />
  </div>
</template>
