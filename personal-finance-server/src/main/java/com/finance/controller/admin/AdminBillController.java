package com.finance.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finance.annotation.AdminLog;
import com.finance.entity.Bill;
import com.finance.entity.SysUser;
import com.finance.mapper.BillMapper;
import com.finance.service.BillService;
import com.finance.service.SysUserService;
import com.finance.utils.ExcelUtil;
import com.finance.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Tag(name = "管理员-账单管理", description = "全平台账单管理、统计、导出")
@RestController
@RequestMapping("/api/admin/bill")
@RequiredArgsConstructor
public class AdminBillController {

    private final BillService billService;
    private final BillMapper billMapper;
    private final SysUserService sysUserService;

    @Operation(summary = "全平台账单分页")
    @GetMapping("/page")
    public Result<Map<String, Object>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount) {
        LambdaQueryWrapper<Bill> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) wrapper.eq(Bill::getUserId, userId);
        if (type != null) wrapper.eq(Bill::getType, type);
        if (startDate != null) wrapper.ge(Bill::getConsumeTime, startDate + " 00:00:00");
        if (endDate != null) wrapper.le(Bill::getConsumeTime, endDate + " 23:59:59");
        if (minAmount != null) wrapper.ge(Bill::getAmount, minAmount);
        if (maxAmount != null) wrapper.le(Bill::getAmount, maxAmount);
        wrapper.orderByDesc(Bill::getConsumeTime);

        Page<Bill> pageResult = billService.page(new Page<>(page, size), wrapper);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        List<Map<String, Object>> records = pageResult.getRecords().stream().map(b -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", b.getId());
            m.put("userId", b.getUserId());
            // lazy-load username
            SysUser u = sysUserService.getById(b.getUserId());
            m.put("username", u != null ? u.getUsername() : "未知");
            m.put("amount", b.getAmount());
            m.put("type", b.getType());
            m.put("categoryId", b.getCategoryId());
            m.put("remark", b.getRemark());
            m.put("hasImage", b.getReceiptImage() != null && !b.getReceiptImage().isEmpty());
            m.put("consumeTime", b.getConsumeTime() != null ? b.getConsumeTime().format(fmt) : null);
            m.put("createdAt", b.getCreatedAt() != null ? b.getCreatedAt().format(fmt) : null);
            return m;
        }).toList();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("records", records);
        data.put("total", pageResult.getTotal());
        data.put("page", pageResult.getCurrent());
        data.put("size", pageResult.getSize());
        data.put("totalPages", pageResult.getPages());
        return Result.ok(data);
    }

    @Operation(summary = "管理员编辑账单")
    @PutMapping("/{id}")
    @AdminLog("管理端编辑账单")
    public Result<Map<String, Object>> update(@PathVariable Long id, @RequestBody Bill bill) {
        Bill updated = billService.updateBill(id, bill, null, true);
        return Result.ok("已修改", billService.getBillDetail(updated.getId(), bill.getUserId()));
    }

    @Operation(summary = "管理员删除账单")
    @DeleteMapping("/{id}")
    @AdminLog("管理端删除账单")
    public Result<Void> delete(@PathVariable Long id) {
        billService.adminDeleteBill(id);
        return Result.ok("账单已删除", null);
    }

    @Operation(summary = "全平台消费统计")
    @GetMapping("/statistics")
    public Result<Map<String, Object>> statistics(@RequestParam(required = false) String yearMonth) {
        if (yearMonth == null) {
            yearMonth = java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userRanking", billMapper.userRanking(yearMonth));
        data.put("categoryDistribution", billMapper.categoryDistribution(yearMonth));
        return Result.ok(data);
    }

    @Operation(summary = "全量导出账单Excel")
    @GetMapping("/export-all")
    public void exportAll(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletResponse response) throws IOException {
        LambdaQueryWrapper<Bill> wrapper = new LambdaQueryWrapper<>();
        if (type != null) wrapper.eq(Bill::getType, type);
        if (startDate != null) wrapper.ge(Bill::getConsumeTime, startDate + " 00:00:00");
        if (endDate != null) wrapper.le(Bill::getConsumeTime, endDate + " 23:59:59");
        wrapper.orderByDesc(Bill::getConsumeTime);
        List<Bill> bills = billService.list(wrapper);

        String[] headers = {"序号","账单ID","用户ID","金额","类型","分类ID","备注", "消费时间","是否有小票"};
        List<Map<String, Object>> dataList = new ArrayList<>();
        int idx = 1;
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (Bill b : bills) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("col0", idx++);
            row.put("col1", b.getId());
            row.put("col2", b.getUserId());
            row.put("col3", b.getAmount());
            row.put("col4", b.getType());
            row.put("col5", b.getCategoryId());
            row.put("col6", b.getRemark());
            row.put("col7", b.getConsumeTime() != null ? b.getConsumeTime().format(fmt) : "");
            row.put("col8", b.getReceiptImage() != null ? "是" : "否");
            dataList.add(row);
        }
        ExcelUtil.exportToResponse(response, "全平台账单导出", "账单", headers, dataList);
    }
}
