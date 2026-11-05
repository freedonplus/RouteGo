import { beforeEach, describe, expect, it } from 'vitest'
import { isGroupCollapsed, isGroupOpen, openGroup, resetGroups, toggleGroup } from './sidebar-groups'

describe('sidebar-groups', () => {
  beforeEach(() => resetGroups())

  it('默认为展开', () => expect(isGroupOpen(111)).toBe(true))

  it('点击一次收起，再点一次展开', () => {
    toggleGroup(111)
    expect(isGroupCollapsed(111)).toBe(true)
    toggleGroup(111)
    expect(isGroupOpen(111)).toBe(true)
  })

  it('openGroup 只展开指定目录，不影响其他目录', () => {
    toggleGroup(111)
    toggleGroup(120)
    openGroup(111)
    expect(isGroupOpen(111)).toBe(true)
    expect(isGroupCollapsed(120)).toBe(true)
  })

  it('数字与字符串 id 视为同一个目录', () => {
    toggleGroup(111)
    expect(isGroupCollapsed('111')).toBe(true)
    openGroup('111')
    expect(isGroupOpen(111)).toBe(true)
  })
})
