<template>
  <div class="register-page">
    <el-card class="register-card">
      <h2 class="title">用户注册</h2>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="0">
        <el-form-item prop="username">
          <el-input v-model="form.username" prefix-icon="User" placeholder="用户名" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" prefix-icon="Lock" placeholder="密码" type="password" size="large" show-password />
        </el-form-item>
        <el-form-item prop="nickname">
          <el-input v-model="form.nickname" prefix-icon="UserFilled" placeholder="昵称（选填）" size="large" />
        </el-form-item>
        <el-form-item prop="phone">
          <el-input v-model="form.phone" prefix-icon="Phone" placeholder="手机号（选填）" size="large" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="handleRegister">注 册</el-button>
        </el-form-item>
      </el-form>
      <div class="footer-link">
        已有账号？<el-button type="primary" link @click="$router.push('/login')">去登录</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '@/api/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '', password: '', nickname: '', phone: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, message: '密码至少6位', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }]
}

const handleRegister = async () => {
  await formRef.value.validate()
  loading.value = true
  try {
    await register(form)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (e) {} finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page { min-height: 100vh; display: flex; align-items: center; justify-content: center; padding: 24px; background: var(--pm-bg); }
.register-card { width: 420px; padding: 24px; border-radius: 16px; border: 1px solid var(--pm-border-warm); box-shadow: var(--pm-shadow-card); }
.title { text-align: center; margin-bottom: 28px; color: var(--pm-ink); font-size: 22px; font-weight: 700; }
.footer-link { text-align: center; color: var(--pm-text-light); }
</style>
