/**
 * 全局错误收集（轻量版）。
 *
 * 职责：去重聚合 + 完整堆栈输出，供开发排查；远端上报留了开关，
 * 但**后端目前没有对应接收接口**（后端侧需求：POST /system/client-log），
 * 开启前先由后端提供接口，避免 404 噪音。
 */
const MAX_RECORDS = 50
const records = []
const seen = new Map()
/** 后端提供 /system/client-log 后改为 true 即完成接入 */
// eslint-disable-next-line no-unused-vars -- 后端接口就绪后启用
const REMOTE_ENABLED = false

function normalize(err) {
  if (err instanceof Error) {
    return { message: err.message, stack: err.stack || '', name: err.name }
  }
  return { message: String(err ?? ''), stack: '', name: 'NonError' }
}

export function reportError(err, { info = '', component = '' } = {}) {
  const { message, stack, name } = normalize(err)
  // 同一错误 60 秒内只记一条，防止渲染异常在轮询里刷屏
  const key = `${name}:${message}`
  const now = Date.now()
  const last = seen.get(key) || 0
  if (now - last < 60000) return
  seen.set(key, now)

  const record = {
    time: new Date().toISOString(),
    name,
    message,
    stack,
    info,
    component,
    page: window.location.pathname,
    version: typeof __APP_VERSION__ === 'string' ? __APP_VERSION__ : '',
  }
  records.push(record)
  if (records.length > MAX_RECORDS) records.shift()

   
  console.error(`[RouteGo] ${info || 'error'} @ ${component || 'unknown'}:`, err)
}

/** 排障用：控制台取最近错误列表 */
export function getRecentErrors() {
  return [...records]
}
