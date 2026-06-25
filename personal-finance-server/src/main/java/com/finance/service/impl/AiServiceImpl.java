package com.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.ai.client.DeepSeekChatClient;
import com.finance.ai.converter.BillMarkdownConverter;
import com.finance.ai.dto.CategoryAlternative;
import com.finance.ai.dto.CategoryRecommendation;
import com.finance.ai.dto.FinanceDiagnosisReport;
import com.finance.ai.prompt.FinancePromptTemplates;
import com.finance.ai.vector.ConsumptionVectorStore;
import com.finance.entity.AiAnalysisRecord;
import com.finance.entity.Category;
import com.finance.entity.SysUser;
import com.finance.exception.BusinessException;
import com.finance.exception.ErrorCode;
import com.finance.mapper.BillMapper;
import com.finance.service.AiAnalysisRecordService;
import com.finance.service.AiService;
import com.finance.service.CategoryService;
import com.finance.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI智能分析服务实现（重构版）
 *
 * 核心流程：
 * 1. 从Qdrant检索用户历史消费画像 → 注入Prompt实现个性化分析
 * 2. 账单数据 → BillMarkdownConverter → Markdown结构化表格
 * 3. FinancePromptTemplates → 加载Prompt模板 → 填充变量
 * 4. DeepSeekChatClient → 调用大模型 → 解析为强类型DTO
 * 5. 分析结果 → ConsumptionVectorStore → 提取特征存入Qdrant记忆库
 * 6. 记录保存到 ai_analysis_record 表
 *
 * @author 胡宪棋
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final BillMapper billMapper;
    private final AiAnalysisRecordService aiAnalysisRecordService;
    private final CategoryService categoryService;
    private final SysUserService sysUserService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== 新注入的AI组件 ====================
    private final DeepSeekChatClient deepSeekChatClient;
    private final BillMarkdownConverter billMarkdownConverter;
    private final FinancePromptTemplates promptTemplates;
    private final ConsumptionVectorStore consumptionVectorStore;

    // ==================== 月度财务诊断（核心功能） ====================

    @Override
    public FinanceDiagnosisReport analyzeMonthly(Long userId, String yearMonth) {
        log.info("开始AI月度分析: userId={}, yearMonth={}", userId, yearMonth);

        // 1. 查询月度账单统计数据
        Map<String, Object> stats = billMapper.monthlyStats(userId, yearMonth);
        if (stats == null || stats.get("billCount") == null
                || ((Number) stats.get("billCount")).longValue() == 0) {
            throw new BusinessException(ErrorCode.AI_NO_BILL_DATA);
        }

        // 2. 获取分类支出汇总
        List<Map<String, Object>> categoryBreakdown = billMapper.categoryBreakdown(userId, yearMonth);

        // 3. 获取全部账单明细（支出+收入，含分类名称和备注）
        List<Map<String, Object>> allBills = getDetailedBills(userId, yearMonth);

        // 4. 检索用户历史消费画像（Qdrant向量记忆 → 个性化分析）
        String historicalProfile = null;
        if (consumptionVectorStore.isAvailable()) {
            historicalProfile = consumptionVectorStore.retrieveUserProfile(userId);
            if (historicalProfile != null) {
                log.debug("已检索到用户{}的历史消费画像", userId);
            }
        }

        // 5. 构建Markdown结构化账单数据
        String markdownData = billMarkdownConverter.buildDiagnosticMarkdown(
                yearMonth, stats, categoryBreakdown, allBills, historicalProfile);

        // 6. 加载并填充Prompt模板
        String systemPrompt = promptTemplates.resolveDiagnosticPrompt(
                Map.of("markdown_bill_data", markdownData));

        // 7. 调用DeepSeek大模型 → 结构化输出
        long startTime = System.currentTimeMillis();
        FinanceDiagnosisReport report;
        try {
            report = deepSeekChatClient.chatStructured(systemPrompt, markdownData,
                    FinanceDiagnosisReport.class);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI服务调用失败: userId={}, yearMonth={}", userId, yearMonth, e);
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
        }
        long processingTimeMs = System.currentTimeMillis() - startTime;

        // 8. 设置运行时字段
        report.setYearMonth(yearMonth);
        report.setProcessingTimeMs(processingTimeMs);

        // 9. 保存分析记录到数据库
        AiAnalysisRecord record = new AiAnalysisRecord();
        record.setUserId(userId);
        record.setYearMonth(yearMonth);
        record.setResultJson(toJson(report));
        record.setModelName(deepSeekChatClient.getModelName());
        record.setProcessingTimeMs(processingTimeMs);
        record.setCreatedAt(LocalDateTime.now());
        aiAnalysisRecordService.save(record);
        report.setRecordId(record.getId());

        // 10. 提取消费特征 → 存入Qdrant向量记忆库（个性化记忆）
        if (consumptionVectorStore.isAvailable()) {
            try {
                Map<String, Object> reportMap = objectMapper.convertValue(report,
                        new TypeReference<Map<String, Object>>() {
                        });
                consumptionVectorStore.storeFeaturesFromReport(userId, reportMap, yearMonth);
                log.debug("用户{}的消费特征已存入向量记忆库", userId);
            } catch (Exception e) {
                log.warn("存储用户消费特征到Qdrant失败（不影响主流程）: {}", e.getMessage());
            }
        }

        log.info("AI月度分析完成: userId={}, yearMonth={}, 耗时={}ms, recordId={}",
                userId, yearMonth, processingTimeMs, record.getId());
        return report;
    }

    // ==================== 智能分类推荐 ====================

    @Override
    public CategoryRecommendation classifyRemark(String remark, String type, Long userId) {
        if (remark == null || remark.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }

        log.info("AI分类推荐: userId={}, remark={}, type={}", userId, remark, type);

        // 1. 获取用户可用分类列表
        List<Category> categories = categoryService.getUserCategories(userId, type);
        if (categories.isEmpty()) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_AVAILABLE);
        }

        // 2. 构建分类Markdown数据
        List<Map<String, Object>> categoryMaps = categories.stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("name", c.getName());
            return m;
        }).toList();
        String markdownData = billMarkdownConverter.buildClassifyMarkdown(remark, categoryMaps);

        // 3. 加载Prompt模板
        String systemPrompt = promptTemplates.resolveClassifyPrompt(
                Map.of("markdown_classify_data", markdownData));

        // 4. 调用大模型
        try {
            // 先获取原始响应，手动解析（因为分类推荐格式较简单）
            String aiResponse = deepSeekChatClient.chat(systemPrompt, markdownData);
            String json = extractJson(aiResponse);
            CategoryRecommendation result = objectMapper.readValue(json, CategoryRecommendation.class);

            // 5. 匹配分类ID（AI返回的是分类名称，需要匹配到系统分类ID）
            String recommendedName = result.getCategoryName();
            for (Category c : categories) {
                if (c.getName().equals(recommendedName)) {
                    result.setCategoryId(c.getId());
                    break;
                }
            }

            // 6. 匹配Top3备选分类的ID
            if (result.getTop3Alternatives() != null) {
                for (CategoryAlternative alt : result.getTop3Alternatives()) {
                    for (Category c : categories) {
                        if (c.getName().equals(alt.getCategoryName())) {
                            alt.setCategoryId(c.getId());
                            break;
                        }
                    }
                }
            }

            return result;
        } catch (Exception e) {
            log.error("AI分类推荐失败: remark={}, type={}", remark, type, e);
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
        }
    }

    // ==================== 历史记录查询 ====================

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
            // 提取overview.summary作为预览
            try {
                FinanceDiagnosisReport report = objectMapper.readValue(r.getResultJson(),
                        FinanceDiagnosisReport.class);
                if (report.getOverview() != null && report.getOverview().getSummary() != null) {
                    String summary = report.getOverview().getSummary();
                    m.put("resultPreview", summary.length() > 100
                            ? summary.substring(0, 100) + "..."
                            : summary);
                } else {
                    m.put("resultPreview", "无预览");
                }
            } catch (Exception e) {
                m.put("resultPreview", "解析失败");
            }
            m.put("createdAt", r.getCreatedAt() != null
                    ? r.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    : null);
            return m;
        }).collect(Collectors.toList());

        return Map.of(
                "records", records,
                "total", pageResult.getTotal(),
                "page", pageResult.getCurrent(),
                "size", pageResult.getSize(),
                "totalPages", pageResult.getPages());
    }

    @Override
    public Map<String, Object> getHistoryDetail(Long recordId, Long userId) {
        AiAnalysisRecord record = aiAnalysisRecordService.getById(recordId);
        if (record == null || !record.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.BILL_NOT_FOUND);
        }
        try {
            Map<String, Object> result = objectMapper.readValue(record.getResultJson(),
                    new TypeReference<Map<String, Object>>() {
                    });
            result.put("recordId", record.getId());
            result.put("yearMonth", record.getYearMonth());
            result.put("processingTimeMs", record.getProcessingTimeMs());
            result.put("createdAt", record.getCreatedAt() != null
                    ? record.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    : null);
            return result;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.AI_PARSE_ERROR);
        }
    }

    // ==================== 管理员功能 ====================

    @Override
    public Map<String, Object> adminGetRecords(Integer page, Integer size, String username, String startDate,
            String endDate) {
        LambdaQueryWrapper<AiAnalysisRecord> wrapper = new LambdaQueryWrapper<>();
        // 时间范围查询
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(AiAnalysisRecord::getCreatedAt, startDate + " 00:00:00");
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(AiAnalysisRecord::getCreatedAt, endDate + " 23:59:59");
        }
        // 用户名模糊查询
        if (username != null && !username.isEmpty()) {
            List<Long> userIds = sysUserService.lambdaQuery()
                    .like(SysUser::getUsername, username)
                    .list()
                    .stream()
                    .map(SysUser::getId)
                    .collect(Collectors.toList());
            if (userIds.isEmpty()) {
                return Map.of("records", List.of(), "total", 0L);
            }
            wrapper.in(AiAnalysisRecord::getUserId, userIds);
        }
        wrapper.orderByDesc(AiAnalysisRecord::getCreatedAt);

        IPage<AiAnalysisRecord> pageResult = aiAnalysisRecordService.page(
                new Page<>(page != null ? page : 1, size != null ? size : 20), wrapper);

        // 收集所有userId，批量查询username
        Set<Long> userIds = pageResult.getRecords().stream()
                .map(AiAnalysisRecord::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            sysUserService.listByIds(userIds).forEach(u -> userMap.put(u.getId(), u.getUsername()));
        }

        List<Map<String, Object>> records = pageResult.getRecords().stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("userId", r.getUserId());
            m.put("username", userMap.getOrDefault(r.getUserId(), "未知用户"));
            m.put("yearMonth", r.getYearMonth());
            m.put("modelName", r.getModelName());
            m.put("processingTimeMs", r.getProcessingTimeMs());
            m.put("createdAt", r.getCreatedAt() != null
                    ? r.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    : null);
            return m;
        }).collect(Collectors.toList());

        return Map.of(
                "records", records,
                "total", pageResult.getTotal(),
                "page", pageResult.getCurrent(),
                "size", pageResult.getSize(),
                "totalPages", pageResult.getPages());
    }

    @Override
    public Map<String, Object> adminGetRecordDetail(Long recordId) {
        AiAnalysisRecord record = aiAnalysisRecordService.getById(recordId);
        if (record == null) {
            throw new BusinessException(ErrorCode.BILL_NOT_FOUND);
        }
        try {
            Map<String, Object> result = objectMapper.readValue(record.getResultJson(),
                    new TypeReference<Map<String, Object>>() {
                    });
            result.put("recordId", record.getId());
            result.put("userId", record.getUserId());
            SysUser user = sysUserService.getById(record.getUserId());
            result.put("username", user != null ? user.getUsername() : "未知用户");
            result.put("yearMonth", record.getYearMonth());
            result.put("processingTimeMs", record.getProcessingTimeMs());
            result.put("createdAt", record.getCreatedAt() != null
                    ? record.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    : null);
            return result;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.AI_PARSE_ERROR);
        }
    }

    @Override
    public void resetUserVector(Long userId) {
        log.info("管理员请求重置用户{}的消费向量记忆数据", userId);
        consumptionVectorStore.deleteUserVectors(userId);
    }

    // ==================== 私有工具方法 ====================

    /**
     * 获取用户某月全部账单明细（带分类名称）
     */
    private List<Map<String, Object>> getDetailedBills(Long userId, String yearMonth) {
        List<Map<String, Object>> bills = new ArrayList<>();

        // 获取支出明细
        List<Map<String, Object>> expenses = billMapper.categoryBreakdown(userId, yearMonth);
        // 获取原始日度明细（包含type字段区分收支）
        List<Map<String, Object>> dailyDetails = billMapper.dailyBreakdown(userId, yearMonth);

        // 使用BillMapper的selectList方式获取带分类名和备注的完整明细
        try {
            // 通过原生日度明细获取更精确的数据
            List<Map<String, Object>> detailedBills = new ArrayList<>();

            // 按日查询每一笔账单
            for (Map<String, Object> daily : dailyDetails) {
                String date = (String) daily.get("date");
                if (date != null) {
                    List<Map<String, Object>> dayBills = getBillsByDate(userId, date);
                    detailedBills.addAll(dayBills);
                }
            }

            if (!detailedBills.isEmpty()) {
                return detailedBills;
            }
        } catch (Exception e) {
            log.debug("按日获取明细失败，使用分类汇总数据: {}", e.getMessage());
        }

        // 回退：使用分类汇总数据构建简化明细
        for (Map<String, Object> exp : expenses) {
            Map<String, Object> bill = new LinkedHashMap<>();
            bill.put("consumeTime", yearMonth + "-01");
            bill.put("amount", exp.get("totalAmount"));
            bill.put("categoryName", exp.get("categoryName"));
            bill.put("remark", "");
            bill.put("type", "expense");
            bills.add(bill);
        }

        return bills;
    }

    /**
     * 按日期查询账单
     */
    private List<Map<String, Object>> getBillsByDate(Long userId, String date) {
        // 这里通过查询带有分类名称、备注、类型等完整信息的账单
        try {
            List<com.finance.entity.Bill> bills = billMapper.selectList(
                    new LambdaQueryWrapper<com.finance.entity.Bill>()
                            .eq(com.finance.entity.Bill::getUserId, userId)
                            .apply("DATE_FORMAT(consume_time, '%Y-%m-%d') = {0}", date));

            return bills.stream().map(b -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("consumeTime", b.getConsumeTime() != null
                        ? b.getConsumeTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                        : date);
                m.put("amount", b.getAmount());
                m.put("categoryName", getCategoryName(b.getCategoryId()));
                m.put("remark", b.getRemark() != null ? b.getRemark() : "");
                m.put("type", b.getType());
                return m;
            }).collect(Collectors.toList());
        } catch (Exception e) {
            return List.of();
        }
    }

    /** 分类名称缓存 */
    private final Map<Long, String> categoryNameCache = new HashMap<>();

    private String getCategoryName(Long categoryId) {
        if (categoryId == null)
            return "未分类";
        return categoryNameCache.computeIfAbsent(categoryId, id -> {
            Category cat = categoryService.getById(id);
            return cat != null ? cat.getName() : "未分类";
        });
    }

    /**
     * 对象转JSON字符串
     */
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("JSON序列化失败: {}", e.getMessage());
            return "{}";
        }
    }

    /**
     * 从AI原始响应中提取JSON字符串
     */
    private String extractJson(String rawResponse) {
        String json = rawResponse.trim();
        if (json.startsWith("```json")) {
            json = json.substring(7);
        } else if (json.startsWith("```")) {
            json = json.substring(3);
        }
        if (json.endsWith("```")) {
            json = json.substring(0, json.length() - 3);
        }
        json = json.trim();

        int startIdx = json.indexOf('{');
        int endIdx = json.lastIndexOf('}');
        if (startIdx >= 0 && endIdx > startIdx) {
            json = json.substring(startIdx, endIdx + 1);
        }
        return json;
    }
}