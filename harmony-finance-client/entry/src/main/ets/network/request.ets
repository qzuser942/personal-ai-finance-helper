/**
 * 统一网络请求封装
 * - 自动携带JWT令牌
 * - 统一错误处理
 * - Token过期自动跳转登录页
 * - 请求/响应拦截
 * - 超时设置
 * @author 胡宪棋 软件2413 202421332084
 */
import http from "@ohos.net.http";
import fs from "@ohos.file.fs";
import environment from "@ohos.file.environment";
import common from "@ohos.app.ability.common";
import { AppState } from "../store/AppState";
import { ToastUtil } from "../utils/toast";
import router from "@ohos.router";
import { FileUtil } from "../utils/FileUtil";

/** 字段错误 */
export interface FieldError {
  field: string;
  message: string;
}

/** 统一后端响应格式 */
export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  errors?: Array<FieldError>;
  timestamp: number;
}

/** 分页响应格式 */
export interface PageResult<T> {
  records: T[];
  total: number;
  page: number;
  size: number;
  totalPages: number;
}

/** 请求配置 */
interface RequestConfig {
  url: string;
  method?: http.RequestMethod;
  params?: object;
  data?: object;
  timeout?: number;
  needAuth?: boolean;
}

/** 上传结果 */
export interface UploadResult {
  fileName: string;
  filePath: string;
  fileUrl: string;
  fileSize: number;
}

/** 登录页导航参数 */
export interface NavExpiredParams {
  expired: boolean;
}

/** 错误响应数据占位 */
interface EmptyData {
  placeholder: number;
}

const TAG = "HttpRequest";

export class HttpRequest {
  /** 默认超时时间（毫秒） */
  private static readonly DEFAULT_TIMEOUT: number = 30000;
  /** 连接超时 */
  private static readonly CONNECT_TIMEOUT: number = 10000;

  /**
   * 通用请求方法
   */
  static async request<T>(config: RequestConfig): Promise<ApiResponse<T>> {
    const url: string = config.url;
    const method: http.RequestMethod = config.method ?? http.RequestMethod.GET;
    const params: object | undefined = config.params;
    const data: object | undefined = config.data;
    const timeout: number = config.timeout ?? HttpRequest.DEFAULT_TIMEOUT;
    const needAuth: boolean = config.needAuth ?? true;

    // 构建完整URL
    let fullUrl = `${AppState.BASE_URL}${url}`;

    // 拼接Query参数
    if (params) {
      const paramsRecord = params as Record<string, string | number | boolean>;
      const keys: string[] = Object.keys(paramsRecord);
      const pairs: string[] = [];
      for (const k of keys) {
        const v = paramsRecord[k];
        if (v !== undefined && v !== null && v !== "") {
          pairs.push(`${encodeURIComponent(k)}=${encodeURIComponent(String(v))}`);
        }
      }
      const queryStr = pairs.join("&");
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
          "Content-Type": "application/json",
          Accept: "application/json",
        },
        readTimeout: timeout,
        connectTimeout: HttpRequest.CONNECT_TIMEOUT,
      };

