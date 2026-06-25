package com.finance.ai.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.ai.exception.AiServiceException;
import com.finance.config.AiConfig;
import com.finance.exception.ErrorCode;
import com.finance.mapper.AiConfigMapper;
import dev.langchain4j.model.openai.OpenAiChatModel;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Objects;

/**
 * DeepSeek大模型客户端（单例Bean）
 * 通过LangChain4j OpenAiChatModel调用DeepSeek官方API（OpenAI兼容接口）
 *
 * <p>
 * 配置优先级：数据库 ai_config 表 > application.yml > 内置默认值
 * </p>
 * <ul>
 * <li>每次调用前从数据库动态读取配置，仅当DB配置变更时才重建 OpenAiChatModel</li>
 * <li>DB读取失败/配置为空时，自动回退到 application.yml 的YAML配置</li>
 * <li>使用 volatile + synchronized 保证线程安全的热更新</li>
 * </ul>
 *
 * @author 胡宪棋
 */
@Slf4j
public class DeepSeekChatClient {

    /** 当前使用的模型实例（volatile保证跨线程可见性，synchronized保证重建原子性） */
    private volatile OpenAiChatModel chatModel;

    /** 当前生效的配置快照（用于变更检测） */
    private volatile ResolvedConfig resolvedConfig;

    /** YAML兜底配置（Spring Boot注入，不可变） */
    private final AiConfig aiConfig;

    /** 数据库配置DAO（用于动态读取 ai_config 表） */
    private final AiConfigMapper aiConfigMapper;

    private final ObjectMapper objectMapper;

    /** 模型重建锁（保证并发安全） */
    private final Object configLock = new Object();

    // ==================== 数据库配置Key常量 ====================
    private static final String DB_KEY_API_KEY = "deepseek_api_key";
    private static final String DB_KEY_BASE_URL = "model_base_url";
    private static final String DB_KEY_MODEL_NAME = "model_name";
    private static final String DB_KEY_TEMPERATURE = "model_temperature";
    private static final String DB_KEY_MAX_TOKENS = "model_max_tokens";
    private static final String DB_KEY_TOP_P = "model_top_p";

    public DeepSeekChatClient(AiConfig aiConfig, AiConfigMapper aiConfigMapper) {
        this.aiConfig = aiConfig;
        this.aiConfigMapper = aiConfigMapper;
        this.objectMapper = new ObjectMapper();

        // 初始构建：优先尝试DB，失败则用YAML兜底
        this.resolvedConfig = resolveFromDb();
        if (this.resolvedConfig == null) {
            this.resolvedConfig = resolveFromYaml();
        }
        this.chatModel = buildModel(this.resolvedConfig);

        log.info("DeepSeekChatClient 初始化完成: model={}, baseUrl={}, temperature={}, maxTokens={}",
                resolvedConfig.modelName, resolvedConfig.baseUrl,
                resolvedConfig.temperature, resolvedConfig.maxTokens);
    }

    // ==================== 配置解析 ====================

