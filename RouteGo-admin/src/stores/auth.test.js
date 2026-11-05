import { describe, it, expect } from 'vitest'
import { hasPermission } from './auth'

describe('hasPermission', () => {
  it('wildcard allows all', () => {
    expect(hasPermission(['*'], 'admin:SysUser:create')).toBe(true)
  })

  it('exact match', () => {
    expect(hasPermission(['admin:SysUser:page'], 'admin:SysUser:page')).toBe(true)
  })

  it('missing permission denies', () => {
    expect(hasPermission(['admin:SysUser:page'], 'admin:SysUser:create')).toBe(false)
  })

  it('array requirement passes with any match', () => {
    expect(hasPermission(['admin:SysUser:create'], ['admin:SysUser:update', 'admin:SysUser:create'])).toBe(true)
  })

  it('empty requirement allows', () => {
    expect(hasPermission([], '')).toBe(true)
  })
})
