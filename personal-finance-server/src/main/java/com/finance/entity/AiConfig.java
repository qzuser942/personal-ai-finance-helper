package com.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI配置表 ai_config
 *
 * @author 胡宪棋
 */
@Data
@TableName("ai_config")
public class AiConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 配置键名 */
    private String configKey;

    /** 配置值 */
    private String configValue;

    /** 值类型：STRING/NUMBER/JSON/TEXT */
    private String configType;

    /** 说明 */
    private String description;

    /** 逻辑删除 */
    @TableLogic
    private Integer isDeleted;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
