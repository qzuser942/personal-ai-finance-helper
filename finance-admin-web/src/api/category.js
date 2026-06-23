/**
 * 分类管理API
 * @author 胡宪棋 软件2413 202421332084
 */
import { get, post, put, del } from './request'

/** 获取全部分类 GET /api/admin/category/list */
export function getCategoryList() {
  return get('/api/admin/category/list')
}

/** 新增全局分类 POST /api/admin/category */
export function addCategory(data) {
  return post('/api/admin/category', data)
}

/** 修改分类 PUT /api/admin/category/{id} */
export function updateCategory(id, data) {
  return put(`/api/admin/category/${id}`, data)
}

/** 删除分类 DELETE /api/admin/category/{id} */
export function deleteCategory(id) {
  return del(`/api/admin/category/${id}`)
}
