<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { deleteMenu, getMenuTree, setMenuStatus, setMenuVisible } from '@/api/system'
import PageHeader from '@/components/PageHeader.vue'
import FilterBar from '@/components/FilterBar.vue'
import { resolveMenuIcon } from '@/components/menu-icons'
import MenuFormDrawer from './MenuFormDrawer.vue'

const loading = ref(false)
const tree = ref([])
const filters = reactive({ keyword: '', type: '' })
const tableKey = ref(0)

const formOpen = ref(false)
const formMode = ref('create')
const formMenuId = ref(null)
const formParentId = ref(0)

const typeMeta = {
  M: { label: '目录', cls: 'bg-[#eff6ff] text-[#1d4ed8]' },
  C: { label: '菜单', cls: 'bg-[#ecfdf3] text-[#15803d]' },
  F: { label: '按钮', cls: 'bg-[#f1f5f9] text-[#475569]' },
}

function filterTree(nodes, keyword, type) {
  const result = []
  nodes.forEach((node) => {
    const children = filterTree(node.children || [], keyword, type)
    const matchesKeyword =
      !keyword ||
      node.menuName.includes(keyword) ||
      String(node.path || '').includes(keyword) ||
      String(node.perms || '').includes(keyword)
    const matchesType = !type || node.menuType === type
    if ((matchesKeyword && matchesType) || children.length) {
      result.push({ ...node, children })
    }
  })
  return result
}

const filtered = computed(() =>
  filterTree(tree.value, filters.keyword.trim(), filters.type)
)

watch(
  () => [filters.keyword, filters.type],
  () => {
    tableKey.value += 1
  }
)

