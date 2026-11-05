<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import {
  deleteCoupon,
  listCouponTemplates,
  pageCoupons,
  pageCustomers,
  refreshExpiredCoupons,
  revokeCoupon,
  useCoupon,
} from '@/api/customer'
import { formatDate, formatDateTime } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'
import FilterBar from '@/components/FilterBar.vue'
import StatusTag from '@/components/StatusTag.vue'
import RemotePicker from '@/components/RemotePicker.vue'

const loading = ref(false)
const rows = ref([])
const templates = ref([])
const filters = reactive({ customerId: null, templateId: '', code: '', status: '' })
const timeRange = ref([])
const page = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const refreshing = ref(false)

function fetchCustomers(keyword) {
  return pageCustomers({ keyword: keyword || undefined, pageNum: 1, pageSize: 20 })
}

async function loadTemplates() {
  try {
    templates.value = await listCouponTemplates()
  } catch {
    templates.value = []
  }
}

async function load() {
  loading.value = true
  try {
    const result = await pageCoupons({
      customerId: filters.customerId || undefined,
      templateId: filters.templateId === '' ? undefined : filters.templateId,
      code: filters.code || undefined,
      status: filters.status === '' ? undefined : filters.status,
      receiveTimeStart: timeRange.value?.[0] ? `${timeRange.value[0]} 00:00:00` : undefined,
      receiveTimeEnd: timeRange.value?.[1] ? `${timeRange.value[1]} 23:59:59` : undefined,
      pageNum: page.pageNum,
      pageSize: page.pageSize,
    })
    rows.value = result.records || []
    page.total = Number(result.total || 0)
  } catch (error) {
    ElMessage.error(error.message || '优惠券加载失败')
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
  Object.assign(filters, { customerId: null, templateId: '', code: '', status: '' })
  timeRange.value = []
  page.pageNum = 1
  load()
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
    await useCoupon(row.id, value ? Number(value) : undefined)
    ElMessage.success('券已核销')
    load()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || '核销失败')
    }
  }
}

async function revoke(row) {
  try {
    await ElMessageBox.confirm(`确定作废券 ${row.code} 吗？作废后不可恢复。`, '作废优惠券', {
      type: 'warning',
      confirmButtonText: '作废',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  try {
    await revokeCoupon(row.id)
    ElMessage.success('券已作废')
    load()
  } catch (error) {
    ElMessage.error(error.message || '作废失败')
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确定删除券 ${row.code} 吗？已使用的券是核销凭证，不允许删除。`, '删除优惠券', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  try {
    await deleteCoupon(row.id)
    ElMessage.success('券已删除')
    load()
  } catch (error) {
    ElMessage.error(error.message || '删除失败')
  }
}

async function refreshExpired() {
  refreshing.value = true
  try {
    const count = await refreshExpiredCoupons()
    ElMessage.success(`已刷新 ${count} 张过期券`)
    load()
  } catch (error) {
    ElMessage.error(error.message || '刷新失败')
  } finally {
    refreshing.value = false
  }
}

onMounted(() => {
  loadTemplates()
  load()
})
</script>

<template>
  <div class="pb-4">
    <PageHeader title="用户优惠券" description="券的有效期由模板推导，过期不会改库，靠刷新操作批量置为已过期">
      <template #actions>
        <el-button size="small" :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
        <el-button v-perm="'customer:Coupon:refreshExpired'" size="small" :loading="refreshing" @click="refreshExpired">
          刷新过期券
        </el-button>
      </template>
    </PageHeader>

    <div class="rg-page space-y-2">
      <FilterBar>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">会员</span>
          <div style="width: 190px">
            <RemotePicker
              v-model="filters.customerId"
              :fetcher="fetchCustomers"
              :formatter="(row) => `${row.nickname || '未命名'}（${row.phone}）`"
              placeholder="搜索昵称/手机号"
            />
          </div>
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">券模板</span>
          <el-select v-model="filters.templateId" size="small" filterable style="width: 170px">
            <el-option label="全部" value="" />
            <el-option v-for="item in templates" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">券码</span>
          <el-input v-model="filters.code" size="small" placeholder="券码" clearable style="width: 150px" class="font-mono" @keyup.enter="search" />
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">状态</span>
          <el-select v-model="filters.status" size="small" style="width: 100px">
            <el-option label="全部" value="" />
            <el-option label="未使用" :value="0" />
            <el-option label="已使用" :value="1" />
            <el-option label="已过期" :value="2" />
          </el-select>
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">领取时间</span>
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
          <el-table v-loading="loading" :data="rows" size="small" border stripe row-key="id" empty-text="没有匹配的优惠券">
            <el-table-column label="券" min-width="180">
              <template #default="{ row }">
                <p class="truncate text-[11px] font-medium text-[#1e293b]">{{ row.templateName }}</p>
                <p class="font-mono text-[10px] text-[#94a3b8]">{{ row.code }}</p>
              </template>
            </el-table-column>
            <el-table-column label="会员" min-width="140">
              <template #default="{ row }">
                <p class="truncate text-[11px] text-[#1e293b]">{{ row.customerNickname || `#${row.customerId}` }}</p>
                <p class="font-mono text-[10px] text-[#94a3b8]">{{ row.customerPhone }}</p>
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
                <span class="font-mono text-[11px]" :class="row.expired ? 'text-[#b91c1c]' : 'text-[#64748b]'">
                  {{ formatDate(row.expireTime) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="领取时间" width="134">
              <template #default="{ row }">
                <span class="font-mono text-[11px] text-[#64748b]">{{ formatDateTime(row.receiveTime) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="使用时间" width="134">
              <template #default="{ row }">
                <span class="font-mono text-[11px] text-[#64748b]">{{ formatDateTime(row.useTime) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="关联订单" width="140">
              <template #default="{ row }">
                <span class="font-mono text-[10px] text-[#94a3b8]">{{ row.orderNo || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <template v-if="Number(row.status) === 0">
                  <el-button v-perm="'customer:Coupon:use'" link type="primary" size="small" @click="use(row)">核销</el-button>
                  <el-button v-perm="'customer:Coupon:revoke'" link type="danger" size="small" @click="revoke(row)">作废</el-button>
                </template>
                <el-button v-if="Number(row.status) !== 1" v-perm="'customer:Coupon:delete'" link type="danger" size="small" @click="remove(row)">
                  删除
                </el-button>
                <span v-if="Number(row.status) === 1" class="text-[10px] text-[#94a3b8]">已核销</span>
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
  </div>
</template>
