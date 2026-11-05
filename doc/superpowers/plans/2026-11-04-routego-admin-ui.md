# RouteGo Admin UI（第一批）Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `RouteGo-admin` 中交付可运行的 Vue 3 + Element Plus 管理端，含登录、仪表盘、用户、角色、菜单、权限点 6 个页面，视觉严格遵循 Data Dense 风格。

**Architecture:** 单页应用，服务端菜单树驱动侧栏与路由解析（`DynamicMenuView` 按 `component` 映射渲染，未实现模块走占位页）。API 层与真实后端同构，开发期由 axios mock adapter 提供内存数据。权限通过路由守卫 + `v-perm` 指令两层控制。

**Tech Stack:** Vue 3.5、Vite 8、Element Plus、Vue Router 4、Pinia、Axios、Tailwind CSS 4、Vitest。

**Spec:** `docs/superpowers/specs/2026-11-04-routego-admin-ui-design.md`
**Style contract:** `docs/design/style-contract.md`

## Global Constraints

- 禁止：`rounded-xl/2xl/3xl/full`、`shadow-lg/xl/2xl`、`p-6~9`、`text-lg/xl/2xl`（数据区域）、`bg-gradient-*`、玻璃态、嵌套卡片。
- 表格行 `py-2` 以内、`text-xs`；表头 `text-[10px] uppercase tracking-wide text-[#64748b] font-medium`。
- 按钮 `px-2.5 py-1 text-xs rounded transition-colors`；卡片 `bg-white border border-[#e2e8f0] rounded`。
- 数字/ID/权限码/路径/时间戳 `font-mono`。
- 状态标签颜色编码且始终带文字；绿=启用、黄=待处理、红=异常、蓝=信息。
- 主色 `#121212`，强调 `#E89AAF`，浅底粉字 `#A8435F`；内容底 `#F8FAFC`，卡片 `#FFFFFF`，边框 `#E2E8F0`。
- 所有动画提供 `prefers-reduced-motion: reduce` 降级。
- 不使用 Inter / Roboto / Geist。

## Review Focus

- 菜单树里的未实现模块（`/system/dict` 等）必须渲染建设中占位页，不能 404 或死链。
- 非 `super_admin` 账号缺少按钮权限时，操作按钮必须隐藏且直接访问受限路由返回 403。
- 401（token 失效）必须清理会话并跳登录，且保留 `redirect`。
- 409 乐观锁冲突必须提示刷新，不能静默覆盖。
- 手机宽度下表格容器自身横向滚动，页面不得整体横向溢出。

---

### Task 1: 工程基础与设计 Token

**Files:**
- Modify: `RouteGo-admin/package.json`
- Modify: `RouteGo-admin/vite.config.js`
- Modify: `RouteGo-admin/src/main.js`
- Modify: `RouteGo-admin/src/App.vue`
- Create: `RouteGo-admin/src/styles/tailwind.css`
- Create: `RouteGo-admin/src/styles/theme.css`
- Create: `RouteGo-admin/src/utils/format.js`
- Create: `RouteGo-admin/src/utils/format.test.js`
- Create: `RouteGo-admin/vitest.config.js`

**Interfaces:**
- Produces: `maskPhone(value: string|null): string`、`maskEmail(value): string`、`formatDateTime(value): string`、`formatFileSize(bytes: number): string`、`isMasked(value): boolean`。

- [ ] **Step 1: 安装依赖**

```bash
cd RouteGo-admin && npm i vue-router@4 pinia axios element-plus @element-plus/icons-vue
npm i -D tailwindcss @tailwindcss/vite vitest
```

- [ ] **Step 2: 写失败的格式化测试**

