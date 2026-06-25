package com.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.entity.Budget;
import com.finance.exception.BusinessException;
import com.finance.exception.ErrorCode;
import com.finance.mapper.BillMapper;
import com.finance.mapper.BudgetMapper;
import com.finance.service.BudgetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetServiceImpl extends ServiceImpl<BudgetMapper, Budget> implements BudgetService {

    private final BillMapper billMapper;
    private final ObjectMapper objectMapper;

    @Override
    public Map<String, Object> getCurrentBudget(Long userId) {
        String yearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        return getBudgetByMonth(userId, yearMonth);
    }

    @Override
    public Map<String, Object> getBudgetByMonth(Long userId, String yearMonth) {
        LambdaQueryWrapper<Budget> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Budget::getUserId, userId).eq(Budget::getYearMonth, yearMonth);
        Budget budget = getOne(wrapper);

        if (budget == null) {
            return null;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", budget.getId());
        result.put("yearMonth", budget.getYearMonth());
        result.put("totalBudget", budget.getTotalBudget());

        // 计算当月实际消费
        Map<String, Object> stats = billMapper.monthlyStats(userId, yearMonth);
        BigDecimal totalSpent = stats != null && stats.get("totalExpense") != null
                ? (BigDecimal) stats.get("totalExpense") : BigDecimal.ZERO;
        BigDecimal remaining = budget.getTotalBudget().subtract(totalSpent);
        BigDecimal usagePercent = budget.getTotalBudget().compareTo(BigDecimal.ZERO) > 0
                ? totalSpent.divide(budget.getTotalBudget(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        result.put("totalSpent", totalSpent);
        result.put("remaining", remaining);
        result.put("usagePercent", usagePercent);

        // 解析分类子预算
        Map<String, Object> categoryBudgetsMap = new LinkedHashMap<>();
        if (budget.getCategoryBudgets() != null && !budget.getCategoryBudgets().isEmpty()) {
            try {
                categoryBudgetsMap = objectMapper.readValue(budget.getCategoryBudgets(), new TypeReference<Map<String, Object>>() {});
            } catch (Exception ignored) {}
        }
        result.put("categoryBudgets", categoryBudgetsMap);

        // 各分类实际消费
        List<Map<String, Object>> categoryBreakdown = billMapper.categoryBreakdown(userId, yearMonth);
        Map<String, BigDecimal> categorySpent = new LinkedHashMap<>();
        if (categoryBreakdown != null) {
            for (Map<String, Object> cb : categoryBreakdown) {
                categorySpent.put(cb.get("categoryId").toString(), (BigDecimal) cb.get("totalAmount"));
            }
        }
        result.put("categorySpent", categorySpent);

        // 预警分类
        List<Map<String, Object>> alertCategories = new ArrayList<>();
        if (categoryBudgetsMap != null) {
            for (Map.Entry<String, Object> entry : categoryBudgetsMap.entrySet()) {
                BigDecimal budgetAmount = new BigDecimal(entry.getValue().toString());
                BigDecimal spent = categorySpent.getOrDefault(entry.getKey(), BigDecimal.ZERO);
                if (budgetAmount.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal pct = spent.divide(budgetAmount, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));
                    if (pct.compareTo(BigDecimal.valueOf(80)) >= 0) {
                        Map<String, Object> alert = new LinkedHashMap<>();
                        alert.put("categoryId", Long.valueOf(entry.getKey()));
                        alert.put("budgetAmount", budgetAmount);
                        alert.put("spentAmount", spent);
                        alert.put("alertLevel", pct.compareTo(BigDecimal.valueOf(100)) >= 0 ? "OVER" : "WARNING");
                        alertCategories.add(alert);
                    }
                }
            }
        }
        result.put("alertCategories", alertCategories);

        return result;
    }

    @Override
    @Transactional
    public Budget saveOrUpdateBudget(Long userId, String yearMonth, Budget budget) {
        LambdaQueryWrapper<Budget> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Budget::getUserId, userId).eq(Budget::getYearMonth, yearMonth);
        Budget existing = getOne(wrapper);

        Budget target;
        if (existing != null) {
            target = existing;
            target.setTotalBudget(budget.getTotalBudget());
            if (budget.getCategoryBudgets() != null) {
                target.setCategoryBudgets(budget.getCategoryBudgets());
            }
            updateById(target);
        } else {
            budget.setUserId(userId);
            budget.setYearMonth(yearMonth);
            save(budget);
            target = budget;
        }
        log.info("用户{}设置{}月预算, 总额={}", userId, yearMonth, budget.getTotalBudget());
        return target;
    }

    @Override
    @Transactional
    public Budget adminUpdateBudget(Long budgetId, Budget budget) {
        Budget existing = getById(budgetId);
        // P1-3 修复：错误码语义错位（之前用 BILL_NOT_FOUND 表示"预算不存在"）
        if (existing == null) {
            throw new BusinessException(ErrorCode.BUDGET_NOT_FOUND);
        }
        if (budget.getTotalBudget() != null) existing.setTotalBudget(budget.getTotalBudget());
        if (budget.getCategoryBudgets() != null) existing.setCategoryBudgets(budget.getCategoryBudgets());
        updateById(existing);
        return existing;
    }
}
