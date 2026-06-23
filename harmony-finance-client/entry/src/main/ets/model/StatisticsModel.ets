/**
 * 统计数据模型
 * @author 胡宪棋 软件2413 202421332084
 */

/** 月度统计 */
export interface MonthlyStatistics {
  yearMonth: string
  totalIncome: number
  totalExpense: number
  balance: number
  billCount: number
  categoryBreakdown: CategoryBreakdown[]
  dailyBreakdown: DailyBreakdown[]
}

/** 分类收支明细 */
export interface CategoryBreakdown {
  categoryId: number
  categoryName: string
  icon: string
  totalAmount: number
  percentage: number
  count: number
}

/** 每日收支明细 */
export interface DailyBreakdown {
  date: string         // yyyy-MM-dd
  dayOfWeek: string
  income: number
  expense: number
}