```js
// src/utils/format.test.js
import { describe, it, expect } from 'vitest'
import { maskPhone, maskEmail, formatDateTime, formatFileSize, isMasked } from './format'

describe('format', () => {
  it('masks phone', () => expect(maskPhone('13812348000')).toBe('138****8000'))
  it('keeps null phone', () => expect(maskPhone(null)).toBe('—'))
  it('masks email', () => expect(maskEmail('ops@example.com')).toBe('o***@example.com'))
  it('formats datetime', () => expect(formatDateTime('2026-11-04T08:05:09')).toBe('2026-11-04 08:05:09'))
  it('formats file size', () => expect(formatFileSize(1024)).toBe('1.0 KB'))
  it('detects mask', () => expect(isMasked('138****8000')).toBe(true))
})
```

- [ ] **Step 3: 运行测试确认失败**

Run: `npx vitest run src/utils/format.test.js`
Expected: FAIL，`format` 模块不存在。

- [ ] **Step 4: 实现 `src/utils/format.js`**

`maskPhone` 保留前 3 后 4；`maskEmail` 保留首字符与域名；`formatDateTime` 输出 `YYYY-MM-DD HH:mm:ss`；`formatFileSize` 按 1024 进位输出 `x.x KB/MB/GB`；`isMasked` 判断包含 `*`。空值统一返回 `—`。

- [ ] **Step 5: 配置 Vite / Tailwind / 入口**

`vite.config.js` 加 `tailwindcss()` 插件与 `@` → `src` 别名。`main.js` 导入顺序：`./styles/tailwind.css` → `element-plus/dist/index.css` → `./styles/theme.css` → 注册 Element Plus 图标。`App.vue` 只保留 `<router-view />`。

- [ ] **Step 6: 写 `theme.css` token 与 Element Plus 覆盖**

必须包含：`:root` 品牌变量；`.el-button--primary` 使用 `#E89AAF` 底 + `#121212` 文字；`.el-button` 高度 26px、字号 12px、圆角 4px；`.el-table` 字号 12px、单元格 `padding: 6px 8px`、表头 `#F8FAFC`、斑马行 `#F8FAFC`、边框 `#E2E8F0`；`.el-input__wrapper` inset 边框与 focus 蓝色 ring；`@media (prefers-reduced-motion: reduce)` 关闭过渡。

- [ ] **Step 7: 运行测试与构建**

Run: `npx vitest run src/utils/format.test.js && npm run build`
Expected: 6 passed；build 成功。

- [ ] **Step 8: Commit**

```bash
git add RouteGo-admin/package.json RouteGo-admin/package-lock.json RouteGo-admin/vite.config.js RouteGo-admin/vitest.config.js RouteGo-admin/src
git commit -m "feat(admin-ui): scaffold Vue app with data-dense theme tokens"
```

---

### Task 2: Mock 数据层与 API 客户端

**Files:**
- Create: `RouteGo-admin/src/mock/db.js`
- Create: `RouteGo-admin/src/mock/captcha.js`
- Create: `RouteGo-admin/src/mock/index.js`
- Create: `RouteGo-admin/src/api/request.js`
- Create: `RouteGo-admin/src/api/auth.js`
- Create: `RouteGo-admin/src/api/system.js`
- Create: `RouteGo-admin/src/mock/db.test.js`

**Interfaces:**
- `db`：`{ users, roles, menus, permissions, notices, dashboard, currentUser }`，所有数组元素字段与设计文档一致；`users[i]` 额外含 `roleIds`、`roleNames`；`roles[i]` 额外含 `userCount`、`builtIn`；`menus[i]` 额外含 `childCount`；`permissions[i]` 额外含 `roleCount`。
- `paginate(list, pageNum, pageSize): { records, total, pageNum, pageSize, pages }`。
- `http`：axios 实例，`baseURL: '/api'`。
- `request(config): Promise<any>`：解包 `{ code, msg, data }`，`code !== 200` 抛 `ApiError`。
- API 函数名见下方各 Task。

- [ ] **Step 1: 写分页与筛选测试**

```js
import { describe, it, expect } from 'vitest'
import { paginate } from './db'

describe('paginate', () => {
  it('slices page 2', () => {
    const r = paginate([1,2,3,4,5,6,7], 2, 3)
    expect(r.records).toEqual([4,5,6])
    expect(r.total).toBe(7)
    expect(r.pages).toBe(3)
  })
})
```

