package com.finance.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.ai.client.DeepSeekChatClient;
import com.finance.annotation.AdminLog;
import com.finance.annotation.RequireSuperAdmin;
import com.finance.annotation.SensitiveRead;
import com.finance.entity.AiAnalysisRecord;
import com.finance.entity.AiConfig;
import com.finance.service.AiAnalysisRecordService;
import com.finance.service.AiConfigService;
import com.finance.service.AiService;
import com.finance.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 管理员 - AI运营管理控制器
 * <p>
 * 权限说明：
 * <ul>
 * <li>GET /config、GET /records、GET /records/{id} → 仅超管（AI配置/全平台用户画像属敏感隐私）</li>
 * <li>PUT /config、POST /config/test、POST /config/reset → 仅超管</li>
 * </ul>
 *
 * @author 胡宪棋
 */
@Slf4j
@Tag(name = "管理员-AI运营", description = "AI配置管理、全平台分析记录、向量记忆重置（仅超管）")
@RestController
@RequestMapping("/api/admin/ai")
@RequiredArgsConstructor
public class AdminAiController {

    private final AiConfigService aiConfigService;
    private final AiService aiService;
    private final AiAnalysisRecordService aiAnalysisRecordService;
    private final DeepSeekChatClient deepSeekChatClient;
    private final com.finance.config.AiConfig aiConfig;
    private final ObjectMapper objectMapper;

    /** 参数校验范围 */
    private static final double TEMPERATURE_MIN = 0.0;
    private static final double TEMPERATURE_MAX = 2.0;
    private static final int MAX_TOKENS_MIN = 256;
    private static final int MAX_TOKENS_MAX = 8192;
    private static final double TOP_P_MIN = 0.0;
    private static final double TOP_P_MAX = 1.0;

    @Operation(summary = "获取AI全部配置（仅超管）")
    @GetMapping("/config")
    @RequireSuperAdmin
    @SensitiveRead("查询全平台AI配置")
    public Result<Map<String, Object>> getConfig() {
        List<AiConfig> configs = aiConfigService.list();
        List<Map<String, Object>> list = new ArrayList<>();
        boolean apiKeyConfigured = false;

        for (AiConfig c : configs) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("configKey", c.getConfigKey());
            m.put("configType", c.getConfigType());
            m.put("description", c.getDescription());

            if ("deepseek_api_key".equals(c.getConfigKey())) {
                // API Key不入库，仅通过环境变量注入；掩码处理，只返回配置状态
                String apiKey = c.getConfigValue();
                if (apiKey != null && !apiKey.isBlank() && !apiKey.startsWith("${")) {
                    apiKeyConfigured = true;
                } else {
                    // 从YAML读取环境变量注入的实际值
                    String yamlKey = aiConfig.getApiKey();
                    apiKeyConfigured = yamlKey != null && !yamlKey.isBlank() && !yamlKey.startsWith("${");
                }
                m.put("configValue", apiKeyConfigured ? "***ENV_CONFIGURED***" : "");
            } else {
                m.put("configValue", c.getConfigValue());
            }
            list.add(m);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("configs", list);
        data.put("apiKeyConfigured", apiKeyConfigured);
        return Result.ok(data);
    }

