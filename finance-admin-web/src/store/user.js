/**
 * 管理员用户状态
 * @author 胡宪棋 软件2413 202421332084
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('admin_token') || '')
  const adminId = ref(Number(localStorage.getItem('admin_id')) || 0)
  const username = ref(localStorage.getItem('admin_username') || '')
  const role = ref(localStorage.getItem('admin_role') || '')

  const isLoggedIn = computed(() => !!token.value)
  const isSuperAdmin = computed(() => role.value === 'SUPER_ADMIN')

  async function login(usernameVal, password) {
    const res = await loginApi({ username: usernameVal, password })
    if (res.code === 200 && res.data) {
      token.value = res.data.token
      adminId.value = res.data.adminId
      username.value = res.data.username
      role.value = res.data.role
      localStorage.setItem('admin_token', res.data.token)
      localStorage.setItem('admin_id', String(res.data.adminId))
      localStorage.setItem('admin_username', res.data.username)
      localStorage.setItem('admin_role', res.data.role)
      return true
    }
    return false
  }

  function logout() {
    token.value = ''
    adminId.value = 0
    username.value = ''
    role.value = ''
    localStorage.clear()
  }

  return { token, adminId, username, role, isLoggedIn, isSuperAdmin, login, logout }
})
