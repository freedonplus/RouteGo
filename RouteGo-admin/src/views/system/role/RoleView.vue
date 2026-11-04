<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { deleteRole, pageRoles, setRoleStatus } from '@/api/system'
import { useAuthStore } from '@/stores/auth'
import { formatDate } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'
import FilterBar from '@/components/FilterBar.vue'
import StatusTag from '@/components/StatusTag.vue'
import RoleFormDrawer from './RoleFormDrawer.vue'
import RoleMenuDrawer from './RoleMenuDrawer.vue'
import RolePermissionDrawer from './RolePermissionDrawer.vue'
import RoleUsersDrawer from './RoleUsersDrawer.vue'

const auth = useAuthStore()
const loading = ref(false)
const rows = ref([])
const filters = reactive({ roleName: '', roleCode: '', status: '' })
const page = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const formOpen = ref(false)
const formMode = ref('create')
const formRoleId = ref(null)
const menuOpen = ref(false)
const permissionOpen = ref(false)
const usersOpen = ref(false)
const currentRole = ref(null)

const canOperate = computed(() =>
  auth.hasPerm([
    'system:role:list',
    'system:role:edit',
    'system:role:assign-menu',
    'system:role:assign-permission',
    'system:role:status',
    'system:role:delete',
  ])
)

