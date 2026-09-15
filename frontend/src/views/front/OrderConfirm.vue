<template>
  <div class="confirm-page">
    <h2 class="page-title">确认订单</h2>

    <el-card class="section-card">
      <h3>选择收货地址</h3>
      <el-radio-group v-model="selectedAddressId" class="address-group">
        <div v-for="addr in addresses" :key="addr.id" class="address-item">
          <el-radio :value="addr.id">
            <strong>{{ addr.receiverName }}</strong> {{ addr.phone }}
            <br /><span class="addr-detail">{{ addr.detailAddress }}</span>
            <el-tag v-if="addr.isDefault === 1" size="small" type="danger">默认</el-tag>
          </el-radio>
        </div>
      </el-radio-group>
      <el-button type="primary" link @click="showAddressDialog = true">+ 新增地址</el-button>
    </el-card>

    <el-card class="section-card">
      <h3>商品清单</h3>
      <el-table :data="selectedCartItems" border>
        <el-table-column label="商品" min-width="280">
          <template #default="{ row }">
            <div style="display:flex;align-items:center;gap:10px">
              <img :src="row.productImage || 'https://via.placeholder.com/50'" style="width:50px;height:50px;border-radius:4px" />
              <span>{{ row.productName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="price" label="单价" width="100">
          <template #default="{ row }">¥{{ row.price }}</template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="80" />
        <el-table-column label="小计" width="120">
          <template #default="{ row }">¥{{ row.subtotal }}</template>
        </el-table-column>
      </el-table>
      <div class="total-row">
        共 {{ totalQuantity }} 件商品，合计：<span class="total-price">¥{{ totalAmount }}</span>
      </div>
    </el-card>

    <div class="submit-row">
      <el-button type="danger" size="large" :loading="submitting" @click="handleSubmit" :disabled="!selectedAddressId">
        提交订单
      </el-button>
    </div>

    <el-dialog v-model="showAddressDialog" title="新增收货地址" width="480px">
      <el-form :model="newAddr" label-width="80px">
        <el-form-item label="收货人"><el-input v-model="newAddr.receiverName" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="newAddr.phone" /></el-form-item>
        <el-form-item label="详细地址"><el-input v-model="newAddr.detailAddress" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddressDialog = false">取消</el-button>
        <el-button type="primary" @click="handleAddAddr">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getAddressList, addAddress } from '@/api/address'
import { getCartList } from '@/api/cart'
import { createOrder } from '@/api/order'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const addresses = ref([])
const cartList = ref([])
const selectedAddressId = ref(null)
const submitting = ref(false)
const showAddressDialog = ref(false)
const newAddr = reactive({ receiverName: '', phone: '', detailAddress: '' })

const cartIds = computed(() => route.query.cartIds ? route.query.cartIds.split(',').map(Number) : [])

const selectedCartItems = computed(() => cartList.value.filter(item => cartIds.value.includes(item.id)))
const totalQuantity = computed(() => selectedCartItems.value.reduce((s, i) => s + i.quantity, 0))
const totalAmount = computed(() => selectedCartItems.value.reduce((s, i) => s + Number(i.subtotal), 0).toFixed(2))

const handleSubmit = async () => {
  if (!selectedAddressId.value) { ElMessage.warning('请选择收货地址'); return }
  submitting.value = true
  try {
    const res = await createOrder({ addressId: selectedAddressId.value, cartIds: cartIds.value })
    ElMessage.success('订单创建成功')
    router.push({ path: '/order/result', query: { orderNo: res.data.orderNo } })
  } catch (e) {} finally {
    submitting.value = false
  }
}

const handleAddAddr = async () => {
  await addAddress(newAddr)
  ElMessage.success('地址添加成功')
  showAddressDialog.value = false
  const res = await getAddressList()
  addresses.value = res.data
  if (!selectedAddressId.value && addresses.value.length > 0) {
    selectedAddressId.value = addresses.value[0].id
  }
}

onMounted(async () => {
  const [addrRes, cartRes] = await Promise.all([getAddressList(), getCartList()])
  addresses.value = addrRes.data
  cartList.value = cartRes.data
  const defaultAddr = addresses.value.find(a => a.isDefault === 1)
  if (defaultAddr) selectedAddressId.value = defaultAddr.id
  else if (addresses.value.length > 0) selectedAddressId.value = addresses.value[0].id
})
</script>

<style scoped>
.page-title { font-size: 22px; margin-bottom: 20px; }
.section-card { margin-bottom: 20px; }
.section-card h3 { margin-bottom: 16px; }
.address-group { display: flex; flex-direction: column; gap: 12px; }
.address-item { padding: 12px; border: 1px solid #eee; border-radius: 8px; }
.addr-detail { color: #666; font-size: 13px; }
.total-row { text-align: right; margin-top: 16px; font-size: 16px; }
.total-price { color: #f56c6c; font-size: 22px; font-weight: bold; }
.submit-row { text-align: right; margin-top: 20px; }
</style>
