<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  deleteCouponTemplate,
  grantCoupons,
  pageCouponTemplates,
  pageCustomers,
  setCouponTemplateStatus,
} from '@/api/customer'
import { formatDate, formatDateTime } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'
import FilterBar from '@/components/FilterBar.vue'
import StatusTag from '@/components/StatusTag.vue'
import CouponTemplateFormDrawer from './CouponTemplateFormDrawer.vue'

const loading = ref(false)
const rows = ref([])
const filters = reactive({ name: '', type: '', status: '' })
const page = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const formOpen = ref(false)
const formMode = ref('create')
const formTemplateId = ref(null)

const grantOpen = ref(false)
const granting = ref(false)
const grantTarget = ref(null)
const grantCustomerIds = ref([])
const customerOptions = ref([])
const customerLoading = ref(false)

async function load() {
  loading.value = true
  try {
    const result = await pageCouponTemplates({
      name: filters.name || undefined,
      type: filters.type === '' ? undefined : filters.type,
      status: filters.status === '' ? undefined : filters.status,
      pageNum: page.pageNum,
      pageSize: page.pageSize,
    })
    rows.value = result.records || []
    page.total = Number(result.total || 0)
  } catch (error) {
    ElMessage.error(error.message || '券模板加载失败')
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
  Object.assign(filters, { name: '', type: '', status: '' })
  page.pageNum = 1
  load()
}

function openCreate() {
  formMode.value = 'create'
  formTemplateId.value = null
  formOpen.value = true
}

function openEdit(row) {
  formMode.value = 'edit'
  formTemplateId.value = row.id
  formOpen.value = true
}

async function toggle(row) {
  try {
    await setCouponTemplateStatus(row.id, row.status === 1 ? 0 : 1)
    ElMessage.success(row.status === 1 ? '已停用' : '已启用')
    load()
  } catch (error) {
    ElMessage.error(error.message || '状态更新失败')
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确定删除券模板「${row.name}」吗？已发过券的模板只能停用。`, '删除模板', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  try {
    await deleteCouponTemplate(row.id)
    ElMessage.success('模板已删除')
    load()
  } catch (error) {
    ElMessage.error(error.message || '删除失败')
  }
}

async function searchCustomers(keyword) {
  customerLoading.value = true
  try {
    const result = await pageCustomers({ keyword: keyword || undefined, pageNum: 1, pageSize: 20 })
    customerOptions.value = (result.records || []).map((row) => ({
      value: row.id,
      label: `${row.nickname || '未命名'}（${row.phone}）`,
    }))
  } catch {
    customerOptions.value = []
  } finally {
    customerLoading.value = false
  }
}

function openGrant(row) {
  grantTarget.value = row
  grantCustomerIds.value = []
  customerOptions.value = []
  grantOpen.value = true
  searchCustomers('')
}

async function submitGrant() {
  if (!grantCustomerIds.value.length) {
    ElMessage.warning('请选择要发放的会员')
    return
  }
  if (grantCustomerIds.value.length > 200) {
    ElMessage.warning('单次最多发放 200 人')
    return
  }
  granting.value = true
  try {
    const count = await grantCoupons({
      templateId: grantTarget.value.id,
      customerIds: grantCustomerIds.value,
    })
    ElMessage.success(`已发放 ${count} 张券`)
    grantOpen.value = false
    load()
  } catch (error) {
    ElMessage.error(error.message || '发券失败')
  } finally {
    granting.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="pb-4">
    <PageHeader title="优惠券模板" description="满减 / 折扣两类；固定区间或领取后按天数计算有效期">
      <template #actions>
        <el-button size="small" :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
        <el-button v-perm="'customer:CouponTemplate:create'" size="small" type="primary" :icon="Plus" @click="openCreate">
          新增模板
        </el-button>
      </template>
    </PageHeader>

    <div class="rg-page space-y-2">
      <FilterBar>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">券名称</span>
          <el-input v-model="filters.name" size="small" placeholder="券名称" clearable style="width: 160px" @keyup.enter="search" />
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">类型</span>
          <el-select v-model="filters.type" size="small" style="width: 100px">
            <el-option label="全部" value="" />
            <el-option label="满减券" :value="1" />
            <el-option label="折扣券" :value="2" />
          </el-select>
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">状态</span>
          <el-select v-model="filters.status" size="small" style="width: 92px">
            <el-option label="全部" value="" />
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </label>
        <template #actions>
          <el-button size="small" type="primary" :icon="Search" @click="search">查询</el-button>
          <el-button size="small" @click="resetFilters">重置</el-button>
        </template>
      </FilterBar>

      <div class="rg-card min-w-0">
        <div class="rg-scroll-x">
          <el-table v-loading="loading" :data="rows" size="small" border stripe row-key="id" empty-text="没有匹配的券模板">
            <el-table-column label="券名称" min-width="170">
              <template #default="{ row }">
                <p class="truncate text-[11px] font-medium text-[#1e293b]">{{ row.name }}</p>
                <p class="text-[10px] text-[#94a3b8]">{{ row.typeDesc }} · {{ row.validDesc }}</p>
              </template>
            </el-table-column>
            <el-table-column label="优惠" width="110">
              <template #default="{ row }">
                <span class="text-[11px] text-[#a8435f]">{{ row.benefitDesc }}</span>
              </template>
            </el-table-column>
            <el-table-column label="发放 / 总量" width="110">
              <template #default="{ row }">
                <span class="font-mono text-[11px]">{{ row.receivedCount }} / {{ row.totalCount }}</span>
              </template>
            </el-table-column>
            <el-table-column label="剩余" width="80">
              <template #default="{ row }">
                <span class="font-mono text-[11px]" :class="row.remainCount > 0 ? 'text-[#15803d]' : 'text-[#b91c1c]'">
                  {{ row.remainCount }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="有效期" min-width="180">
              <template #default="{ row }">
                <span v-if="row.validType === 1" class="font-mono text-[10px] text-[#64748b]">
                  {{ formatDateTime(row.startTime) }} ~ {{ formatDateTime(row.endTime) }}
                </span>
                <span v-else class="text-[10px] text-[#64748b]">领取后 {{ row.validDays }} 天</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="76">
              <template #default="{ row }">
                <StatusTag :value="row.status" :labels="['停用', '启用']" :tone="row.status === 1 ? 'success' : 'muted'" />
              </template>
            </el-table-column>
            <el-table-column label="创建时间" width="100">
              <template #default="{ row }">
                <span class="font-mono text-[11px] text-[#64748b]">{{ formatDate(row.createTime) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="210" fixed="right">
              <template #default="{ row }">
                <el-button v-perm="'customer:CouponTemplate:grant'" link type="primary" size="small" @click="openGrant(row)">发券</el-button>
                <el-button v-perm="'customer:CouponTemplate:update'" link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
                <el-button v-perm="'customer:CouponTemplate:updateStatus'" link size="small" @click="toggle(row)">
                  {{ row.status === 1 ? '停用' : '启用' }}
                </el-button>
                <el-button v-perm="'customer:CouponTemplate:delete'" link type="danger" size="small" @click="remove(row)">删除</el-button>
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

    <CouponTemplateFormDrawer v-model="formOpen" :mode="formMode" :template-id="formTemplateId" @saved="load" />

    <el-dialog v-model="grantOpen" title="批量发券" width="520px" append-to-body>
      <div v-if="grantTarget" class="mb-2 rounded bg-[#f8fafc] p-2 text-[11px] text-[#475569]">
        <p>{{ grantTarget.name }} · {{ grantTarget.benefitDesc }}</p>
        <p class="mt-0.5 text-[10px] text-[#94a3b8]">
          剩余可发 {{ grantTarget.remainCount }} 张，单次最多选择 200 人
        </p>
      </div>
      <el-select
        v-model="grantCustomerIds"
        multiple
        filterable
        remote
        :remote-method="searchCustomers"
        :loading="customerLoading"
        placeholder="搜索昵称/手机号，可多选"
        class="w-full"
      >
        <el-option v-for="item in customerOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <p class="mt-1 text-[10px] text-[#94a3b8]">已选 {{ grantCustomerIds.length }} 人。整批事务发放，任意一人失败不会产生半批券。</p>
      <template #footer>
        <div class="flex justify-end gap-1">
          <el-button size="small" @click="grantOpen = false">取消</el-button>
          <el-button size="small" type="primary" :loading="granting" @click="submitGrant">确认发放</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
