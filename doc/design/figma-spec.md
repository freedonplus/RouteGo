# RouteGo Admin — Figma 构建规格

当前环境的 Figma 连接为只读，无法直接写入画布。本文件给出可直接照着搭组件与页面的参数；实现代码在 `RouteGo-admin/`，两边以本文件为对齐基准。

## 1. 画板

| 名称 | 尺寸 | 用途 |
| --- | --- | --- |
| Desktop / 1440 | 1440 × 900 | 主设计稿 |
| Tablet / 768 | 768 × 1024 | 折叠侧栏 |
| Mobile / 390 | 390 × 844 | 抽屉侧栏、表格横滚 |

命名：`RG / Page / <page>`、`RG / Component / <name>`、`RG / Variant / <name>`。

## 2. Color Styles

| Style | 值 | 用途 |
| --- | --- | --- |
| `rg/ink` | `#121212` | 侧栏底、主文字、主按钮文字 |
| `rg/ink-soft` | `#1F1F1F` | 侧栏选中/悬停底 |
| `rg/ink-line` | `#2A2A2A` | 侧栏分隔线 |
| `rg/accent` | `#E89AAF` | 主按钮底、选中态、关键数字 |
| `rg/accent-hover` | `#D97F98` | 主按钮悬停 |
| `rg/accent-active` | `#C96F88` | 主按钮按下 |
| `rg/accent-ink` | `#A8435F` | 浅底上的粉色文字（AA） |
| `rg/accent-soft` | `#FDF1F5` | 粉色浅底标签/选中行 |
| `rg/surface` | `#FFFFFF` | 卡片、表头 |
| `rg/surface-alt` | `#F8FAFC` | 页面底、斑马行、筛选栏 |
| `rg/line` | `#E2E8F0` | 边框、分隔线 |
| `rg/text` | `#1E293B` | 正文 |
| `rg/text-muted` | `#64748B` | 次要文字、表头 |
| `rg/text-faint` | `#94A3B8` | 弱化文字 |

状态色（必须同时带文字）：

| Style | 文字 | 底 |
| --- | --- | --- |
| `rg/status/success` | `#15803D` | `#ECFDF3` |
| `rg/status/warning` | `#B45309` | `#FFFBEB` |
| `rg/status/danger` | `#B91C1C` | `#FEF2F2` |
| `rg/status/info` | `#1D4ED8` | `#EFF6FF` |
| `rg/status/muted` | `#475569` | `#F1F5F9` |

## 3. Text Styles

| Style | 字体 | 字号/行高 | 字重 | 其他 |
| --- | --- | --- | --- | --- |
| `rg/h1` | 系统无衬线 | 16 / 24 | 600 | letter-spacing -0.01em |
| `rg/h2` | 系统无衬线 | 14 / 20 | 600 | letter-spacing -0.01em |
| `rg/body` | 系统无衬线 | 12 / 18 | 400 | |
| `rg/body-strong` | 系统无衬线 | 12 / 18 | 500 | |
| `rg/label` | 系统无衬线 | 10 / 14 | 500 | uppercase，tracking 0.06em |
| `rg/mono` | JetBrains Mono / Consolas | 11 / 16 | 400 | 数字、ID、权限码、路径 |
| `rg/mono-strong` | JetBrains Mono / Consolas | 12 / 18 | 600 | KPI 数值 |

禁止使用 Inter / Roboto / Geist。

## 4. 组件

所有组件：Auto Layout，圆角 4，默认无阴影，过渡 150ms。

### Button

- 尺寸：`sm` 高 26、左右 10、字号 12；`xs` 高 24、左右 8、字号 11。
- 变体：
  - `primary`：底 `rg/accent`，文字 `rg/ink`，hover `rg/accent-hover`，按下 `rg/accent-active`。
  - `secondary`：底白，边框 `rg/line`，文字 `rg/text-muted`，hover 底 `rg/surface-alt`。
  - `link`：无底无框，文字 `rg/accent-ink`；danger link 文字 `#B91C1C`。
  - `danger`：仅用于确认框主按钮，底 `#B91C1C`，白字。
