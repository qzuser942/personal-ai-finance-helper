/**
 * 后台看板API
 * @author 胡宪棋 软件2413 202421332084
 */
import { get } from './request'

/** 后台总看板数据 GET /api/admin/dashboard */
export function getDashboard() {
  return get('/api/admin/dashboard')
}

/** 全平台消费统计 GET /api/admin/bill/statistics */
export function getBillStatistics(yearMonth) {
  return get('/api/admin/bill/statistics', { yearMonth })
}
