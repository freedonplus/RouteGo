import { describe, it, expect } from 'vitest'
import { hasPermission } from './auth'

describe('hasPermission', () => {
  it('wildcard allows all', () => {
    expect(hasPermission(['*'], 'system:user:add')).toBe(true)
  })

  it('exact match', () => {
    expect(hasPermission(['system:user:list'], 'system:user:list')).toBe(true)
  })

  it('missing permission denies', () => {
    expect(hasPermission(['system:user:list'], 'system:user:add')).toBe(false)
  })

  it('array requirement passes with any match', () => {
    expect(hasPermission(['system:user:add'], ['system:user:edit', 'system:user:add'])).toBe(true)
  })

  it('empty requirement allows', () => {
    expect(hasPermission([], '')).toBe(true)
  })
})
