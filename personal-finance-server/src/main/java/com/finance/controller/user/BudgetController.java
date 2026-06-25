package com.finance.controller.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.entity.Budget;
import com.finance.interceptor.JwtInterceptor;
import com.finance.service.BudgetService;
import com.finance.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@Tag(name = "预算管理", description = "月度预算设置、查询、预警")
@RestController
@RequestMapping("/api/budget")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;
    private final ObjectMapper objectMapper;

    @Operation(summary = "获取当月预算")
    @GetMapping("/current")
    public Result<Map<String, Object>> current(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.USER_ID_ATTR);
        Map<String, Object> data = budgetService.getCurrentBudget(userId);
        return Result.ok(data);
    }

    @Operation(summary = "获取指定月份预算")
    @GetMapping("/{yearMonth}")
    public Result<Map<String, Object>> getByMonth(@PathVariable String yearMonth, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.USER_ID_ATTR);
        Map<String, Object> data = budgetService.getBudgetByMonth(userId, yearMonth);
        return Result.ok(data);
    }

    @Operation(summary = "创建/覆盖月度预算")
    @PostMapping
    public Result<Map<String, Object>> save(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.USER_ID_ATTR);
        String yearMonth = (String) body.get("yearMonth");

        Budget budget = new Budget();
        budget.setYearMonth(yearMonth);
        if (body.get("totalBudget") != null) {
            budget.setTotalBudget(new BigDecimal(body.get("totalBudget").toString()));
        }
        // categoryBudgets前端传的是对象，需转为JSON字符串存储
        if (body.get("categoryBudgets") != null) {
            try {
                String categoryBudgetsJson = objectMapper.writeValueAsString(body.get("categoryBudgets"));
                budget.setCategoryBudgets(categoryBudgetsJson);
            } catch (Exception e) {
                budget.setCategoryBudgets(body.get("categoryBudgets").toString());
            }
        }

        Budget saved = budgetService.saveOrUpdateBudget(userId, yearMonth, budget);
        Map<String, Object> data = budgetService.getBudgetByMonth(userId, yearMonth);
        return Result.ok("预算设置成功", data);
    }
}