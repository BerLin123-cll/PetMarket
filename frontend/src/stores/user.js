import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getUserInfo } from '@/api/user'

export const useUserStore = defineStore('user', () => {
  const token = ref('')
  const userInfo = ref(null)

  const setToken = (t) => {
    token.value = t
  }

  const setUserInfo = (info) => {
    userInfo.value = info
  }

  const fetchUserInfo = async () => {
    const res = await getUserInfo()
    userInfo.value = res.data
  }

  const logout = () => {
    token.value = ''
    userInfo.value = null
  }

  const isLoggedIn = () => !!token.value

  return { token, userInfo, setToken, setUserInfo, fetchUserInfo, logout, isLoggedIn }
}, {
  persist: {
    paths: ['token', 'userInfo']
  }
})
