package com.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.finance.entity.AdminOperationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 管理员操作日志 Mapper
 *
 * @author 胡宪棋
 */
@Mapper
public interface AdminOperationLogMapper extends BaseMapper<AdminOperationLog> {
}