- [ ] **Step 2: 运行确认失败**

Run: `npx vitest run src/mock/db.test.js`
Expected: FAIL，模块不存在。

- [ ] **Step 3: 实现 `db.js`**

内置 24 个用户、3 个角色（`admin` / `super_admin` / `ops`）、11 个菜单（与 `sql/admin-init.sql` 一致）、41 个权限点；两个登录账号：`admin / Admin123!`（`permissions: ['*']`）、`ops / Ops12345`（仅 `system:user:list`）。导出 `paginate` 与 `resetDb()`。

- [ ] **Step 4: 实现 `captcha.js`**

`createCaptcha(): { uuid, image, code }` 用 canvas 画 4 位字符，返回 PNG Base64（不含前缀）；`verifyCaptcha(uuid, code)` 校验后删除，2 分钟过期。

- [ ] **Step 5: 实现 mock adapter**

`src/mock/index.js` 导出 `mockAdapter(config)`：按 `method + pathname` 匹配路由，支持 `config.params` 查询与 `JSON.parse(config.data)`，统一返回 `{ data: { code, msg, data, timestamp }, status: 200 }`。实现所有 Task 7–10 需要的 CRUD、状态切换、409 版本冲突、分页筛选。延迟 150–350ms。

- [ ] **Step 6: 实现 `request.js` 与 API 模块**

`request.js` 在 `VITE_USE_MOCK !== 'false'` 时把 `http.defaults.adapter` 设为 `mockAdapter`；响应拦截器解包；401 清理 localStorage 并跳 `/login?redirect=`。`auth.js` / `system.js` 按设计文档导出函数，函数名与 Task 3–10 的调用一致。

- [ ] **Step 7: 运行测试与 lint-free 构建**

Run: `npx vitest run src/mock/db.test.js && npm run build`
Expected: passed；build 成功。

- [ ] **Step 8: Commit**

```bash
git add RouteGo-admin/src/mock RouteGo-admin/src/api RouteGo-admin/src/utils
git commit -m "feat(admin-ui): add mock backend and typed api client"
```

---

### Task 3: 认证 Store、路由与权限指令

**Files:**
- Create: `RouteGo-admin/src/stores/auth.js`
- Create: `RouteGo-admin/src/stores/auth.test.js`
- Create: `RouteGo-admin/src/router/index.js`
- Create: `RouteGo-admin/src/router/menu-components.js`
- Create: `RouteGo-admin/src/directives/permission.js`

**Interfaces:**
- `useAuthStore()`：state `{ token, user, menus, loaded }`；getter `hasPerm(perm): boolean`；action `login({username,password,captchaUuid,captchaCode})`、`loadSession()`、`logout()`、`clear()`。
- `menuComponents: Record<string, () => Promise<Component>>`。
- `v-perm`：绑定值为权限码或数组，`super_admin` / `*` 放行。

- [ ] **Step 1: 写权限测试**

```js
import { describe, it, expect } from 'vitest'
import { hasPermission } from './auth'

describe('hasPermission', () => {
  it('wildcard allows all', () => expect(hasPermission(['*'], 'system:user:add')).toBe(true))
  it('exact match', () => expect(hasPermission(['system:user:list'], 'system:user:add')).toBe(false))
})
```

- [ ] **Step 2: 运行确认失败**

Run: `npx vitest run src/stores/auth.test.js`
Expected: FAIL。

- [ ] **Step 3: 实现 store 与 `hasPermission`**

`hasPermission(perms, needed)` 导出为纯函数；store getter 复用它。`login` 调 `POST /admin/auth/login`，存 `tokenValue` 与用户到 state + localStorage；`loadSession` 并发拉 `/auth/me` 与 `/menu/user-tree`；`logout` 调接口后 `clear()`。

- [ ] **Step 4: 实现路由与守卫**

