package com.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.finance.entity.SysAdmin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 管理员 Mapper
 *
 * @author 胡宪棋
 */
@Mapper
public interface SysAdminMapper extends BaseMapper<SysAdmin> {

    @Select("SELECT * FROM sys_admin WHERE username = #{username} AND is_deleted = 0")
    SysAdmin findByUsername(@Param("username") String username);

    @Update("UPDATE sys_admin SET last_login_time = NOW() WHERE id = #{id}")
    int updateLastLoginTime(@Param("id") Long id);
}
