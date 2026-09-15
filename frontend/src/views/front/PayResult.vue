<template>
  <div class="result-page">
    <el-card class="result-card">
      <el-result icon="success" title="订单创建成功" :sub-title="`订单号：${orderNo}`">
        <template #extra>
          <el-button type="primary" :loading="paying" @click="handlePay">模拟支付</el-button>
          <el-button @click="$router.push('/user/orders')">查看订单</el-button>
        </template>
      </el-result>
      <div v-if="paid" class="paid-tip">
        <el-alert title="🎉 支付成功！感谢您的购买" type="success" show-icon :closable="false" />
        <div style="margin-top:16px;text-align:center">
          <el-button type="primary" @click="$router.push('/')">继续购物</el-button>
          <el-button @click="$router.push('/user/orders')">查看订单</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { payOrder } from '@/api/order'
import { ElMessage } from 'element-plus'

const route = useRoute()
const orderNo = computed(() => route.query.orderNo || '')
const paying = ref(false)
const paid = ref(false)

const handlePay = async () => {
  paying.value = true
  try {
    await payOrder(orderNo.value)
    paid.value = true
    ElMessage.success('支付成功')
  } catch (e) {} finally {
    paying.value = false
  }
}
</script>

<style scoped>
.result-page { max-width: 600px; margin: 60px auto; }
.paid-tip { margin-top: 20px; }
</style>