```js
// 路由表
/login (public)
/ -> AppLayout
  dashboard
  system/:pathMatch(.*)* -> DynamicMenuView
/403
/:pathMatch(.*)* -> 404
```
`beforeEach`：公开路由放行；无 token → `/login?redirect=`；未加载 → `await loadSession()`，失败清会话回登录。

- [ ] **Step 5: 实现 `v-perm` 与 `menu-components.js`**

在 `main.js` 注册 `v-perm` 指令。`menuComponents` 映射 `system/user/index`、`system/role/index`、`system/menu/index`、`system/permission/index` 到对应视图的 `() => import(...)`。

- [ ] **Step 6: 运行测试与构建**

Run: `npx vitest run && npm run build`
Expected: 全部通过。

- [ ] **Step 7: Commit**

```bash
git add RouteGo-admin/src/stores RouteGo-admin/src/router RouteGo-admin/src/directives
git commit -m "feat(admin-ui): add auth store, guards and permission directive"
```

---

### Task 4: 应用外壳（侧栏 / 顶栏 / 动态视图）

**Files:**
- Create: `RouteGo-admin/src/layouts/AppLayout.vue`
- Create: `RouteGo-admin/src/components/AppSidebar.vue`
- Create: `RouteGo-admin/src/components/SidebarItem.vue`
- Create: `RouteGo-admin/src/components/AppHeader.vue`
- Create: `RouteGo-admin/src/components/CommandPalette.vue`
- Create: `RouteGo-admin/src/components/ChangePasswordDialog.vue`
- Create: `RouteGo-admin/src/components/menu-icons.js`
- Create: `RouteGo-admin/src/views/system/DynamicMenuView.vue`
- Create: `RouteGo-admin/src/views/system/PlaceholderView.vue`
- Create: `RouteGo-admin/src/views/error/ForbiddenView.vue`
- Create: `RouteGo-admin/src/views/error/NotFoundView.vue`

**Interfaces:**
- `AppLayout` 提供 `el-container` 结构：侧栏 224px / 折叠 56px，顶栏 48px，内容区 `#F8FAFC`。
- `DynamicMenuView` 读取 `auth.menus` 按 `route.path` 查找节点；无节点 → 404；无权限 → 403；有 `component` 映射 → 异步组件；否则 `PlaceholderView`。

- [ ] **Step 1: 实现侧栏**

深色 `#121212`；品牌区 48px；递归渲染 `menuType = M/C` 的菜单；选中项 `#1F1F1F` 底 + 粉色文字/图标；折叠时只显示图标；底部版本号。禁止单侧粗边框。

- [ ] **Step 2: 实现顶栏**

折叠按钮、面包屑、搜索按钮（显示 `Ctrl K`）、公告 popover（`getPublishedNotices`）、用户下拉（修改密码 / 退出登录）。高度 48px，白底，`border-b border-[#e2e8f0]`。修改密码抽屉调用 `changePassword({ oldPassword, newPassword })`，成功后提示并强制退出登录。

- [ ] **Step 3: 实现命令面板**

`Ctrl+K` 或 `/` 打开；输入过滤 `auth.menus` 扁平列表；↑↓ 选择、Enter 跳转、Esc 关闭；无结果时显示“无匹配页面”。

- [ ] **Step 4: 实现动态视图与占位页**

`DynamicMenuView` 用 `defineAsyncComponent` 渲染映射组件；`PlaceholderView` 显示菜单名 + “该模块将在后续批次开放” + 返回仪表盘按钮，不出现空状态大面积留白。

- [ ] **Step 5: 浏览器验证**

Run: `npm run dev`，用 `admin / Admin123!` 登录；确认侧栏 11 项、4 个已实现模块可进入、其余显示占位页；折叠/展开正常；`Ctrl+K` 可跳转。

- [ ] **Step 6: Commit**

```bash
git add RouteGo-admin/src/layouts RouteGo-admin/src/components RouteGo-admin/src/views
git commit -m "feat(admin-ui): add app shell driven by server menu tree"
```

---

