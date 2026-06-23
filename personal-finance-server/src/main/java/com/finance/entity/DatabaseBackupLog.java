package com.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据库备份日志表 database_backup_log
 *
 * @author 胡宪棋
 */
@Data
@TableName("database_backup_log")
public class DatabaseBackupLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 执行备份的管理员ID */
    private Long adminId;

    /** 备份文件名 */
    private String fileName;

    /** 备份文件完整路径 */
    private String filePath;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 状态：1-成功，0-失败 */
    private Integer status;

    /** 失败错误信息 */
    private String errorMsg;

    /** 逻辑删除 */
    @TableLogic
    private Integer isDeleted;

    /** 备份时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
