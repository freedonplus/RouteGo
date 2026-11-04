import {
  Bell,
  Clock,
  Document,
  Folder,
  Key,
  Menu as MenuIcon,
  Notebook,
  Odometer,
  SetUp,
  Setting,
  User,
  UserFilled,
} from '@element-plus/icons-vue'

export const menuIcons = {
  Settings: Setting,
  User,
  Team: UserFilled,
  Menu: MenuIcon,
  Key,
  Book: Notebook,
  SetUp,
  Bell,
  Folder,
  Document,
  Clock,
  Odometer,
}

export function resolveMenuIcon(name) {
  return menuIcons[name] || MenuIcon
}
