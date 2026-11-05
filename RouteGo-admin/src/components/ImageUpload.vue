<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Delete, Plus, UploadFilled } from '@element-plus/icons-vue'
import { uploadFile } from '@/api/system'
import { resolveFileUrl } from '@/utils/format'

const props = defineProps({
  modelValue: { type: String, default: '' },
  /** 允许的扩展名 */
  accept: { type: String, default: '.jpg,.jpeg,.png,.webp' },
  /** 大小限制（MB） */
  maxSize: { type: Number, default: 20 },
  /** 是否只允许图片（决定预览方式） */
  imageOnly: { type: Boolean, default: true },
  /** 提示文案 */
  tip: { type: String, default: '拖拽图片到此处，或点击选择本地文件' },
})

const emit = defineEmits(['update:modelValue'])
const uploading = ref(false)

const previewUrl = computed(() => resolveFileUrl(props.modelValue))

function resolveAccept() {
  return props.accept
}

function beforeUpload(file) {
  const ext = String(file.name || '').split('.').pop().toLowerCase()
  const allowed = props.accept
    .split(',')
    .map((item) => item.trim().replace(/^\./, '').toLowerCase())
    .filter(Boolean)
  if (allowed.length && !allowed.includes(ext)) {
    ElMessage.error(`仅支持 ${allowed.join(' / ')} 格式`)
    return false
  }
  if (file.size > props.maxSize * 1024 * 1024) {
    ElMessage.error(`文件不能超过 ${props.maxSize}MB`)
    return false
  }
  return true
}

async function handleUpload({ file }) {
  uploading.value = true
  try {
    const result = await uploadFile(file)
    const url = result?.fileUrl || ''
    if (!url) throw new Error('上传结果缺少访问地址')
    emit('update:modelValue', url)
    ElMessage.success('上传成功')
  } catch (error) {
    ElMessage.error(error.message || '上传失败')
  } finally {
    uploading.value = false
  }
}

function clearValue() {
  emit('update:modelValue', '')
}
</script>

<template>
  <div class="w-full">
    <!-- 已有图片：预览 + 移除 -->
    <div
      v-if="previewUrl"
      v-loading="uploading"
      class="group relative w-fit overflow-hidden rounded border border-[#e2e8f0] bg-[#f8fafc]"
    >
      <img :src="previewUrl" alt="预览" class="block h-28 w-44 object-cover" />
      <div
        class="absolute inset-0 hidden items-center justify-center gap-1 bg-black/45 group-hover:flex"
      >
        <el-upload
          :show-file-list="false"
          :http-request="handleUpload"
          :before-upload="beforeUpload"
          :accept="resolveAccept()"
        >
          <el-button size="small" type="primary" :icon="UploadFilled" plain>重新上传</el-button>
        </el-upload>
        <el-button size="small" type="danger" :icon="Delete" plain @click="clearValue">移除</el-button>
      </div>
    </div>

    <!-- 无图片：拖拽区（支持点击选择） -->
    <el-upload
      v-else
      drag
      :show-file-list="false"
      :http-request="handleUpload"
      :before-upload="beforeUpload"
      :accept="resolveAccept()"
      class="image-upload-drag w-full"
    >
      <div v-loading="uploading" class="flex flex-col items-center gap-1 py-4">
        <el-icon class="text-xl text-[#a8435f]"><Plus /></el-icon>
        <p class="text-[11px] text-[#475569]">{{ tip }}</p>
        <p class="text-[10px] text-[#94a3b8]">
          支持 {{ accept.replaceAll('.', '').replaceAll(',', ' / ') }}，不超过 {{ maxSize }}MB
        </p>
      </div>
    </el-upload>
  </div>
</template>

<style scoped>
.image-upload-drag :deep(.el-upload-dragger) {
  padding: 0;
  border-radius: 4px;
}
.image-upload-drag :deep(.el-upload-dragger:hover) {
  border-color: #a8435f;
}
</style>
