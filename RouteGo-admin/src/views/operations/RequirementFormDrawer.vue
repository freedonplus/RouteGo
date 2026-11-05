<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  createRequirement,
  getRequirement,
  updateRequirement,
} from '@/api/operations'
import { pageCustomers } from '@/api/customer'
import { pageUsers } from '@/api/system'
import RemotePicker from '@/components/RemotePicker.vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  mode: { type: String, default: 'create' },
  requirementId: { type: [Number, String], default: null },
})
const emit = defineEmits(['update:modelValue', 'saved'])

const formRef = ref()
const saving = ref(false)
const isEdit = computed(() => props.mode === 'edit')
const customerLabel = ref('')

const emptyForm = () => ({
  customerId: null,
  contactName: '',
  contactPhone: '',
  destination: '',
  departDate: '',
  days: null,
  peopleCount: 2,
  budget: null,
  preferences: '',
  handlerId: null,
  remark: '',
})

const form = reactive(emptyForm())

const rules = {
  contactName: [{ required: true, message: '请填写联系人', trigger: 'blur' }],
  contactPhone: [
    { required: true, message: '请填写联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
  destination: [{ required: true, message: '请填写目的地', trigger: 'blur' }],
}

function fetchCustomers(keyword) {
  return pageCustomers({ keyword: keyword || undefined, pageNum: 1, pageSize: 20 })
}

function fetchUsers(keyword) {
  return pageUsers({ username: keyword || undefined, pageNum: 1, pageSize: 20 })
}

watch(
  () => props.modelValue,
  async (open) => {
    if (!open) return
    Object.assign(form, emptyForm())
    customerLabel.value = ''
    formRef.value?.clearValidate()
    if (!isEdit.value || !props.requirementId) return
    try {
      const detail = await getRequirement(props.requirementId)
      Object.assign(form, {
        customerId: detail.customerId,
        contactName: detail.contactName,
        // 脱敏号码只在用户重新输入时才提交，避免把 **** 写回库
        contactPhone: detail.contactPhone || '',
        destination: detail.destination,
        departDate: detail.departDate || '',
        days: detail.days,
        peopleCount: detail.peopleCount,
        budget: detail.budget === null || detail.budget === undefined ? null : Number(detail.budget),
        preferences: detail.preferences || '',
        handlerId: detail.handlerId,
        remark: detail.remark || '',
      })
      customerLabel.value = detail.customerNickname || ''
    } catch (error) {
      ElMessage.error(error.message || '需求加载失败')
    }
  }
)

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  const payload = { ...form }
  if (isEdit.value && String(payload.contactPhone).includes('*')) delete payload.contactPhone
  Object.keys(payload).forEach((key) => {
    if (payload[key] === '') payload[key] = null
  })
  try {
    if (isEdit.value) {
      await updateRequirement(props.requirementId, payload)
      ElMessage.success('需求已更新')
    } else {
      await createRequirement(payload)
      ElMessage.success('需求已录入')
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
    :title="isEdit ? '编辑定制需求' : '录入定制需求'"
    size="540px"
    append-to-body
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="small">
      <el-form-item label="关联会员（可选）">
        <RemotePicker
          v-model="form.customerId"
          :fetcher="fetchCustomers"
          :formatter="(row) => `${row.nickname || '未命名'}（${row.phone}）`"
          :initial-label="customerLabel"
          placeholder="搜索昵称/手机号"
        />
      </el-form-item>
      <div class="grid grid-cols-2 gap-2">
        <el-form-item label="联系人" prop="contactName">
          <el-input v-model="form.contactName" maxlength="50" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="form.contactPhone" maxlength="11" placeholder="11 位手机号" />
        </el-form-item>
      </div>
      <el-form-item label="目的地" prop="destination">
        <el-input v-model="form.destination" maxlength="100" placeholder="如：云南 大理 丽江" />
      </el-form-item>
      <div class="grid grid-cols-2 gap-2">
        <el-form-item label="计划出行日期">
          <el-date-picker v-model="form.departDate" type="date" value-format="YYYY-MM-DD" class="!w-full" placeholder="可留空" />
        </el-form-item>
        <el-form-item label="预算（元）">
          <el-input-number v-model="form.budget" :min="0" :precision="2" :step="1000" class="w-full" controls-position="right" />
        </el-form-item>
        <el-form-item label="出行天数">
          <el-input-number v-model="form.days" :min="1" :max="365" class="w-full" controls-position="right" />
        </el-form-item>
        <el-form-item label="出行人数">
          <el-input-number v-model="form.peopleCount" :min="1" :max="999" class="w-full" controls-position="right" />
        </el-form-item>
      </div>
      <el-form-item label="偏好与要求">
        <el-input v-model="form.preferences" type="textarea" :rows="4" placeholder="亲子、酒店要求、忌口、必须打卡的景点等" />
      </el-form-item>
      <el-form-item label="跟进人">
        <RemotePicker
          v-model="form.handlerId"
          :fetcher="fetchUsers"
          :formatter="(row) => `${row.nickname || row.username}（${row.username}）`"
          placeholder="不选则留待分配"
        />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="255" show-word-limit />
      </el-form-item>
      <p class="text-[10px] leading-4 text-[#94a3b8]">
        联系电话加密存储、接口出口脱敏；编辑时若要改号码请整段重新输入。
      </p>
    </el-form>
    <template #footer>
      <div class="flex justify-end gap-1">
        <el-button size="small" @click="emit('update:modelValue', false)">取消</el-button>
        <el-button size="small" type="primary" :loading="saving" @click="submit">
          {{ isEdit ? '保存修改' : '录入需求' }}
        </el-button>
      </div>
    </template>
  </el-drawer>
</template>
