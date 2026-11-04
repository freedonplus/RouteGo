<script setup>
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { createUser, getUser, pageRoles, updateUser } from '@/api/system'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  mode: { type: String, default: 'create' },
  userId: { type: [Number, String], default: null },
})
const emit = defineEmits(['update:modelValue', 'saved'])

const formRef = ref()
const loading = ref(false)
const saving = ref(false)
const roles = ref([])

const form = reactive({
  username: '',
  password: '',
  nickname: '',
  avatar: '',
  phone: '',
  email: '',
  remark: '',
  roleIds: [],
  version: 0,
})

const isEdit = computed(() => props.mode === 'edit')

const rules = computed(() => ({
  username: isEdit.value
    ? []
    : [
        { required: true, message: '请输入账号', trigger: 'blur' },
        {
          pattern: /^[A-Za-z0-9_]{3,50}$/,
          message: '3–50 位字母、数字或下划线',
          trigger: 'blur',
        },
      ],
  password: isEdit.value
    ? []
    : [
        { required: true, message: '请输入密码', trigger: 'blur' },
        {
          pattern: /^(?=.*[A-Za-z])(?=.*\d).{8,20}$/,
          message: '8–20 位，至少包含字母和数字',
          trigger: 'blur',
        },
      ],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  roleIds: isEdit.value
    ? []
    : [
        {
          type: 'array',
          required: true,
          min: 1,
          message: '至少选择一个角色',
          trigger: 'change',
        },
      ],
}))

function reset() {
  Object.assign(form, {
    username: '',
    password: '',
    nickname: '',
    avatar: '',
    phone: '',
    email: '',
    remark: '',
    roleIds: [],
    version: 0,
  })
  formRef.value?.clearValidate()
}

async function loadRoles() {
  try {
    const result = await pageRoles({ pageNum: 1, pageSize: 200 })
    roles.value = result.records || []
  } catch {
    roles.value = []
  }
}

async function loadDetail() {
  if (!props.userId) return
  loading.value = true
  try {
    const detail = await getUser(props.userId)
    Object.assign(form, {
      username: detail.username,
      nickname: detail.nickname,
      avatar: detail.avatar || '',
      phone: detail.phone || '',
      email: detail.email || '',
      remark: detail.remark || '',
      roleIds: detail.roleIds || [],
      version: detail.version,
    })
    await nextTick()
    formRef.value?.clearValidate()
  } catch (error) {
    ElMessage.error(error.message || '用户详情加载失败')
  } finally {
    loading.value = false
  }
}

watch(
  () => props.modelValue,
  async (open) => {
    if (!open) return
    reset()
    await loadRoles()
    if (isEdit.value) await loadDetail()
  }
)

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    if (isEdit.value) {
      await updateUser(props.userId, {
        version: form.version,
        nickname: form.nickname,
        avatar: form.avatar,
        phone: form.phone,
        email: form.email,
        remark: form.remark,
      })
      ElMessage.success('用户资料已更新')
    } else {
      await createUser({
        username: form.username.trim(),
        password: form.password,
        nickname: form.nickname,
        avatar: form.avatar,
        phone: form.phone,
        email: form.email,
        remark: form.remark,
        roleIds: form.roleIds,
      })
      ElMessage.success('用户已创建')
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
    :title="isEdit ? '编辑用户' : '新增用户'"
    size="440px"
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
      <div :class="isEdit ? '' : 'grid grid-cols-2 gap-2'">
        <el-form-item label="账号" prop="username">
          <el-input
            v-model="form.username"
            :disabled="isEdit"
            placeholder="字母/数字/下划线"
            class="font-mono"
          />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="初始密码" prop="password">
          <el-input v-model="form.password" type="password" show-password autocomplete="new-password" />
        </el-form-item>
      </div>

      <el-form-item label="昵称" prop="nickname">
        <el-input v-model="form.nickname" placeholder="显示名称" />
      </el-form-item>

      <el-form-item label="头像 URL" prop="avatar">
        <el-input v-model="form.avatar" placeholder="https://..." class="font-mono" />
      </el-form-item>

      <div class="grid grid-cols-2 gap-2">
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="13812348000" class="font-mono" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="ops@example.com" class="font-mono" />
        </el-form-item>
      </div>

      <el-form-item v-if="!isEdit" label="角色" prop="roleIds">
        <el-select v-model="form.roleIds" multiple placeholder="至少选择一个角色" class="w-full">
          <el-option
            v-for="role in roles"
            :key="role.id"
            :label="role.roleName"
            :value="role.id"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="备注" prop="remark">
        <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="200" show-word-limit />
      </el-form-item>

      <p v-if="isEdit" class="text-[10px] leading-4 text-[#94a3b8]">
        手机号 / 邮箱带 * 的掩码值表示不修改；角色请使用行内“角色”操作单独保存。
      </p>
    </el-form>

    <template #footer>
      <div class="flex justify-end gap-1">
        <el-button size="small" @click="emit('update:modelValue', false)">取消</el-button>
        <el-button size="small" type="primary" :loading="saving" @click="submit">
          {{ isEdit ? '保存修改' : '创建用户' }}
        </el-button>
      </div>
    </template>
  </el-drawer>
</template>
