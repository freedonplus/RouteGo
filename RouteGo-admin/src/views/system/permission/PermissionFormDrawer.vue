<script setup>
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { createPermission, getPermission, updatePermission } from '@/api/system'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  mode: { type: String, default: 'create' },
  permissionId: { type: [Number, String], default: null },
})
const emit = defineEmits(['update:modelValue', 'saved'])

const formRef = ref()
const loading = ref(false)
const saving = ref(false)

const form = reactive({
  permissionName: '',
  permissionCode: '',
  module: 'system',
  description: '',
  status: 1,
  version: 0,
})

const isEdit = computed(() => props.mode === 'edit')

const rules = computed(() => ({
  permissionName: [{ required: true, message: '请输入权限名称', trigger: 'blur' }],
  permissionCode: [
        { required: true, message: '请输入权限码', trigger: 'blur' },
        {
          // 与后端保持一致：允许大小写、下划线与短横线（历史数据形如 admin:SysUser:listByUser）
          pattern: /^[A-Za-z][A-Za-z0-9_-]*(?::[A-Za-z0-9_-]+)+$/,
          message: '格式如 system:user:list',
          trigger: 'blur',
        },
      ],
  module: [{ required: true, message: '请输入所属模块', trigger: 'blur' }],
}))

function reset() {
  Object.assign(form, {
    permissionName: '',
    permissionCode: '',
    module: 'system',
    description: '',
    status: 1,
    version: 0,
  })
  formRef.value?.clearValidate()
}

async function loadDetail() {
  if (!props.permissionId) return
  loading.value = true
  try {
    const detail = await getPermission(props.permissionId)
    Object.assign(form, {
      permissionName: detail.permissionName,
      permissionCode: detail.permissionCode,
      module: detail.module,
      description: detail.description || '',
      status: detail.status,
      version: detail.version,
    })
    await nextTick()
    formRef.value?.clearValidate()
  } catch (error) {
    ElMessage.error(error.message || '权限点加载失败')
  } finally {
    loading.value = false
  }
}

watch(
  () => props.modelValue,
  async (open) => {
    if (!open) return
    reset()
    if (isEdit.value) await loadDetail()
  }
)

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (isEdit.value) {
      await updatePermission(props.permissionId, {
        version: form.version,
        permissionName: form.permissionName,
        permissionCode: form.permissionCode.trim(),
        module: form.module,
        description: form.description,
        status: form.status,
      })
      ElMessage.success('权限点已更新')
    } else {
      await createPermission({
        permissionName: form.permissionName,
        permissionCode: form.permissionCode.trim(),
        module: form.module,
        description: form.description,
        status: form.status,
      })
      ElMessage.success('权限点已创建')
    }
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
    :title="isEdit ? '编辑权限点' : '新增权限点'"
    size="400px"
    append-to-body
    :close-on-click-modal="false"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-form
      ref="formRef"
      v-loading="loading"
      :model="form"
      :rules="rules"
      label-position="top"
      size="small"
    >
      <el-form-item label="权限名称" prop="permissionName">
        <el-input v-model="form.permissionName" placeholder="如：查看用户" />
      </el-form-item>
      <el-form-item label="权限码" prop="permissionCode">
        <el-input
          v-model="form.permissionCode"
          placeholder="system:user:list"
          class="font-mono"
        />
      </el-form-item>
      <div class="grid grid-cols-2 gap-2">
        <el-form-item label="所属模块" prop="module">
          <el-select
            v-model="form.module"
            filterable
            allow-create
            default-first-option
            class="w-full"
          >
            <el-option label="system" value="system" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" class="w-full">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
      </div>
      <el-form-item label="说明" prop="description">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="2"
          maxlength="200"
          show-word-limit
          placeholder="这个权限允许什么操作"
        />
      </el-form-item>
      <p class="text-[10px] leading-4 text-[#94a3b8]">
        权限码可修改，保存后立即对所有已登录用户生效；被角色引用时不能删除。
      </p>
    </el-form>

    <template #footer>
      <div class="flex justify-end gap-1">
        <el-button size="small" @click="emit('update:modelValue', false)">取消</el-button>
        <el-button size="small" type="primary" :loading="saving" @click="submit">
          {{ isEdit ? '保存修改' : '创建权限点' }}
        </el-button>
      </div>
    </template>
  </el-drawer>
</template>
