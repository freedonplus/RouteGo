import { state } from './db'

const CHARS = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'
const TTL = 2 * 60 * 1000

function randomCode(length = 4) {
  let code = ''
  for (let i = 0; i < length; i += 1) {
    code += CHARS[Math.floor(Math.random() * CHARS.length)]
  }
  return code
}

function uuid() {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) return crypto.randomUUID()
  return `mock-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

function draw(code) {
  const canvas = document.createElement('canvas')
  canvas.width = 120
  canvas.height = 36
  const ctx = canvas.getContext('2d')
  ctx.fillStyle = '#f8fafc'
  ctx.fillRect(0, 0, canvas.width, canvas.height)

  for (let i = 0; i < 6; i += 1) {
    ctx.strokeStyle = i % 2 === 0 ? '#e2e8f0' : '#f3d7de'
    ctx.beginPath()
    ctx.moveTo(Math.random() * 120, Math.random() * 36)
    ctx.lineTo(Math.random() * 120, Math.random() * 36)
    ctx.stroke()
  }

  code.split('').forEach((char, index) => {
    ctx.save()
    ctx.translate(18 + index * 26, 26)
    ctx.rotate((Math.random() - 0.5) * 0.34)
    ctx.font = '600 20px "JetBrains Mono", Consolas, monospace'
    ctx.fillStyle = index % 2 === 0 ? '#121212' : '#a8435f'
    ctx.fillText(char, 0, 0)
    ctx.restore()
  })

  return canvas.toDataURL('image/png').replace(/^data:image\/png;base64,/, '')
}

export function createCaptcha() {
  const id = uuid()
  const code = randomCode()
  state.captchas.set(id, { code, expires: Date.now() + TTL })
  return { uuid: id, image: draw(code), code }
}

export function verifyCaptcha(id, input) {
  const record = state.captchas.get(id)
  if (!record) return false
  state.captchas.delete(id)
  if (record.expires < Date.now()) return false
  return String(input || '').trim().toUpperCase() === record.code
}
