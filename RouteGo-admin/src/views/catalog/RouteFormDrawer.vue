<script setup>
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { createRoute, getCategoryTree, getRoute, updateRoute } from '@/api/catalog'
import ImageUpload from '@/components/ImageUpload.vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  mode: { type: String, default: 'create' },
  routeId: { type: [Number, String], default: null },
})
const emit = defineEmits(['update:modelValue', 'saved'])

const formRef = ref()
const loading = ref(false)
const saving = ref(false)
const categories = ref([])
const isEdit = computed(() => props.mode === 'edit')

const emptyForm = () => ({
  categoryId: null,
  routeNo: '',
  title: '',
  subtitle: '',
  coverImage: '',
  destination: '',
  departureCity: '',
  longitude: null,
  latitude: null,
  address: '',
  days: 3,
  nights: 2,
  minPrice: 999,
  originalPrice: null,
  highlights: '',
  includeItems: '',
  excludeItems: '',
  bookingNotice: '',
  status: 0,
  sort: 0,
})

const form = reactive(emptyForm())

const rules = {
  categoryId: [{ required: true, message: '请选择线路分类', trigger: 'change' }],
  title: [{ required: true, message: '请输入线路标题', trigger: 'blur' }],
  destination: [{ required: true, message: '请输入目的地', trigger: 'blur' }],
  departureCity: [{ required: true, message: '请输入出发城市', trigger: 'blur' }],
  days: [{ required: true, message: '请输入行程天数', trigger: 'blur' }],
  minPrice: [{ required: true, message: '请输入起价', trigger: 'blur' }],
}

function flatten(nodes, depth = 0, acc = []) {
  nodes.forEach((node) => {
    acc.push({ id: node.id, label: `${'　'.repeat(depth)}${node.name}` })
    if (node.children?.length) flatten(node.children, depth + 1, acc)
  })
  return acc
}

async function loadCategories() {
  try {
    categories.value = flatten(await getCategoryTree())
  } catch {
    categories.value = []
  }
}

