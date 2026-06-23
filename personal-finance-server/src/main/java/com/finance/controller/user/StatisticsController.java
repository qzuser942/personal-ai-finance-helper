package com.finance.controller.user;

import com.finance.interceptor.JwtInterceptor;
import com.finance.mapper.BillMapper;
import com.finance.mapper.CategoryMapper;
import com.finance.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Tag(name = "统计分析", description = "月度收支统计、分类占比、日趋势")
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final BillMapper billMapper;

    @Operation(summary = "月度收支统计")
    @GetMapping("/monthly")
    public Result<Map<String, Object>> monthly(@RequestParam String yearMonth,
                                                HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.USER_ID_ATTR);
        Map<String, Object> stats = billMapper.monthlyStats(userId, yearMonth);
        BigDecimal totalIncome = stats != null ? (BigDecimal) stats.getOrDefault("totalIncome", BigDecimal.ZERO) : BigDecimal.ZERO;
        BigDecimal totalExpense = stats != null ? (BigDecimal) stats.getOrDefault("totalExpense", BigDecimal.ZERO) : BigDecimal.ZERO;
        BigDecimal balance = totalIncome.subtract(totalExpense);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("yearMonth", yearMonth);
        result.put("totalIncome", totalIncome);
        result.put("totalExpense", totalExpense);
        result.put("balance", balance);
        result.put("billCount", stats != null ? stats.getOrDefault("billCount", 0) : 0);

        // 分类占比
        List<Map<String, Object>> categoryBreakdown = billMapper.categoryBreakdown(userId, yearMonth);
        if (categoryBreakdown != null) {
            for (Map<String, Object> cb : categoryBreakdown) {
                BigDecimal catAmount = (BigDecimal) cb.get("totalAmount");
                BigDecimal pct = totalExpense.compareTo(BigDecimal.ZERO) > 0
                        ? catAmount.divide(totalExpense, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
                cb.put("percentage", pct);
            }
        }
        result.put("categoryBreakdown", categoryBreakdown != null ? categoryBreakdown : new ArrayList<>());

        // 每日趋势
        List<Map<String, Object>> dailyBreakdown = billMapper.dailyBreakdown(userId, yearMonth);
        String[] weekDays = {"周一","周二","周三","周四","周五","周六","周日"};
        if (dailyBreakdown != null) {
            for (Map<String, Object> db : dailyBreakdown) {
                // 简单添加星期（实际应计算）
                db.put("dayOfWeek", "");
            }
        }
        result.put("dailyBreakdown", dailyBreakdown != null ? dailyBreakdown : new ArrayList<>());

        return Result.ok(result);
    }
}
