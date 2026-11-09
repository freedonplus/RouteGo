<script setup>
import { ref, watch } from 'vue'
import { assignUserRoles, getUserRoles, pageRoles } from '@/api/system'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  user: { type: Object, default: null },
})
const emit = defineEmits(['update:modelValue', 'saved'])

const loading = ref(false)
const saving = ref(false)
const roles = ref([])
const checked = ref([])

async function load() {
  if (!props.user) return
  loading.value = true
  try {
    const [rolePage, current] = await Promise.all([
      pageRoles({ pageNum: 1, pageSize: 200 }),
      getUserRoles(props.user.id),
    ])
    roles.value = rolePage.records || []
    checked.value = current || []
  } catch (error) {
    ElMessage.error(error.message || '角色加载失败')
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
  if (!checked.value.length) {
    ElMessage.warning('至少选择一个角色')
    return
  }
  saving.value = true
  try {
    await assignUserRoles(props.user.id, checked.value)
    ElMessage.success('角色已更新，该用户已被踢下线')
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
    title="分配角色"
    size="400px"
    append-to-body
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div v-loading="loading">
      <div class="mb-2 flex items-center gap-2 border-b border-[#e2e8f0] pb-2">
        <span class="flex h-6 w-6 items-center justify-center rounded bg-[#fdf1f5] text-[10px] font-semibold text-[#a8435f]">
          {{ (user?.nickname || 'U').slice(0, 1) }}
        </span>
        <div class="min-w-0">
          <p class="truncate text-xs font-medium text-[#1e293b]">{{ user?.nickname }}</p>
          <p class="font-mono text-[10px] text-[#94a3b8]">{{ user?.username }}</p>
        </div>
      </div>

      <el-checkbox-group v-model="checked" class="flex flex-col gap-1">
        <div
          v-for="role in roles"
          :key="role.id"
          class="flex items-center gap-2 rounded border border-[#e2e8f0] px-2.5 py-2 transition-colors duration-150 hover:bg-[#f8fafc]"
        >
          <el-checkbox :value="role.id">
            <span class="text-xs text-[#1e293b]">{{ role.roleName }}</span>
          </el-checkbox>
          <span class="font-mono text-[10px] text-[#94a3b8]">{{ role.roleCode }}</span>
          <span class="ml-auto font-mono text-[10px] text-[#94a3b8]">
            {{ role.userCount }} 人
          </span>
        </div>
      </el-checkbox-group>

      <p class="mt-2 text-[10px] leading-4 text-[#94a3b8]">
        保存后会清空该用户的权限缓存并强制下线，重新登录后生效。
      </p>
    </div>

    <template #footer>
      <div class="flex justify-end gap-1">
        <el-button size="small" @click="emit('update:modelValue', false)">取消</el-button>
        <el-button size="small" type="primary" :loading="saving" @click="submit">
          保存角色
        </el-button>
      </div>
    </template>
  </el-drawer>
</template>
