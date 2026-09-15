<!-- 文件路径: E:\SpringDemo\PetMarket\frontend\src\views\admin\ProductManage.vue -->
<template>
  <div class="product-manage">
    <el-card>
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input v-model="query.keyword" placeholder="搜索商品名称" clearable style="width:220px" @clear="fetchData" @keyup.enter="fetchData">
            <template #append><el-button icon="Search" @click="fetchData" /></template>
          </el-input>
          <el-select v-model="query.categoryId" placeholder="全部分类" clearable @change="fetchData" style="width:150px">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </div>
        <el-button type="primary" @click="openDialog()">新增商品</el-button>
      </div>

      <el-table :data="products" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="图片" width="100">
          <template #default="{ row }">
            <img :src="row.mainImage || 'https://via.placeholder.com/60'" style="width:60px;height:60px;border-radius:4px;object-fit:cover" />
          </template>
        </el-table-column>
        <el-table-column prop="name" label="商品名称" min-width="160" show-overflow-tooltip />
        <el-table-column label="分类" width="100">
          <template #default="{ row }">{{ getCategoryName(row.categoryId) }}</template>
        </el-table-column>
        <el-table-column label="价格" width="100">
          <template #default="{ row }">¥{{ row.price }}</template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="80" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              active-text="上架"
              inactive-text="下架"
              @change="(val) => handleToggle(row.id, val)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDialog(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @current-change="fetchData"
          @size-change="fetchData"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑商品' : '新增商品'" width="600px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入商品名称" />
        </el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择分类" style="width:100%">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="价格" prop="price">
          <el-input-number v-model="form.price" :min="0.01" :precision="2" :step="1" style="width:100%" />
        </el-form-item>
        <el-form-item label="库存" prop="stock">
          <el-input-number v-model="form.stock" :min="0" :step="1" style="width:100%" />
        </el-form-item>
        <el-form-item label="商品图片" prop="mainImage">
          <ImageUpload v-model="form.mainImage" biz-type="product" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="商品描述（选填）" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">上架</el-radio>
            <el-radio :value="0">下架</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getAdminProductList, createProduct, updateProduct, deleteProduct, toggleProductStatus } from '@/api/admin'
import { getAdminCategoryList } from '@/api/admin'
import ImageUpload from '@/components/ImageUpload.vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const products = ref([])
const categories = ref([])
const loading = ref(false)
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const submitting = ref(false)
const formRef = ref()

const query = reactive({ keyword: '', categoryId: null, pageNum: 1, pageSize: 10 })

const defaultForm = { name: '', categoryId: null, price: 0, stock: 0, mainImage: '', description: '', status: 1 }
const form = reactive({ ...defaultForm })

const formRules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }],
  stock: [{ required: true, message: '请输入库存', trigger: 'blur' }]
}

const getCategoryName = (id) => {
  const cat = categories.value.find(c => c.id === id)
  return cat ? cat.name : '-'
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getAdminProductList(query)
    products.value = res.data.records
    total.value = Number(res.data.total)
  } catch (e) {} finally {
    loading.value = false
  }
}

const fetchCategories = async () => {
  try {
    const res = await getAdminCategoryList()
    categories.value = res.data
  } catch (e) {}
}

const openDialog = (row) => {
  if (row) {
    isEdit.value = true
    editId.value = row.id
    Object.assign(form, {
      name: row.name, categoryId: row.categoryId, price: Number(row.price),
      stock: row.stock, mainImage: row.mainImage || '', description: row.description || '', status: row.status
    })
  } else {
    isEdit.value = false
    editId.value = null
    Object.assign(form, { ...defaultForm })
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate()
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateProduct(editId.value, form)
    } else {
      await createProduct(form)
    }
    ElMessage.success(isEdit.value ? '修改成功' : '添加成功')
    dialogVisible.value = false
    fetchData()
  } catch (e) {} finally {
    submitting.value = false
  }
}

const handleToggle = async (id, val) => {
  const status = val ? 1 : 0
  await toggleProductStatus(id, status)
  ElMessage.success(val ? '已上架' : '已下架')
  fetchData()
}

const handleDelete = async (id) => {
  await ElMessageBox.confirm('确定删除该商品？', '提示', { type: 'warning' })
  await deleteProduct(id)
  ElMessage.success('已删除')
  fetchData()
}

onMounted(() => {
  fetchData()
  fetchCategories()
})
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.toolbar-left { display: flex; gap: 12px; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
