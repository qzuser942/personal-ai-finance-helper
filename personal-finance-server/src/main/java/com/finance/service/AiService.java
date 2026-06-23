package com.finance.service;

import java.util.Map;

/**
 * AI智能分析 Service
 */
public interface AiService {

    /** AI月度账单分析 */
    Map<String, Object> analyzeMonthly(Long userId, String yearMonth);

    /** AI智能分类推荐 */
    Map<String, Object> classifyRemark(String remark, String type, Long userId);

    /** 获取用户AI分析历史 */
    Map<String, Object> getHistory(Long userId, Integer page, Integer size);

    /** 获取AI分析详情 */
    Map<String, Object> getHistoryDetail(Long recordId, Long userId);

    /** 管理员获取全平台AI记录 */
    Map<String, Object> adminGetRecords(Integer page, Integer size, String username, String yearMonth);

    /** 管理员获取AI详情 */
    Map<String, Object> adminGetRecordDetail(Long recordId);

    /** 重置用户向量数据 */
    void resetUserVector(Long userId);
}
