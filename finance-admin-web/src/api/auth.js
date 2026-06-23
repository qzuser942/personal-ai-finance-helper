/**
 * 管理员认证API
 * @author 胡宪棋 软件2413 202421332084
 */
import service, { get, post, put, del } from './request'

/** 管理员登录 POST /api/admin/login */
export function login(data) {
  return post('/api/admin/login', data)
}
