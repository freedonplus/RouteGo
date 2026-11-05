<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  createDeparture,
  deleteDeparture,
  pageDepartures,
  pageRoutes,
  setDepartureStatus,
  updateDeparture,
} from '@/api/catalog'
import { formatMoney } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'
import FilterBar from '@/components/FilterBar.vue'
import StatusTag from '@/components/StatusTag.vue'
import RemotePicker from '@/components/RemotePicker.vue'

const loading = ref(false)
const rows = ref([])
const filters = reactive({ routeId: null, status: '', startDate: '', endDate: '' })
const dateRange = ref([])
const page = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const dialogOpen = ref(false)
const saving = ref(false)
const editingId = ref(null)
const editingRouteLabel = ref('')
const formRef = ref()
const form = reactive({
  routeId: null,
  departDate: '',
  adultPrice: 0,
  childPrice: null,
  stock: 10,
  status: 1,
})

const rules = {
  routeId: [{ required: true, message: '请选择线路', trigger: 'change' }],
  departDate: [{ required: true, message: '请选择出发日期', trigger: 'change' }],
  adultPrice: [{ required: true, message: '请输入成人价', trigger: 'blur' }],
  stock: [{ required: true, message: '请输入库存', trigger: 'blur' }],
}

function fetchRoutes(keyword) {
  return pageRoutes({ title: keyword || undefined, pageNum: 1, pageSize: 20 })
}

