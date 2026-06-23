package com.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.finance.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 普通用户 Mapper
 *
 * @author 胡宪棋
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("SELECT * FROM sys_user WHERE username = #{username} AND is_deleted = 0")
    SysUser findByUsername(@Param("username") String username);

    @Update("UPDATE sys_user SET last_login_time = NOW() WHERE id = #{id}")
    int updateLastLoginTime(@Param("id") Long id);
}
