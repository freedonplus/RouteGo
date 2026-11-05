<script setup>
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { getOauthProviders, getOauthUrl, sendCode } from '@/api/auth'

const REMEMBER_KEY = 'rg-remember-account'
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
const loginMode = ref('password')
const codeSending = ref(false)
const countdown = ref(0)
const oauthProviders = ref([])
let countdownTimer = null

const form = reactive({
  account: localStorage.getItem(REMEMBER_KEY) || '',
  password: '',
  captchaCode: '',
  codeTarget: '',
  code: '',
})

const captchaUuid = ref('')

const rules = {
  account: [
    { required: true, message: '请输入用户名 / 手机号 / 邮箱', trigger: 'blur' },
    {
      pattern: /^[A-Za-z0-9_@.-]{3,64}$/,
      message: '支持用户名、手机号或邮箱',
      trigger: 'blur',
    },
  ],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
  codeTarget: [
    { required: true, message: '请输入手机号或邮箱', trigger: 'blur' },
    {
      pattern: /^(1[3-9]\d{9}|[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,})$/,
      message: '请输入正确的手机号或邮箱',
      trigger: 'blur',
    },
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { pattern: /^\d{4,8}$/, message: '验证码为 4–8 位数字', trigger: 'blur' },
  ],
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
  loginMode.value = 'password'
  form.account = username
  form.password = password
  form.captchaCode = ''
  errorMessage.value = ''
}

function startCountdown(seconds) {
  countdown.value = seconds
  if (countdownTimer) clearInterval(countdownTimer)
  countdownTimer = setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) {
      clearInterval(countdownTimer)
      countdownTimer = null
    }
  }, 1000)
}

async function sendLoginCode() {
  if (countdown.value > 0 || codeSending.value) return
  const target = form.codeTarget.trim()
  if (!/^(1[3-9]\d{9}|[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,})$/.test(target)) {
    errorMessage.value = '请输入正确的手机号或邮箱'
    return
  }
  codeSending.value = true
  errorMessage.value = ''
  try {
    const result = await sendCode({ target, scene: 'LOGIN' })
    startCountdown(result?.resendAfterSeconds || 60)
    ElMessage.success(`验证码已发送至 ${result?.target || target}`)
  } catch (error) {
    errorMessage.value = error.message || '验证码发送失败'
  } finally {
    codeSending.value = false
  }
}

function afterLogin() {
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
}

async function submit() {
  errorMessage.value = ''
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    if (loginMode.value === 'password') {
      await auth.login({
        account: form.account.trim(),
        password: form.password,
        captchaUuid: captchaUuid.value,
        captchaCode: form.captchaCode,
      })
      if (remember.value) localStorage.setItem(REMEMBER_KEY, form.account.trim())
      else localStorage.removeItem(REMEMBER_KEY)
    } else {
      await auth.loginWithCode({
        target: form.codeTarget.trim(),
        code: form.code.trim(),
      })
    }
    ElMessage.success('登录成功')
    afterLogin()
  } catch (error) {
    errorMessage.value = error.message || '登录失败'
    if (loginMode.value === 'password') await refreshCaptcha()
  } finally {
    loading.value = false
  }
}

async function startOauth(provider) {
  if (!provider.available) {
    ElMessage.warning(provider.reason || `${provider.label} 登录暂不可用`)
    return
  }
  try {
    const url = await getOauthUrl(provider.code)
    if (url) window.location.href = url
  } catch (error) {
    errorMessage.value = error.message || '第三方登录暂不可用'
  }
}

function switchMode(mode) {
  loginMode.value = mode
  errorMessage.value = ''
  formRef.value?.clearValidate()
}

onMounted(async () => {
  try {
    oauthProviders.value = (await getOauthProviders()) || []
  } catch {
    oauthProviders.value = []
  }

  if (route.query.oauth === 'success') {
    try {
      await auth.loadSession()
      ElMessage.success('登录成功')
      afterLogin()
      return
    } catch (error) {
      errorMessage.value = '第三方登录状态已失效，请重试'
    }
  } else if (route.query.oauth === 'failed') {
    errorMessage.value =
      typeof route.query.message === 'string' ? route.query.message : '第三方登录失败'
  }
  await refreshCaptcha()
})

