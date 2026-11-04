<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { assignRolePermissions, getGroupedPermissions, getRolePermissions } from '@/api/system'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  role: { type: Object, default: null },
})
const emit = defineEmits(['update:modelValue', 'saved'])

const loading = ref(false)
const saving = ref(false)
const groups = ref([])
const checked = ref([])

function groupIds(group) {
  return group.permissions.map((item) => item.id)
}

function allChecked(group) {
  const ids = groupIds(group)
  return ids.length > 0 && ids.every((id) => checked.value.includes(id))
}

function someChecked(group) {
  const ids = groupIds(group)
  const count = ids.filter((id) => checked.value.includes(id)).length
  return count > 0 && count < ids.length
}

function toggleGroup(group, value) {
  const ids = groupIds(group)
  if (value) checked.value = [...new Set([...checked.value, ...ids])]
  else checked.value = checked.value.filter((id) => !ids.includes(id))
}

async function load() {
  if (!props.role) return
  loading.value = true
  try {
    const [grouped, current] = await Promise.all([
      getGroupedPermissions(),
      getRolePermissions(props.role.id),
    ])
    groups.value = grouped || []
    checked.value = current || []
  } catch (error) {
    ElMessage.error(error.message || '权限点加载失败')
  } finally {
    loading.value = false
  }
}

watch(
  () => props.modelValue,
  (open) => {
    if (open) load()
  }
)

async function submit() {
  saving.value = true
  try {
    await assignRolePermissions(props.role.id, checked.value)
    ElMessage.success('权限点已保存')
    emit('saved')
    emit('update:modelValue', false)
  } catch (error) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <el-drawer
    :model-value="modelValue"
    title="数据权限"
    size="560px"
    append-to-body
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div v-loading="loading">
      <div class="mb-2 flex items-center gap-2 border-b border-[#e2e8f0] pb-2">
        <div class="min-w-0">
          <p class="truncate text-xs font-medium text-[#1e293b]">{{ role?.roleName }}</p>
          <p class="font-mono text-[10px] text-[#94a3b8]">{{ role?.roleCode }}</p>
        </div>
        <span class="ml-auto font-mono text-[10px] text-[#64748b]">
          已选 {{ checked.length }} 项
        </span>
      </div>

      <div class="max-h-[calc(100vh-220px)] space-y-2 overflow-y-auto">
        <section
          v-for="group in groups"
          :key="group.module"
          class="rounded border border-[#e2e8f0]"
        >
          <div
            class="flex flex-wrap items-center gap-2 border-b border-[#e2e8f0] bg-[#f8fafc] px-2.5 py-1.5"
          >
            <span class="rg-section-title">{{ group.module }}</span>
            <span class="font-mono text-[10px] text-[#94a3b8]">
              {{ group.permissions.length }} 项
            </span>
            <el-checkbox
              class="ml-auto"
              :model-value="allChecked(group)"
              :indeterminate="someChecked(group)"
              @change="toggleGroup(group, $event)"
            >
              全选
            </el-checkbox>
          </div>
          <el-checkbox-group v-model="checked" class="grid grid-cols-1 gap-1 px-2.5 py-2 sm:grid-cols-2">
            <el-checkbox
              v-for="permission in group.permissions"
              :key="permission.id"
              :value="permission.id"
            >
              <span class="text-xs text-[#1e293b]">{{ permission.permissionName }}</span>
              <span class="ml-1 font-mono text-[10px] text-[#94a3b8]">
                {{ permission.permissionCode }}
              </span>
            </el-checkbox>
          </el-checkbox-group>
        </section>
      </div>

      <p class="mt-2 text-[10px] leading-4 text-[#94a3b8]">
        只能授予当前操作人已持有的权限点；保存后角色下的用户需要重新登录生效。
      </p>
    </div>

    <template #footer>
      <div class="flex justify-end gap-1">
        <el-button size="small" @click="emit('update:modelValue', false)">取消</el-button>
        <el-button size="small" type="primary" :loading="saving" @click="submit">
          保存权限点
        </el-button>
      </div>
    </template>
  </el-drawer>
</template>
