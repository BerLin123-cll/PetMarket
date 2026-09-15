<!-- 文件路径: E:\SpringDemo\PetMarket\frontend\src\views\admin\UserManage.vue -->
<template>
  <div class="user-manage">
    <el-card>
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="搜索用户名/昵称/手机号" clearable style="width:280px" @clear="fetchData" @keyup.enter="fetchData">
          <template #append><el-button icon="Search" @click="fetchData" /></template>
        </el-input>
      </div>

      <el-table :data="users" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" width="130" />
        <el-table-column prop="nickname" label="昵称" width="130" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column label="性别" width="80">
          <template #default="{ row }">
            {{ row.gender === 1 ? '男' : row.gender === 0 ? '女' : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="注册时间" min-width="160">
          <template #default="{ row }">{{ row.createTime }}</template>
        </el-table-column>
        <el-table-column label="账号状态" width="120">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              active-text="正常"
              inactive-text="禁用"
              @change="(val) => handleToggle(row.id, val)"
            />
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
import { getAdminUserList, toggleUserStatus } from '@/api/admin'
import { ElMessage, ElMessageBox } from 'element-plus'

const users = ref([])
const loading = ref(false)
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const keyword = ref('')

const fetchData = async () => {
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (keyword.value) params.keyword = keyword.value
    const res = await getAdminUserList(params)
    users.value = res.data.records
    total.value = Number(res.data.total)
  } catch (e) {} finally {
    loading.value = false
  }
}

const handleToggle = async (id, val) => {
  const status = val ? 1 : 0
  const action = val ? '启用' : '禁用'
  await ElMessageBox.confirm(`确定${action}该用户？`, '提示', { type: 'warning' })
  await toggleUserStatus(id, status)
  ElMessage.success(`已${action}`)
  fetchData()
}

onMounted(fetchData)
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
