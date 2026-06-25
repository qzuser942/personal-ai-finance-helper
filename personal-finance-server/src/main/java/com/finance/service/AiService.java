package com.finance.service;

import com.finance.ai.dto.CategoryRecommendation;
import com.finance.ai.dto.FinanceDiagnosisReport;

import java.util.Map;

/**
 * AI智能分析 Service 接口
 *
 * @author 胡宪棋
 */
public interface AiService {

    /**
     * AI月度财务诊断分析
     *
     * @param userId    用户ID
     * @param yearMonth 分析月份（格式：YYYY-MM）
     * @return 结构化财务诊断报告
     */
    FinanceDiagnosisReport analyzeMonthly(Long userId, String yearMonth);

    /**
     * AI智能消费分类推荐
     *
     * @param remark 消费备注文字
     * @param type   收支类型（income/expense）
     * @param userId 用户ID
     * @return 分类推荐结果（Top3备选）
     */
    CategoryRecommendation classifyRemark(String remark, String type, Long userId);

    /**
     * 获取用户AI分析历史列表
     */
    Map<String, Object> getHistory(Long userId, Integer page, Integer size);

    /**
     * 获取AI分析记录详情
     */
    Map<String, Object> getHistoryDetail(Long recordId, Long userId);

    /**
     * 管理员获取全平台AI分析记录
     */
    Map<String, Object> adminGetRecords(Integer page, Integer size, String username, String startDate, String endDate);

    /**
     * 管理员获取AI分析详情
     */
    Map<String, Object> adminGetRecordDetail(Long recordId);

    /**
     * 重置用户向量记忆数据（管理员功能）
     *
     * @param userId 用户ID
     */
    void resetUserVector(Long userId);
}