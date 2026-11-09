import { createStreamTicket } from '@/api/operations'
import { USE_MOCK } from '@/api/request'
import { API_BASE_URL } from '@/utils/api-base'

/**
 * 客服消息推送客户端。
 *
 * 两个坑绕不开，这里一并处理：
 *
 * 1. EventSource 无法携带 Authorization 头，所以先换一张一次性票据再建连；
 * 2. 票据是一次性的，而 EventSource 断线后会拿同一个 URL 自动重试，
 *    重试必然 401 —— 表现为疯狂刷请求。因此必须手动接管 onerror：
 *    先 close，重新换票，再按退避策略重连。
 */
const RETRY_DELAYS = [1000, 2000, 5000, 10000, 30000]

export function createCsStream({ onEvent, onStatus } = {}) {
  let source = null
  let closed = false
  let retryTimer = null
  let retryCount = 0

  const base = API_BASE_URL

  function parse(raw) {
    try {
      return JSON.parse(raw)
    } catch {
      return null
    }
  }

  function cleanup() {
    if (source) {
      source.onerror = null
      source.close()
      source = null
    }
  }

  function scheduleRetry() {
    if (closed || retryTimer) return
    const delay = RETRY_DELAYS[Math.min(retryCount, RETRY_DELAYS.length - 1)]
    retryCount += 1
    retryTimer = setTimeout(() => {
      retryTimer = null
      connect()
    }, delay)
  }

  async function connect() {
    if (closed) return
    cleanup()
    try {
      const data = await createStreamTicket()
      if (closed) return
      const ticket = data?.ticket
      if (!ticket) {
        onStatus?.('offline')
        scheduleRetry()
        return
      }
      const es = new EventSource(`${base}/operations/cs/stream?ticket=${encodeURIComponent(ticket)}`)
      source = es
      es.addEventListener('open', () => {
        retryCount = 0
        onStatus?.('online')
      })
      es.addEventListener('hello', (event) => {
        const payload = parse(event.data)
        if (payload) onEvent?.('hello', payload)
      })
      es.addEventListener('cs', (event) => {
        const payload = parse(event.data)
        if (payload) onEvent?.('cs', payload)
      })
      es.onerror = () => {
        if (closed) return
        cleanup()
        onStatus?.('offline')
        scheduleRetry()
      }
    } catch {
      // 换票失败（无权限、后端未重启、网络断了）——退避后重试，别把请求打成风暴
      onStatus?.('offline')
      scheduleRetry()
    }
  }

  if (USE_MOCK || typeof EventSource === 'undefined') {
    // mock 模式与老浏览器没有推送，交给调用方的兜底轮询
    onStatus?.('unsupported')
  } else {
    connect()
  }

  return {
    close() {
      closed = true
      if (retryTimer) {
        clearTimeout(retryTimer)
        retryTimer = null
      }
      cleanup()
    },
    reconnect() {
      if (closed) return
      retryCount = 0
      connect()
    },
  }
}
