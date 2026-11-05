const EMPTY = '—'

export function isMasked(value) {
  return typeof value === 'string' && value.includes('*')
}

export function maskPhone(value) {
  if (!value) return EMPTY
  const text = String(value)
  if (text.length < 7) return text
  return `${text.slice(0, 3)}****${text.slice(-4)}`
}

export function maskEmail(value) {
  if (!value) return EMPTY
  const text = String(value)
  const [name, domain] = text.split('@')
  if (!domain || !name) return text
  return `${name.slice(0, 1)}***@${domain}`
}

export function formatDate(value) {
  const text = formatDateTime(value)
  return text === EMPTY ? text : text.slice(0, 10)
}

export function formatDateTime(value) {
  if (!value) return EMPTY
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(
    date.getHours()
  )}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

/**
 * 金额格式化：默认保留两位小数、不带千分位，负数原样保留。
 * 后端用 BigDecimal 序列化成数字，前端只做展示，不参与计算。
 */
export function formatMoney(value, options = {}) {
  const { digits = 2, prefix = '¥' } = options
  if (value === null || value === undefined || value === '') return EMPTY
  const amount = Number(value)
  if (!Number.isFinite(amount)) return String(value)
  const text = amount.toFixed(digits)
  return prefix ? `${prefix}${text}` : text
}

export function formatNumber(value) {
  if (value === null || value === undefined || value === '') return EMPTY
  const num = Number(value)
  if (!Number.isFinite(num)) return String(value)
  return num.toLocaleString('zh-CN')
}

export function formatFileSize(bytes) {
  const size = Number(bytes)
  if (!Number.isFinite(size) || size < 0) return EMPTY
  if (size < 1024) return `${size} B`
  const units = ['KB', 'MB', 'GB', 'TB']
  let value = size / 1024
  let unit = 0
  while (value >= 1024 && unit < units.length - 1) {
    value /= 1024
    unit += 1
  }
  return `${value.toFixed(1)} ${units[unit]}`
}

export function formatRelative(value) {
  if (!value) return EMPTY
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  const diff = Date.now() - date.getTime()
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour
  if (diff < minute) return '刚刚'
  if (diff < hour) return `${Math.floor(diff / minute)} 分钟前`
  if (diff < day) return `${Math.floor(diff / hour)} 小时前`
  if (diff < 7 * day) return `${Math.floor(diff / day)} 天前`
  return formatDateTime(date)
}

/** 后端返回的文件访问地址（相对路径）补上 API 前缀，供 <img>/el-image 直接使用 */
export function resolveFileUrl(url) {
  if (!url) return ''
  if (/^(https?:)?\/\//.test(url) || url.startsWith('data:')) return url
  const base = import.meta.env.VITE_API_BASE_URL || '/api'
  return base + url
}
