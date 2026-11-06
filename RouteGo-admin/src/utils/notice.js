/**
 * 客服会话的提醒：提示音 + 标题角标。
 *
 * 提示音不用音频文件：WebAudio 现合成一个短促的「叮咚」，
 * 省一个静态资源，也不会出现首次加载慢半拍的问题。
 * 浏览器要求先有用户交互才能出声（自动播放策略），
 * 客服登录、点开工单都算交互，因此实际使用时不会被拦。
 */

const SOUND_KEY = 'routego:cs:sound'

let audioCtx = null

/** 提示音是否开启，默认开 */
export function isSoundOn() {
  return localStorage.getItem(SOUND_KEY) !== 'off'
}

export function setSoundOn(on) {
  localStorage.setItem(SOUND_KEY, on ? 'on' : 'off')
}

/**
 * 播放新消息提示音。
 * 任何异常都静默吞掉：听不到提示顶多漏一次提醒，不该把页面带崩。
 */
export function playNewMessageSound() {
  if (!isSoundOn()) return
  try {
    const Ctx = window.AudioContext || window.webkitAudioContext
    if (!Ctx) return
    audioCtx = audioCtx || new Ctx()
    if (audioCtx.state === 'suspended') audioCtx.resume()
    const start = audioCtx.currentTime
    // 两声上行音（E6 → A6），间隔 150ms，像门铃但不刺耳
    const tones = [
      { freq: 1318.51, at: 0 },
      { freq: 1760, at: 0.15 },
    ]
    tones.forEach(({ freq, at }) => {
      const osc = audioCtx.createOscillator()
      const gain = audioCtx.createGain()
      osc.type = 'sine'
      osc.frequency.value = freq
      gain.gain.setValueAtTime(0.0001, start + at)
      gain.gain.exponentialRampToValueAtTime(0.16, start + at + 0.02)
      gain.gain.exponentialRampToValueAtTime(0.0001, start + at + 0.22)
      osc.connect(gain)
      gain.connect(audioCtx.destination)
      osc.start(start + at)
      osc.stop(start + at + 0.24)
    })
  } catch {
    // 浏览器不支持或音频上下文被策略阻塞，忽略
  }
}

let baseTitle = null

/**
 * 在浏览器标签上显示未读数，切到别的标签页也能看到有几条待回。
 * 首次调用时记住原始标题，未读数归零后恢复。
 */
export function setTitleBadge(count) {
  if (baseTitle === null) {
    baseTitle = document.title || 'RouteGo'
  }
  const next = count > 0 ? `(${count > 99 ? '99+' : count}) ${baseTitle}` : baseTitle
  if (document.title !== next) {
    document.title = next
  }
}
