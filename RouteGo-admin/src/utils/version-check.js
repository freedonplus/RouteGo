/**
 * 新版本发布提示。
 *
 * 原理：构建产物入口脚本带内容 hash。记下本次加载的入口 src，
 * 每 5 分钟拉一次当前 index.html（no-store，服务器配置了不缓存），
 * 解析出新的入口 src —— 不一致说明发了新版，提示用户刷新。
 *
 * 只对长驻页面有意义：管理员后台经常挂一整天，旧页面引用的旧
 * hash 文件被新版本替换后仍可用（文件未删），但逻辑已落后。
 */
const CHECK_INTERVAL = 5 * 60 * 1000

function currentEntrySrc() {
  const el = document.querySelector('script[type="module"][src]')
  return el ? el.getAttribute('src') : ''
}

let entryAtBoot = ''
let notified = false

async function checkOnce() {
  if (document.hidden || notified || !entryAtBoot) return
  try {
    const res = await fetch(`${window.location.pathname}${window.location.search}`, {
      cache: 'no-store',
    })
    if (!res.ok) return
    const html = await res.text()
    const match = html.match(/<script[^>]+type="module"[^>]+src="([^"]+)"/)
    if (match && match[1] && match[1] !== entryAtBoot) {
      notified = true
      ElNotification({
        title: '系统已更新',
        message: '检测到新版本，刷新页面后生效。',
        type: 'info',
        duration: 0,
        onClick: () => window.location.reload(),
      })
    }
  } catch {
    /* 网络异常不打扰用户，下个周期重试 */
  }
}

export function startVersionCheck() {
  entryAtBoot = currentEntrySrc()
  if (!entryAtBoot) return
  setInterval(checkOnce, CHECK_INTERVAL)
  document.addEventListener('visibilitychange', () => {
    if (!document.hidden) checkOnce()
  })
}
