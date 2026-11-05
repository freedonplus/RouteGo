<script setup>
import { reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { bindUserOauth, getUserOauth, unbindUserOauth } from '@/api/system'
import { formatDate } from '@/utils/format'
import ImageUpload from '@/components/ImageUpload.vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  user: { type: Object, default: null },
})
const emit = defineEmits(['update:modelValue'])

const providers = [
  { value: 'wechat', label: '微信' },
  { value: 'qq', label: 'QQ' },
  { value: 'apple', label: 'Apple' },
  { value: 'dingtalk', label: '钉钉' },
]
const providerLabel = (value) => providers.find((item) => item.value === value)?.label || value

const loading = ref(false)
const saving = ref(false)
const bindings = ref([])
const form = reactive({ provider: 'wechat', openid: '', unionid: '', nickname: '', avatar: '' })

async function load() {
  if (!props.user) return
  loading.value = true
  try {
    bindings.value = (await getUserOauth(props.user.id)) || []
  } catch (error) {
    ElMessage.error(error.message || '绑定信息加载失败')
    bindings.value = []
  } finally {
    loading.value = false
  }
}

watch(
  () => props.modelValue,
  (open) => {
    if (!open) return
    Object.assign(form, { provider: 'wechat', openid: '', unionid: '', nickname: '', avatar: '' })
    load()
  }
)

async function bind() {
  if (!form.openid.trim()) {
    ElMessage.warning('请输入 openid')
    return
  }
  saving.value = true
  try {
    await bindUserOauth({
      userId: props.user.id,
      provider: form.provider,
      openid: form.openid.trim(),
      unionid: form.unionid.trim() || undefined,
      nickname: form.nickname.trim() || undefined,
      avatar: form.avatar.trim() || undefined,
    })
    ElMessage.success('绑定成功')
    Object.assign(form, { openid: '', unionid: '', nickname: '', avatar: '' })
    load()
  } catch (error) {
    ElMessage.error(error.message || '绑定失败')
  } finally {
    saving.value = false
  }
}

async function unbind(row) {
  try {
    await ElMessageBox.confirm(
      `确定解绑 ${providerLabel(row.provider)} 账号吗？`,
      '解绑',
      { type: 'warning', confirmButtonText: '解绑', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await unbindUserOauth(row.id)
    ElMessage.success('已解绑')
    load()
  } catch (error) {
    ElMessage.error(error.message || '解绑失败')
  }
}
</script>

<template>
  <el-drawer
    :model-value="modelValue"
    title="第三方绑定"
    size="480px"
    append-to-body
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div class="mb-2 flex items-center gap-2 border-b border-[#e2e8f0] pb-2">
      <div class="min-w-0">
        <p class="truncate text-xs font-medium text-[#1e293b]">{{ user?.nickname }}</p>
        <p class="font-mono text-[10px] text-[#94a3b8]">{{ user?.username }}</p>
      </div>
      <span class="ml-auto font-mono text-[10px] text-[#64748b]">{{ bindings.length }} 个绑定</span>
    </div>

    <div v-loading="loading" class="space-y-1">
      <div
        v-for="row in bindings"
        :key="row.id"
        class="flex items-center gap-2 rounded border border-[#e2e8f0] px-2.5 py-2"
      >
        <span class="rounded bg-[#fdf1f5] px-1.5 py-0.5 text-[10px] text-[#a8435f]">
          {{ providerLabel(row.provider) }}
        </span>
        <div class="min-w-0 flex-1">
          <p class="truncate text-[11px] text-[#1e293b]">{{ row.nickname || '未命名' }}</p>
          <p class="truncate font-mono text-[10px] text-[#94a3b8]">{{ row.openid }}</p>
        </div>
        <span class="shrink-0 font-mono text-[10px] text-[#94a3b8]">{{ formatDate(row.bindTime) }}</span>
        <el-button v-perm="'admin:SysUser:update'" link type="danger" size="small" @click="unbind(row)">
          解绑
        </el-button>
      </div>
      <p v-if="!bindings.length && !loading" class="py-4 text-center text-[11px] text-[#94a3b8]">
        暂无第三方绑定
      </p>
    </div>

    <div class="mt-3 rounded border border-[#e2e8f0] px-2.5 py-2">
      <p class="rg-section-title">新增绑定</p>
      <el-form label-position="top" size="small" class="mt-1.5">
        <div class="grid grid-cols-2 gap-2">
          <el-form-item label="平台">
            <el-select v-model="form.provider" class="w-full">
              <el-option v-for="item in providers" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="昵称">
            <el-input v-model="form.nickname" placeholder="第三方昵称" />
          </el-form-item>
        </div>
        <el-form-item label="openid">
          <el-input v-model="form.openid" placeholder="必填" class="font-mono" />
        </el-form-item>
        <el-form-item label="unionid">
          <el-input v-model="form.unionid" placeholder="可选" class="font-mono" />
        </el-form-item>
        <el-form-item label="头像">
          <ImageUpload v-model="form.avatar" tip="拖拽头像到此处，或点击从本地选择" />
        </el-form-item>
      </el-form>
      <div class="flex justify-end">
        <el-button v-perm="'admin:SysUser:update'" size="small" type="primary" :loading="saving" @click="bind">
          绑定
        </el-button>
      </div>
    </div>
  </el-drawer>
</template>
