<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  createCustomer,
  deleteCustomer,
  pageCustomers,
  setCustomerStatus,
} from '@/api/customer'
import { formatDate, formatDateTime, formatMoney, resolveFileUrl } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'
import FilterBar from '@/components/FilterBar.vue'
import StatusTag from '@/components/StatusTag.vue'
import CustomerDetailDrawer from './CustomerDetailDrawer.vue'

const loading = ref(false)
const rows = ref([])
const filters = reactive({ keyword: '', phone: '', level: '', status: '' })
const timeRange = ref([])
const page = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const detailOpen = ref(false)
const detailCustomerId = ref(null)

const createOpen = ref(false)
const creating = ref(false)
const createFormRef = ref()
const createForm = reactive({
  phone: '',
  password: '',
  nickname: '',
  realName: '',
  gender: 0,
  email: '',
  level: 1,
  points: 0,
  status: 1,
})

const createRules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
  password: [{ min: 8, max: 32, message: '密码长度 8~32 位', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
}

const LEVEL_LABELS = ['普通会员', '银卡会员', '金卡会员', '钻石会员']

async function load() {
  loading.value = true
  try {
    const result = await pageCustomers({
      keyword: filters.keyword || undefined,
      phone: filters.phone || undefined,
      level: filters.level === '' ? undefined : filters.level,
      status: filters.status === '' ? undefined : filters.status,
      registerTimeStart: timeRange.value?.[0] ? `${timeRange.value[0]} 00:00:00` : undefined,
      registerTimeEnd: timeRange.value?.[1] ? `${timeRange.value[1]} 23:59:59` : undefined,
      pageNum: page.pageNum,
      pageSize: page.pageSize,
    })
    rows.value = result.records || []
    page.total = Number(result.total || 0)
  } catch (error) {
    ElMessage.error(error.message || '会员加载失败')
    rows.value = []
    page.total = 0
  } finally {
    loading.value = false
  }
}

function search() {
  page.pageNum = 1
  load()
}

function resetFilters() {
  Object.assign(filters, { keyword: '', phone: '', level: '', status: '' })
  timeRange.value = []
  page.pageNum = 1
  load()
}

function openDetail(row) {
  detailCustomerId.value = row.id
  detailOpen.value = true
}

function openCreate() {
  Object.assign(createForm, {
    phone: '',
    password: '',
    nickname: '',
    realName: '',
    gender: 0,
    email: '',
    level: 1,
    points: 0,
    status: 1,
  })
  createOpen.value = true
}

async function submitCreate() {
  const valid = await createFormRef.value?.validate().catch(() => false)
  if (!valid) return
  creating.value = true
  try {
    const payload = { ...createForm }
    if (!payload.password) delete payload.password
    await createCustomer(payload)
    ElMessage.success('会员已创建')
    createOpen.value = false
    load()
  } catch (error) {
    ElMessage.error(error.message || '创建失败')
  } finally {
    creating.value = false
  }
}

async function toggle(row) {
  const next = row.status === 1 ? 0 : 1
  try {
    await ElMessageBox.confirm(
      next === 1 ? `确定恢复会员「${row.nickname || row.phone}」吗？` : `禁用后「${row.nickname || row.phone}」将无法登录，确定吗？`,
      '状态变更',
      { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await setCustomerStatus(row.id, next)
    ElMessage.success(next === 1 ? '已恢复' : '已禁用')
    load()
  } catch (error) {
    ElMessage.error(error.message || '状态更新失败')
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确定删除会员「${row.nickname || row.phone}」吗？`, '删除会员', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  try {
    await deleteCustomer(row.id)
    ElMessage.success('会员已删除')
    load()
  } catch (error) {
    ElMessage.error(error.message || '删除失败')
  }
}

onMounted(load)
</script>

<template>
  <div class="pb-4">
    <PageHeader title="会员列表" description="手机号加密存储，列表展示为脱敏号码；禁用后会员无法登录">
      <template #actions>
        <el-button size="small" :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
        <el-button v-perm="'customer:Customer:create'" size="small" type="primary" :icon="Plus" @click="openCreate">
          新增会员
        </el-button>
      </template>
    </PageHeader>

    <div class="rg-page space-y-2">
      <FilterBar>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">关键字</span>
          <el-input v-model="filters.keyword" size="small" placeholder="昵称/姓名" clearable style="width: 140px" @keyup.enter="search" />
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">手机号</span>
          <el-input v-model="filters.phone" size="small" placeholder="完整手机号" clearable style="width: 140px" class="font-mono" @keyup.enter="search" />
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">等级</span>
          <el-select v-model="filters.level" size="small" style="width: 108px">
            <el-option label="全部" value="" />
            <el-option v-for="(label, index) in LEVEL_LABELS" :key="index" :label="label" :value="index + 1" />
          </el-select>
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">状态</span>
          <el-select v-model="filters.status" size="small" style="width: 92px">
            <el-option label="全部" value="" />
            <el-option label="正常" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">注册时间</span>
          <el-date-picker
            v-model="timeRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始"
            end-placeholder="结束"
            size="small"
            style="width: 224px"
          />
        </label>
        <template #actions>
          <el-button size="small" type="primary" :icon="Search" @click="search">查询</el-button>
          <el-button size="small" @click="resetFilters">重置</el-button>
        </template>
      </FilterBar>

      <div class="rg-card min-w-0">
        <div class="rg-scroll-x">
          <el-table v-loading="loading" :data="rows" size="small" border stripe row-key="id" empty-text="没有匹配的会员">
            <el-table-column label="会员" min-width="170">
              <template #default="{ row }">
                <div class="flex items-center gap-1.5">
                  <el-avatar :size="24" :src="resolveFileUrl(row.avatar)">{{ (row.nickname || '会')[0] }}</el-avatar>
                  <div class="min-w-0">
                    <p class="truncate text-[11px] font-medium text-[#1e293b]">
                      {{ row.nickname || '未命名' }}
                      <span v-if="row.wechatBound" class="ml-1 rounded bg-[#ecfdf5] px-1 text-[9px] text-[#15803d]">微信</span>
                    </p>
                    <p class="font-mono text-[10px] text-[#94a3b8]">{{ row.phone }}</p>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="等级" width="96">
              <template #default="{ row }">
                <span class="text-[11px] text-[#64748b]">{{ row.levelDesc }}</span>
              </template>
            </el-table-column>
            <el-table-column label="积分" width="76">
              <template #default="{ row }">
                <span class="font-mono text-[11px] text-[#64748b]">{{ row.points }}</span>
              </template>
            </el-table-column>
            <el-table-column label="订单" width="66">
              <template #default="{ row }">
                <span class="font-mono text-[11px] text-[#64748b]">{{ row.orderCount }}</span>
              </template>
            </el-table-column>
            <el-table-column label="累计消费" width="110">
              <template #default="{ row }">
                <span class="font-mono text-[11px] text-[#a8435f]">{{ formatMoney(row.totalAmount) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="可用券" width="70">
              <template #default="{ row }">
                <span class="font-mono text-[11px] text-[#64748b]">{{ row.availableCouponCount }}</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="76">
              <template #default="{ row }">
                <StatusTag :value="row.status" :labels="['禁用', '正常']" :tone="row.status === 1 ? 'success' : 'muted'" />
              </template>
            </el-table-column>
            <el-table-column label="注册时间" width="100">
              <template #default="{ row }">
                <span class="font-mono text-[11px] text-[#64748b]">{{ formatDate(row.registerTime) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="170" fixed="right">
              <template #default="{ row }">
                <el-button v-perm="'customer:Customer:detail'" link type="primary" size="small" @click="openDetail(row)">详情</el-button>
                <el-button v-perm="'customer:Customer:updateStatus'" link size="small" @click="toggle(row)">
                  {{ row.status === 1 ? '禁用' : '恢复' }}
                </el-button>
                <el-button v-perm="'customer:Customer:delete'" link type="danger" size="small" @click="remove(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <div class="flex flex-wrap items-center justify-between gap-2 border-t border-[#e2e8f0] px-3 py-2">
          <span class="font-mono text-[10px] text-[#94a3b8]">共 {{ page.total }} 条</span>
          <el-pagination
            v-model:current-page="page.pageNum"
            v-model:page-size="page.pageSize"
            :total="page.total"
            :page-sizes="[10, 20, 50, 100]"
            layout="sizes, prev, pager, next"
            size="small"
            @current-change="load"
            @size-change="search"
          />
        </div>
      </div>
    </div>

    <CustomerDetailDrawer v-model="detailOpen" :customer-id="detailCustomerId" @changed="load" />

    <el-dialog v-model="createOpen" title="新增会员" width="520px" append-to-body>
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-position="top" size="small">
        <div class="grid grid-cols-2 gap-2">
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="createForm.phone" maxlength="11" placeholder="11 位手机号" />
          </el-form-item>
          <el-form-item label="初始密码" prop="password">
            <el-input v-model="createForm.password" maxlength="32" placeholder="留空则不设密码" show-password />
          </el-form-item>
          <el-form-item label="昵称">
            <el-input v-model="createForm.nickname" maxlength="50" />
          </el-form-item>
          <el-form-item label="真实姓名">
            <el-input v-model="createForm.realName" maxlength="50" />
          </el-form-item>
          <el-form-item label="性别">
            <el-select v-model="createForm.gender" class="w-full">
              <el-option label="未知" :value="0" />
              <el-option label="男" :value="1" />
              <el-option label="女" :value="2" />
            </el-select>
          </el-form-item>
          <el-form-item label="会员等级">
            <el-select v-model="createForm.level" class="w-full">
              <el-option v-for="(label, index) in LEVEL_LABELS" :key="index" :label="label" :value="index + 1" />
            </el-select>
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="createForm.email" maxlength="100" />
          </el-form-item>
          <el-form-item label="初始积分">
            <el-input-number v-model="createForm.points" :min="0" class="w-full" controls-position="right" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <div class="flex justify-end gap-1">
          <el-button size="small" @click="createOpen = false">取消</el-button>
          <el-button size="small" type="primary" :loading="creating" @click="submitCreate">创建会员</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