async function load() {
  loading.value = true
  try {
    const result = await pageRoles({
      roleName: filters.roleName || undefined,
      roleCode: filters.roleCode || undefined,
      status: filters.status === '' ? undefined : filters.status,
      pageNum: page.pageNum,
      pageSize: page.pageSize,
    })
    rows.value = result.records || []
    page.total = result.total || 0
  } catch (error) {
    ElMessage.error(error.message || '角色列表加载失败')
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

async function refreshAfterAuthChange() {
  await load()
  try {
    await auth.loadSession()
  } catch {
    // 会话失效时拦截器会跳转登录
  }
}

function resetFilters() {
  Object.assign(filters, { roleName: '', roleCode: '', status: '' })
  page.pageNum = 1
  load()
}

function openCreate() {
  formMode.value = 'create'
  formRoleId.value = null
  formOpen.value = true
}

function openEdit(row) {
  formMode.value = 'edit'
  formRoleId.value = row.id
  formOpen.value = true
}

function openMenus(row) {
  currentRole.value = row
  menuOpen.value = true
}

function openPermissions(row) {
  currentRole.value = row
  permissionOpen.value = true
}

function openUsers(row) {
  currentRole.value = row
  usersOpen.value = true
}

async function toggleStatus(row) {
  const next = row.status === 1 ? 0 : 1
  const action = next === 1 ? '启用' : '停用'
  try {
    await ElMessageBox.confirm(
      `确定要${action}角色 ${row.roleName} 吗？该角色下的用户会被踢下线。`,
      `${action}角色`,
      { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await setRoleStatus(row.id, next)
    ElMessage.success(`已${action} ${row.roleName}`)
    load()
  } catch (error) {
    ElMessage.error(error.message || `${action}失败`)
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除角色 ${row.roleName} 吗？该操作不可恢复。`,
      '删除角色',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await deleteRole(row.id)
    ElMessage.success('角色已删除')
    load()
  } catch (error) {
    ElMessage.error(error.message || '删除失败')
  }
}

function onSizeChange() {
  page.pageNum = 1
  load()
}

onMounted(load)
</script>

<template>
  <div class="pb-4">
    <PageHeader title="角色管理" description="角色、菜单与数据权限">
      <template #actions>
        <el-button size="small" :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
        <el-button
          v-perm="'system:role:add'"
          size="small"
          type="primary"
          :icon="Plus"
          @click="openCreate"
        >
          新增角色
        </el-button>
      </template>
    </PageHeader>

    <div class="rg-page space-y-2">
      <FilterBar>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">角色名称</span>
          <el-input
            v-model="filters.roleName"
            size="small"
            placeholder="名称"
            clearable
            style="width: 140px"
            @keyup.enter="search"
          />
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">角色编码</span>
          <el-input
            v-model="filters.roleCode"
            size="small"
            placeholder="编码"
            clearable
            style="width: 130px"
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

      <div class="rg-card min-w-0">
        <div class="rg-scroll-x">
          <el-table
            v-loading="loading"
            :data="rows"
            size="small"
            border
            stripe
            row-key="id"
            empty-text="没有匹配的角色"
          >
            <el-table-column label="ID" width="50">
              <template #default="{ row }">
                <span class="font-mono text-[11px] text-[#64748b]">{{ row.id }}</span>
              </template>
            </el-table-column>
            <el-table-column label="角色名称" min-width="160">
              <template #default="{ row }">
                <div class="flex items-center gap-1.5">
                  <span class="truncate text-[11px] font-medium text-[#1e293b]">
                    {{ row.roleName }}
                  </span>
                  <span
                    v-if="row.builtIn"
                    class="shrink-0 rounded bg-[#eff6ff] px-1.5 py-0.5 text-[10px] text-[#1d4ed8]"
                  >
                    内置
                  </span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="编码" width="130">
              <template #default="{ row }">
                <span class="font-mono text-[11px] text-[#64748b]">{{ row.roleCode }}</span>
              </template>
            </el-table-column>
            <el-table-column label="排序" width="60" align="right">
              <template #default="{ row }">
                <span class="font-mono text-[11px]">{{ row.sort }}</span>
              </template>
            </el-table-column>
            <el-table-column label="用户数" width="70" align="right">
              <template #default="{ row }">
                <span class="font-mono text-[11px]">{{ row.userCount }}</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="64">
              <template #default="{ row }">
                <StatusTag :value="row.status" />
              </template>
            </el-table-column>
            <el-table-column label="创建时间" width="100">
              <template #default="{ row }">
                <span class="font-mono text-[11px] text-[#64748b]">
                  {{ formatDate(row.createTime) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column v-if="canOperate" label="操作" width="286" fixed="right">
              <template #default="{ row }">
                <div class="flex items-center gap-0.5">
                  <el-button
                    v-perm="'system:role:edit'"
                    link
                    type="primary"
                    size="small"
                    @click="openEdit(row)"
                  >
                    编辑
                  </el-button>
                  <el-button
                    v-perm="'system:role:assign-menu'"
                    link
                    type="primary"
                    size="small"
                    @click="openMenus(row)"
                  >
                    菜单
                  </el-button>
                  <el-button
                    v-perm="'system:role:assign-permission'"
                    link
                    type="primary"
                    size="small"
                    @click="openPermissions(row)"
                  >
                    权限
                  </el-button>
                  <el-button
                    v-perm="'system:role:list'"
                    link
                    type="primary"
                    size="small"
                    @click="openUsers(row)"
                  >
                    用户
                  </el-button>
                  <el-button
                    v-perm="'system:role:status'"
                    link
                    size="small"
                    :disabled="row.builtIn"
                    :title="row.builtIn ? '内置角色不能停用' : ''"
                    @click="toggleStatus(row)"
                  >
                    {{ row.status === 1 ? '停用' : '启用' }}
                  </el-button>
                  <el-button
                    v-perm="'system:role:delete'"
                    link
                    type="danger"
                    size="small"
                    :disabled="row.builtIn || row.userCount > 0"
                    :title="
                      row.builtIn
                        ? '内置角色不能删除'
                        : row.userCount > 0
                          ? '角色下仍有用户'
                          : ''
                    "
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

    <RoleFormDrawer v-model="formOpen" :mode="formMode" :role-id="formRoleId" @saved="load" />
    <RoleMenuDrawer v-model="menuOpen" :role="currentRole" @saved="refreshAfterAuthChange" />
    <RolePermissionDrawer
      v-model="permissionOpen"
      :role="currentRole"
      @saved="refreshAfterAuthChange"
    />
    <RoleUsersDrawer v-model="usersOpen" :role="currentRole" />
  </div>
</template>
