package com.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 月度预算表 budget
 *
 * @author 胡宪棋
 */
@Data
@TableName("budget")
public class Budget {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户ID */
    private Long userId;

    /** 预算月份 YYYY-MM */
    private String yearMonth;

    /** 月度总预算 */
    private BigDecimal totalBudget;

    /** 分类子预算JSON：{"category_id": amount} */
    private String categoryBudgets;

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
