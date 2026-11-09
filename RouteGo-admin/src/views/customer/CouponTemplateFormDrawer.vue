<script setup>
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { createCouponTemplate, getCouponTemplate, updateCouponTemplate } from '@/api/customer'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  mode: { type: String, default: 'create' },
  templateId: { type: [Number, String], default: null },
})
const emit = defineEmits(['update:modelValue', 'saved'])

const formRef = ref()
const loading = ref(false)
const saving = ref(false)
const isEdit = computed(() => props.mode === 'edit')

const emptyForm = () => ({
  name: '',
  type: 1,
  threshold: 0,
  amount: 50,
  discount: null,
  totalCount: 1000,
  validType: 1,
  startTime: '',
  endTime: '',
  validDays: 30,
  status: 1,
})

const form = reactive(emptyForm())

const rules = {
  name: [{ required: true, message: '请输入券名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择券类型', trigger: 'change' }],
  totalCount: [{ required: true, message: '请输入发行总量', trigger: 'blur' }],
  validType: [{ required: true, message: '请选择有效期类型', trigger: 'change' }],
}

watch(
  () => props.modelValue,
  async (open) => {
    if (!open) return
    Object.assign(form, emptyForm())
    formRef.value?.clearValidate()
    if (!isEdit.value || !props.templateId) return
    loading.value = true
    try {
      const detail = await getCouponTemplate(props.templateId)
      Object.assign(form, {
        name: detail.name,
        type: detail.type,
        threshold: detail.threshold === null ? 0 : Number(detail.threshold),
        amount: detail.amount === null ? null : Number(detail.amount),
        discount: detail.discount === null ? null : Number(detail.discount),
        totalCount: detail.totalCount,
        validType: detail.validType,
        startTime: detail.startTime || '',
        endTime: detail.endTime || '',
        validDays: detail.validDays || 30,
        status: detail.status,
      })
      await nextTick()
      formRef.value?.clearValidate()
    } catch (error) {
      ElMessage.error(error.message || '券模板加载失败')
    } finally {
      loading.value = false
    }
  }
)

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  if (form.validType === 1 && (!form.startTime || !form.endTime)) {
    ElMessage.warning('固定区间需要选择起止时间')
    return
  }
  if (form.type === 1 && !form.amount) {
    ElMessage.warning('满减券需要填写减免金额')
    return
  }
  if (form.type === 2 && !form.discount) {
    ElMessage.warning('折扣券需要填写折扣率')
    return
  }
  saving.value = true
  const payload = { ...form }
  // 只提交当前类型需要的字段，避免后端写入无意义的旧值
  if (payload.type === 1) {
    payload.discount = null
  } else {
    payload.amount = null
  }
  if (payload.validType === 2) {
    payload.startTime = null
    payload.endTime = null
  } else {
    payload.validDays = null
  }
  try {
    if (isEdit.value) {
      await updateCouponTemplate(props.templateId, payload)
      ElMessage.success('券模板已更新')
    } else {
      await createCouponTemplate(payload)
      ElMessage.success('券模板已创建')
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
    :title="isEdit ? '编辑券模板' : '新增券模板'"
    size="520px"
    append-to-body
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-form ref="formRef" v-loading="loading" :model="form" :rules="rules" label-position="top" size="small">
      <el-form-item label="券名称" prop="name">
        <el-input v-model="form.name" maxlength="100" placeholder="如：满 1000 减 100" />
      </el-form-item>
      <div class="grid grid-cols-2 gap-2">
        <el-form-item label="券类型" prop="type">
          <el-select v-model="form.type" class="w-full">
            <el-option label="满减券" :value="1" />
            <el-option label="折扣券" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="使用门槛（元）">
          <el-input-number v-model="form.threshold" :min="0" :precision="2" :step="50" class="w-full" controls-position="right" />
        </el-form-item>
        <el-form-item v-if="form.type === 1" label="减免金额（元）">
          <el-input-number v-model="form.amount" :min="0.01" :precision="2" :step="10" class="w-full" controls-position="right" />
        </el-form-item>
        <el-form-item v-else label="折扣率（0–1）">
          <el-input-number v-model="form.discount" :min="0.01" :max="1" :precision="2" :step="0.05" class="w-full" controls-position="right" />
        </el-form-item>
        <el-form-item label="发行总量" prop="totalCount">
          <el-input-number v-model="form.totalCount" :min="0" class="w-full" controls-position="right" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" class="w-full">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
      </div>

      <el-form-item label="有效期类型" prop="validType">
        <el-radio-group v-model="form.validType">
          <el-radio :value="1">固定区间</el-radio>
          <el-radio :value="2">领取后计算</el-radio>
        </el-radio-group>
      </el-form-item>
      <div v-if="form.validType === 1" class="grid grid-cols-2 gap-2">
        <el-form-item label="开始时间">
          <el-date-picker
            v-model="form.startTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="开始"
            class="!w-full"
          />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker
            v-model="form.endTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="结束"
            class="!w-full"
          />
        </el-form-item>
      </div>
      <el-form-item v-else label="领取后有效天数">
        <el-input-number v-model="form.validDays" :min="1" class="w-full" controls-position="right" />
      </el-form-item>
      <p class="text-[10px] leading-4 text-[#94a3b8]">
        折扣率 0.9 表示九折；已发过券的模板不能删除，只能停用。
      </p>
    </el-form>
    <template #footer>
      <div class="flex justify-end gap-1">
        <el-button size="small" @click="emit('update:modelValue', false)">取消</el-button>
        <el-button size="small" type="primary" :loading="saving" @click="submit">
          {{ isEdit ? '保存修改' : '创建模板' }}
        </el-button>
      </div>
    </template>
  </el-drawer>
</template>
