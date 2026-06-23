/**
 * 预算相关数据模型
 * @author 胡宪棋 软件2413 202421332084
 */

/** 预算数据 */
export interface BudgetData {
  id: number
  yearMonth: string
  totalBudget: number
  totalSpent: number
  remaining: number
  usagePercent: number
  categoryBudgets: Record<string, number>
  categorySpent: Record<string, number>
  alertCategories: BudgetAlertCategory[]
}

/** 预算预警分类 */
export interface BudgetAlertCategory {
  categoryId: number
  categoryName: string
  budgetAmount: number
  spentAmount: number
  alertLevel: 'WARNING' | 'OVER'   // WARNING=≥80%  OVER=≥100%
}

/** 创建/覆盖预算请求 */
export interface BudgetCreateRequest {
  yearMonth: string
  totalBudget: number
  categoryBudgets?: Record<string, number>
}
