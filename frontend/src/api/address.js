import request from '@/utils/request'

export const getAddressList = () => request.get('/api/address/list')

export const addAddress = (data) => request.post('/api/address/add', data)

export const updateAddress = (id, data) => request.put(`/api/address/update/${id}`, data)

export const deleteAddress = (id) => request.delete(`/api/address/delete/${id}`)

export const setDefaultAddress = (id) => request.put(`/api/address/setDefault/${id}`)
