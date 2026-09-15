<!-- 文件路径: E:\SpringDemo\PetMarket\frontend\src\components\SplitLogin.vue -->
<!--
  登录页（V1.3 重新设计）：左右分屏 + 多角色分段切换
  ─────────────────────────────────────────────────────────────
  UI 设计原则（对齐企业级产品的登录页做法）：
    · 一个强调色、一个中性色阶，不用彩色渐变按钮
    · 字号阶梯固定：12 / 13 / 14 / 16 / 24 / 40，不随视口乱缩放
    · 左屏承担品牌叙事（实色深底 + 点阵纹理，不用模糊光斑与图案水印）
    · 右屏专注表单，输入框与按钮等高（48px）、圆角统一（10px）
    · 留白优先：品牌区内容左边缘统一，表单区上下居中且左右留白对称

  多角色切换：
    用户登录   → POST /api/user/login      → userStore  → 跳前台
    管理员登录 → POST /admin/auth/login    → adminStore → 跳 /admin
  分段控件用原生 button 自绘（不用 el-tabs），避免第三方组件事件时序问题，
  切换时同步 URL，保证刷新与分享链接都落到正确的角色。
-->
<template>
  <div class="auth">
    <!-- ==================== 左屏：品牌 ==================== -->
    <aside class="brand">
      <div class="brand-inner">
        <div class="brand-head">
          <span class="logo" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor"
                 stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="7.2" cy="9" r="2.1" />
              <circle cx="12" cy="7" r="2.1" />
              <circle cx="16.8" cy="9" r="2.1" />
              <path d="M12 12.4c2.8 0 4.9 1.9 4.9 4.1 0 1.6-1.2 2.6-2.7 2.6-.9 0-1.5-.4-2.2-.4s-1.3.4-2.2.4c-1.5 0-2.7-1-2.7-2.6 0-2.2 2.1-4.1 4.9-4.1z" />
            </svg>
          </span>
          <span class="logo-text">PetMarket</span>
        </div>

        <h1 class="brand-title">宠物用品，一次买齐</h1>
        <p class="brand-lead">
          从主粮、零食到玩具与清洁用品，覆盖犬、猫、水族与鸟类。<br />
          下单即锁定库存，支付超时自动释放，购物流程清晰可控。
        </p>

        <dl class="brand-facts">
          <div class="fact">
            <dt>4</dt>
            <dd>宠物分类</dd>
          </div>
          <div class="fact">
            <dt>30<small>分钟</small></dt>
            <dd>支付时限</dd>
          </div>
          <div class="fact">
            <dt>2<small>种</small></dt>
            <dd>角色权限</dd>
          </div>
        </dl>
      </div>

      <ul class="brand-points">
        <li>库存实时预占，避免超卖</li>
        <li>商品图片本地上传，即传即用</li>
        <li>用户与管理员权限隔离</li>
      </ul>

      <p class="brand-copy">© 2026 PetMarket</p>
    </aside>

    <!-- ==================== 右屏：表单 ==================== -->
    <main class="panel">
      <div class="panel-inner">
        <div class="panel-head">
          <span class="logo logo-sm" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor"
                 stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="7.2" cy="9" r="2.1" />
              <circle cx="12" cy="7" r="2.1" />
              <circle cx="16.8" cy="9" r="2.1" />
              <path d="M12 12.4c2.8 0 4.9 1.9 4.9 4.1 0 1.6-1.2 2.6-2.7 2.6-.9 0-1.5-.4-2.2-.4s-1.3.4-2.2.4c-1.5 0-2.7-1-2.7-2.6 0-2.2 2.1-4.1 4.9-4.1z" />
            </svg>
          </span>
          <span>PetMarket</span>
        </div>

        <h2 class="panel-title">{{ role.title }}</h2>
        <p class="panel-sub">{{ role.subtitle }}</p>

        <!-- 角色分段控件：原生 button，切换逻辑完全可控 -->
        <div class="segmented" role="tablist" aria-label="登录角色">
          <button
            v-for="item in roles"
            :key="item.name"
            type="button"
            role="tab"
            class="segment"
            :class="{ active: activeRole === item.name }"
            :aria-selected="activeRole === item.name"
            @click="switchRole(item.name)"
          >
            {{ item.tab }}
          </button>
          <span class="segment-thumb" :style="{ transform: `translateX(${activeIndex * 100}%)` }"></span>
        </div>

        <el-form
          id="login-form"
          ref="formRef"
          :model="form"
          :rules="rules"
          :disabled="locked"
          label-position="top"
          size="large"
          class="form"
          @keyup.enter="submit"
        >
          <el-form-item :label="role.nameField" prop="username">
            <el-input
              ref="usernameRef"
              v-model="form.username"
              :placeholder="role.namePlaceholder"
              autocomplete="username"
              :prefix-icon="User"
              clearable
            />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              ref="passwordRef"
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              autocomplete="current-password"
              :prefix-icon="Lock"
              show-password
            />
          </el-form-item>

          <div class="row">
            <el-checkbox v-model="remember" label="记住账号" />
            <span class="muted">忘记密码请联系{{ isAdmin ? '系统管理员' : '客服' }}</span>
          </div>

          <p v-if="errorText" class="alert" role="alert">{{ errorText }}</p>

          <el-button
            class="submit"
            type="primary"
            size="large"
            :loading="loading"
            :disabled="locked"
            @click="submit"
          >
            {{ locked ? `请稍候 ${lockLeft}s` : '登 录' }}
          </el-button>
        </el-form>

        <div class="hint">
          <p class="hint-label">测试账号（点击填入）</p>
          <div class="hint-body">
            <button type="button" class="hint-btn" :disabled="locked" @click="fill('user')">
              用户 testuser / 123456
            </button>
            <button type="button" class="hint-btn" :disabled="locked" @click="fill('admin')">
              管理员 admin / admin123
            </button>
          </div>
        </div>

        <footer class="panel-foot">
          <template v-if="isAdmin">
            <span>后台仅限授权人员访问</span>
            <el-button link type="primary" @click="router.push('/')">返回商城</el-button>
          </template>
          <template v-else>
            <span>还没有账号？</span>
            <el-button link type="primary" @click="router.push('/register')">立即注册</el-button>
          </template>
        </footer>
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { login } from '@/api/user'
import { adminLogin } from '@/api/admin'
import { useUserStore } from '@/stores/user'
import { useAdminStore } from '@/stores/admin'