async function load() {
  loading.value = true
  try {
    tree.value = (await getMenuTree()) || []
    tableKey.value += 1
  } catch (error) {
    ElMessage.error(error.message || '菜单树加载失败')
    tree.value = []
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.keyword = ''
  filters.type = ''
}

function openCreate(parentRow = null) {
  formMode.value = 'create'
  formMenuId.value = null
  formParentId.value = parentRow?.id || 0
  formOpen.value = true
}

function openEdit(row) {
  formMode.value = 'edit'
  formMenuId.value = row.id
  formParentId.value = row.parentId
  formOpen.value = true
}

async function toggleField(row, field, enabled) {
  const value = enabled ? 1 : 0
  const api = field === 'visible' ? setMenuVisible : setMenuStatus
  try {
    await api(row.id, value)
    row[field] = value
    ElMessage.success(
      field === 'visible' ? (value ? '已显示' : '已隐藏') : value ? '已启用' : '已禁用'
    )
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
    load()
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除「${row.menuName}」吗？删除后不可恢复。`,
      '删除菜单',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await deleteMenu(row.id)
    ElMessage.success('菜单已删除')
    load()
  } catch (error) {
    ElMessage.error(error.message || '删除失败')
  }
}

onMounted(load)
</script>

<template>
  <div class="pb-4">
    <PageHeader title="菜单管理" description="目录、页面与按钮级权限标识">
      <template #actions>
        <el-button size="small" :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
        <el-button
          v-perm="'system:menu:add'"
          size="small"
          type="primary"
          :icon="Plus"
          @click="openCreate()"
        >
          新增菜单
        </el-button>
      </template>
    </PageHeader>

    <div class="rg-page space-y-2">
      <FilterBar>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">关键字</span>
          <el-input
            v-model="filters.keyword"
            size="small"
            placeholder="名称 / 路径 / 权限码"
            clearable
            style="width: 200px"
            @keyup.enter="tableKey += 1"
          />
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">类型</span>
          <el-select v-model="filters.type" size="small" style="width: 100px">
            <el-option label="全部" value="" />
            <el-option label="目录" value="M" />
            <el-option label="菜单" value="C" />
            <el-option label="按钮" value="F" />
          </el-select>
        </label>
        <template #actions>
          <el-button size="small" type="primary" :icon="Search" @click="tableKey += 1">
            查询
          </el-button>
          <el-button size="small" @click="resetFilters">重置</el-button>
        </template>
      </FilterBar>

      <div class="rg-card min-w-0">
        <div class="rg-scroll-x">
          <el-table
            :key="tableKey"
            v-loading="loading"
            :data="filtered"
            size="small"
            border
            stripe
            row-key="id"
            :tree-props="{ children: 'children' }"
            default-expand-all
            empty-text="没有匹配的菜单"
          >
            <el-table-column label="菜单名称" min-width="190">
              <template #default="{ row }">
                <div class="flex items-center gap-1.5">
                  <el-icon class="shrink-0 text-[#64748b]">
                    <component :is="resolveMenuIcon(row.icon)" />
                  </el-icon>
                  <span class="truncate text-[11px] font-medium text-[#1e293b]">
                    {{ row.menuName }}
                  </span>
                  <span class="shrink-0 font-mono text-[10px] text-[#94a3b8]">#{{ row.id }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="类型" width="64">
              <template #default="{ row }">
                <span
                  class="rounded px-1.5 py-0.5 text-[10px]"
                  :class="typeMeta[row.menuType]?.cls"
                >
                  {{ typeMeta[row.menuType]?.label || row.menuType }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="路由路径" width="150">
              <template #default="{ row }">
                <span class="truncate font-mono text-[11px] text-[#64748b]">
                  {{ row.path || '—' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="组件" width="160">
              <template #default="{ row }">
                <span class="truncate font-mono text-[11px] text-[#64748b]">
                  {{ row.component || '—' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="权限标识" width="160">
              <template #default="{ row }">
                <span class="truncate font-mono text-[11px] text-[#64748b]">
                  {{ row.perms || '—' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="排序" width="58" align="right">
              <template #default="{ row }">
                <span class="font-mono text-[11px]">{{ row.sort }}</span>
              </template>
            </el-table-column>
            <el-table-column label="显示" width="64" align="center">
              <template #default="{ row }">
                <el-switch
                  v-perm="'system:menu:edit'"
                  size="small"
                  :model-value="row.visible === 1"
                  :disabled="row.menuType === 'F'"
                  @change="(value) => toggleField(row, 'visible', value)"
                />
                <span v-if="!row.visible" class="ml-1 text-[10px] text-[#94a3b8]">隐藏</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="64" align="center">
              <template #default="{ row }">
                <el-switch
                  v-perm="'system:menu:status'"
                  size="small"
                  :model-value="row.status === 1"
                  @change="(value) => toggleField(row, 'status', value)"
                />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="176" fixed="right">
              <template #default="{ row }">
                <div class="flex items-center gap-0.5">
                  <el-button
                    v-if="row.menuType !== 'F'"
                    v-perm="'system:menu:add'"
                    link
                    type="primary"
                    size="small"
                    @click="openCreate(row)"
                  >
                    新增子项
                  </el-button>
                  <el-button
                    v-perm="'system:menu:edit'"
                    link
                    type="primary"
                    size="small"
                    @click="openEdit(row)"
                  >
                    编辑
                  </el-button>
                  <el-button
                    v-perm="'system:menu:delete'"
                    link
                    type="danger"
                    size="small"
                    :disabled="row.childCount > 0 || row.roleCount > 0"
                    :title="
                      row.childCount > 0
                        ? '存在子菜单'
                        : row.roleCount > 0
                          ? '角色仍引用该菜单'
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
        <div class="border-t border-[#e2e8f0] px-3 py-1.5">
          <span class="font-mono text-[10px] text-[#94a3b8]">
            共 {{ tree.length }} 个顶级节点 · 删除前需先解除角色引用与子级
          </span>
        </div>
      </div>
    </div>

    <MenuFormDrawer
      v-model="formOpen"
      :mode="formMode"
      :menu-id="formMenuId"
      :parent-id="formParentId"
      @saved="load"
    />
  </div>
</template>
