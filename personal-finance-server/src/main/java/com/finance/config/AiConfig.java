package com.finance.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AI配置 - DeepSeek大模型参数
 *
 * @author 胡宪棋
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ai.deepseek")
public class AiConfig {

    /** 大模型API地址 */
    private String baseUrl = "http://localhost:11434";

    /** 模型名称 */
    private String modelName = "deepseek-chat";

    /** 温度参数 */
    private Double temperature = 0.7;

    /** 最大输出Token */
    private Integer maxTokens = 2048;

    /** 核采样参数 */
    private Double topP = 0.9;
}
