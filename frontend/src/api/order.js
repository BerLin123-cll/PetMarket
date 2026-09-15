import request from '@/utils/request'

export const createOrder = (data) => request.post('/api/order/create', data)

export const payOrder = (orderNo) => request.post('/api/order/pay', { orderNo })

export const getOrderList = (params) => request.get('/api/order/list', { params })

export const getOrderDetail = (id) => request.get(`/api/order/${id}`)

export const cancelOrder = (id) => request.post(`/api/order/cancel/${id}`)