/**
 * AI分析相关数据模型
 * @author 胡宪棋 软件2413 202421332084
 */

/** AI分析请求 */
export interface AiAnalyzeRequest {
  yearMonth: string
}

/** AI分析结果 */
export interface AiAnalysisResult {
  recordId: number
  yearMonth: string
  redundantItems: RedundantItem[]
  badHabits: BadHabit[]
  savingPlans: SavingPlan[]
  monthlyReview: string
  processingTimeMs: number
}

/** 冗余消费项 */
export interface RedundantItem {
  name: string
  amount: number
  reason: string
  suggestion: string
}

/** 不良消费习惯 */
export interface BadHabit {
  habit: string
  description: string
  impact: string
}

/** 省钱方案 */
export interface SavingPlan {
  plan: string
  description: string
  estimatedSave: string
}

/** AI分类推荐请求 */
export interface AiClassifyRequest {
  remark: string
  type?: string
}

/** AI分类推荐结果 */
export interface AiClassifyResult {
  categoryId: number
  categoryName: string
  confidence: number
  reason: string
}

/** AI分析历史记录项 */
export interface AiHistoryItem {
  id: number
  yearMonth: string
  modelName: string
  processingTimeMs: number
  resultPreview: string
  createdAt: string
}