watch(
  () => props.modelValue,
  async (open) => {
    if (!open) return
    Object.assign(form, emptyForm())
    formRef.value?.clearValidate()
    await loadCategories()
    if (!isEdit.value || !props.routeId) return
    loading.value = true
    try {
      const detail = await getRoute(props.routeId)
      Object.keys(form).forEach((key) => {
        if (detail[key] !== undefined && detail[key] !== null) form[key] = detail[key]
      })
      await nextTick()
      formRef.value?.clearValidate()
    } catch (error) {
      ElMessage.error(error.message || '线路加载失败')
    } finally {
      loading.value = false
    }
  }
)

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  const payload = { ...form }
  // 空字符串交给后端按 null 处理，避免把空串写进 decimal
  Object.keys(payload).forEach((key) => {
    if (payload[key] === '') payload[key] = null
  })
  try {
    if (isEdit.value) {
      await updateRoute(props.routeId, payload)
      ElMessage.success('线路已更新')
    } else {
      await createRoute(payload)
      ElMessage.success('线路已创建，编号由系统自动生成')
    }
    emit('saved')
    emit('update:modelValue', false)
  } catch (error) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <el-drawer
    :model-value="modelValue"
    :title="isEdit ? '编辑线路' : '新增线路'"
    size="640px"
    append-to-body
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-form ref="formRef" v-loading="loading" :model="form" :rules="rules" label-position="top" size="small">
      <p class="rg-section-title">基础信息</p>
      <div class="grid grid-cols-2 gap-2">
        <el-form-item label="线路分类" prop="categoryId">
          <el-select v-model="form.categoryId" filterable placeholder="选择分类" class="w-full">
            <el-option v-for="item in categories" :key="item.id" :label="item.label" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="线路编号" prop="routeNo">
          <el-input v-model="form.routeNo" class="font-mono" placeholder="留空自动生成" maxlength="32" />
        </el-form-item>
      </div>
      <el-form-item label="线路标题" prop="title">
        <el-input v-model="form.title" placeholder="如：云南大理丽江双飞 6 日游" maxlength="100" show-word-limit />
      </el-form-item>
      <el-form-item label="副标题">
        <el-input v-model="form.subtitle" placeholder="卖点一句话" maxlength="200" />
      </el-form-item>
      <el-form-item label="封面图">
        <ImageUpload v-model="form.coverImage" tip="拖拽封面图到此处，或点击从本地选择" />
      </el-form-item>

      <p class="rg-section-title">行程与价格</p>
      <div class="grid grid-cols-2 gap-2">
        <el-form-item label="目的地" prop="destination">
          <el-input v-model="form.destination" placeholder="云南 大理 丽江" maxlength="100" />
        </el-form-item>
        <el-form-item label="出发城市" prop="departureCity">
          <el-input v-model="form.departureCity" placeholder="北京" maxlength="50" />
        </el-form-item>
        <el-form-item label="行程天数" prop="days">
          <el-input-number v-model="form.days" :min="1" :max="365" class="w-full" controls-position="right" />
        </el-form-item>
        <el-form-item label="住宿晚数">
          <el-input-number v-model="form.nights" :min="0" :max="365" class="w-full" controls-position="right" />
        </el-form-item>
        <el-form-item label="起价（元）" prop="minPrice">
          <el-input-number v-model="form.minPrice" :min="0" :precision="2" :step="100" class="w-full" controls-position="right" />
        </el-form-item>
        <el-form-item label="划线价（元）">
          <el-input-number v-model="form.originalPrice" :min="0" :precision="2" :step="100" class="w-full" controls-position="right" />
        </el-form-item>
        <el-form-item label="经度">
          <el-input-number v-model="form.longitude" :precision="6" :step="0.0001" class="w-full" controls-position="right" />
        </el-form-item>
        <el-form-item label="纬度">
          <el-input-number v-model="form.latitude" :precision="6" :step="0.0001" class="w-full" controls-position="right" />
        </el-form-item>
      </div>
      <el-form-item label="集合地址">
        <el-input v-model="form.address" placeholder="如：首都机场 T3 航站楼" maxlength="255" />
      </el-form-item>

      <p class="rg-section-title">图文详情</p>
      <el-form-item label="行程亮点">
        <el-input v-model="form.highlights" type="textarea" :rows="3" placeholder="每行一条卖点" />
      </el-form-item>
      <div class="grid grid-cols-1 gap-2">
        <el-form-item label="费用包含">
          <el-input v-model="form.includeItems" type="textarea" :rows="3" placeholder="每行一条" />
        </el-form-item>
        <el-form-item label="费用不含">
          <el-input v-model="form.excludeItems" type="textarea" :rows="3" placeholder="每行一条" />
        </el-form-item>
        <el-form-item label="预订须知">
          <el-input v-model="form.bookingNotice" type="textarea" :rows="3" placeholder="退改规则、证件要求等" />
        </el-form-item>
      </div>

      <div class="grid grid-cols-2 gap-2">
        <el-form-item label="状态">
          <el-select v-model="form.status" class="w-full">
            <el-option label="下架" :value="0" />
            <el-option label="上架" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" class="w-full" controls-position="right" />
        </el-form-item>
      </div>
      <p class="text-[10px] leading-4 text-[#94a3b8]">
        行程、图片、标签、班期请在列表「详情」中维护；班期是下单与库存的依据。
      </p>
    </el-form>
    <template #footer>
      <div class="flex justify-end gap-1">
        <el-button size="small" @click="emit('update:modelValue', false)">取消</el-button>
        <el-button size="small" type="primary" :loading="saving" @click="submit">
          {{ isEdit ? '保存修改' : '创建线路' }}
        </el-button>
      </div>
    </template>
  </el-drawer>
</template>
