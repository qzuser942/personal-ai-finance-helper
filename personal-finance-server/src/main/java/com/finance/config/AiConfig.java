package com.finance.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * AI配置 - DeepSeek大模型参数
 * 所有配置项均可在 application.yml 中修改，支持环境变量覆盖
 *
 * @author 胡宪棋
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ai.deepseek")
public class AiConfig {

    /** DeepSeek官方API Key（环境变量 DEEPSEEK_API_KEY） */
    private String apiKey;

    /** DeepSeek官方API地址（OpenAI兼容接口） */
    private String baseUrl = "https://api.deepseek.com/v1";

    /** 模型名称：deepseek-chat / deepseek-reasoner */
    private String modelName = "deepseek-chat";

    /** 温度参数（0-2），控制输出随机性，越高越随机 */
    private Double temperature = 0.7;

    /** 最大输出Token数 */
    private Integer maxTokens = 4096;

    /** 核采样参数（0-1），控制输出多样性 */
    private Double topP = 0.9;

    /** API调用超时时间 */
    private Duration timeout = Duration.ofSeconds(120);

    /** API调用失败最大重试次数 */
    private Integer maxRetries = 3;
}
