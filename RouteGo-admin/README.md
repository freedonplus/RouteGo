# RouteGo Admin

Vue 3 + Element Plus 管理端，Data Dense 风格，深色侧栏 `#121212` + 烟灰粉 `#E89AAF`。

## 环境

| 文件 | 用途 |
| --- | --- |
| `.env.development` | 开发默认，真实后端，Vite 代理 `/api` |
| `.env.production` | 生产构建，真实后端，同源 `/api` |
| `.env.mock` | 内存 mock，用于无后端演示 |

可复制 `.env.example` 为 `.env.local` 覆盖本机配置（`.env.local` 不入库）。

后端地址通过 `VITE_PROXY_TARGET` 修改，默认 `http://localhost:8080`。

## 运行

```bash
npm install
npm run dev        # 真实后端
npm run dev:mock   # mock 数据
npm run build      # 生产构建
npm run preview
```

真实后端启动后访问 `http://localhost:5173`。登录使用后端已创建的账号。

Mock 模式演示账号：`admin / Admin123!`、`ops / Ops12345`。Mock 目前只覆盖第一批 RBAC 页面，字典、参数、公告、文件、日志需真实后端。

## 约定

- 浏览器端认证依赖后端写入的 HttpOnly Cookie；Bearer Token 只保存在内存。
- 统一响应 `{ code, msg, data }`，401 自动清理会话并跳登录。
- 风格约束见 `../docs/design/style-contract.md`，Figma 规格见 `../docs/design/figma-spec.md`。
