package com.finance.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 备选分类（Top3推荐中的一项）
 *
 * @author 胡宪棋
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryAlternative {

    /** 分类ID */
    @JsonProperty("categoryId")
    private Long categoryId;

    /** 分类名称 */
    @JsonProperty("categoryName")
    private String categoryName;

    /** 置信度（0.0 - 1.0） */
    @JsonProperty("confidence")
    private Double confidence;
}
