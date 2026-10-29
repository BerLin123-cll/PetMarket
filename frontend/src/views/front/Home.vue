<template>
  <div class="home">
    <!-- ==================== 主 Banner ==================== -->
    <section class="hero-section">
      <el-carousel
        class="hero-carousel"
        height="340px"
        :interval="5200"
        arrow="hover"
      >
        <el-carousel-item v-for="slide in slides" :key="slide.key">
          <div class="hero-slide" :class="`tone-${slide.tone}`">
            <div class="hero-copy">
              <span class="hero-tag">{{ slide.tag }}</span>
              <h1 class="hero-title">{{ slide.title }}</h1>
              <p class="hero-desc">{{ slide.desc }}</p>
              <div class="hero-actions">
                <el-button type="primary" size="large" @click="$router.push('/products')">
                  {{ slide.cta }}
                </el-button>
              </div>
            </div>
            <div class="hero-art" :class="{ 'is-flipped': slide.flip }">
              <img :src="slide.image" :alt="slide.alt" />
            </div>
          </div>
        </el-carousel-item>
      </el-carousel>
    </section>

    <!-- ==================== 商品分类 ==================== -->
    <section class="page-section">
      <div class="section-head">
        <div>
          <h2 class="section-title">商品分类</h2>
          <p class="section-desc">按宠物种类，找到更合适的那一件</p>
        </div>
        <router-link class="section-more" to="/products">
          查看全部
          <el-icon><ArrowRight /></el-icon>
        </router-link>
      </div>

      <div v-if="categories.length" class="category-grid">
        <button
          v-for="cat in categoryCards"
          :key="cat.id"
          type="button"
          class="category-card"
          @click="$router.push({ path: '/products', query: { categoryId: cat.id } })"
        >
          <span
            class="category-icon"
            :style="{ backgroundColor: cat.theme.bg, color: cat.theme.color }"
          >
            <CategoryIcon :name="cat.theme.icon" />
          </span>
          <span class="category-name">{{ cat.name }}</span>
        </button>
      </div>

      <div v-else-if="loading" class="category-grid">
        <div v-for="n in 7" :key="n" class="category-card is-skeleton">
          <span class="skeleton-icon" />
          <span class="skeleton-line" />
        </div>
      </div>
    </section>

    <!-- ==================== 热门商品 ==================== -->
    <section class="page-section">
      <div class="section-head">
        <div>
          <h2 class="section-title">热门商品</h2>
          <p class="section-desc">毛孩子日常用得上的，都在这里</p>
        </div>
        <router-link class="section-more" to="/products">
          更多
          <el-icon><ArrowRight /></el-icon>
        </router-link>
      </div>

      <div v-loading="loading" class="products-grid" element-loading-background="transparent">
        <el-row :gutter="18">
          <el-col
            v-for="(item, index) in hotProducts"
            :key="item.id"
            :xs="12"
            :sm="12"
            :md="12"
            :lg="6"
            class="product-col"
          >
            <ProductCard :product="item" :badge="badgeFor(index)" />
          </el-col>
        </el-row>
        <el-empty v-if="!loading && !hotProducts.length" description="暂时没有热门商品" />
      </div>
    </section>

    <!-- ==================== 服务承诺 ==================== -->
    <section class="service-strip">
      <div class="service-item">
        <el-icon :size="22"><Goods /></el-icon>
        <div>
          <strong>用心选品</strong>
          <span>只上架认真挑选的日常用品</span>
        </div>
      </div>
      <div class="service-item">
        <el-icon :size="22"><Van /></el-icon>
        <div>
          <strong>安心配送</strong>
          <span>订单进度随时可查</span>
        </div>
      </div>
      <div class="service-item">
        <el-icon :size="22"><Service /></el-icon>
        <div>
          <strong>售后无忧</strong>
          <span>遇到问题，随时联系我们</span>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { getCategoryList } from '@/api/category'
import { getHotProducts } from '@/api/product'
import CategoryIcon from '@/components/CategoryIcon.vue'
import ProductCard from '@/components/ProductCard.vue'
import bannerDog from '@/assets/banner-dog.svg'
import bannerCat from '@/assets/banner-cat.svg'

const categories = ref([])
const hotProducts = ref([])
const loading = ref(true)

// Banner 文案与插画均为纯前端展示内容，图片使用本地 SVG，便于后续替换成运营图
const slides = [
  {
    key: 'new-arrival',
    tone: 'cream',
    tag: '新品上架',
    title: '给毛孩子的换季好物',
    desc: '从主粮到日常护理，挑一件它真正需要的。',
    cta: '去逛逛',
    image: bannerDog,
    alt: '狗狗插画'
  },
  {
    key: 'daily-care',
    tone: 'sage',
    tag: '日常护理',
    title: '掉毛季，也能从容一点',
    desc: '温和洗护与每日梳理，让家里清爽一些。',
    cta: '看护理好物',
    image: bannerCat,
    alt: '猫咪插画',
    flip: false
  },
  {
    key: 'safe-choice',
    tone: 'sand',
    tag: '安心之选',
    title: '成分讲清楚，才敢给它用',
    desc: '精选配方与材质，照顾敏感的小家伙。',
    cta: '查看精选',
    image: bannerCat,
    alt: '猫咪插画',
    flip: true
  }
]

