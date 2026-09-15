import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useAdminStore } from '@/stores/admin'
import router from '@/router'

const request = axios.create({
    baseURL: '',
    timeout: 10000
})


request.interceptors.request.use(
    (config) => {
        // 登录接口不携带 token
        if (config.url.includes('/login') || config.url.includes('/register')) {
            return config
        }

        if (config.url.startsWith('/admin')) {
            const adminStore = useAdminStore()
            if (adminStore.token) {
                config.headers['Authorization'] = `Bearer ${adminStore.token}`
            }
        } else {
            const userStore = useUserStore()
            if (userStore.token) {
                config.headers['Authorization'] = `Bearer ${userStore.token}`
            }
        }
        return config
    },
    (error) => Promise.reject(error)
)



request.interceptors.response.use(
    (response) => {
        const res = response.data
        if (res.code === 200) {
            return res
        }
        ElMessage.error(res.message || '请求失败')
        return Promise.reject(new Error(res.message))
    },
    (error) => {
        if (error.response) {
            const status = error.response.status
            if (status === 401) {
                const url = error.config?.url || ''
                if (url.startsWith('/admin')) {
                    const adminStore = useAdminStore()
                    adminStore.logout()
                    ElMessage.error('管理员登录已过期')
                    router.push('/admin/login')
                } else {
                    const userStore = useUserStore()
                    userStore.logout()
                    ElMessage.error('登录已过期，请重新登录')
                    router.push('/login')
                }
            } else if (status === 403) {
                // 403 = 已登录但没有权限（例如普通用户访问后台接口）
                const url = error.config?.url || ''
                if (url.startsWith('/admin')) {
                    const adminStore = useAdminStore()
                    adminStore.logout()
                    ElMessage.error('当前账号无后台管理权限，请使用管理员账号登录')
                    router.push('/admin/login')
                } else {
                    ElMessage.error(error.response.data?.message || '没有权限访问')
                }
            } else {
                ElMessage.error(error.response.data?.message || '服务器错误')
            }
        } else {
            ElMessage.error('网络连接失败')
        }
        return Promise.reject(error)
    }
)

export default request
