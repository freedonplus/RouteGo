<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { pageCustomers } from '@/api/customer'
import { formatDate } from '@/utils/format'
import StatusTag from '@/components/StatusTag.vue'

/**
 * 会员批量选择器：弹窗里按条件筛人、跨页勾选，再把选中的 ID 交回表单。
 * <p>
 * 之所以不用 el-select + remote：远程多选每输入一次就整批替换候选项，已经选中的会员
 * 标签会退回成裸 ID，也没法「按条件全选」，发券时只能一个个搜人。
 * 这里把筛选、分页、已选集合分开维护，翻页/换条件都不会丢已选。
 */
const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  max: { type: Number, default: 200 },
  disabled: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue', 'change'])

const LEVEL_LABELS = ['普通会员', '银卡会员', '金卡会员', '钻石会员']
/** 后端 PageQuery 允许的最大页大小，也是单次全选筛选结果的上限 */
const MAX_PAGE_SIZE = 200

const open = ref(false)
const loading = ref(false)
const bulkLoading = ref(false)
const rows = ref([])
const page = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const timeRange = ref([])
const filters = reactive({ keyword: '', level: '', status: 1 })
/** 已选会员：id -> 行快照，跨页、跨筛选条件都保留 */
const selected = reactive(new Map())

const selectedIds = computed(() => Array.from(selected.keys()))
const selectedRows = computed(() => Array.from(selected.values()))
const selectedCount = computed(() => selected.size)
const summary = computed(() => {
  const list = selectedRows.value
  if (!list.length) return ''
  const head = list
    .slice(0, 3)
    .map((row) => row.nickname || row.phone || `#${row.id}`)
    .join('、')
  return list.length > 3 ? `${head} 等 ${list.length} 人` : head
})

const pageAllChecked = computed(
  () => rows.value.length > 0 && rows.value.every((row) => selected.has(row.id))
)
const pageIndeterminate = computed(
  () => !pageAllChecked.value && rows.value.some((row) => selected.has(row.id))
)

function buildParams(overrides = {}) {
  return {
    keyword: filters.keyword.trim() || undefined,
    level: filters.level === '' ? undefined : filters.level,
    status: filters.status === '' ? undefined : filters.status,
    registerTimeStart: timeRange.value?.[0] ? `${timeRange.value[0]} 00:00:00` : undefined,
    registerTimeEnd: timeRange.value?.[1] ? `${timeRange.value[1]} 23:59:59` : undefined,
    pageNum: page.pageNum,
    pageSize: page.pageSize,
    ...overrides,
  }
}

