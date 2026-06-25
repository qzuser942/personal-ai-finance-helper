/**
 * 文件下载工具
 * @author 胡宪棋 软件2413 202421332084
 */
import axios from 'axios'
import { ElMessage } from 'element-plus'

/**
 * 下载远程文件（带 Authorization header）
 * 关键修复：解决 window.open() 无法附加 token 的问题
 * @param {string} url - 后端接口 URL（如 '/api/admin/log/export'）
 * @param {object} params - URL 查询参数
 * @param {string} filename - 下载文件名（含扩展名）
 * @returns {Promise<void>}
 */
export async function downloadFile(url, params, filename) {
  try {
    const baseURL = import.meta.env.VITE_API_BASE_URL || ''
    const token = localStorage.getItem('admin_token') || ''
    const res = await axios({
      url,
      method: 'get',
      baseURL,
      params: { ...params, _t: Date.now() },
      responseType: 'blob',
      headers: {
        'Authorization': token ? `Bearer ${token}` : ''
      }
    })
    const blob = res.data
    if (!blob || (blob.size !== undefined && blob.size === 0)) {
      ElMessage.error('下载失败：未获取到文件内容')
      return
    }
    // 从响应头里取 filename（后端若设置了 content-disposition）
    const disposition = res.headers?.['content-disposition'] || ''
    let finalName = filename
    const match = disposition.match(/filename\*=UTF-8''([^;]+)/) || disposition.match(/filename="?([^";]+)"?/)
    if (match) {
      try { finalName = decodeURIComponent(match[1]) } catch { finalName = match[1] }
    }
    if (!finalName) {
      const ext = (res.headers?.['content-type'] || '').includes('sheet') ? 'xlsx' : 'xls'
      finalName = `download_${Date.now()}.${ext}`
    }
    // 触发浏览器下载
    const blobUrl = window.URL.createObjectURL(new Blob([blob]))
    const link = document.createElement('a')
    link.href = blobUrl
    link.download = finalName
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(blobUrl)
    ElMessage.success('下载成功')
  } catch (e) {
    console.error('[downloadFile] failed', e)
    ElMessage.error('下载失败：' + (e.message || '网络异常'))
    throw e
  }
}
