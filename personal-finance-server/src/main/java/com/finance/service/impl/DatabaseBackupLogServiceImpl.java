package com.finance.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.finance.entity.DatabaseBackupLog;
import com.finance.mapper.DatabaseBackupLogMapper;
import com.finance.service.DatabaseBackupLogService;
import org.springframework.stereotype.Service;

@Service
public class DatabaseBackupLogServiceImpl extends ServiceImpl<DatabaseBackupLogMapper, DatabaseBackupLog> implements DatabaseBackupLogService {
}
