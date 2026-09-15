<!-- 文件路径: E:\SpringDemo\PetMarket\frontend\src\views\front\user\Address.vue -->
<template>
  <el-card>
    <div style="display:flex;justify-content:space-between;margin-bottom:20px">
      <h3>收货地址</h3>
      <el-button type="primary" @click="openDialog()">新增地址</el-button>
    </div>
    <el-table :data="addresses" v-loading="loading">
      <el-table-column prop="receiverName" label="收货人" width="100" />
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column prop="detailAddress" label="详细地址" />
      <el-table-column label="默认" width="80">
        <template #default="{ row }">
          <el-tag v-if="row.isDefault === 1" type="danger" size="small">默认</el-tag>
          <el-button v-else type="primary" link size="small" @click="handleSetDefault(row.id)">设为默认</el-button>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button type="primary" link @click="openDialog(row)">编辑</el-button>
          <el-button type="danger" link @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑地址' : '新增地址'" width="480px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="收货人"><el-input v-model="form.receiverName" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="详细地址"><el-input v-model="form.detailAddress" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getAddressList, addAddress, updateAddress, deleteAddress, setDefaultAddress } from '@/api/address'
import { ElMessage, ElMessageBox } from 'element-plus'

const addresses = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const form = reactive({ receiverName: '', phone: '', detailAddress: '' })

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getAddressList()
    addresses.value = res.data
  } catch (e) {} finally {
    loading.value = false
  }
}

const openDialog = (row) => {
  if (row) {
    isEdit.value = true
    editId.value = row.id
    Object.assign(form, { receiverName: row.receiverName, phone: row.phone, detailAddress: row.detailAddress })
  } else {
    isEdit.value = false
    editId.value = null
    Object.assign(form, { receiverName: '', phone: '', detailAddress: '' })
  }
  dialogVisible.value = true
}

const handleSave = async () => {
  if (isEdit.value) {
    await updateAddress(editId.value, form)
  } else {
    await addAddress(form)
  }
  ElMessage.success(isEdit.value ? '修改成功' : '添加成功')
  dialogVisible.value = false
  fetchData()
}

const handleDelete = async (id) => {
  await ElMessageBox.confirm('确定删除该地址？', '提示', { type: 'warning' })
  await deleteAddress(id)
  ElMessage.success('已删除')
  fetchData()
}

const handleSetDefault = async (id) => {
  await setDefaultAddress(id)
  ElMessage.success('设置成功')
  fetchData()
}

onMounted(fetchData)
</script>
