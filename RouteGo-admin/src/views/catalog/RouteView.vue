<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { deleteRoute, getCategoryTree, pageRoutes, setRouteStatus } from '@/api/catalog'
import { formatDate, formatMoney, resolveFileUrl } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'
import FilterBar from '@/components/FilterBar.vue'
import StatusTag from '@/components/StatusTag.vue'
import RouteFormDrawer from './RouteFormDrawer.vue'
import RouteDetailDrawer from './RouteDetailDrawer.vue'

const loading = ref(false)
const rows = ref([])
const categories = ref([])
const filters = reactive({
  title: '',
  routeNo: '',
  destination: '',
  departureCity: '',
  categoryId: '',
  status: '',
})
const page = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const formOpen = ref(false)
const formMode = ref('create')
const formRouteId = ref(null)
const detailOpen = ref(false)
const detailRouteId = ref(null)

/** 分类树拍平成下拉项，前缀缩进表示层级 */
function flattenCategories(nodes, depth = 0, acc = []) {
  nodes.forEach((node) => {
    acc.push({ id: node.id, label: `${'　'.repeat(depth)}${node.name}` })
    if (node.children?.length) flattenCategories(node.children, depth + 1, acc)
  })
  return acc
}

async function loadCategories() {
  try {
    categories.value = flattenCategories(await getCategoryTree())
  } catch {
    categories.value = []
  }
}

