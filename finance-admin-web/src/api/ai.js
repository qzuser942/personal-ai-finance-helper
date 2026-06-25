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

/** 测试AI连接 POST /api/admin/ai/config/test */
export function testAiConnection() {
  return post('/api/admin/ai/config/test')
}

/** 重置AI模型参数为默认值 POST /api/admin/ai/config/reset */
export function resetAiConfig() {
  return post('/api/admin/ai/config/reset')
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

/** 获取Prompt模板默认值 GET /api/admin/ai/prompt/template?key=xxx */
export function getPromptTemplateDefault(key) {
  return get('/api/admin/ai/prompt/template', { key })
}

/** 获取Prompt预览真实数据 GET /api/admin/ai/prompt/preview-data?key=xxx */
export function getPromptPreviewData(key) {
  return get('/api/admin/ai/prompt/preview-data', { key })
}

/** 提取模板变量列表 GET /api/admin/ai/prompt/variables?key=xxx */
export function getPromptVariables(key) {
  return get('/api/admin/ai/prompt/variables', { key })
}