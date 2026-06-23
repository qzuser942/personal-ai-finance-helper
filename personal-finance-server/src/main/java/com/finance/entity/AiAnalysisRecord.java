package com.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI分析记录表 ai_analysis_record
 *
 * @author 胡宪棋
 */
@Data
@TableName("ai_analysis_record")
public class AiAnalysisRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户ID */
    private Long userId;

    /** 分析月份 YYYY-MM */
    private String yearMonth;

    /** AI分析结果JSON */
    private String resultJson;

    /** Prompt模板快照 */
    private String promptTemplateSnapshot;

    /** 模型名称 */
    private String modelName;

    /** 处理耗时（毫秒） */
    private Long processingTimeMs;

    /** 逻辑删除 */
    @TableLogic
    private Integer isDeleted;

    /** 分析时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
