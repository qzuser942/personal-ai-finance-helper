package com.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员操作日志表 admin_operation_log
 *
 * @author 胡宪棋
 */
@Data
@TableName("admin_operation_log")
public class AdminOperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作管理员ID */
    private Long adminId;

    /** 操作管理员账号（冗余） */
    private String adminUsername;

    /** 操作管理员角色：SUPER_ADMIN / OPERATOR */
    private String adminRole;

    /** 操作类型描述 */
    private String operation;

    /** HTTP方法 */
    private String method;

    /** 请求路径 */
    private String requestUrl;

    /** 关联资源ID（如userId、billId），用于追踪"谁动了什么数据" */
    private String resourceId;

    /** 请求参数JSON */
    private String requestParams;

    /** 操作IP */
    private String ipAddress;

    /** 操作结果：1-成功，0-失败 */
    private Integer status;

    /** 失败错误信息 */
    private String errorMsg;

    /** 逻辑删除 */
    @TableLogic
    private Integer isDeleted;

    /** 操作时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}