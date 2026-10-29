<template>
  <div class="product-list-page">
    <div class="list-head">
      <div>
        <h1 class="page-title">全部商品</h1>
        <p class="page-desc">按分类、关键词或价格，挑选适合毛孩子的日常用品</p>
      </div>
      <span class="result-count">共 {{ total }} 件商品</span>
    </div>

    <div class="filter-bar">
      <el-input
        v-model="query.keyword"
        placeholder="搜索商品..."
        clearable
        class="search-input"
        @clear="fetchData"
        @keyup.enter="fetchData"
      >
        <template #append>
          <el-button icon="Search" @click="fetchData" />
        </template>
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

    <div v-loading="loading" class="products-wrap" element-loading-background="transparent">
      <el-row :gutter="18">
        <el-col
          v-for="item in products"
          :key="item.id"
          :xs="12"
          :sm="12"
          :md="12"
          :lg="6"
          class="product-col"
        >
          <ProductCard :product="item" />
        </el-col>
      </el-row>

      <el-empty v-if="!loading && products.length === 0" description="暂无商品" />
    </div>

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
import ProductCard from '@/components/ProductCard.vue'

const route = useRoute()
const products = ref([])
const categories = ref([])
const loading = ref(false)
const total = ref(0)
const sortValue = ref('')

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
.list-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--pm-ink);
}

.page-desc {
  margin-top: 5px;
  font-size: 13px;
  color: var(--pm-text-muted);
}

.result-count {
  font-size: 13px;
  color: var(--pm-text-light);
}

.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  margin-bottom: 22px;
  background: var(--pm-surface);
  border: 1px solid var(--pm-border-warm);
  border-radius: var(--pm-radius-card);
  box-shadow: var(--pm-shadow-card);
}

.search-input {
  width: 320px;
}

.filter-right {
  display: flex;
  gap: 12px;
}

.products-wrap {
  min-height: 160px;
}

.product-col {
  margin-bottom: 18px;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin: 10px 0 24px;
}

@media (max-width: 768px) {
  .list-head {
    align-items: flex-start;
    flex-direction: column;
    gap: 4px;
  }

  .filter-bar {
    align-items: stretch;
    flex-direction: column;
    gap: 10px;
  }

  .search-input {
    width: 100%;
  }

  .filter-right {
    gap: 10px;
  }

  .filter-right :deep(.el-select) {
    flex: 1;
    width: auto !important;
  }
}
</style>
