import { describe, it, expect } from 'vitest'
import {
  maskPhone,
  maskEmail,
  formatDate,
  formatDateTime,
  formatFileSize,
  isMasked,
  formatMoney,
  formatNumber,
} from './format'

describe('format', () => {
  it('masks phone', () => expect(maskPhone('13812348000')).toBe('138****8000'))
  it('keeps null phone', () => expect(maskPhone(null)).toBe('—'))
  it('masks email', () => expect(maskEmail('ops@example.com')).toBe('o***@example.com'))
  it('formats datetime', () => expect(formatDateTime('2026-11-04T08:05:09')).toBe('2026-11-04 08:05:09'))
  it('formats date only', () => expect(formatDate('2026-11-04T08:05:09')).toBe('2026-11-04'))
  it('formats file size', () => expect(formatFileSize(1024)).toBe('1.0 KB'))
  it('detects mask', () => expect(isMasked('138****8000')).toBe(true))
  it('formats money with symbol', () => expect(formatMoney(1200)).toBe('¥1200.00'))
  it('formats money without symbol', () => expect(formatMoney('99.5', { prefix: '' })).toBe('99.50'))
  it('keeps empty money', () => expect(formatMoney(null)).toBe('—'))
  it('formats number with grouping', () => expect(formatNumber(12345)).toBe('12,345'))
})
