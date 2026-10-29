<!-- 文件路径: E:\SpringDemo\PetMarket\frontend\src\components\ImageUpload.vue -->
<!--
  通用图片上传组件（V1.1 新增）
  特点：
   1. 图片保存到「后端所在电脑的本地磁盘」，无需服务器/OSS；
   2. 上传成功后通过 v-model 回传可直接使用的访问地址（如 /uploads/product/xxx.jpg）；
   3. 自动携带管理员 Token（走 el-upload 的 headers 设置，与 axios 拦截器解耦）；
   4. 前端做一次大小与格式预校验，后端还有二次校验（扩展名 + MIME + 文件头魔数）。
  用法：
    <ImageUpload v-model="form.mainImage" biz-type="product" />
-->
<template>
  <div class="image-upload">
    <el-upload
      class="uploader"
      :action="action"
      :headers="headers"
      :data="{ bizType }"
      name="file"
      :show-file-list="false"
      :before-upload="beforeUpload"
      :on-success="handleSuccess"
      :on-error="handleError"
      accept="image/jpeg,image/png,image/gif,image/webp,image/bmp"
    >
      <div class="uploader-box" :style="{ width: boxSize, height: boxSize }">
        <img v-if="modelValue" :src="modelValue" class="preview" alt="预览图" />
        <div v-else class="placeholder">
          <el-icon class="plus-icon"><Plus /></el-icon>
          <span class="tip">点击上传</span>
        </div>
        <div v-if="uploading" class="mask">
          <el-icon class="is-loading"><Loading /></el-icon>
        </div>
      </div>
    </el-upload>

    <div class="uploader-side">
      <el-input
        :model-value="modelValue"
        placeholder="也可直接粘贴图片地址"
        clearable
        @update:model-value="(val) => emit('update:modelValue', val)"
      />
      <div class="uploader-actions">
        <el-button v-if="modelValue" link type="danger" @click="emit('update:modelValue', '')">
          清除图片
        </el-button>
        <span class="hint">支持 JPG / PNG / GIF / WEBP / BMP，不超过 {{ maxSizeMb }}MB，保存在本机</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Loading, Plus } from '@element-plus/icons-vue'
import { useAdminStore } from '@/stores/admin'

const props = defineProps({
  /** 图片访问地址（v-model） */
  modelValue: { type: String, default: '' },
  /** 业务分类，决定保存子目录：product / avatar / category / other */
  bizType: { type: String, default: 'product' },
  /** 单文件大小上限（MB） */
  maxSizeMb: { type: Number, default: 5 },
  /** 预览框边长 */
  boxSize: { type: String, default: '120px' }
})

const emit = defineEmits(['update:modelValue', 'uploaded'])

const adminStore = useAdminStore()
const uploading = ref(false)

/** 上传接口：经 vite 代理转发到后端 8081 */
const action = '/admin/upload/image'

/** el-upload 使用原生 XHR，不会经过 axios 拦截器，这里手动带上管理员 Token */
const headers = computed(() => ({
  Authorization: `Bearer ${adminStore.token}`
}))

const beforeUpload = (file) => {
  if (!adminStore.token) {
    ElMessage.error('登录状态已失效，请重新登录后再上传')
    return false
  }
  const isImage = /^image\/(jpeg|png|gif|webp|bmp)$/.test(file.type)
  if (!isImage) {
    ElMessage.error('只能上传图片文件（JPG / PNG / GIF / WEBP / BMP）')
    return false
  }
  if (file.size > props.maxSizeMb * 1024 * 1024) {
    ElMessage.error(`图片大小不能超过 ${props.maxSizeMb}MB`)
    return false
  }
  uploading.value = true
  return true
}

const handleSuccess = (res) => {
  uploading.value = false
  if (res && res.code === 200 && res.data?.url) {
    emit('update:modelValue', res.data.url)
    emit('uploaded', res.data)
    ElMessage.success('图片上传成功')
  } else {
    ElMessage.error(res?.message || '图片上传失败')
  }
}

const handleError = (error) => {
  uploading.value = false
  let message = '图片上传失败，请稍后重试'
  try {
    const body = JSON.parse(error?.message || '{}')
    if (body?.message) message = body.message
  } catch (e) {
    // 忽略解析失败，使用默认提示
  }
  ElMessage.error(message)
}
</script>

<style scoped>
.image-upload { display: flex; gap: 12px; align-items: flex-start; }
.uploader-box {
  position: relative;
  border: 1px dashed #c0c4cc;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  cursor: pointer;
  background: #fafafa;
  transition: border-color 0.2s;
}
.uploader-box:hover { border-color: var(--pm-primary); }
.preview { width: 100%; height: 100%; object-fit: cover; display: block; }
.placeholder { display: flex; flex-direction: column; align-items: center; gap: 4px; color: #909399; }
.plus-icon { font-size: 22px; }
.tip { font-size: 12px; }
.mask {
  position: absolute; inset: 0; background: rgba(255, 255, 255, 0.7);
  display: flex; align-items: center; justify-content: center; font-size: 20px; color: var(--pm-primary);
}
.uploader-side { flex: 1; min-width: 220px; }
.uploader-actions { display: flex; align-items: center; justify-content: space-between; margin-top: 4px; }
.hint { font-size: 12px; color: #909399; line-height: 1.4; }
</style>
