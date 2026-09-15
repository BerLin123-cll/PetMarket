<!-- 文件路径: E:\SpringDemo\PetMarket\frontend\src\views\front\user\Profile.vue -->
<!-- 将 E:\SpringDemo\PetMarket\frontend\src\views\front\Profile.vue 移动到此位置 -->
<template>
  <el-card>
    <h3 style="margin-bottom:20px">个人信息</h3>
    <el-form ref="formRef" :model="form" label-width="80px" style="max-width:500px">
      <el-form-item label="用户名">
        <el-input :value="form.username" disabled />
      </el-form-item>
      <el-form-item label="昵称">
        <el-input v-model="form.nickname" />
      </el-form-item>
      <el-form-item label="手机号">
        <el-input v-model="form.phone" />
      </el-form-item>
      <el-form-item label="性别">
        <el-radio-group v-model="form.gender">
          <el-radio :value="1">男</el-radio>
          <el-radio :value="0">女</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="saving" @click="handleSave">保存修改</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { useUserStore } from '@/stores/user'
import { updateUserInfo } from '@/api/user'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const saving = ref(false)
const form = reactive({ username: '', nickname: '', phone: '', gender: 1 })

const handleSave = async () => {
  saving.value = true
  try {
    await updateUserInfo({ nickname: form.nickname, phone: form.phone, gender: form.gender })
    await userStore.fetchUserInfo()
    ElMessage.success('保存成功')
  } catch (e) {} finally {
    saving.value = false
  }
}

onMounted(() => {
  if (userStore.userInfo) {
    Object.assign(form, userStore.userInfo)
  }
})
</script>
