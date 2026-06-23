/**
 * 消费分类数据模型
 * @author 胡宪棋 软件2413 202421332084
 */

/** 消费分类 */
export interface Category {
  id: number
  name: string
  icon: string
  type: string          // 'income' | 'expense'
  sortOrder: number
  isSystem: boolean     // true=系统内置不可删除
}

/** 创建分类请求 */
export interface CategoryCreateRequest {
  name: string
  icon?: string
  type: string
  sortOrder?: number
}
