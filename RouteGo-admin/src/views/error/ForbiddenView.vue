<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="flex min-h-[280px] items-center justify-center px-4 py-6">
    <div class="rg-card w-full max-w-md px-4 py-4">
      <p class="font-mono text-base font-semibold text-[#121212]">403</p>
      <p class="mt-1 text-sm font-semibold text-[#1e293b]">没有访问权限</p>
      <p class="mt-1 text-xs leading-5 text-[#64748b]">
        当前账号缺少该页面的权限码，请联系管理员在角色权限中分配。
      </p>
      <div class="mt-3 flex gap-2">
        <el-button size="small" type="primary" @click="router.push('/dashboard')">
          返回仪表盘
        </el-button>
        <el-button size="small" @click="router.back()">返回上一页</el-button>
      </div>
    </div>
  </div>
</template>
