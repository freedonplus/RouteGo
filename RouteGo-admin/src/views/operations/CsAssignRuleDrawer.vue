<script setup>
import { reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Plus, Refresh } from '@element-plus/icons-vue'
import {
  createAssignRule,
  deleteAssignRule,
  getAssignWorkload,
  listTicketHandlers,
  pageAssignRules,
  previewAssignRule,
  setAssignRuleStatus,
  updateAssignRule,
} from '@/api/operations'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
})
const emit = defineEmits(['update:modelValue'])

const TYPE_OPTIONS = [
  { value: 1, label: '售前咨询' },
  { value: 2, label: '订单问题' },
  { value: 3, label: '退款售后' },
  { value: 4, label: '投诉建议' },
  { value: 5, label: '其他' },
]
const PRIORITY_OPTIONS = [
  { value: 1, label: '低' },
  { value: 2, label: '中' },
  { value: 3, label: '高' },
]
const STRATEGY_OPTIONS = [
  { value: 2, label: '在处理最少', hint: '优先分给当前处理中工单最少的人' },
  { value: 1, label: '轮询', hint: '按候选顺序依次轮流分配' },
]

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const page = reactive({ pageNum: 1, pageSize: 10 })
const keyword = ref('')
const enabledFilter = ref('')

/* 表单 */
const formOpen = ref(false)
const saving = ref(false)
const formRef = ref(null)
const editingId = ref(null)
const form = reactive({
  ruleName: '',
  matchType: null,
  matchPriority: null,
  strategy: 2,
  userIds: [],
  enabled: 1,
  sort: 100,
  remark: '',
})
const rules = {
  ruleName: [{ required: true, message: '请填写规则名称', trigger: 'blur' }],
  userIds: [{ required: true, type: 'array', min: 1, message: '至少选择一个客服', trigger: 'change' }],
}

/* 客服下拉：远程搜索 + 已选回填 */
const userOptions = ref([])
const userSearching = ref(false)
const workload = ref([])

/* 试算 */
const preview = reactive({ type: 1, priority: 2, loading: false, result: null })

const strategyText = (value) => STRATEGY_OPTIONS.find((item) => item.value === Number(value))?.label || '—'
const typeText = (value) => (value === null || value === undefined ? '不限' : TYPE_OPTIONS.find((i) => i.value === Number(value))?.label || '—')
const priorityText = (value) => (value === null || value === undefined ? '不限' : PRIORITY_OPTIONS.find((i) => i.value === Number(value))?.label || '—')

async function searchUsers(text) {
  userSearching.value = true
  try {
    const list = (await listTicketHandlers(text)) || []
    const options = list.map((user) => ({
      id: user.handlerId,
      label: `${user.handlerName || user.username} · 处理中 ${user.processing ?? 0} 单`,
    }))
    // 已选中的客服不在搜索结果里时补回去，否则下拉会显示成一串 ID
    const selected = userOptions.value.filter((item) => form.userIds.includes(item.id))
    const merged = [...options]
    selected.forEach((item) => {
      if (!merged.some((option) => option.id === item.id)) merged.push(item)
    })
    userOptions.value = merged
  } catch {
    // 搜索失败不清空已有选项，避免编辑中的表单突然空掉
  } finally {
    userSearching.value = false
  }
}

