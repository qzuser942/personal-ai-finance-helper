package com.finance.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 下月消费优化方案
 *
 * @author 胡宪棋
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NextMonthPlan {

    /** 下月建议总预算 */
    @JsonProperty("totalBudget")
    private BigDecimal totalBudget;

    /** 分类预算分配：{ "餐饮": 1500.00, "交通": 500.00, ... } */
    @JsonProperty("categoryAllocations")
    private Map<String, BigDecimal> categoryAllocations;

    /** 优化小贴士 */
    @JsonProperty("tips")
    private List<String> tips;
}
