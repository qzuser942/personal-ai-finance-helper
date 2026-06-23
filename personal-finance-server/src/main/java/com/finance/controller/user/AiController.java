package com.finance.controller.user;

import com.finance.interceptor.JwtInterceptor;
import com.finance.service.AiService;
import com.finance.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "AI智能分析", description = "AI月度分析、智能分类推荐、分析历史")
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @Operation(summary = "AI月度账单分析")
    @PostMapping("/analyze")
    public Result<Map<String, Object>> analyze(@RequestBody Map<String, String> body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.USER_ID_ATTR);
        String yearMonth = body.get("yearMonth");
        Map<String, Object> result = aiService.analyzeMonthly(userId, yearMonth);
        return Result.ok("AI分析完成", result);
    }

    @Operation(summary = "AI消费分类推荐")
    @PostMapping("/classify")
    public Result<Map<String, Object>> classify(@RequestBody Map<String, String> body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.USER_ID_ATTR);
        String remark = body.get("remark");
        String type = body.getOrDefault("type", "expense");
        Map<String, Object> result = aiService.classifyRemark(remark, type, userId);
        return Result.ok(result);
    }

    @Operation(summary = "获取个人AI分析历史")
    @GetMapping("/history")
    public Result<Map<String, Object>> history(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.USER_ID_ATTR);
        return Result.ok(aiService.getHistory(userId, page, size));
    }

    @Operation(summary = "获取AI分析记录详情")
    @GetMapping("/history/{id}")
    public Result<Map<String, Object>> historyDetail(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.USER_ID_ATTR);
        return Result.ok(aiService.getHistoryDetail(id, userId));
    }
}