/* ---------------- 角色定义 ---------------- */
const roles = [
  {
    name: 'user',
    tab: '用户登录',
    title: '欢迎回来',
    subtitle: '登录后可管理购物车、订单与收货地址',
    nameField: '用户名',
    namePlaceholder: '请输入用户名',
    redirect: '/'
  },
  {
    name: 'admin',
    tab: '管理员登录',
    title: '后台管理登录',
    subtitle: '管理商品、分类、订单与用户',
    nameField: '管理员账号',
    namePlaceholder: '请输入管理员账号',
    redirect: '/admin'
  }
]

/** 测试账号（与 sql/pet_market.sql 初始化数据一致，仅用于本地演示） */
const DEMO = {
  user: { username: 'testuser', password: '123456' },
  admin: { username: 'admin', password: 'admin123' }
}

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const adminStore = useAdminStore()

const formRef = ref()
const passwordRef = ref()

const activeRole = ref('user')
const loading = ref(false)
const remember = ref(false)
const errorText = ref('')
const form = reactive({ username: '', password: '' })

const role = computed(() => roles.find((r) => r.name === activeRole.value) || roles[0])
const activeIndex = computed(() => roles.findIndex((r) => r.name === activeRole.value))
const isAdmin = computed(() => activeRole.value === 'admin')

/* ---------------- 登录失败频控 ---------------- */
const MAX_ATTEMPTS = 4
const LOCK_SECONDS = 60
let failedAttempts = 0
let lockedUntil = 0
let lockTimer = null
const lockLeft = ref(0)
const locked = computed(() => lockLeft.value > 0)

const startLockTimer = () => {
  lockLeft.value = Math.ceil((lockedUntil - Date.now()) / 1000)
  if (lockTimer) return
  lockTimer = window.setInterval(() => {
    const left = Math.ceil((lockedUntil - Date.now()) / 1000)
    lockLeft.value = left > 0 ? left : 0
    if (left <= 0) {
      window.clearInterval(lockTimer)
      lockTimer = null
      failedAttempts = 0
      errorText.value = ''
    }
  }, 500)
}

const registerFailure = (message) => {
  errorText.value = message
  failedAttempts += 1
  if (failedAttempts >= MAX_ATTEMPTS) {
    lockedUntil = Date.now() + LOCK_SECONDS * 1000
    startLockTimer()
    errorText.value = `连续 ${MAX_ATTEMPTS} 次登录失败，请 ${LOCK_SECONDS} 秒后重试`
  }
}

