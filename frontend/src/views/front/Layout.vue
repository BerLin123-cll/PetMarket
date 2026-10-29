<template>
  <div class="front-layout">
    <el-header class="site-header" height="60px">
      <div class="header-content">
        <div class="logo" @click="$router.push('/')">
          <span class="logo-mark">
            <svg viewBox="0 0 32 32" fill="none" aria-hidden="true">
              <ellipse cx="16" cy="21.1" rx="6.1" ry="4.9" />
              <ellipse cx="8.7" cy="14.5" rx="2.15" ry="2.85" transform="rotate(-24 8.7 14.5)" />
              <ellipse cx="13.2" cy="10.4" rx="2.1" ry="2.85" transform="rotate(-8 13.2 10.4)" />
              <ellipse cx="18.8" cy="10.4" rx="2.1" ry="2.85" transform="rotate(8 18.8 10.4)" />
              <ellipse cx="23.3" cy="14.5" rx="2.15" ry="2.85" transform="rotate(24 23.3 14.5)" />
            </svg>
          </span>
          <span class="logo-word">PetMarket</span>
          <span class="logo-sub">宠物商城</span>
        </div>

        <el-menu
          :default-active="activeMenu"
          mode="horizontal"
          :ellipsis="false"
          router
          class="nav-menu"
        >
          <el-menu-item index="/">首页</el-menu-item>
          <el-menu-item index="/products">全部商品</el-menu-item>
        </el-menu>

        <div class="header-right">
          <!-- 搜索框仅做前端占位，暂不接搜索逻辑 -->
          <el-input
            v-model="searchKeyword"
            class="header-search"
            placeholder="搜索狗粮、猫砂、玩具"
            clearable
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>

          <button class="icon-btn" type="button" aria-label="购物车" @click="$router.push('/cart')">
            <el-badge :value="cartCount" :hidden="!cartCount" :offset="[3, 3]">
              <el-icon :size="21"><ShoppingCart /></el-icon>
            </el-badge>
          </button>

          <template v-if="userStore.token">
            <el-dropdown trigger="click" @command="handleCommand">
              <span class="user-info">
                <el-avatar :size="30" :src="userStore.userInfo?.avatar" class="user-avatar">
                  {{ userStore.userInfo?.nickname?.charAt(0) || 'U' }}
                </el-avatar>
                <span class="username">
                  {{ userStore.userInfo?.nickname || userStore.userInfo?.username || '测试用户' }}
                </span>
                <el-icon class="user-arrow"><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                  <el-dropdown-item command="pets">我的宠物</el-dropdown-item>
                  <el-dropdown-item command="orders">我的订单</el-dropdown-item>
                  <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button type="primary" link @click="$router.push('/login')">登录</el-button>
            <el-button link @click="$router.push('/register')">注册</el-button>
          </template>
        </div>
      </div>
    </el-header>

    <el-main class="main-content">
      <router-view />
    </el-main>

    <el-footer class="site-footer" height="auto">
      <div class="footer-inner">
        <div class="footer-brand">
          <span class="footer-logo">PetMarket</span>
          <span class="footer-slogan">为每一种毛孩子，挑一份日常好物。</span>
        </div>
        <div class="footer-meta">
          <span>© {{ currentYear }} PetMarket 宠物商城</span>
          <span class="footer-dot">·</span>
          <span>用心挑选，安心陪伴</span>
        </div>
      </div>
    </el-footer>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getCartList } from '@/api/cart'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const cartCount = ref(0)
const searchKeyword = ref('')
const currentYear = new Date().getFullYear()

const activeMenu = computed(() => route.path)

const fetchCartCount = async () => {
  if (!userStore.token) return
  try {
    const res = await getCartList()
    cartCount.value = res.data.length
  } catch (e) {
    // 静默失败，不影响导航
  }
}

const refreshCart = () => {
  if (userStore.token) fetchCartCount()
}

const handleCommand = (cmd) => {
  if (cmd === 'profile') router.push('/user/profile')
  else if (cmd === 'pets') router.push('/user/pets')
  else if (cmd === 'orders') router.push('/user/orders')
  else if (cmd === 'logout') {
    userStore.logout()
    cartCount.value = 0
    router.push('/')
  }
}

onMounted(() => {
  if (userStore.token) {
    userStore.fetchUserInfo()
    fetchCartCount()
  }
  window.addEventListener('pm:cart-updated', refreshCart)
})

onBeforeUnmount(() => {
  window.removeEventListener('pm:cart-updated', refreshCart)
})
</script>

<style scoped>
.front-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

/* ==================== 顶部导航 ==================== */
.site-header {
  position: sticky;
  top: 0;
  z-index: 100;
  height: 60px;
  padding: 0;
  background: var(--pm-surface);
  border-bottom: 1px solid #f0e7dd;
}

.header-content {
  display: flex;
  align-items: center;
  gap: 26px;
  width: 100%;
  max-width: var(--pm-max-width);
  height: 100%;
  padding: 0 20px;
  margin: 0 auto;
}

.logo {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: 9px;
  cursor: pointer;
}

.logo-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  color: var(--pm-primary);
  background: var(--pm-primary-soft);
  border-radius: 10px;
}

