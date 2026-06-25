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

// 开发环境走 Vite 代理（/api -> 8080），避免 CORS 和 IP 写死问题
// 生产环境可通过环境变量 VITE_API_BASE_URL 指定真实后端地址
const BASE_URL = import.meta.env.VITE_API_BASE_URL || ''

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
    // Blob 响应（文件下载/导出）返回完整 response，保留 headers 供解析文件名
    if (response.config.responseType === 'blob' || response.data instanceof Blob) {
      return response
    }
    const res = response.data
    if (res.code && res.code !== 200) {
      ElMessage.error(res.message || '操作失败')
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
        // 403 由具体页面/操作自行处理，不在拦截器统一弹窗
        // 避免运营管理员每次路由切换 fetchInfo 时反复弹窗"无权限"
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

/**
 * 文件导出专用方法：超时60s + 自动解析 Content-Disposition 文件名 + 空Blob检测
 * @param {string} url 导出接口地址
 * @param {object} params 请求参数
 * @param {string} fallbackName 兜底文件名
 * @returns {Promise<{ blob: Blob, fileName: string }>}
 */
export async function downloadFile(url, params, fallbackName = 'export.csv') {
  try {
    const resp = await service({
      url,
      method: 'get',
      params,
      responseType: 'blob',
      timeout: 60_000
    })
    const blob = resp.data
    if (!blob || blob.size === 0) {
      ElMessage.warning('导出数据为空，请确认查询条件')
      return null
    }
    // 从 Content-Disposition 解析文件名
    let fileName = fallbackName
    const disposition = resp.headers['content-disposition']
    if (disposition) {
      const match = disposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/)
      if (match && match[1]) {
        fileName = decodeURIComponent(match[1].replace(/['"]/g, ''))
      }
    }
    return { blob, fileName }
  } catch (e) {
    if (e.response?.status !== 200) {
      ElMessage.error('导出失败，请稍后重试')
    }
    return null
  }
}