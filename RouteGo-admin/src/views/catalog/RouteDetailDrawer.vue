<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import {
  createDeparture,
  createItinerary,
  createRouteImage,
  deleteDeparture,
  deleteItinerary,
  deleteRouteImage,
  getRoute,
  listDepartures,
  listTags,
  setDepartureStatus,
  updateDeparture,
  updateItinerary,
  assignRouteTags,
} from '@/api/catalog'
import { formatMoney, resolveFileUrl } from '@/utils/format'
import StatusTag from '@/components/StatusTag.vue'
import ImageUpload from '@/components/ImageUpload.vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  routeId: { type: [Number, String], default: null },
})
const emit = defineEmits(['update:modelValue', 'changed'])

const loading = ref(false)
const activeTab = ref('base')
const detail = ref(null)
const departures = ref([])
const allTags = ref([])
const checkedTags = ref([])
const savingTags = ref(false)

/* 行程弹窗 */
const itineraryOpen = ref(false)
const itineraryEditingId = ref(null)
const itinerarySaving = ref(false)
const itineraryForm = reactive({
  dayNo: 1,
  title: '',
  description: '',
  meals: '',
  hotel: '',
  transport: '',
  address: '',
})

/* 图片弹窗 */
const imageOpen = ref(false)
const imageSaving = ref(false)
const imageForm = reactive({ imageUrl: '', sort: 0 })

/* 班期弹窗 */
const departureOpen = ref(false)
const departureEditingId = ref(null)
const departureSaving = ref(false)
const departureForm = reactive({
  departDate: '',
  adultPrice: 0,
  childPrice: null,
  stock: 10,
  status: 1,
})

const title = computed(() => detail.value?.title || '线路详情')

async function loadDetail() {
  if (!props.routeId) return
  loading.value = true
  try {
    const data = await getRoute(props.routeId)
    detail.value = data
    checkedTags.value = (data.tags || []).map((tag) => tag.id)
  } catch (error) {
    ElMessage.error(error.message || '线路详情加载失败')
    detail.value = null
  } finally {
    loading.value = false
  }
}

async function loadDepartures() {
  if (!props.routeId) return
  try {
    departures.value = await listDepartures(props.routeId)
  } catch (error) {
    ElMessage.error(error.message || '班期加载失败')
    departures.value = []
  }
}

async function loadTags() {
  try {
    allTags.value = (await listTags()).filter((tag) => tag.status === 1)
  } catch {
    allTags.value = []
  }
}

watch(
  () => props.modelValue,
  async (open) => {
    if (!open) return
    activeTab.value = 'base'
    detail.value = null
    departures.value = []
    await Promise.all([loadDetail(), loadDepartures(), loadTags()])
  }
)

async function saveTags() {
  savingTags.value = true
  try {
    await assignRouteTags(props.routeId, checkedTags.value)
    ElMessage.success('标签已保存')
    await loadDetail()
    emit('changed')
  } catch (error) {
    ElMessage.error(error.message || '标签保存失败')
  } finally {
    savingTags.value = false
  }
}

function openItinerary(row) {
  itineraryEditingId.value = row?.id || null
  Object.assign(itineraryForm, {
    dayNo: row?.dayNo ?? (detail.value?.itineraries?.length || 0) + 1,
    title: row?.title || '',
    description: row?.description || '',
    meals: row?.meals || '',
    hotel: row?.hotel || '',
    transport: row?.transport || '',
    address: row?.address || '',
  })
  itineraryOpen.value = true
}

async function submitItinerary() {
  if (!itineraryForm.title.trim()) {
    ElMessage.warning('请填写当日主题')
    return
  }
  itinerarySaving.value = true
  try {
    const payload = { ...itineraryForm, routeId: props.routeId }
    if (itineraryEditingId.value) {
      await updateItinerary(itineraryEditingId.value, payload)
      ElMessage.success('行程已更新')
    } else {
      await createItinerary(payload)
      ElMessage.success('行程已添加')
    }
    itineraryOpen.value = false
    await loadDetail()
    emit('changed')
  } catch (error) {
    ElMessage.error(error.message || '行程保存失败')
  } finally {
    itinerarySaving.value = false
  }
}

