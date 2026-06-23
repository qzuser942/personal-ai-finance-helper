package com.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员表 sys_admin
 *
 * @author 胡宪棋
 */
@Data
@TableName("sys_admin")
public class SysAdmin {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 管理员账号（唯一） */
    private String username;

    /** BCrypt加密密码 */
    private String password;

    /** 角色：SUPER_ADMIN / OPERATOR */
    private String role;

    /** 最近登录时间 */
    private LocalDateTime lastLoginTime;

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
