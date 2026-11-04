# RouteGo Admin UI 设计方案（第一批）

日期：2026-11-04
状态：待确认
范围：登录 + 仪表盘 + RBAC（用户 / 角色 / 菜单 / 权限点）

## 1. 目标

把 `doc/admin-api.md` 描述的 13 个后台模块，先落地为**可运行的 Vue 3 + Element Plus 管理端**第一批页面，视觉严格遵循 Data Dense（数据密集）风格，品牌色使用藤紫黑 `#121212` 与烟灰粉 `#E89AAF`。

- 目标：可启动、可点击、可增删改查（mock 数据），视觉与交互可作为后续 9 个模块的模板。
- 非目标：本批次不做真实后端联调、不做文件上传/日志/字典/参数/公告页面、不做国际化。
- 反目标：大圆角、大留白、装饰性渐变/阴影、把常用操作藏进下拉菜单。

## 2. 约束

- 技术栈：Vue 3.5 + Vite 8 + Element Plus + Vue Router 4 + Pinia + Axios + Tailwind CSS 4。
- 样式规则以用户提供的 Data Dense Hard Prompt 为最高优先级；冲突时禁止项优先。
- 主题：浅色内容区（`bg-white` / `#f8fafc`）+ 深色侧栏 `#121212` + 烟灰粉 `#E89AAF` 强调。
- 所有页面默认 mock 数据，切换 `VITE_USE_MOCK=true`；API 层与真实后端同构，后续去掉 mock 即可联调。
- 不使用 Inter / Roboto / Geist；中文正文用系统字体栈，数字/ID/权限码用等宽字体。

## 3. 设计 Token

### 3.1 颜色

| 角色 | 值 | 用途 |
| --- | --- | --- |
| ink | `#121212` | 侧栏底色、主文字、主按钮文字 |
| ink-soft | `#1f1f1f` | 侧栏 hover / 选中底 |
| ink-line | `#2a2a2a` | 侧栏分隔线 |
| accent | `#E89AAF` | 主按钮底、选中标识、关键数字 |
| accent-hover | `#D97F98` | 主按钮 hover |
| accent-ink | `#A8435F` | 浅底上的粉色文字（保证 AA 对比度） |
| accent-soft | `#FDF1F5` | 粉色浅底标签 |
| surface | `#FFFFFF` | 卡片、表头 |
| surface-alt | `#F8FAFC` | 页面底、斑马行 |
| line | `#E2E8F0` | 所有边框/分隔线 |
| text | `#1E293B` | 正文 |
| text-muted | `#64748B` | 次要文字、表头 |
| text-faint | `#94A3B8` | 弱化文字 |

语义状态（不单靠颜色传达，始终带文字）：

| 语义 | 文字色 | 底色 |
| --- | --- | --- |
| 启用 / 成功 | `#15803D` | `#ECFDF3` |
| 待处理 / 警告 | `#B45309` | `#FFFBEB` |
| 异常 / 错误 | `#B91C1C` | `#FEF2F2` |
| 信息 / 中性 | `#1D4ED8` | `#EFF6FF` |

### 3.2 字体与字号

- 正文：`-apple-system, "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif`
- 等宽：`"JetBrains Mono", "SFMono-Regular", Consolas, "Liberation Mono", monospace`
- 页面标题 `text-base`（16px，semibold）；区块标题 `text-sm`（14px，semibold）；正文/表格 `text-xs`（12px）；表头 `text-[10px]` 大写 `tracking-wide`。
- 数字、ID、权限码、路径、时间戳一律 `font-mono`。

### 3.3 间距 / 圆角 / 阴影

- 间距 4px 递增：`2 / 4 / 6 / 8 / 12 / 16 / 24`，页面区块 `gap-3`，页面内 `gap-2`，控件内 `gap-1`。
- 圆角一律 `rounded`（4px）；卡片、输入框、按钮、标签同族。
- 阴影：默认 `shadow-none`；仅吸顶操作栏 `shadow-sm`；禁止 `shadow-lg` 及以上。
- 动效：`transition-colors duration-150`；`active:scale-95`；提供 `prefers-reduced-motion` 降级。