async function removeItinerary(row) {
  try {
    await ElMessageBox.confirm(`确定删除第 ${row.dayNo} 天「${row.title}」吗？`, '删除行程', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  try {
    await deleteItinerary(row.id)
    ElMessage.success('行程已删除')
    await loadDetail()
  } catch (error) {
    ElMessage.error(error.message || '删除失败')
  }
}

function openImage() {
  Object.assign(imageForm, {
    imageUrl: '',
    sort: (detail.value?.images?.length || 0) + 1,
  })
  imageOpen.value = true
}

async function submitImage() {
  if (!imageForm.imageUrl.trim()) {
    ElMessage.warning('请先上传图片')
    return
  }
  imageSaving.value = true
  try {
    await createRouteImage({ ...imageForm, routeId: props.routeId })
    ElMessage.success('图片已添加')
    imageOpen.value = false
    await loadDetail()
  } catch (error) {
    ElMessage.error(error.message || '图片添加失败')
  } finally {
    imageSaving.value = false
  }
}

async function removeImage(row) {
  try {
    await ElMessageBox.confirm('确定删除这张图片吗？', '删除图片', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  try {
    await deleteRouteImage(row.id)
    ElMessage.success('图片已删除')
    await loadDetail()
  } catch (error) {
    ElMessage.error(error.message || '删除失败')
  }
}

function openDeparture(row) {
  departureEditingId.value = row?.id || null
  Object.assign(departureForm, {
    departDate: row?.departDate || '',
    adultPrice: row?.adultPrice ?? detail.value?.minPrice ?? 0,
    childPrice: row?.childPrice ?? null,
    stock: row?.stock ?? 10,
    status: row?.status ?? 1,
  })
  departureOpen.value = true
}

async function submitDeparture() {
  if (!departureForm.departDate) {
    ElMessage.warning('请选择出发日期')
    return
  }
  departureSaving.value = true
  try {
    const payload = { ...departureForm, routeId: props.routeId }
    if (departureEditingId.value) {
      await updateDeparture(departureEditingId.value, payload)
      ElMessage.success('班期已更新')
    } else {
      await createDeparture(payload)
      ElMessage.success('班期已添加')
    }
    departureOpen.value = false
    await loadDepartures()
    emit('changed')
  } catch (error) {
    ElMessage.error(error.message || '班期保存失败')
  } finally {
    departureSaving.value = false
  }
}

async function toggleDeparture(row) {
  try {
    await setDepartureStatus(row.id, row.status === 1 ? 0 : 1)
    ElMessage.success(row.status === 1 ? '已停售' : '已恢复可售')
    await loadDepartures()
  } catch (error) {
    ElMessage.error(error.message || '状态更新失败')
  }
}

async function removeDeparture(row) {
  try {
    await ElMessageBox.confirm(`确定删除 ${row.departDate} 的班期吗？`, '删除班期', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  try {
    await deleteDeparture(row.id)
    ElMessage.success('班期已删除')
    await loadDepartures()
  } catch (error) {
    ElMessage.error(error.message || '删除失败')
  }
}
</script>

<template>
  <el-drawer
    :model-value="modelValue"
    :title="title"
    size="760px"
    append-to-body
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div v-loading="loading">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="概览" name="base">
          <el-descriptions v-if="detail" :column="2" size="small" border>
            <el-descriptions-item label="线路编号">
              <span class="font-mono">{{ detail.routeNo }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="分类">{{ detail.categoryName || '—' }}</el-descriptions-item>
            <el-descriptions-item label="目的地">{{ detail.destination }}</el-descriptions-item>
            <el-descriptions-item label="出发城市">{{ detail.departureCity }}</el-descriptions-item>
            <el-descriptions-item label="行程">{{ detail.days }} 天 {{ detail.nights || 0 }} 晚</el-descriptions-item>
            <el-descriptions-item label="起价">{{ formatMoney(detail.minPrice) }}</el-descriptions-item>
            <el-descriptions-item label="销量">{{ detail.sales }}</el-descriptions-item>
            <el-descriptions-item label="评分">{{ Number(detail.rating || 0).toFixed(2) }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <StatusTag :value="detail.status" :labels="['下架', '上架']" :tone="detail.status === 1 ? 'success' : 'muted'" />
            </el-descriptions-item>
            <el-descriptions-item label="集合地址">{{ detail.address || '—' }}</el-descriptions-item>
            <el-descriptions-item label="行程亮点" :span="2">
              <span class="whitespace-pre-line">{{ detail.highlights || '—' }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="费用包含" :span="2">
              <span class="whitespace-pre-line">{{ detail.includeItems || '—' }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="费用不含" :span="2">
              <span class="whitespace-pre-line">{{ detail.excludeItems || '—' }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="预订须知" :span="2">
              <span class="whitespace-pre-line">{{ detail.bookingNotice || '—' }}</span>
            </el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>

        <el-tab-pane :label="`行程（${detail?.itineraries?.length || 0}）`" name="itinerary">
          <div class="mb-2 flex justify-end">
            <el-button v-perm="'catalog:RouteItinerary:create'" size="small" type="primary" :icon="Plus" @click="openItinerary(null)">
              添加一天
            </el-button>
          </div>
          <el-empty v-if="!detail?.itineraries?.length" description="还没有行程，按天补充即可" :image-size="60" />
          <div v-else class="space-y-2">
            <div v-for="item in detail.itineraries" :key="item.id" class="rounded border border-[#e2e8f0] p-2">
              <div class="flex items-start justify-between gap-2">
                <div class="min-w-0">
                  <p class="text-[11px] font-medium text-[#1e293b]">
                    <span class="mr-1 rounded bg-[#fdf1f5] px-1 font-mono text-[10px] text-[#a8435f]">D{{ item.dayNo }}</span>
                    {{ item.title }}
                  </p>
                  <p v-if="item.description" class="mt-1 whitespace-pre-line text-[11px] text-[#64748b]">{{ item.description }}</p>
                  <p class="mt-1 flex flex-wrap gap-x-3 gap-y-0.5 text-[10px] text-[#94a3b8]">
                    <span v-if="item.transport">交通：{{ item.transport }}</span>
                    <span v-if="item.meals">用餐：{{ item.meals }}</span>
                    <span v-if="item.hotel">住宿：{{ item.hotel }}</span>
                    <span v-if="item.address">地址：{{ item.address }}</span>
                  </p>
                </div>
                <div class="flex shrink-0 gap-0.5">
                  <el-button v-perm="'catalog:RouteItinerary:update'" link type="primary" size="small" @click="openItinerary(item)">编辑</el-button>
                  <el-button v-perm="'catalog:RouteItinerary:delete'" link type="danger" size="small" @click="removeItinerary(item)">删除</el-button>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane :label="`图片（${detail?.images?.length || 0}）`" name="image">
          <div class="mb-2 flex justify-end">
            <el-button v-perm="'catalog:RouteImage:create'" size="small" type="primary" :icon="Plus" @click="openImage">添加图片</el-button>
          </div>
          <el-empty v-if="!detail?.images?.length" description="还没有配图" :image-size="60" />
          <div v-else class="grid grid-cols-3 gap-2">
            <div v-for="item in detail.images" :key="item.id" class="overflow-hidden rounded border border-[#e2e8f0]">
              <el-image
                :src="resolveFileUrl(item.imageUrl)"
                fit="cover"
                class="h-24 w-full"
                :preview-src-list="[resolveFileUrl(item.imageUrl)]"
                preview-teleported
              />
              <div class="flex items-center justify-between px-1.5 py-1">
                <span class="font-mono text-[10px] text-[#94a3b8]">排序 {{ item.sort ?? 0 }}</span>
                <el-button v-perm="'catalog:RouteImage:delete'" link type="danger" size="small" @click="removeImage(item)">删除</el-button>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="标签" name="tag">
          <p class="mb-2 text-[11px] text-[#64748b]">勾选后保存，标签用于小程序端筛选与首页聚合。</p>
          <el-checkbox-group v-model="checkedTags" class="flex flex-wrap gap-2">
            <el-checkbox v-for="tag in allTags" :key="tag.id" :value="tag.id" border size="small">
              <span :style="{ color: tag.color || undefined }">{{ tag.tagName }}</span>
            </el-checkbox>
          </el-checkbox-group>
          <el-empty v-if="!allTags.length" description="没有可用标签，请先在标签管理中创建" :image-size="60" />
          <div class="mt-3">
            <el-button v-perm="'catalog:RouteTagRel:assign'" size="small" type="primary" :loading="savingTags" @click="saveTags">
              保存标签
            </el-button>
          </div>
        </el-tab-pane>

        <el-tab-pane :label="`班期（${departures.length}）`" name="departure">
          <div class="mb-2 flex justify-end">
            <el-button v-perm="'catalog:RouteDeparture:create'" size="small" type="primary" :icon="Plus" @click="openDeparture(null)">
              添加班期
            </el-button>
          </div>
          <el-table :data="departures" size="small" border stripe empty-text="还没有班期">
            <el-table-column label="出发日期" width="110">
              <template #default="{ row }">
                <span class="font-mono text-[11px]">{{ row.departDate }}</span>
              </template>
            </el-table-column>
            <el-table-column label="成人价" width="100">
              <template #default="{ row }">
                <span class="font-mono text-[11px] text-[#a8435f]">{{ formatMoney(row.adultPrice) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="儿童价" width="100">
              <template #default="{ row }">
                <span class="font-mono text-[11px] text-[#64748b]">{{ formatMoney(row.childPrice) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="库存 / 已售" width="110">
              <template #default="{ row }">
                <span class="font-mono text-[11px]">{{ row.remaining }} / {{ row.sold }}</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="76">
              <template #default="{ row }">
                <StatusTag :value="row.status" :labels="['停售', '可售']" :tone="row.status === 1 ? 'success' : 'muted'" />
              </template>
            </el-table-column>
            <el-table-column label="操作" min-width="150">
              <template #default="{ row }">
                <el-button v-perm="'catalog:RouteDeparture:update'" link type="primary" size="small" @click="openDeparture(row)">编辑</el-button>
                <el-button v-perm="'catalog:RouteDeparture:updateStatus'" link size="small" @click="toggleDeparture(row)">
                  {{ row.status === 1 ? '停售' : '恢复' }}
                </el-button>
                <el-button v-perm="'catalog:RouteDeparture:delete'" link type="danger" size="small" @click="removeDeparture(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <p class="mt-2 text-[10px] leading-4 text-[#94a3b8]">
            库存不能小于已售；有成交的班期只能停售，不能删除。
          </p>
        </el-tab-pane>
      </el-tabs>
    </div>

    <el-dialog v-model="itineraryOpen" :title="itineraryEditingId ? '编辑行程' : '添加行程'" width="520px" append-to-body>
      <el-form :model="itineraryForm" label-position="top" size="small">
        <div class="grid grid-cols-3 gap-2">
          <el-form-item label="第几天">
            <el-input-number v-model="itineraryForm.dayNo" :min="1" :max="365" class="w-full" controls-position="right" />
          </el-form-item>
          <el-form-item label="当日主题" class="col-span-2">
            <el-input v-model="itineraryForm.title" placeholder="如：抵达丽江 · 束河古镇" maxlength="100" />
          </el-form-item>
        </div>
        <el-form-item label="行程描述">
          <el-input v-model="itineraryForm.description" type="textarea" :rows="4" placeholder="按时间线描述当天安排" />
        </el-form-item>
        <div class="grid grid-cols-3 gap-2">
          <el-form-item label="交通">
            <el-input v-model="itineraryForm.transport" maxlength="50" placeholder="飞机/大巴" />
          </el-form-item>
          <el-form-item label="用餐">
            <el-input v-model="itineraryForm.meals" maxlength="50" placeholder="早/中/晚" />
          </el-form-item>
          <el-form-item label="住宿">
            <el-input v-model="itineraryForm.hotel" maxlength="100" placeholder="酒店名称" />
          </el-form-item>
        </div>
        <el-form-item label="地址">
          <el-input v-model="itineraryForm.address" maxlength="255" placeholder="景点或酒店地址" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="flex justify-end gap-1">
          <el-button size="small" @click="itineraryOpen = false">取消</el-button>
          <el-button size="small" type="primary" :loading="itinerarySaving" @click="submitItinerary">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="imageOpen" title="添加图片" width="460px" append-to-body>
      <el-form :model="imageForm" label-position="top" size="small">
        <el-form-item label="图片">
          <ImageUpload v-model="imageForm.imageUrl" tip="拖拽图片到此处，或点击从本地选择" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="imageForm.sort" :min="0" class="w-full" controls-position="right" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="flex justify-end gap-1">
          <el-button size="small" @click="imageOpen = false">取消</el-button>
          <el-button size="small" type="primary" :loading="imageSaving" @click="submitImage">添加</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="departureOpen" :title="departureEditingId ? '编辑班期' : '添加班期'" width="460px" append-to-body>
      <el-form :model="departureForm" label-position="top" size="small">
        <el-form-item label="出发日期">
          <el-date-picker
            v-model="departureForm.departDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            class="!w-full"
          />
        </el-form-item>
        <div class="grid grid-cols-2 gap-2">
          <el-form-item label="成人价">
            <el-input-number v-model="departureForm.adultPrice" :min="0" :precision="2" :step="100" class="w-full" controls-position="right" />
          </el-form-item>
          <el-form-item label="儿童价">
            <el-input-number v-model="departureForm.childPrice" :min="0" :precision="2" :step="100" class="w-full" controls-position="right" />
          </el-form-item>
          <el-form-item label="库存">
            <el-input-number v-model="departureForm.stock" :min="0" class="w-full" controls-position="right" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="departureForm.status" class="w-full">
              <el-option label="可售" :value="1" />
              <el-option label="停售" :value="0" />
            </el-select>
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <div class="flex justify-end gap-1">
          <el-button size="small" @click="departureOpen = false">取消</el-button>
          <el-button size="small" type="primary" :loading="departureSaving" @click="submitDeparture">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </el-drawer>
</template>
