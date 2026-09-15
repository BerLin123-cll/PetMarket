<!-- 文件路径: E:\SpringDemo\PetMarket\frontend\src\views\admin\AdminLayout.vue -->
<template>
  <div class="admin-layout">
    <el-aside width="220px" class="sidebar">
      <div class="logo">
        <el-icon size="24"><Setting /></el-icon>
        <span>后台管理系统</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        background-color="#1d1e2c"
        text-color="#a3a6ad"
        active-text-color="#409eff"
      >
        <el-menu-item index="/admin">
          <el-icon><DataAnalysis /></el-icon>
          <span>数据概览</span>
        </el-menu-item>
        <el-menu-item index="/admin/product">
          <el-icon><Goods /></el-icon>
          <span>商品管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/category">
          <el-icon><Menu /></el-icon>
          <span>分类管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/user">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/order">
          <el-icon><Document /></el-icon>
          <span>订单管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <div class="main-area">
      <el-header class="admin-header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/admin' }">后台管理</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <span class="admin-name">{{ adminStore.adminInfo?.username || '管理员' }}</span>
          <el-button type="danger" link @click="handleLogout">退出登录</el-button>
        </div>
      </el-header>
      <el-main class="admin-main">
        <router-view />
      </el-main>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAdminStore } from '@/stores/admin'
import { ElMessage } from 'element-plus'
import {DataAnalysis, Document, Goods, Setting, User} from "@element-plus/icons-vue";

const route = useRoute()
const router = useRouter()
const adminStore = useAdminStore()

const activeMenu = computed(() => route.path)

const titleMap = {
  '/admin': '数据概览',
  '/admin/product': '商品管理',
  '/admin/category': '分类管理',
  '/admin/user': '用户管理',
  '/admin/order': '订单管理'
}
const currentTitle = computed(() => titleMap[route.path] || '数据概览')

const handleLogout = () => {
  adminStore.logout()
  ElMessage.success('已退出登录')
  router.push('/admin/login')
}
</script>

<style scoped>
.admin-layout { display: flex; min-height: 100vh; }
.sidebar { background: #1d1e2c; overflow-y: auto; }
.logo {
  height: 64px; display: flex; align-items: center; justify-content: center; gap: 8px;
  color: #fff; font-size: 18px; font-weight: bold; border-bottom: 1px solid #2d2e3e;
}
.main-area { flex: 1; display: flex; flex-direction: column; background: #f0f2f5; }
.admin-header {
  background: #fff; display: flex; align-items: center; justify-content: space-between;
  box-shadow: 0 1px 4px rgba(0,0,0,0.08); height: 64px; padding: 0 24px;
}
.header-right { display: flex; align-items: center; gap: 16px; }
.admin-name { font-size: 14px; color: #666; }
.admin-main { padding: 20px; }
</style>
