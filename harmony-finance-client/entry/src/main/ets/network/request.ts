/**
 * 统一网络请求封装
 * - 自动携带JWT令牌
 * - 统一错误处理
 * - Token过期自动跳转登录页
 * - 请求/响应拦截
 * - 超时设置
 * @author 胡宪棋 软件2413 202421332084
 */
import http from '@ohos.net.http';
import { AppState } from '../store/AppState';
import { ToastUtil } from '../utils/toast';
import router from '@ohos.router';

/** 统一后端响应格式 */
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  errors?: Array<{ field: string, message: string }>
  timestamp: number
}

/** 分页响应格式 */
export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
  totalPages: number
}

/** 请求配置 */
interface RequestConfig {
  url: string
  method?: http.RequestMethod
  params?: Record<string, string | number | boolean>
  data?: object
  timeout?: number
  needAuth?: boolean
}

const TAG = 'HttpRequest';

export class HttpRequest {
  /** 默认超时时间（毫秒） */
  private static readonly DEFAULT_TIMEOUT: number = 30000;
  /** 连接超时 */
  private static readonly CONNECT_TIMEOUT: number = 10000;

  /**
   * 通用请求方法
   */
  static async request<T>(config: RequestConfig): Promise<ApiResponse<T>> {
    const {
      url,
      method = http.RequestMethod.GET,
      params,
      data,
      timeout = HttpRequest.DEFAULT_TIMEOUT,
      needAuth = true
    } = config;

    // 构建完整URL
    let fullUrl = `${AppState.BASE_URL}${url}`;

    // 拼接Query参数
    if (params) {
      const queryStr = Object.entries(params)
        .filter(([_, v]) => v !== undefined && v !== null && v !== '')
        .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(String(v))}`)
        .join('&');
      if (queryStr) {
        fullUrl += `?${queryStr}`;
      }
    }

    const httpRequest = http.createHttp();

    try {
      // 构建请求选项
      const options: http.HttpRequestOptions = {
        method: method,
        header: {
          'Content-Type': 'application/json',
          'Accept': 'application/json'
        },
        readTimeout: timeout,
        connectTimeout: HttpRequest.CONNECT_TIMEOUT
      };

      // 自动携带JWT令牌
      if (needAuth && AppState.token) {
        options.header = {
          ...options.header,
          'Authorization': AppState.getAuthHeader()
        };
      }

      // 设置请求体
      if (data && (method === http.RequestMethod.POST || method === http.RequestMethod.PUT)) {
        options.extraData = JSON.stringify(data);
      }

      console.info(`${TAG}: [${method}] ${fullUrl}`);

      // 发送请求
      const response = await httpRequest.request(fullUrl, options);
      const statusCode = response.responseCode;

      // 响应处理
      if (statusCode === 200) {
        const result = JSON.parse(response.result as string) as ApiResponse<T>;
        // Token过期处理
        if (result.code === 10004) {
          await HttpRequest.handleTokenExpired();
          return result;
        }
        return result;
      } else if (statusCode === 401) {
        // HTTP 401未认证
        await HttpRequest.handleTokenExpired();
        return {
          code: 401,
          message: '登录已过期，请重新登录',
          data: null as T,
          timestamp: Date.now()
        };
      } else if (statusCode === 403) {
        ToastUtil.showError('无权限访问');
        return {
          code: 403,
          message: '无权限访问',
          data: null as T,
          timestamp: Date.now()
        };
      } else {
        ToastUtil.showError(`请求失败 (${statusCode})`);
        return {
          code: statusCode,
          message: `请求失败 (${statusCode})`,
          data: null as T,
          timestamp: Date.now()
        };
      }
    } catch (err) {
      const errObj = JSON.stringify(err);
      console.error(`${TAG}: 网络请求异常`, errObj);

      // 判断网络不可达（离线状态）
      if (errObj.includes('timeout') || errObj.includes('Timeout')) {
        ToastUtil.showError('网络连接超时，请检查网络');
      } else if (errObj.includes('Unreachable') || errObj.includes('refused') ||
                 errObj.includes('resolve') || errObj.includes('unknown host')) {
        // 静默处理，由各业务层判断处理
        console.info(`${TAG}: 网络不可达，可能处于离线模式`);
      } else {
        ToastUtil.showError('网络异常，请稍后重试');
      }

      return {
        code: -1,
        message: '网络异常',
        data: null as T,
        timestamp: Date.now()
      };
    } finally {
      httpRequest.destroy();
    }
  }

  /** GET请求 */
  static async get<T>(url: string, params?: Record<string, string | number | boolean>, needAuth: boolean = true):
    Promise<ApiResponse<T>> {
    return HttpRequest.request<T>({ url, method: http.RequestMethod.GET, params, needAuth });
  }

  /** POST请求 */
  static async post<T>(url: string, data?: object, needAuth: boolean = true): Promise<ApiResponse<T>> {
    return HttpRequest.request<T>({ url, method: http.RequestMethod.POST, data, needAuth });
  }

  /** PUT请求 */
  static async put<T>(url: string, data?: object, needAuth: boolean = true): Promise<ApiResponse<T>> {
    return HttpRequest.request<T>({ url, method: http.RequestMethod.PUT, data, needAuth });
  }

  /** DELETE请求 */
  static async delete<T>(url: string, needAuth: boolean = true): Promise<ApiResponse<T>> {
    return HttpRequest.request<T>({ url, method: http.RequestMethod.DELETE, needAuth });
  }

  /**
   * 上传文件
   */
  static async uploadFile(url: string, filePath: string, fileName: string):
    Promise<ApiResponse<object>> {
    const fullUrl = `${AppState.BASE_URL}${url}`;
    const httpRequest = http.createHttp();

    try {
      const response = await httpRequest.request(fullUrl, {
        method: http.RequestMethod.POST,
        header: {
          'Content-Type': 'multipart/form-data',
          'Authorization': AppState.getAuthHeader()
        },
        extraData: filePath,
        readTimeout: 60000,
        connectTimeout: 15000
      });

      if (response.responseCode === 200) {
        return JSON.parse(response.result as string) as ApiResponse<object>;
      }

      if (response.responseCode === 401) {
        await HttpRequest.handleTokenExpired();
      }

      return {
        code: response.responseCode,
        message: '上传失败',
        data: null,
        timestamp: Date.now()
      };
    } catch (err) {
      console.error(`${TAG}: 文件上传异常`, JSON.stringify(err));
      ToastUtil.showError('上传失败，请重试');
      return { code: -1, message: '上传异常', data: null, timestamp: Date.now() };
    } finally {
      httpRequest.destroy();
    }
  }

  /**
   * 下载文件
   */
  static async downloadFile(url: string): Promise<string> {
    const fullUrl = `${AppState.BASE_URL}${url}`;
    const httpRequest = http.createHttp();

    try {
      const response = await httpRequest.request(fullUrl, {
        method: http.RequestMethod.GET,
        header: {
          'Authorization': AppState.getAuthHeader()
        },
        readTimeout: 60000,
        connectTimeout: 15000
      });

      if (response.responseCode === 200) {
        return response.result as string;
      }
      return '';
    } catch (err) {
      console.error(`${TAG}: 文件下载异常`, JSON.stringify(err));
      return '';
    } finally {
      httpRequest.destroy();
    }
  }

  /**
   * Token过期处理 - 清除登录态并跳转登录页
   */
  private static async handleTokenExpired(): Promise<void> {
    await AppState.logout();
    ToastUtil.showError('登录已过期，请重新登录');
    router.replaceUrl({ url: 'pages/auth/LoginPage', params: { expired: true } });
  }
}