/**
 * 从异常中提取对用户有意义的提示。
 *
 * 注意两种失败形态：
 *  1. 网络层失败：axios 抛出 RequestError（带 code，如 ERR_NETWORK），没有 response；
 *  2. 业务失败：request.js 的响应拦截器在 code !== 200 时 reject 一个普通 Error，
 *     真正的后端提示在 error.message 里（此时没有 response）。
 *     —— 早期实现只看 error.response，会把"密码错误"误报成"连不上服务器"，已修正。
 */
const friendlyMessage = (error) => {
  const isNetworkFailure = !error?.response && (
    error?.code === 'ERR_NETWORK' ||
    error?.code === 'ECONNABORTED' ||
    /network error|timeout|网络连接失败|无法连接/i.test(error?.message || '')
  )
  if (isNetworkFailure) {
    return '无法连接服务器，请确认后端服务（默认 8081 端口）已启动'
  }
  if (error?.response?.data?.message) return error.response.data.message
  if (error?.message) return error.message
  return '登录失败，请稍后重试'
}

/* ---------------- 校验规则 ---------------- */
const rules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 2, message: '账号长度至少 2 位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少 6 位', trigger: 'blur' }
  ]
}

/* ---------------- 角色切换 ---------------- */
const applyRole = (name, { syncUrl = true } = {}) => {
  if (activeRole.value !== name) activeRole.value = name
  form.password = ''
  errorText.value = ''
  formRef.value?.clearValidate()
  if (syncUrl) {
    const target = name === 'admin' ? '/admin/login' : '/login'
    if (route.path !== target) {
      router.replace(route.query.redirect ? { path: target, query: { redirect: route.query.redirect } } : target)
    }
  }
}

/** 分段控件点击：先切 UI，再同步 URL */
const switchRole = (name) => applyRole(name)

/** 依据当前 URL 决定初始角色（/admin/login 或 ?role=admin → 管理员） */
const roleFromRoute = () =>
  route.path.startsWith('/admin') || route.query.role === 'admin' ? 'admin' : 'user'

/* ---------------- 测试账号填入 ---------------- */
const fill = async (key) => {
  const account = DEMO[key]
  if (!account) return
  errorText.value = ''
  formRef.value?.clearValidate()
  // 先切角色，保证填入的账号与当前角色一致
  if ((key === 'admin') !== isAdmin.value) applyRole(key)
  form.username = account.username
  form.password = account.password
  await nextTick()
  passwordRef.value?.focus?.()
}

/* ---------------- 登录 ---------------- */
const resolveRedirect = (fallback) => {
  const target = route.query.redirect
  if (typeof target === 'string' && target.startsWith('/') && !target.startsWith('//')) return target
  return fallback
}

const submit = async () => {
  if (locked.value || loading.value) return
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  errorText.value = ''
  try {
    const payload = { username: form.username.trim(), password: form.password }
    const res = isAdmin.value ? await adminLogin(payload) : await login(payload)

    if (isAdmin.value) {
      userStore.logout()
      adminStore.setToken(res.data.token)
      adminStore.setAdminInfo(res.data.admin)
    } else {
      adminStore.logout()
      userStore.setToken(res.data.token)
      userStore.setUserInfo(res.data.user)
    }

    // 只记住账号名，绝不保存密码
    if (remember.value) window.localStorage.setItem('petmarket:lastLoginName', payload.username)
    else window.localStorage.removeItem('petmarket:lastLoginName')

    failedAttempts = 0
    ElMessage.success(isAdmin.value ? '登录成功，正在进入后台' : '登录成功，欢迎回来')
    await router.replace(resolveRedirect(role.value.redirect))
  } catch (e) {
    registerFailure(friendlyMessage(e))
  } finally {
    loading.value = false
  }
}

/* ---------------- 同步与生命周期 ---------------- */
const applyRememberedName = () => {
  if (form.username) return
  const saved = window.localStorage.getItem('petmarket:lastLoginName')
  if (saved) {
    form.username = saved
    remember.value = true
  }
}

watch(() => form.username, () => {
  if (errorText.value) errorText.value = ''
})

// 路由变化（/login ↔ /admin/login）时同步角色，保证 URL 与 UI 一致
watch(() => [route.path, route.query.role], () => {
  applyRole(roleFromRoute(), { syncUrl: false })
})

onMounted(() => {
  activeRole.value = roleFromRoute()
  applyRememberedName()
  document.title = isAdmin.value ? '后台管理登录 · PetMarket' : '登录 · PetMarket'
})

watch(isAdmin, (v) => {
  document.title = v ? '后台管理登录 · PetMarket' : '登录 · PetMarket'
})

