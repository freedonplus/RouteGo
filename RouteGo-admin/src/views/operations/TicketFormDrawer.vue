<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { createTicket, listTicketHandlers } from '@/api/operations'
import { pageOrders } from '@/api/trade'
import { pageCustomers } from '@/api/customer'
import RemotePicker from '@/components/RemotePicker.vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
})
const emit = defineEmits(['update:modelValue', 'saved'])

const formRef = ref()
const saving = ref(false)

const form = reactive({
  type: 1,
  title: '',
  content: '',
  priority: 2,
  customerId: null,
  orderId: null,
  handlerId: null,
})

const customerLabel = ref('')
const orderLabel = ref('')

const rules = {
  type: [{ required: true, message: '请选择工单类型', trigger: 'change' }],
  title: [{ required: true, message: '请填写工单标题', trigger: 'blur' }],
  content: [{ required: true, message: '请填写问题描述', trigger: 'blur' }],
}

const canSubmit = computed(() => form.title.trim() && form.content.trim())

function fetchCustomers(keyword) {
  return pageCustomers({ keyword: keyword || undefined, pageNum: 1, pageSize: 20 })
}

function fetchOrders(keyword) {
  return pageOrders({ orderNo: keyword || undefined, pageNum: 1, pageSize: 20 })
}

function fetchUsers(keyword) {
  return listTicketHandlers(keyword)
}

function handlerLabel(row) {
  return `${row.handlerName || row.username || `#${row.handlerId}`} · 处理中 ${row.processing ?? 0} 单`
}

watch(
  () => props.modelValue,
  (open) => {
    if (!open) return
    Object.assign(form, {
      type: 1,
      title: '',
      content: '',
      priority: 2,
      customerId: null,
      orderId: null,
      handlerId: null,
    })
    customerLabel.value = ''
    orderLabel.value = ''
    formRef.value?.clearValidate()
  }
)

/** 选了订单就把会员一并带出来，避免两边对不上 */
function onOrderChange(row) {
  orderLabel.value = row ? `${row.orderNo}` : ''
  if (row) {
    form.customerId = row.customerId
    customerLabel.value = row.customerNickname || ''
  }
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await createTicket({ ...form })
    ElMessage.success('工单已创建')
    emit('saved')
    emit('update:modelValue', false)
  } catch (error) {
    ElMessage.error(error.message || '创建失败')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <el-drawer
    :model-value="modelValue"
    title="新建客服工单"
    size="520px"
    append-to-body
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="small">
      <div class="grid grid-cols-2 gap-2">
        <el-form-item label="工单类型" prop="type">
          <el-select v-model="form.type" class="w-full">
            <el-option label="售前咨询" :value="1" />
            <el-option label="订单问题" :value="2" />
            <el-option label="退款售后" :value="3" />
            <el-option label="投诉建议" :value="4" />
            <el-option label="其他" :value="5" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="form.priority" class="w-full">
            <el-option label="低" :value="1" />
            <el-option label="中" :value="2" />
            <el-option label="高" :value="3" />
          </el-select>
        </el-form-item>
      </div>
      <el-form-item label="工单标题" prop="title">
        <el-input v-model="form.title" maxlength="100" show-word-limit placeholder="一句话概括问题" />
      </el-form-item>
      <el-form-item label="问题描述" prop="content">
        <el-input v-model="form.content" type="textarea" :rows="5" placeholder="客户原话、诉求、已尝试的处理" />
      </el-form-item>

      <p class="rg-section-title">关联信息（可选）</p>
      <el-form-item label="关联会员">
        <RemotePicker
          v-model="form.customerId"
          :fetcher="fetchCustomers"
          :formatter="(row) => `${row.nickname || '未命名'}（${row.phone}）`"
          :initial-label="customerLabel"
          placeholder="搜索昵称/手机号"
        />
      </el-form-item>
      <el-form-item label="关联订单">
        <RemotePicker
          v-model="form.orderId"
          :fetcher="fetchOrders"
          :formatter="(row) => `${row.orderNo} · ${row.routeTitle || ''}`"
          :initial-label="orderLabel"
          placeholder="搜索订单号"
          @change="onOrderChange"
        />
      </el-form-item>
      <el-form-item label="直接指派处理人">
        <RemotePicker
          v-model="form.handlerId"
          :fetcher="fetchUsers"
          :formatter="handlerLabel"
          placeholder="不选则进入待处理池"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="flex justify-end gap-1">
        <el-button size="small" @click="emit('update:modelValue', false)">取消</el-button>
        <el-button size="small" type="primary" :loading="saving" :disabled="!canSubmit" @click="submit">创建工单</el-button>
      </div>
    </template>
  </el-drawer>
</template>
