/**
 * 用户管理API
 * @author 胡宪棋 软件2413 202421332084
 */
import { get, put } from './request'

/** 用户分页列表 GET /api/admin/user/page */
export function getUserPage(params) {
  return get('/api/admin/user/page', params)
}

/** 冻结/解冻 PUT /api/admin/user/{userId}/status */
export function updateUserStatus(userId, status) {
  return put(`/api/admin/user/${userId}/status`, { status })
}

/** 重置密码 PUT /api/admin/user/{userId}/reset-password */
export function resetUserPassword(userId) {
  return put(`/api/admin/user/${userId}/reset-password`)
}

/** 导出用户Excel GET /api/admin/user/export */
export function exportUsers(params) {
  return get('/api/admin/user/export', { ...params, _t: Date.now() })
}
