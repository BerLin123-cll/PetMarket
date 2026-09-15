<template>
  <div class="home">
    <el-carousel height="360px" class="banner">
      <el-carousel-item v-for="i in 3" :key="i">
        <div class="banner-item" :style="{ background: bannerColors[i-1] }">
          <h2>{{ bannerTexts[i-1] }}</h2>
        </div>
      </el-carousel-item>
    </el-carousel>

    <div class="section">
      <h2 class="section-title">商品分类</h2>
      <div class="category-grid">
        <div
          v-for="cat in categories"
          :key="cat.id"
          class="category-card"
          @click="$router.push({ path: '/products', query: { categoryId: cat.id } })"
        >
          <el-icon size="36" color="#409eff"><Goods /></el-icon>
          <span>{{ cat.name }}</span>
        </div>
      </div>
    </div>

    <div class="section">
      <h2 class="section-title">热门商品</h2>
      <el-row :gutter="20">
        <el-col :span="6" v-for="item in hotProducts" :key="item.id">
          <el-card shadow="hover" class="product-card" @click="$router.push(`/product/${item.id}`)">
            <img :src="item.mainImage || defaultImg" class="product-img" />
            <div class="product-info">
              <h4 class="product-name">{{ item.name }}</h4>
              <p class="product-price">¥{{ item.price }}</p>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getCategoryList } from '@/api/category'
import { getHotProducts } from '@/api/product'

const categories = ref([])
const hotProducts = ref([])
const defaultImg = 'https://via.placeholder.com/280x200?text=Pet+Product'
const bannerColors = ['linear-gradient(135deg, #667eea 0%, #764ba2 100%)', 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)', 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)']
const bannerTexts = ['🐾 宠物用品精选 · 品质保障', '🐶 新品上架 · 限时优惠', '🐱 关爱宠物 · 从这里开始']

onMounted(async () => {
  const [catRes, hotRes] = await Promise.all([getCategoryList(), getHotProducts({ pageNum: 1, pageSize: 8 })])
  categories.value = catRes.data
  hotProducts.value = hotRes.data.records
})
</script>

<style scoped>
.banner { border-radius: 8px; overflow: hidden; margin-bottom: 30px; }
.banner-item { height: 100%; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 28px; }
.section { margin-bottom: 40px; }
.section-title { font-size: 22px; margin-bottom: 20px; padding-left: 10px; border-left: 4px solid #409eff; }
.category-grid { display: flex; gap: 16px; flex-wrap: wrap; }
.category-card {
  width: 120px; height: 100px; background: #fff; border-radius: 8px; display: flex;
  flex-direction: column; align-items: center; justify-content: center; gap: 8px;
  cursor: pointer; transition: all 0.3s; border: 1px solid #eee;
}
.category-card:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.1); transform: translateY(-2px); }
.category-card span { font-size: 14px; color: #666; }
.product-card { cursor: pointer; border-radius: 8px; overflow: hidden; margin-bottom: 16px; }
.product-card:hover { transform: translateY(-4px); transition: all 0.3s; }
.product-img { width: 100%; height: 180px; object-fit: cover; border-radius: 4px; }
.product-info { padding: 8px 0; }
.product-name { font-size: 14px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.product-price { color: #f56c6c; font-size: 18px; font-weight: bold; margin-top: 6px; }
</style>
