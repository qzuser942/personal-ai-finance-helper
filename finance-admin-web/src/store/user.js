/**
 * 管理员用户状态
 * @author 胡宪棋 软件2413 202421332084
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi } from '@/api/auth'
import { getAdminInfo } from '@/api/system'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('admin_token') || '')
  const adminId = ref(Number(localStorage.getItem('admin_id')) || 0)
  const username = ref(localStorage.getItem('admin_username') || '')
  const role = ref(localStorage.getItem('admin_role') || '')
  // 关键修复：权限位列表（每次 fetchInfo 从后端 /api/admin/info 拉取）
  const permissions = ref([])

  const isLoggedIn = computed(() => !!token.value)
  const isSuperAdmin = computed(() => role.value === 'SUPER_ADMIN')

  /**
   * 判断当前管理员是否拥有某个权限位
   * <p>关键修复：统一从后端拉取的 permissions 列表判断，避免 localStorage 篡改导致的误判
   * @param {string} perm 权限位字符串，如 'bill:write'、'category:delete'
   * @returns {boolean}
   */
  function hasPermission(perm) {
    if (!perm) return true
    // 超管拥有所有权限
    if (isSuperAdmin.value) return true
    return permissions.value.includes(perm)
  }

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
      // 登录成功后立即拉取一次权限位
      await fetchInfo()
      return true
    }
    return false
  }

  /**
   * 关键修复：拉取最新管理员信息（防 JWT 签发后被改）
   * 进入管理后台、路由切换、刷新页面时调用，强制覆盖本地缓存
   * <p>同时拉取 permissions 权限位列表（v-permission 指令依赖此字段）
   */
  async function fetchInfo() {
    if (!token.value) return
    try {
      const res = await getAdminInfo()
      if (res.code === 200 && res.data) {
        username.value = res.data.username
        role.value = res.data.role
        adminId.value = res.data.adminId
        if (Array.isArray(res.data.permissions)) {
          permissions.value = res.data.permissions
        }
        localStorage.setItem('admin_role', res.data.role)
        localStorage.setItem('admin_username', res.data.username)
        localStorage.setItem('admin_id', String(res.data.adminId))
      }
    } catch (e) {
      // 401 已由拦截器处理，403 不弹窗（运营管理员无权限访问 info 时降级使用 localStorage 缓存）
      console.warn('[UserStore] fetchInfo 失败，使用缓存数据:', e?.response?.status)
    }
  }

  function logout() {
    token.value = ''
    adminId.value = 0
    username.value = ''
    role.value = ''
    permissions.value = []
    localStorage.removeItem('admin_token')
    localStorage.removeItem('admin_id')
    localStorage.removeItem('admin_username')
    localStorage.removeItem('admin_role')
  }

  return {
    token, adminId, username, role, permissions,
    isLoggedIn, isSuperAdmin,
    hasPermission,
    login, logout, fetchInfo
  }
})