package com.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 账单表 bill
 *
 * @author 胡宪棋
 */
@Data
@TableName("bill")
public class Bill {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户ID */
    private Long userId;

    /** 金额（正数） */
    private BigDecimal amount;

    /** 收支类型：income / expense */
    private String type;

    /** 消费分类ID */
    private Long categoryId;

    /** 文字备注 */
    private String remark;

    /** 小票图片路径 */
    private String receiptImage;

    /** 离线同步UUID（去重用） */
    private String syncUuid;

    /** 消费时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime consumeTime;

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