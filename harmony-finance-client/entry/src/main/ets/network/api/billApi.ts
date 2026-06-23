/**
 * 账单模块API
 * @author 胡宪棋 软件2413 202421332084
 */
import { HttpRequest, ApiResponse, PageResult } from '../request';
import { BillAddRequest, BillListItem, BillDetail, OfflineBill } from '../../model/BillModel';

export class BillApi {
  /** 新增账单 POST /api/bill */
  static async add(data: BillAddRequest): Promise<ApiResponse<BillDetail>> {
    return HttpRequest.post('/api/bill', data);
  }

  /** 离线批量同步 POST /api/bill/sync-batch */
  static async syncBatch(bills: object[]): Promise<ApiResponse<SyncBatchResult>> {
    return HttpRequest.post('/api/bill/sync-batch', bills);
  }

  /** 查询账单详情 GET /api/bill/{id} */
  static async getDetail(id: number): Promise<ApiResponse<BillDetail>> {
    return HttpRequest.get(`/api/bill/${id}`);
  }

  /** 分页查询账单 GET /api/bill/page */
  static async getPage(page: number = 1, size: number = 20): Promise<ApiResponse<PageResult<BillListItem>>> {
    return HttpRequest.get('/api/bill/page', { page, size });
  }

  /** 多条件筛选账单 GET /api/bill/search */
  static async search(params: BillSearchParams): Promise<ApiResponse<PageResult<BillListItem>>> {
    return HttpRequest.get('/api/bill/search', params as Record<string, string | number | boolean>);
  }

  /** 修改账单 PUT /api/bill/{id} */
  static async update(id: number, data: Partial<BillAddRequest>): Promise<ApiResponse<BillDetail>> {
    return HttpRequest.put(`/api/bill/${id}`, data);
  }

  /** 删除账单 DELETE /api/bill/{id} */
  static async delete(id: number): Promise<ApiResponse<null>> {
    return HttpRequest.delete(`/api/bill/${id}`);
  }
}

/** 批量同步结果 */
export interface SyncBatchResult {
  total: number
  successCount: number
  failCount: number
  duplicateCount: number
  results: Array<{
    uuid: string
    status: 'success' | 'duplicate' | 'fail'
    billId?: number
    message: string
  }>
}

/** 账单搜索参数 */
export interface BillSearchParams {
  page?: number
  size?: number
  type?: string
  categoryId?: number
  keyword?: string
  startDate?: string
  endDate?: string
  yearMonth?: string
  minAmount?: number
  maxAmount?: number
}
