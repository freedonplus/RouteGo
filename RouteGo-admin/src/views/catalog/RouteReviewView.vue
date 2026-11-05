<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { deleteReview, pageReviews, pageRoutes, replyReview, setReviewStatus } from '@/api/catalog'
import { formatDateTime } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'
import FilterBar from '@/components/FilterBar.vue'
import StatusTag from '@/components/StatusTag.vue'
import RemotePicker from '@/components/RemotePicker.vue'

const loading = ref(false)
const rows = ref([])
const filters = reactive({ routeId: null, rating: '', status: '' })
const page = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const replyOpen = ref(false)
const replySaving = ref(false)
const replyTarget = ref(null)
const replyText = ref('')

function fetchRoutes(keyword) {
  return pageRoutes({ title: keyword || undefined, pageNum: 1, pageSize: 20 })
}

/** 图片字段存的是 JSON 数组文本，解析失败就当作没有图片 */
function imageList(row) {
  if (!row.images) return []
  try {
    const parsed = JSON.parse(row.images)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return String(row.images)
      .split(',')
      .map((item) => item.trim())
      .filter(Boolean)
  }
}

async function load() {
  loading.value = true
  try {
    const result = await pageReviews({
      routeId: filters.routeId || undefined,
      rating: filters.rating === '' ? undefined : filters.rating,
      status: filters.status === '' ? undefined : filters.status,
      pageNum: page.pageNum,
      pageSize: page.pageSize,
    })
    rows.value = result.records || []
    page.total = Number(result.total || 0)
  } catch (error) {
    ElMessage.error(error.message || '评价加载失败')
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
  Object.assign(filters, { routeId: null, rating: '', status: '' })
  page.pageNum = 1
  load()
}

function openReply(row) {
  replyTarget.value = row
  replyText.value = row.reply || ''
  replyOpen.value = true
}

async function submitReply() {
  if (!replyText.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  replySaving.value = true
  try {
    await replyReview(replyTarget.value.id, replyText.value.trim())
    ElMessage.success('回复已保存')
    replyOpen.value = false
    load()
  } catch (error) {
    ElMessage.error(error.message || '回复失败')
  } finally {
    replySaving.value = false
  }
}

async function toggle(row) {
  try {
    await setReviewStatus(row.id, row.status === 1 ? 0 : 1)
    ElMessage.success(row.status === 1 ? '已隐藏' : '已显示')
    load()
  } catch (error) {
    ElMessage.error(error.message || '状态更新失败')
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm('确定删除这条评价吗？删除后线路评分会重新计算。', '删除评价', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  try {
    await deleteReview(row.id)
    ElMessage.success('评价已删除')
    load()
  } catch (error) {
    ElMessage.error(error.message || '删除失败')
  }
}

onMounted(load)
</script>

<template>
  <div class="pb-4">
    <PageHeader title="线路评价" description="评分 1–5 星，隐藏后小程序端不再展示，回复会写入回复时间">
      <template #actions>
        <el-button size="small" :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
      </template>
    </PageHeader>

    <div class="rg-page space-y-2">
      <FilterBar>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">线路</span>
          <div style="width: 220px">
            <RemotePicker
              v-model="filters.routeId"
              :fetcher="fetchRoutes"
              :formatter="(row) => `${row.title}（${row.routeNo}）`"
              placeholder="搜索线路标题"
            />
          </div>
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">评分</span>
          <el-select v-model="filters.rating" size="small" style="width: 92px">
            <el-option label="全部" value="" />
            <el-option v-for="n in 5" :key="n" :label="`${n} 星`" :value="n" />
          </el-select>
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">状态</span>
          <el-select v-model="filters.status" size="small" style="width: 92px">
            <el-option label="全部" value="" />
            <el-option label="显示" :value="1" />
            <el-option label="隐藏" :value="0" />
          </el-select>
        </label>
        <template #actions>
          <el-button size="small" type="primary" :icon="Search" @click="search">查询</el-button>
          <el-button size="small" @click="resetFilters">重置</el-button>
        </template>
      </FilterBar>

      <div class="rg-card min-w-0">
        <div class="rg-scroll-x">
          <el-table v-loading="loading" :data="rows" size="small" border stripe row-key="id" empty-text="没有匹配的评价">
            <el-table-column label="线路" min-width="160">
              <template #default="{ row }">
                <span class="truncate text-[11px] text-[#1e293b]">{{ row.routeTitle || `#${row.routeId}` }}</span>
              </template>
            </el-table-column>
            <el-table-column label="评分" width="90">
              <template #default="{ row }">
                <el-rate :model-value="row.rating" disabled size="small" />
              </template>
            </el-table-column>
            <el-table-column label="评价内容" min-width="280">
              <template #default="{ row }">
                <p class="line-clamp-2 text-[11px] text-[#1e293b]">{{ row.content }}</p>
                <div v-if="imageList(row).length" class="mt-1 flex gap-1">
                  <el-image
                    v-for="(url, index) in imageList(row).slice(0, 3)"
                    :key="index"
                    :src="url"
                    fit="cover"
                    class="h-8 w-8 rounded"
                    :preview-src-list="imageList(row)"
                    preview-teleported
                  />
                </div>
              </template>
            </el-table-column>
            <el-table-column label="商家回复" min-width="200">
              <template #default="{ row }">
                <p v-if="row.reply" class="line-clamp-2 text-[11px] text-[#64748b]">{{ row.reply }}</p>
                <span v-else class="text-[11px] text-[#cbd5e1]">未回复</span>
                <p v-if="row.replyTime" class="mt-0.5 font-mono text-[10px] text-[#94a3b8]">
                  {{ formatDateTime(row.replyTime) }}
                </p>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="76">
              <template #default="{ row }">
                <StatusTag :value="row.status" :labels="['隐藏', '显示']" :tone="row.status === 1 ? 'success' : 'muted'" />
              </template>
            </el-table-column>
            <el-table-column label="评价时间" width="134">
              <template #default="{ row }">
                <span class="font-mono text-[11px] text-[#64748b]">{{ formatDateTime(row.createTime) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="170" fixed="right">
              <template #default="{ row }">
                <el-button v-perm="'catalog:RouteReview:reply'" link type="primary" size="small" @click="openReply(row)">
                  {{ row.reply ? '改回复' : '回复' }}
                </el-button>
                <el-button v-perm="'catalog:RouteReview:updateStatus'" link size="small" @click="toggle(row)">
                  {{ row.status === 1 ? '隐藏' : '显示' }}
                </el-button>
                <el-button v-perm="'catalog:RouteReview:delete'" link type="danger" size="small" @click="remove(row)">删除</el-button>
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

    <el-dialog v-model="replyOpen" title="回复评价" width="480px" append-to-body>
      <p class="mb-2 rounded bg-[#f8fafc] p-2 text-[11px] text-[#475569]">
        {{ replyTarget?.content }}
      </p>
      <el-input v-model="replyText" type="textarea" :rows="4" maxlength="2000" show-word-limit placeholder="输入回复内容" />
      <template #footer>
        <div class="flex justify-end gap-1">
          <el-button size="small" @click="replyOpen = false">取消</el-button>
          <el-button size="small" type="primary" :loading="replySaving" @click="submitReply">保存回复</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
