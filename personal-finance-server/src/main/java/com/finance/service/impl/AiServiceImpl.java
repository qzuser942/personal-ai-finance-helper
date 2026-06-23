package com.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.config.AiConfig;
import com.finance.entity.AiAnalysisRecord;
import com.finance.entity.Category;
import com.finance.exception.BusinessException;
import com.finance.exception.ErrorCode;
import com.finance.mapper.AiConfigMapper;
import com.finance.mapper.BillMapper;
import com.finance.service.AiAnalysisRecordService;
import com.finance.service.AiService;
import com.finance.service.CategoryService;
import com.finance.utils.MarkdownBuilder;
import com.finance.utils.PageResult;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI智能分析服务实现
 * 核心：Markdown表格组装 -> LangChain4j调用DeepSeek -> 解析JSON -> 存储记录
 *
 * @author 胡宪棋
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final BillMapper billMapper;
    private final AiConfigMapper aiConfigMapper;
    private final AiAnalysisRecordService aiAnalysisRecordService;
    private final CategoryService categoryService;
    private final AiConfig aiConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<String, Object> analyzeMonthly(Long userId, String yearMonth) {
        // 1. 收集当月账单数据
        Map<String, Object> stats = billMapper.monthlyStats(userId, yearMonth);
        if (stats == null || stats.get("billCount") == null || ((Number) stats.get("billCount")).longValue() == 0) {
            throw new BusinessException(ErrorCode.AI_NO_BILL_DATA);
        }

        BigDecimal totalIncome = (BigDecimal) stats.get("totalIncome");
        BigDecimal totalExpense = (BigDecimal) stats.get("totalExpense");
        BigDecimal balance = totalIncome.subtract(totalExpense);

        // 2. 获取账单明细
        List<Map<String, Object>> expenseDetails = billMapper.categoryBreakdown(userId, yearMonth);
        // 获取收入明细（type=income的categoryBreakdown）
        List<Map<String, Object>> allBills = billMapper.dailyBreakdown(userId, yearMonth);

        // 3. 读取Prompt模板
        String promptTemplate = aiConfigMapper.findValueByKey("prompt_template_analysis");
        if (promptTemplate == null) {
            promptTemplate = null; // 使用默认模板
        }

        // 4. 组装Markdown表格
        String prompt = MarkdownBuilder.buildAnalysisPrompt(
                yearMonth, totalIncome, totalExpense, balance,
                expenseDetails, // 支出明细按category
                new ArrayList<>(), // 收入明细简化
                expenseDetails, // 分类占比
                promptTemplate
        );

        // 5. 调用DeepSeek大模型
        long startTime = System.currentTimeMillis();
        String aiResponse;
        try {
            aiResponse = callDeepSeekModel(prompt);
        } catch (Exception e) {
            log.error("AI服务调用失败: ", e);
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
        }
        long processingTimeMs = System.currentTimeMillis() - startTime;

        // 6. 解析AI返回JSON
        Map<String, Object> result;
        try {
            result = parseAiResponse(aiResponse);
        } catch (Exception e) {
            log.error("AI结果解析失败: {}", aiResponse, e);
            throw new BusinessException(ErrorCode.AI_PARSE_ERROR);
        }

        // 7. 存储分析记录
        AiAnalysisRecord record = new AiAnalysisRecord();
        record.setUserId(userId);
        record.setYearMonth(yearMonth);
        record.setResultJson(objectMapper.writeValueAsString(result));
        record.setPromptTemplateSnapshot(promptTemplate);
        record.setModelName(aiConfig.getModelName());
        record.setProcessingTimeMs(processingTimeMs);
        record.setCreatedAt(LocalDateTime.now());
        aiAnalysisRecordService.save(record);

        result.put("recordId", record.getId());
        result.put("yearMonth", yearMonth);
        result.put("processingTimeMs", processingTimeMs);

        log.info("AI分析完成: userId={}, yearMonth={}, 耗时={}ms", userId, yearMonth, processingTimeMs);
        return result;
    }

    @Override
    public Map<String, Object> classifyRemark(String remark, String type, Long userId) {
        // 获取所有分类列表
        List<Category> categories = categoryService.getUserCategories(userId, type);
        String categoryListJson = categories.stream()
                .map(c -> String.format("{\"id\":%d,\"name\":\"%s\"}", c.getId(), c.getName()))
                .collect(Collectors.joining(",", "[", "]"));

        // 读取分类Prompt模板
        String promptTemplate = aiConfigMapper.findValueByKey("prompt_template_classify");
        String prompt;
        if (promptTemplate != null) {
            prompt = promptTemplate.replace("{category_list}", categoryListJson)
                    .replace("{remark_text}", remark);
        } else {
            prompt = MarkdownBuilder.buildClassifyPrompt(remark, categoryListJson);
        }

        try {
            String aiResponse = callDeepSeekModel(prompt);
            Map<String, Object> result = objectMapper.readValue(aiResponse, new TypeReference<Map<String, Object>>() {});

            // 匹配分类ID
            String recommendedName = (String) result.get("categoryName");
            for (Category c : categories) {
                if (c.getName().equals(recommendedName)) {
                    result.put("categoryId", c.getId());
                    break;
                }
            }
            if (!result.containsKey("categoryId")) {
                result.put("categoryId", null);
            }
            return result;
        } catch (Exception e) {
            log.error("AI分类推荐失败: ", e);
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
        }
    }

    @Override
    public Map<String, Object> getHistory(Long userId, Integer page, Integer size) {
        LambdaQueryWrapper<AiAnalysisRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiAnalysisRecord::getUserId, userId)
                .orderByDesc(AiAnalysisRecord::getCreatedAt);
        IPage<AiAnalysisRecord> pageResult = aiAnalysisRecordService.page(
                new Page<>(page != null ? page : 1, size != null ? size : 10), wrapper);

        List<Map<String, Object>> records = pageResult.getRecords().stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("yearMonth", r.getYearMonth());
            m.put("modelName", r.getModelName());
            m.put("processingTimeMs", r.getProcessingTimeMs());
            // 截取前100字作为预览
            try {
                Map<String, Object> full = objectMapper.readValue(r.getResultJson(),
                        new TypeReference<Map<String, Object>>() {});
                String review = (String) full.get("monthlyReview");
                m.put("resultPreview", review != null && review.length() > 100
                        ? review.substring(0, 100) + "..." : review);
            } catch (Exception e) {
                m.put("resultPreview", "解析失败");
            }
            m.put("createdAt", r.getCreatedAt() != null
                    ? r.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null);
            return m;
        }).collect(Collectors.toList());

        return Map.of(
                "records", records,
                "total", pageResult.getTotal(),
                "page", pageResult.getCurrent(),
                "size", pageResult.getSize(),
                "totalPages", pageResult.getPages()
        );
    }

    @Override
    public Map<String, Object> getHistoryDetail(Long recordId, Long userId) {
        AiAnalysisRecord record = aiAnalysisRecordService.getById(recordId);
        if (record == null || !record.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.BILL_NOT_FOUND);
        }
        try {
            Map<String, Object> result = objectMapper.readValue(record.getResultJson(),
                    new TypeReference<Map<String, Object>>() {});
            result.put("recordId", record.getId());
            result.put("yearMonth", record.getYearMonth());
            result.put("processingTimeMs", record.getProcessingTimeMs());
            result.put("promptTemplateSnapshot", record.getPromptTemplateSnapshot());
            result.put("createdAt", record.getCreatedAt() != null
                    ? record.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null);
            return result;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.AI_PARSE_ERROR);
        }
    }

    @Override
    public Map<String, Object> adminGetRecords(Integer page, Integer size, String username, String yearMonth) {
        LambdaQueryWrapper<AiAnalysisRecord> wrapper = new LambdaQueryWrapper<>();
        if (yearMonth != null && !yearMonth.isEmpty()) {
            wrapper.eq(AiAnalysisRecord::getYearMonth, yearMonth);
        }
        wrapper.orderByDesc(AiAnalysisRecord::getCreatedAt);

        IPage<AiAnalysisRecord> pageResult = aiAnalysisRecordService.page(
                new Page<>(page != null ? page : 1, size != null ? size : 20), wrapper);

        List<Map<String, Object>> records = pageResult.getRecords().stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("userId", r.getUserId());
            m.put("yearMonth", r.getYearMonth());
            m.put("modelName", r.getModelName());
            m.put("processingTimeMs", r.getProcessingTimeMs());
            m.put("createdAt", r.getCreatedAt() != null
                    ? r.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null);
            return m;
        }).collect(Collectors.toList());

        return Map.of(
                "records", records,
                "total", pageResult.getTotal(),
                "page", pageResult.getCurrent(),
                "size", pageResult.getSize(),
                "totalPages", pageResult.getPages()
        );
    }

    @Override
    public Map<String, Object> adminGetRecordDetail(Long recordId) {
        AiAnalysisRecord record = aiAnalysisRecordService.getById(recordId);
        if (record == null) throw new BusinessException(ErrorCode.BILL_NOT_FOUND);
        try {
            Map<String, Object> result = objectMapper.readValue(record.getResultJson(),
                    new TypeReference<Map<String, Object>>() {});
            result.put("recordId", record.getId());
            result.put("userId", record.getUserId());
            result.put("yearMonth", record.getYearMonth());
            result.put("processingTimeMs", record.getProcessingTimeMs());
            result.put("createdAt", record.getCreatedAt() != null
                    ? record.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null);
            return result;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.AI_PARSE_ERROR);
        }
    }

    @Override
    public void resetUserVector(Long userId) {
        // Qdrant向量重置 - 当Qdrant未部署时仅记录日志
        log.info("管理员请求重置用户{}的消费向量数据（Qdrant未部署时仅记录）", userId);
    }

    /**
     * 调用DeepSeek大模型（通过LangChain4j OpenAI兼容接口）
     */
    private String callDeepSeekModel(String prompt) {
        String baseUrl = aiConfig.getBaseUrl();
        String modelName = aiConfig.getModelName();

        OpenAiChatModel model = OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(aiConfig.getTemperature())
                .maxTokens(aiConfig.getMaxTokens())
                .topP(aiConfig.getTopP())
                .timeout(Duration.ofSeconds(60))
                .build();

        try {
            String response = model.generate(prompt);
            log.debug("DeepSeek响应: {}", response);
            return response;
        } catch (Exception e) {
            log.error("DeepSeek调用失败: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 解析AI返回的JSON（处理可能的Markdown代码块包裹）
     */
    private Map<String, Object> parseAiResponse(String aiResponse) throws Exception {
        String json = aiResponse.trim();

        // 去除可能的Markdown代码块标记
        if (json.startsWith("```json")) {
            json = json.substring(7);
        } else if (json.startsWith("```")) {
            json = json.substring(3);
        }
        if (json.endsWith("```")) {
            json = json.substring(0, json.length() - 3);
        }
        json = json.trim();

        // 尝试提取第一个JSON对象
        int startIdx = json.indexOf('{');
        int endIdx = json.lastIndexOf('}');
        if (startIdx >= 0 && endIdx > startIdx) {
            json = json.substring(startIdx, endIdx + 1);
        }

        return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
    }
}
