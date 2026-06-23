package com.finance.ai.vector;

import com.finance.ai.client.AliCloudEmbeddingClient;
import com.finance.ai.config.QdrantConfig;
import com.finance.ai.exception.AiServiceException;
import com.finance.exception.ErrorCode;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections;
import io.qdrant.client.grpc.Points;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * 用户消费向量记忆库
 * 封装Qdrant向量数据库操作：集合管理、向量存储、相似检索、用户记忆清除
 *
 * 集合设计：
 * - 名称：user_consumption_vectors
 * - 维度：1536（匹配阿里云 text-embedding-v2）
 * - 距离度量：Cosine
 * - Payload字段：userId、featureType、createdAt
 *
 * @author 胡宪棋
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConsumptionVectorStore {

    private final QdrantConfig qdrantConfig;
    private final QdrantClient qdrantClient;
    private final AliCloudEmbeddingClient embeddingClient;

    private QdrantEmbeddingStore embeddingStore;

    /**
     * 服务启动时自动初始化Qdrant集合
     */
    @PostConstruct
    public void ensureCollectionExists() {
        try {
            String collectionName = qdrantConfig.getCollectionName();

            // 检查集合是否存在
            boolean exists = qdrantClient.collectionExistsAsync(collectionName).get();
            if (!exists) {
                log.info("Qdrant集合 {} 不存在，开始创建...", collectionName);

                int dim = qdrantConfig.getEmbeddingDimension() > 0
                        ? qdrantConfig.getEmbeddingDimension() : 1536;

                // 创建集合（Cosine距离）
                qdrantClient.createCollectionAsync(
                        collectionName,
                        Collections.VectorParams.newBuilder()
                                .setSize(dim)
                                .setDistance(Collections.Distance.Cosine)
                                .build()
                ).get();

                // 创建Payload索引（加速按userId过滤查询）
                qdrantClient.createPayloadIndexAsync(
                        collectionName,
                        "userId",
                        Collections.PayloadSchemaType.Integer,
                        null,   // payloadIndexParams
                        null,   // wait
                        null,   // ordering
                        null    // timeout
                ).get();

                log.info("Qdrant集合 {} 创建成功，维度={}，距离度量=Cosine", collectionName, dim);
            } else {
                log.info("Qdrant集合 {} 已存在，跳过创建", collectionName);
            }

            // 初始化LangChain4j QdrantEmbeddingStore
            this.embeddingStore = QdrantEmbeddingStore.builder()
                    .host(qdrantConfig.getHost())
                    .port(qdrantConfig.getPort())
                    .collectionName(collectionName)
                    .useTls(qdrantConfig.isUseTls())
                    .build();

        } catch (InterruptedException | ExecutionException e) {
            log.error("Qdrant集合初始化失败: {}", e.getMessage());
            log.warn("向量存储功能暂时不可用，AI分析将跳过个性化记忆功能");
        }
    }

    /**
     * 存储用户消费特征向量
     */
    public void storeUserFeature(Long userId, String featureText, Map<String, Object> metadata) {
        if (embeddingStore == null) {
            log.warn("QdrantEmbeddingStore未初始化，跳过向量存储: userId={}", userId);
            return;
        }
        if (featureText == null || featureText.isBlank()) {
            log.debug("特征文本为空，跳过向量存储: userId={}", userId);
            return;
        }

        try {
            // 使用阿里云百炼生成文本嵌入向量
            Response<Embedding> embeddingResponse = embeddingClient.embed(featureText);
            Embedding embedding = embeddingResponse.content();

            // 构造LangChain4j Metadata
            Metadata langchainMetadata = new Metadata();
            langchainMetadata.put("userId", userId.toString());
            langchainMetadata.put("createdAt", java.time.LocalDateTime.now().toString());
            langchainMetadata.put("featureText", featureText);
            if (metadata != null) {
                metadata.forEach((k, v) -> langchainMetadata.put(k, v != null ? v.toString() : ""));
            }

            // 创建TextSegment并存储
            TextSegment segment = TextSegment.from(featureText, langchainMetadata);
            embeddingStore.add(embedding, segment);

            log.debug("用户消费特征已存入Qdrant: userId={}, featureLen={}", userId, featureText.length());

        } catch (Exception e) {
            log.error("存储用户消费向量失败: userId={}, error={}", userId, e.getMessage());
            throw new AiServiceException(ErrorCode.AI_VECTOR_STORE_ERROR,
                    "存储向量失败：" + e.getMessage(), true);
        }
    }

    /**
     * 检索用户历史消费特征（相似度搜索）
     */
    public List<String> searchSimilar(Long userId, String queryText, int topK) {
        if (embeddingStore == null) {
            log.warn("QdrantEmbeddingStore未初始化，返回空检索结果");
            return List.of();
        }

        try {
            Response<Embedding> queryEmbedding = embeddingClient.embed(queryText);

            EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding.content())
                    .maxResults(topK)
                    .minScore(0.5)
                    .filter(new IsEqualTo("userId", userId.toString()))
                    .build();

            EmbeddingSearchResult<TextSegment> result = embeddingStore.search(searchRequest);

            return result.matches().stream()
                    .map(EmbeddingMatch::embedded)
                    .map(TextSegment::text)
                    .toList();

        } catch (Exception e) {
            log.error("检索用户消费向量失败: userId={}, error={}", userId, e.getMessage());
            return List.of();
        }
    }

    /**
     * 获取用户历史消费画像（聚合文本描述）
     */
    public String retrieveUserProfile(Long userId) {
        List<String> features = searchSimilar(userId,
                "消费习惯 支出模式 浪费 省钱", 5);

        if (features.isEmpty()) {
            log.debug("用户 {} 暂无历史消费特征数据", userId);
            return null;
        }

        StringBuilder profile = new StringBuilder();
        profile.append("### 历史消费行为特征（基于过往AI分析）\n\n");
        for (int i = 0; i < features.size(); i++) {
            profile.append("- ").append(features.get(i)).append("\n");
        }
        profile.append("\n> 请在分析时结合以上历史消费特征，评估用户是否有改善，并给出针对性建议。");

        return profile.toString();
    }

    /**
     * 从诊断报告中提取多条特征并存储
     */
    public void storeFeaturesFromReport(Long userId, Map<String, Object> report, String yearMonth) {
        if (report == null || report.isEmpty()) {
            return;
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("yearMonth", yearMonth);
        metadata.put("featureType", "diagnosis_feedback");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> wasteItems = (List<Map<String, Object>>) report.get("wasteItems");
        if (wasteItems != null && !wasteItems.isEmpty()) {
            for (int i = 0; i < Math.min(wasteItems.size(), 3); i++) {
                Map<String, Object> item = wasteItems.get(i);
                String featureText = String.format("冗余消费：%s（分类：%s，金额：%s），原因：%s",
                        item.get("name"), item.get("category"),
                        item.get("amount"), item.get("reason"));
                storeUserFeature(userId, featureText, metadata);
            }
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> badHabits = (List<Map<String, Object>>) report.get("badHabits");
        if (badHabits != null && !badHabits.isEmpty()) {
            for (int i = 0; i < Math.min(badHabits.size(), 2); i++) {
                Map<String, Object> habit = badHabits.get(i);
                String featureText = String.format("不良消费习惯：%s，影响：%s",
                        habit.get("habit"), habit.get("impact"));
                storeUserFeature(userId, featureText, metadata);
            }
        }
    }

    /**
     * 清空指定用户的所有向量记忆（管理员功能）
     */
    public void deleteUserVectors(Long userId) {
        if (embeddingStore == null) {
            log.warn("QdrantEmbeddingStore未初始化，跳过向量删除: userId={}", userId);
            return;
        }

        try {
            Points.Filter filter = Points.Filter.newBuilder()
                    .addMust(Points.Condition.newBuilder()
                            .setField(Points.FieldCondition.newBuilder()
                                    .setKey("userId")
                                    .setMatch(Points.Match.newBuilder()
                                            .setInteger(userId)
                                            .build())
                                    .build())
                            .build())
                    .build();

            qdrantClient.deleteAsync(qdrantConfig.getCollectionName(), filter).get();
            log.info("已清空用户 {} 的向量记忆数据", userId);

        } catch (Exception e) {
            log.error("清空用户向量记忆失败: userId={}, error={}", userId, e.getMessage());
            throw new AiServiceException(ErrorCode.AI_VECTOR_STORE_ERROR,
                    "清空向量失败：" + e.getMessage(), true);
        }
    }

    /**
     * 获取向量存储是否可用
     */
    public boolean isAvailable() {
        return embeddingStore != null;
    }

    /**
     * 获取嵌入维度
     */
    public int getEmbeddingDimension() {
        return embeddingClient.dimension();
    }
}