### Task 5: 登录页

**Files:**
- Create: `RouteGo-admin/src/views/login/LoginView.vue`
- Modify: `RouteGo-admin/src/router/index.js`（`/login` 路由 meta.public）

**Interfaces:**
- 调用 `getCaptcha()`、`auth.login()`；成功后 `router.replace(query.redirect || '/dashboard')`。

- [ ] **Step 1: 实现左侧品牌区**

`#121212` 底，宽度 44%，≥1024px 显示；字标 `RouteGo`、副标题“运营控制台”、三条能力说明用细线分隔；右下角装饰用 CSS 网格线，不用渐变。

- [ ] **Step 2: 实现表单**

账号、密码、验证码（图片 + 点击刷新）、记住账号（localStorage）、登录按钮。校验：账号 3–50、密码 8–20、验证码必填。错误用 `el-alert` 紧凑样式显示；加载态禁用按钮。mock 模式下在表单底部显示两个演示账号。

- [ ] **Step 3: 浏览器验证**

Run: `npm run dev`；错误验证码提示拦截；`admin / Admin123!` 登录进入仪表盘；`ops / Ops12345` 登录进入后用户管理只显示“查询/重置”。

- [ ] **Step 4: Commit**

```bash
git add RouteGo-admin/src/views/login RouteGo-admin/src/router
git commit -m "feat(admin-ui): add data-dense login page"
```

---

### Task 6: 仪表盘

**Files:**
- Create: `RouteGo-admin/src/views/dashboard/DashboardView.vue`
- Create: `RouteGo-admin/src/components/KpiCard.vue`
- Create: `RouteGo-admin/src/components/StatusTag.vue`
- Create: `RouteGo-admin/src/components/PageHeader.vue`

**Interfaces:**
- `getDashboardOverview(): { kpis, recentOperations, loginFailures, systemStatus }`。
- `StatusTag` props：`value: 0|1`、`type: 'status'|'result'|'visible'`、`labels?: [string,string]`。

- [ ] **Step 1: 实现 `StatusTag` 与 `PageHeader`**

`StatusTag` 输出 `rounded px-1.5 py-0.5 text-xs`，颜色编码 + 文字。`PageHeader` 左侧标题/说明，右侧操作插槽。

- [ ] **Step 2: 实现 KPI 卡片**

`bg-white border border-[#e2e8f0] rounded px-3 py-2.5`；标签 `text-[10px] uppercase tracking-wide text-[#64748b]`；数值 `text-base font-mono font-semibold`；环比用绿色/红色小字 + 箭头。

- [ ] **Step 3: 实现仪表盘布局**

4 卡 `grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-2`；下方 `grid grid-cols-1 xl:grid-cols-3 gap-2`：左 2 栏最近操作表，右 1 栏异常登录 + 系统状态；底部 4 个快捷入口按钮。无图表库。

- [ ] **Step 4: 浏览器验证**

Run: `npm run dev`；检查 1440/768/390 三档无整体横向溢出；KPI 在手机单列。

- [ ] **Step 5: Commit**

```bash
git add RouteGo-admin/src/views/dashboard RouteGo-admin/src/components
git commit -m "feat(admin-ui): add dense dashboard with kpi and status panels"
```

---

### Task 7: 用户管理

**Files:**
- Create: `RouteGo-admin/src/views/system/user/UserView.vue`
- Create: `RouteGo-admin/src/views/system/user/UserFormDrawer.vue`
- Create: `RouteGo-admin/src/views/system/user/UserRoleDrawer.vue`
- Create: `RouteGo-admin/src/views/system/user/ResetPasswordDialog.vue`
- Create: `RouteGo-admin/src/components/FilterBar.vue`

**Interfaces:**
- `pageUsers({ username, nickname, phone, status, roleId, createTimeStart, createTimeEnd, pageNum, pageSize })`。
- 行内操作：编辑、分配角色、重置密码、启停、删除。

- [ ] **Step 1: 实现筛选与表格**

