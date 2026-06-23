package com.finance.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 个性化省钱建议
 *
 * @author 胡宪棋
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Suggestion {

    /** 方案名称 */
    @JsonProperty("plan")
    private String plan;

    /** 具体做法描述 */
    @JsonProperty("description")
    private String description;

    /** 预估每月节省金额 */
    @JsonProperty("estimatedMonthlySave")
    private String estimatedMonthlySave;

    /** 实施难度：EASY / MODERATE / CHALLENGING */
    @JsonProperty("difficulty")
    private String difficulty;
}
