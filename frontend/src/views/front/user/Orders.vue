<!-- 文件路径: E:\SpringDemo\PetMarket\frontend\src\views\front\user\Orders.vue -->
<template>
  <el-card>
    <h3 style="margin-bottom:16px">我的订单</h3>
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="全部" name="" />
      <el-tab-pane label="待支付" name="0" />
      <el-tab-pane label="已支付" name="1" />
      <el-tab-pane label="已发货" name="2" />
      <el-tab-pane label="已完成" name="3" />
      <el-tab-pane label="已取消" name="4" />
    </el-tabs>

    <div v-for="order in orders" :key="order.id" class="order-card">
      <div class="order-header">
        <span>订单号：{{ order.orderNo }}</span>
        <span>{{ order.createTime }}</span>
        <el-tag :type="statusType(order.orderStatus)">{{ statusText(order.orderStatus) }}</el-tag>
        <span v-if="order.orderStatus === 0" class="pay-countdown" :class="{ expired: payRemaining(order) <= 0 }">
          <el-icon><Clock /></el-icon>
          <template v-if="payRemaining(order) > 0">剩余 {{ formatCountdown(payRemaining(order)) }} 自动取消</template>
          <template v-else>已超时，即将自动取消</template>
        </span>
      </div>
      <el-table :data="order.items" size="small" border>
        <el-table-column label="商品" min-width="250">
          <template #default="{ row }">
            <div style="display:flex;align-items:center;gap:8px">
              <img :src="row.productImage || '/placeholder-square.svg'" style="width:40px;height:40px;border-radius:6px;object-fit:cover;background:var(--pm-surface-soft)" />
              {{ row.productName }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="单价" width="100">
          <template #default="{ row }">¥{{ row.price }}</template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="70" />
        <el-table-column label="小计" width="100">
          <template #default="{ row }">¥{{ row.subtotal }}</template>
        </el-table-column>
      </el-table>
      <div class="order-footer">
        <span>收货人：{{ order.receiverName }} {{ order.receiverPhone }}</span>
        <div>
          <span class="order-total">合计：¥{{ order.totalAmount }}</span>
          <el-button
            v-if="order.orderStatus === 0"
            type="primary"
            size="small"
            :disabled="payRemaining(order) <= 0"
            @click="handlePay(order)"
          >去支付</el-button>
          <el-button v-if="order.orderStatus === 0" size="small" @click="handleCancel(order.id)">取消订单</el-button>
        </div>
      </div>
    </div>

    <el-empty v-if="orders.length === 0" description="暂无订单" />

    <div style="text-align:center;margin-top:16px">
      <el-pagination
        v-model:current-page="pageNum"
        :total="total"
        :page-size="10"
        layout="prev, pager, next"
        @current-change="fetchData"
      />
    </div>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getOrderList, cancelOrder, payOrder } from '@/api/order'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useNow, remainingSeconds, formatCountdown } from '@/composables/useCountdown'

const orders = ref([])
const pageNum = ref(1)
const total = ref(0)
const activeTab = ref('')

// 用统一时钟驱动所有待支付订单的倒计时（每秒刷新，避免每行各开一个定时器）
const now = useNow()
const payRemaining = (order) => remainingSeconds(order.payExpireTime, now.value)

const statusText = (s) => ['待支付', '已支付', '已发货', '已完成', '已取消'][s] || '未知'
const statusType = (s) => ['warning', 'primary', '', 'success', 'info'][s] || ''

const fetchData = async () => {
  const params = { pageNum: pageNum.value, pageSize: 10 }
  if (activeTab.value !== '') params.status = Number(activeTab.value)
  const res = await getOrderList(params)
  orders.value = res.data.records
  total.value = Number(res.data.total)
}

const handleTabChange = () => { pageNum.value = 1; fetchData() }

const handleCancel = async (id) => {
  await ElMessageBox.confirm('确定取消该订单？', '提示', { type: 'warning' })
  await cancelOrder(id)
  ElMessage.success('订单已取消')
  fetchData()
}

const handlePay = async (order) => {
  await payOrder(order.orderNo)
  ElMessage.success('支付成功')
  fetchData()
}

onMounted(fetchData)
</script>

<style scoped>
.order-card { border: 1px solid var(--pm-border-warm); border-radius: var(--pm-radius-card); margin-bottom: 16px; overflow: hidden; }
.order-header { display: flex; align-items: center; gap: 16px; padding: 12px 16px; }
.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-top: 1px solid var(--pm-border-warm);
}
.order-total {
  font-size: 16px;
  font-weight: bold;
  color: var(--pm-primary);
  margin-right: 12px;
}
.pay-countdown {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-left: auto;
  font-size: 13px;
  color: #e6a23c;
}
.pay-countdown.expired { color: #f56c6c; }
</style>