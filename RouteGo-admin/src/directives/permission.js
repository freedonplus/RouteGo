import { useAuthStore } from '@/stores/auth'

export const permission = {
  mounted(el, binding) {
    const auth = useAuthStore()
    if (!auth.hasPerm(binding.value)) {
      el.parentNode?.removeChild(el)
    }
  },
}

export default permission