## 4. 信息架构与导航

导航由服务端菜单树驱动：登录后调用 `GET /admin/system/menu/user-tree`，只渲染 `menuType = M/C` 的节点，侧栏层级与菜单管理页保持一致。

```text
/login                         登录（公开）
/                               AppLayout（需登录）
  /dashboard                    仪表盘（内置，不依赖菜单）
  /system/user                  用户管理
  /system/role                  角色管理
  /system/menu                  菜单管理
  /system/permission            权限点管理
  /system/*                     其余菜单节点 → 建设中占位页
/403                            无权限
/:pathMatch(.*)*                404
```

- 组件映射：菜单的 `component` 字符串（如 `system/user/index`）通过 `import.meta.glob` 映射到 `views/`；找不到映射时渲染 `PlaceholderView`，显示“模块建设中”和菜单名，不产生死链。
- 首批 4 个模块之外的路由（字典、参数、公告、文件、日志）全部走占位页，后续批次替换即可。
- 侧栏底部显示版本号与构建时间；菜单管理页的修改在 mock 模式下会实时反映到侧栏（菜单 store 共享）。
- 权限：路由 `meta.perms` 控制访问；按钮级用 `v-perm="'system:user:add'"` 指令，`super_admin` / `*` 全放行。

## 5. 全局框架

- 侧栏 224px，`#121212`；折叠后 56px，仅图标。底部显示版本与当前环境。
- 侧栏品牌区：RouteGo 字标 + 粉色小方块；导航项 32px 高，选中态 `#1F1F1F` 底 + 粉色图标/文字，不使用单侧粗边框。
- 顶栏 48px 白底细线：折叠按钮、面包屑、全局搜索（`/` 或 `Ctrl+K` 聚焦）、公告入口、用户菜单（修改密码 / 退出登录直接可见，不隐藏核心操作）。
- 内容区：`#F8FAFC` 底，`px-4 py-3`；页面头部含标题、说明、主操作按钮。

## 6. 页面规格

### 6.1 登录页 `/login`

- 左右分栏：左侧 44% `#121212` 品牌区（字标、标语、三条能力说明、细网格纹理），右侧登录表单。
- 表单：账号、密码、图形验证码（图片 + 刷新）、记住账号、登录按钮。
- 校验：账号 3–50 位、密码 8–20 位、验证码必填；错误用行内提示，锁定/限流用顶部条。
- 主按钮：粉色底 + `#121212` 文字；`Enter` 提交；加载态禁用。

### 6.2 仪表盘 `/dashboard`

数据契约：`GET /admin/dashboard/overview`（已实现）。返回 `{ kpis, recentOperations, loginFailures, systemStatus }`，指标来自登录日志、操作日志与用户表。仪表盘不依赖 `operlog` / `loginlog` 权限，避免非超级管理员 403。

- 顶部 4 个紧凑 KPI 卡片（每卡：标签、主数值 `font-mono`、环比、说明）：今日登录、在线用户、管理员总数、7 日异常登录。
- 主区两栏（`lg:grid-cols-3`）：
  - 左 2 栏：最近操作日志表（时间、操作人、动作、结果、耗时），行内“查看”。
  - 右 1 栏：登录失败 / 锁定账号列表 + 系统状态条（服务、缓存、存储、最后同步时间）。
- 底部：快捷入口（新增用户 / 分配角色 / 菜单配置 / 权限点），4 个按钮直接可见。

### 6.3 用户管理 `/system/user`

