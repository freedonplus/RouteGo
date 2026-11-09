<script setup>
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { createDictType, getDictType, updateDictType } from '@/api/system'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  mode: { type: String, default: 'create' },
  dictTypeId: { type: [Number, String], default: null },
})
const emit = defineEmits(['update:modelValue', 'saved'])

const formRef = ref()
const loading = ref(false)
const saving = ref(false)
const isEdit = computed(() => props.mode === 'edit')
const form = reactive({ dictName: '', dictType: '', status: 1, remark: '' })

const rules = computed(() => ({
  dictName: [{ required: true, message: '请输入字典名称', trigger: 'blur' }],
  dictType: isEdit.value
    ? []
    : [
        { required: true, message: '请输入字典类型', trigger: 'blur' },
        {
          pattern: /^[a-z][a-z0-9_]{1,49}$/,
          message: '小写字母开头，2–50 位小写字母、数字或下划线',
          trigger: 'blur',
        },
      ],
}))

function reset() {
  Object.assign(form, { dictName: '', dictType: '', status: 1, remark: '' })
  formRef.value?.clearValidate()
}

watch(
  () => props.modelValue,
  async (open) => {
    if (!open) return
    reset()
    if (!isEdit.value || !props.dictTypeId) return
    loading.value = true
    try {
      const detail = await getDictType(props.dictTypeId)
      Object.assign(form, {
        dictName: detail.dictName,
        dictType: detail.dictType,
        status: detail.status,
        remark: detail.remark || '',
      })
      await nextTick()
      formRef.value?.clearValidate()
    } catch (error) {
      ElMessage.error(error.message || '字典类型加载失败')
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
      await updateDictType(props.dictTypeId, {
        dictName: form.dictName,
        status: form.status,
        remark: form.remark,
      })
      ElMessage.success('字典类型已更新')
    } else {
      await createDictType({
        dictName: form.dictName,
        dictType: form.dictType.trim(),
        status: form.status,
        remark: form.remark,
      })
      ElMessage.success('字典类型已创建')
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
    :title="isEdit ? '编辑字典类型' : '新增字典类型'"
    size="400px"
    append-to-body
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-form ref="formRef" v-loading="loading" :model="form" :rules="rules" label-position="top" size="small">
      <el-form-item label="字典名称" prop="dictName">
        <el-input v-model="form.dictName" placeholder="如：用户状态" />
      </el-form-item>
      <el-form-item label="字典类型" prop="dictType">
        <el-input v-model="form.dictType" :disabled="isEdit" placeholder="sys_user_status" class="font-mono" />
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
          {{ isEdit ? '保存修改' : '创建类型' }}
        </el-button>
      </div>
    </template>
  </el-drawer>
</template>
