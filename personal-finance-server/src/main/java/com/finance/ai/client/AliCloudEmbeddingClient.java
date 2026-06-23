package com.finance.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.ai.config.AliCloudEmbeddingConfig;
import com.finance.ai.exception.AiServiceException;
import com.finance.exception.ErrorCode;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 阿里云百炼 DashScope 文本嵌入客户端
 * 实现LangChain4j EmbeddingModel接口，对接阿里云 text-embedding-v2
 *
 * API文档：https://help.aliyun.com/zh/model-studio/text-embedding-api
 * 向量维度：text-embedding-v2 = 1536
 *
 * @author 胡宪棋
 */
@Slf4j
public class AliCloudEmbeddingClient implements EmbeddingModel {

    private final AliCloudEmbeddingConfig config;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final int dimension;

    public AliCloudEmbeddingClient(AliCloudEmbeddingConfig config) {
        this.config = config;
        this.dimension = config.getEmbeddingDimension();
        this.objectMapper = new ObjectMapper();

        this.restClient = RestClient.builder()
                .baseUrl(config.getEmbeddingBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getDashscopeApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        log.info("AliCloudEmbeddingClient 初始化完成: model={}, dimension={}, baseUrl={}",
                config.getEmbeddingModel(), dimension, config.getEmbeddingBaseUrl());
    }

    @Override
    public Response<Embedding> embed(String text) {
        if (text == null || text.isBlank()) {
            log.warn("嵌入文本为空，返回零向量");
            return Response.from(Embedding.from(new float[dimension]));
        }

        try {
            // 构造请求体
            Map<String, Object> requestBody = buildRequestBody(List.of(text));

            // 调用DashScope API
            String responseJson = restClient.post()
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            // 解析响应
            JsonNode root = objectMapper.readTree(responseJson);

            // 检查API错误
            if (root.has("code") && root.has("message")) {
                String errorCode = root.get("code").asText();
                String errorMsg = root.get("message").asText();
                log.error("DashScope API错误: code={}, message={}", errorCode, errorMsg);
                throw new AiServiceException(ErrorCode.AI_EMBEDDING_ERROR, errorMsg);
            }

            // 提取第一个文本的嵌入向量
            JsonNode embeddings = root.path("output").path("embeddings");
            if (embeddings.isArray() && embeddings.size() > 0) {
                JsonNode embeddingNode = embeddings.get(0).path("embedding");
                float[] vector = new float[embeddingNode.size()];
                for (int i = 0; i < embeddingNode.size(); i++) {
                    vector[i] = embeddingNode.get(i).floatValue();
                }
                return Response.from(Embedding.from(vector));
            }

            throw new AiServiceException(ErrorCode.AI_EMBEDDING_ERROR, "DashScope返回结果为空");
        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("阿里云百炼嵌入API调用失败: {}", e.getMessage());
            throw new AiServiceException(ErrorCode.AI_EMBEDDING_ERROR, e);
        }
    }

    @Override
    public Response<List<Embedding>> embedAll(List<TextSegment> textSegments) {
        if (textSegments == null || textSegments.isEmpty()) {
            log.warn("嵌入文本列表为空，返回空列表");
            return Response.from(List.of());
        }

        try {
            List<String> texts = textSegments.stream()
                    .map(TextSegment::text)
                    .toList();

            Map<String, Object> requestBody = buildRequestBody(texts);

            String responseJson = restClient.post()
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseJson);

            if (root.has("code") && root.has("message")) {
                String errorCode = root.get("code").asText();
                String errorMsg = root.get("message").asText();
                log.error("DashScope API错误: code={}, message={}", errorCode, errorMsg);
                throw new AiServiceException(ErrorCode.AI_EMBEDDING_ERROR, errorMsg);
            }

            JsonNode embeddings = root.path("output").path("embeddings");
            List<Embedding> result = new ArrayList<>();
            if (embeddings.isArray()) {
                for (JsonNode item : embeddings) {
                    JsonNode embeddingNode = item.path("embedding");
                    float[] vector = new float[embeddingNode.size()];
                    for (int i = 0; i < embeddingNode.size(); i++) {
                        vector[i] = embeddingNode.get(i).floatValue();
                    }
                    result.add(Embedding.from(vector));
                }
            }

            return Response.from(result);
        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("阿里云百炼批量嵌入API调用失败: {}", e.getMessage());
            throw new AiServiceException(ErrorCode.AI_EMBEDDING_ERROR, e);
        }
    }

    @Override
    public int dimension() {
        return dimension;
    }

    /**
     * 构造DashScope API请求体
     * 格式：{"model":"text-embedding-v2","input":{"texts":["text1","text2"]}}
     */
    private Map<String, Object> buildRequestBody(List<String> texts) {
        Map<String, Object> input = new HashMap<>();
        input.put("texts", texts);

        Map<String, Object> body = new HashMap<>();
        body.put("model", config.getEmbeddingModel());
        body.put("input", input);

        return body;
    }
}
