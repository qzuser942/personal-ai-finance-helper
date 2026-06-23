package com.finance.ai.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.ai.exception.AiServiceException;
import com.finance.config.AiConfig;
import com.finance.exception.ErrorCode;
import dev.langchain4j.model.openai.OpenAiChatModel;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

/**
 * DeepSeek大模型客户端（单例Bean）
 * 通过LangChain4j OpenAiChatModel调用DeepSeek官方API（OpenAI兼容接口）
 *
 * 核心优化：单例复用HTTP连接池，避免每次调用重新创建模型实例
 *
 * @author 胡宪棋
 */
@Slf4j
public class DeepSeekChatClient {

    private final OpenAiChatModel chatModel;
    private final AiConfig aiConfig;
    private final ObjectMapper objectMapper;

    public DeepSeekChatClient(AiConfig aiConfig) {
        this.aiConfig = aiConfig;
        this.objectMapper = new ObjectMapper();

        this.chatModel = OpenAiChatModel.builder()
                .baseUrl(aiConfig.getBaseUrl())
                .apiKey(aiConfig.getApiKey())
                .modelName(aiConfig.getModelName())
                .temperature(aiConfig.getTemperature())
                .maxTokens(aiConfig.getMaxTokens())
                .topP(aiConfig.getTopP())
                .timeout(aiConfig.getTimeout() != null ? aiConfig.getTimeout() : Duration.ofSeconds(120))
                .build();

        log.info("DeepSeekChatClient 初始化完成: model={}, baseUrl={}, timeout={}, maxRetries={}",
                aiConfig.getModelName(), aiConfig.getBaseUrl(),
                aiConfig.getTimeout(), aiConfig.getMaxRetries());
    }

    /**
     * 发送聊天请求（带重试机制）
     *
     * @param systemPrompt 系统提示词
     * @param userMessage  用户消息（Markdown结构化账单数据）
     * @return AI原始响应文本
     */
    @Retry(name = "llmService")
    public String chat(String systemPrompt, String userMessage) {
        log.debug("调用DeepSeek: systemPrompt长度={}, userMessage长度={}",
                systemPrompt != null ? systemPrompt.length() : 0,
                userMessage != null ? userMessage.length() : 0);

        try {
            // 组装 ChatMessage（System + User）
            dev.langchain4j.data.message.SystemMessage sysMsg =
                    dev.langchain4j.data.message.SystemMessage.from(systemPrompt);
            dev.langchain4j.data.message.UserMessage usrMsg =
                    dev.langchain4j.data.message.UserMessage.from(userMessage);

            String response = chatModel.generate(sysMsg, usrMsg).content().text();
            log.debug("DeepSeek响应长度: {}", response != null ? response.length() : 0);
            return response;
        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("DeepSeek API调用失败: {}", e.getMessage());
            throw new AiServiceException(ErrorCode.AI_SERVICE_ERROR, e);
        }
    }

    /**
     * 发送聊天请求并解析为强类型Java对象（结构化输出）
     *
     * @param systemPrompt 系统提示词（需包含JSON输出格式说明）
     * @param userMessage  用户消息（Markdown结构化账单数据）
     * @param responseType 期望的返回类型
     * @param <T>          泛型类型
     * @return 反序列化后的Java对象
     */
    @Retry(name = "llmService")
    public <T> T chatStructured(String systemPrompt, String userMessage, Class<T> responseType) {
        String rawResponse = chat(systemPrompt, userMessage);
        String json = extractJson(rawResponse);

        try {
            return objectMapper.readValue(json, responseType);
        } catch (JsonProcessingException e) {
            log.error("AI响应JSON解析失败: class={}, rawResponse={}", responseType.getSimpleName(), rawResponse);
            throw new AiServiceException(ErrorCode.AI_PARSE_ERROR,
                    "无法将AI响应解析为 " + responseType.getSimpleName());
        }
    }

    /**
     * 从AI原始响应中提取JSON字符串
     * 处理Markdown代码块包裹、前导文字等情况
     */
    private String extractJson(String rawResponse) {
        String text = rawResponse.trim();

        // 去除Markdown代码块标记 ```json ... ``` 或 ``` ... ```
        if (text.startsWith("```json")) {
            text = text.substring(7);
        } else if (text.startsWith("```")) {
            text = text.substring(3);
        }
        if (text.endsWith("```")) {
            text = text.substring(0, text.length() - 3);
        }
        text = text.trim();

        // 提取第一个完整的JSON对象
        int startIdx = text.indexOf('{');
        int endIdx = text.lastIndexOf('}');
        if (startIdx >= 0 && endIdx > startIdx) {
            text = text.substring(startIdx, endIdx + 1);
        }

        return text;
    }

    /**
     * 获取当前使用的模型名称
     */
    public String getModelName() {
        return aiConfig.getModelName();
    }
}
