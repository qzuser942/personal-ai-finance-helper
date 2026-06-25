package com.finance.controller.user;

import com.finance.interceptor.JwtInterceptor;
import com.finance.mapper.BillMapper;
import com.finance.mapper.CategoryMapper;
import com.finance.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Slf4j
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
        if (dailyBreakdown != null) {
            // P1-1 修复：真实计算星期几（之前硬编码为空字符串）
            String[] weekDays = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
            for (Map<String, Object> db : dailyBreakdown) {
                Object dateObj = db.get("date");
                if (dateObj != null) {
                    try {
                        String dateStr = dateObj.toString();
                        // 兼容多种日期格式：yyyy-MM-dd / yyyy-MM-dd HH:mm:ss
                        if (dateStr.length() > 10) {
                            dateStr = dateStr.substring(0, 10);
                        }
                        java.time.LocalDate ld = java.time.LocalDate.parse(dateStr);
                        // DayOfWeek: MONDAY=1 ... SUNDAY=7
                        int dow = ld.getDayOfWeek().getValue();
                        db.put("dayOfWeek", weekDays[dow - 1]);
                    } catch (Exception e) {
                        log.warn("解析日期失败: {}", dateObj);
                        db.put("dayOfWeek", "");
                    }
                } else {
                    db.put("dayOfWeek", "");
                }
            }
        }
        result.put("dailyBreakdown", dailyBreakdown != null ? dailyBreakdown : new ArrayList<>());

        return Result.ok(result);
    }
}