- 筛选栏：用户名、昵称、手机号、状态、角色、创建时间范围；查询 / 重置 / 新增用户。
- 表格列：ID（mono）、用户（头像 + 昵称 + 账号 mono）、手机号（脱敏 mono）、邮箱、角色标签、状态、最后登录（mono）、创建时间（mono）、操作。
- 数据扩展：mock 的 `SysUserVO` 额外返回 `roleNames: string[]`；真实后端对应 VO 需要补该字段（见 §12）。
- 编辑抽屉拆分为三个独立入口：资料（`PUT /user/{id}`，带 `version`）、分配角色（`PUT /user/{id}/roles`）、重置密码（`PUT /user/{id}/password`）。不做“一次保存多接口”的混合事务，避免部分成功。
- 批量启用/停用：前端循环调用单条 `PUT /{id}/status`；失败项汇总提示。后端若后续提供批量接口再替换。
- 行内操作始终可见：编辑、分配角色、重置密码、启用/停用、删除。高风险操作二次确认。
- 新增/编辑抽屉：账号（仅新增可填）、密码（仅新增）、昵称、头像 URL、手机号、邮箱、备注、角色多选；编辑时回传 `version`。
- 批量：勾选后显示批量启用/停用；分页默认 10，可选 20/50/100。

### 6.4 角色管理 `/system/role`

- 筛选：角色名称、编码、状态。
- 表格列：ID、角色名称、编码（mono）、排序、用户数、状态、创建时间、操作。
- 数据扩展：mock 的 `SysRoleVO` 额外返回 `userCount`；真实后端对应 VO 需要补该字段（见 §12）。
- 删除按钮启用条件依赖 `userCount === 0` 且非内置角色，避免只靠服务端 400 兜底。
- 操作：编辑、菜单权限、数据权限（权限点）、查看用户、启用/停用、删除。
- “菜单权限”抽屉：菜单树复选（目录/菜单/按钮），支持全选/展开/半选；“权限点”抽屉：按模块分组的复选清单。
- 内置角色（`admin` / `super_admin`）显示“内置”标签，删除按钮禁用。

### 6.5 菜单管理 `/system/menu`

- 树形表格（`el-table` tree）：菜单名称 + 图标、类型标签（目录/菜单/按钮）、路由路径（mono）、组件（mono）、权限标识（mono）、排序、显示、状态、操作。
- 操作：新增子项、编辑、启用/停用、显示/隐藏、删除（有子级或角色引用时禁用）。
- 新增/编辑抽屉：上级菜单（树选择）、类型、名称、路径、组件、权限标识、图标、排序、显示、状态、`version`。

### 6.6 权限点 `/system/permission`

- 顶部：模块筛选 + 名称/编码搜索 + 状态；视图切换“列表 / 按模块分组”。
- 列表列：ID、权限名称、权限码（mono）、模块标签、说明、状态、创建时间、操作。
- 分组视图：模块卡片内紧凑表格，避免嵌套卡片。
- 新增/编辑抽屉：名称、权限码（创建后只读）、模块、说明、状态。

## 7. 技术架构

```text
src/
  api/            request.js（axios 统一响应/401/错误）、auth.js、system.js
  mock/           index.js（axios adapter）、data.js（内存数据集，CRUD 可持久到内存）
  stores/         auth.js（token、用户、权限、菜单）
  router/         index.js（守卫、权限路由）
  directives/     permission.js
  layouts/        AppLayout.vue
  components/     AppSidebar / AppHeader / PageHeader / FilterBar / StatusTag / CrudDrawer
  views/          login / dashboard / system/{user,role,menu,permission}
  utils/          format.js（时间、脱敏、文件大小）
  style.css       token + Element Plus 覆盖 + 工具类
```

- Mock：`src/mock/index.js` 作为 axios adapter，按 method + url 匹配；数据放 `data.js`，支持分页、筛选、CRUD、409 版本冲突模拟。
- 鉴权：登录后保存 `tokenValue` 与用户信息；请求拦截器附带 `Authorization: Bearer <token>`；401 清理并跳登录。
- Element Plus：通过 CSS 变量改主色与圆角，表格/按钮密度统一在 `style.css` 覆盖。
- 不放 Tailwind 的 `preflight` 之外的全局重置，避免破坏 Element Plus；类名仍按风格令牌使用。

