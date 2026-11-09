<script setup>
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { createRole, getRole, updateRole } from '@/api/system'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  mode: { type: String, default: 'create' },
  roleId: { type: [Number, String], default: null },
})
const emit = defineEmits(['update:modelValue', 'saved'])

const formRef = ref()
const loading = ref(false)
const saving = ref(false)

const form = reactive({
  roleName: '',
  roleCode: '',
  sort: 0,
  status: 1,
  remark: '',
  version: 0,
})

const isEdit = computed(() => props.mode === 'edit')

const rules = computed(() => ({
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleCode: isEdit.value
    ? []
    : [
        { required: true, message: '请输入角色编码', trigger: 'blur' },
        {
          pattern: /^[a-z][a-z0-9_]{1,31}$/,
          message: '小写字母开头，2–32 位小写字母、数字或下划线',
          trigger: 'blur',
        },
      ],
}))

function reset() {
  Object.assign(form, {
    roleName: '',
    roleCode: '',
    sort: 0,
    status: 1,
    remark: '',
    version: 0,
  })
  formRef.value?.clearValidate()
}

async function loadDetail() {
  if (!props.roleId) return
  loading.value = true
  try {
    const detail = await getRole(props.roleId)
    Object.assign(form, {
      roleName: detail.roleName,
      roleCode: detail.roleCode,
      sort: detail.sort,
      status: detail.status,
      remark: detail.remark || '',
      version: detail.version,
    })
    await nextTick()
    formRef.value?.clearValidate()
  } catch (error) {
    ElMessage.error(error.message || '角色详情加载失败')
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
      await updateRole(props.roleId, {
        version: form.version,
        roleName: form.roleName,
        sort: form.sort,
        remark: form.remark,
      })
      ElMessage.success('角色已更新')
    } else {
      await createRole({
        roleName: form.roleName,
        roleCode: form.roleCode.trim(),
        sort: form.sort,
        status: form.status,
        remark: form.remark,
      })
      ElMessage.success('角色已创建')
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
    :title="isEdit ? '编辑角色' : '新增角色'"
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
      <el-form-item label="角色名称" prop="roleName">
        <el-input v-model="form.roleName" placeholder="如：运营" />
      </el-form-item>
      <el-form-item label="角色编码" prop="roleCode">
        <el-input
          v-model="form.roleCode"
          :disabled="isEdit"
          placeholder="如：ops"
          class="font-mono"
        />
      </el-form-item>
      <div class="grid grid-cols-2 gap-2">
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :max="999" controls-position="right" class="w-full" />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="状态" prop="status">
          <el-select v-model="form.status" class="w-full">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
      </div>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="200" show-word-limit />
      </el-form-item>
      <p v-if="isEdit" class="text-[10px] leading-4 text-[#94a3b8]">
        角色编码创建后不可修改；启停请使用行内开关。
      </p>
    </el-form>

    <template #footer>
      <div class="flex justify-end gap-1">
        <el-button size="small" @click="emit('update:modelValue', false)">取消</el-button>
        <el-button size="small" type="primary" :loading="saving" @click="submit">
          {{ isEdit ? '保存修改' : '创建角色' }}
        </el-button>
      </div>
    </template>
  </el-drawer>
</template>
