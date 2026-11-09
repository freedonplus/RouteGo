<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { changePassword } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

defineProps({
  modelValue: { type: Boolean, default: false },
})
const emit = defineEmits(['update:modelValue'])

const auth = useAuthStore()
const router = useRouter()
const formRef = ref()
const loading = ref(false)
const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const rules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
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
  form.oldPassword = ''
  form.newPassword = ''
  form.confirmPassword = ''
  formRef.value?.clearValidate()
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await changePassword({ oldPassword: form.oldPassword, newPassword: form.newPassword })
    ElMessage.success('密码已修改，请重新登录')
    emit('update:modelValue', false)
    reset()
    await auth.logout()
    router.replace('/login')
  } catch (error) {
    ElMessage.error(error.message || '修改失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    title="修改密码"
    width="420px"
    append-to-body
    @update:model-value="emit('update:modelValue', $event)"
    @closed="reset"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="small">
      <el-form-item label="旧密码" prop="oldPassword">
        <el-input v-model="form.oldPassword" type="password" show-password autocomplete="off" />
      </el-form-item>
      <el-form-item label="新密码" prop="newPassword">
        <el-input v-model="form.newPassword" type="password" show-password autocomplete="off" />
      </el-form-item>
      <el-form-item label="确认新密码" prop="confirmPassword">
        <el-input
          v-model="form.confirmPassword"
          type="password"
          show-password
          autocomplete="off"
          @keyup.enter="submit"
        />
      </el-form-item>
    </el-form>
    <p class="text-[10px] text-[#64748b]">修改成功后全部会话失效，需要重新登录。</p>
    <template #footer>
      <el-button size="small" @click="emit('update:modelValue', false)">取消</el-button>
      <el-button size="small" type="primary" :loading="loading" @click="submit">确认修改</el-button>
    </template>
  </el-dialog>
</template>
