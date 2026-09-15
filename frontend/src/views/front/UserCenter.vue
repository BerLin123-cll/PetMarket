<template>
  <div class="user-center">
    <el-row :gutter="20">
      <el-col :span="5">
        <el-card>
          <el-menu :default-active="activeMenu" router>
            <el-menu-item index="/user/profile">
              <el-icon><User /></el-icon>个人信息
            </el-menu-item>
            <el-menu-item index="/user/address">
              <el-icon><Location /></el-icon>收货地址
            </el-menu-item>
            <el-menu-item index="/user/orders">
              <el-icon><Document /></el-icon>我的订单
            </el-menu-item>
          </el-menu>
        </el-card>
      </el-col>
      <el-col :span="19">
        <router-view />
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const activeMenu = computed(() => route.path)
</script>

<style scoped>
.user-center { margin-top: 10px; }
</style>
