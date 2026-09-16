<template>
  <div class="result-page">
    <el-card class="result-card">
      <el-result icon="success" title="订单创建成功" :sub-title="`订单号：${orderNo}`">
        <template #extra>
          <el-button type="primary" :loading="paying" :disabled="expired" @click="handlePay">模拟支付</el-button>
          <el-button @click="$router.push('/user/orders')">查看订单</el-button>
        </template>
      </el-result>

      <el-alert
        v-if="!paid && payExpireTime"
        :type="expired ? 'error' : 'warning'"
        :closable="false"
        show-icon
        class="countdown-tip"
      >
        <template v-if="!expired">
          请在 <strong>{{ formatCountdown(remaining) }}</strong> 内完成支付，否则订单将自动取消
        </template>
        <template v-else>
          订单已超过支付时限，将自动取消，请重新下单
        </template>
      </el-alert>

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
import { useNow, remainingSeconds, formatCountdown } from '@/composables/useCountdown'

const route = useRoute()
const orderNo = computed(() => route.query.orderNo || '')
const payExpireTime = computed(() => route.query.payExpireTime || '')
const paying = ref(false)
const paid = ref(false)

// 支付倒计时：仅在拿到截止时间时生效，归零后禁用支付按钮
const now = useNow()
const remaining = computed(() => remainingSeconds(payExpireTime.value, now.value))
const expired = computed(() => !!payExpireTime.value && remaining.value <= 0)

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
.countdown-tip { margin-top: 16px; }
.countdown-tip strong { color: #f56c6c; font-size: 16px; }
</style>
