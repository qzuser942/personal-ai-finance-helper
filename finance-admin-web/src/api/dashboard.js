/**
 * 后台看板API
 * @author 胡宪棋 软件2413 202421332084
 */
import { get } from './request'
import { downloadFile } from './request'

/** 后台总看板数据 GET /api/admin/dashboard?months=6 */
export function getDashboard(months = 6) {
  return get('/api/admin/dashboard', { months })
}

/** 异常检测告警 GET /api/admin/dashboard/alerts */
export function getDashboardAlerts() {
  return get('/api/admin/dashboard/alerts')
}

/** 导出看板报表CSV（超时60s + 空Blob检测 + 文件名解析） */
export function exportDashboard(months = 6) {
  return downloadFile('/api/admin/dashboard/export', { months }, 'dashboard.csv')
}

/** 全平台消费统计 GET /api/admin/bill/statistics */
export function getBillStatistics(yearMonth) {
  return get('/api/admin/bill/statistics', { yearMonth })
}