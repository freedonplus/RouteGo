<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const REMEMBER_KEY = 'rg-remember-username'
const isMock = import.meta.env.VITE_USE_MOCK === 'true'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const formRef = ref()
const loading = ref(false)
const captchaLoading = ref(false)
const errorMessage = ref('')
const captchaImage = ref('')
const remember = ref(Boolean(localStorage.getItem(REMEMBER_KEY)))

const form = reactive({
  username: localStorage.getItem(REMEMBER_KEY) || '',
  password: '',
  captchaCode: '',
})

const captchaUuid = ref('')

const rules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_]{3,50}$/, message: '账号为 3–50 位字母、数字或下划线', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, max: 20, message: '密码为 8–20 位', trigger: 'blur' },
  ],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
}

async function refreshCaptcha() {
  captchaLoading.value = true
  try {
    const result = await auth.fetchCaptcha()
    captchaUuid.value = result.uuid
    captchaImage.value = `data:image/png;base64,${result.image}`
    form.captchaCode = ''
  } catch (error) {
    errorMessage.value = error.message || '验证码加载失败'
  } finally {
    captchaLoading.value = false
  }
}

function fillDemo(username, password) {
  form.username = username
  form.password = password
  form.captchaCode = ''
  errorMessage.value = ''
}

async function submit() {
  errorMessage.value = ''
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await auth.login({
      username: form.username.trim(),
      password: form.password,
      captchaUuid: captchaUuid.value,
      captchaCode: form.captchaCode,
    })
    if (remember.value) localStorage.setItem(REMEMBER_KEY, form.username.trim())
    else localStorage.removeItem(REMEMBER_KEY)
    ElMessage.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
    let safe = '/dashboard'
    try {
      const target = new URL(redirect, window.location.origin)
      if (target.origin === window.location.origin && redirect.startsWith('/')) {
        safe = `${target.pathname}${target.search}${target.hash}`
      }
    } catch {
      safe = '/dashboard'
    }
    router.replace(safe)
  } catch (error) {
    errorMessage.value = error.message || '登录失败'
    await refreshCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(refreshCaptcha)
</script>

<template>
  <div class="flex min-h-screen bg-[#f8fafc]">
    <section
      class="hidden w-[44%] shrink-0 flex-col justify-between bg-[#121212] px-5 py-5 lg:flex"
    >
      <div class="flex items-center gap-2">
        <span class="h-6 w-6 rounded bg-[#e89aaf]" aria-hidden="true"></span>
        <span class="text-sm font-semibold tracking-tight text-white">RouteGo</span>
        <span class="ml-auto rounded bg-[#1f1f1f] px-1.5 py-0.5 text-[10px] text-[#94a3b8]">
          Admin
        </span>
      </div>

      <div>
        <p class="text-[10px] font-medium uppercase tracking-wide text-[#6b7280]">
          Operations Console
        </p>
        <h1 class="mt-2 text-base font-semibold leading-6 tracking-tight text-white">
          把用户、角色与权限<br />放进同一个视图
        </h1>
        <p class="mt-2 max-w-sm text-xs leading-5 text-[#94a3b8]">
          紧凑表格、行内操作、颜色编码状态。少一次跳转，多一条有效信息。
        </p>

        <ul class="mt-4 divide-y divide-[#2a2a2a] border-y border-[#2a2a2a]">
          <li class="flex items-center gap-2 py-2">
            <span class="font-mono text-[10px] text-[#e89aaf]">01</span>
            <span class="text-xs text-[#cbd5e1]">RBAC 菜单与按钮级权限</span>
          </li>
          <li class="flex items-center gap-2 py-2">
            <span class="font-mono text-[10px] text-[#e89aaf]">02</span>
            <span class="text-xs text-[#cbd5e1]">12px 表格密度，一屏更多行</span>
          </li>
          <li class="flex items-center gap-2 py-2">
            <span class="font-mono text-[10px] text-[#e89aaf]">03</span>
            <span class="text-xs text-[#cbd5e1]">乐观锁与操作审计</span>
          </li>
        </ul>
      </div>

      <p class="font-mono text-[10px] text-[#6b7280]">v1.0.0 · RBAC · Data Dense</p>
    </section>

    <section class="flex min-w-0 flex-1 items-center justify-center px-4 py-6">
      <div class="w-full max-w-[360px]">
        <div class="mb-4 flex items-center gap-2 lg:hidden">
          <span class="h-5 w-5 rounded bg-[#e89aaf]" aria-hidden="true"></span>
          <span class="text-sm font-semibold tracking-tight text-[#121212]">RouteGo Admin</span>
        </div>

        <div class="rg-card px-4 py-4">
          <p class="text-base font-semibold tracking-tight text-[#121212]">登录控制台</p>
          <p class="mt-0.5 text-xs text-[#64748b]">使用管理员账号进入后台</p>

          <el-alert
            v-if="errorMessage"
            :title="errorMessage"
            type="error"
            :closable="false"
            show-icon
            class="mt-3"
          />

          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            label-position="top"
            size="small"
            class="mt-3"
            @keyup.enter="submit"
          >
            <el-form-item label="账号" prop="username">
              <el-input
                v-model="form.username"
                placeholder="请输入账号"
                autocomplete="username"
                clearable
              />
            </el-form-item>

            <el-form-item label="密码" prop="password">
              <el-input
                v-model="form.password"
                type="password"
                placeholder="请输入密码"
                autocomplete="current-password"
                show-password
              />
            </el-form-item>

            <el-form-item label="验证码" prop="captchaCode">
              <div class="flex w-full items-center gap-2">
                <el-input
                  v-model="form.captchaCode"
                  placeholder="4 位字符"
                  maxlength="4"
                  class="min-w-0 flex-1 font-mono"
                />
                <button
                  type="button"
                  class="relative h-[30px] w-[120px] shrink-0 overflow-hidden rounded border border-[#e2e8f0] bg-[#f8fafc]"
                  aria-label="刷新验证码"
                  :disabled="captchaLoading"
                  @click="refreshCaptcha"
                >
                  <img
                    v-if="captchaImage"
                    :src="captchaImage"
                    alt="图形验证码"
                    class="h-full w-full object-cover"
                  />
                  <span
                    v-else
                    class="flex h-full w-full items-center justify-center text-[10px] text-[#94a3b8]"
                  >
                    加载中…
                  </span>
                  <span
                    class="absolute bottom-0 right-0 flex h-3.5 w-3.5 items-center justify-center bg-[#121212] text-white"
                  >
                    <el-icon class="text-[8px]"><Refresh /></el-icon>
                  </span>
                </button>
              </div>
            </el-form-item>

            <div class="mb-3 flex items-center justify-between">
              <el-checkbox v-model="remember" size="small">记住账号</el-checkbox>
              <button
                type="button"
                class="text-[11px] text-[#a8435f] hover:underline"
                @click="ElMessage.info('请联系超级管理员重置密码')"
              >
                忘记密码？
              </button>
            </div>

            <el-button
              type="primary"
              class="w-full"
              :loading="loading"
              @click="submit"
            >
              登 录
            </el-button>
          </el-form>

          <div v-if="isMock" class="mt-3 border-t border-[#e2e8f0] pt-2">
            <p class="text-[10px] font-medium uppercase tracking-wide text-[#64748b]">
              演示账号
            </p>
            <div class="mt-1 grid grid-cols-2 gap-1">
              <button
                type="button"
                class="rounded border border-[#e2e8f0] bg-[#f8fafc] px-2 py-1 text-left transition-colors duration-150 hover:border-[#e89aaf]"
                @click="fillDemo('admin', 'Admin123!')"
              >
                <span class="block font-mono text-[10px] text-[#1e293b]">admin</span>
                <span class="block font-mono text-[10px] text-[#94a3b8]">Admin123!</span>
              </button>
              <button
                type="button"
                class="rounded border border-[#e2e8f0] bg-[#f8fafc] px-2 py-1 text-left transition-colors duration-150 hover:border-[#e89aaf]"
                @click="fillDemo('ops', 'Ops12345')"
              >
                <span class="block font-mono text-[10px] text-[#1e293b]">ops</span>
                <span class="block font-mono text-[10px] text-[#94a3b8]">Ops12345</span>
              </button>
            </div>
            <p class="mt-1 text-[10px] text-[#94a3b8]">点击账号自动填充，验证码按图片输入。</p>
          </div>
        </div>

        <p class="mt-2 text-center font-mono text-[10px] text-[#94a3b8]">
          RouteGo Admin · v1.0.0
        </p>
      </div>
    </section>
  </div>
</template>
