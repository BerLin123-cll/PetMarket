<!-- 文件路径: E:\SpringDemo\PetMarket\frontend\src\views\front\Login.vue -->
<!--
  用户登录页（V1.2 改为左右分屏 + 多角色 Tab）
  真正的实现见 @/components/SplitLogin.vue，由 /login 与 /admin/login 共用，
  这样"多角色登录"只需一套 UI 与一套逻辑，两个路由各自保留各自的 URL。
-->
<template>
  <SplitLogin />
</template>

<script setup>
import SplitLogin from '@/components/SplitLogin.vue'
</script>
