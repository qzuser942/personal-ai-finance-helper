package com.finance.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 月度财务概况
 *
 * @author 胡宪棋
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinanceOverview {

    /** 月度总收入 */
    @JsonProperty("totalIncome")
    private BigDecimal totalIncome;

    /** 月度总支出 */
    @JsonProperty("totalExpense")
    private BigDecimal totalExpense;

    /** 月度结余 */
    @JsonProperty("balance")
    private BigDecimal balance;

    /** 财务健康评分（0-100） */
    @JsonProperty("healthScore")
    private Integer healthScore;

    /** 收支概况总结文案 */
    @JsonProperty("summary")
    private String summary;
}
