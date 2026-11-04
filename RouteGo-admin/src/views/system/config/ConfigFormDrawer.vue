<script setup>
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { createConfig, getConfig, updateConfig } from '@/api/system'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  mode: { type: String, default: 'create' },
  configId: { type: [Number, String], default: null },
})
const emit = defineEmits(['update:modelValue', 'saved'])

const formRef = ref()
const loading = ref(false)
const saving = ref(false)
const isEdit = computed(() => props.mode === 'edit')
const form = reactive({ configName: '', configKey: '', configValue: '', remark: '' })

const rules = computed(() => ({
  configName: [{ required: true, message: '请输入参数名称', trigger: 'blur' }],
  configKey: isEdit.value
    ? []
    : [
        { required: true, message: '请输入参数键', trigger: 'blur' },
        {
          pattern: /^[a-zA-Z][a-zA-Z0-9._-]{1,99}$/,
          message: '字母开头，可含字母、数字、点、下划线、短横线',
          trigger: 'blur',
        },
      ],
  configValue: [{ required: true, message: '请输入参数值', trigger: 'blur' }],
}))

function reset() {
  Object.assign(form, { configName: '', configKey: '', configValue: '', remark: '' })
  formRef.value?.clearValidate()
}

watch(
  () => props.modelValue,
  async (open) => {
    if (!open) return
    reset()
    if (!isEdit.value || !props.configId) return
    loading.value = true
    try {
      const detail = await getConfig(props.configId)
      Object.assign(form, {
        configName: detail.configName,
        configKey: detail.configKey,
        configValue: detail.configValue,
        remark: detail.remark || '',
      })
      await nextTick()
      formRef.value?.clearValidate()
    } catch (error) {
      ElMessage.error(error.message || '参数加载失败')
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
      await updateConfig(props.configId, {
        configName: form.configName,
        configValue: form.configValue,
        remark: form.remark,
      })
      ElMessage.success('参数已更新')
    } else {
      await createConfig({
        configName: form.configName,
        configKey: form.configKey.trim(),
        configValue: form.configValue,
        remark: form.remark,
      })
      ElMessage.success('参数已创建')
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
    :title="isEdit ? '编辑参数' : '新增参数'"
    size="420px"
    append-to-body
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-form ref="formRef" v-loading="loading" :model="form" :rules="rules" label-position="top" size="small">
      <el-form-item label="参数名称" prop="configName">
        <el-input v-model="form.configName" placeholder="如：站点名称" />
      </el-form-item>
      <el-form-item label="参数键" prop="configKey">
        <el-input v-model="form.configKey" :disabled="isEdit" placeholder="site.name" class="font-mono" />
      </el-form-item>
      <el-form-item label="参数值" prop="configValue">
        <el-input v-model="form.configValue" type="textarea" :rows="3" placeholder="RouteGo" class="font-mono" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="200" show-word-limit />
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="flex justify-end gap-1">
        <el-button size="small" @click="emit('update:modelValue', false)">取消</el-button>
        <el-button size="small" type="primary" :loading="saving" @click="submit">
          {{ isEdit ? '保存修改' : '创建参数' }}
        </el-button>
      </div>
    </template>
  </el-drawer>
</template>
