# RouteGo Admin

Vue 3 + Element Plus 管理端，Data Dense 风格，深色侧栏 `#121212` + 烟灰粉 `#E89AAF`。

## 环境

| 文件 | 用途 |
| --- | --- |
| `.env.development` | 开发默认，后端地址 `http://localhost:8080/api` |
| `.env.production` | 生产构建，填部署机器的真实后端地址 |
| `.env.mock` | 内存 mock，用于无后端演示 |

可复制 `.env.example` 为 `.env.local` 覆盖本机配置（`.env.local` 不入库）。

### 后端地址怎么配

接口地址统一由 `src/utils/api-base.js` 从 `import.meta.env.VITE_API_BASE_URL` 读取，
兜底 `/api`。**不做反向代理**，浏览器直接请求该地址。

按环境改对应的 env 文件即可：

| 文件 | 用途 | 默认 |
| --- | --- | --- |
| `.env.development` | `npm run dev` | `http://localhost:8080/api` |
| `.env.production` | `npm run build` | 部署机器的真实后端地址 |

```ini
# .env.production
VITE_API_BASE_URL=http://1.2.3.4:8080/api
```

这是构建期注入，地址会被编译进静态产物——**改完要重新构建**（容器部署就是重建 admin 镜像）。

如果确实要走同源反代（外层 nginx 把 `/api` 转发到后端），把值写成 `/api` 即可；
`npm run dev` 下的 `/api` 会由 Vite 代理到 `VITE_PROXY_TARGET`。

跨域时后端 `ROUTEGO_SECURITY_ALLOWED_ORIGINS` 必须包含本前端的访问地址（协议+域名+端口），
否则接口被 CORS 拦截、写操作被 `CsrfOriginFilter` 判为非法跨站请求。

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
