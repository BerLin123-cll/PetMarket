<template>
  <div class="product-list-page">
    <div class="filter-bar">
      <el-input v-model="query.keyword" placeholder="搜索商品..." clearable style="width:300px" @clear="fetchData" @keyup.enter="fetchData">
        <template #append><el-button icon="Search" @click="fetchData" /></template>
      </el-input>
      <div class="filter-right">
        <el-select v-model="query.categoryId" placeholder="全部分类" clearable @change="fetchData" style="width:150px">
          <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-select v-model="sortValue" placeholder="排序" @change="handleSort" style="width:140px">
          <el-option label="默认排序" value="" />
          <el-option label="价格从低到高" value="price_asc" />
          <el-option label="价格从高到低" value="price_desc" />
        </el-select>
      </div>
    </div>

    <el-row :gutter="20">
      <el-col :span="6" v-for="item in products" :key="item.id">
        <el-card shadow="hover" class="product-card" @click="$router.push(`/product/${item.id}`)">
          <img :src="item.mainImage || defaultImg" class="product-img" />
          <div class="product-info">
            <h4 class="product-name">{{ item.name }}</h4>
            <p class="product-cat">{{ item.categoryName }}</p>
            <p class="product-price">¥{{ item.price }}</p>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-empty v-if="!loading && products.length === 0" description="暂无商品" />

    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[8, 16, 24]"
        layout="total, sizes, prev, pager, next"
        @current-change="fetchData"
        @size-change="fetchData"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getProductList } from '@/api/product'
import { getCategoryList } from '@/api/category'

const route = useRoute()
const products = ref([])
const categories = ref([])
const loading = ref(false)
const total = ref(0)
const sortValue = ref('')
const defaultImg = 'https://via.placeholder.com/280x200?text=Pet+Product'

const query = reactive({
  keyword: '',
  categoryId: null,
  pageNum: 1,
  pageSize: 8,
  sortField: '',
  sortOrder: ''
})

const handleSort = (val) => {
  if (val) {
    const [field, order] = val.split('_')
    query.sortField = field
    query.sortOrder = order
  } else {
    query.sortField = ''
    query.sortOrder = ''
  }
  fetchData()
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getProductList(query)
    products.value = res.data.records
    total.value = Number(res.data.total)
  } catch (e) {} finally {
    loading.value = false
  }
}

onMounted(async () => {
  const catRes = await getCategoryList()
  categories.value = catRes.data
  if (route.query.categoryId) {
    query.categoryId = Number(route.query.categoryId)
  }
  if (route.query.keyword) {
    query.keyword = route.query.keyword
  }
  fetchData()
})
</script>

<style scoped>
.filter-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; background: #fff; padding: 16px; border-radius: 8px; }
.filter-right { display: flex; gap: 12px; }
.product-card { cursor: pointer; border-radius: 8px; margin-bottom: 16px; }
.product-card:hover { transform: translateY(-4px); transition: all 0.3s; }
.product-img { width: 100%; height: 180px; object-fit: cover; border-radius: 4px; }
.product-name { font-size: 14px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.product-cat { font-size: 12px; color: #999; margin: 4px 0; }
.product-price { color: #f56c6c; font-size: 18px; font-weight: bold; }
.pagination-wrap { display: flex; justify-content: center; margin-top: 24px; }
</style>
