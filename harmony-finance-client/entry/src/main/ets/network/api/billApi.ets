/**
 * 账单模块API
 * @author 胡宪棋 软件2413 202421332084
 */
import fs from "@ohos.file.fs";
import { HttpRequest, ApiResponse, PageResult } from "../request";
import { BillAddRequest, BillListItem, BillDetail, OfflineBill } from "../../model/BillModel";
import { SyncBillItem } from "../../model/SyncModel";
import { FileApi } from "./fileApi";

export class BillApi {
  /** 新增账单 POST /api/bill */
  static async add(data: BillAddRequest): Promise<ApiResponse<BillDetail>> {
    return HttpRequest.post("/api/bill", data);
  }

  /** 离线批量同步 POST /api/bill/sync-batch */
  static async syncBatch(bills: object[]): Promise<ApiResponse<SyncBatchResult>> {
    return HttpRequest.post("/api/bill/sync-batch", bills);
  }

  /**
   * 离线账单同步（含凭证图片上传）
   * 对每条待同步账单：如有凭证图片则先上传，再组装 SyncBillItem 批量提交
   * @param pending 待同步的离线账单列表
   * @returns SyncBatchResult 或 null（网络不可达）
   */
  static async syncOfflineBills(pending: OfflineBill[]): Promise<SyncBatchResult | null> {
    const syncData: SyncBillItem[] = [];

    for (const b of pending) {
      let hasImage = false;
      let receiptImage = "";

      if (b.hasImage && b.receiptImage) {
        try {
          fs.accessSync(b.receiptImage);
          const uploadRes = await FileApi.uploadReceipt(b.receiptImage, "receipt.jpg");
          if (uploadRes.code === 200 && uploadRes.data) {
            hasImage = true;
            receiptImage = uploadRes.data.filePath;
          }
        } catch (e) {
          console.info(`BillApi: 离线账单 ${b.uuid} 凭证图片上传失败或文件不存在`);
        }
      }

      syncData.push({
        uuid: b.uuid,
        amount: b.amount,
        type: b.type,
        categoryId: b.categoryId,
        categoryName: b.categoryName,
        remark: b.remark,
        consumeTime: b.consumeTime,
        createdAt: b.createdAt,
        hasImage: hasImage,
        receiptImage: receiptImage,
      });
    }

    const res = await BillApi.syncBatch(syncData);
    if (res.code === 200 && res.data) {
      return res.data;
    }
    return null;
  }

  /** 查询账单详情 GET /api/bill/{id} */
  static async getDetail(id: number): Promise<ApiResponse<BillDetail>> {
    return HttpRequest.get(`/api/bill/${id}`);
  }

  /** 分页查询账单 GET /api/bill/page */
  static async getPage(
    page: number = 1,
    size: number = 20,
  ): Promise<ApiResponse<PageResult<BillListItem>>> {
    const params: BillPageParams = { page: page, size: size };
    return HttpRequest.get("/api/bill/page", params);
  }

  /** 多条件筛选账单 GET /api/bill/search */
  static async search(params: BillSearchParams): Promise<ApiResponse<PageResult<BillListItem>>> {
    const flat: Record<string, string | number | boolean> = {};
    if (params.page !== undefined) flat["page"] = params.page;
    if (params.size !== undefined) flat["size"] = params.size;
    if (params.type !== undefined) flat["type"] = params.type;
    if (params.categoryId !== undefined) flat["categoryId"] = params.categoryId;
    if (params.keyword !== undefined) flat["keyword"] = params.keyword;
    if (params.startDate !== undefined) flat["startDate"] = params.startDate;
    if (params.endDate !== undefined) flat["endDate"] = params.endDate;
    if (params.yearMonth !== undefined) flat["yearMonth"] = params.yearMonth;
    if (params.minAmount !== undefined) flat["minAmount"] = params.minAmount;
    if (params.maxAmount !== undefined) flat["maxAmount"] = params.maxAmount;
    return HttpRequest.get("/api/bill/search", flat);
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

/** 批量同步单项结果 */
export interface SyncBatchItem {
  uuid: string;
  status: "success" | "duplicate" | "fail";
  billId?: number;
  message: string;
}

/** 批量同步结果 */
export interface SyncBatchResult {
  total: number;
  successCount: number;
  failCount: number;
  duplicateCount: number;
  results: SyncBatchItem[];
}

/** 账单搜索参数 */
export interface BillSearchParams {
  page?: number;
  size?: number;
  type?: string;
  categoryId?: number;
  keyword?: string;
  startDate?: string;
  endDate?: string;
  yearMonth?: string;
  minAmount?: number;
  maxAmount?: number;
}

/** 分页查询参数 */
interface BillPageParams {
  page: number;
  size: number;
}