// 分类名称 → 图标与浅色底托；后端分类名固定为 7 类，按关键字匹配，未命中时用爪印兜底
const categoryThemes = [
  { icon: 'dog', bg: '#f8e8d8', color: '#bd7642', match: (name) => name.includes('狗') },
  { icon: 'cat', bg: '#f6e6e0', color: '#b96b52', match: (name) => name.includes('猫') },
  { icon: 'fish', bg: '#e4eff2', color: '#4c7c90', match: (name) => /水族|鱼/.test(name) },
  { icon: 'bird', bg: '#f7eedb', color: '#a98336', match: (name) => name.includes('鸟') },
  { icon: 'rabbit', bg: '#eee8f4', color: '#7c6696', match: (name) => /小宠|兔|仓鼠|龙猫/.test(name) },
  { icon: 'reptile', bg: '#e6f0e2', color: '#5c8456', match: (name) => name.includes('爬') },
  { icon: 'medical', bg: '#f8e7e9', color: '#b25a66', match: (name) => /医疗|保健|健康/.test(name) }
]

const fallbackTheme = { icon: 'dog', bg: '#f7f1ea', color: '#a39384' }

const resolveTheme = (name) => {
  const matched = categoryThemes.find((theme) => theme.match(name || ''))
  return matched || fallbackTheme
}

const categoryCards = computed(() =>
  categories.value.map((cat) => ({
    ...cat,
    theme: resolveTheme(cat.name)
  }))
)

// 热门商品接口按创建时间倒序（后端 /api/product/hot 的实现），
// 这里仅用列表位置做展示角标；等接口返回 tag 字段后可替换为真实标签。
const badgeFor = (index) => {
  if (index < 2) return '热销'
  if (index === 2) return '新品'
  return ''
}

onMounted(async () => {
  try {
    const [catRes, hotRes] = await Promise.all([
      getCategoryList(),
      // 多取一些再挑 8 个：优先展示已有主图的商品，避免首页整屏都是占位图。
      // 数据仍全部来自后端热门接口，没有在前端额外造商品。
      getHotProducts({ pageNum: 1, pageSize: 50 })
    ])
    categories.value = catRes.data || []
    const records = hotRes.data?.records || []
    hotProducts.value = [
      ...records.filter((item) => item.mainImage),
      ...records.filter((item) => !item.mainImage)
    ].slice(0, 8)
  } catch (e) {
    // 错误提示由 request 拦截器统一处理，首页保持可用的空状态
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.home {
  padding-bottom: 8px;
}

/* ==================== Banner ==================== */
.hero-section {
  margin-bottom: 44px;
}

.hero-carousel {
  overflow: hidden;
  border-radius: 16px;
  background: #fbf3ea;
  box-shadow: var(--pm-shadow-card);
}

.hero-slide {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
  padding: 0 64px;
}

.tone-cream {
  background: #fbf3ea;
}

.tone-sage {
  background: #f1f5ef;
}

.tone-sand {
  background: #f8f1e8;
}

.hero-copy {
  max-width: 520px;
}

.hero-tag {
  display: inline-flex;
  align-items: center;
  padding: 5px 13px;
  font-size: 12px;
  font-weight: 600;
  line-height: 18px;
  color: #a45f28;
  letter-spacing: 0.6px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(232, 131, 58, 0.2);
  border-radius: var(--pm-radius-pill);
}

.hero-title {
  margin: 15px 0 10px;
  font-size: 36px;
  font-weight: 700;
  line-height: 1.25;
  color: var(--pm-ink);
  letter-spacing: -0.6px;
}

.hero-desc {
  max-width: 420px;
  font-size: 14px;
  line-height: 1.8;
  color: #6b5b4e;
}

.hero-actions {
  margin-top: 22px;
}

.hero-actions :deep(.el-button) {
  height: 42px;
  padding: 0 26px;
  font-size: 14px;
  border-radius: var(--pm-radius-btn);
}

.hero-art {
  flex-shrink: 0;
  width: 370px;
  margin-right: 6px;
}

.hero-art.is-flipped img {
  transform: scaleX(-1);
}

.hero-art img {
  width: 100%;
  height: auto;
}

/* 轮播控件：小圆点 + 半透明圆形箭头 */
.hero-carousel :deep(.el-carousel__indicators--horizontal) {
  bottom: 14px;
}

.hero-carousel :deep(.el-carousel__indicator) {
  padding: 6px 4px;
}

.hero-carousel :deep(.el-carousel__button) {
  width: 6px;
  height: 6px;
  background: rgba(61, 46, 34, 0.24);
  border-radius: var(--pm-radius-pill);
  opacity: 1;
  transition: width 0.25s ease, background 0.25s ease;
}

.hero-carousel :deep(.el-carousel__indicator.is-active .el-carousel__button) {
  width: 18px;
  background: var(--pm-primary);
}

.hero-carousel :deep(.el-carousel__arrow) {
  width: 32px;
  height: 32px;
  color: #6b5b4e;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(61, 46, 34, 0.06);
  box-shadow: 0 2px 8px rgba(61, 46, 34, 0.06);
  opacity: 0;
  transition: opacity 0.2s ease, background 0.2s ease, color 0.2s ease;
}

.hero-carousel:hover :deep(.el-carousel__arrow) {
  opacity: 1;
}

.hero-carousel :deep(.el-carousel__arrow:hover) {
  color: var(--pm-primary);
  background: #ffffff;
}

.hero-carousel :deep(.el-carousel__arrow i) {
  font-size: 13px;
}

/* ==================== 通用区块 ==================== */
.page-section {
  margin-bottom: 44px;
}

.section-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.section-title {
  font-size: 22px;
  font-weight: 700;
  line-height: 1.3;
  color: var(--pm-ink);
  letter-spacing: -0.2px;
}

.section-desc {
  margin-top: 5px;
  font-size: 13px;
  color: var(--pm-text-muted);
}

.section-more {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding-bottom: 2px;
  font-size: 13px;
  color: var(--pm-text-muted);
  transition: color 0.2s ease;
}

.section-more:hover {
  color: var(--pm-primary);
}

.section-more .el-icon {
  font-size: 12px;
}

/* ==================== 商品分类 ==================== */
.category-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 14px;
}

.category-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  min-width: 0;
  padding: 18px 8px 14px;
  font-family: inherit;
  background: var(--pm-surface);
  border: 1px solid var(--pm-border-warm);
  border-radius: var(--pm-radius-card);
  cursor: pointer;
  transition: transform 0.22s ease, box-shadow 0.22s ease, border-color 0.22s ease;
}

.category-card:hover {
  transform: translateY(-2px);
  border-color: #eadcca;
  box-shadow: 0 6px 18px rgba(61, 46, 34, 0.08);
}

.category-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 52px;
  height: 52px;
  font-size: 27px;
  border-radius: 50%;
}

