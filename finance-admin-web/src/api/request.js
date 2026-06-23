/**
 * Axios统一请求封装 - 管理员后台专用
 * - 自动携带Admin JWT Token
 * - 统一错误拦截与提示
 * - 401/403 自动跳转登录
 * @author 胡宪棋 软件2413 202421332084
 */
import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useUserStore } from '@/store/user'

const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://192.168.1.100:8080'

const service = axios.create({
  baseURL: BASE_URL,
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' }
})

// 请求拦截器 - 自动添加Admin Token
service.interceptors.request.use(
  config => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器 - 统一处理
service.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code && res.code !== 200) {
      ElMessage.error(res.message || '操作失败')
      // Token过期
      if (res.code === 10004) {
        handleTokenExpired()
      }
      return Promise.reject(new Error(res.message || 'Error'))
    }
    return res
  },
  error => {
    if (error.response) {
      const { status } = error.response
      if (status === 401) {
        handleTokenExpired()
      } else if (status === 403) {
        ElMessage.error('无权限访问')
      } else {
        ElMessage.error(`请求失败(${status})`)
      }
    } else {
      ElMessage.error('网络异常，请检查连接')
    }
    return Promise.reject(error)
  }
)

function handleTokenExpired() {
  const userStore = useUserStore()
  userStore.logout()
  ElMessage.warning('登录已过期，请重新登录')
  router.replace('/login')
}

// 便捷方法
export function get(url, params) {
  return service({ url, method: 'get', params })
}

export function post(url, data) {
  return service({ url, method: 'post', data })
}

export function put(url, data) {
  return service({ url, method: 'put', data })
}

export function del(url) {
  return service({ url, method: 'delete' })
}

export default service
