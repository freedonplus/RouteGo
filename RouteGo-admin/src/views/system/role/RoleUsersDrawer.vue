<script setup>
import { reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { pageRoleUsers } from '@/api/system'
import { formatDate } from '@/utils/format'
import StatusTag from '@/components/StatusTag.vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  role: { type: Object, default: null },
})
const emit = defineEmits(['update:modelValue'])

const loading = ref(false)
const rows = ref([])
const page = reactive({ pageNum: 1, pageSize: 10, total: 0 })

async function load() {
  if (!props.role) return
  loading.value = true
  try {
    const result = await pageRoleUsers(props.role.id, {
      pageNum: page.pageNum,
      pageSize: page.pageSize,
    })
    rows.value = result.records || []
    page.total = result.total || 0
  } catch (error) {
    ElMessage.error(error.message || '角色用户加载失败')
    rows.value = []
    page.total = 0
  } finally {
    loading.value = false
  }
}

watch(
  () => props.modelValue,
  (open) => {
    if (!open) return
    page.pageNum = 1
    load()
  }
)
</script>

<template>
  <el-drawer
    :model-value="modelValue"
    title="角色用户"
    size="480px"
    append-to-body
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div class="mb-2 flex items-center gap-2 border-b border-[#e2e8f0] pb-2">
      <div class="min-w-0">
        <p class="truncate text-xs font-medium text-[#1e293b]">{{ role?.roleName }}</p>
        <p class="font-mono text-[10px] text-[#94a3b8]">{{ role?.roleCode }}</p>
      </div>
      <span class="ml-auto font-mono text-[10px] text-[#64748b]">共 {{ page.total }} 人</span>
    </div>

    <div v-loading="loading" class="rg-scroll-x rounded border border-[#e2e8f0]">
      <el-table :data="rows" size="small" border empty-text="该角色下暂无用户">
        <el-table-column label="ID" width="50">
          <template #default="{ row }">
            <span class="font-mono text-[11px] text-[#64748b]">{{ row.id }}</span>
          </template>
        </el-table-column>
        <el-table-column label="用户" min-width="150">
          <template #default="{ row }">
            <p class="truncate text-[11px] font-medium text-[#1e293b]">{{ row.nickname }}</p>
            <p class="truncate font-mono text-[10px] text-[#94a3b8]">{{ row.username }}</p>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="70">
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
      </el-table>
    </div>

    <div class="mt-2 flex items-center justify-between">
      <span class="font-mono text-[10px] text-[#94a3b8]">只读视图</span>
      <el-pagination
        v-model:current-page="page.pageNum"
        :page-size="page.pageSize"
        :total="page.total"
        layout="prev, pager, next"
        size="small"
        @current-change="load"
      />
    </div>
  </el-drawer>
</template>
