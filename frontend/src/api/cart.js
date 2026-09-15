import request from '@/utils/request'

export const getCartList = () => request.get('/api/cart/list')

export const addToCart = (data) => request.post('/api/cart/add', data)

export const updateCartQuantity = (id, quantity) =>
    request.put(`/api/cart/update/${id}`, { quantity })

export const removeFromCart = (id) => request.delete(`/api/cart/remove/${id}`)

export const clearCart = () => request.delete('/api/cart/clear')