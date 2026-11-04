<script setup>
import { formatDate } from '@/utils/format'
import StatusTag from '@/components/StatusTag.vue'

defineProps({
  data: { type: Array, default: () => [] },
  showModule: { type: Boolean, default: true },
  loading: { type: Boolean, default: false },
})

const emit = defineEmits(['edit', 'toggle', 'remove'])

function actionLabel(row) {
  return row.status === 1 ? '停用' : '启用'
}
</script>

<template>
  <el-table
    v-loading="loading"
    :data="data"
    size="small"
    border
    stripe
    row-key="id"
    empty-text="没有匹配的权限点"
  >
    <el-table-column label="ID" width="50">
      <template #default="{ row }">
        <span class="font-mono text-[11px] text-[#64748b]">{{ row.id }}</span>
      </template>
    </el-table-column>
    <el-table-column label="权限名称" min-width="140">
      <template #default="{ row }">
        <span class="truncate text-[11px] font-medium text-[#1e293b]">
          {{ row.permissionName }}
        </span>
      </template>
    </el-table-column>
    <el-table-column label="权限码" min-width="200">
      <template #default="{ row }">
        <span class="truncate font-mono text-[11px] text-[#64748b]">
          {{ row.permissionCode }}
        </span>
      </template>
    </el-table-column>
    <el-table-column v-if="showModule" label="模块" width="90">
      <template #default="{ row }">
        <span class="rounded bg-[#f1f5f9] px-1.5 py-0.5 text-[10px] text-[#475569]">
          {{ row.module }}
        </span>
      </template>
    </el-table-column>
    <el-table-column label="说明" min-width="160">
      <template #default="{ row }">
        <span class="truncate text-[11px] text-[#64748b]">{{ row.description || '—' }}</span>
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
    <el-table-column label="操作" width="160" fixed="right">
      <template #default="{ row }">
        <div class="flex items-center gap-0.5">
          <el-button
            v-perm="'system:permission:edit'"
            link
            type="primary"
            size="small"
            @click="emit('edit', row)"
          >
            编辑
          </el-button>
          <el-button
            v-perm="'system:permission:status'"
            link
            size="small"
            @click="emit('toggle', row)"
          >
            {{ actionLabel(row) }}
          </el-button>
          <el-button
            v-perm="'system:permission:delete'"
            link
            type="danger"
            size="small"
            :disabled="row.roleCount > 0"
            :title="row.roleCount > 0 ? `被 ${row.roleCount} 个角色引用` : ''"
            @click="emit('remove', row)"
          >
            删除
          </el-button>
        </div>
      </template>
    </el-table-column>
  </el-table>
</template>
