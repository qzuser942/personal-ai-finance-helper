package com.finance.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI月度财务诊断完整报告（顶层DTO）
 * DeepSeek大模型结构化输出对应此POJO
 *
 * @author 胡宪棋
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinanceDiagnosisReport {

    /** 月度财务概况 */
    @JsonProperty("overview")
    private FinanceOverview overview;

    /** 冗余/不必要消费项（最多5项） */
    @JsonProperty("wasteItems")
    private List<WasteItem> wasteItems;

    /** 不良消费习惯（2-3项） */
    @JsonProperty("badHabits")
    private List<BadHabit> badHabits;

    /** 个性化省钱建议（3-5条） */
    @JsonProperty("suggestions")
    private List<Suggestion> suggestions;

    /** 下月消费优化方案 */
    @JsonProperty("nextMonthPlan")
    private NextMonthPlan nextMonthPlan;

    // ==================== 运行时填充字段（非LLM输出） ====================

    /** 分析记录ID（保存DB后回填） */
    @JsonProperty("recordId")
    private Long recordId;

    /** 分析月份（YYYY-MM） */
    @JsonProperty("yearMonth")
    private String yearMonth;

    /** AI处理耗时（毫秒） */
    @JsonProperty("processingTimeMs")
    private Long processingTimeMs;
}
