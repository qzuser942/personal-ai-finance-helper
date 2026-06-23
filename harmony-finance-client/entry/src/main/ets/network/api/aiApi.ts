/**
 * AI智能理财模块API
 * @author 胡宪棋 软件2413 202421332084
 */
import { HttpRequest, ApiResponse, PageResult } from '../request';
import { AiAnalyzeRequest, AiAnalysisResult, AiClassifyRequest, AiClassifyResult, AiHistoryItem } from '../../model/AiModel';

export class AiApi {
  /** AI月度账单分析 POST /api/ai/analyze */
  static async analyze(data: AiAnalyzeRequest): Promise<ApiResponse<AiAnalysisResult>> {
    return HttpRequest.post('/api/ai/analyze', data);
  }

  /** AI智能分类推荐 POST /api/ai/classify */
  static async classify(data: AiClassifyRequest): Promise<ApiResponse<AiClassifyResult>> {
    return HttpRequest.post('/api/ai/classify', data);
  }

  /** 获取个人AI分析历史 GET /api/ai/history */
  static async getHistory(page: number = 1, size: number = 10): Promise<ApiResponse<PageResult<AiHistoryItem>>> {
    return HttpRequest.get('/api/ai/history', { page, size });
  }

  /** 获取AI分析记录详情 GET /api/ai/history/{id} */
  static async getHistoryDetail(id: number): Promise<ApiResponse<AiAnalysisResult>> {
    return HttpRequest.get(`/api/ai/history/${id}`);
  }
}
