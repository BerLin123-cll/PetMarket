// 文件路径: E:\SpringDemo\PetMarket\frontend\src\api\product.js
import request from '@/utils/request'

export const getProductList = (params) => request.get('/api/product/list', { params })

export const getProductDetail = (id) => request.get(`/api/product/${id}`)

export const getHotProducts = (params) => request.get('/api/product/hot', { params })
