/**
 * 统计模块API
 * @author 胡宪棋 软件2413 202421332084
 */
import { HttpRequest, ApiResponse } from '../request';
import { MonthlyStatistics } from '../../model/StatisticsModel';

/** 月度统计查询参数 */
interface MonthlyParams {
  yearMonth: string
}

export class StatisticsApi {
  /** 月度收支统计 GET /api/statistics/monthly */
  static async getMonthly(yearMonth: string): Promise<ApiResponse<MonthlyStatistics>> {
    const params: MonthlyParams = { yearMonth: yearMonth };
    return HttpRequest.get('/api/statistics/monthly', params);
  }
}
