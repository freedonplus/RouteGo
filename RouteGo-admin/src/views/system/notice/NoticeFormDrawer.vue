<script setup>
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { createNotice, getNotice, updateNotice } from '@/api/system'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  mode: { type: String, default: 'create' },
  noticeId: { type: [Number, String], default: null },
})
const emit = defineEmits(['update:modelValue', 'saved'])

const formRef = ref()
const loading = ref(false)
const saving = ref(false)
const isEdit = computed(() => props.mode === 'edit')
const form = reactive({ title: '', content: '', type: 1, status: 0, isTop: 0 })

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }],
}

function reset() {
  Object.assign(form, { title: '', content: '', type: 1, status: 0, isTop: 0 })
  formRef.value?.clearValidate()
}

watch(
  () => props.modelValue,
  async (open) => {
    if (!open) return
    reset()
    if (!isEdit.value || !props.noticeId) return
    loading.value = true
    try {
      const detail = await getNotice(props.noticeId)
      Object.assign(form, {
        title: detail.title,
        content: detail.content,
        type: detail.type,
        status: detail.status,
        isTop: detail.isTop ?? 0,
      })
      await nextTick()
      formRef.value?.clearValidate()
    } catch (error) {
      ElMessage.error(error.message || '公告加载失败')
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
      await updateNotice(props.noticeId, { ...form })
      ElMessage.success('公告已更新')
    } else {
      await createNotice({ ...form })
      ElMessage.success('公告已创建')
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
    :title="isEdit ? '编辑公告' : '新增公告'"
    size="520px"
    append-to-body
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-form ref="formRef" v-loading="loading" :model="form" :rules="rules" label-position="top" size="small">
      <el-form-item label="标题" prop="title">
        <el-input v-model="form.title" placeholder="公告标题" maxlength="200" show-word-limit />
      </el-form-item>
      <el-form-item label="内容" prop="content">
        <el-input v-model="form.content" type="textarea" :rows="7" placeholder="公告正文" />
      </el-form-item>
      <div class="grid grid-cols-2 gap-2">
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" class="w-full">
            <el-option label="通知" :value="1" />
            <el-option label="公告" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="置顶" prop="isTop">
          <el-select v-model="form.isTop" class="w-full">
            <el-option label="否" :value="0" />
            <el-option label="是" :value="1" />
          </el-select>
        </el-form-item>
      </div>
      <p class="text-[10px] leading-4 text-[#94a3b8]">
        新建默认草稿；发布/下架请使用列表行内操作，以便写入发布时间。
      </p>
    </el-form>
    <template #footer>
      <div class="flex justify-end gap-1">
        <el-button size="small" @click="emit('update:modelValue', false)">取消</el-button>
        <el-button size="small" type="primary" :loading="saving" @click="submit">
          {{ isEdit ? '保存修改' : '创建公告' }}
        </el-button>
      </div>
    </template>
  </el-drawer>
</template>
