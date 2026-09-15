<!-- 文件路径: E:\SpringDemo\PetMarket\frontend\src\views\admin\OrderManage.vue -->
<template>
  <div class="order-manage">
    <el-card>
      <div class="toolbar">
        <el-select v-model="statusFilter" placeholder="全部状态" clearable @change="handleFilterChange" style="width:150px">
          <el-option label="待支付" :value="0" />
          <el-option label="已支付" :value="1" />
          <el-option label="已发货" :value="2" />
          <el-option label="已完成" :value="3" />
          <el-option label="已取消" :value="4" />
        </el-select>
      </div>

      <el-table :data="orders" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="orderNo" label="订单号" width="200" />
        <el-table-column label="商品信息" min-width="260">
          <template #default="{ row }">
            <div v-for="item in row.items" :key="item.id" class="order-item-row">
              <span>{{ item.productName }} × {{ item.quantity }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="总金额" width="100">
          <template #default="{ row }">¥{{ row.totalAmount }}</template>
        </el-table-column>
        <el-table-column label="收货人" width="100">
          <template #default="{ row }">{{ row.receiverName }}</template>
        </el-table-column>
        <el-table-column prop="receiverPhone" label="手机号" width="130" />
        <el-table-column label="订单状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.orderStatus)" size="small">{{ statusText(row.orderStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ row.createTime }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.orderStatus === 1" type="primary" link @click="handleShip(row.id)">发货</el-button>
            <span v-else class="no-action">-</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @current-change="fetchData"
          @size-change="fetchData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAdminOrderList, shipOrder } from '@/api/admin'
import { ElMessage, ElMessageBox } from 'element-plus'

const orders = ref([])
const loading = ref(false)
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const statusFilter = ref(null)

const statusText = (s) => ['待支付', '已支付', '已发货', '已完成', '已取消'][s] || '未知'
const statusType = (s) => ['warning', 'primary', '', 'success', 'info'][s] || ''

const fetchData = async () => {
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (statusFilter.value !== null && statusFilter.value !== '') params.status = statusFilter.value
    const res = await getAdminOrderList(params)
    orders.value = res.data.records
    total.value = Number(res.data.total)
  } catch (e) {} finally {
    loading.value = false
  }
}

const handleFilterChange = () => {
  pageNum.value = 1
  fetchData()
}

const handleShip = async (id) => {
  await ElMessageBox.confirm('确定对该订单进行发货？', '提示', { type: 'warning' })
  await shipOrder(id)
  ElMessage.success('发货成功')
  fetchData()
}

onMounted(fetchData)
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.order-item-row { font-size: 13px; line-height: 1.8; }
.no-action { color: #ccc; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
