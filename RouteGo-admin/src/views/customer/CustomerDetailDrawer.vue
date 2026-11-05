<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  adjustCustomerPoints,
  getCustomer,
  listCustomerCoupons,
  resetCustomerPassword,
  setCustomerLevel,
  setCustomerStatus,
  updateCustomer,
  updateCustomerPhone,
  revokeCoupon,
  useCoupon,
} from '@/api/customer'
import { formatDate, formatDateTime, formatMoney } from '@/utils/format'
import ImageUpload from '@/components/ImageUpload.vue'
import StatusTag from '@/components/StatusTag.vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  customerId: { type: [Number, String], default: null },
})
const emit = defineEmits(['update:modelValue', 'changed'])

const loading = ref(false)
const acting = ref(false)
const detail = ref(null)
const coupons = ref([])
const couponStatus = ref('')

const LEVEL_LABELS = ['普通会员', '银卡会员', '金卡会员', '钻石会员']

const profileOpen = ref(false)
const profileForm = reactive({
  nickname: '',
  avatar: '',
  realName: '',
  idCard: '',
  gender: 0,
  birthday: '',
  email: '',
})

const phoneOpen = ref(false)
const phoneForm = reactive({ phone: '' })

const passwordOpen = ref(false)
const passwordForm = reactive({ password: '' })

const levelOpen = ref(false)
const levelForm = reactive({ level: 1 })

const pointsOpen = ref(false)
const pointsForm = reactive({ delta: 0 })

const usedCount = computed(() => coupons.value.filter((item) => Number(item.status) === 1).length)

async function load() {
  if (!props.customerId) return
  loading.value = true
  try {
    detail.value = await getCustomer(props.customerId)
  } catch (error) {
    ElMessage.error(error.message || '会员详情加载失败')
    detail.value = null
  } finally {
    loading.value = false
  }
}

async function loadCoupons() {
  if (!props.customerId) return
  try {
    coupons.value = await listCustomerCoupons(
      props.customerId,
      couponStatus.value === '' ? undefined : couponStatus.value
    )
  } catch {
    coupons.value = []
  }
}

watch(
  () => props.modelValue,
  async (open) => {
    if (!open) return
    detail.value = null
    coupons.value = []
    couponStatus.value = ''
    await Promise.all([load(), loadCoupons()])
  }
)

watch(couponStatus, loadCoupons)

async function act(fn, text) {
  acting.value = true
  try {
    await fn()
    ElMessage.success(text)
    await Promise.all([load(), loadCoupons()])
    emit('changed')
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    acting.value = false
  }
}

function openProfile() {
  Object.assign(profileForm, {
    nickname: detail.value.nickname || '',
    avatar: detail.value.avatar || '',
    realName: detail.value.realName || '',
    idCard: detail.value.idCard || '',
    gender: detail.value.gender ?? 0,
    birthday: detail.value.birthday || '',
    email: detail.value.email || '',
  })
  profileOpen.value = true
}

async function submitProfile() {
  profileOpen.value = false
  await act(() => updateCustomer(props.customerId, { ...profileForm }), '资料已更新')
}

function openPhone() {
  phoneForm.phone = ''
  phoneOpen.value = true
}

async function submitPhone() {
  if (!/^1[3-9]\d{9}$/.test(phoneForm.phone)) {
    ElMessage.warning('手机号格式不正确')
    return
  }
  phoneOpen.value = false
  await act(() => updateCustomerPhone(props.customerId, phoneForm.phone), '手机号已换绑')
}

function openPassword() {
  passwordForm.password = ''
  passwordOpen.value = true
}

async function submitPassword() {
  if (passwordForm.password.length < 8) {
    ElMessage.warning('密码至少 8 位')
    return
  }
  passwordOpen.value = false
  await act(() => resetCustomerPassword(props.customerId, passwordForm.password), '密码已重置')
}

function openLevel() {
  levelForm.level = Number(detail.value.level || 1)
  levelOpen.value = true
}

async function submitLevel() {
  levelOpen.value = false
  await act(() => setCustomerLevel(props.customerId, levelForm.level), '等级已调整')
}

function openPoints() {
  pointsForm.delta = 0
  pointsOpen.value = true
}

async function submitPoints() {
  if (!pointsForm.delta) {
    ElMessage.warning('请输入积分变化量')
    return
  }
  pointsOpen.value = false
  await act(() => adjustCustomerPoints(props.customerId, pointsForm.delta), '积分已调整')
}