    @Operation(summary = "更新AI配置（仅超管）")
    @PutMapping("/config")
    @RequireSuperAdmin
    @AdminLog("更新AI配置")
    public Result<Void> updateConfig(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Map<String, String>> configs = (List<Map<String, String>>) body.get("configs");
        if (configs == null || configs.isEmpty()) {
            return Result.fail(400, "配置列表不能为空");
        }

        List<String> errors = new ArrayList<>();
        int updated = 0;

        for (Map<String, String> item : configs) {
            String key = item.get("configKey");
            String value = item.get("configValue");

            if (key == null || key.isBlank()) {
                errors.add("configKey不能为空");
                continue;
            }

            // API Key由环境变量管理，禁止通过管理端修改
            if ("deepseek_api_key".equals(key)) {
                errors.add("deepseek_api_key: API Key由环境变量管理，不支持通过管理端修改");
                continue;
            }

            // 参数校验
            String validationError = validateConfigValue(key, value);
            if (validationError != null) {
                errors.add(key + ": " + validationError);
                continue;
            }

            AiConfig config = aiConfigService.getOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiConfig>()
                            .eq(AiConfig::getConfigKey, key));
            if (config == null) {
                log.warn("AI配置Key '{}' 在数据库中不存在，已跳过更新", key);
                errors.add(key + ": 配置项不存在");
                continue;
            }

            // 值未变化，跳过更新
            if (value.equals(config.getConfigValue())) {
                log.debug("AI配置Key '{}' 值未变化，跳过更新", key);
                continue;
            }

            config.setConfigValue(value);
            aiConfigService.updateById(config);
            updated++;
            log.info("AI配置更新: {} = {}",
                    key.contains("api_key") ? "***" : value);
        }

        if (updated == 0 && errors.isEmpty()) {
            return Result.ok("配置无变化，无需更新", null);
        }

        if (updated == 0) {
            return Result.fail(400, "未能更新任何配置项: " + String.join("; ", errors));
        }

