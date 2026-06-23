/**
 * 用户相关数据模型
 * @author 胡宪棋 软件2413 202421332084
 */

/** 注册请求 */
export interface RegisterRequest {
  username: string
  password: string
  confirmPassword: string
}

/** 登录请求 */
export interface LoginRequest {
  username: string
  password: string
}

/** 登录响应 */
export interface LoginResponse {
  token: string
  tokenType: string
  expiresIn: number
  userId: number
  username: string
}

/** 用户信息 */
export interface UserProfile {
  userId: number
  username: string
  status: number       // 1-正常 0-冻结
  lastLoginTime: string
  createdAt: string
}

/** 修改密码请求 */
export interface ChangePasswordRequest {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}
