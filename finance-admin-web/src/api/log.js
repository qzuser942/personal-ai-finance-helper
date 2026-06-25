/**
 * 操作日志API
 * @author 胡宪棋 软件2413 202421332084
 */
import axios from 'axios'
import { get } from './request'

/** 操作日志分页 GET /api/admin/log/page */
export function getLogPage(params) {
  return get('/api/admin/log/page', params)
}

/**
 * 导出操作日志 GET /api/admin/log/export
 * 关键修复：直接用 axios 调用（不走统一拦截器），确保返回原始 Blob 数据
 * 原代码走 get() → 响应拦截器会试图解析 res.code（二进制文件会失败）
 */
export function exportLogs(params) {
  const baseURL = import.meta.env.VITE_API_BASE_URL || ''
  const token = localStorage.getItem('admin_token') || ''
  return axios({
    url: '/api/admin/log/export',
    method: 'get',
    baseURL,
    params: { ...params, _t: Date.now() },
    responseType: 'blob',          // 关键：告诉 axios 返回二进制
    headers: {
      'Authorization': token ? `Bearer ${token}` : ''
    }
  }).then(res => res.data)         // 返回 Blob 对象
}
