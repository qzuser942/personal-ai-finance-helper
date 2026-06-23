package com.finance.controller.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finance.annotation.AdminLog;
import com.finance.entity.Bill;
import com.finance.interceptor.JwtInterceptor;
import com.finance.service.BillService;
import com.finance.utils.ExcelUtil;
import com.finance.utils.PageResult;
import com.finance.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Tag(name = "用户账单", description = "账单CRUD、分页、筛选、导出")
@RestController
@RequestMapping("/api/bill")
@RequiredArgsConstructor
public class BillController {

    private final BillService billService;

    @Operation(summary = "新增账单")
    @PostMapping
    public Result<Map<String, Object>> add(@RequestBody Bill bill, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.USER_ID_ATTR);
        Bill saved = billService.addBill(bill, userId);
        return Result.ok("记账成功", billService.getBillDetail(saved.getId(), userId));
    }

    @Operation(summary = "离线批量同步")
    @PostMapping("/sync-batch")
    public Result<Map<String, Object>> syncBatch(@RequestBody List<Map<String, Object>> bills, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.USER_ID_ATTR);
        Map<String, Object> result = billService.syncBatch(bills, userId);
        return Result.ok("批量同步完成", result);
    }

    @Operation(summary = "查询账单详情")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.USER_ID_ATTR);
        return Result.ok(billService.getBillDetail(id, userId));
    }

    @Operation(summary = "分页查询账单")
    @GetMapping("/page")
    public Result<PageResult<Map<String, Object>>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.USER_ID_ATTR);
        LambdaQueryWrapper<Bill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Bill::getUserId, userId).orderByDesc(Bill::getConsumeTime);
        Page<Bill> pageResult = billService.page(new Page<>(page, Math.min(size, 100)), wrapper);
        List<Map<String, Object>> records = pageResult.getRecords().stream().map(b -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", b.getId());
            m.put("amount", b.getAmount());
            m.put("type", b.getType());
            m.put("categoryId", b.getCategoryId());
            m.put("remark", b.getRemark());
            m.put("hasImage", b.getReceiptImage() != null && !b.getReceiptImage().isEmpty());
            m.put("consumeTime", b.getConsumeTime() != null ? b.getConsumeTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null);
            return m;
        }).toList();
        return Result.ok(new PageResult<>(records, pageResult.getTotal(), (int) pageResult.getCurrent(), (int) pageResult.getSize(), (int) pageResult.getPages()));
    }

    @Operation(summary = "多条件筛选账单")
    @GetMapping("/search")
    public Result<PageResult<Map<String, Object>>> search(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String yearMonth,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.USER_ID_ATTR);
        LambdaQueryWrapper<Bill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Bill::getUserId, userId);
        if (type != null) wrapper.eq(Bill::getType, type);
        if (categoryId != null) wrapper.eq(Bill::getCategoryId, categoryId);
        if (keyword != null) wrapper.like(Bill::getRemark, keyword);
        if (yearMonth != null) wrapper.likeRight(Bill::getConsumeTime, yearMonth);
        if (startDate != null) wrapper.ge(Bill::getConsumeTime, startDate + " 00:00:00");
        if (endDate != null) wrapper.le(Bill::getConsumeTime, endDate + " 23:59:59");
        if (minAmount != null) wrapper.ge(Bill::getAmount, minAmount);
        if (maxAmount != null) wrapper.le(Bill::getAmount, maxAmount);
        wrapper.orderByDesc(Bill::getConsumeTime);

        Page<Bill> pageResult = billService.page(new Page<>(page, Math.min(size, 100)), wrapper);
        List<Map<String, Object>> records = pageResult.getRecords().stream().map(b -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", b.getId());
            m.put("amount", b.getAmount());
            m.put("type", b.getType());
            m.put("categoryId", b.getCategoryId());
            m.put("remark", b.getRemark());
            m.put("hasImage", b.getReceiptImage() != null && !b.getReceiptImage().isEmpty());
            m.put("consumeTime", b.getConsumeTime() != null ? b.getConsumeTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null);
            return m;
        }).toList();
        return Result.ok(new PageResult<>(records, pageResult.getTotal(), (int) pageResult.getCurrent(), (int) pageResult.getSize(), (int) pageResult.getPages()));
    }

    @Operation(summary = "修改账单")
    @PutMapping("/{id}")
    public Result<Map<String, Object>> update(@PathVariable Long id, @RequestBody Bill bill, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.USER_ID_ATTR);
        Bill updated = billService.updateBill(id, bill, userId, false);
        return Result.ok("已修改", billService.getBillDetail(updated.getId(), userId));
    }

    @Operation(summary = "删除账单")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.USER_ID_ATTR);
        billService.deleteBill(id, userId, false);
        return Result.ok("账单已删除", null);
    }

    @Operation(summary = "导出个人账单Excel")
    @GetMapping("/export")
    public void export(@RequestParam(required = false) String yearMonth,
                       @RequestParam(required = false) String type,
                       HttpServletRequest request, HttpServletResponse response) throws IOException {
        Long userId = (Long) request.getAttribute(JwtInterceptor.USER_ID_ATTR);
        LambdaQueryWrapper<Bill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Bill::getUserId, userId);
        if (yearMonth != null) wrapper.likeRight(Bill::getConsumeTime, yearMonth);
        if (type != null) wrapper.eq(Bill::getType, type);
        wrapper.orderByDesc(Bill::getConsumeTime);
        List<Bill> bills = billService.list(wrapper);

        String[] headers = {"序号", "金额", "收支类型", "分类ID", "备注", "消费时间", "有无小票"};
        List<Map<String, Object>> dataList = new ArrayList<>();
        int idx = 1;
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (Bill b : bills) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("col0", idx++);
            row.put("col1", b.getAmount());
            row.put("col2", "income".equals(b.getType()) ? "收入" : "支出");
            row.put("col3", b.getCategoryId());
            row.put("col4", b.getRemark());
            row.put("col5", b.getConsumeTime() != null ? b.getConsumeTime().format(fmt) : "");
            row.put("col6", b.getReceiptImage() != null ? "是" : "否");
            dataList.add(row);
        }
        ExcelUtil.exportToResponse(response, "账单导出", "账单", headers, dataList);
    }
}
