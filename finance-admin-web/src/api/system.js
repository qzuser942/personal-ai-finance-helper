/**
 * 系统运维API - 管理员账号、文件管理
 * @author 胡宪棋 软件2413 202421332084
 */
import { get, post, put, del } from './request'

// ===== 管理员账号管理 =====
/** 管理员列表 GET /api/admin/account/page */
export function getAdminPage(params) {
  return get('/api/admin/account/page', params)
}

/** 新增管理员 POST /api/admin/account */
export function addAdmin(data) {
  return post('/api/admin/account', data)
}

/** 修改管理员 PUT /api/admin/account/{id} */
export function updateAdmin(id, data) {
  return put(`/api/admin/account/${id}`, data)
}

/** 删除管理员 DELETE /api/admin/account/{id} */
export function deleteAdmin(id) {
  return del(`/api/admin/account/${id}`)
}

/** 重置管理员密码 PUT /api/admin/account/{id}/reset-password */
export function resetAdminPassword(id) {
  return put(`/api/admin/account/${id}/reset-password`)
}

// ===== 文件管理 =====
/** 文件存储概览 GET /api/admin/file/overview */
export function getFileOverview() {
  return get('/api/admin/file/overview')
}

/** 清理无效文件 DELETE /api/admin/file/clean */
export function cleanFiles() {
  return del('/api/admin/file/clean')
}

// ===== 数据库备份 =====
/** 数据库备份 POST /api/admin/database/backup */
export function databaseBackup() {
  return post('/api/admin/database/backup')
}

/** 备份历史 GET /api/admin/database/backup/log */
export function getBackupLog(params) {
  return get('/api/admin/database/backup/log', params)
}