筛选栏一行：用户名、昵称、手机号、状态、角色、创建时间范围；查询/重置；右侧“新增用户”。表格列见设计 §6.3；ID/手机号/时间 `font-mono`；角色用粉色浅底标签；状态 `StatusTag`。

- [ ] **Step 2: 实现行内操作**

用 `v-perm` 控制；启停/删除用 `ElMessageBox.confirm`；409 提示“数据已被修改，请刷新后重试”。勾选后出现批量工具栏，批量启停逐条调用并汇总失败数。

- [ ] **Step 3: 实现资料抽屉**

新增：账号、密码、昵称、手机号、邮箱、备注、角色多选。编辑：账号只读、不传密码、带 `version`；手机/邮箱掩码值回显且不覆盖。保存后刷新列表。

- [ ] **Step 4: 实现角色抽屉与重置密码**

角色抽屉：全量角色复选，保存调用 `assignUserRoles`；提示“保存后该用户会被踢下线”。重置密码：新密码 8–20 位含字母数字；成功后提示并关闭。

- [ ] **Step 5: 浏览器验证**

Run: `npm run dev`；完成新增 → 编辑 → 分配角色 → 重置密码 → 停用 → 删除全流程；`ops` 账号只能查询。

- [ ] **Step 6: Commit**

```bash
git add RouteGo-admin/src/views/system/user RouteGo-admin/src/components/FilterBar.vue
git commit -m "feat(admin-ui): add user management with inline actions"
```

---

### Task 8: 角色管理

**Files:**
- Create: `RouteGo-admin/src/views/system/role/RoleView.vue`
- Create: `RouteGo-admin/src/views/system/role/RoleFormDrawer.vue`
- Create: `RouteGo-admin/src/views/system/role/RoleMenuDrawer.vue`
- Create: `RouteGo-admin/src/views/system/role/RolePermissionDrawer.vue`
- Create: `RouteGo-admin/src/views/system/role/RoleUsersDrawer.vue`

**Interfaces:**
- `pageRoles({ roleName, roleCode, status, pageNum, pageSize })`；`getRoleMenus(id)` / `assignRoleMenus(id, ids)`；`getRolePermissions(id)` / `assignRolePermissions(id, ids)`；`pageRoleUsers(id, page)`。

- [ ] **Step 1: 实现列表与筛选**

列：ID、名称、编码 mono、排序、用户数 mono、状态、创建时间、操作。内置角色显示“内置”蓝色标签；删除在 `builtIn` 或 `userCount > 0` 时禁用。

- [ ] **Step 2: 实现新增/编辑抽屉**

名称、编码（编辑只读）、排序、状态、备注；编辑带 `version`；409 提示刷新。

- [ ] **Step 3: 实现菜单授权抽屉**

`el-tree` `show-checkbox` `node-key="id"`，数据来自 `getMenuTree()`，回显 `getRoleMenus`；保存全量替换。父节点半选按 Element Plus 规则提交 `getCheckedKeys()` + `getHalfCheckedKeys()`。

- [ ] **Step 4: 实现权限点授权抽屉**

`getGroupedPermissions()` 按模块分组；每组一个标题 + 复选网格；全选/清空；保存全量替换。

- [ ] **Step 5: 实现角色用户抽屉**

打开时 `pageRoleUsers`；紧凑表格，只读；显示用户名、昵称、状态、最后登录。

- [ ] **Step 6: 浏览器验证**

Run: `npm run dev`；给角色分配菜单与权限后重新打开回显一致；内置角色删除按钮禁用。

- [ ] **Step 7: Commit**

```bash
git add RouteGo-admin/src/views/system/role
git commit -m "feat(admin-ui): add role management with menu and permission assignment"
```

---

### Task 9: 菜单管理

**Files:**
- Create: `RouteGo-admin/src/views/system/menu/MenuView.vue`
- Create: `RouteGo-admin/src/views/system/menu/MenuFormDrawer.vue`

