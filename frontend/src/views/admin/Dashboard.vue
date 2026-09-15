<!-- 文件路径: E:\SpringDemo\PetMarket\frontend\src\views\admin\Dashboard.vue -->
<template>
  <div class="dashboard">
    <el-row :gutter="20" class="stat-row">
      <el-col :span="8">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon" style="background:#409eff"><el-icon size="32" color="#fff"><User /></el-icon></div>
          <div class="stat-info">
            <p class="stat-label">用户总数</p>
            <p class="stat-value">{{ stats.userCount || 0 }}</p>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon" style="background:#67c23a"><el-icon size="32" color="#fff"><Document /></el-icon></div>
          <div class="stat-info">
            <p class="stat-label">订单总数</p>
            <p class="stat-value">{{ stats.orderCount || 0 }}</p>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon" style="background:#e6a23c"><el-icon size="32" color="#fff"><Money /></el-icon></div>
          <div class="stat-info">
            <p class="stat-label">销售总额</p>
            <p class="stat-value">¥{{ stats.totalSales || '0.00' }}</p>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top:20px">
      <template #header><span>系统信息</span></template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="项目名称">PetMarket 宠物商城</el-descriptions-item>
        <el-descriptions-item label="项目类型">毕业设计 Demo</el-descriptions-item>
        <el-descriptions-item label="后端框架">Spring Boot 3.2.x</el-descriptions-item>
        <el-descriptions-item label="前端框架">Vue 3 + Element Plus</el-descriptions-item>
        <el-descriptions-item label="数据库">MySQL 8.0</el-descriptions-item>
        <el-descriptions-item label="持久层">MyBatis-Plus</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getDashboardStats } from '@/api/admin'

const stats = ref({})

onMounted(async () => {
  try {
    const res = await getDashboardStats()
    stats.value = res.data
  } catch (e) {}
})
</script>

<style scoped>
.stat-row { margin-bottom: 10px; }
.stat-card :deep(.el-card__body) { display: flex; align-items: center; gap: 20px; padding: 24px; }
.stat-icon {
  width: 64px; height: 64px; border-radius: 12px; display: flex;
  align-items: center; justify-content: center; flex-shrink: 0;
}
.stat-label { font-size: 14px; color: #999; margin-bottom: 8px; }
.stat-value { font-size: 28px; font-weight: bold; color: #333; }
</style>
