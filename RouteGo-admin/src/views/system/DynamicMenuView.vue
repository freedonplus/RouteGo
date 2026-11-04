<script setup>
import { computed, defineAsyncComponent } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { findMenuByPath } from '@/utils/menu'
import { resolveMenuComponent } from '@/router/menu-components'
import PlaceholderView from './PlaceholderView.vue'
import ForbiddenView from '@/views/error/ForbiddenView.vue'
import NotFoundView from '@/views/error/NotFoundView.vue'

const route = useRoute()
const auth = useAuthStore()

const node = computed(() => findMenuByPath(auth.menus, route.path))
const allowed = computed(() => !node.value?.perms || auth.hasPerm(node.value.perms))

const asyncComponent = computed(() => {
  if (!node.value) return null
  const loader = resolveMenuComponent(node.value.component)
  return loader ? defineAsyncComponent(loader) : null
})
</script>

<template>
  <NotFoundView v-if="!node" />
  <ForbiddenView v-else-if="!allowed" />
  <component :is="asyncComponent" v-else-if="asyncComponent" />
  <PlaceholderView v-else :menu-name="node.menuName" />
</template>