onBeforeUnmount(() => {
  if (lockTimer) window.clearInterval(lockTimer)
})
</script>

<style scoped>
/* ============================================================
   设计令牌
   ============================================================ */
.auth {
  --brand-bg: #0d1526;
  --brand-bg-2: #111d33;
  --brand-line: rgba(255, 255, 255, 0.1);
  --brand-text: #f2f5fa;
  --brand-text-2: rgba(233, 240, 252, 0.66);
  --brand-text-3: rgba(233, 240, 252, 0.5);

  --accent: #2f5fe0;
  --accent-hover: #2853c9;
  --accent-soft: #eef2fd;

  --ink-1: #101828;
  --ink-2: #475467;
  --ink-3: #667085;
  --line: #e4e7ec;

  display: flex;
  min-height: 100vh;
  font-family: 'Inter', 'HarmonyOS Sans SC', 'PingFang SC', 'Microsoft YaHei', system-ui, sans-serif;
  color: var(--ink-1);
  background: #fff;
}

/* ============================================================
   左屏：品牌
   ============================================================ */
.brand {
  position: relative;
  flex: 0 0 50%;
  max-width: 50%;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 40px;
  padding: 56px 64px 40px;
  color: var(--brand-text);
  background: var(--brand-bg);
  overflow: hidden;
}

/* 点阵纹理：比模糊光斑更克制，也不影响文字对比度 */
.brand::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image: radial-gradient(rgba(255, 255, 255, 0.075) 1px, transparent 1px);
  background-size: 22px 22px;
  background-position: 0 0;
  pointer-events: none;
}
/* 右下角一层极淡的同色系光，制造纵深 */
.brand::after {
  content: '';
  position: absolute;
  right: -160px;
  bottom: -180px;
  width: 520px;
  height: 520px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(47, 95, 224, 0.28) 0%, transparent 70%);
  pointer-events: none;
}

.brand-inner,
.brand-points,
.brand-copy {
  position: relative;
  z-index: 1;
}

.brand-head { display: flex; align-items: center; gap: 10px; }

.logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  flex: none;
  border-radius: 9px;
  color: #fff;
  background: var(--accent);
}
.logo-sm { width: 26px; height: 26px; border-radius: 7px; }

.logo-text {
  font-size: 17px;
  font-weight: 650;
  letter-spacing: -0.01em;
}

.brand-title {
  margin: 56px 0 0;
  font-size: 40px;
  font-weight: 700;
  line-height: 1.22;
  letter-spacing: -0.025em;
}

.brand-lead {
  margin: 20px 0 0;
  max-width: 30em;
  font-size: 15px;
  line-height: 1.85;
  color: var(--brand-text-2);
}

.brand-facts {
  display: flex;
  gap: 40px;
  margin: 44px 0 0;
  padding: 0 0 24px;
  border-bottom: 1px solid var(--brand-line);
}
.fact dt {
  font-size: 26px;
  font-weight: 650;
  letter-spacing: -0.02em;
  line-height: 1.2;
}
.fact dt small {
  margin-left: 2px;
  font-size: 13px;
  font-weight: 500;
  color: var(--brand-text-3);
}
.fact dd {
  margin: 6px 0 0;
  font-size: 13px;
  color: var(--brand-text-3);
}

.brand-points {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 12px;
  font-size: 13.5px;
  color: var(--brand-text-2);
}
.brand-points li {
  display: flex;
  align-items: baseline;
  gap: 10px;
}
.brand-points li::before {
  content: '';
  flex: none;
  width: 4px;
  height: 4px;
  margin-top: 6px;
  border-radius: 50%;
  background: var(--accent);
  box-shadow: 0 0 0 3px rgba(47, 95, 224, 0.22);
}

.brand-copy {
  margin: 0;
  font-size: 12px;
  color: var(--brand-text-3);
}

/* ============================================================
   右屏：表单
   ============================================================ */
.panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 56px;
  background: #fff;
}

.panel-inner { width: 100%; max-width: 400px; }

.panel-head {
  display: none;
  align-items: center;
  gap: 8px;
  margin-bottom: 26px;
  font-size: 16px;
  font-weight: 650;
}

.panel-title {
  margin: 0;
  font-size: 24px;
  font-weight: 650;
  letter-spacing: -0.02em;
}
.panel-sub {
  margin: 8px 0 0;
  font-size: 14px;
  color: var(--ink-2);
}

