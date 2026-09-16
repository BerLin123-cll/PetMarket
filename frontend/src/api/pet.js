import request from '@/utils/request'

// 我的宠物列表（含年龄、驱虫提醒）
export const getPetList = () => request.get('/api/pet/list')

// 新增宠物
export const addPet = (data) => request.post('/api/pet/add', data)

// 更新宠物
export const updatePet = (id, data) => request.put(`/api/pet/update/${id}`, data)

// 删除宠物
export const deletePet = (id) => request.delete(`/api/pet/delete/${id}`)

// 为指定宠物推荐商品
export const getPetRecommend = (id) => request.get(`/api/pet/${id}/recommend`)
