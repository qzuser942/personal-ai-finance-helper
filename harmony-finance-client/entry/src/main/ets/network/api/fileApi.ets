/**
 * 文件模块API
 * @author 胡宪棋 软件2413 202421332084
 */
import { HttpRequest, ApiResponse } from '../request';

export interface UploadResult {
  fileName: string
  filePath: string
  fileUrl: string
  fileSize: number
}

export class FileApi {
  /** 上传小票图片 POST /api/file/upload/receipt (multipart/form-data) */
  static async uploadReceipt(filePath: string, fileName: string): Promise<ApiResponse<UploadResult>> {
    return HttpRequest.uploadFile('/api/file/upload/receipt', filePath, fileName) as Promise<ApiResponse<UploadResult>>;
  }

  /** 获取图片完整访问URL */
  static getImageUrl(imagePath: string): string {
    if (!imagePath) return '';
    if (imagePath.startsWith('http')) return imagePath;
    return `http://192.168.1.100:8080${imagePath}`;
  }
}
