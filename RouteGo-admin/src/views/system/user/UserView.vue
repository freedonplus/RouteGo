<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { deleteUser, pageRoles, pageUsers, setUserStatus } from '@/api/system'
import { formatDate, formatDateTime } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'
import FilterBar from '@/components/FilterBar.vue'
import StatusTag from '@/components/StatusTag.vue'
import UserFormDrawer from './UserFormDrawer.vue'
import UserRoleDrawer from './UserRoleDrawer.vue'
import ResetPasswordDialog from './ResetPasswordDialog.vue'
import UserOauthDrawer from './UserOauthDrawer.vue'

const auth = useAuthStore()
const loading = ref(false)
const rows = ref([])
const roles = ref([])
const selection = ref([])
const tableRef = ref()

const filters = reactive({
  username: '',
  nickname: '',
  phone: '',
  status: '',
  roleId: '',
  createRange: [],
})
const page = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const formOpen = ref(false)
const formMode = ref('create')
const formUserId = ref(null)
const roleOpen = ref(false)
const roleUser = ref(null)
const passwordOpen = ref(false)
const passwordUser = ref(null)
const oauthOpen = ref(false)
const oauthUser = ref(null)

const canOperate = computed(() =>
  auth.hasPerm([
    'system:user:edit',
    'system:user:assign-role',
    'system:user:reset-password',
    'system:user:status',
    'system:user:delete',
  ])
)

function isProtected(row) {
  return row.username === 'admin'
}

function isSelf(row) {
  return row.id === auth.user?.userId
}

async function load() {
  loading.value = true
  try {
    const params = {
      username: filters.username || undefined,
      nickname: filters.nickname || undefined,
      phone: filters.phone || undefined,
      status: filters.status === '' ? undefined : filters.status,
      roleId: filters.roleId || undefined,
      pageNum: page.pageNum,
      pageSize: page.pageSize,
    }
    if (filters.createRange?.length === 2) {
      params.createTimeStart = filters.createRange[0]
      params.createTimeEnd = filters.createRange[1]
    }
    const result = await pageUsers(params)
    rows.value = result.records || []
    page.total = result.total || 0
  } catch (error) {
    ElMessage.error(error.message || '用户列表加载失败')
    rows.value = []
    page.total = 0
  } finally {
    loading.value = false
  }
}

async function loadRoles() {
  try {
    const result = await pageRoles({ pageNum: 1, pageSize: 200 })
    roles.value = result.records || []
  } catch {
    roles.value = []
  }
}

function search() {
  page.pageNum = 1
  load()
}

function resetFilters() {
  Object.assign(filters, {
    username: '',
    nickname: '',
    phone: '',
    status: '',
    roleId: '',
    createRange: [],
  })
  page.pageNum = 1
  load()
}

function openCreate() {
  formMode.value = 'create'
  formUserId.value = null
  formOpen.value = true
}

function openEdit(row) {
  formMode.value = 'edit'
  formUserId.value = row.id
  formOpen.value = true
}

function openRoles(row) {
  roleUser.value = row
  roleOpen.value = true
}

function openReset(row) {
  passwordUser.value = row
  passwordOpen.value = true
}

function openOauth(row) {
  oauthUser.value = row
  oauthOpen.value = true
}

