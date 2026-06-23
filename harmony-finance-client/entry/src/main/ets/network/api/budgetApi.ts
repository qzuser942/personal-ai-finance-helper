/**
 * 预算模块API
 * @author 胡宪棋 软件2413 202421332084
 */
import { HttpRequest, ApiResponse } from '../request';
import { BudgetData, BudgetCreateRequest } from '../../model/BudgetModel';

export class BudgetApi {
  /** 获取当月预算 GET /api/budget/current */
  static async getCurrent(): Promise<ApiResponse<BudgetData>> {
    return HttpRequest.get('/api/budget/current');
  }

  /** 获取指定月份预算 GET /api/budget/{yearMonth} */
  static async getByMonth(yearMonth: string): Promise<ApiResponse<BudgetData>> {
    return HttpRequest.get(`/api/budget/${yearMonth}`);
  }

  /** 创建/覆盖月度预算 POST /api/budget */
  static async createOrUpdate(data: BudgetCreateRequest): Promise<ApiResponse<BudgetData>> {
    return HttpRequest.post('/api/budget', data);
  }
}
