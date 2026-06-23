/**
 * 预算&存钱目标管理API
 * @author 胡宪棋 软件2413 202421332084
 */
import { get, put } from './request'

/** 查看用户预算 GET /api/admin/budget/{userId} */
export function getUserBudget(userId, yearMonth) {
  return get(`/api/admin/budget/${userId}`, { yearMonth })
}

/** 修正用户预算 PUT /api/admin/budget/{id} */
export function updateUserBudget(id, data) {
  return put(`/api/admin/budget/${id}`, data)
}

/** 查看用户存钱目标 GET /api/admin/save-target/{userId} */
export function getUserTargets(userId) {
  return get(`/api/admin/save-target/${userId}`)
}

/** 修正存钱目标 PUT /api/admin/save-target/{id} */
export function updateUserTarget(id, data) {
  return put(`/api/admin/save-target/${id}`, data)
}
