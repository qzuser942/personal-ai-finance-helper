package com.finance.ai.prompt;

import com.finance.mapper.AiConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prompt模板管理器
 *
 * 加载优先级：
 * 1. classpath:prompts/*.txt 文件（优先，方便开发调试）
 * 2. 数据库 ai_config 表（回退，支持管理员后台动态修改）
 * 3. 硬编码默认模板（兜底）
 *
 * 支持 {{variable}} 占位符变量替换
 *
 * @author 胡宪棋
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FinancePromptTemplates {

    private final ResourceLoader resourceLoader;
    private final AiConfigMapper aiConfigMapper;

    /** 模板缓存 */
    private final Map<String, String> templateCache = new ConcurrentHashMap<>();

    // 模板键常量
    public static final String KEY_DIAGNOSTIC = "prompt_template_analysis";
    public static final String KEY_CLASSIFY = "prompt_template_classify";
    public static final String KEY_FEATURE_EXTRACTION = "prompt_template_feature_extraction";

    /**
     * 获取月度诊断Prompt模板（填充变量后返回完整系统提示）
     *
     * @param variables 变量替换映射
     * @return 完整的系统提示词
     */
    public String resolveDiagnosticPrompt(Map<String, String> variables) {
        String template = loadTemplate("prompts/diagnostic-report.txt", KEY_DIAGNOSTIC);
        return resolveVariables(template, variables);
    }

    /**
     * 获取消费分类推荐Prompt模板
     *
     * @param variables 变量替换映射
     * @return 完整的分类推荐提示词
     */
    public String resolveClassifyPrompt(Map<String, String> variables) {
        String template = loadTemplate("prompts/category-classify.txt", KEY_CLASSIFY);
        return resolveVariables(template, variables);
    }

    /**
     * 获取消费特征提取Prompt模板
     *
     * @param variables 变量替换映射
     * @return 完整的特征提取提示词
     */
    public String resolveFeatureExtractionPrompt(Map<String, String> variables) {
        String template = loadTemplate("prompts/feature-extraction.txt", KEY_FEATURE_EXTRACTION);
        return resolveVariables(template, variables);
    }

    /**
     * 加载模板 - 三级回退策略
     */
    private String loadTemplate(String classpathPath, String dbConfigKey) {
        // 1. 从classpath加载
        String template = loadFromClasspath(classpathPath);
        if (template != null) {
            return template;
        }

        // 2. 从数据库 ai_config 表加载
        template = loadFromDatabase(dbConfigKey);
        if (template != null) {
            return template;
        }

        // 3. 返回默认模板
        log.warn("模板 {} 无法从classpath和DB加载，使用默认模板", classpathPath);
        return getDefaultTemplate(dbConfigKey);
    }

    /**
     * 从classpath加载模板文件
     */
    private String loadFromClasspath(String classpathPath) {
        // 检查缓存
        if (templateCache.containsKey(classpathPath)) {
            return templateCache.get(classpathPath);
        }

        try {
            org.springframework.core.io.Resource resource =
                    resourceLoader.getResource("classpath:" + classpathPath);
            if (resource.exists()) {
                String content = resource.getContentAsString(StandardCharsets.UTF_8);
                templateCache.put(classpathPath, content);
                log.debug("从classpath加载Prompt模板: {}", classpathPath);
                return content;
            }
        } catch (IOException e) {
            log.debug("无法从classpath加载模板 {}: {}", classpathPath, e.getMessage());
        }
        return null;
    }

    /**
     * 从数据库 ai_config 表加载模板
     */
    private String loadFromDatabase(String configKey) {
        String cacheKey = "db:" + configKey;
        if (templateCache.containsKey(cacheKey)) {
            return templateCache.get(cacheKey);
        }
        try {
            String value = aiConfigMapper.findValueByKey(configKey);
            if (value != null && !value.isBlank()) {
                templateCache.put(cacheKey, value);
                log.debug("从数据库加载Prompt模板: configKey={}", configKey);
                return value;
            }
        } catch (Exception e) {
            log.debug("无法从数据库加载模板 configKey={}: {}", configKey, e.getMessage());
        }
        return null;
    }

    /**
     * 变量替换 {{variable}} → value
     */
    private String resolveVariables(String template, Map<String, String> variables) {
        if (variables == null || variables.isEmpty()) {
            return template;
        }
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue() : "";
            result = result.replace(placeholder, value);
        }
        return result;
    }

    /**
     * 默认Prompt模板（兜底）
     */
    private String getDefaultTemplate(String configKey) {
        return switch (configKey) {
            case KEY_DIAGNOSTIC -> """
                    你是一名专业的个人理财顾问。
                    请基于以下用户的月度账单数据进行全面的财务诊断分析，并以JSON格式返回结果。

                    {{markdown_bill_data}}

                    返回JSON格式：
                    {
                      "overview": {"totalIncome": 0, "totalExpense": 0, "balance": 0, "healthScore": 0, "summary": ""},
                      "wasteItems": [],
                      "badHabits": [],
                      "suggestions": [],
                      "nextMonthPlan": {"totalBudget": 0, "categoryAllocations": {}, "tips": []}
                    }
                    """;
            case KEY_CLASSIFY -> """
                    你是一个消费分类助手。根据消费备注判断最可能属于哪个分类。

                    {{markdown_classify_data}}

                    返回JSON格式：
                    {"categoryName": "", "confidence": 0.0, "reason": "", "top3Alternatives": []}
                    """;
            case KEY_FEATURE_EXTRACTION -> """
                    从以下AI诊断报告中提取用户消费行为特征。
                    {{diagnosis_summary}}
                    返回2-3句简洁的特征描述文本。
                    """;
            default -> "请进行分析并返回JSON格式结果。";
        };
    }

    /**
     * 清空模板缓存（用于管理员修改DB配置后重新加载）
     */
    public void clearCache() {
        templateCache.clear();
        log.info("Prompt模板缓存已清空");
    }
}
