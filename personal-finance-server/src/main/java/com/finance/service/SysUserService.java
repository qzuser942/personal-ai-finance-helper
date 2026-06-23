package com.finance.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.finance.entity.SysUser;

/**
 * 普通用户 Service
 */
public interface SysUserService extends IService<SysUser> {

    /** 用户注册 */
    SysUser register(String username, String password, String confirmPassword);

    /** 用户登录 */
    String login(String username, String password);

    /** 修改密码 */
    void changePassword(Long userId, String oldPassword, String newPassword, String confirmPassword);

    /** 修改用户信息 */
    SysUser updateProfile(Long userId, String username);

    /** 重置密码（管理员） */
    String resetPassword(Long userId);

    /** 冻结/解冻用户 */
    void updateStatus(Long userId, Integer status);
}
