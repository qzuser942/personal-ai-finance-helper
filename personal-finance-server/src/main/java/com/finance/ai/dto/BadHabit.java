package com.finance.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 不良消费习惯
 *
 * @author 胡宪棋
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BadHabit {

    /** 习惯名称 */
    @JsonProperty("habit")
    private String habit;

    /** 具体描述 */
    @JsonProperty("description")
    private String description;

    /** 对财务的影响评估 */
    @JsonProperty("impact")
    private String impact;

    /** 严重程度：HIGH / MEDIUM / LOW */
    @JsonProperty("severity")
    private String severity;
}
