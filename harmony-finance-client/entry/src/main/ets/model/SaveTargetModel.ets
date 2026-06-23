/**
 * 存钱目标数据模型
 * @author 胡宪棋 软件2413 202421332084
 */

/** 存钱目标 */
export interface SaveTarget {
  id: number
  name: string
  targetAmount: number
  savedAmount: number
  progressPercent: number
  status: number          // 0-进行中 1-已完成
  createdAt: string
  completedAt?: string
}

/** 创建目标请求 */
export interface SaveTargetCreateRequest {
  name: string
  targetAmount: number
}

/** 更新目标请求（追加存款） */
export interface SaveTargetUpdateRequest {
  name?: string
  targetAmount?: number
  addAmount?: number
}
