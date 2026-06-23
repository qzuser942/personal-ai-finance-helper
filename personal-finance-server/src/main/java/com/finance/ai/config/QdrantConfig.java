package com.finance.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Qdrant向量数据库配置
 *
 * @author 胡宪棋
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ai.qdrant")
public class QdrantConfig {

    /** Qdrant服务主机地址 */
    private String host = "localhost";

    /** Qdrant gRPC端口（默认6334） */
    private int port = 6334;

    /** 向量集合名称 */
    private String collectionName = "user_consumption_vectors";

    /** 是否启用TLS */
    private boolean useTls = false;

    /** 向量维度（默认1536，从AliCloudEmbeddingConfig继承） */
    private int embeddingDimension = 1536;
}
