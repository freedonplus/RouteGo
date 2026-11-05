<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Plus } from '@element-plus/icons-vue'
import {
  applyRefund,
  cancelOrder,
  completeOrder,
  getOrder,
  payOrder,
  replaceTravelers,
  startOrder,
  updateOrder,
} from '@/api/trade'
import { formatDateTime, formatMoney } from '@/utils/format'
import StatusTag from '@/components/StatusTag.vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  orderId: { type: [Number, String], default: null },
})
const emit = defineEmits(['update:modelValue', 'changed'])

const loading = ref(false)
const acting = ref(false)
const detail = ref(null)
const activeTab = ref('traveler')

const STATUS_LABELS = ['待支付', '已支付', '出行中', '已完成', '已取消', '退款中', '已退款']
const STATUS_TONES = ['warning', 'success', 'info', 'success', 'muted', 'warning', 'danger']

const contactOpen = ref(false)
const contactForm = reactive({ contactName: '', contactPhone: '', remark: '' })

const travelerOpen = ref(false)
const travelerRows = ref([])

const refundOpen = ref(false)
const refundForm = reactive({ amount: 0, reason: '' })

const status = computed(() => Number(detail.value?.status ?? -1))
const canPay = computed(() => status.value === 0)
const canCancel = computed(() => status.value === 0)
const canStart = computed(() => status.value === 1)
const canComplete = computed(() => status.value === 2)
const canRefund = computed(() => status.value === 1)

async function load() {
  if (!props.orderId) return
  loading.value = true
  try {
    detail.value = await getOrder(props.orderId)
  } catch (error) {
    ElMessage.error(error.message || '订单详情加载失败')
    detail.value = null
  } finally {
    loading.value = false
  }
}

watch(
  () => props.modelValue,
  (open) => {
    if (!open) return
    activeTab.value = 'traveler'
    detail.value = null
    load()
  }
)

async function act(fn, successText) {
  acting.value = true
  try {
    await fn()
    ElMessage.success(successText)
    await load()
    emit('changed')
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    acting.value = false
  }
}