/* ---- 角色分段控件 ---- */
.segmented {
  position: relative;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0;
  margin: 26px 0 24px;
  padding: 3px;
  background: #f2f4f7;
  border-radius: 10px;
}
.segment {
  position: relative;
  z-index: 1;
  padding: 0;
  height: 38px;
  font-family: inherit;
  font-size: 13.5px;
  font-weight: 500;
  color: var(--ink-2);
  background: none;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: color 0.18s ease;
}
.segment:hover { color: var(--ink-1); }
.segment.active { color: var(--ink-1); font-weight: 600; }
.segment-thumb {
  position: absolute;
  top: 3px;
  left: 3px;
  width: calc(50% - 3px);
  height: 38px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(16, 24, 40, 0.08), 0 1px 3px rgba(16, 24, 40, 0.06);
  transition: transform 0.22s cubic-bezier(0.4, 0, 0.2, 1);
}

/* ---- 表单 ---- */
.form :deep(.el-form-item) { margin-bottom: 16px; }
.form :deep(.el-form-item__label) {
  padding-bottom: 6px;
  font-size: 13px;
  font-weight: 500;
  color: var(--ink-2);
  line-height: 1.4;
}
.form :deep(.el-input__wrapper) {
  padding: 2px 12px;
  border-radius: 10px;
  box-shadow: 0 0 0 1px var(--line) inset;
  transition: box-shadow 0.16s ease;
}
.form :deep(.el-input__wrapper:hover) { box-shadow: 0 0 0 1px #cfd4dc inset; }
.form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--accent) inset, 0 0 0 3px rgba(47, 95, 224, 0.12);
}
.form :deep(.el-input__inner) { height: 44px; font-size: 14px; }
.form :deep(.el-input__inner::placeholder) { color: var(--ink-3); opacity: 1; }
.form :deep(.el-input__prefix) { color: #98a2b3; font-size: 15px; }
.form :deep(.el-form-item.is-error .el-input__wrapper) {
  box-shadow: 0 0 0 1px #d92d20 inset;
}

.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 2px 0 18px;
}
.row :deep(.el-checkbox__label) { font-size: 13px; color: var(--ink-2); }
.muted { font-size: 12.5px; color: var(--ink-3); }

.alert {
  display: flex;
  align-items: center;
  margin: 0 0 16px;
  padding: 10px 12px;
  font-size: 13px;
  line-height: 1.5;
  color: #912018;
  background: #fef3f2;
  border: 1px solid #fda29b;
  border-radius: 10px;
}

.submit {
  width: 100%;
  height: 48px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.04em;
  border: none;
  border-radius: 10px;
  background: var(--accent);
  box-shadow: 0 1px 2px rgba(16, 24, 40, 0.08);
  transition: background 0.16s ease;
}
.submit:hover:not(.is-disabled) { background: var(--accent-hover); }
.submit:active:not(.is-disabled) { background: #234ab4; }

/* ---- 测试账号 ---- */
.hint {
  margin-top: 24px;
  padding-top: 18px;
  border-top: 1px solid var(--line);
}
.hint-label {
  margin: 0 0 10px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.04em;
  color: var(--ink-3);
}
.hint-body { display: flex; flex-wrap: wrap; gap: 8px; }
.hint-btn {
  padding: 7px 12px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 12.5px;
  color: var(--ink-2);
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.16s ease;
}
.hint-btn:hover:not(:disabled) {
  color: var(--accent);
  border-color: var(--accent);
  background: var(--accent-soft);
}
.hint-btn:disabled { cursor: not-allowed; opacity: 0.5; }

.panel-foot {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: 22px;
  font-size: 13px;
  color: var(--ink-2);
}
.panel-foot :deep(.el-button) { font-size: 13px; }

/* ============================================================
   响应式
   ============================================================ */
@media (max-width: 1200px) {
  .brand { padding: 44px 40px 32px; }
  .brand-title { font-size: 34px; margin-top: 44px; }
  .brand-facts { gap: 28px; }
  .panel { padding: 40px 32px; }
}

@media (max-width: 980px) {
  .auth { flex-direction: column; }
  .brand { display: none; }
  .panel {
    flex: 1;
    align-items: flex-start;
    padding: 40px 24px 56px;
  }
  .panel-inner { margin: 0 auto; }
  .panel-head { display: flex; }
}

@media (max-width: 480px) {
  .panel { padding: 28px 18px 44px; }
  .panel-title { font-size: 22px; }
  .row { flex-direction: column; align-items: flex-start; gap: 4px; }
}

@media (prefers-reduced-motion: reduce) {
  .segment-thumb { transition: none; }
}
</style>