async function load() {
  loading.value = true
  try {
    const result = await pageAssignRules({
      keyword: keyword.value || undefined,
      enabled: enabledFilter.value === '' ? undefined : enabledFilter.value,
      pageNum: page.pageNum,
      pageSize: page.pageSize,
    })
    rows.value = result.records || []
    total.value = Number(result.total || 0)
  } catch (error) {
    ElMessage.error(error.message || '规则加载失败')
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

watch(
  () => props.modelValue,
  (open) => {
    if (open) {
      page.pageNum = 1
      load()
      searchUsers('')
      preview.result = null
    }
  }
)

function openCreate() {
  editingId.value = null
  Object.assign(form, {
    ruleName: '',
    matchType: null,
    matchPriority: null,
    strategy: 2,
    userIds: [],
    enabled: 1,
    sort: 100,
    remark: '',
  })
  workload.value = []
  formOpen.value = true
  searchUsers('')
}

function openEdit(row) {
  editingId.value = row.id
  Object.assign(form, {
    ruleName: row.ruleName,
    matchType: row.matchType ?? null,
    matchPriority: row.matchPriority ?? null,
    strategy: Number(row.strategy || 2),
    userIds: [...(row.userIds || [])],
    enabled: Number(row.enabled ?? 1),
    sort: row.sort ?? 100,
    remark: row.remark || '',
  })
  // 已选客服先塞进选项，昵称直接从列表回填
  userOptions.value = (row.userIds || []).map((id, index) => ({
    id,
    label: row.userNames?.[index] || `用户 ${id}`,
  }))
  formOpen.value = true
  searchUsers('')
  loadWorkload(row.userIds)
}

async function loadWorkload(userIds) {
  if (!userIds || !userIds.length) {
    workload.value = []
    return
  }
  try {
    workload.value = await getAssignWorkload(userIds)
  } catch {
    workload.value = []
  }
}

watch(
  () => [...form.userIds],
  (ids) => loadWorkload(ids),
  { deep: true }
)

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload = {
      ruleName: form.ruleName.trim(),
      matchType: form.matchType,
      matchPriority: form.matchPriority,
      strategy: form.strategy,
      userIds: form.userIds,
      enabled: form.enabled,
      sort: form.sort,
      remark: form.remark || undefined,
    }
    if (editingId.value) {
      await updateAssignRule(editingId.value, payload)
      ElMessage.success('规则已更新')
    } else {
      await createAssignRule(payload)
      ElMessage.success('规则已创建')
    }
    formOpen.value = false
    load()
  } catch (error) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function toggle(row) {
  const next = Number(row.enabled) === 1 ? 0 : 1
  try {
    await setAssignRuleStatus(row.id, next)
    row.enabled = next
    ElMessage.success(next === 1 ? '规则已启用' : '规则已停用')
    load()
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除规则「${row.ruleName}」吗？之后的新工单不再命中它，已分配的工单不受影响。`,
      '删除规则',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await deleteAssignRule(row.id)
    ElMessage.success('规则已删除')
    load()
  } catch (error) {
    ElMessage.error(error.message || '删除失败')
  }
}

async function runPreview() {
  preview.loading = true
  try {
    preview.result = await previewAssignRule(preview.type, preview.priority)
  } catch (error) {
    ElMessage.error(error.message || '试算失败')
    preview.result = null
  } finally {
    preview.loading = false
  }
}
</script>

<template>
  <el-drawer
    :model-value="modelValue"
    title="工单自动分配规则"
    size="820px"
    append-to-body
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div class="flex h-full flex-col">
      <div class="mb-2 rounded bg-[#f8fafc] px-2 py-1.5 text-[11px] leading-5 text-[#475569]">
        新工单没有手动指派处理人时，按「排序」从小到大找第一条命中的启用规则：
        类型/优先级填「不限」即不参与筛选。候选客服里已停用或已删除的账号会被自动跳过。
      </div>

      <div class="mb-2 flex flex-wrap items-center gap-1">
        <el-input
          v-model="keyword"
          size="small"
          placeholder="规则名称"
          clearable
          style="width: 160px"
          @keyup.enter="page.pageNum = 1; load()"
        />
        <el-select v-model="enabledFilter" size="small" style="width: 100px">
          <el-option label="全部状态" value="" />
          <el-option label="启用" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
        <el-button size="small" type="primary" @click="page.pageNum = 1; load()">查询</el-button>
        <el-button size="small" :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
        <el-button
          v-perm="'operations:CsAssignRule:create'"
          class="ml-auto"
          size="small"
          type="primary"
          :icon="Plus"
          @click="openCreate"
        >
          新建规则
        </el-button>
      </div>

      <div class="min-h-0 flex-1 overflow-y-auto">
        <el-table v-loading="loading" :data="rows" size="small" border empty-text="还没有配置分配规则">
          <el-table-column label="规则" min-width="150">
            <template #default="{ row }">
              <p class="text-[11px] font-medium text-[#1e293b]">{{ row.ruleName }}</p>
              <p class="text-[10px] text-[#94a3b8]">
                {{ typeText(row.matchType) }} · {{ priorityText(row.matchPriority) }}优先级
              </p>
            </template>
          </el-table-column>
          <el-table-column label="策略" width="90">
            <template #default="{ row }">
              <span class="text-[11px] text-[#475569]">{{ strategyText(row.strategy) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="候选客服" min-width="180">
            <template #default="{ row }">
              <div class="flex flex-wrap gap-0.5">
                <span
                  v-for="(name, index) in row.userNames"
                  :key="index"
                  class="rounded bg-[#eef6ff] px-1 py-0.5 text-[10px] text-[#1d4ed8]"
                >
                  {{ name }}
                </span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="排序" width="64">
            <template #default="{ row }">
              <span class="font-mono text-[11px] text-[#64748b]">{{ row.sort }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="72">
            <template #default="{ row }">
              <el-switch
                v-perm="'operations:CsAssignRule:updateStatus'"
                :model-value="Number(row.enabled) === 1"
                size="small"
                @change="toggle(row)"
              />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="108" fixed="right">
            <template #default="{ row }">
              <el-button v-perm="'operations:CsAssignRule:update'" link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
              <el-button v-perm="'operations:CsAssignRule:delete'" link type="danger" size="small" :icon="Delete" @click="remove(row)" />
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="mt-2 flex items-center justify-between border-t border-[#e2e8f0] pt-2">
        <span class="font-mono text-[10px] text-[#94a3b8]">共 {{ total }} 条</span>
        <el-pagination
          v-model:current-page="page.pageNum"
          v-model:page-size="page.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="sizes, prev, pager, next"
          size="small"
          @current-change="load"
          @size-change="load"
        />
      </div>

      <div class="mt-2 rounded border border-dashed border-[#cbd5e1] px-2 py-1.5">
        <div class="flex flex-wrap items-center gap-1">
          <span class="text-[11px] font-medium text-[#475569]">分配试算</span>
          <el-select v-model="preview.type" size="small" style="width: 104px">
            <el-option v-for="item in TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-select v-model="preview.priority" size="small" style="width: 88px">
            <el-option v-for="item in PRIORITY_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-button size="small" :loading="preview.loading" @click="runPreview">试算</el-button>
          <span v-if="preview.result" class="text-[11px] text-[#475569]">
            <template v-if="preview.result.handlerName">
              命中「{{ preview.result.ruleName }}」（{{ preview.result.strategyDesc }}）→ 分给
              <span class="font-medium text-[#1d4ed8]">{{ preview.result.handlerName }}</span>
            </template>
            <template v-else>{{ preview.result.message }}</template>
          </span>
        </div>
        <p class="mt-1 text-[10px] text-[#94a3b8]">
          轮询策略下试算会真实推进一次游标，所以连续试算会依次给出不同的人，属正常现象。
        </p>
      </div>
    </div>

    <el-dialog
      v-model="formOpen"
      :title="editingId ? '编辑分配规则' : '新建分配规则'"
      width="560px"
      append-to-body
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="small">
        <el-form-item label="规则名称" prop="ruleName">
          <el-input v-model="form.ruleName" maxlength="50" show-word-limit placeholder="如：售前咨询-售前组轮询" />
        </el-form-item>
        <div class="grid grid-cols-2 gap-2">
          <el-form-item label="适用工单类型">
            <el-select v-model="form.matchType" clearable placeholder="不限" class="w-full">
              <el-option v-for="item in TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="适用优先级">
            <el-select v-model="form.matchPriority" clearable placeholder="不限" class="w-full">
              <el-option v-for="item in PRIORITY_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="分配策略">
          <el-radio-group v-model="form.strategy">
            <el-radio v-for="item in STRATEGY_OPTIONS" :key="item.value" :value="item.value">
              {{ item.label }}
            </el-radio>
          </el-radio-group>
          <p class="mt-0.5 text-[10px] text-[#94a3b8]">
            {{ STRATEGY_OPTIONS.find((item) => item.value === form.strategy)?.hint }}
          </p>
        </el-form-item>
        <el-form-item label="候选客服" prop="userIds">
          <el-select
            v-model="form.userIds"
            multiple
            filterable
            remote
            reserve-keyword
            :remote-method="searchUsers"
            :loading="userSearching"
            placeholder="搜索昵称或账号，可多选"
            class="w-full"
          >
            <el-option v-for="item in userOptions" :key="item.id" :label="item.label" :value="item.id" />
          </el-select>
        </el-form-item>
        <div v-if="workload.length" class="mb-2 flex flex-wrap gap-1">
          <span
            v-for="item in workload"
            :key="item.handlerId"
            class="rounded px-1.5 py-0.5 text-[10px]"
            :class="item.available ? 'bg-[#f1f5f9] text-[#475569]' : 'bg-[#fef2f2] text-[#b91c1c]'"
          >
            {{ item.handlerName }}：{{ item.available ? `在处理 ${item.processing} 单` : '已停用' }}
          </span>
        </div>
        <div class="grid grid-cols-2 gap-2">
          <el-form-item label="排序（数字小的先命中）">
            <el-input-number v-model="form.sort" :min="0" :max="9999" controls-position="right" class="w-full" />
          </el-form-item>
          <el-form-item label="状态">
            <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
          </el-form-item>
        </div>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="255" placeholder="选填，例如：该组客服 9:00-18:00 在线" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="flex justify-end gap-1">
          <el-button size="small" @click="formOpen = false">取消</el-button>
          <el-button size="small" type="primary" :loading="saving" @click="submit">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </el-drawer>
</template>
