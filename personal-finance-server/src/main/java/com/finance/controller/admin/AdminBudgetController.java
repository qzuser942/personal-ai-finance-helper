package com.finance.controller.admin;

import com.finance.annotation.AdminLog;
import com.finance.entity.Budget;
import com.finance.service.BudgetService;
import com.finance.service.SaveTargetService;
import com.finance.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@Tag(name = "管理员-预算目标", description = "查看/修正用户预算与存钱目标")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminBudgetController {

    private final BudgetService budgetService;
    private final SaveTargetService saveTargetService;

    @Operation(summary = "查看用户预算")
    @GetMapping("/budget/{userId}")
    public Result<Map<String, Object>> getUserBudget(@PathVariable Long userId,
                                                      @RequestParam(required = false) String yearMonth) {
        if (yearMonth == null) {
            yearMonth = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
        }
        return Result.ok(budgetService.getBudgetByMonth(userId, yearMonth));
    }

    @Operation(summary = "修正用户预算")
    @PutMapping("/budget/{id}")
    @AdminLog("修正用户预算")
    public Result<Map<String, Object>> updateBudget(@PathVariable Long id, @RequestBody Budget budget) {
        Budget updated = budgetService.adminUpdateBudget(id, budget);
        return Result.ok("已修正", budgetService.getBudgetByMonth(updated.getUserId(), updated.getYearMonth()));
    }

    @Operation(summary = "查看用户存钱目标")
    @GetMapping("/save-target/{userId}")
    public Result<?> getUserTargets(@PathVariable Long userId) {
        return Result.ok(saveTargetService.getUserTargets(userId, null));
    }

    @Operation(summary = "修正用户存钱目标")
    @PutMapping("/save-target/{id}")
    @AdminLog("修正用户存钱目标")
    public Result<?> updateTarget(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        BigDecimal targetAmount = body.get("targetAmount") != null ? new BigDecimal(body.get("targetAmount").toString()) : null;
        BigDecimal addAmount = body.get("addAmount") != null ? new BigDecimal(body.get("addAmount").toString()) : null;
        saveTargetService.updateTarget(id, name, targetAmount, addAmount, null, true);
        return Result.ok("已修正", null);
    }
}
