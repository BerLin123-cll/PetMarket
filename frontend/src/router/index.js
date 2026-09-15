import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useAdminStore } from '@/stores/admin'

const routes = [
  // ========== 前台商城 ==========
  {
    path: '/',
    component: () => import('@/views/front/Layout.vue'),
    children: [
      { path: '', name: 'Home', component: () => import('@/views/front/Home.vue') },
      { path: 'products', name: 'ProductList', component: () => import('@/views/front/ProductList.vue') },
      { path: 'product/:id', name: 'ProductDetail', component: () => import('@/views/front/ProductDetail.vue') },
      { path: 'cart', name: 'Cart', component: () => import('@/views/front/Cart.vue'), meta: { requireAuth: true } },
      { path: 'order/confirm', name: 'OrderConfirm', component: () => import('@/views/front/OrderConfirm.vue'), meta: { requireAuth: true } },
      { path: 'order/result', name: 'PayResult', component: () => import('@/views/front/PayResult.vue'), meta: { requireAuth: true } },
      {
        path: 'user',
        name: 'UserCenter',
        component: () => import('@/views/front/UserCenter.vue'),
        meta: { requireAuth: true },
        children: [
          { path: '', redirect: '/user/profile' },
          { path: 'profile', name: 'UserProfile', component: () => import('@/views/front/user/Profile.vue') },
          { path: 'address', name: 'UserAddress', component: () => import('@/views/front/user/Address.vue') },
          { path: 'orders', name: 'UserOrders', component: () => import('@/views/front/user/Orders.vue') }
        ]
      }
    ]
  },
  // 登录 / 注册
  { path: '/login', name: 'Login', component: () => import('@/views/front/Login.vue') },
  { path: '/register', name: 'Register', component: () => import('@/views/front/Register.vue') },

  // ========== 后台管理 ==========
  { path: '/admin/login', name: 'AdminLogin', component: () => import('@/views/admin/AdminLogin.vue') },
  {
    path: '/admin',
    component: () => import('@/views/admin/AdminLayout.vue'),
    meta: { requireAdmin: true },
    children: [
      { path: '', name: 'Dashboard', component: () => import('@/views/admin/Dashboard.vue') },
      { path: 'product', name: 'ProductManage', component: () => import('@/views/admin/ProductManage.vue') },
      { path: 'category', name: 'CategoryManage', component: () => import('@/views/admin/CategoryManage.vue') },
      { path: 'user', name: 'UserManage', component: () => import('@/views/admin/UserManage.vue') },
      { path: 'order', name: 'OrderManage', component: () => import('@/views/admin/OrderManage.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  if (to.meta.requireAuth) {
    const userStore = useUserStore()
    if (!userStore.token) {
      next({ path: '/login', query: { redirect: to.fullPath } })
      return
    }
  }
  if (to.meta.requireAdmin) {
    const adminStore = useAdminStore()
    if (!adminStore.token) {
      next('/admin/login')
      return
    }
  }
  next()
})

export default router