      // 自动携带JWT令牌
      if (needAuth && AppState.token) {
        const authHeader: string = AppState.getAuthHeader();
        if (options.header) {
          options.header["Authorization"] = authHeader;
        }
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
        return HttpRequest.buildErrorResponse<T>(401, "登录已过期，请重新登录");
      } else if (statusCode === 403) {
        ToastUtil.showError("无权限访问");
        return HttpRequest.buildErrorResponse<T>(403, "无权限访问");
      } else {
        ToastUtil.showError(`请求失败 (${statusCode})`);
        return HttpRequest.buildErrorResponse<T>(statusCode, `请求失败 (${statusCode})`);
      }
    } catch (err) {
      const errObj = JSON.stringify(err);
      console.error(`${TAG}: 网络请求异常`, errObj);

      // 判断网络不可达（离线状态）
      if (errObj.includes("timeout") || errObj.includes("Timeout")) {
        ToastUtil.showError("网络连接超时，请检查网络");
      } else if (
        errObj.includes("Unreachable") ||
        errObj.includes("refused") ||
        errObj.includes("resolve") ||
        errObj.includes("unknown host")
      ) {
        // 静默处理，由各业务层判断处理
        console.info(`${TAG}: 网络不可达，可能处于离线模式`);
      } else {
        ToastUtil.showError("网络异常，请稍后重试");
      }

      return HttpRequest.buildErrorResponse<T>(-1, "网络异常");
    } finally {
      httpRequest.destroy();
    }
  }

  /** 构建统一错误响应 */
  private static buildErrorResponse<T>(code: number, message: string): ApiResponse<T> {
    const emptyData: EmptyData = { placeholder: 0 };
    const resp: ApiResponse<T> = {
      code: code,
      message: message,
      data: emptyData as Object as T,
      timestamp: Date.now(),
    };
    return resp;
  }

  /** GET请求 */
  static async get<T>(
    url: string,
    params?: object,
    needAuth: boolean = true,
  ): Promise<ApiResponse<T>> {
    return HttpRequest.request<T>({ url, method: http.RequestMethod.GET, params, needAuth });
  }

  /** POST请求 */
  static async post<T>(
    url: string,
    data?: object,
    needAuth: boolean = true,
  ): Promise<ApiResponse<T>> {
    return HttpRequest.request<T>({ url, method: http.RequestMethod.POST, data, needAuth });
  }

  /** PUT请求 */
  static async put<T>(
    url: string,
    data?: object,
    needAuth: boolean = true,
  ): Promise<ApiResponse<T>> {
    return HttpRequest.request<T>({ url, method: http.RequestMethod.PUT, data, needAuth });
  }

  /** DELETE请求 */
  static async delete<T>(url: string, needAuth: boolean = true): Promise<ApiResponse<T>> {
    return HttpRequest.request<T>({ url, method: http.RequestMethod.DELETE, needAuth });
  }

  /**
   * 上传文件
   */
  static async uploadFile(
    url: string,
    filePath: string,
    fileName: string,
  ): Promise<ApiResponse<UploadResult>> {
    const fullUrl = `${AppState.BASE_URL}${url}`;
    const httpRequest = http.createHttp();

    const errorResp = (code: number, message: string): ApiResponse<UploadResult> => {
      const emptyData: UploadResult = {
        fileName: "",
        filePath: "",
        fileUrl: "",
        fileSize: 0,
      };
      const r: ApiResponse<UploadResult> = {
        code: code,
        message: message,
        data: emptyData,
        timestamp: Date.now(),
      };
      return r;
    };

    try {
      const response = await httpRequest.request(fullUrl, {
        method: http.RequestMethod.POST,
        header: {
          Authorization: AppState.getAuthHeader(),
        },
        extraData: filePath,
        readTimeout: 60000,
        connectTimeout: 15000,
      });

      if (response.responseCode === 200) {
        return JSON.parse(response.result as string) as ApiResponse<UploadResult>;
      }

      if (response.responseCode === 401) {
        await HttpRequest.handleTokenExpired();
      }

      return errorResp(response.responseCode, "上传失败");
    } catch (err) {
      console.error(`${TAG}: 文件上传异常`, JSON.stringify(err));
      ToastUtil.showError("上传失败，请重试");
      return errorResp(-1, "上传异常");
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
          Authorization: AppState.getAuthHeader(),
        },
        readTimeout: 60000,
        connectTimeout: 15000,
      });

      if (response.responseCode === 200) {
        return response.result as string;
      }
      return "";
    } catch (err) {
      console.error(`${TAG}: 文件下载异常`, JSON.stringify(err));
      return "";
    } finally {
      httpRequest.destroy();
    }
  }

  /**
   * 下载文件到应用沙箱并同步至公共下载目录
   * <p>用于 P0-1 / P0-2：把后端 /api/bill/export 等接口的 Excel 二进制流保存到本地。
   * <p>实现方式：使用 http.createHttp() 下载二进制流，分两步落盘：
   *   1) 先确保目录存在，用 fs.openSync(CREATE) 创建空文件占位
   *   2) 再用 fs.openSync(WRITE_ONLY|TRUNC) 打开写入数据
   *   两步分离可避免 CREATE|WRITE_ONLY|TRUNC 组合在不同 API 版本下的兼容问题。
   * <p>优先写入公共下载目录，失败回退至沙箱；沙箱失败则回退至 cacheDir。
   * @param fullUrl 完整 URL（含 BASE_URL 和 query string）
   * @param fileName 保存到沙箱的文件名（不含路径）
   * @returns 公共下载目录或沙箱内的完整文件路径
   */
  static downloadToFile(fullUrl: string, fileName?: string): Promise<string> {
    const ctx: common.Context = AppState.context;
    if (!ctx) {
      return Promise.reject(new Error("AppState.context 未初始化"));
    }
    const finalName: string = fileName || `download_${Date.now()}.bin`;

    return new Promise<string>((resolve, reject) => {
      let settled: boolean = false;
      const doResolve = (path: string): void => {
        if (!settled) {
          settled = true;
          resolve(path);
        }
      };
      const doReject = (err: Error): void => {
        if (!settled) {
          settled = true;
          reject(err);
        }
      };

      const httpReq = http.createHttp();

      httpReq
        .request(fullUrl, {
          method: http.RequestMethod.GET,
          header: {
            Authorization: AppState.getAuthHeader(),
          },
          readTimeout: 60000,
          connectTimeout: 15000,
          expectDataType: http.HttpDataType.ARRAY_BUFFER,
        })
        .then((resp) => {
          if (resp.responseCode !== 200) {
            doReject(new Error(`服务器返回错误 (${resp.responseCode})`));
            return;
          }

          const arrayBuffer = resp.result as ArrayBuffer;
          if (!arrayBuffer || arrayBuffer.byteLength === 0) {
            doReject(new Error("下载内容为空"));
            return;
          }

          const savePath = HttpRequest.writeBufferToFile(ctx, finalName, arrayBuffer);
          if (!savePath) {
            doReject(new Error("文件写入失败"));
            return;
          }

          try {
            const publicDir = environment.getUserDownloadDir();
            FileUtil.ensureDirSync(publicDir);
            const publicPath = `${publicDir}/${finalName}`;
            fs.copyFileSync(savePath, publicPath);
            doResolve(publicPath);
          } catch (e) {
            console.info(`${TAG}: 复制到公共下载目录失败，使用沙箱路径`, JSON.stringify(e));
            doResolve(savePath);
          }
        })
        .catch((err: Error) => {
          console.error(`${TAG}: 文件下载异常`, JSON.stringify(err));
          doReject(new Error(err.message || "下载失败"));
        })
        .finally(() => {
          httpReq.destroy();
        });
    });
  }

  /**
   * 将 ArrayBuffer 写入文件（多级回退策略）
   * 1) 优先写入 filesDir/exports/
   * 2) 失败则回退 filesDir/
   * 3) 再失败则回退 cacheDir/
   */
  private static writeBufferToFile(
    ctx: common.Context,
    fileName: string,
    buffer: ArrayBuffer,
  ): string | null {
    const candidates: string[] = [`${ctx.filesDir}/exports`, ctx.filesDir];
    if (ctx.cacheDir) {
      candidates.push(ctx.cacheDir);
    }

    for (const dir of candidates) {
      try {
        FileUtil.ensureDirSync(dir);
        const filePath = `${dir}/${fileName}`;

        const fd = fs.openSync(
          filePath,
          fs.OpenMode.CREATE | fs.OpenMode.WRITE_ONLY | fs.OpenMode.TRUNC,
        );
        try {
          fs.writeSync(fd.fd, buffer);
          console.info(`${TAG}: 文件写入成功: ${filePath}`);
          return filePath;
        } finally {
          fs.closeSync(fd);
        }
      } catch (e) {
        console.warn(`${TAG}: 写入 ${dir} 失败:`, JSON.stringify(e));
      }
    }

    console.error(`${TAG}: 所有路径写入均失败`);
    return null;
  }

  /**
   * Token过期处理 - 清除登录态并跳转登录页
   */
  private static async handleTokenExpired(): Promise<void> {
    await AppState.logout();
    ToastUtil.showError("登录已过期，请重新登录");
    const navParams: NavExpiredParams = { expired: true };
    router.replaceUrl({ url: "pages/auth/LoginPage", params: navParams });
  }
}
