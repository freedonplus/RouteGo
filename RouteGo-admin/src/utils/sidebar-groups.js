import { reactive } from 'vue'

const STORAGE_KEY = 'routego.sidebar.groups'

/**
 * 根菜单（目录）展开状态。
 * 默认全部展开；用户点击收起后写入 localStorage，刷新、切页、重登都保持。
 * 值为 false 表示该目录被收起，其余情况（含首次访问）都视为展开。
 */
const state = reactive({ collapsed: load() })

function load() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    const parsed = raw ? JSON.parse(raw) : {}
    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch {
    return {}
  }
}

function persist() {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(state.collapsed))
  } catch {
    // 隐私模式下不可写，忽略即可，不影响本次会话内的交互
  }
}

export function isGroupCollapsed(id) {
  return state.collapsed[String(id)] === true
}

export function isGroupOpen(id) {
  return !isGroupCollapsed(id)
}

export function toggleGroup(id) {
  const key = String(id)
  state.collapsed[key] = !isGroupCollapsed(key)
  persist()
}

export function openGroup(id) {
  const key = String(id)
  if (isGroupCollapsed(key)) {
    state.collapsed[key] = false
    persist()
  }
}

/** 退出登录或切换会话时可整体恢复默认（全部展开） */
export function resetGroups() {
  state.collapsed = {}
  persist()
}
