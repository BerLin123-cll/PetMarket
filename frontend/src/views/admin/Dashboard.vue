<!-- 文件路径: E:\SpringDemo\PetMarket\frontend\src\views\admin\Dashboard.vue -->
<template>
  <div class="dashboard">
    <!-- 基础统计卡片 -->
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

    <!-- 销售趋势 + 用户复购率 -->
    <el-row :gutter="20" class="block-row">
      <el-col :span="16">
        <el-card shadow="hover" v-loading="loading">
          <template #header><span>销售趋势（近 7 天）</span></template>
          <div class="trend-chart">
            <div v-for="point in salesTrend" :key="point.saleDate" class="trend-col">
              <div class="trend-bar"
                   :style="{ height: trendBarHeight(point.amount) }"
                   :title="`${point.saleDate}：¥${point.amount}`"></div>
              <span class="trend-label">{{ shortDate(point.saleDate) }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" v-loading="loading">
          <template #header><span>用户复购率</span></template>
          <div class="repurchase">
            <div class="repurchase-ring" :style="ringStyle">
              <div class="repurchase-inner">{{ repurchaseRate }}%</div>
            </div>
            <p class="repurchase-desc">
              复购用户 <b>{{ repurchase.repeatBuyers || 0 }}</b> / 购买用户 <b>{{ repurchase.totalBuyers || 0 }}</b>
            </p>
            <p class="repurchase-hint">下单 ≥2 笔视为复购</p>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 订单状态漏斗 + 热销商品 -->
    <el-row :gutter="20" class="block-row">
      <el-col :span="10">
        <el-card shadow="hover" v-loading="loading">
          <template #header><span>订单状态漏斗</span></template>
          <div class="funnel">
            <div v-for="item in orderStatus" :key="item.status" class="funnel-row">
              <span class="funnel-label">{{ item.statusText }}</span>
              <div class="funnel-track">
                <div class="funnel-fill" :style="{ width: statusWidth(item.count) }"></div>
              </div>
              <span class="funnel-count">{{ item.count }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="14">
        <el-card shadow="hover" v-loading="loading">
          <template #header><span>热销商品 Top 5</span></template>
          <el-empty v-if="!hotProducts.length" description="暂无销量数据" :image-size="60" />
          <div v-else class="hot-list">
            <div v-for="(product, index) in hotProducts" :key="product.productId" class="hot-row">
              <span class="hot-rank" :class="rankClass(index)">{{ index + 1 }}</span>
              <span class="hot-name" :title="product.productName">{{ product.productName }}</span>
              <div class="hot-track">
                <div class="hot-fill" :style="{ width: hotWidth(product.quantity) }"></div>
              </div>
              <span class="hot-qty">x{{ product.quantity }}</span>
              <span class="hot-amt">¥{{ product.amount }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 低库存预警 -->
    <el-row :gutter="20" class="block-row">
      <el-col :span="24">
        <el-card shadow="hover" v-loading="loading">
          <template #header>
            <span>低库存预警（库存 ≤ 10）</span>
          </template>
          <el-empty v-if="!lowStock.length" description="暂无低库存商品" :image-size="60" />
          <el-table v-else :data="lowStock" size="small">
            <el-table-column prop="name" label="商品名称" min-width="240" />
            <el-table-column label="剩余库存" width="140">
              <template #default="{ row }">
                <el-tag :type="stockTagType(row.stock)" effect="dark">{{ row.stock }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="单价" width="140">
              <template #default="{ row }">¥{{ row.price }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import {
  getDashboardStats,
  getSalesTrend,
  getOrderStatusFunnel,
  getHotProducts,
  getLowStockProducts,
  getRepurchaseRate
} from '@/api/admin'

const loading = ref(false)
const stats = ref({})
const salesTrend = ref([])
const orderStatus = ref([])
const hotProducts = ref([])
const lowStock = ref([])
const repurchase = ref({})

const trendMax = computed(() => Math.max(1, ...salesTrend.value.map((p) => Number(p.amount) || 0)))
const statusMax = computed(() => Math.max(1, ...orderStatus.value.map((s) => Number(s.count) || 0)))
const hotMax = computed(() => Math.max(1, ...hotProducts.value.map((p) => Number(p.quantity) || 0)))
const repurchaseRate = computed(() => Number(repurchase.value.rate || 0))

// 复购率环：conic-gradient 以角度填充，360 度对应 100%
const ringStyle = computed(() => ({
  background: `conic-gradient(#409eff ${repurchaseRate.value * 3.6}deg, #ebeef5 0deg)`
}))

const shortDate = (date) => (date ? String(date).slice(5) : '')

const trendBarHeight = (amount) => {
  const value = Number(amount) || 0
  return value <= 0 ? '2px' : Math.max(4, Math.round((value / trendMax.value) * 100)) + '%'
}

const statusWidth = (count) => Math.round(((Number(count) || 0) / statusMax.value) * 100) + '%'

const hotWidth = (quantity) => Math.max(4, Math.round(((Number(quantity) || 0) / hotMax.value) * 100)) + '%'

const rankClass = (index) => (index < 3 ? `rank-${index + 1}` : '')

const stockTagType = (stock) => (stock <= 3 ? 'danger' : 'warning')

const loadAll = async () => {
  loading.value = true
  try {
    const [basic, trend, status, hot, low, rep] = await Promise.all([
      getDashboardStats(),
      getSalesTrend(7),
      getOrderStatusFunnel(),
      getHotProducts(5),
      getLowStockProducts({ threshold: 10, limit: 10 }),
      getRepurchaseRate()
    ])
    stats.value = basic.data || {}
    salesTrend.value = trend.data || []
    orderStatus.value = status.data || []
    hotProducts.value = hot.data || []
    lowStock.value = low.data || []
    repurchase.value = rep.data || {}
  } catch (e) {
    // 请求异常已由全局拦截器提示，这里保持页面可用
  } finally {
    loading.value = false
  }
}

onMounted(loadAll)
</script>

<style scoped>
.stat-row { margin-bottom: 10px; }
.block-row { margin-top: 20px; }
.stat-card :deep(.el-card__body) { display: flex; align-items: center; gap: 20px; padding: 24px; }
.stat-icon {
  width: 64px; height: 64px; border-radius: 12px; display: flex;
  align-items: center; justify-content: center; flex-shrink: 0;
}
.stat-label { font-size: 14px; color: #999; margin-bottom: 8px; }
.stat-value { font-size: 28px; font-weight: bold; color: #333; }

/* 销售趋势柱状图 */
.trend-chart {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  height: 200px;
  padding-top: 8px;
}
.trend-col {
  flex: 1;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
}
.trend-bar {
  width: 100%;
  max-width: 40px;
  min-height: 2px;
  background: linear-gradient(180deg, #79bbff, #409eff);
  border-radius: 4px 4px 0 0;
  transition: height 0.3s;
}
.trend-label { font-size: 12px; color: #909399; margin-top: 6px; white-space: nowrap; }

/* 用户复购率环 */
.repurchase { display: flex; flex-direction: column; align-items: center; padding: 10px 0; }
.repurchase-ring {
  width: 130px; height: 130px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
}
.repurchase-inner {
  width: 96px; height: 96px; border-radius: 50%; background: #fff;
  display: flex; align-items: center; justify-content: center;
  font-size: 24px; font-weight: bold; color: #409eff;
}
.repurchase-desc { margin-top: 16px; font-size: 14px; color: #606266; }
.repurchase-desc b { color: #303133; }
.repurchase-hint { margin-top: 6px; font-size: 12px; color: #c0c4cc; }

/* 订单状态漏斗 */
.funnel { display: flex; flex-direction: column; gap: 14px; padding: 6px 0; }
.funnel-row { display: flex; align-items: center; gap: 10px; }
.funnel-label { width: 56px; font-size: 13px; color: #606266; text-align: right; }
.funnel-track {
  flex: 1; height: 18px; background: #f0f2f5; border-radius: 9px; overflow: hidden;
}
.funnel-fill {
  height: 100%; border-radius: 9px;
  background: linear-gradient(90deg, #79bbff, #409eff);
  transition: width 0.3s;
}
.funnel-count { width: 40px; font-size: 13px; color: #303133; font-weight: bold; }

/* 热销商品 */
.hot-list { display: flex; flex-direction: column; gap: 12px; padding: 4px 0; }
.hot-row { display: flex; align-items: center; gap: 10px; }
.hot-rank {
  width: 22px; height: 22px; border-radius: 50%; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  font-size: 12px; color: #fff; background: #c0c4cc;
}
.hot-rank.rank-1 { background: #f56c6c; }
.hot-rank.rank-2 { background: #e6a23c; }
.hot-rank.rank-3 { background: #409eff; }
.hot-name {
  width: 150px; font-size: 13px; color: #303133;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.hot-track { flex: 1; height: 14px; background: #f0f2f5; border-radius: 7px; overflow: hidden; }
.hot-fill {
  height: 100%; border-radius: 7px;
  background: linear-gradient(90deg, #95d475, #67c23a);
  transition: width 0.3s;
}
.hot-qty { width: 48px; font-size: 13px; color: #606266; }
.hot-amt { width: 88px; font-size: 13px; color: #f56c6c; text-align: right; }
</style>
