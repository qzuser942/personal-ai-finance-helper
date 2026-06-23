package com.finance.ai.config;

import com.finance.ai.client.AliCloudEmbeddingClient;
import com.finance.ai.client.DeepSeekChatClient;
import com.finance.config.AiConfig;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI客户端Bean配置
 * 声明DeepSeekChatClient、AliCloudEmbeddingClient、QdrantClient等核心Bean
 *
 * @author 胡宪棋
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AiClientConfig {

    private final AiConfig aiConfig;
    private final AliCloudEmbeddingConfig aliCloudEmbeddingConfig;
    private final QdrantConfig qdrantConfig;

    /**
     * DeepSeek大模型客户端（单例，复用HTTP连接池）
     */
    @Bean
    public DeepSeekChatClient deepSeekChatClient() {
        log.info("初始化 DeepSeekChatClient: model={}, baseUrl={}", aiConfig.getModelName(), aiConfig.getBaseUrl());
        return new DeepSeekChatClient(aiConfig);
    }

    /**
     * 阿里云百炼文本嵌入客户端
     */
    @Bean
    public AliCloudEmbeddingClient aliCloudEmbeddingClient() {
        log.info("初始化 AliCloudEmbeddingClient: model={}, dimension={}",
                aliCloudEmbeddingConfig.getEmbeddingModel(), aliCloudEmbeddingConfig.getEmbeddingDimension());
        return new AliCloudEmbeddingClient(aliCloudEmbeddingConfig);
    }

    /**
     * Qdrant gRPC原生客户端（用于集合初始化、Schema管理）
     */
    @Bean
    public QdrantClient qdrantClient() {
        log.info("初始化 QdrantClient: host={}, port={}", qdrantConfig.getHost(), qdrantConfig.getPort());
        QdrantGrpcClient grpcClient = QdrantGrpcClient.newBuilder(
                        qdrantConfig.getHost(),
                        qdrantConfig.getPort(),
                        qdrantConfig.isUseTls())
                .build();
        return new QdrantClient(grpcClient);
    }
}