async function load() {
  loading.value = true
  try {
    const result = await pageDepartures({
      routeId: filters.routeId || undefined,
      status: filters.status === '' ? undefined : filters.status,
      startDate: dateRange.value?.[0] || undefined,
      endDate: dateRange.value?.[1] || undefined,
      pageNum: page.pageNum,
      pageSize: page.pageSize,
    })
    rows.value = result.records || []
    page.total = Number(result.total || 0)
  } catch (error) {
    ElMessage.error(error.message || '班期加载失败')
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
  Object.assign(filters, { routeId: null, status: '' })
  dateRange.value = []
  page.pageNum = 1
  load()
}

function openCreate() {
  editingId.value = null
  editingRouteLabel.value = ''
  Object.assign(form, {
    routeId: filters.routeId || null,
    departDate: '',
    adultPrice: 0,
    childPrice: null,
    stock: 10,
    status: 1,
  })
  dialogOpen.value = true
}

function openEdit(row) {
  editingId.value = row.id
  editingRouteLabel.value = row.routeTitle || ''
  Object.assign(form, {
    routeId: row.routeId,
    departDate: row.departDate,
    adultPrice: Number(row.adultPrice ?? 0),
    childPrice: row.childPrice === null || row.childPrice === undefined ? null : Number(row.childPrice),
    stock: row.stock ?? 0,
    status: row.status ?? 1,
  })
  dialogOpen.value = true
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload = { ...form }
    if (editingId.value) {
      await updateDeparture(editingId.value, payload)
      ElMessage.success('班期已更新')
    } else {
      await createDeparture(payload)
      ElMessage.success('班期已创建')
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
    await setDepartureStatus(row.id, row.status === 1 ? 0 : 1)
    ElMessage.success(row.status === 1 ? '已停售' : '已恢复可售')
    load()
  } catch (error) {
    ElMessage.error(error.message || '状态更新失败')
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除 ${row.routeTitle || ''} ${row.departDate} 的班期吗？有成交的班期只能停售。`,
      '删除班期',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await deleteDeparture(row.id)
    ElMessage.success('班期已删除')
    load()
  } catch (error) {
    ElMessage.error(error.message || '删除失败')
  }
}

onMounted(load)
</script>

<template>
  <div class="pb-4">
    <PageHeader title="出发班期" description="班期是库存与价格的来源；库存不能低于已售，有成交只能停售">
      <template #actions>
        <el-button size="small" :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
        <el-button v-perm="'catalog:RouteDeparture:create'" size="small" type="primary" :icon="Plus" @click="openCreate">
          新增班期
        </el-button>
      </template>
    </PageHeader>

    <div class="rg-page space-y-2">
      <FilterBar>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">线路</span>
          <div style="width: 220px">
            <RemotePicker
              v-model="filters.routeId"
              :fetcher="fetchRoutes"
              :formatter="(row) => `${row.title}（${row.routeNo}）`"
              placeholder="搜索线路标题"
            />
          </div>
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">出发日期</span>
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始"
            end-placeholder="结束"
            size="small"
            style="width: 224px"
          />
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">状态</span>
          <el-select v-model="filters.status" size="small" style="width: 92px">
            <el-option label="全部" value="" />
            <el-option label="可售" :value="1" />
            <el-option label="停售" :value="0" />
          </el-select>
        </label>
        <template #actions>
          <el-button size="small" type="primary" :icon="Search" @click="search">查询</el-button>
          <el-button size="small" @click="resetFilters">重置</el-button>
        </template>
      </FilterBar>

      <div class="rg-card min-w-0">
        <div class="rg-scroll-x">
          <el-table v-loading="loading" :data="rows" size="small" border stripe row-key="id" empty-text="没有匹配的班期">
            <el-table-column label="线路" min-width="200">
              <template #default="{ row }">
                <span class="truncate text-[11px] font-medium text-[#1e293b]">{{ row.routeTitle || `#${row.routeId}` }}</span>
              </template>
            </el-table-column>
            <el-table-column label="出发日期" width="110">
              <template #default="{ row }">
                <span class="font-mono text-[11px]">{{ row.departDate }}</span>
              </template>
            </el-table-column>
            <el-table-column label="成人价" width="100">
              <template #default="{ row }">
                <span class="font-mono text-[11px] font-medium text-[#a8435f]">{{ formatMoney(row.adultPrice) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="儿童价" width="100">
              <template #default="{ row }">
                <span class="font-mono text-[11px] text-[#64748b]">{{ formatMoney(row.childPrice) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="库存" width="70">
              <template #default="{ row }">
                <span class="font-mono text-[11px] text-[#1e293b]">{{ row.stock }}</span>
              </template>
            </el-table-column>
            <el-table-column label="已售" width="70">
              <template #default="{ row }">
                <span class="font-mono text-[11px] text-[#64748b]">{{ row.sold }}</span>
              </template>
            </el-table-column>
            <el-table-column label="余位" width="70">
              <template #default="{ row }">
                <span class="font-mono text-[11px]" :class="row.remaining > 0 ? 'text-[#15803d]' : 'text-[#b91c1c]'">
                  {{ row.remaining }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="76">
              <template #default="{ row }">
                <StatusTag :value="row.status" :labels="['停售', '可售']" :tone="row.status === 1 ? 'success' : 'muted'" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="170" fixed="right">
              <template #default="{ row }">
                <el-button v-perm="'catalog:RouteDeparture:update'" link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
                <el-button v-perm="'catalog:RouteDeparture:updateStatus'" link size="small" @click="toggle(row)">
                  {{ row.status === 1 ? '停售' : '恢复' }}
                </el-button>
                <el-button v-perm="'catalog:RouteDeparture:delete'" link type="danger" size="small" @click="remove(row)">删除</el-button>
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

    <el-dialog v-model="dialogOpen" :title="editingId ? '编辑班期' : '新增班期'" width="480px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="small">
        <el-form-item label="线路" prop="routeId">
          <RemotePicker
            v-model="form.routeId"
            :fetcher="fetchRoutes"
            :formatter="(row) => `${row.title}（${row.routeNo}）`"
            :initial-label="editingRouteLabel"
            placeholder="搜索线路标题"
          />
        </el-form-item>
        <div class="grid grid-cols-2 gap-2">
          <el-form-item label="出发日期" prop="departDate">
            <el-date-picker v-model="form.departDate" type="date" value-format="YYYY-MM-DD" class="!w-full" placeholder="选择日期" />
          </el-form-item>
          <el-form-item label="库存" prop="stock">
            <el-input-number v-model="form.stock" :min="0" class="w-full" controls-position="right" />
          </el-form-item>
          <el-form-item label="成人价" prop="adultPrice">
            <el-input-number v-model="form.adultPrice" :min="0" :precision="2" :step="100" class="w-full" controls-position="right" />
          </el-form-item>
          <el-form-item label="儿童价">
            <el-input-number v-model="form.childPrice" :min="0" :precision="2" :step="100" class="w-full" controls-position="right" />
          </el-form-item>
        </div>
        <el-form-item label="状态">
          <el-select v-model="form.status" class="w-full">
            <el-option label="可售" :value="1" />
            <el-option label="停售" :value="0" />
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
