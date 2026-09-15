<template>
  <div class="cart-page">
    <h2 class="page-title">我的购物车</h2>
    <el-table :data="cartList" @selection-change="handleSelectionChange" v-loading="loading">
      <el-table-column type="selection" width="50" />
      <el-table-column label="商品" min-width="300">
        <template #default="{ row }">
          <div class="product-cell">
            <img :src="row.productImage || defaultImg" class="cart-img" />
            <span>{{ row.productName }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="单价" width="120">
        <template #default="{ row }">
          <span class="price">¥{{ row.price }}</span>
        </template>
      </el-table-column>
      <el-table-column label="数量" width="160">
        <template #default="{ row }">
          <el-input-number v-model="row.quantity" :min="1" size="small" @change="(val) => handleQuantityChange(row.id, val)" />
        </template>
      </el-table-column>
      <el-table-column label="小计" width="120">
        <template #default="{ row }">
          <span class="subtotal">¥{{ row.subtotal }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button type="danger" link @click="handleRemove(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && cartList.length === 0" description="购物车是空的">
      <el-button type="primary" @click="$router.push('/products')">去逛逛</el-button>
    </el-empty>

    <div class="cart-footer" v-if="cartList.length > 0">
      <div class="footer-left">
        <el-button type="danger" link @click="handleClear">清空购物车</el-button>
      </div>
      <div class="footer-right">
        <span>已选 {{ selectedItems.length }} 件，合计：</span>
        <span class="total-price">¥{{ totalAmount }}</span>
        <el-button type="danger" size="large" :disabled="selectedItems.length === 0" @click="goSettle">去结算</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getCartList, updateCartQuantity, removeFromCart, clearCart } from '@/api/cart'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const cartList = ref([])
const selectedItems = ref([])
const loading = ref(false)
const defaultImg = 'https://via.placeholder.com/80x80?text=Product'

const totalAmount = computed(() => {
  return selectedItems.value.reduce((sum, item) => sum + Number(item.subtotal), 0).toFixed(2)
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getCartList()
    cartList.value = res.data
  } catch (e) {} finally {
    loading.value = false
  }
}

const handleSelectionChange = (selection) => {
  selectedItems.value = selection
}

const handleQuantityChange = async (id, val) => {
  await updateCartQuantity(id, val)
  fetchData()
}

const handleRemove = async (id) => {
  await ElMessageBox.confirm('确定删除该商品？', '提示', { type: 'warning' })
  await removeFromCart(id)
  ElMessage.success('已删除')
  fetchData()
}

const handleClear = async () => {
  await ElMessageBox.confirm('确定清空购物车？', '提示', { type: 'warning' })
  await clearCart()
  ElMessage.success('购物车已清空')
  fetchData()
}

const goSettle = () => {
  const cartIds = selectedItems.value.map(item => item.id)
  router.push({ path: '/order/confirm', query: { cartIds: cartIds.join(',') } })
}

onMounted(fetchData)
</script>

<style scoped>
.page-title { font-size: 22px; margin-bottom: 20px; }
.product-cell { display: flex; align-items: center; gap: 12px; }
.cart-img { width: 60px; height: 60px; object-fit: cover; border-radius: 4px; }
.price { color: #666; }
.subtotal { color: #f56c6c; font-weight: bold; }
.cart-footer {
  display: flex; justify-content: space-between; align-items: center;
  background: #fff; padding: 16px 20px; border-radius: 8px; margin-top: 16px;
}
.footer-right { display: flex; align-items: center; gap: 12px; }
.total-price { font-size: 24px; color: #f56c6c; font-weight: bold; }
</style>
