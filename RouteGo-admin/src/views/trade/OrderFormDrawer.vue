<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Delete, Plus } from '@element-plus/icons-vue'
import { listDepartures, pageRoutes } from '@/api/catalog'
import { pageCustomers } from '@/api/customer'
import { createOrder } from '@/api/trade'
import { formatMoney } from '@/utils/format'
import RemotePicker from '@/components/RemotePicker.vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
})
const emit = defineEmits(['update:modelValue', 'saved'])

const formRef = ref()
const saving = ref(false)
const departures = ref([])
const routeLabel = ref('')
const customerLabel = ref('')

const form = reactive({
  customerId: null,
  routeId: null,
  departureId: null,
  departDate: '',
  travelerCount: 1,
  unitPrice: null,
  discountAmount: null,
  contactName: '',
  contactPhone: '',
  source: 1,
  remark: '',
  travelers: [],
})

const rules = {
  customerId: [{ required: true, message: '请选择会员', trigger: 'change' }],
  routeId: [{ required: true, message: '请选择线路', trigger: 'change' }],
  travelerCount: [{ required: true, message: '请填写出行人数', trigger: 'blur' }],
  contactName: [{ required: true, message: '请填写联系人', trigger: 'blur' }],
  contactPhone: [
    { required: true, message: '请填写联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
}

const selectedDeparture = computed(
  () => departures.value.find((item) => item.id === form.departureId) || null
)

const totalAmount = computed(() => {
  const unit = Number(form.unitPrice || 0)
  return unit * Number(form.travelerCount || 0)
})

const payAmount = computed(() => totalAmount.value - Number(form.discountAmount || 0))

function fetchRoutes(keyword) {
  return pageRoutes({ title: keyword || undefined, status: 1, pageNum: 1, pageSize: 20 })
}

function fetchCustomers(keyword) {
  return pageCustomers({ keyword: keyword || undefined, pageNum: 1, pageSize: 20 })
}

function reset() {
  Object.assign(form, {
    customerId: null,
    routeId: null,
    departureId: null,
    departDate: '',
    travelerCount: 1,
    unitPrice: null,
    discountAmount: null,
    contactName: '',
    contactPhone: '',
    source: 1,
    remark: '',
    travelers: [],
  })
  departures.value = []
  routeLabel.value = ''
  customerLabel.value = ''
  formRef.value?.clearValidate()
}

watch(
  () => props.modelValue,
  (open) => {
    if (open) reset()
  }
)

watch(
  () => form.routeId,
  async (routeId) => {
    form.departureId = null
    form.departDate = ''
    departures.value = []
    if (!routeId) return
    try {
      departures.value = (await listDepartures(routeId)).filter((item) => item.status === 1 && item.remaining > 0)
    } catch {
      departures.value = []
    }
  }
)

watch(
  () => form.departureId,
  (id) => {
    const departure = departures.value.find((item) => item.id === id)
    if (!departure) return
    form.departDate = departure.departDate
    form.unitPrice = Number(departure.adultPrice)
  }
)

watch(
  () => form.travelerCount,
  (count) => {
    const size = Number(count || 0)
    if (size > 0 && form.travelers.length > size) form.travelers.splice(size)
  }
)

function onCustomerChange(row) {
  customerLabel.value = row ? `${row.nickname || '未命名'}（${row.phone}）` : ''
  if (!row) return
  form.contactName = row.nickname || row.realName || ''
  // 手机号在接口里是脱敏的，只有不带 * 时才能直接带出
  form.contactPhone = row.phone && !String(row.phone).includes('*') ? row.phone : ''
}

function onRouteChange(row) {
  routeLabel.value = row ? `${row.title}（${row.routeNo}）` : ''
  if (row && form.unitPrice === null) form.unitPrice = Number(row.minPrice || 0)
}

function addTraveler() {
  form.travelers.push({ name: '', idCard: '', phone: '', gender: 0, travelerType: 1, remark: '' })
  form.travelerCount = Math.max(form.travelerCount, form.travelers.length)
}

function removeTraveler(index) {
  form.travelers.splice(index, 1)
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  if (form.departureId === null && !form.departDate) {
    ElMessage.warning('请选择班期或填写出发日期')
    return
  }
  const travelers = form.travelers.filter((item) => item.name?.trim())
  if (travelers.length > form.travelerCount) {
    ElMessage.warning('出行人数量不能多于出行人数')
    return
  }
  saving.value = true
  try {
    await createOrder({
      ...form,
      travelers,
      unitPrice: form.unitPrice === null ? undefined : form.unitPrice,
      discountAmount: form.discountAmount === null ? undefined : form.discountAmount,
    })
    ElMessage.success('订单已创建')
    emit('saved')
    emit('update:modelValue', false)
  } catch (error) {
    ElMessage.error(error.message || '下单失败')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <el-drawer
    :model-value="modelValue"
    title="后台代录订单"
    size="700px"
    append-to-body
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="small">
      <p class="rg-section-title">下单信息</p>
      <div class="grid grid-cols-2 gap-2">
        <el-form-item label="会员" prop="customerId">
          <RemotePicker
            v-model="form.customerId"
            :fetcher="fetchCustomers"
            :formatter="(row) => `${row.nickname || '未命名'}（${row.phone}）`"
            :initial-label="customerLabel"
            placeholder="搜索昵称/手机号"
            @change="onCustomerChange"
          />
        </el-form-item>
        <el-form-item label="线路" prop="routeId">
          <RemotePicker
            v-model="form.routeId"
            :fetcher="fetchRoutes"
            :formatter="(row) => `${row.title}（${row.routeNo}）`"
            :initial-label="routeLabel"
            placeholder="搜索上架线路"
            @change="onRouteChange"
          />
        </el-form-item>
        <el-form-item label="班期">
          <el-select v-model="form.departureId" clearable placeholder="选择可售班期" class="w-full">
            <el-option
              v-for="item in departures"
              :key="item.id"
              :label="`${item.departDate} · 余 ${item.remaining} · ${formatMoney(item.adultPrice)}`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="出发日期">
          <el-date-picker
            v-model="form.departDate"
            type="date"
            value-format="YYYY-MM-DD"
            :disabled="Boolean(selectedDeparture)"
            placeholder="选择日期"
            class="!w-full"
          />
        </el-form-item>
        <el-form-item label="出行人数" prop="travelerCount">
          <el-input-number v-model="form.travelerCount" :min="1" :max="50" class="w-full" controls-position="right" />
        </el-form-item>
        <el-form-item label="单价（元）">
          <el-input-number v-model="form.unitPrice" :min="0" :precision="2" :step="100" class="w-full" controls-position="right" />
        </el-form-item>
        <el-form-item label="优惠金额（元）">
          <el-input-number v-model="form.discountAmount" :min="0" :precision="2" :step="10" class="w-full" controls-position="right" />
        </el-form-item>
        <el-form-item label="来源">
          <el-select v-model="form.source" class="w-full">
            <el-option label="后台代录" :value="1" />
            <el-option label="微信小程序" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="联系人" prop="contactName">
          <el-input v-model="form.contactName" maxlength="50" placeholder="出行联系人" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="form.contactPhone" maxlength="20" placeholder="11 位手机号" />
        </el-form-item>
      </div>
      <el-form-item label="备注">
        <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="255" show-word-limit placeholder="特殊要求" />
      </el-form-item>

      <div class="mb-1 flex items-center justify-between">
        <p class="rg-section-title !mb-0">出行人（可选）</p>
        <el-button size="small" :icon="Plus" @click="addTraveler">添加出行人</el-button>
      </div>
      <div v-if="form.travelers.length" class="space-y-1">
        <div v-for="(traveler, index) in form.travelers" :key="index" class="rounded border border-[#e2e8f0] p-2">
          <div class="grid grid-cols-4 gap-1">
            <el-input v-model="traveler.name" size="small" placeholder="姓名" maxlength="50" />
            <el-input v-model="traveler.idCard" size="small" placeholder="证件号" maxlength="32" />
            <el-input v-model="traveler.phone" size="small" placeholder="手机号" maxlength="20" />
            <div class="flex gap-1">
              <el-select v-model="traveler.gender" size="small" style="width: 70px">
                <el-option label="未知" :value="0" />
                <el-option label="男" :value="1" />
                <el-option label="女" :value="2" />
              </el-select>
              <el-select v-model="traveler.travelerType" size="small" style="width: 70px">
                <el-option label="成人" :value="1" />
                <el-option label="儿童" :value="2" />
              </el-select>
              <el-button size="small" :icon="Delete" circle @click="removeTraveler(index)" />
            </div>
          </div>
        </div>
      </div>
      <p v-else class="text-[10px] text-[#94a3b8]">可以不填，后续在订单详情里补录出行人。</p>

      <div class="mt-3 rounded bg-[#f8fafc] px-3 py-2">
        <div class="flex items-center justify-between text-[11px] text-[#475569]">
          <span>订单总额</span>
          <span class="font-mono">{{ formatMoney(totalAmount) }}</span>
        </div>
        <div class="mt-0.5 flex items-center justify-between text-[11px] text-[#475569]">
          <span>优惠</span>
          <span class="font-mono">-{{ formatMoney(form.discountAmount || 0) }}</span>
        </div>
        <div class="mt-1 flex items-center justify-between border-t border-[#e2e8f0] pt-1 text-[11px] font-medium text-[#121212]">
          <span>应付金额</span>
          <span class="font-mono text-[#a8435f]">{{ formatMoney(payAmount) }}</span>
        </div>
      </div>
    </el-form>
    <template #footer>
      <div class="flex justify-end gap-1">
        <el-button size="small" @click="emit('update:modelValue', false)">取消</el-button>
        <el-button size="small" type="primary" :loading="saving" @click="submit">创建订单</el-button>
      </div>
    </template>
  </el-drawer>
</template>
