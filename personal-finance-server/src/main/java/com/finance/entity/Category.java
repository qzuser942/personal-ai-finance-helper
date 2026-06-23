package com.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消费分类表 category
 *
 * @author 胡宪棋
 */
@Data
@TableName("category")
public class Category {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户ID：0-系统内置，>0-用户自定义 */
    private Long userId;

    /** 分类名称 */
    private String name;

    /** 图标标识 */
    private String icon;

    /** 分类类型：income / expense */
    private String type;

    /** 排序序号 */
    private Integer sortOrder;

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
