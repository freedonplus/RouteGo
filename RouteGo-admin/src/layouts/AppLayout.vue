<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import AppSidebar from '@/components/AppSidebar.vue'
import AppHeader from '@/components/AppHeader.vue'

const auth = useAuthStore()

const collapsed = ref(false)
const mobileOpen = ref(false)
const isMobile = ref(false)

function syncViewport() {
  isMobile.value = window.innerWidth < 1024
  if (!isMobile.value) mobileOpen.value = false
}

function handleToggle() {
  if (isMobile.value) mobileOpen.value = !mobileOpen.value
  else collapsed.value = !collapsed.value
}

onMounted(() => {
  syncViewport()
  window.addEventListener('resize', syncViewport)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', syncViewport)
})
</script>

<template>
  <div class="flex min-h-screen bg-[#f8fafc]">
    <aside
      class="fixed inset-y-0 left-0 z-40 w-[224px] shrink-0 transition-transform duration-150 lg:static lg:translate-x-0"
      :class="[
        mobileOpen ? 'translate-x-0' : '-translate-x-full',
        collapsed ? 'lg:w-[56px]' : 'lg:w-[224px]',
      ]"
    >
      <AppSidebar :collapsed="collapsed && !isMobile" @navigate="mobileOpen = false" />
    </aside>

    <div
      v-if="mobileOpen"
      class="fixed inset-0 z-30 bg-black/40 lg:hidden"
      @click="mobileOpen = false"
    ></div>

    <div class="flex min-w-0 flex-1 flex-col">
      <AppHeader :collapsed="collapsed" @toggle="handleToggle" />
      <main class="min-w-0 flex-1">
        <router-view :key="auth.sessionVersion" />
      </main>
    </div>
  </div>
</template>