.logo-mark svg {
  width: 21px;
  height: 21px;
  stroke: currentColor;
  stroke-width: 1.7;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.logo-word {
  font-size: 18px;
  font-weight: 700;
  color: var(--pm-primary-dark);
  letter-spacing: -0.2px;
}

.logo-sub {
  padding-left: 9px;
  font-size: 12px;
  font-weight: 400;
  color: var(--pm-text-muted);
  letter-spacing: 1px;
  border-left: 1px solid var(--pm-border-warm);
}

.nav-menu {
  flex: 0 0 auto;
  height: 60px;
  background: transparent;
  border-bottom: none;
}

.nav-menu :deep(.el-menu-item) {
  height: 60px;
  padding: 0 17px;
  font-size: 14px;
  color: #6b5b4e;
  background: transparent;
  border-bottom: 2px solid transparent;
  transition: color 0.2s ease, border-color 0.2s ease;
}

.nav-menu :deep(.el-menu-item:hover) {
  color: var(--pm-ink);
  background: transparent;
}

.nav-menu :deep(.el-menu-item.is-active) {
  font-weight: 600;
  color: var(--pm-ink);
  background: transparent;
  border-bottom-color: var(--pm-primary);
}

.header-right {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: 14px;
  margin-left: auto;
  white-space: nowrap;
}

.header-search {
  width: 224px;
}

.header-search :deep(.el-input__wrapper) {
  height: 36px;
  padding: 0 13px;
  background: #f7f3ee;
  border: 1px solid transparent;
  border-radius: var(--pm-radius-pill);
  box-shadow: none;
  transition: background 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

.header-search :deep(.el-input__wrapper:hover) {
  background: #f4eee7;
}

.header-search :deep(.el-input__wrapper.is-focus) {
  background: #ffffff;
  border-color: #ebd9c8;
  box-shadow: 0 0 0 3px rgba(232, 131, 58, 0.08);
}

.header-search :deep(.el-input__prefix) {
  color: var(--pm-text-light);
}

.header-search :deep(.el-input__inner) {
  font-size: 13px;
  color: var(--pm-ink);
}

.header-search :deep(.el-input__inner::placeholder) {
  color: #b3a494;
}

.icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  color: #5d4c3f;
  background: transparent;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  transition: color 0.2s ease, background 0.2s ease;
}

.icon-btn:hover {
  color: var(--pm-primary);
  background: var(--pm-primary-soft);
}

.icon-btn :deep(.el-badge__content) {
  height: 16px;
  padding: 0 5px;
  font-size: 10px;
  line-height: 16px;
  border: none;
  background: var(--pm-primary);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 7px;
  cursor: pointer;
  outline: none;
}

.user-avatar {
  font-size: 13px;
  font-weight: 600;
  color: #b06a38;
  background: var(--pm-primary-soft);
}

.username {
  max-width: 84px;
  overflow: hidden;
  font-size: 13px;
  color: var(--pm-ink);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-arrow {
  font-size: 11px;
  color: var(--pm-text-light);
}

.header-right :deep(.el-button.is-link) {
  font-size: 13px;
  color: #6b5b4e;
}

.header-right :deep(.el-button.is-link.el-button--primary) {
  color: var(--pm-primary-dark);
}

/* ==================== 主内容 ==================== */
.main-content {
  flex: 1;
  width: 100%;
  max-width: var(--pm-max-width);
  padding: 24px 20px 0;
  margin: 0 auto;
}

/* ==================== 页脚 ==================== */
.site-footer {
  margin-top: 12px;
  padding: 0;
  background: #fffdfa;
  border-top: 1px solid var(--pm-border-warm);
}

.footer-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  width: 100%;
  max-width: var(--pm-max-width);
  padding: 22px 20px;
  margin: 0 auto;
}

.footer-brand {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.footer-logo {
  font-size: 15px;
  font-weight: 700;
  color: var(--pm-primary-dark);
}

.footer-slogan {
  font-size: 12px;
  color: var(--pm-text-muted);
}

.footer-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--pm-text-light);
}

.footer-dot {
  color: #d8c8b8;
}

/* ==================== 响应式 ==================== */
@media (max-width: 1080px) {
  .header-content {
    gap: 18px;
  }

  .header-search {
    width: 188px;
  }

  .nav-menu :deep(.el-menu-item) {
    padding: 0 13px;
  }
}

@media (max-width: 880px) {
  .header-search {
    display: none;
  }

  .logo-sub {
    display: none;
  }
}

@media (max-width: 640px) {
  .header-content {
    gap: 10px;
    padding: 0 14px;
  }

  .logo-word {
    font-size: 16px;
  }

  .logo-mark {
    width: 30px;
    height: 30px;
  }

  .nav-menu :deep(.el-menu-item) {
    padding: 0 9px;
    font-size: 13px;
  }

  .username {
    display: none;
  }

  .header-right {
    gap: 8px;
  }

  .footer-inner {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: 6px;
    padding: 18px 20px;
  }

  .footer-brand {
    flex-direction: column;
    gap: 2px;
  }
}

@media (max-width: 480px) {
  .header-content {
    gap: 6px;
    padding: 0 12px;
  }

  .logo {
    gap: 7px;
  }

  .logo-word {
    font-size: 15px;
  }

  .logo-mark {
    width: 28px;
    height: 28px;
  }

  .nav-menu :deep(.el-menu-item) {
    padding: 0 8px;
    font-size: 12.5px;
  }

  .header-right {
    gap: 4px;
  }

  .icon-btn {
    width: 32px;
    height: 32px;
  }

  /* 小屏优先保留「登录」，注册入口可后续从登录页进入 */
  .header-right :deep(.el-button.is-link:not(.el-button--primary)) {
    display: none;
  }
}
</style>
