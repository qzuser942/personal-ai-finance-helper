package com.finance.controller.user;

import com.finance.ai.dto.CategoryRecommendation;
import com.finance.ai.dto.FinanceDiagnosisReport;
import com.finance.interceptor.JwtInterceptor;
import com.finance.service.AiService;
import com.finance.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户端 - AI智能分析控制器
 *
 * @author 胡宪棋
 */
@Slf4j
@Tag(name = "AI智能分析", description = "AI月度财务诊断、智能分类推荐、分析历史")
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @Operation(summary = "AI月度财务诊断", description = "一键生成用户月度完整理财报告，包含收支概况、冗余消费识别、不良习惯分析、省钱建议、下月优化方案")
    @PostMapping("/analyze")
    public Result<FinanceDiagnosisReport> analyze(
            @Parameter(description = "{\"yearMonth\": \"2026-06\"}") @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.USER_ID_ATTR);
        String yearMonth = body.get("yearMonth");
        log.info("用户{}请求AI月度分析: yearMonth={}", userId, yearMonth);
        FinanceDiagnosisReport report = aiService.analyzeMonthly(userId, yearMonth);
        return Result.ok("AI财务诊断完成", report);
    }

    @Operation(summary = "AI消费分类推荐", description = "根据消费备注文字智能推荐最匹配的分类，返回Top3备选及置信度")
    @PostMapping("/classify")
    public Result<CategoryRecommendation> classify(
            @Parameter(description = "{\"remark\": \"星巴克拿铁\", \"type\": \"expense\"}") @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.USER_ID_ATTR);
        String remark = body.get("remark");
        String type = body.getOrDefault("type", "expense");
        log.info("用户{}请求AI分类推荐: remark={}, type={}", userId, remark, type);
        CategoryRecommendation result = aiService.classifyRemark(remark, type, userId);
        return Result.ok(result);
    }

    @Operation(summary = "获取个人AI分析历史", description = "分页查询当前用户的AI分析历史记录列表")
    @GetMapping("/history")
    public Result<Map<String, Object>> history(
            @Parameter(description = "页码，默认1") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页条数，默认10") @RequestParam(defaultValue = "10") Integer size,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.USER_ID_ATTR);
        return Result.ok(aiService.getHistory(userId, page, size));
    }

    @Operation(summary = "获取AI分析记录详情", description = "查看某次AI分析的完整诊断报告")
    @GetMapping("/history/{id}")
    public Result<Map<String, Object>> historyDetail(
            @Parameter(description = "分析记录ID") @PathVariable Long id,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.USER_ID_ATTR);
        return Result.ok(aiService.getHistoryDetail(id, userId));
    }
}