onBeforeUnmount(() => {
  if (countdownTimer) clearInterval(countdownTimer)
})
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
            <span class="text-xs text-[#cbd5e1]">用户名 / 手机号 / 邮箱统一登录</span>
          </li>
          <li class="flex items-center gap-2 py-2">
            <span class="font-mono text-[10px] text-[#e89aaf]">03</span>
            <span class="text-xs text-[#cbd5e1]">乐观锁与操作审计</span>
          </li>
        </ul>
      </div>

      <p class="font-mono text-[10px] text-[#6b7280]">v1.1.0 · RBAC · Data Dense</p>
    </section>

    <section class="flex min-w-0 flex-1 items-center justify-center px-4 py-6">
      <div class="w-full max-w-[360px]">
        <div class="mb-4 flex items-center gap-2 lg:hidden">
          <span class="h-5 w-5 rounded bg-[#e89aaf]" aria-hidden="true"></span>
          <span class="text-sm font-semibold tracking-tight text-[#121212]">RouteGo Admin</span>
        </div>

        <div class="rg-card px-4 py-4">
          <p class="text-base font-semibold tracking-tight text-[#121212]">登录控制台</p>
          <p class="mt-0.5 text-xs text-[#64748b]">
            {{ loginMode === 'password' ? '用户名、手机号或邮箱均可登录' : '使用短信 / 邮箱验证码登录' }}
          </p>

          <div class="mt-2 flex gap-1 rounded border border-[#e2e8f0] p-0.5">
            <button
              type="button"
              class="flex-1 rounded px-2 py-1 text-[11px] transition-colors duration-150"
              :class="loginMode === 'password' ? 'bg-[#121212] text-white' : 'text-[#64748b]'"
              @click="switchMode('password')"
            >
              账号密码
            </button>
            <button
              type="button"
              class="flex-1 rounded px-2 py-1 text-[11px] transition-colors duration-150"
              :class="loginMode === 'code' ? 'bg-[#121212] text-white' : 'text-[#64748b]'"
              @click="switchMode('code')"
            >
              验证码登录
            </button>
          </div>

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
            <template v-if="loginMode === 'password'">
              <el-form-item label="账号" prop="account">
                <el-input
                  v-model="form.account"
                  placeholder="用户名 / 手机号 / 邮箱"
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
            </template>

            <template v-else>
              <el-form-item label="手机号 / 邮箱" prop="codeTarget">
                <el-input
                  v-model="form.codeTarget"
                  placeholder="13800001111 或 name@example.com"
                  clearable
                />
              </el-form-item>

              <el-form-item label="验证码" prop="code">
                <div class="flex w-full items-center gap-2">
                  <el-input
                    v-model="form.code"
                    placeholder="6 位数字"
                    maxlength="8"
                    class="min-w-0 flex-1 font-mono"
                  />
                  <el-button
                    size="small"
                    class="shrink-0"
                    :disabled="countdown > 0"
                    :loading="codeSending"
                    @click="sendLoginCode"
                  >
                    {{ countdown > 0 ? `${countdown}s 后重发` : '发送验证码' }}
                  </el-button>
                </div>
              </el-form-item>
            </template>

            <div v-if="loginMode === 'password'" class="mb-3 flex items-center justify-between">
              <el-checkbox v-model="remember" size="small">记住账号</el-checkbox>
              <button
                type="button"
                class="text-[11px] text-[#a8435f] hover:underline"
                @click="ElMessage.info('请联系超级管理员重置密码')"
              >
                忘记密码？
              </button>
            </div>
            <div v-else class="mb-3"></div>

            <el-button type="primary" class="w-full" :loading="loading" @click="submit">
              登 录
            </el-button>
          </el-form>

          <div
            v-if="oauthProviders.length"
            class="mt-3 border-t border-[#e2e8f0] pt-2"
          >
            <p class="text-[10px] font-medium uppercase tracking-wide text-[#64748b]">
              第三方登录
            </p>
            <div class="mt-1 flex flex-wrap gap-1">
              <button
                v-for="provider in oauthProviders"
                :key="provider.code"
                type="button"
                :title="provider.available ? `使用 ${provider.label} 登录` : provider.reason"
                class="rounded border px-2 py-1 text-[11px] transition-colors duration-150"
                :class="
                  provider.available
                    ? 'border-[#e2e8f0] bg-[#f8fafc] text-[#1e293b] hover:border-[#e89aaf]'
                    : 'cursor-not-allowed border-dashed border-[#e2e8f0] bg-[#f1f5f9] text-[#94a3b8]'
                "
                @click="startOauth(provider)"
              >
                {{ provider.label }}
              </button>
            </div>
          </div>

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
          RouteGo Admin · v1.1.0
        </p>
      </div>
    </section>
  </div>
</template>
