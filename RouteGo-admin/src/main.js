import { createApp } from 'vue'
import { createPinia } from 'pinia'
// ElLoading 是指令型插件，必须显式 import 注册（不走 unplugin 自动注入）；
// 其样式由下面这行显式引入，其余组件/消息 API 均由 unplugin 按需补样式
import { ElLoading } from 'element-plus'
import 'element-plus/es/components/loading/style/css'
import './styles/tailwind.css'
import './styles/theme.css'
import App from './App.vue'
import router from './router'
import { permission } from './directives/permission'
import { reportError } from './utils/error-report'
import { startVersionCheck } from './utils/version-check'

const app = createApp(App)

app.use(createPinia())
app.use(router)
// v-loading 指令按需注册（Element Plus 其余组件与消息 API 由 unplugin 按需引入，见 vite.config.js）
app.use(ElLoading)
app.directive('perm', permission)

// 全局错误兜底：任何渲染异常不再静默白屏，至少留下完整堆栈与一条可见提示
app.config.errorHandler = (err, instance, info) => {
  reportError(err, {
    info,
    component: instance?.$options?.name || instance?.$options?.__name || '',
  })
}

// 捕获未处理的 Promise 拒绝（如漏写 catch 的异步逻辑）
window.addEventListener('unhandledrejection', (event) => {
  reportError(event.reason, { info: 'unhandledrejection', component: '' })
})

app.mount('#app')

// 长驻页面检测新版本，提示用户刷新（详见 utils/version-check.js）
startVersionCheck()
