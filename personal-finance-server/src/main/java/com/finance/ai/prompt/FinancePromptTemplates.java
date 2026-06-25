package com.finance.ai.prompt;

import com.finance.mapper.AiConfigMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Prompt模板管理器
 *
 * <p>
 * 加载优先级（由高到低）：
 * </p>
 * <ol>
 * <li>数据库 ai_config 表 — 管理员后台动态修改，即时生效，每次读取（无缓存）</li>
 * <li>classpath:prompts/*.txt 文件 — 开发阶段默认模板，启动时缓存</li>
 * <li>硬编码默认模板 — 最终兜底</li>
 * </ol>
 *
 * <p>
 * 支持 {{variable}} 占位符变量替换
 * </p>
 *
 * @author 胡宪棋
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FinancePromptTemplates {

    private final ResourceLoader resourceLoader;
    private final AiConfigMapper aiConfigMapper;

    /** classpath文件模板缓存（文件内容不会在运行时变化） */
    private final Map<String, String> classpathCache = new ConcurrentHashMap<>();

    // 模板键常量
    public static final String KEY_DIAGNOSTIC = "prompt_template_analysis";
    public static final String KEY_CLASSIFY = "prompt_template_classify";
    public static final String KEY_FEATURE_EXTRACTION = "prompt_template_feature_extraction";

    /** 变量契约：代码注入的变量名 → 模板Key */
    private static final Map<String, Set<String>> VARIABLE_CONTRACT = Map.of(
            KEY_DIAGNOSTIC, Set.of("markdown_bill_data"),
            KEY_CLASSIFY, Set.of("markdown_classify_data"),
            KEY_FEATURE_EXTRACTION, Set.of("diagnosis_summary"));

    private static final Pattern VAR_PATTERN = Pattern.compile("\\{\\{(\\w+)\\}\\}");

    /**
     * 启动时校验模板变量契约：确保模板中的变量与代码注入的变量一致
     * 不一致时输出WARN日志，不阻塞启动
     */
    @PostConstruct
    public void validateVariableContracts() {
        log.info("========== Prompt模板变量契约校验开始 ==========");
        for (Map.Entry<String, Set<String>> entry : VARIABLE_CONTRACT.entrySet()) {
            String templateKey = entry.getKey();
            Set<String> injectedVars = entry.getValue();

            String template = loadTemplate(getClasspathPath(templateKey), templateKey);
            if (template == null)
                continue;

            Set<String> templateVars = new LinkedHashSet<>();
            Matcher m = VAR_PATTERN.matcher(template);
            while (m.find()) {
                templateVars.add(m.group(1));
            }

            // 模板中有但代码未注入的变量
            Set<String> missing = new LinkedHashSet<>(templateVars);
            missing.removeAll(injectedVars);
            if (!missing.isEmpty()) {
                log.warn("⚠️ 模板 [{}] 中的变量 {} 未被代码注入，运行时将被替换为空字符串", templateKey, missing);
            }

            // 代码注入但模板未使用的变量
            Set<String> unused = new LinkedHashSet<>(injectedVars);
            unused.removeAll(templateVars);
            if (!unused.isEmpty()) {
                log.info("ℹ️ 模板 [{}] 未使用注入变量 {}（代码仍会注入但无效果）", templateKey, unused);
            }

            if (missing.isEmpty() && unused.isEmpty()) {
                log.info("✅ 模板 [{}] 变量契约校验通过（注入: {} ↔ 模板: {}）", templateKey, injectedVars, templateVars);
            }
        }
        log.info("========== Prompt模板变量契约校验结束 ==========");
    }

    private static String getClasspathPath(String key) {
        return switch (key) {
            case KEY_DIAGNOSTIC -> "prompts/diagnostic-report.txt";
            case KEY_CLASSIFY -> "prompts/category-classify.txt";
            case KEY_FEATURE_EXTRACTION -> "prompts/feature-extraction.txt";
            default -> null;
        };
    }

    /**
     * 获取月度诊断Prompt模板（填充变量后返回完整系统提示）
     */
    public String resolveDiagnosticPrompt(Map<String, String> variables) {
        String template = loadTemplate("prompts/diagnostic-report.txt", KEY_DIAGNOSTIC);
        return resolveVariables(template, variables);
    }

    /**
     * 获取消费分类推荐Prompt模板
     */
    public String resolveClassifyPrompt(Map<String, String> variables) {
        String template = loadTemplate("prompts/category-classify.txt", KEY_CLASSIFY);
        return resolveVariables(template, variables);
    }

    /**
     * 获取消费特征提取Prompt模板
     */
    public String resolveFeatureExtractionPrompt(Map<String, String> variables) {
        String template = loadTemplate("prompts/feature-extraction.txt", KEY_FEATURE_EXTRACTION);
        return resolveVariables(template, variables);
    }

    /**
     * 加载模板 — 三级回退策略：DB > classpath > 默认
     * <p>
     * DB无缓存，每次实时读取以保证管理员修改即时生效
     * </p>
     */
    private String loadTemplate(String classpathPath, String dbConfigKey) {
        // 1. 从数据库加载（优先，支持管理员动态修改，无缓存）
        String template = loadFromDatabase(dbConfigKey);
        if (template != null) {
            return template;
        }

        // 2. 从classpath加载（开发阶段默认模板，启动时缓存）
        template = loadFromClasspath(classpathPath);
        if (template != null) {
            return template;
        }

        // 3. 硬编码默认模板（最终兜底）
        log.warn("模板 {} 无法从DB和classpath加载，使用默认模板", dbConfigKey);
        return getDefaultTemplate(dbConfigKey);
    }

    /**
     * 从classpath加载模板文件（缓存，内容不会在运行时改变）
     */
    private String loadFromClasspath(String classpathPath) {
        String cached = classpathCache.get(classpathPath);
        if (cached != null) {
            return cached;
        }

        try {
            org.springframework.core.io.Resource resource = resourceLoader.getResource("classpath:" + classpathPath);
            if (resource.exists()) {
                String content = resource.getContentAsString(StandardCharsets.UTF_8);
                classpathCache.put(classpathPath, content);
                log.debug("从classpath加载Prompt模板: {}", classpathPath);
                return content;
            }
        } catch (IOException e) {
            log.debug("无法从classpath加载模板 {}: {}", classpathPath, e.getMessage());
        }
        return null;
    }

    /**
     * 从数据库 ai_config 表实时加载模板（无缓存，保证管理员修改即时生效）
     */
    private String loadFromDatabase(String configKey) {
        try {
            String value = aiConfigMapper.findValueByKey(configKey);
            if (value != null && !value.isBlank()) {
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
     * 清空classpath模板缓存
     */
    public void clearCache() {
        classpathCache.clear();
        log.info("Prompt classpath模板缓存已清空");
    }
}