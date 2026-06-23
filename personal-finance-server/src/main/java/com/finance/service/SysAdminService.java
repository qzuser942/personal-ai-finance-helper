package com.finance.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.finance.entity.SysAdmin;

/**
 * 管理员 Service
 */
public interface SysAdminService extends IService<SysAdmin> {

    /** 管理员登录 */
    String login(String username, String password);

    /** 注册管理员 */
    SysAdmin register(String username, String password, String role);

    /** 重置管理员密码 */
    String resetPassword(Long adminId);
}
