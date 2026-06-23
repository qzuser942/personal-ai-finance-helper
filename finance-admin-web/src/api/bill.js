/**
 * 账单管理API
 * @author 胡宪棋 软件2413 202421332084
 */
import { get, put, del } from './request'

/** 全平台账单分页 GET /api/admin/bill/page */
export function getBillPage(params) {
  return get('/api/admin/bill/page', params)
}

/** 编辑账单 PUT /api/admin/bill/{id} */
export function updateBill(id, data) {
  return put(`/api/admin/bill/${id}`, data)
}

/** 删除账单 DELETE /api/admin/bill/{id} */
export function deleteBill(id) {
  return del(`/api/admin/bill/${id}`)
}

/** 全平台消费统计 GET /api/admin/bill/statistics */
export function getBillStats(yearMonth) {
  return get('/api/admin/bill/statistics', { yearMonth })
}

/** 全量导出账单 GET /api/admin/bill/export-all */
export function exportAllBills(params) {
  return get('/api/admin/bill/export-all', { ...params, _t: Date.now() })
}
