package com.finance.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 冗余/不必要消费项
 *
 * @author 胡宪棋
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WasteItem {

    /** 消费项目名称 */
    @JsonProperty("name")
    private String name;

    /** 消费金额 */
    @JsonProperty("amount")
    private BigDecimal amount;

    /** 关联消费分类 */
    @JsonProperty("category")
    private String category;

    /** 判定为冗余的原因 */
    @JsonProperty("reason")
    private String reason;

    /** 改进建议 */
    @JsonProperty("suggestion")
    private String suggestion;

    /** 严重程度：HIGH / MEDIUM / LOW */
    @JsonProperty("severity")
    private String severity;
}