async function load() {
  loading.value = true
  try {
    const result = await pageRoutes({
      title: filters.title || undefined,
      routeNo: filters.routeNo || undefined,
      destination: filters.destination || undefined,
      departureCity: filters.departureCity || undefined,
      categoryId: filters.categoryId === '' ? undefined : filters.categoryId,
      status: filters.status === '' ? undefined : filters.status,
      pageNum: page.pageNum,
      pageSize: page.pageSize,
    })
    rows.value = result.records || []
    page.total = Number(result.total || 0)
  } catch (error) {
    ElMessage.error(error.message || '线路加载失败')
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
  Object.assign(filters, {
    title: '',
    routeNo: '',
    destination: '',
    departureCity: '',
    categoryId: '',
    status: '',
  })
  page.pageNum = 1
  load()
}

function openCreate() {
  formMode.value = 'create'
  formRouteId.value = null
  formOpen.value = true
}

function openEdit(row) {
  formMode.value = 'edit'
  formRouteId.value = row.id
  formOpen.value = true
}

function openDetail(row) {
  detailRouteId.value = row.id
  detailOpen.value = true
}

async function toggleStatus(row) {
  const next = row.status === 1 ? 0 : 1
  try {
    await setRouteStatus(row.id, next)
    ElMessage.success(next === 1 ? '已上架' : '已下架')
    load()
  } catch (error) {
    ElMessage.error(error.message || '状态更新失败')
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除线路「${row.title}」吗？已产生订单的线路建议改为下架。`,
      '删除线路',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await deleteRoute(row.id)
    ElMessage.success('线路已删除')
    load()
  } catch (error) {
    ElMessage.error(error.message || '删除失败')
  }
}

onMounted(() => {
  loadCategories()
  load()
})
</script>

<template>
  <div class="pb-4">
    <PageHeader title="线路列表" description="线路编号自动生成，上下架控制小程序端展示">
      <template #actions>
        <el-button size="small" :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
        <el-button v-perm="'catalog:Route:create'" size="small" type="primary" :icon="Plus" @click="openCreate">
          新增线路
        </el-button>
      </template>
    </PageHeader>

    <div class="rg-page space-y-2">
      <FilterBar>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">标题</span>
          <el-input v-model="filters.title" size="small" placeholder="线路标题" clearable style="width: 150px" @keyup.enter="search" />
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">线路编号</span>
          <el-input v-model="filters.routeNo" size="small" placeholder="RG2026..." clearable style="width: 140px" class="font-mono" @keyup.enter="search" />
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">目的地</span>
          <el-input v-model="filters.destination" size="small" placeholder="云南" clearable style="width: 110px" @keyup.enter="search" />
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">出发城市</span>
          <el-input v-model="filters.departureCity" size="small" placeholder="北京" clearable style="width: 110px" @keyup.enter="search" />
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">分类</span>
          <el-select v-model="filters.categoryId" size="small" filterable style="width: 150px">
            <el-option label="全部" value="" />
            <el-option v-for="item in categories" :key="item.id" :label="item.label" :value="item.id" />
          </el-select>
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">状态</span>
          <el-select v-model="filters.status" size="small" style="width: 92px">
            <el-option label="全部" value="" />
            <el-option label="上架" :value="1" />
            <el-option label="下架" :value="0" />
          </el-select>
        </label>
        <template #actions>
          <el-button size="small" type="primary" :icon="Search" @click="search">查询</el-button>
          <el-button size="small" @click="resetFilters">重置</el-button>
        </template>
      </FilterBar>

      <div class="rg-card min-w-0">
        <div class="rg-scroll-x">
          <el-table v-loading="loading" :data="rows" size="small" border stripe row-key="id" empty-text="没有匹配的线路">
            <el-table-column label="线路" min-width="260">
              <template #default="{ row }">
                <div class="flex items-center gap-2">
                  <el-image
                    v-if="row.coverImage"
                    :src="resolveFileUrl(row.coverImage)"
                    fit="cover"
                    class="h-9 w-12 shrink-0 rounded"
                  >
                    <template #error>
                      <div class="flex h-9 w-12 items-center justify-center rounded bg-[#f1f5f9] text-[10px] text-[#94a3b8]">无图</div>
                    </template>
                  </el-image>
                  <div class="min-w-0">
                    <p class="truncate text-[11px] font-medium text-[#1e293b]">{{ row.title }}</p>
                    <p class="truncate font-mono text-[10px] text-[#94a3b8]">{{ row.routeNo }}</p>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="分类" width="100">
              <template #default="{ row }">
                <span class="truncate text-[11px] text-[#64748b]">{{ row.categoryName || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="目的地" width="120">
              <template #default="{ row }">
                <span class="block truncate text-[11px] text-[#1e293b]">{{ row.destination }}</span>
                <span class="block truncate text-[10px] text-[#94a3b8]">{{ row.departureCity }} 出发</span>
              </template>
            </el-table-column>
            <el-table-column label="行程" width="72">
              <template #default="{ row }">
                <span class="font-mono text-[11px] text-[#64748b]">{{ row.days }}天{{ row.nights || 0 }}晚</span>
              </template>
            </el-table-column>
            <el-table-column label="起价" width="94">
              <template #default="{ row }">
                <span class="font-mono text-[11px] font-medium text-[#a8435f]">{{ formatMoney(row.minPrice) }}</span>
                <span v-if="row.originalPrice" class="ml-1 font-mono text-[10px] text-[#94a3b8] line-through">
                  {{ formatMoney(row.originalPrice) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="销量" width="60">
              <template #default="{ row }">
                <span class="font-mono text-[11px] text-[#64748b]">{{ row.sales }}</span>
              </template>
            </el-table-column>
            <el-table-column label="评分" width="60">
              <template #default="{ row }">
                <span class="font-mono text-[11px] text-[#b45309]">{{ Number(row.rating || 0).toFixed(1) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="70">
              <template #default="{ row }">
                <StatusTag :value="row.status" :labels="['下架', '上架']" :tone="row.status === 1 ? 'success' : 'muted'" />
              </template>
            </el-table-column>
            <el-table-column label="创建时间" width="100">
              <template #default="{ row }">
                <span class="font-mono text-[11px] text-[#64748b]">{{ formatDate(row.createTime) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="186" fixed="right">
              <template #default="{ row }">
                <div class="flex items-center gap-0.5">
                  <el-button v-perm="'catalog:Route:detail'" link type="primary" size="small" @click="openDetail(row)">详情</el-button>
                  <el-button v-perm="'catalog:Route:update'" link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
                  <el-button v-perm="'catalog:Route:updateStatus'" link size="small" @click="toggleStatus(row)">
                    {{ row.status === 1 ? '下架' : '上架' }}
                  </el-button>
                  <el-button v-perm="'catalog:Route:delete'" link type="danger" size="small" @click="remove(row)">删除</el-button>
                </div>
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

    <RouteFormDrawer v-model="formOpen" :mode="formMode" :route-id="formRouteId" @saved="load" />
    <RouteDetailDrawer v-model="detailOpen" :route-id="detailRouteId" @changed="load" />
  </div>
</template>