## 8. 响应式与无障碍

- ≥1280：侧栏常驻，表格全列。
- 768–1279：侧栏折叠为 56px，次要列隐藏或合并。
- <768：侧栏变抽屉；KPI 单列；筛选栏纵向堆叠；表格横向滚动。
- 焦点：所有交互元素 `focus-visible:ring-1 ring-[#3b82f6]`；图标按钮带 `aria-label`。
- 对比度：主按钮使用深色文字；粉色文字统一用 `#A8435F`；状态标签同时带文字。
- 动效：尊重 `prefers-reduced-motion`。

## 9. Figma 落地说明

当前环境 Figma 连接为只读，无法直接写入画布。交付时另外提供 `docs/design/figma-spec.md`：

- 颜色样式（Color Styles）与文字样式（Text Styles）命名表。
- 组件集：Button（primary/secondary/ghost/danger，3 尺寸）、Input、Select、Tag、Table Row、KPI Card、Sidebar Item、Drawer、Filter Bar。
- 页面 Frame：1440×900 桌面、768 平板、390 手机三档。
- Auto Layout 参数与间距对应表，直接照着建即可。

## 10. 验收清单

- [ ] 无 `rounded-xl` / `rounded-2xl` / `shadow-lg` / `p-6` / `p-8` / `text-xl` 以上（页面标题除外时也不使用）。
- [ ] 表格行 `py-2` 以内、`text-xs`；数字与 ID `font-mono`。
- [ ] 状态标签颜色编码且带文字；按钮 `px-2.5 py-1 text-xs rounded`。
- [ ] 白底 + `#f8fafc` 斑马行；边框 `#e2e8f0`；无渐变、无装饰阴影。
- [ ] 常用操作行内可见，不藏下拉。
- [ ] 手机 / 平板 / 桌面无横向溢出（表格自身滚动除外）。
- [ ] `npm run dev` 可启动，登录 → 仪表盘 → 四个模块全流程可走通。
- [ ] `npm run build` 通过。

## 11. 交付物

1. `RouteGo-admin/` 可运行的 Vue 3 + Element Plus 应用（上述 6 个页面 + 全局框架）。
2. Mock 数据与 API 层，后续可切换真实后端。
3. `docs/design/figma-spec.md`：Figma 组件与样式规格。
4. `docs/design/style-contract.md`：Data Dense 硬性风格契约（禁止项 / 必须项 / 自检清单）。
5. 本设计文档。

## 12. 后端扩展（已完成）

| 接口 | 变更 | 状态 |
| --- | --- | --- |
| `GET /admin/dashboard/overview` | 新增仪表盘聚合接口 | 已实现 |
| `GET /admin/system/user/page` | `SysUserVO` 增加 `roleNames` | 已实现（批量查询） |
| `GET /admin/system/role/page` | `SysRoleVO` 增加 `userCount` | 已实现 |
| `GET /admin/system/menu/tree` | 节点增加 `childCount`、`roleCount` | 已实现 |
| `GET /admin/system/permission/page` | `SysPermissionVO` 增加 `roleCount` | 已实现 |
| `PUT /admin/system/user/status/batch` | 可选批量启停 | 未实现，前端循环单条调用 |

## 13. Builder 决策（已确定，不再询问）

- 全局搜索只检索路由/页面名称并跳转，不做跨实体搜索（无对应 API）。
- 顶栏公告入口使用 `GET /admin/system/notice/published` 弹出面板；公告页本身仍属后续批次。
- 菜单/权限点的状态、显示/隐藏通过行内开关调用独立接口，不放进编辑抽屉。
- 角色删除、菜单删除、权限点删除的禁用条件在 mock 中计算；真实后端仍需服务端校验兜底。
- 错误提示统一：401 跳登录，403 页内提示，409 提示“数据已被修改，请刷新”，429 提示稍后重试。
