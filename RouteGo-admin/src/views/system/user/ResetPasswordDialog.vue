<script setup>
import { reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { resetUserPassword } from '@/api/system'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  user: { type: Object, default: null },
})
const emit = defineEmits(['update:modelValue', 'saved'])

const formRef = ref()
const saving = ref(false)
const form = reactive({ newPassword: '', confirmPassword: '' })

const rules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    {
      pattern: /^(?=.*[A-Za-z])(?=.*\d).{8,20}$/,
      message: '8–20 位，至少包含字母和数字',
      trigger: 'blur',
    },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_, value, callback) => {
        if (value !== form.newPassword) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
}

function reset() {
  form.newPassword = ''
  form.confirmPassword = ''
  formRef.value?.clearValidate()
}

watch(
  () => props.modelValue,
  (open) => {
    if (open) reset()
  }
)

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await resetUserPassword(props.user.id, form.newPassword)
    ElMessage.success('密码已重置，该用户已被踢下线')
    emit('saved')
    emit('update:modelValue', false)
  } catch (error) {
    ElMessage.error(error.message || '重置失败')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    title="重置密码"
    width="400px"
    append-to-body
    @update:model-value="emit('update:modelValue', $event)"
  >
    <p class="mb-2 rounded bg-[#f8fafc] px-2.5 py-1.5 text-[11px] text-[#64748b]">
      目标账号：<span class="font-mono text-[#1e293b]">{{ user?.username }}</span>
    </p>
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="small">
      <el-form-item label="新密码" prop="newPassword">
        <el-input v-model="form.newPassword" type="password" show-password autocomplete="new-password" />
      </el-form-item>
      <el-form-item label="确认新密码" prop="confirmPassword">
        <el-input
          v-model="form.confirmPassword"
          type="password"
          show-password
          autocomplete="new-password"
          @keyup.enter="submit"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button size="small" @click="emit('update:modelValue', false)">取消</el-button>
      <el-button size="small" type="primary" :loading="saving" @click="submit">
        确认重置
      </el-button>
    </template>
  </el-dialog>
</template>
