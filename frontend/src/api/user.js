// 文件路径: E:\SpringDemo\PetMarket\frontend\src\api\user.js
import request from '@/utils/request'

export const login = (data) => request.post('/api/user/login', data)

export const register = (data) => request.post('/api/user/register', data)

export const getUserInfo = () => request.get('/api/user/info')

export const updateUserInfo = (data) => request.put('/api/user/update', data)

// 上传头像（保存到后端所在电脑的本地磁盘，返回 { url, name, size }）
export const uploadAvatar = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/api/upload/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
