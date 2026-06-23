package com.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.finance.entity.DatabaseBackupLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 数据库备份日志 Mapper
 *
 * @author 胡宪棋
 */
@Mapper
public interface DatabaseBackupLogMapper extends BaseMapper<DatabaseBackupLog> {
}