async function toggleStatus() {
  const next = detail.value.status === 1 ? 0 : 1
  try {
    await ElMessageBox.confirm(next === 1 ? '确定恢复该会员？' : '禁用后该会员无法登录，确定吗？', '状态变更', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  await act(() => setCustomerStatus(props.customerId, next), next === 1 ? '已恢复' : '已禁用')
}

async function revoke(row) {
  try {
    await ElMessageBox.confirm(`确定作废券 ${row.code} 吗？`, '作废优惠券', {
      type: 'warning',
      confirmButtonText: '作废',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  await act(() => revokeCoupon(row.id), '券已作废')
}

async function use(row) {
  try {
    const { value } = await ElMessageBox.prompt('输入关联订单 ID（可留空）', '核销优惠券', {
      inputPlaceholder: '订单 ID',
      inputPattern: /^\d*$/,
      inputErrorMessage: '只能填数字',
      confirmButtonText: '核销',
      cancelButtonText: '取消',
    })
    await act(() => useCoupon(row.id, value ? Number(value) : undefined), '券已核销')
  } catch {
    // 取消
  }
}
</script>

<template>
  <el-drawer
    :model-value="modelValue"
    :title="detail ? `会员 ${detail.nickname || detail.phone}` : '会员详情'"
    size="720px"
    append-to-body
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div v-loading="loading" class="space-y-2">
      <template v-if="detail">
        <div class="flex flex-wrap items-center gap-1">
          <StatusTag :value="detail.status" :labels="['禁用', '正常']" :tone="detail.status === 1 ? 'success' : 'muted'" />
          <span class="text-[11px] text-[#64748b]">{{ detail.levelDesc }} · {{ detail.points }} 积分</span>
          <div class="ml-auto flex flex-wrap gap-1">
            <el-button v-perm="'customer:Customer:update'" size="small" @click="openProfile">编辑资料</el-button>
            <el-button v-perm="'customer:Customer:updatePhone'" size="small" @click="openPhone">换绑手机号</el-button>
            <el-button v-perm="'customer:Customer:resetPassword'" size="small" @click="openPassword">重置密码</el-button>
            <el-button v-perm="'customer:Customer:updateLevel'" size="small" @click="openLevel">调整等级</el-button>
            <el-button v-perm="'customer:Customer:adjustPoints'" size="small" @click="openPoints">调整积分</el-button>
            <el-button v-perm="'customer:Customer:updateStatus'" size="small" :loading="acting" @click="toggleStatus">
              {{ detail.status === 1 ? '禁用' : '恢复' }}
            </el-button>
          </div>
        </div>

        <el-descriptions :column="2" size="small" border>
          <el-descriptions-item label="手机号">
            <span class="font-mono">{{ detail.phone }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ detail.email || '—' }}</el-descriptions-item>
          <el-descriptions-item label="真实姓名">{{ detail.realName || '—' }}</el-descriptions-item>
          <el-descriptions-item label="证件号">{{ detail.idCard || '—' }}</el-descriptions-item>
          <el-descriptions-item label="性别">{{ detail.genderDesc || '未知' }}</el-descriptions-item>
          <el-descriptions-item label="生日">{{ detail.birthday || '—' }}</el-descriptions-item>
          <el-descriptions-item label="订单数">{{ detail.orderCount }}</el-descriptions-item>
          <el-descriptions-item label="累计消费">{{ formatMoney(detail.totalAmount) }}</el-descriptions-item>
          <el-descriptions-item label="可用券">{{ detail.availableCouponCount }}</el-descriptions-item>
          <el-descriptions-item label="已用券">{{ detail.usedCouponCount }}</el-descriptions-item>
          <el-descriptions-item label="注册时间">{{ formatDateTime(detail.registerTime) }}</el-descriptions-item>
          <el-descriptions-item label="最近登录">{{ formatDateTime(detail.lastLoginTime) }}</el-descriptions-item>
        </el-descriptions>

        <div class="flex items-center justify-between">
          <p class="rg-section-title !mb-0">券包（当前 {{ coupons.length }} 张，已用 {{ usedCount }} 张）</p>
          <el-select v-model="couponStatus" size="small" style="width: 110px">
            <el-option label="全部" value="" />
            <el-option label="未使用" :value="0" />
            <el-option label="已使用" :value="1" />
            <el-option label="已过期" :value="2" />
          </el-select>
        </div>
        <el-table :data="coupons" size="small" border stripe empty-text="没有券">
          <el-table-column label="券" min-width="170">
            <template #default="{ row }">
              <p class="truncate text-[11px] font-medium text-[#1e293b]">{{ row.templateName }}</p>
              <p class="font-mono text-[10px] text-[#94a3b8]">{{ row.code }}</p>
            </template>
          </el-table-column>
          <el-table-column label="优惠" width="110">
            <template #default="{ row }">
              <span class="text-[11px] text-[#a8435f]">{{ row.benefitDesc }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="76">
            <template #default="{ row }">
              <StatusTag :value="row.status" :labels="['未使用', '已使用', '已过期']" />
            </template>
          </el-table-column>
          <el-table-column label="有效期至" width="100">
            <template #default="{ row }">
              <span class="font-mono text-[11px] text-[#64748b]">{{ formatDate(row.expireTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="110">
            <template #default="{ row }">
              <template v-if="Number(row.status) === 0">
                <el-button v-perm="'customer:Coupon:use'" link type="primary" size="small" @click="use(row)">核销</el-button>
                <el-button v-perm="'customer:Coupon:revoke'" link type="danger" size="small" @click="revoke(row)">作废</el-button>
              </template>
              <span v-else class="text-[10px] text-[#94a3b8]">—</span>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </div>

    <el-dialog v-model="profileOpen" title="编辑会员资料" width="480px" append-to-body>
      <el-form :model="profileForm" label-position="top" size="small">
        <div class="grid grid-cols-2 gap-2">
          <el-form-item label="昵称">
            <el-input v-model="profileForm.nickname" maxlength="50" />
          </el-form-item>
          <el-form-item label="真实姓名">
            <el-input v-model="profileForm.realName" maxlength="50" />
          </el-form-item>
          <el-form-item label="证件号">
            <el-input v-model="profileForm.idCard" maxlength="32" />
          </el-form-item>
          <el-form-item label="性别">
            <el-select v-model="profileForm.gender" class="w-full">
              <el-option label="未知" :value="0" />
              <el-option label="男" :value="1" />
              <el-option label="女" :value="2" />
            </el-select>
          </el-form-item>
          <el-form-item label="生日">
            <el-date-picker v-model="profileForm.birthday" type="date" value-format="YYYY-MM-DD" class="!w-full" />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="profileForm.email" maxlength="100" />
          </el-form-item>
        </div>
        <el-form-item label="头像">
          <ImageUpload v-model="profileForm.avatar" tip="拖拽头像到此处，或点击从本地选择" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="flex justify-end gap-1">
          <el-button size="small" @click="profileOpen = false">取消</el-button>
          <el-button size="small" type="primary" :loading="acting" @click="submitProfile">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="phoneOpen" title="换绑手机号" width="400px" append-to-body>
      <el-input v-model="phoneForm.phone" maxlength="11" placeholder="新的 11 位手机号" />
      <p class="mt-1 text-[10px] text-[#94a3b8]">换绑后登录账号同步变更，原手机号立即失效。</p>
      <template #footer>
        <div class="flex justify-end gap-1">
          <el-button size="small" @click="phoneOpen = false">取消</el-button>
          <el-button size="small" type="primary" :loading="acting" @click="submitPhone">确定换绑</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="passwordOpen" title="重置密码" width="400px" append-to-body>
      <el-input v-model="passwordForm.password" maxlength="32" show-password placeholder="8~32 位新密码" />
      <template #footer>
        <div class="flex justify-end gap-1">
          <el-button size="small" @click="passwordOpen = false">取消</el-button>
          <el-button size="small" type="primary" :loading="acting" @click="submitPassword">重置</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="levelOpen" title="调整会员等级" width="360px" append-to-body>
      <el-select v-model="levelForm.level" class="w-full" size="small">
        <el-option v-for="(label, index) in LEVEL_LABELS" :key="index" :label="label" :value="index + 1" />
      </el-select>
      <template #footer>
        <div class="flex justify-end gap-1">
          <el-button size="small" @click="levelOpen = false">取消</el-button>
          <el-button size="small" type="primary" :loading="acting" @click="submitLevel">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="pointsOpen" title="调整积分" width="360px" append-to-body>
      <el-input-number v-model="pointsForm.delta" :step="100" class="w-full" controls-position="right" />
      <p class="mt-1 text-[10px] text-[#94a3b8]">正数加分、负数扣分，扣完不会小于 0。</p>
      <template #footer>
        <div class="flex justify-end gap-1">
          <el-button size="small" @click="pointsOpen = false">取消</el-button>
          <el-button size="small" type="primary" :loading="acting" @click="submitPoints">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </el-drawer>
</template>