async function pay() {
  try {
    await ElMessageBox.confirm(
      `确认收款 ${formatMoney(detail.value.payAmount)} 吗？`,
      '收款确认',
      { type: 'warning', confirmButtonText: '确认收款', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  act(() => payOrder(props.orderId, { payType: 1, remark: '后台收款' }), '收款成功')
}

async function cancel() {
  try {
    const { value } = await ElMessageBox.prompt('请填写取消原因', '取消订单', {
      inputPlaceholder: '如：客户临时有事',
      inputValidator: (text) => (text && text.trim() ? true : '请填写取消原因'),
      confirmButtonText: '取消订单',
      cancelButtonText: '返回',
    })
    await act(() => cancelOrder(props.orderId, value.trim()), '订单已取消')
  } catch {
    // 用户关闭弹窗
  }
}

function openContact() {
  Object.assign(contactForm, {
    contactName: detail.value.contactName || '',
    contactPhone: detail.value.contactPhone || '',
    remark: detail.value.remark || '',
  })
  contactOpen.value = true
}

async function submitContact() {
  if (!contactForm.contactName.trim() || !contactForm.contactPhone.trim()) {
    ElMessage.warning('联系人与电话不能为空')
    return
  }
  contactOpen.value = false
  await act(() => updateOrder(props.orderId, { ...contactForm }), '联系人已更新')
}

function openTravelers() {
  travelerRows.value = (detail.value.travelers || []).map((item) => ({
    name: item.name || '',
    idCard: item.idCard || '',
    phone: item.phone || '',
    gender: item.gender ?? 0,
    travelerType: item.travelerType ?? 1,
    remark: item.remark || '',
  }))
  travelerOpen.value = true
}

function addTravelerRow() {
  travelerRows.value.push({ name: '', idCard: '', phone: '', gender: 0, travelerType: 1, remark: '' })
}

async function submitTravelers() {
  const items = travelerRows.value.filter((item) => item.name?.trim())
  travelerOpen.value = false
  await act(() => replaceTravelers(props.orderId, items), '出行人已保存')
}

function openRefund() {
  refundForm.amount = Number(detail.value.payAmount || 0)
  refundForm.reason = ''
  refundOpen.value = true
}

async function submitRefund() {
  if (!refundForm.amount || refundForm.amount <= 0) {
    ElMessage.warning('退款金额必须大于 0')
    return
  }
  if (!refundForm.reason.trim()) {
    ElMessage.warning('请填写退款原因')
    return
  }
  refundOpen.value = false
  await act(() => applyRefund(props.orderId, { ...refundForm }), '退款申请已提交')
}
</script>

<template>
  <el-drawer
    :model-value="modelValue"
    :title="detail ? `订单 ${detail.orderNo}` : '订单详情'"
    size="780px"
    append-to-body
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div v-loading="loading" class="space-y-2">
      <template v-if="detail">
        <div class="flex flex-wrap items-center gap-1">
          <StatusTag :value="detail.status" :labels="STATUS_LABELS" :tone="STATUS_TONES[status]" />
          <span class="text-[11px] text-[#64748b]">{{ detail.payStatusDesc }}</span>
          <div class="ml-auto flex flex-wrap gap-1">
            <el-button v-if="canPay" v-perm="'trade:Orders:pay'" size="small" type="primary" :loading="acting" @click="pay">
              收款
            </el-button>
            <el-button v-if="canStart" v-perm="'trade:Orders:startTravel'" size="small" type="primary" :loading="acting" @click="act(() => startOrder(props.orderId), '订单已进入出行中')">
              开始行程
            </el-button>
            <el-button v-if="canComplete" v-perm="'trade:Orders:complete'" size="small" type="primary" :loading="acting" @click="act(() => completeOrder(props.orderId), '订单已完成')">
              完成订单
            </el-button>
            <el-button v-if="canRefund" v-perm="'trade:RefundRecord:apply'" size="small" :loading="acting" @click="openRefund">
              申请退款
            </el-button>
            <el-button v-if="canCancel" v-perm="'trade:Orders:cancel'" size="small" :loading="acting" @click="cancel">取消订单</el-button>
            <el-button v-perm="'trade:Orders:update'" size="small" @click="openContact">改联系人</el-button>
          </div>
        </div>

        <el-descriptions :column="2" size="small" border>
          <el-descriptions-item label="会员">
            {{ detail.customerNickname || `#${detail.customerId}` }}
            <span class="ml-1 font-mono text-[10px] text-[#94a3b8]">{{ detail.customerPhone }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="线路">{{ detail.routeTitle || `#${detail.routeId}` }}</el-descriptions-item>
          <el-descriptions-item label="出发日期">{{ detail.departDate || '—' }}</el-descriptions-item>
          <el-descriptions-item label="出行人数">{{ detail.travelerCount }} 人</el-descriptions-item>
          <el-descriptions-item label="单价">{{ formatMoney(detail.unitPrice) }}</el-descriptions-item>
          <el-descriptions-item label="总额">{{ formatMoney(detail.totalAmount) }}</el-descriptions-item>
          <el-descriptions-item label="优惠">{{ formatMoney(detail.discountAmount) }}</el-descriptions-item>
          <el-descriptions-item label="实付">
            <span class="font-mono font-medium text-[#a8435f]">{{ formatMoney(detail.payAmount) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="联系人">{{ detail.contactName }} · {{ detail.contactPhone }}</el-descriptions-item>
          <el-descriptions-item label="来源">{{ detail.sourceDesc || '—' }}</el-descriptions-item>
          <el-descriptions-item label="支付时间">{{ formatDateTime(detail.payTime) }}</el-descriptions-item>
          <el-descriptions-item label="取消时间">{{ formatDateTime(detail.cancelTime) }}</el-descriptions-item>
          <el-descriptions-item label="取消原因" :span="2">{{ detail.cancelReason || '—' }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detail.remark || '—' }}</el-descriptions-item>
        </el-descriptions>

        <el-tabs v-model="activeTab">
          <el-tab-pane :label="`出行人（${detail.travelers?.length || 0}）`" name="traveler">
            <div class="mb-2 flex justify-end">
              <el-button v-perm="'trade:OrderTraveler:replaceAll'" size="small" @click="openTravelers">维护出行人</el-button>
            </div>
            <el-table :data="detail.travelers || []" size="small" border stripe empty-text="还没有出行人">
              <el-table-column prop="name" label="姓名" min-width="90" />
              <el-table-column prop="idCard" label="证件号" min-width="160" />
              <el-table-column prop="phone" label="手机号" width="120" />
              <el-table-column prop="genderDesc" label="性别" width="60" />
              <el-table-column prop="travelerTypeDesc" label="类型" width="60" />
              <el-table-column prop="remark" label="备注" min-width="100" />
            </el-table>
          </el-tab-pane>

          <el-tab-pane :label="`状态流水（${detail.statusLogs?.length || 0}）`" name="log">
            <el-timeline v-if="detail.statusLogs?.length">
              <el-timeline-item
                v-for="log in detail.statusLogs"
                :key="log.id"
                :timestamp="formatDateTime(log.createTime)"
                placement="top"
              >
                <p class="text-[11px] text-[#1e293b]">
                  {{ log.fromStatusDesc || '创建' }} → <span class="font-medium">{{ log.toStatusDesc }}</span>
                </p>
                <p class="mt-0.5 text-[10px] text-[#94a3b8]">
                  {{ log.operatorName || '系统' }}<span v-if="log.remark"> · {{ log.remark }}</span>
                </p>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-else description="没有状态流水" :image-size="60" />
          </el-tab-pane>

          <el-tab-pane :label="`支付记录（${detail.paymentRecords?.length || 0}）`" name="payment">
            <el-table :data="detail.paymentRecords || []" size="small" border stripe empty-text="没有支付记录">
              <el-table-column prop="payNo" label="支付单号" min-width="160">
                <template #default="{ row }">
                  <span class="font-mono text-[11px]">{{ row.payNo }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="payTypeDesc" label="方式" width="90" />
              <el-table-column label="金额" width="100">
                <template #default="{ row }">
                  <span class="font-mono text-[11px] text-[#a8435f]">{{ formatMoney(row.amount) }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="statusDesc" label="状态" width="70" />
              <el-table-column label="支付时间" width="134">
                <template #default="{ row }">
                  <span class="font-mono text-[11px]">{{ formatDateTime(row.payTime) }}</span>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane :label="`退款记录（${detail.refundRecords?.length || 0}）`" name="refund">
            <el-table :data="detail.refundRecords || []" size="small" border stripe empty-text="没有退款记录">
              <el-table-column prop="refundNo" label="退款单号" min-width="160">
                <template #default="{ row }">
                  <span class="font-mono text-[11px]">{{ row.refundNo }}</span>
                </template>
              </el-table-column>
              <el-table-column label="金额" width="100">
                <template #default="{ row }">
                  <span class="font-mono text-[11px] text-[#a8435f]">{{ formatMoney(row.amount) }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="statusDesc" label="状态" width="80" />
              <el-table-column prop="reason" label="原因" min-width="120" />
              <el-table-column label="申请时间" width="134">
                <template #default="{ row }">
                  <span class="font-mono text-[11px]">{{ formatDateTime(row.applyTime) }}</span>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </template>
    </div>

    <el-dialog v-model="contactOpen" title="修改联系人" width="440px" append-to-body>
      <el-form :model="contactForm" label-position="top" size="small">
        <el-form-item label="联系人">
          <el-input v-model="contactForm.contactName" maxlength="50" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="contactForm.contactPhone" maxlength="20" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="contactForm.remark" type="textarea" :rows="2" maxlength="255" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="flex justify-end gap-1">
          <el-button size="small" @click="contactOpen = false">取消</el-button>
          <el-button size="small" type="primary" :loading="acting" @click="submitContact">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="travelerOpen" title="维护出行人" width="640px" append-to-body>
      <div class="space-y-1">
        <div v-for="(row, index) in travelerRows" :key="index" class="flex gap-1">
          <el-input v-model="row.name" size="small" placeholder="姓名" style="width: 100px" />
          <el-input v-model="row.idCard" size="small" placeholder="证件号" style="width: 160px" />
          <el-input v-model="row.phone" size="small" placeholder="手机号" style="width: 120px" />
          <el-select v-model="row.gender" size="small" style="width: 76px">
            <el-option label="未知" :value="0" />
            <el-option label="男" :value="1" />
            <el-option label="女" :value="2" />
          </el-select>
          <el-select v-model="row.travelerType" size="small" style="width: 76px">
            <el-option label="成人" :value="1" />
            <el-option label="儿童" :value="2" />
          </el-select>
          <el-button size="small" :icon="Delete" circle @click="travelerRows.splice(index, 1)" />
        </div>
      </div>
      <el-button class="mt-2" size="small" :icon="Plus" @click="addTravelerRow">添加一行</el-button>
      <p class="mt-1 text-[10px] text-[#94a3b8]">保存会用当前列表整体覆盖，空姓名的行会被忽略。</p>
      <template #footer>
        <div class="flex justify-end gap-1">
          <el-button size="small" @click="travelerOpen = false">取消</el-button>
          <el-button size="small" type="primary" :loading="acting" @click="submitTravelers">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="refundOpen" title="申请退款" width="440px" append-to-body>
      <el-form :model="refundForm" label-position="top" size="small">
        <el-form-item label="退款金额">
          <el-input-number v-model="refundForm.amount" :min="0.01" :precision="2" :max="Number(detail?.payAmount || 0)" class="w-full" controls-position="right" />
        </el-form-item>
        <el-form-item label="退款原因">
          <el-input v-model="refundForm.reason" type="textarea" :rows="3" maxlength="255" placeholder="如：行程取消" />
        </el-form-item>
      </el-form>
      <p class="text-[10px] leading-4 text-[#94a3b8]">
        申请后订单进入「退款中」，需在退款管理中审核；驳回会回到已支付。
      </p>
      <template #footer>
        <div class="flex justify-end gap-1">
          <el-button size="small" @click="refundOpen = false">取消</el-button>
          <el-button size="small" type="primary" :loading="acting" @click="submitRefund">提交申请</el-button>
        </div>
      </template>
    </el-dialog>
  </el-drawer>
</template>
