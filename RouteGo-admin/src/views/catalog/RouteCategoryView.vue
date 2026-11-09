<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Plus, Refresh } from '@element-plus/icons-vue'
import {
  createCategory,
  deleteCategory,
  getCategoryTree,
  setCategoryStatus,
  updateCategory,
} from '@/api/catalog'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'

const loading = ref(false)
const tree = ref([])
const dialogOpen = ref(false)
const saving = ref(false)
const editingId = ref(null)
const formRef = ref()

const form = reactive({ parentId: 0, name: '', sort: 0, status: 1, icon: '' })

const rules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
}

/** el-tree-select 需要 value/label 结构，这里把后端的 id/name 映射一遍 */
function toOptions(nodes) {
  return nodes.map((node) => ({
    value: node.id,
    label: node.name,
    children: node.children?.length ? toOptions(node.children) : undefined,
  }))
}

const options = computed(() => toOptions(tree.value))

async function load() {
  loading.value = true
  try {
    tree.value = await getCategoryTree()
  } catch (error) {
    ElMessage.error(error.message || '分类加载失败')
    tree.value = []
  } finally {
    loading.value = false
  }
}

function openCreate(parent) {
  editingId.value = null
  Object.assign(form, { parentId: parent?.id || 0, name: '', sort: 0, status: 1, icon: '' })
  dialogOpen.value = true
}

function openEdit(row) {
  editingId.value = row.id
  Object.assign(form, {
    parentId: row.parentId || 0,
    name: row.name,
    sort: row.sort ?? 0,
    status: row.status ?? 1,
    icon: row.icon || '',
  })
  dialogOpen.value = true
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload = { ...form, parentId: form.parentId || 0 }
    if (editingId.value) {
      await updateCategory(editingId.value, payload)
      ElMessage.success('分类已更新')
    } else {
      await createCategory(payload)
      ElMessage.success('分类已创建')
    }
    dialogOpen.value = false
    load()
  } catch (error) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function toggle(row) {
  try {
    await setCategoryStatus(row.id, row.status === 1 ? 0 : 1)
    ElMessage.success(row.status === 1 ? '已禁用' : '已启用')
    load()
  } catch (error) {
    ElMessage.error(error.message || '状态更新失败')
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除分类「${row.name}」吗？存在子分类或已挂线路时会被拒绝。`,
      '删除分类',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await deleteCategory(row.id)
    ElMessage.success('分类已删除')
    load()
  } catch (error) {
    ElMessage.error(error.message || '删除失败')
  }
}

onMounted(load)
</script>

<template>
  <div class="pb-4">
    <PageHeader title="线路分类" description="两级分类树，分类下挂有线路时不允许删除">
      <template #actions>
        <el-button size="small" :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
        <el-button v-perm="'catalog:RouteCategory:create'" size="small" type="primary" :icon="Plus" @click="openCreate(null)">
          新增分类
        </el-button>
      </template>
    </PageHeader>

    <div class="rg-page">
      <div class="rg-card min-w-0">
        <div class="rg-scroll-x">
          <el-table
            v-loading="loading"
            :data="tree"
            size="small"
            border
            row-key="id"
            default-expand-all
            :tree-props="{ children: 'children' }"
            empty-text="还没有分类"
          >
            <el-table-column label="分类名称" min-width="220">
              <template #default="{ row }">
                <span class="text-[11px] font-medium text-[#1e293b]">{{ row.name }}</span>
                <span v-if="row.icon" class="ml-1 font-mono text-[10px] text-[#94a3b8]">{{ row.icon }}</span>
              </template>
            </el-table-column>
            <el-table-column label="ID" width="60">
              <template #default="{ row }">
                <span class="font-mono text-[11px] text-[#64748b]">{{ row.id }}</span>
              </template>
            </el-table-column>
            <el-table-column label="子分类" width="70">
              <template #default="{ row }">
                <span class="font-mono text-[11px] text-[#64748b]">{{ row.children?.length || 0 }}</span>
              </template>
            </el-table-column>
            <el-table-column label="排序" width="60">
              <template #default="{ row }">
                <span class="font-mono text-[11px] text-[#64748b]">{{ row.sort ?? 0 }}</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="76">
              <template #default="{ row }">
                <StatusTag :value="row.status" :labels="['禁用', '启用']" :tone="row.status === 1 ? 'success' : 'muted'" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="240" fixed="right">
              <template #default="{ row }">
                <div class="flex items-center gap-0.5">
                  <el-button v-perm="'catalog:RouteCategory:create'" link type="primary" size="small" @click="openCreate(row)">
                    添加子分类
                  </el-button>
                  <el-button v-perm="'catalog:RouteCategory:update'" link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
                  <el-button v-perm="'catalog:RouteCategory:updateStatus'" link size="small" @click="toggle(row)">
                    {{ row.status === 1 ? '禁用' : '启用' }}
                  </el-button>
                  <el-button v-perm="'catalog:RouteCategory:delete'" link type="danger" size="small" @click="remove(row)">删除</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </div>

    <el-dialog v-model="dialogOpen" :title="editingId ? '编辑分类' : '新增分类'" width="440px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="small">
        <el-form-item label="上级分类">
          <el-tree-select
            v-model="form.parentId"
            :data="[{ value: 0, label: '顶级分类', children: options }]"
            check-strictly
            :render-after-expand="false"
            class="w-full"
          />
        </el-form-item>
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" placeholder="如：国内跟团" maxlength="50" />
        </el-form-item>
        <div class="grid grid-cols-2 gap-2">
          <el-form-item label="排序">
            <el-input-number v-model="form.sort" :min="0" class="w-full" controls-position="right" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="form.status" class="w-full">
              <el-option label="启用" :value="1" />
              <el-option label="禁用" :value="0" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="图标地址">
          <el-input v-model="form.icon" placeholder="可选，图标 URL" maxlength="255" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="flex justify-end gap-1">
          <el-button size="small" @click="dialogOpen = false">取消</el-button>
          <el-button size="small" type="primary" :loading="saving" @click="submit">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
