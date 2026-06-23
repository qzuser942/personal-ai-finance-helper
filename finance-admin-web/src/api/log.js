/**
 * 操作日志API
 * @author 胡宪棋 软件2413 202421332084
 */
import { get } from './request'

/** 操作日志分页 GET /api/admin/log/page */
export function getLogPage(params) {
  return get('/api/admin/log/page', params)
}

/** 导出操作日志 GET /api/admin/log/export */
export function exportLogs(params) {
  return get('/api/admin/log/export', { ...params, _t: Date.now() })
}
