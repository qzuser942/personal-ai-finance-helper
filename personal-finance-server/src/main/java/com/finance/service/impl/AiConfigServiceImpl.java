package com.finance.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.finance.entity.AiConfig;
import com.finance.mapper.AiConfigMapper;
import com.finance.service.AiConfigService;
import org.springframework.stereotype.Service;

@Service
public class AiConfigServiceImpl extends ServiceImpl<AiConfigMapper, AiConfig> implements AiConfigService {
}