- 所有变体带 `transition-colors`、`focus-visible` 1px `#3B82F6` ring。

### Input / Select

- 高 30，左右内边距 8，边框 1 `rg/line`，圆角 4，文字 12。
- 聚焦：1px `#3B82F6` 内描边。
- Placeholder：`rg/text-faint`。
- 表单标签：`rg/label`，与输入框间距 4。

### Status Tag

- 高 18，左右 6，圆角 3，字号 11，带 4px 圆点。
- 变体：success / warning / danger / info / muted，颜色见状态色。

### KPI Card

- Auto Layout 垂直，间距 4，内边距 `12 × 10`。
- 结构：Label（10/14）→ 数值行（mono 16/20 + 单位 10 + 环比 10）→ 说明（10/14，`rg/text-faint`）。
- 底白，边框 `rg/line`，圆角 4。

### Table

- 表头高 30，底 `rg/surface-alt`，文字 `rg/label`，下边框 `rg/line`。
- 行高最小 34（`py-2`），字号 12，行悬停 `rg/surface-alt`，斑马行 `rg/surface-alt`。
- 单元格左右内边距 8；数字/ID/时间用 mono。
- 行内操作：link 按钮，间距 4，始终可见，不放入下拉。

### Sidebar

- 宽 224（折叠 56），底 `rg/ink`。
- 品牌区高 48，粉色 20×20 方块 + 白色字标。
- 分组标题：10/14，uppercase，`#6B7280`。
- 导航项高 32，圆角 4，左内边距 8；选中底 `#1F1F1F`、文字 `rg/accent`；默认文字 `#CBD5E1`。
- 底部版本：mono 10，`#6B7280`。

### Drawer

- 宽 400 / 460 / 480（按表单密度）。
- 头部高 44，底白，下边框 `rg/line`，标题 13/18 semibold。
- 内容内边距 `12 × 16`；底部操作栏高 44，上边框 `rg/line`。

### Filter Bar

- 底白，边框 `rg/line`，圆角 4，内边距 `8 × 12`。
- 字段纵向：Label 10/14 + 控件；间距 8；操作按钮右对齐。

## 5. 页面框架

### 登录

- Desktop：左 44% `rg/ink` 品牌区，右 56% `rg/surface-alt` 表单区；表单最大宽 360。
- 品牌区：48 高字标栏、标题 16、三条说明、底部版本；不用渐变。
- 表单：账号、密码、验证码（120×36）、记住账号、主按钮全宽。
- Mobile：隐藏品牌区，顶部显示字标，表单占满。

### 应用外壳

- Sidebar 224 / 56；Header 48；内容区 `rg/surface-alt`，内边距 `12 × 16`。
- Header：折叠按钮 28、面包屑 12、搜索框（`Ctrl K`）、公告铃铛、头像 20 + 昵称 + 下拉。

## 6. 响应式断点

| 断点 | 侧栏 | 表格 | KPI |
| --- | --- | --- | --- |
| ≥ 1280 | 常驻 224 | 全列 | 4 列 |
| 1024–1279 | 常驻 224 / 可折叠 56 | 全列 | 2 列 |
| 768–1023 | 抽屉 | 横向滚动 | 2 列 |
| < 768 | 抽屉 | 横向滚动 | 1 列 |

## 7. 交付检查

- 颜色、字号、间距全部使用本文件的 Style/Token，不出现裸值。
- 组件使用 Variant 而非复制图层。
- 状态标签带文字；主按钮文字为 `rg/ink`。
- 无 `rounded-xl+`、`shadow-lg+`、`p-6+`、装饰渐变。
- Desktop / Tablet / Mobile 三个 Frame 均无页面级横向溢出（表格自身滚动除外）。
