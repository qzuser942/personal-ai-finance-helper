/**
 * AI运营配置API
 * @author 胡宪棋 软件2413 202421332084
 */
import { get, put, post } from './request'

/** 获取AI全部配置 GET /api/admin/ai/config */
export function getAiConfig() {
  return get('/api/admin/ai/config')
}

/** 更新AI配置 PUT /api/admin/ai/config */
export function updateAiConfig(data) {
  return put('/api/admin/ai/config', data)
}

/** 全平台AI分析记录 GET /api/admin/ai/records */
export function getAiRecords(params) {
  return get('/api/admin/ai/records', params)
}

/** AI分析记录详情 GET /api/admin/ai/records/{id} */
export function getAiRecordDetail(id) {
  return get(`/api/admin/ai/records/${id}`)
}

/** 重置用户Qdrant向量 POST /api/admin/ai/qdrant/reset */
export function resetUserVector(userId) {
  return post('/api/admin/ai/qdrant/reset', { userId })
}
