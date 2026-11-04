<script setup>
import { SetUp } from '@element-plus/icons-vue'

defineProps({
  menuName: { type: String, default: '模块' },
})
</script>

<template>
  <div class="rg-page">
    <div class="rg-card px-3 py-3">
      <div class="flex items-start gap-3">
        <span
          class="flex h-8 w-8 shrink-0 items-center justify-center rounded bg-[#fdf1f5] text-[#a8435f]"
        >
          <el-icon><SetUp /></el-icon>
        </span>
        <div class="min-w-0">
          <p class="rg-section-title">{{ menuName }} · 建设中</p>
          <p class="mt-1 max-w-2xl text-xs leading-5 text-[#64748b]">
            该模块将在后续批次开放。菜单、路由与权限数据已经接入，替换对应视图即可上线。
          </p>
          <router-link
            to="/dashboard"
            class="mt-2 inline-flex h-6 items-center rounded border border-[#e2e8f0] bg-white px-2 text-[11px] text-[#64748b] transition-colors duration-150 hover:bg-[#f8fafc] hover:text-[#1e293b]"
          >
            返回仪表盘
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>
