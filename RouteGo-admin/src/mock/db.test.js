import { describe, it, expect, beforeEach } from 'vitest'
import { paginate, resetDb, state, permissionCodesForUser } from './db'

describe('paginate', () => {
  it('slices page 2', () => {
    const r = paginate([1, 2, 3, 4, 5, 6, 7], 2, 3)
    expect(r.records).toEqual([4, 5, 6])
    expect(r.total).toBe(7)
    expect(r.pages).toBe(3)
  })

  it('clamps page size to 200', () => {
    const r = paginate([1, 2, 3], 1, 500)
    expect(r.pageSize).toBe(200)
  })
})

describe('permissionCodesForUser', () => {
  beforeEach(() => resetDb())

  it('returns wildcard for super admin', () => {
    expect(permissionCodesForUser(1)).toEqual(['*'])
  })

  it('returns merged role permissions for ops', () => {
    const user = state.users.find((u) => u.username === 'ops')
    expect(permissionCodesForUser(user.id)).toContain('admin:SysUser:page')
  })
})
