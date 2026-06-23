package com.finance.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.finance.entity.AdminOperationLog;
import com.finance.mapper.AdminOperationLogMapper;
import com.finance.service.AdminOperationLogService;
import org.springframework.stereotype.Service;

@Service
public class AdminOperationLogServiceImpl extends ServiceImpl<AdminOperationLogMapper, AdminOperationLog> implements AdminOperationLogService {
}
