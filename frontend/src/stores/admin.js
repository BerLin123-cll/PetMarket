import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAdminStore = defineStore('admin', () => {
  const token = ref('')
  const adminInfo = ref(null)

  const setToken = (t) => {
    token.value = t
  }

  const setAdminInfo = (info) => {
    adminInfo.value = info
  }

  const logout = () => {
    token.value = ''
    adminInfo.value = null
  }

  return { token, adminInfo, setToken, setAdminInfo, logout }
}, {
  persist: {
    paths: ['token', 'adminInfo']
  }
})
