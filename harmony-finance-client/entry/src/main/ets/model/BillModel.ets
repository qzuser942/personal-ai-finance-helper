/**
 * 账单相关数据模型
 * @author 胡宪棋 软件2413 202421332084
 */

/** 新增账单请求 */
export interface BillAddRequest {
  amount: number
  type: string          // 'income' | 'expense'
  categoryId: number
  remark?: string
  receiptImage?: string
  consumeTime: string   // yyyy-MM-dd HH:mm:ss
}

/** 账单列表项（分页查询返回） */
export interface BillListItem {
  id: number
  amount: number
  type: string
  categoryId: number
  categoryName?: string
  remark?: string
  hasImage: boolean
  consumeTime: string
}

/** 账单详情 */
export interface BillDetail {
  id: number
  amount: number
  type: string
  categoryId: number
  categoryName: string
  remark?: string
  receiptImage?: string
  consumeTime: string
  createdAt: string
  updatedAt?: string
}

/** 离线账单（Preferences存储） */
export interface OfflineBill {
  uuid: string
  amount: number
  type: string
  categoryId: number
  categoryName: string
  remark: string
  consumeTime: string
  createdAt: string
  offlineCreatedAt: string
  syncStatus: 'pending' | 'syncing' | 'synced'
  hasImage: boolean
}

/** 草稿账单 */
export interface DraftBill {
  amount: string
  type: string
  categoryId: number
  categoryName: string
  remark: string
  consumeTime: string
}
