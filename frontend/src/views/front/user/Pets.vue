<template>
  <div class="pets-page">
    <div class="page-header">
      <div>
        <h3>我的宠物</h3>
        <p class="sub">登记宠物信息，获得专属商品推荐与驱虫提醒</p>
      </div>
      <el-button type="primary" @click="openAdd">
        <el-icon><Plus /></el-icon>添加宠物
      </el-button>
    </div>

    <el-empty v-if="!loading && pets.length === 0" description="还没有宠物，点击右上角添加吧" />

    <el-row :gutter="16">
      <el-col :span="8" v-for="pet in pets" :key="pet.id">
        <el-card shadow="hover" class="pet-card">
          <div class="pet-head">
            <el-avatar :size="56" :src="pet.avatar">{{ pet.name?.charAt(0) }}</el-avatar>
            <div class="pet-basic">
              <div class="pet-name">
                {{ pet.name }}
                <el-tag size="small" type="info">{{ pet.speciesLabel }}</el-tag>
              </div>
              <div class="pet-sub">{{ pet.breed || '未填写品种' }} · {{ pet.ageText }}</div>
            </div>
          </div>

          <div class="pet-meta">
            <span>体重：{{ pet.weight != null ? pet.weight + ' kg' : '—' }}</span>
            <span>性别：{{ genderText(pet.gender) }}</span>
          </div>
          <div class="pet-meta">
            <span>过敏史：{{ pet.allergyHistory || '无' }}</span>
          </div>

          <div class="deworm" :class="{ due: pet.dewormDue }">
            <el-icon><Bell /></el-icon>
            <span>{{ pet.dewormTip }}</span>
          </div>

          <div class="pet-actions">
            <el-button size="small" type="primary" plain @click="openRecommend(pet)">为它推荐</el-button>
            <el-button size="small" @click="openEdit(pet)">编辑</el-button>
            <el-button size="small" type="danger" plain @click="handleDelete(pet)">删除</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑宠物' : '添加宠物'" width="560px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="昵称" required>
          <el-input v-model="form.name" placeholder="宠物昵称" maxlength="50" />
        </el-form-item>
        <el-form-item label="物种" required>
          <el-select v-model="form.species" placeholder="请选择物种" style="width:100%">
            <el-option v-for="s in SPECIES_OPTIONS" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="品种">
          <el-input v-model="form.breed" placeholder="如：金毛、英短" maxlength="50" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="form.gender">
            <el-radio :value="0">未知</el-radio>
            <el-radio :value="1">公</el-radio>
            <el-radio :value="2">母</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="生日">
          <el-date-picker v-model="form.birthday" type="date" value-format="YYYY-MM-DD"
                          placeholder="选择生日" style="width:100%" />
        </el-form-item>
        <el-form-item label="体重(kg)">
          <el-input-number v-model="form.weight" :min="0" :max="999" :precision="2" :step="0.1" />
        </el-form-item>
        <el-form-item label="过敏史">
          <el-input v-model="form.allergyHistory" type="textarea" :rows="2"
                    placeholder="多个关键词用逗号分隔，如：鸡肉,牛肉" maxlength="500" />
        </el-form-item>
        <el-form-item label="上次驱虫">
          <el-date-picker v-model="form.lastDewormDate" type="date" value-format="YYYY-MM-DD"
                          placeholder="选择上次驱虫日期" style="width:100%" />
        </el-form-item>
        <el-form-item label="宠物头像">
          <el-upload
            :show-file-list="false"
            :http-request="handleAvatarUpload"
            :before-upload="beforeAvatarUpload"
            accept="image/jpeg,image/png,image/gif,image/webp"
          >
            <el-avatar :size="72" :src="form.avatar">{{ form.name?.charAt(0) || '+' }}</el-avatar>
          </el-upload>
          <span class="upload-hint">点击更换头像（可选）</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 推荐弹窗 -->
    <el-dialog v-model="recommendVisible" :title="`为「${currentPet?.name || ''}」推荐`" width="740px">
      <el-empty v-if="recommendList.length === 0" description="暂时没有合适的推荐商品" />
      <el-row :gutter="16">
        <el-col :span="8" v-for="item in recommendList" :key="item.id">
          <el-card shadow="hover" class="rec-card" @click="goProduct(item.id)">
            <img :src="item.mainImage || defaultImg" class="rec-img" />
            <div class="rec-name">{{ item.name }}</div>
            <div class="rec-price">¥{{ item.price }}</div>
          </el-card>
        </el-col>
      </el-row>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPetList, addPet, updatePet, deletePet, getPetRecommend } from '@/api/pet'
import { uploadAvatar } from '@/api/user'

const router = useRouter()

