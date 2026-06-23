package com.finance.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI智能分类推荐结果
 *
 * @author 胡宪棋
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRecommendation {

    /** 推荐分类ID（匹配到系统分类后填充） */
    @JsonProperty("categoryId")
    private Long categoryId;

    /** 推荐分类名称 */
    @JsonProperty("categoryName")
    private String categoryName;

    /** 置信度（0.0 - 1.0） */
    @JsonProperty("confidence")
    private Double confidence;

    /** 推荐理由 */
    @JsonProperty("reason")
    private String reason;

    /** Top3备选分类列表 */
    @JsonProperty("top3Alternatives")
    private List<CategoryAlternative> top3Alternatives;
}