**Interfaces:**
- `getMenuTree()`、`getMenu(id)`、`createMenu`、`updateMenu`、`deleteMenu`、`setMenuStatus`、`setMenuVisible`。

- [ ] **Step 1: 实现树形表格**

`el-table row-key="id" :tree-props="{ children: 'children' }"`；列：名称 + 图标、类型标签、路径 mono、组件 mono、权限标识 mono、排序、显示、状态、操作。默认全部展开。

- [ ] **Step 2: 实现行内开关与操作**

显示/隐藏、启用/停用使用 `el-switch` 行内切换；新增子项、编辑、删除。删除在 `childCount > 0` 时禁用。

- [ ] **Step 3: 实现表单抽屉**

上级菜单 `el-tree-select`（不能选自己或后代）、类型 M/C/F、名称、路径、组件、权限标识、图标（下拉选择已知图标名）、排序、显示、状态。编辑带 `version`。

- [ ] **Step 4: 浏览器验证**

Run: `npm run dev`；菜单树层级正确；新增子菜单后侧栏实时更新；把父级改到自己下面被拦截。

- [ ] **Step 5: Commit**

```bash
git add RouteGo-admin/src/views/system/menu
git commit -m "feat(admin-ui): add menu tree management"
```

---

### Task 10: 权限点管理

**Files:**
- Create: `RouteGo-admin/src/views/system/permission/PermissionView.vue`
- Create: `RouteGo-admin/src/views/system/permission/PermissionFormDrawer.vue`

**Interfaces:**
- `pagePermissions({ permissionName, permissionCode, module, status, pageNum, pageSize })`、`getGroupedPermissions()`。

- [ ] **Step 1: 实现列表视图**

筛选：名称、编码、模块、状态；视图切换“列表 / 分组”。列：ID、名称、权限码 mono、模块标签、说明、状态、创建时间、操作。

- [ ] **Step 2: 实现分组视图**

按模块渲染区块标题 + 紧凑表格，不使用嵌套卡片；每个模块显示权限点数量。

- [ ] **Step 3: 实现表单抽屉**

新增：名称、权限码、模块、说明、状态；编辑时权限码只读；带 `version`（如后端无版本则省略）。删除在 `roleCount > 0` 时禁用。

- [ ] **Step 4: 浏览器验证**

Run: `npm run dev`；分组视图与列表视图数据一致；新增后编码重复被拦截。

- [ ] **Step 5: Commit**

```bash
git add RouteGo-admin/src/views/system/permission
git commit -m "feat(admin-ui): add permission management with module grouping"
```

---

### Task 11: Figma 规格与整体验收

**Files:**
- Create: `docs/design/figma-spec.md`
- Modify: `docs/superpowers/specs/2026-11-04-routego-admin-ui-design.md`（如实现中调整）

**Interfaces:**
- 无代码接口；产出设计交付物与验收记录。

- [ ] **Step 1: 写 Figma 规格**

包含：Color Styles（`rg/ink`、`rg/accent`、`rg/surface`、`rg/line`、状态四色）、Text Styles（H1/H2/body/table-header/mono）、组件集（Button 3 形态 × 3 尺寸、Input、Select、Tag、Table Row、KPI Card、Sidebar Item、Drawer、Filter Bar）、三种 Frame 尺寸与 Auto Layout 参数。

- [ ] **Step 2: 运行构建**

Run: `npm run build`
Expected: 成功，无 Tailwind 警告。

- [ ] **Step 3: 全流程验收**

用 Playwright 在 1440×900、768×1024、390×844 截取登录、仪表盘、用户、角色、菜单、权限 6 页；确认无整体横向溢出、无禁用 class、状态标签颜色正确、行内操作可见。

- [ ] **Step 4: 自检清单**

逐条核对 `docs/design/style-contract.md` 的交付前自检，记录未通过项并修复。

- [ ] **Step 5: Commit**

```bash
git add docs/design/figma-spec.md docs/superpowers/specs
git commit -m "docs(admin-ui): add figma spec and acceptance checklist"
```
