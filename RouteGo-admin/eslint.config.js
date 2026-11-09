import js from '@eslint/js'
import pluginVue from 'eslint-plugin-vue'
import globals from 'globals'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

// unplugin-auto-import 生成的自动注入清单（vite.config.js 里 eslintrc.enabled 开启，
// 跑一次 dev/build 后生成）；文件不存在时不阻断 lint
let autoImportGlobals = {}
try {
  autoImportGlobals = JSON.parse(
    readFileSync(fileURLToPath(new URL('./.eslintrc-auto-import.json', import.meta.url)), 'utf-8')
  ).globals ?? {}
} catch {
  /* 首次运行前尚无该文件 */
}

/**
 * ESLint 9/10 flat config。
 *
 * 策略：essential 级规则集（只抓真错误，不掺格式偏好）+ 少量 recommended 规则。
 * 不引入 Prettier / stylistic 规则：存量代码风格统一收益低、全量格式化 diff 污染大，
 * 由 IDE 保存时格式化按需推进。
 */
export default [
  { ignores: ['dist/**', 'node_modules/**', 'coverage/**'] },

  js.configs.recommended,
  ...pluginVue.configs['flat/essential'],

  {
    languageOptions: {
      ecmaVersion: 'latest',
      sourceType: 'module',
      globals: {
        ...globals.browser,
        // vite.config.js define 注入的构建期常量
        __APP_VERSION__: 'readonly',
        // unplugin-auto-import 注入的 ElMessage/ElMessageBox 等
        ...autoImportGlobals,
      },
    },
    rules: {
      /* ---- Vue：essential 之外补几个高价值规则 ---- */
      // 视图组件普遍单词命名（UserView 等），业务约定优先
      'vue/multi-word-component-names': 'off',
      // 推荐但不强制的两个：属性顺序与自闭合交给 IDE，不进 CI 阻断
      'vue/attributes-order': 'off',
      'vue/html-self-closing': 'off',
      'vue/attribute-hyphenation': 'off',
      'vue/v-bind-style': 'off',
      'vue/v-on-style': 'off',
      // key 用业务主键（报告 G5 项的静态枚举数组允许 index）
      'vue/require-v-for-key': 'error',
      'vue/no-use-v-if-with-v-for': 'error',

      /* ---- JS ---- */
      'no-unused-vars': ['warn', { args: 'none', caughtErrors: 'none' }],
      'no-console': 'off', // utils/error-report.js 的兜底 console.error 受控使用
      'prefer-const': 'warn',
      eqeqeq: ['warn', 'smart'],
    },
  },

  // 构建配置跑在 Node 环境
  {
    files: ['vite.config.js', 'eslint.config.js'],
    languageOptions: {
      globals: { ...globals.node },
    },
  },
]
