package com.finance.controller.admin;

import com.finance.mapper.BillMapper;
import com.finance.service.SysUserService;
import com.finance.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Tag(name = "管理员看板", description = "后台总看板数据")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final SysUserService sysUserService;
    private final BillMapper billMapper;

    @Operation(summary = "后台总看板")
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        long totalUsers = sysUserService.count();
        Long activeUsers = billMapper.countMonthlyActiveUsers(currentMonth);
        Long monthlyBills = billMapper.countMonthlyBills(currentMonth);
        BigDecimal monthlyAmount = billMapper.sumMonthlyAmount(currentMonth);
        List<Map<String, Object>> trend = billMapper.recent6MonthTrend();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalUsers", totalUsers);
        data.put("activeUsers", activeUsers != null ? activeUsers : 0);
        data.put("currentMonthBillCount", monthlyBills != null ? monthlyBills : 0);
        data.put("currentMonthTotalAmount", monthlyAmount != null ? monthlyAmount : BigDecimal.ZERO);
        data.put("recent6MonthTrend", trend != null ? trend : new ArrayList<>());
        return Result.ok(data);
    }
}