async function load() {
  loading.value = true
  try {
    const result = await pageCustomers(buildParams())
    rows.value = result.records || []
    page.total = Number(result.total || 0)
  } catch (error) {
    ElMessage.error(error.message || '会员加载失败')
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
  Object.assign(filters, { keyword: '', level: '', status: 1 })
  timeRange.value = []
  page.pageNum = 1
  load()
}

function sync() {
  emit('update:modelValue', selectedIds.value)
  emit('change', selectedRows.value)
}

function isSelected(id) {
  return selected.has(id)
}

function addRows(list) {
  const room = props.max - selected.size
  if (room <= 0) {
    ElMessage.warning(`单次最多选择 ${props.max} 人`)
    return 0
  }
  let added = 0
  for (const row of list) {
    if (added >= room) break
    if (selected.has(row.id)) continue
    selected.set(row.id, row)
    added++
  }
  return added
}

function toggleRow(row, checked) {
  if (checked) {
    if (row.status !== 1) {
      ElMessage.warning('该会员已禁用，不能发券')
      return
    }
    if (selected.size >= props.max) {
      ElMessage.warning(`单次最多选择 ${props.max} 人`)
      return
    }
    selected.set(row.id, row)
  } else {
    selected.delete(row.id)
  }
  sync()
}

function togglePage(checked) {
  if (checked) {
    const available = rows.value.filter((row) => row.status === 1)
    const added = addRows(available)
    if (added > 0) ElMessage.success(`已加入 ${added} 位会员`)
  } else {
    rows.value.forEach((row) => selected.delete(row.id))
  }
  sync()
}

/** 把当前筛选结果（最多 max 人）一次性加入，适合「给某等级/某时间段会员统一发券」 */
async function selectAllFiltered() {
  bulkLoading.value = true
  try {
    const result = await pageCustomers(buildParams({ pageNum: 1, pageSize: MAX_PAGE_SIZE }))
    const candidates = (result.records || []).filter((row) => row.status === 1)
    const total = Number(result.total || 0)
    const added = addRows(candidates)
    if (added > 0) {
      const rest = total - candidates.length
      if (rest > 0) {
        ElMessage.warning(`筛选结果共 ${total} 人，本次按上限加入 ${added} 人，请缩小筛选范围再补选`)
      } else {
        ElMessage.success(`已加入 ${added} 位会员`)
      }
    }
    sync()
  } catch (error) {
    ElMessage.error(error.message || '批量加入失败')
  } finally {
    bulkLoading.value = false
  }
}

function removeSelected(id) {
  selected.delete(id)
  sync()
}

function clearSelected() {
  selected.clear()
  sync()
}

function openPicker() {
  if (props.disabled) return
  open.value = true
  page.pageNum = 1
  load()
}

function confirm() {
  if (!selected.size) {
    ElMessage.warning('请至少选择一位会员')
    return
  }
  open.value = false
}

// 父级点「取消」或重新打开表单时清空列表，选择集合跟着一起清
watch(
  () => props.modelValue,
  (value) => {
    if ((!value || value.length === 0) && selected.size > 0) {
      selected.clear()
    }
  }
)

defineExpose({ open: openPicker, clear: clearSelected })
</script>

<template>
  <div>
    <div
      class="flex min-h-[34px] flex-wrap items-center gap-1.5 rounded border border-[#e2e8f0] bg-white px-2 py-1"
      :class="disabled ? 'opacity-60' : 'cursor-pointer hover:border-[#c7d2fe]'"
      @click="openPicker"
    >
      <template v-if="selectedCount">
        <el-tag
          v-for="row in selectedRows.slice(0, 3)"
          :key="row.id"
          size="small"
          closable
          type="info"
          @close.stop="removeSelected(row.id)"
        >
          {{ row.nickname || row.phone || `#${row.id}` }}
        </el-tag>
        <span v-if="selectedCount > 3" class="text-[11px] text-[#64748b]">等 {{ selectedCount }} 人</span>
      </template>
      <span v-else class="text-[12px] text-[#94a3b8]">未选择会员</span>
      <span class="ml-auto shrink-0 text-[11px] text-[#2563eb]">
        {{ selectedCount ? `已选 ${selectedCount} 人 · 修改` : '选择会员' }}
      </span>
    </div>

    <el-dialog v-model="open" title="选择会员" width="860px" append-to-body>
      <div class="flex flex-wrap items-end gap-2">
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">关键词</span>
          <el-input
            v-model="filters.keyword"
            size="small"
            placeholder="昵称/姓名/完整手机号"
            clearable
            style="width: 190px"
            @keyup.enter="search"
          />
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">等级</span>
          <el-select v-model="filters.level" size="small" style="width: 120px">
            <el-option label="全部" value="" />
            <el-option v-for="(label, index) in LEVEL_LABELS" :key="index" :label="label" :value="index + 1" />
          </el-select>
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">状态</span>
          <el-select v-model="filters.status" size="small" style="width: 96px">
            <el-option label="全部" value="" />
            <el-option label="正常" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">注册时间</span>
          <el-date-picker
            v-model="timeRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始"
            end-placeholder="结束"
            size="small"
            style="width: 224px"
          />
        </label>
        <el-button size="small" type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button size="small" @click="resetFilters">重置</el-button>
      </div>

      <div class="mt-2 flex flex-wrap items-center gap-2 rounded bg-[#f8fafc] px-2 py-1.5">
        <span class="text-[11px] text-[#475569]">
          已选 <span class="font-mono text-[#a8435f]">{{ selectedCount }}</span> / {{ max }} 人
        </span>
        <el-button size="small" :loading="bulkLoading" @click="selectAllFiltered">
          全选筛选结果（最多 {{ max }} 人）
        </el-button>
        <el-button size="small" :disabled="!selectedCount" @click="clearSelected">清空已选</el-button>
        <span class="ml-auto truncate text-[10px] text-[#94a3b8]" :title="summary">{{ summary }}</span>
      </div>

      <div class="mt-2 rg-scroll-x">
        <el-table
          v-loading="loading"
          :data="rows"
          size="small"
          border
          stripe
          row-key="id"
          max-height="360"
          empty-text="没有匹配的会员"
        >
          <el-table-column width="44" align="center">
            <template #header>
              <el-checkbox
                :model-value="pageAllChecked"
                :indeterminate="pageIndeterminate"
                @change="togglePage"
              />
            </template>
            <template #default="{ row }">
              <el-checkbox
                :model-value="isSelected(row.id)"
                :disabled="row.status !== 1"
                @change="(checked) => toggleRow(row, checked)"
              />
            </template>
          </el-table-column>
          <el-table-column label="会员" min-width="160">
            <template #default="{ row }">
              <p class="truncate text-[12px] text-[#1e293b]">{{ row.nickname || '未命名' }}</p>
              <p class="font-mono text-[10px] text-[#94a3b8]">{{ row.phone || '—' }}</p>
            </template>
          </el-table-column>
          <el-table-column label="等级" width="96">
            <template #default="{ row }">
              <span class="text-[11px] text-[#475569]">{{ LEVEL_LABELS[(row.level || 1) - 1] }}</span>
            </template>
          </el-table-column>
          <el-table-column label="积分" width="80">
            <template #default="{ row }">
              <span class="font-mono text-[11px] text-[#64748b]">{{ row.points ?? 0 }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="76">
            <template #default="{ row }">
              <StatusTag :value="row.status" />
            </template>
          </el-table-column>
          <el-table-column label="注册时间" width="110">
            <template #default="{ row }">
              <span class="font-mono text-[11px] text-[#64748b]">{{ formatDate(row.registerTime) }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="mt-2 flex flex-wrap items-center justify-between gap-2">
        <span class="font-mono text-[10px] text-[#94a3b8]">共 {{ page.total }} 位会员</span>
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

      <template #footer>
        <div class="flex items-center justify-end gap-1">
          <span class="mr-auto text-[10px] text-[#94a3b8]">已禁用会员不可选，翻页或换条件不会丢失已选</span>
          <el-button size="small" @click="open = false">取消</el-button>
          <el-button size="small" type="primary" @click="confirm">确定（{{ selectedCount }} 人）</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
