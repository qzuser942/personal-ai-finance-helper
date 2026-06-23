package com.finance.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云百炼 DashScope 文本嵌入配置
 *
 * @author 胡宪棋
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ai.alicloud")
public class AliCloudEmbeddingConfig {

    /** 阿里云百炼 DashScope API Key（环境变量 DASHSCOPE_API_KEY） */
    private String dashscopeApiKey;

    /** 嵌入模型名称 */
    private String embeddingModel = "text-embedding-v2";

    /** 向量维度（text-embedding-v2 = 1536） */
    private int embeddingDimension = 1536;

    /** DashScope 文本嵌入 API 端点 */
    private String embeddingBaseUrl = "https://dashscope.aliyuncs.com/api/v1/services/embeddings/text-embedding/text-embedding";
}
