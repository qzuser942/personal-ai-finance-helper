package com.finance.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.finance.entity.Budget;

import java.util.Map;

/**
 * 预算 Service
 */
public interface BudgetService extends IService<Budget> {

    /** 获取当月预算（含预警） */
    Map<String, Object> getCurrentBudget(Long userId);

    /** 获取指定月份预算 */
    Map<String, Object> getBudgetByMonth(Long userId, String yearMonth);

    /** 创建/覆盖预算 */
    Budget saveOrUpdateBudget(Long userId, String yearMonth, Budget budget);

    /** 管理员修正预算 */
    Budget adminUpdateBudget(Long budgetId, Budget budget);
}
