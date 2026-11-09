import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { fileURLToPath, URL } from 'node:url'
import { execSync } from 'node:child_process'
import { readFileSync } from 'node:fs'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  // 版本号 = package.json 版本 + git 短 sha，注入为全局常量 __APP_VERSION__
  // （消费方见 src/utils/version-check.js；git 不可用时降级 unknown，不阻塞构建）
  let gitSha = 'unknown'
  try {
    gitSha = execSync('git rev-parse --short HEAD', { stdio: ['ignore', 'pipe', 'ignore'] })
      .toString()
      .trim()
  } catch {
    /* 非 git 环境忽略 */
  }
  const { version } = JSON.parse(readFileSync(fileURLToPath(new URL('./package.json', import.meta.url)), 'utf-8'))

  return {
    plugins: [
      vue(),
      tailwindcss(),
      // Element Plus 按需引入：模板里的 el-* 组件与 script 里的 ElMessage/ElMessageBox/ElNotification
      // 由 resolver 自动补 import 与样式，不再全量注册（原 main.js 全量 use 已移除）
      AutoImport({
        resolvers: [ElementPlusResolver()],
        // 生成 .eslintrc-auto-import.json，供 eslint.config.js 把自动注入的
        // ElMessage/ElMessageBox 等登记为全局量，避免 no-undef 误报
        eslintrc: { enabled: true },
      }),
      Components({
        resolvers: [ElementPlusResolver()],
      }),
    ],
    define: {
      __APP_VERSION__: JSON.stringify(`${version}+${gitSha}`),
    },
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
    build: {
      sourcemap: false,
      rollupOptions: {
        output: {
          // 第三方库拆独立 chunk：业务改动不会使其长效缓存失效。
          // 注意 Vite 8（Rolldown）的 manualChunks 只支持函数形式。
          manualChunks(id) {
            if (!id.includes('node_modules')) return undefined
            if (id.includes('element-plus') || id.includes('@element-plus')) return 'element-plus'
            if (/[\\/]node_modules[\\/](vue|vue-router|pinia|@vue)[\\/]/.test(id)) return 'vue'
            return 'vendor'
          },
        },
      },
    },
    server: {
      port: 5173,
      proxy: {
        '/api': {
          target: env.VITE_PROXY_TARGET || 'http://localhost:8080',
          changeOrigin: true,
        },
      },
    },
  }
})
