<script setup>
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { createDictData, getDictData, updateDictData } from '@/api/system'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  mode: { type: String, default: 'create' },
  dictDataId: { type: [Number, String], default: null },
  dictType: { type: String, default: '' },
})
const emit = defineEmits(['update:modelValue', 'saved'])

const formRef = ref()
const loading = ref(false)
const saving = ref(false)
const isEdit = computed(() => props.mode === 'edit')
const form = reactive({
  dictType: '',
  dictLabel: '',
  dictValue: '',
  sort: 0,
  cssClass: '',
  isDefault: 0,
  status: 1,
  remark: '',
})

const rules = {
  dictLabel: [{ required: true, message: '请输入字典标签', trigger: 'blur' }],
  dictValue: [{ required: true, message: '请输入字典值', trigger: 'blur' }],
}

function reset() {
  Object.assign(form, {
    dictType: props.dictType,
    dictLabel: '',
    dictValue: '',
    sort: 0,
    cssClass: '',
    isDefault: 0,
    status: 1,
    remark: '',
  })
  formRef.value?.clearValidate()
}

watch(
  () => props.modelValue,
  async (open) => {
    if (!open) return
    reset()
    if (!isEdit.value || !props.dictDataId) return
    loading.value = true
    try {
      const detail = await getDictData(props.dictDataId)
      Object.assign(form, {
        dictType: detail.dictType,
        dictLabel: detail.dictLabel,
        dictValue: detail.dictValue,
        sort: detail.sort ?? 0,
        cssClass: detail.cssClass || '',
        isDefault: detail.isDefault ?? 0,
        status: detail.status,
        remark: detail.remark || '',
      })
      await nextTick()
      formRef.value?.clearValidate()
    } catch (error) {
      ElMessage.error(error.message || '字典数据加载失败')
    } finally {
      loading.value = false
    }
  }
)

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (isEdit.value) {
      await updateDictData(props.dictDataId, {
        dictLabel: form.dictLabel,
        dictValue: form.dictValue,
        sort: form.sort,
        cssClass: form.cssClass,
        isDefault: form.isDefault,
        status: form.status,
        remark: form.remark,
      })
      ElMessage.success('字典数据已更新')
    } else {
      await createDictData({ ...form, dictType: props.dictType })
      ElMessage.success('字典数据已创建')
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
    :title="isEdit ? '编辑字典数据' : '新增字典数据'"
    size="420px"
    append-to-body
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-form ref="formRef" v-loading="loading" :model="form" :rules="rules" label-position="top" size="small">
      <el-form-item label="字典类型">
        <el-input :model-value="dictType" disabled class="font-mono" />
      </el-form-item>
      <div class="grid grid-cols-2 gap-2">
        <el-form-item label="标签" prop="dictLabel">
          <el-input v-model="form.dictLabel" placeholder="启用" />
        </el-form-item>
        <el-form-item label="值" prop="dictValue">
          <el-input v-model="form.dictValue" placeholder="1" class="font-mono" />
        </el-form-item>
      </div>
      <div class="grid grid-cols-2 gap-2">
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :max="999" controls-position="right" class="w-full" />
        </el-form-item>
        <el-form-item label="是否默认" prop="isDefault">
          <el-select v-model="form.isDefault" class="w-full">
            <el-option label="否" :value="0" />
            <el-option label="是" :value="1" />
          </el-select>
        </el-form-item>
      </div>
      <el-form-item label="样式类" prop="cssClass">
        <el-input v-model="form.cssClass" placeholder="可选，如 text-success" class="font-mono" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="form.status" class="w-full">
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="200" show-word-limit />
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="flex justify-end gap-1">
        <el-button size="small" @click="emit('update:modelValue', false)">取消</el-button>
        <el-button size="small" type="primary" :loading="saving" @click="submit">
          {{ isEdit ? '保存修改' : '创建数据' }}
        </el-button>
      </div>
    </template>
  </el-drawer>
</template>
