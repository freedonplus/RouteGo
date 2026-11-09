<template>
  <!-- zh-cn 国际化：按需引入模式下改用 ConfigProvider 全局下发（替代原 app.use(ElementPlus, { locale })） -->
  <el-config-provider :locale="zhCn">
    <router-view />
  </el-config-provider>
</template>

<script setup>
import zhCn from 'element-plus/es/locale/lang/zh-cn'
</script>