async function toggleStatus(row) {
  const next = row.status === 1 ? 0 : 1
  const action = next === 1 ? '启用' : '停用'
  try {
    await ElMessageBox.confirm(`确定要${action}用户 ${row.username} 吗？`, `${action}用户`, {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  try {
    await setUserStatus(row.id, next)
    ElMessage.success(`已${action} ${row.username}`)
    load()
  } catch (error) {
    ElMessage.error(error.message || `${action}失败`)
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除用户 ${row.username} 吗？该操作会清理其角色关联。`,
      '删除用户',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await deleteUser(row.id)
    ElMessage.success('用户已删除')
    load()
  } catch (error) {
    ElMessage.error(error.message || '删除失败')
  }
}

async function batchStatus(status) {
  const targets = selection.value.filter((row) => !isProtected(row) && !isSelf(row))
  if (!targets.length) {
    ElMessage.warning('所选用户均受保护，无法操作')
    return
  }
  const action = status === 1 ? '启用' : '停用'
  try {
    await ElMessageBox.confirm(`确定批量${action} ${targets.length} 个用户吗？`, `批量${action}`, {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  const results = await Promise.allSettled(targets.map((row) => setUserStatus(row.id, status)))
  const failed = results.filter((item) => item.status === 'rejected').length
  if (failed) ElMessage.warning(`已${action} ${targets.length - failed} 个，失败 ${failed} 个`)
  else ElMessage.success(`已${action} ${targets.length} 个用户`)
  tableRef.value?.clearSelection()
  load()
}

function onSizeChange() {
  page.pageNum = 1
  load()
}

function onSelectionChange(value) {
  selection.value = value
}

onMounted(() => {
  load()
  loadRoles()
})
</script>

<template>
  <div class="pb-4">
    <PageHeader title="用户管理" description="系统账号、角色与登录状态">
      <template #actions>
        <el-button size="small" :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
        <el-button v-perm="'system:user:add'" size="small" type="primary" :icon="Plus" @click="openCreate">
          新增用户
        </el-button>
      </template>
    </PageHeader>

    <div class="rg-page space-y-2">
      <FilterBar>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">用户名</span>
          <el-input
            v-model="filters.username"
            size="small"
            placeholder="账号"
            clearable
            style="width: 128px"
            class="font-mono"
            @keyup.enter="search"
          />
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">昵称</span>
          <el-input
            v-model="filters.nickname"
            size="small"
            placeholder="昵称"
            clearable
            style="width: 120px"
            @keyup.enter="search"
          />
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">手机号</span>
          <el-input
            v-model="filters.phone"
            size="small"
            placeholder="手机号"
            clearable
            style="width: 128px"
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
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">角色</span>
          <el-select
            v-model="filters.roleId"
            size="small"
            clearable
            placeholder="全部"
            style="width: 128px"
          >
            <el-option
              v-for="role in roles"
              :key="role.id"
              :label="role.roleName"
              :value="role.id"
            />
          </el-select>
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">创建时间</span>
          <el-date-picker
            v-model="filters.createRange"
            type="daterange"
            size="small"
            value-format="YYYY-MM-DD"
            start-placeholder="开始"
            end-placeholder="结束"
            style="width: 220px"
          />
        </label>
        <template #actions>
          <el-button size="small" type="primary" :icon="Search" @click="search">查询</el-button>
          <el-button size="small" @click="resetFilters">重置</el-button>
        </template>
      </FilterBar>

      <div class="rg-card min-w-0">
        <div
          v-if="selection.length"
          class="flex flex-wrap items-center gap-2 border-b border-[#e2e8f0] bg-[#fdf1f5] px-3 py-1.5"
        >
          <span class="text-[11px] text-[#a8435f]">已选 {{ selection.length }} 项</span>
          <el-button v-perm="'system:user:status'" size="small" @click="batchStatus(1)">
            批量启用
          </el-button>
          <el-button v-perm="'system:user:status'" size="small" @click="batchStatus(0)">
            批量停用
          </el-button>
          <el-button link size="small" class="ml-auto" @click="tableRef?.clearSelection()">
            取消选择
          </el-button>
        </div>

        <div class="rg-scroll-x">
          <el-table
            ref="tableRef"
            v-loading="loading"
            :data="rows"
            size="small"
            border
            stripe
            row-key="id"
            empty-text="没有匹配的用户"
            @selection-change="onSelectionChange"
          >
            <el-table-column type="selection" width="36" />
            <el-table-column label="ID" width="50">
              <template #default="{ row }">
                <span class="font-mono text-[11px] text-[#64748b]">{{ row.id }}</span>
              </template>
            </el-table-column>
            <el-table-column label="用户" min-width="150">
              <template #default="{ row }">
                <div class="flex items-center gap-1.5">
                  <img
                    v-if="row.avatar"
                    :src="row.avatar"
                    alt=""
                    class="h-6 w-6 shrink-0 rounded object-cover"
                  />
                  <span
                    v-else
                    class="flex h-6 w-6 shrink-0 items-center justify-center rounded bg-[#fdf1f5] text-[10px] font-semibold text-[#a8435f]"
                  >
                    {{ (row.nickname || row.username).slice(0, 1) }}
                  </span>
                  <div class="min-w-0">
                    <p class="truncate text-[11px] font-medium text-[#1e293b]">
                      {{ row.nickname }}
                    </p>
                    <p class="truncate font-mono text-[10px] text-[#94a3b8]">
                      {{ row.username }}
                    </p>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="手机号" width="110">
              <template #default="{ row }">
                <span class="font-mono text-[11px]">{{ row.phone }}</span>
              </template>
            </el-table-column>
            <el-table-column label="邮箱" min-width="130">
              <template #default="{ row }">
                <span class="truncate font-mono text-[11px] text-[#64748b]">{{ row.email }}</span>
              </template>
            </el-table-column>
            <el-table-column label="角色" min-width="100">
              <template #default="{ row }">
                <div class="flex flex-wrap gap-1">
                  <span
                    v-for="name in row.roleNames"
                    :key="name"
                    class="rounded bg-[#fdf1f5] px-1.5 py-0.5 text-[10px] text-[#a8435f]"
                  >
                    {{ name }}
                  </span>
                  <span v-if="!row.roleNames?.length" class="text-[10px] text-[#94a3b8]">—</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="64">
              <template #default="{ row }">
                <StatusTag :value="row.status" />
              </template>
            </el-table-column>
            <el-table-column label="最后登录" width="134">
              <template #default="{ row }">
                <span
                  class="font-mono text-[11px]"
                  :title="row.lastLoginIp ? `登录 IP：${row.lastLoginIp}` : '—'"
                >
                  {{ formatDateTime(row.lastLoginTime) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="创建时间" width="100">
              <template #default="{ row }">
                <span class="font-mono text-[11px] text-[#64748b]">
                  {{ formatDate(row.createTime) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column v-if="canOperate" label="操作" width="278" fixed="right">
              <template #default="{ row }">
                <div class="flex items-center gap-0.5">
                  <el-button
                    v-perm="'system:user:edit'"
                    link
                    type="primary"
                    size="small"
                    @click="openEdit(row)"
                  >
                    编辑
                  </el-button>
                  <el-button
                    v-perm="'system:user:assign-role'"
                    link
                    type="primary"
                    size="small"
                    @click="openRoles(row)"
                  >
                    角色
                  </el-button>
                  <el-button
                    v-perm="'system:user:edit'"
                    link
                    type="primary"
                    size="small"
                    @click="openOauth(row)"
                  >
                    绑定
                  </el-button>
                  <el-button
                    v-perm="'system:user:reset-password'"
                    link
                    type="primary"
                    size="small"
                    @click="openReset(row)"
                  >
                    重置
                  </el-button>
                  <el-button
                    v-perm="'system:user:status'"
                    link
                    size="small"
                    :disabled="isProtected(row) || isSelf(row)"
                    :title="isProtected(row) || isSelf(row) ? '受保护账号不能操作' : ''"
                    @click="toggleStatus(row)"
                  >
                    {{ row.status === 1 ? '停用' : '启用' }}
                  </el-button>
                  <el-button
                    v-perm="'system:user:delete'"
                    link
                    type="danger"
                    size="small"
                    :disabled="isProtected(row) || isSelf(row)"
                    :title="isProtected(row) || isSelf(row) ? '受保护账号不能删除' : ''"
                    @click="remove(row)"
                  >
                    删除
                  </el-button>
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
            @size-change="onSizeChange"
          />
        </div>
      </div>
    </div>

    <UserFormDrawer
      v-model="formOpen"
      :mode="formMode"
      :user-id="formUserId"
      @saved="load"
    />
    <UserRoleDrawer v-model="roleOpen" :user="roleUser" @saved="load" />
    <ResetPasswordDialog v-model="passwordOpen" :user="passwordUser" />
    <UserOauthDrawer v-model="oauthOpen" :user="oauthUser" />
  </div>
</template>
