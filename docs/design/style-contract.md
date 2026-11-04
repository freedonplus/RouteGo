# Data Dense 风格契约（项目内权威版本）

本文件是 RouteGo Admin 的硬性风格约束，优先级高于组件库默认值。生成或修改页面前逐条核对；冲突时以「绝对禁止」为最高优先级。

## 主题覆盖

- 品牌主色：藤紫黑 `#121212`（侧栏、主文字、主按钮文字）
- 品牌强调：烟灰粉 `#E89AAF`（主按钮底、选中态、关键数字）
- 粉色文字在浅底上使用 `#A8435F`（满足 WCAG AA）
- 内容区保持白底 `#FFFFFF` + 交替行 `#F8FAFC`

## 绝对禁止

- `rounded-xl` / `rounded-2xl` / `rounded-3xl` / `rounded-full`
- `shadow-lg` / `shadow-xl` / `shadow-2xl`
- `p-6` / `p-7` / `p-8` / `p-9`
- `text-lg` / `text-xl` / `text-2xl` 作为表格与正文数据
- `bg-gradient-*`、渐变文字、玻璃态
- 紫色到蓝色渐变、单侧粗边框装饰
- 把常用操作藏进下拉菜单
- 大面积空白、嵌套卡片
- Inter / Roboto / Geist 作为正文字体
- bounce / elastic 缓动

## 必须遵守

- 表格行高 `py-2` 以内，字号 `text-xs` 或 `text-sm`
- 表头 `text-[10px] uppercase tracking-wide text-[#64748b] font-medium`
- 数字、ID、权限码、路径、时间戳使用 `font-mono`
- 状态标签：`rounded px-1.5 py-0.5 text-xs`，颜色编码且带文字（绿=启用、黄=待处理、红=异常、蓝=信息）
- 按钮：`px-2.5 py-1 text-xs rounded transition-colors`
- 卡片：`bg-white border border-[#e2e8f0] rounded`
- 输入框：`border border-[#e2e8f0] rounded focus:ring-1 focus:ring-[#3b82f6]`
- 间距 4px 递增：`gap-1 / gap-2 / gap-3`
- 分隔线 `border-[#e2e8f0]`
- 交互：`hover:bg-[#f8fafc]`、`active:scale-95`、`transition-colors duration-150`

## Token 字典

```text
边框:  border / border-[#e2e8f0] / rounded
阴影:  shadow-none | shadow-sm（仅吸顶栏）
字体:  font-semibold tracking-tight（标题）/ font-normal（正文）/ font-mono（数据）
字号:  H1 text-base / H2 text-sm / 正文 text-xs / 表头 text-[10px]
间距:  区块 py-3 / 容器 px-4 / 卡片 px-3 py-2.5 / 控件 gap-1~2
颜色:  ink #121212 / accent #E89AAF / accent-ink #A8435F
       正文 #1E293B / 次要 #64748B / 弱化 #94A3B8
       底 #FFFFFF / 辅底 #F8FAFC / 线 #E2E8F0
```

## 交付前自检

- [ ] 没有禁用 class（圆角、阴影、大间距、大字号）
- [ ] 表格 `py-2` 以内、`text-xs`，数字与 ID `font-mono`
- [ ] 状态标签颜色编码 + 文字
- [ ] 按钮 `px-2.5 py-1 text-xs rounded`
- [ ] 白底 + `#f8fafc` 交替行，边框 `#e2e8f0`
- [ ] 无渐变、无装饰阴影、无嵌套卡片
- [ ] 常用操作行内可见
- [ ] 手机 / 平板 / 桌面无横向溢出
- [ ] 正文对比度 ≥ 4.5:1
- [ ] 动效有 `prefers-reduced-motion` 降级
