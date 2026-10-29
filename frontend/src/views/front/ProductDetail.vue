<template>
  <div class="detail-page" v-if="product">
    <el-card>
      <el-row :gutter="40">
        <el-col :span="10">
          <img :src="product.mainImage || defaultImg" class="detail-img" />
        </el-col>
        <el-col :span="14">
          <h1 class="detail-name">{{ product.name }}</h1>
          <el-tag>{{ product.categoryName }}</el-tag>
          <p class="detail-price">¥{{ product.price }}</p>
          <p class="detail-stock">库存：{{ product.stock }} 件</p>
          <div class="detail-desc">
            <h4>商品描述</h4>
            <p>{{ product.description || '暂无描述' }}</p>
          </div>
          <div class="quantity-row">
            <span>数量：</span>
            <el-input-number v-model="quantity" :min="1" :max="product.stock" />
          </div>
          <div class="action-btns">
            <el-button type="primary" plain size="large" @click="handleAddCart" :loading="cartLoading">
              <el-icon><ShoppingCart /></el-icon> 加入购物车
            </el-button>
            <el-button type="primary" size="large" @click="handleBuyNow">立即购买</el-button>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProductDetail } from '@/api/product'
import { addToCart } from '@/api/cart'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const product = ref(null)
const quantity = ref(1)
const cartLoading = ref(false)
const defaultImg = '/placeholder-product.svg'

const handleAddCart = async () => {
  if (!userStore.token) { router.push('/login'); return }
  cartLoading.value = true
  try {
    await addToCart({ productId: product.value.id, quantity: quantity.value })
    ElMessage.success('已加入购物车')
  } catch (e) {} finally {
    cartLoading.value = false
  }
}

const handleBuyNow = async () => {
  if (!userStore.token) { router.push('/login'); return }
  cartLoading.value = true
  try {
    await addToCart({ productId: product.value.id, quantity: quantity.value })
    ElMessage.success('已加入购物车')
    router.push('/cart')
  } catch (e) {} finally {
    cartLoading.value = false
  }
}



onMounted(async () => {
  const res = await getProductDetail(route.params.id)
  product.value = res.data
})
</script>

<style scoped>
.detail-page { max-width: 1000px; margin: 0 auto; }
.detail-img { width: 100%; aspect-ratio: 4 / 3; object-fit: cover; border-radius: var(--pm-radius-card); background: var(--pm-surface-soft); }
.detail-name { font-size: 24px; margin-bottom: 12px; }
.detail-price { font-size: 28px; color: var(--pm-primary); font-weight: bold; margin: 16px 0; }
.detail-stock { color: var(--pm-text-light); margin-bottom: 16px; }
.detail-desc { background: var(--pm-surface-soft); padding: 16px; border-radius: var(--pm-radius-card); margin-bottom: 20px; }
.detail-desc h4 { margin-bottom: 8px; }
.detail-desc p { color: #666; line-height: 1.6; }
.quantity-row { display: flex; align-items: center; gap: 12px; margin-bottom: 24px; }
.action-btns { display: flex; gap: 16px; }
</style>