.category-name {
  overflow: hidden;
  font-size: 13px;
  font-weight: 500;
  color: var(--pm-ink);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.category-card.is-skeleton {
  cursor: default;
}

.skeleton-icon {
  width: 52px;
  height: 52px;
  background: #f4ede5;
  border-radius: 50%;
}

.skeleton-line {
  width: 56px;
  height: 12px;
  background: #f4ede5;
  border-radius: 6px;
}

/* ==================== 热门商品 ==================== */
.products-grid {
  min-height: 120px;
}

.product-col {
  margin-bottom: 18px;
}

/* ==================== 服务承诺 ==================== */
.service-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0;
  margin: 0 0 44px;
  padding: 20px 8px;
  background: #fffdfa;
  border: 1px solid var(--pm-border-warm);
  border-radius: var(--pm-radius-card);
}

.service-item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 0 18px;
  color: var(--pm-primary);
}

.service-item + .service-item {
  border-left: 1px solid var(--pm-border-warm);
}

.service-item div {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.service-item strong {
  font-size: 13px;
  font-weight: 600;
  color: var(--pm-ink);
}

.service-item span {
  font-size: 12px;
  color: var(--pm-text-light);
}

/* ==================== 响应式 ==================== */
@media (max-width: 1100px) {
  .hero-slide {
    padding: 0 44px;
  }

  .hero-art {
    width: 320px;
  }

  .hero-title {
    font-size: 32px;
  }

  .category-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 860px) {
  .hero-slide {
    padding: 0 36px;
  }

  .hero-art {
    display: none;
  }

  .hero-copy {
    max-width: 100%;
  }

  .hero-title {
    font-size: 30px;
  }
}

@media (max-width: 640px) {
  .hero-slide {
    padding: 0 24px;
  }

  .hero-title {
    margin-top: 12px;
    font-size: 26px;
  }

  .hero-desc {
    font-size: 13px;
  }

  .hero-actions {
    margin-top: 18px;
  }

  .section-head {
    align-items: flex-end;
    margin-bottom: 16px;
  }

  .section-title {
    font-size: 20px;
  }

  .section-desc {
    display: none;
  }

  .category-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
  }

  .category-card {
    padding: 14px 6px 12px;
  }

  .category-icon {
    width: 46px;
    height: 46px;
    font-size: 24px;
  }

  .service-strip {
    grid-template-columns: 1fr;
    gap: 14px;
    padding: 16px;
  }

  .service-item {
    justify-content: flex-start;
    padding: 0;
  }

  .service-item + .service-item {
    padding-top: 14px;
    border-top: 1px solid var(--pm-border-warm);
    border-left: none;
  }
}
</style>
