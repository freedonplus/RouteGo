export function flattenMenus(menus = []) {
  const result = []
  const walk = (nodes) => {
    nodes.forEach((node) => {
      result.push(node)
      if (node.children?.length) walk(node.children)
    })
  }
  walk(menus)
  return result
}

export function findMenuByPath(menus, path) {
  return flattenMenus(menus).find((node) => node.path === path) || null
}

export function menuTrail(menus, path) {
  const flat = flattenMenus(menus)
  const node = flat.find((item) => item.path === path)
  if (!node) return []
  const trail = [node]
  let current = node
  while (current.parentId) {
    const parent = flat.find((item) => item.id === current.parentId)
    if (!parent) break
    trail.unshift(parent)
    current = parent
  }
  return trail
}

export function navItems(menus = []) {
  return flattenMenus(menus).filter((node) => node.menuType !== 'F' && node.path)
}
