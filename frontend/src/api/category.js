import request from '@/utils/request'

export const getCategoryList = () => request.get('/api/category/list')
