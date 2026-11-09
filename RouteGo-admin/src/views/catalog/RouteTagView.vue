<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { createTag, deleteTag, pageTags, setTagStatus, updateTag } from '@/api/catalog'
import { formatDate } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'
import FilterBar from '@/components/FilterBar.vue'
import StatusTag from '@/components/StatusTag.vue'

const loading = ref(false)
const rows = ref([])
const filters = reactive({ tagName: '', status: '' })
const page = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const dialogOpen = ref(false)
const saving = ref(false)
const editingId = ref(null)
const formRef = ref()
const form = reactive({ tagName: '', color: '#a8435f', sort: 0, status: 1 })

const rules = {
  tagName: [{ required: true, message: '请输入标签名称', trigger: 'blur' }],
}

async function load() {
  loading.value = true
  try {
    const result = await pageTags({
      tagName: filters.tagName || undefined,
      status: filters.status === '' ? undefined : filters.status,
      pageNum: page.pageNum,
      pageSize: page.pageSize,
    })
    rows.value = result.records || []
    page.total = Number(result.total || 0)
  } catch (error) {
    ElMessage.error(error.message || '标签加载失败')
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
  Object.assign(filters, { tagName: '', status: '' })
  page.pageNum = 1
  load()
}

function openCreate() {
  editingId.value = null
  Object.assign(form, { tagName: '', color: '#a8435f', sort: 0, status: 1 })
  dialogOpen.value = true
}

function openEdit(row) {
  editingId.value = row.id
  Object.assign(form, {
    tagName: row.tagName,
    color: row.color || '#a8435f',
    sort: row.sort ?? 0,
    status: row.status ?? 1,
  })
  dialogOpen.value = true
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (editingId.value) {
      await updateTag(editingId.value, { ...form })
      ElMessage.success('标签已更新')
    } else {
      await createTag({ ...form })
      ElMessage.success('标签已创建')
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
    await setTagStatus(row.id, row.status === 1 ? 0 : 1)
    ElMessage.success(row.status === 1 ? '已禁用' : '已启用')
    load()
  } catch (error) {
    ElMessage.error(error.message || '状态更新失败')
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确定删除标签「${row.tagName}」吗？`, '删除标签', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  try {
    await deleteTag(row.id)
    ElMessage.success('标签已删除')
    load()
  } catch (error) {
    ElMessage.error(error.message || '删除失败')
  }
}

onMounted(load)
</script>

<template>
  <div class="pb-4">
    <PageHeader title="线路标签" description="标签用于线路筛选与首页聚合，禁用后不再出现在可选列表">
      <template #actions>
        <el-button size="small" :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
        <el-button v-perm="'catalog:RouteTag:create'" size="small" type="primary" :icon="Plus" @click="openCreate">
          新增标签
        </el-button>
      </template>
    </PageHeader>

    <div class="rg-page space-y-2">
      <FilterBar>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">标签名称</span>
          <el-input v-model="filters.tagName" size="small" placeholder="亲子" clearable style="width: 140px" @keyup.enter="search" />
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">状态</span>
          <el-select v-model="filters.status" size="small" style="width: 92px">
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
          <el-table v-loading="loading" :data="rows" size="small" border stripe row-key="id" empty-text="没有匹配的标签">
            <el-table-column label="ID" width="50">
              <template #default="{ row }">
                <span class="font-mono text-[11px] text-[#64748b]">{{ row.id }}</span>
              </template>
            </el-table-column>
            <el-table-column label="标签" min-width="160">
              <template #default="{ row }">
                <span class="inline-flex items-center gap-1.5">
                  <span class="h-2.5 w-2.5 rounded-sm" :style="{ backgroundColor: row.color || '#cbd5e1' }" />
                  <span class="text-[11px] font-medium text-[#1e293b]">{{ row.tagName }}</span>
                </span>
              </template>
            </el-table-column>
            <el-table-column label="色值" width="110">
              <template #default="{ row }">
                <span class="font-mono text-[11px] text-[#64748b]">{{ row.color || '—' }}</span>
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
            <el-table-column label="创建时间" width="110">
              <template #default="{ row }">
                <span class="font-mono text-[11px] text-[#64748b]">{{ formatDate(row.createTime) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="170" fixed="right">
              <template #default="{ row }">
                <el-button v-perm="'catalog:RouteTag:update'" link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
                <el-button v-perm="'catalog:RouteTag:updateStatus'" link size="small" @click="toggle(row)">
                  {{ row.status === 1 ? '禁用' : '启用' }}
                </el-button>
                <el-button v-perm="'catalog:RouteTag:delete'" link type="danger" size="small" @click="remove(row)">删除</el-button>
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

    <el-dialog v-model="dialogOpen" :title="editingId ? '编辑标签' : '新增标签'" width="420px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="small">
        <el-form-item label="标签名称" prop="tagName">
          <el-input v-model="form.tagName" placeholder="如：亲子游" maxlength="50" />
        </el-form-item>
        <div class="grid grid-cols-2 gap-2">
          <el-form-item label="颜色">
            <el-color-picker v-model="form.color" />
            <span class="ml-2 font-mono text-[10px] text-[#94a3b8]">{{ form.color }}</span>
          </el-form-item>
          <el-form-item label="排序">
            <el-input-number v-model="form.sort" :min="0" class="w-full" controls-position="right" />
          </el-form-item>
        </div>
        <el-form-item label="状态">
          <el-select v-model="form.status" class="w-full">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
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
