import request from '@/utils/request'

// 管理员登录
export const adminLogin = (data) => request.post('/admin/auth/login', data)

// 数据统计
export const getDashboardStats = () => request.get('/admin/dashboard/stats')

// 图片上传（本地电脑保存，返回 { url, name, size }）
export const uploadImage = (file, bizType = 'product') => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('bizType', bizType)
  return request.post('/admin/upload/image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 商品管理
export const getAdminProductList = (params) => request.get('/admin/product/list', { params })
export const createProduct = (data) => request.post('/admin/product/create', data)
export const updateProduct = (id, data) => request.put(`/admin/product/update/${id}`, data)
export const deleteProduct = (id) => request.delete(`/admin/product/delete/${id}`)
export const toggleProductStatus = (id, status) =>
  request.post(`/admin/product/toggleStatus/${id}`, null, { params: { status } })

// 分类管理
export const getAdminCategoryList = () => request.get('/admin/category/list')
export const createCategory = (data) => request.post('/admin/category/create', data)
export const updateCategory = (id, data) => request.put(`/admin/category/update/${id}`, data)
export const deleteCategory = (id) => request.delete(`/admin/category/delete/${id}`)

// 用户管理
export const getAdminUserList = (params) => request.get('/admin/user/list', { params })
export const toggleUserStatus = (id, status) =>
  request.post(`/admin/user/toggleStatus/${id}`, { status })

// 订单管理
export const getAdminOrderList = (params) => request.get('/admin/order/list', { params })
export const shipOrder = (id) => request.post(`/admin/order/ship/${id}`)