const SPECIES_OPTIONS = [
  { value: 1, label: '狗' },
  { value: 2, label: '猫' },
  { value: 3, label: '小宠' },
  { value: 4, label: '爬宠' },
  { value: 5, label: '鸟' },
  { value: 6, label: '鱼' }
]

const defaultImg = '/placeholder-square.svg'

const pets = ref([])
const loading = ref(false)

const dialogVisible = ref(false)
const saving = ref(false)
const form = reactive(emptyForm())

const recommendVisible = ref(false)
const recommendList = ref([])
const currentPet = ref(null)

function emptyForm() {
  return {
    id: null,
    name: '',
    species: 1,
    breed: '',
    gender: 0,
    birthday: null,
    weight: null,
    allergyHistory: '',
    lastDewormDate: null,
    avatar: ''
  }
}

const genderText = (g) => (g === 1 ? '公' : g === 2 ? '母' : '未知')

const fetchPets = async () => {
  loading.value = true
  try {
    const res = await getPetList()
    pets.value = res.data || []
  } catch (e) {} finally {
    loading.value = false
  }
}

const openAdd = () => {
  Object.assign(form, emptyForm())
  dialogVisible.value = true
}

const openEdit = (pet) => {
  Object.assign(form, emptyForm(), {
    id: pet.id,
    name: pet.name,
    species: pet.species,
    breed: pet.breed,
    gender: pet.gender ?? 0,
    birthday: pet.birthday,
    weight: pet.weight != null ? Number(pet.weight) : null,
    allergyHistory: pet.allergyHistory,
    lastDewormDate: pet.lastDewormDate,
    avatar: pet.avatar
  })
  dialogVisible.value = true
}

const handleSave = async () => {
  if (!form.name || !form.name.trim()) {
    ElMessage.warning('请填写宠物昵称')
    return
  }
  if (!form.species) {
    ElMessage.warning('请选择宠物物种')
    return
  }
  saving.value = true
  try {
    if (form.id) {
      await updatePet(form.id, { ...form })
      ElMessage.success('保存成功')
    } else {
      const payload = { ...form }
      delete payload.id
      await addPet(payload)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    await fetchPets()
  } catch (e) {} finally {
    saving.value = false
  }
}

const handleDelete = (pet) => {
  ElMessageBox.confirm(`确定删除宠物「${pet.name}」吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await deletePet(pet.id)
      ElMessage.success('已删除')
      await fetchPets()
    })
    .catch(() => {})
}

const openRecommend = async (pet) => {
  currentPet.value = pet
  recommendList.value = []
  recommendVisible.value = true
  try {
    const res = await getPetRecommend(pet.id)
    recommendList.value = res.data || []
  } catch (e) {}
}

const goProduct = (id) => router.push(`/product/${id}`)

const beforeAvatarUpload = (file) => {
  const isImage = /^image\/(jpeg|png|gif|webp)$/.test(file.type)
  if (!isImage) {
    ElMessage.error('只能上传 JPG / PNG / GIF / WEBP 图片')
    return false
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过 5MB')
    return false
  }
  return true
}

const handleAvatarUpload = async (options) => {
  try {
    const res = await uploadAvatar(options.file)
    form.avatar = res.data.url
    ElMessage.success('头像上传成功')
  } catch (e) {
    ElMessage.error('头像上传失败')
  }
}

onMounted(fetchPets)
</script>

<style scoped>
.pets-page { margin-top: 10px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h3 { margin: 0; }
.page-header .sub { margin: 4px 0 0; font-size: 13px; color: #909399; }
.pet-card { margin-bottom: 16px; }
.pet-head { display: flex; align-items: center; gap: 12px; }
.pet-basic { flex: 1; min-width: 0; }
.pet-name { display: flex; align-items: center; gap: 6px; font-size: 16px; font-weight: 600; }
.pet-sub { margin-top: 4px; font-size: 13px; color: #909399; }
.pet-meta { margin-top: 10px; font-size: 13px; color: #606266; display: flex; gap: 16px; }
.deworm {
  margin-top: 12px; padding: 6px 10px; border-radius: 6px; font-size: 13px;
  background: #f4f4f5; color: #909399; display: flex; align-items: center; gap: 6px;
}
.deworm.due { background: #fef0f0; color: #f56c6c; }
.pet-actions { margin-top: 14px; display: flex; justify-content: flex-end; }
.upload-hint { margin-left: 10px; font-size: 12px; color: #909399; }
.rec-card { cursor: pointer; margin-bottom: 16px; }
.rec-img { width: 100%; height: 130px; object-fit: cover; border-radius: 4px; }
.rec-name { margin-top: 8px; font-size: 13px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.rec-price { margin-top: 6px; color: #f56c6c; font-weight: bold; }
</style>