        String msg = "已更新 " + updated + " 项配置";
        if (!errors.isEmpty()) {
            msg += "（" + errors.size() + " 项失败: " + String.join("; ", errors) + "）";
        }
        return Result.ok(msg, null);
    }

    /**
     * 校验配置值的合法性
     */
    private String validateConfigValue(String key, String value) {
        if (value == null || value.isBlank()) {
            return "配置值不能为空";
        }

        switch (key) {
            case "model_name":
                // 模型名称不做白名单限制，支持任意DeepSeek兼容模型
                break;
            case "model_temperature":
                try {
                    double t = Double.parseDouble(value);
                    if (t < TEMPERATURE_MIN || t > TEMPERATURE_MAX) {
                        return "Temperature必须在 " + TEMPERATURE_MIN + "~" + TEMPERATURE_MAX + " 之间";
                    }
                } catch (NumberFormatException e) {
                    return "Temperature必须是数字";
                }
                break;
            case "model_max_tokens":
                try {
                    int t = Integer.parseInt(value);
                    if (t < MAX_TOKENS_MIN || t > MAX_TOKENS_MAX) {
                        return "Max Tokens必须在 " + MAX_TOKENS_MIN + "~" + MAX_TOKENS_MAX + " 之间";
                    }
                } catch (NumberFormatException e) {
                    return "Max Tokens必须是整数";
                }
                break;
            case "model_top_p":
                try {
                    double p = Double.parseDouble(value);
                    if (p < TOP_P_MIN || p > TOP_P_MAX) {
                        return "Top P必须在 " + TOP_P_MIN + "~" + TOP_P_MAX + " 之间";
                    }
                } catch (NumberFormatException e) {
                    return "Top P必须是数字";
                }
                break;
            case "model_base_url":
                if (!value.startsWith("http://") && !value.startsWith("https://")) {
                    return "Base URL必须以 http:// 或 https:// 开头";
                }
                break;
            default:
                break;
        }
        return null;
    }

    @Operation(summary = "测试AI连接（仅超管）")
    @PostMapping("/config/test")
    @RequireSuperAdmin
    @AdminLog("测试AI连接")
    public Result<Map<String, Object>> testConnection() {
        long start = System.currentTimeMillis();
        try {
            String response = deepSeekChatClient.chat(
                    "你是一个简洁的助手，只回复'连接成功'三个字，不要回复其他任何内容。",
                    "测试连接");
            long elapsed = System.currentTimeMillis() - start;
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("success", response != null && response.contains("连接成功"));
            data.put("elapsedMs", elapsed);
            data.put("modelName", deepSeekChatClient.getModelName());
            return Result.ok(data);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("AI连接测试失败: {}", e.getMessage());
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("success", false);
            data.put("elapsedMs", elapsed);
            data.put("error", e.getMessage());
            return Result.fail(500, "AI连接测试失败: " + e.getMessage());
        }
    }

    @Operation(summary = "重置AI模型参数为YAML默认值（仅超管）")
    @PostMapping("/config/reset")
    @RequireSuperAdmin
    @AdminLog("重置AI模型参数为默认值")
    public Result<Void> resetDefaults() {
        Map<String, String> defaults = new LinkedHashMap<>();
        defaults.put("model_base_url", aiConfig.getBaseUrl());
        defaults.put("model_name", aiConfig.getModelName());
        defaults.put("model_temperature", String.valueOf(aiConfig.getTemperature()));
        defaults.put("model_max_tokens", String.valueOf(aiConfig.getMaxTokens()));
        defaults.put("model_top_p", String.valueOf(aiConfig.getTopP()));

        int updated = 0;
        for (Map.Entry<String, String> entry : defaults.entrySet()) {
            AiConfig config = aiConfigService.getOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiConfig>()
                            .eq(AiConfig::getConfigKey, entry.getKey()));
            if (config != null) {
                config.setConfigValue(entry.getValue());
                aiConfigService.updateById(config);
                updated++;
                log.info("AI配置重置: {} = {}", entry.getKey(),
                        entry.getKey().contains("api_key") ? "***" : entry.getValue());
            } else {
                log.warn("AI配置Key '{}' 不存在，无法重置", entry.getKey());
            }
        }
        return Result.ok("已重置 " + updated + " 项AI模型参数为YAML默认值", null);
    }

    @Operation(summary = "获取Prompt模板默认值（仅超管）")
    @GetMapping("/prompt/template")
    @RequireSuperAdmin
    public Result<Map<String, String>> getPromptTemplate(
            @Parameter(description = "模板Key") @RequestParam String key) {
        Map<String, String> classpathMap = Map.of(
                "prompt_template_analysis", "prompts/analysis.txt",
                "prompt_template_classify", "prompts/classify.txt",
                "prompt_template_feature_extraction", "prompts/feature-extraction.txt");

        String classpathPath = classpathMap.get(key);
        if (classpathPath == null) {
            return Result.fail(400, "不支持的模板Key: " + key);
        }

        try {
            var resource = new org.springframework.core.io.ClassPathResource(classpathPath);
            String content = new String(resource.getInputStream().readAllBytes(),
                    java.nio.charset.StandardCharsets.UTF_8);
            Map<String, String> data = new LinkedHashMap<>();
            data.put("key", key);
            data.put("content", content);
            return Result.ok(data);
        } catch (Exception e) {
            log.error("读取classpath模板失败: {}", classpathPath, e);
            return Result.fail(500, "读取classpath模板失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取Prompt模板变量预览数据（仅超管）")
    @GetMapping("/prompt/preview-data")
    @RequireSuperAdmin
    public Result<Map<String, Object>> getPreviewData(
            @Parameter(description = "模板Key") @RequestParam String key) {
        Map<String, Object> data = new LinkedHashMap<>();

        // 从DB取最近一条AI分析记录作为真实样本
        List<AiAnalysisRecord> records = aiAnalysisRecordService.list(
                new LambdaQueryWrapper<AiAnalysisRecord>()
                        .orderByDesc(AiAnalysisRecord::getCreatedAt)
                        .last("LIMIT 1"));
        Map<String, Object> recordData = null;
        String yearMonth = null;
        if (!records.isEmpty()) {
            try {
                recordData = objectMapper.readValue(
                        records.get(0).getResultJson(), new TypeReference<>() {
                        });
                yearMonth = records.get(0).getYearMonth();
            } catch (Exception e) {
                log.warn("解析最近AI分析记录失败: {}", e.getMessage());
            }
        }
        data.put("yearMonth", yearMonth != null ? yearMonth : "无真实数据");

        // 构造所有可能的变量值（从DB真实数据提取，无数据则用默认值兜底）
        String markdownBill = buildSampleMarkdownBill(recordData);
        String classifyData = buildSampleClassifyData(recordData);
        String diagnosisSummary = buildSampleDiagnosisSummary(recordData);

        // 分析Prompt模板相关变量
        data.put("markdown_bill_data", markdownBill);

        // 分类Prompt模板相关变量
        data.put("markdown_classify_data", classifyData);

        // 特征提取Prompt模板相关变量
        data.put("diagnosis_summary", diagnosisSummary);

        // 通用变量（用户可能在任意模板中使用）
        data.put("transactions", extractTransactions(recordData));
        data.put("transactionDescription", extractTransactions(recordData));
        data.put("categoryList", "餐饮、交通、购物、娱乐、居住、通讯、医疗、教育、服饰、其他");
        data.put("monthlyReview", diagnosisSummary);
        data.put("spendingContext", extractSpendingContext(recordData));
        data.put("identifiedIssues", extractIdentifiedIssues(recordData));
        data.put("userHabits", "用户偏好外卖消费，每周点外卖5-6次，周末习惯打车出行");

        return Result.ok(data);
    }

    /** 从DB记录提取交易明细 */
    private String extractTransactions(Map<String, Object> recordData) {
        if (recordData == null) {
            return "外卖：麦当劳 35元\n外卖：肯德基 42元\n滴滴打车 28元\n淘宝 299元";
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> redundantItems = (List<Map<String, Object>>) recordData.get("redundantItems");
        if (redundantItems == null || redundantItems.isEmpty()) {
            return "外卖：麦当劳 35元\n外卖：肯德基 42元\n滴滴打车 28元\n淘宝 299元";
        }
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> item : redundantItems) {
            sb.append(item.get("name")).append("：").append(item.get("amount")).append("元\n");
        }
        return sb.toString().trim();
    }

    /** 提取消费背景 */
    private String extractSpendingContext(Map<String, Object> recordData) {
        if (recordData != null) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> plans = (List<Map<String, Object>>) recordData.get("savingPlans");
            if (plans != null && !plans.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (Map<String, Object> plan : plans) {
                    sb.append(plan.get("plan")).append(": ").append(plan.get("description")).append("\n");
                }
                return sb.toString().trim();
            }
        }
        return "用户为在校大学生，月生活费2,000元，本月消费超出预算75%";
    }

    /** 提取识别的问题 */
    private String extractIdentifiedIssues(Map<String, Object> recordData) {
        if (recordData == null) {
            return "外卖消费占比过高，存在冲动消费行为";
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> redundantItems = (List<Map<String, Object>>) recordData.get("redundantItems");
        if (redundantItems == null || redundantItems.isEmpty()) {
            return "外卖消费占比过高，存在冲动消费行为";
        }
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> item : redundantItems) {
            sb.append(item.get("name")).append("(").append(item.get("amount")).append("元): ")
                    .append(item.get("reason")).append("\n");
        }
        return sb.toString().trim();
    }

    /** 构造 {{markdown_bill_data}} 样本数据 */
    private String buildSampleMarkdownBill(Map<String, Object> recordData) {
        if (recordData != null && recordData.containsKey("monthlyReview")) {
            String monthlyReview = String.valueOf(recordData.getOrDefault("monthlyReview", ""));
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> redundantItems = (List<Map<String, Object>>) recordData.get("redundantItems");
            StringBuilder sb = new StringBuilder();
            sb.append("## 月度账单分析\n\n");
            sb.append("### 月度复盘\n").append(monthlyReview).append("\n\n");
            if (redundantItems != null && !redundantItems.isEmpty()) {
                sb.append("### 冗余消费项\n");
                for (Map<String, Object> item : redundantItems) {
                    sb.append("- **").append(item.get("name")).append("**：")
                            .append(item.get("amount")).append("元 - ")
                            .append(item.get("reason")).append("\n");
                }
            }
            return sb.toString();
        }
        return """
                ## 月度账单分析

                ### 月度复盘
                2025年6月共消费3,500元，其中餐饮1,200元（占比34%）、交通800元（占比23%）、
                购物1,500元（占比43%）。本月支出超出预算75%，消费结构偏向购物和餐饮。

                ### 冗余消费项
                - **外卖过度消费**：450元 - 每周点外卖5-6次，可减少至3次
                - **冲动型网购**：320元 - 购买非必需品，建议设置购物冷静期
                - **打车频次过高**：280元 - 短途出行可改用公交/地铁
                """;
    }

    /** 构造 {{markdown_classify_data}} 样本数据 */
    private String buildSampleClassifyData(Map<String, Object> recordData) {
        if (recordData != null) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> redundantItems = (List<Map<String, Object>>) recordData.get("redundantItems");
            if (redundantItems != null && !redundantItems.isEmpty()) {
                Map<String, Object> first = redundantItems.get(0);
                return "消费备注: " + first.getOrDefault("name", "外卖消费") + " " +
                        first.getOrDefault("amount", "35") + "元\n" +
                        "可用分类: 餐饮、交通、购物、娱乐、居住、通讯、医疗、教育、服饰、其他";
            }
        }
        return """
                消费备注: 麦当劳外卖 35元
                可用分类: 餐饮、交通、购物、娱乐、居住、通讯、医疗、教育、服饰、其他
                """;
    }

    /** 构造 {{diagnosis_summary}} 样本数据 */
    private String buildSampleDiagnosisSummary(Map<String, Object> recordData) {
        if (recordData != null && recordData.containsKey("monthlyReview")) {
            return String.valueOf(recordData.get("monthlyReview"));
        }
        return "该用户消费偏好集中在餐饮外卖（占比35%），存在外卖过度消费和冲动型网购的不良习惯。" +
                "主要改进方向为减少外卖频次、设置购物冷静期。";
    }

    /** 模板变量名正则 */
    private static final Pattern VAR_PATTERN = Pattern.compile("\\{\\{(\\w+)\\}\\}");

    @Operation(summary = "提取模板变量列表（仅超管）")
    @GetMapping("/prompt/variables")
    @RequireSuperAdmin
    public Result<Map<String, Object>> extractVariables(
            @Parameter(description = "模板Key") @RequestParam String key) {
        AiConfig config = aiConfigService.getOne(
                new LambdaQueryWrapper<AiConfig>().eq(AiConfig::getConfigKey, key));
        if (config == null || config.getConfigValue() == null) {
            return Result.fail(404, "模板不存在: " + key);
        }

        List<String> variables = new ArrayList<>();
        Matcher m = VAR_PATTERN.matcher(config.getConfigValue());
        while (m.find()) {
            String var = m.group(1);
            if (!variables.contains(var))
                variables.add(var);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("key", key);
        data.put("variables", variables);
        data.put("count", variables.size());
        return Result.ok(data);
    }

    @Operation(summary = "全平台AI分析记录（仅超管）")
    @GetMapping("/records")
    @RequireSuperAdmin
    @SensitiveRead("查询全平台AI分析记录")
    public Result<Map<String, Object>> records(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "用户名模糊筛选") @RequestParam(required = false) String username,
            @Parameter(description = "起始日期（YYYY-MM-DD）") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期（YYYY-MM-DD）") @RequestParam(required = false) String endDate) {
        return Result.ok(aiService.adminGetRecords(page, size, username, startDate, endDate));
    }

    @Operation(summary = "AI分析记录详情（仅超管）")
    @GetMapping("/records/{id}")
    @RequireSuperAdmin
    @SensitiveRead("查看AI分析详情")
    public Result<Map<String, Object>> recordDetail(
            @Parameter(description = "分析记录ID") @PathVariable Long id) {
        return Result.ok(aiService.adminGetRecordDetail(id));
    }

    @Operation(summary = "重置用户向量记忆（仅超管）")
    @PostMapping("/qdrant/reset")
    @RequireSuperAdmin
    @AdminLog("重置用户Qdrant向量记忆")
    public Result<Void> resetQdrant(
            @Parameter(description = "{\"userId\": 用户ID}") @RequestBody Map<String, Long> body) {
        Long userId = body.get("userId");
        log.info("管理员请求重置用户{}的向量记忆数据", userId);
        aiService.resetUserVector(userId);
        return Result.ok("用户消费向量记忆已清空，下次分析将以全新状态进行", null);
    }
}