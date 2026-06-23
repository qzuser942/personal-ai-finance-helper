package com.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 存钱目标表 save_target
 *
 * @author 胡宪棋
 */
@Data
@TableName("save_target")
public class SaveTarget {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户ID */
    private Long userId;

    /** 目标名称 */
    private String name;

    /** 目标总金额 */
    private BigDecimal targetAmount;

    /** 已存金额 */
    private BigDecimal savedAmount;

    /** 状态：0-进行中，1-已完成 */
    private Integer status;

    /** 完成时间 */
    private LocalDateTime completedAt;

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