    /**
     * 从数据库 ai_config 表读取配置，与YAML合并
     * <p>
     * 合并规则：DB有值且非空 → 使用DB值；否则 → 使用YAML值
     * </p>
     *
     * @return 合并后的配置，若DB完全不可用则返回null
     */
    private ResolvedConfig resolveFromDb() {
        try {
            String dbApiKey = aiConfigMapper.findValueByKey(DB_KEY_API_KEY);
            String dbBaseUrl = aiConfigMapper.findValueByKey(DB_KEY_BASE_URL);
            String dbModelName = aiConfigMapper.findValueByKey(DB_KEY_MODEL_NAME);
            String dbTemperature = aiConfigMapper.findValueByKey(DB_KEY_TEMPERATURE);
            String dbMaxTokens = aiConfigMapper.findValueByKey(DB_KEY_MAX_TOKENS);
            String dbTopP = aiConfigMapper.findValueByKey(DB_KEY_TOP_P);

            return new ResolvedConfig(
                    firstNonBlank(dbApiKey, aiConfig.getApiKey()),
                    firstNonBlank(dbBaseUrl, aiConfig.getBaseUrl()),
                    firstNonBlank(dbModelName, aiConfig.getModelName()),
                    firstNonBlankDouble(dbTemperature, aiConfig.getTemperature()),
                    firstNonBlankInt(dbMaxTokens, aiConfig.getMaxTokens()),
                    firstNonBlankDouble(dbTopP, aiConfig.getTopP()),
                    aiConfig.getTimeout() != null ? aiConfig.getTimeout() : Duration.ofSeconds(120));
        } catch (Exception e) {
            log.warn("从数据库读取AI配置失败，将使用YAML兜底配置: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从YAML配置构建（兜底方案）
     */
    private ResolvedConfig resolveFromYaml() {
        return new ResolvedConfig(
                aiConfig.getApiKey(),
                aiConfig.getBaseUrl(),
                aiConfig.getModelName(),
                aiConfig.getTemperature(),
                aiConfig.getMaxTokens(),
                aiConfig.getTopP(),
                aiConfig.getTimeout() != null ? aiConfig.getTimeout() : Duration.ofSeconds(120));
    }

    /**
     * 检测DB配置是否变更，若变更则热更新 OpenAiChatModel
     */
    private void refreshConfigIfNeeded() {
        ResolvedConfig dbConfig = resolveFromDb();
        if (dbConfig == null) {
            return; // DB不可用，继续使用当前配置
        }

        if (!dbConfig.equals(this.resolvedConfig)) {
            synchronized (configLock) {
                // 双重检查：可能已被其他线程刷新
                if (dbConfig.equals(this.resolvedConfig)) {
                    return;
                }
                ResolvedConfig old = this.resolvedConfig;
                this.resolvedConfig = dbConfig;
                this.chatModel = buildModel(dbConfig);
                log.info("DeepSeekChatClient 配置热更新: model={}→{}, baseUrl={}→{}, temperature={}→{}",
                        old.modelName, dbConfig.modelName,
                        old.baseUrl, dbConfig.baseUrl,
                        old.temperature, dbConfig.temperature);
            }
        }
    }

    /**
     * 根据解析后的配置构建 OpenAiChatModel 实例
     */
    private OpenAiChatModel buildModel(ResolvedConfig config) {
        return OpenAiChatModel.builder()
                .baseUrl(config.baseUrl)
                .apiKey(config.apiKey)
                .modelName(config.modelName)
                .temperature(config.temperature)
                .maxTokens(config.maxTokens)
                .topP(config.topP)
                .timeout(config.timeout)
                .build();
    }

    // ==================== 公共API ====================

    /**
     * 发送聊天请求（带重试机制）
     * <p>
     * 每次调用前自动检测DB配置变更并热更新模型
     * </p>
     *
     * @param systemPrompt 系统提示词
     * @param userMessage  用户消息（Markdown结构化账单数据）
     * @return AI原始响应文本
     */
    @Retry(name = "llmService")
    public String chat(String systemPrompt, String userMessage) {
        // 每次调用前检测DB配置变更（DB异常不影响主流程）
        refreshConfigIfNeeded();

        log.debug("调用DeepSeek: model={}, systemPrompt长度={}, userMessage长度={}",
                resolvedConfig.modelName,
                systemPrompt != null ? systemPrompt.length() : 0,
                userMessage != null ? userMessage.length() : 0);

        try {
            dev.langchain4j.data.message.SystemMessage sysMsg = dev.langchain4j.data.message.SystemMessage
                    .from(systemPrompt);
            dev.langchain4j.data.message.UserMessage usrMsg = dev.langchain4j.data.message.UserMessage
                    .from(userMessage);

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

        if (text.startsWith("```json")) {
            text = text.substring(7);
        } else if (text.startsWith("```")) {
            text = text.substring(3);
        }
        if (text.endsWith("```")) {
            text = text.substring(0, text.length() - 3);
        }
        text = text.trim();

        int startIdx = text.indexOf('{');
        int endIdx = text.lastIndexOf('}');
        if (startIdx >= 0 && endIdx > startIdx) {
            text = text.substring(startIdx, endIdx + 1);
        }

        return text;
    }

    /**
     * 获取当前生效的模型名称（DB优先，YAML兜底）
     */
    public String getModelName() {
        return resolvedConfig.modelName;
    }

    // ==================== 工具方法 ====================

    /**
     * DB值优先，但排除占位符模式（如 ${DEEPSEEK_API_KEY:xxx}），
     * 此时回退到YAML兜底值（YAML会通过Spring解析真实环境变量）
     */
    private static String firstNonBlank(String dbValue, String fallback) {
        if (dbValue != null && !dbValue.isBlank() && !dbValue.startsWith("${")) {
            return dbValue;
        }
        return fallback;
    }

    private static Double firstNonBlankDouble(String dbValue, Double fallback) {
        if (dbValue != null && !dbValue.isBlank()) {
            try {
                return Double.parseDouble(dbValue.trim());
            } catch (NumberFormatException e) {
                log.warn("DB配置值无法解析为Double: {}, 使用YAML兜底值: {}", dbValue, fallback);
            }
        }
        return fallback;
    }

    private static Integer firstNonBlankInt(String dbValue, Integer fallback) {
        if (dbValue != null && !dbValue.isBlank()) {
            try {
                return Integer.parseInt(dbValue.trim());
            } catch (NumberFormatException e) {
                log.warn("DB配置值无法解析为Integer: {}, 使用YAML兜底值: {}", dbValue, fallback);
            }
        }
        return fallback;
    }

    // ==================== 内部类：配置快照 ====================

    /**
     * 解析后的AI配置快照（不可变）
     * 用于变更检测：每次从DB读取后与当前快照比较，仅当不一致时才重建模型
     */
    private static class ResolvedConfig {
        final String apiKey;
        final String baseUrl;
        final String modelName;
        final Double temperature;
        final Integer maxTokens;
        final Double topP;
        final Duration timeout;

        ResolvedConfig(String apiKey, String baseUrl, String modelName,
                Double temperature, Integer maxTokens, Double topP, Duration timeout) {
            this.apiKey = apiKey;
            this.baseUrl = baseUrl;
            this.modelName = modelName;
            this.temperature = temperature;
            this.maxTokens = maxTokens;
            this.topP = topP;
            this.timeout = timeout;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof ResolvedConfig that))
                return false;
            return Objects.equals(apiKey, that.apiKey)
                    && Objects.equals(baseUrl, that.baseUrl)
                    && Objects.equals(modelName, that.modelName)
                    && Objects.equals(temperature, that.temperature)
                    && Objects.equals(maxTokens, that.maxTokens)
                    && Objects.equals(topP, that.topP);
        }

        @Override
        public int hashCode() {
            return Objects.hash(apiKey, baseUrl, modelName, temperature, maxTokens, topP);
        }
    }
}