/**
 * 认证模块API
 * @author 胡宪棋 软件2413 202421332084
 */
import { HttpRequest, ApiResponse } from '../request';
import { RegisterRequest, LoginRequest, LoginResponse, UserProfile, ChangePasswordRequest } from '../../model/UserModel';

export class AuthApi {
  /** 用户注册 POST /api/user/register */
  static async register(data: RegisterRequest): Promise<ApiResponse<object>> {
    return HttpRequest.post('/api/user/register', data, false);
  }

  /** 用户登录 POST /api/user/login */
  static async login(data: LoginRequest): Promise<ApiResponse<LoginResponse>> {
    return HttpRequest.post('/api/user/login', data, false);
  }

  /** 获取用户信息 GET /api/user/profile */
  static async getProfile(): Promise<ApiResponse<UserProfile>> {
    return HttpRequest.get('/api/user/profile');
  }

  /** 修改用户信息 PUT /api/user/profile */
  static async updateProfile(username: string): Promise<ApiResponse<object>> {
    return HttpRequest.put('/api/user/profile', { username });
  }

  /** 修改密码 PUT /api/user/password */
  static async changePassword(data: ChangePasswordRequest): Promise<ApiResponse<null>> {
    return HttpRequest.put('/api/user/password', data);
  }
}
