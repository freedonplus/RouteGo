<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Download, Refresh, Search, UploadFilled } from '@element-plus/icons-vue'
import { deleteFile, downloadFile, pageFiles, uploadFile } from '@/api/system'
import { formatDateTime, formatFileSize } from '@/utils/format'
import { API_BASE_URL } from '@/utils/api-base'
import PageHeader from '@/components/PageHeader.vue'
import FilterBar from '@/components/FilterBar.vue'

const loading = ref(false)
const uploading = ref(false)
const rows = ref([])
const filters = reactive({ originalName: '', fileType: '' })
const page = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const ALLOWED = ['jpg', 'jpeg', 'png', 'webp', 'pdf']
const MAX_SIZE = 20 * 1024 * 1024
const API_BASE = API_BASE_URL
const IMAGE_TYPES = ['jpg', 'jpeg', 'png', 'webp']
const STORAGE_LABELS = { 1: '本地', 2: 'OSS', 3: 'MinIO' }

const pendingCount = ref(0)

function isImage(row) {
  return IMAGE_TYPES.includes(String(row.fileType || '').toLowerCase())
}

function previewUrl(row) {
  if (!isImage(row) || !row.fileUrl) return ''
  if (/^https?:\/\//.test(row.fileUrl)) return row.fileUrl
  return API_BASE + row.fileUrl
}

function storageLabel(row) {
  return STORAGE_LABELS[row.storageType] || '其他'
}

async function load() {
  loading.value = true
  try {
    const result = await pageFiles({
      originalName: filters.originalName || undefined,
      fileType: filters.fileType || undefined,
      pageNum: page.pageNum,
      pageSize: page.pageSize,
    })
    rows.value = result.records || []
    page.total = Number(result.total || 0)
  } catch (error) {
    ElMessage.error(error.message || '文件加载失败')
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
  filters.originalName = ''
  filters.fileType = ''
  page.pageNum = 1
  load()
}

function beforeUpload(file) {
  const ext = String(file.name || '').split('.').pop().toLowerCase()
  if (!ALLOWED.includes(ext)) {
    ElMessage.error('仅支持 jpg、jpeg、png、webp、pdf')
    return false
  }
  if (file.size > MAX_SIZE) {
    ElMessage.error('文件不能超过 20MB')
    return false
  }
  return true
}

async function handleUpload({ file }) {
  pendingCount.value += 1
  uploading.value = true
  try {
    await uploadFile(file)
    ElMessage.success(`「${file.name}」上传成功`)
    load()
  } catch (error) {
    ElMessage.error(`「${file.name}」上传失败：${error.message || '未知错误'}`)
  } finally {
    pendingCount.value -= 1
    if (pendingCount.value <= 0) {
      pendingCount.value = 0
      uploading.value = false
    }
  }
}

async function download(row) {
  try {
    const blob = await downloadFile(row.id)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = row.originalName || row.fileName
    link.click()
    URL.revokeObjectURL(url)
  } catch (error) {
    ElMessage.error(error.message || '下载失败')
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确定删除文件「${row.originalName}」吗？`, '删除文件', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  try {
    await deleteFile(row.id)
    ElMessage.success('文件已删除')
    load()
  } catch (error) {
    ElMessage.error(error.message || '删除失败')
  }
}

onMounted(load)
</script>

<template>
  <div class="pb-4">
    <PageHeader title="文件管理" description="MinIO 对象存储 · 支持 jpg / png / webp / pdf，最大 20MB">
      <template #actions>
        <el-button size="small" :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
        <el-upload
          v-perm="'admin:SysFile:upload'"
          :show-file-list="false"
          :http-request="handleUpload"
          :before-upload="beforeUpload"
          accept=".jpg,.jpeg,.png,.webp,.pdf"
        >
          <el-button size="small" type="primary" :icon="Upload" :loading="uploading">上传文件</el-button>
        </el-upload>
      </template>
    </PageHeader>

    <div class="rg-page space-y-2">
      <!-- 拖拽 / 点击上传区 -->
      <el-upload
        v-perm="'admin:SysFile:upload'"
        drag
        multiple
        :show-file-list="false"
        :http-request="handleUpload"
        :before-upload="beforeUpload"
        accept=".jpg,.jpeg,.png,.webp,.pdf"
        class="file-drag-upload"
      >
        <div v-loading="uploading" class="flex items-center justify-center gap-3 py-4">
          <el-icon class="text-2xl text-[#a8435f]"><UploadFilled /></el-icon>
          <div class="text-left">
            <p class="text-[12px] font-medium text-[#1e293b]">
              将文件拖到此处，或 <em class="not-italic text-[#a8435f] underline underline-offset-2">点击选择本地文件</em>
            </p>
            <p class="mt-0.5 text-[10px] text-[#94a3b8]">
              支持 jpg / jpeg / png / webp / pdf，单个不超过 20MB，可多选 · 存储于 MinIO 对象存储
            </p>
          </div>
        </div>
      </el-upload>

      <FilterBar>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">文件名</span>
          <el-input v-model="filters.originalName" size="small" placeholder="原始文件名" clearable style="width: 180px" @keyup.enter="search" />
        </label>
        <label class="flex flex-col gap-0.5">
          <span class="rg-label">类型</span>
          <el-input v-model="filters.fileType" size="small" placeholder="png" clearable style="width: 90px" class="font-mono" @keyup.enter="search" />
        </label>
        <template #actions>
          <el-button size="small" type="primary" :icon="Search" @click="search">查询</el-button>
          <el-button size="small" @click="resetFilters">重置</el-button>
        </template>
      </FilterBar>

      <div class="rg-card min-w-0">
        <div class="rg-scroll-x">
          <el-table v-loading="loading" :data="rows" size="small" border stripe row-key="id" empty-text="暂无文件">
            <el-table-column label="ID" width="50">
              <template #default="{ row }">
                <span class="font-mono text-[11px] text-[#64748b]">{{ row.id }}</span>
              </template>
            </el-table-column>
            <el-table-column label="文件名" min-width="220">
              <template #default="{ row }">
                <div class="flex items-center gap-2">
                  <el-image
                    v-if="isImage(row) && previewUrl(row)"
                    :src="previewUrl(row)"
                    :preview-src-list="[previewUrl(row)]"
                    preview-teleported
                    fit="cover"
                    class="h-9 w-12 shrink-0 rounded border border-[#e2e8f0]"
                  >
                    <template #error>
                      <span
                        class="flex h-9 w-12 items-center justify-center rounded border border-[#e2e8f0] bg-[#f8fafc] font-mono text-[10px] text-[#94a3b8]"
                      >
                        {{ row.fileType }}
                      </span>
                    </template>
                  </el-image>
                  <div class="min-w-0">
                    <p class="truncate text-[11px] font-medium text-[#1e293b]">{{ row.originalName }}</p>
                    <p class="truncate font-mono text-[10px] text-[#94a3b8]">{{ row.filePath }}</p>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="类型" width="66">
              <template #default="{ row }">
                <span class="rounded bg-[#f1f5f9] px-1.5 py-0.5 font-mono text-[10px] text-[#475569]">
                  {{ row.fileType }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="大小" width="82" align="right">
              <template #default="{ row }">
                <span class="font-mono text-[11px]">{{ formatFileSize(row.fileSize) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="存储" width="70">
              <template #default="{ row }">
                <span
                  class="rounded px-1.5 py-0.5 text-[10px]"
                  :class="row.storageType === 3
                    ? 'bg-[#ecfdf5] text-[#15803d]'
                    : 'bg-[#f1f5f9] text-[#475569]'"
                >
                  {{ storageLabel(row) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="上传时间" width="134">
              <template #default="{ row }">
                <span class="font-mono text-[11px] text-[#64748b]">{{ formatDateTime(row.createTime) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="132" fixed="right">
              <template #default="{ row }">
                <el-button v-perm="'admin:SysFile:page'" link type="primary" size="small" :icon="Download" @click="download(row)">
                  下载
                </el-button>
                <el-button v-perm="'admin:SysFile:delete'" link type="danger" size="small" @click="remove(row)">
                  删除
                </el-button>
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
  </div>
</template>

<style scoped>
.file-drag-upload :deep(.el-upload-dragger) {
  padding: 0;
  border-radius: 6px;
}
.file-drag-upload :deep(.el-upload-dragger:hover) {
  border-color: #a8435f;
}
.file-drag-upload {
  width: 100%;
}
.file-drag-upload :deep(.el-upload) {
  width: 100%;
}
</style>
