<template>
  <div class="front-layout">
    <el-header class="header">
      <div class="header-content">
        <div class="logo" @click="$router.push('/')">
          <el-icon size="28"><Shop /></el-icon>
          <span>PetMarket 宠物商城</span>
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
          <el-badge :value="cartCount" :hidden="!cartCount" class="cart-badge">
            <el-icon size="22" @click="$router.push('/cart')" style="cursor:pointer"><ShoppingCart /></el-icon>
          </el-badge>
          <template v-if="userStore.token">
            <el-dropdown @command="handleCommand">
              <span class="user-info">
                <el-avatar :size="32" :src="userStore.userInfo?.avatar">
                  {{ userStore.userInfo?.nickname?.charAt(0) || 'U' }}
                </el-avatar>
                <span class="username">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</span>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                  <el-dropdown-item command="orders">我的订单</el-dropdown-item>
                  <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button type="primary" link @click="$router.push('/login')">登录</el-button>
            <el-button type="primary" link @click="$router.push('/register')">注册</el-button>
          </template>
        </div>
      </div>
    </el-header>
    <el-main class="main-content">
      <router-view />
    </el-main>
    <el-footer class="footer">
    </el-footer>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getCartList } from '@/api/cart'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const cartCount = ref(0)

const activeMenu = computed(() => route.path)

const fetchCartCount = async () => {
  if (!userStore.token) return
  try {
    const res = await getCartList()
    cartCount.value = res.data.length
  } catch (e) {}
}

const handleCommand = (cmd) => {
  if (cmd === 'profile') router.push('/user/profile')
  else if (cmd === 'orders') router.push('/user/orders')
  else if (cmd === 'logout') {
    userStore.logout()
    router.push('/')
  }
}

onMounted(() => {
  if (userStore.token) {
    userStore.fetchUserInfo()
    fetchCartCount()
  }
})
</script>

<style scoped>
.front-layout { min-height: 100vh; display: flex; flex-direction: column; }
.header {
  background: #fff; border-bottom: 1px solid #eee; padding: 0; height: 64px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06); position: sticky; top: 0; z-index: 100;
}
.header-content {
  max-width: 1200px; margin: 0 auto; display: flex; align-items: center;
  height: 100%; padding: 0 20px;
}
.logo {
  display: flex; align-items: center; gap: 8px; font-size: 20px; font-weight: bold;
  color: #409eff; cursor: pointer; white-space: nowrap;
}
.nav-menu { flex: 1; border-bottom: none; margin-left: 40px; }
.header-right { display: flex; align-items: center; gap: 16px; white-space: nowrap; }
.cart-badge { cursor: pointer; }
.user-info { display: flex; align-items: center; gap: 8px; cursor: pointer; }
.username { font-size: 14px; color: #333; }
.main-content { max-width: 1200px; margin: 0 auto; width: 100%; padding: 20px; flex: 1; }
.footer { text-align: center; color: #999; font-size: 13px; background: #fff; border-top: 1px solid #eee; }
</style>
