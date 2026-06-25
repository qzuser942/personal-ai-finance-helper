package com.finance.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finance.annotation.AdminLog;
import com.finance.annotation.RequireSuperAdmin;
import com.finance.annotation.SensitiveRead;
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

@Tag(name = "管理员-账单管理", description = "全平台账单管理、统计、导出（读+导出对运营开放，改/删仅超管）")
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
        if (userId != null)
            wrapper.eq(Bill::getUserId, userId);
        // 用户名模糊查询：查用户表获取匹配的userId列表
        if (username != null && !username.isEmpty()) {
            List<Long> userIds = sysUserService.lambdaQuery()
                    .like(SysUser::getUsername, username)
                    .list()
                    .stream()
                    .map(SysUser::getId)
                    .toList();
            if (userIds.isEmpty()) {
                return Result.ok(Map.of("records", List.of(), "total", 0L));
            }
            wrapper.in(Bill::getUserId, userIds);
        }
        if (type != null)
            wrapper.eq(Bill::getType, type);
        if (startDate != null)
            wrapper.ge(Bill::getConsumeTime, startDate + " 00:00:00");
        if (endDate != null)
            wrapper.le(Bill::getConsumeTime, endDate + " 23:59:59");
        if (minAmount != null)
            wrapper.ge(Bill::getAmount, minAmount);
        if (maxAmount != null)
            wrapper.le(Bill::getAmount, maxAmount);
        wrapper.orderByDesc(Bill::getConsumeTime);

        Page<Bill> pageResult = billService.page(new Page<>(page, size), wrapper);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        List<Map<String, Object>> records = pageResult.getRecords().stream().map(b -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", b.getId());
            m.put("userId", b.getUserId());
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

    @Operation(summary = "管理员编辑账单（仅超管）")
    @PutMapping("/{id}")
    @RequireSuperAdmin
    @AdminLog("管理端编辑账单")
    public Result<Map<String, Object>> update(@PathVariable Long id, @RequestBody Bill bill) {
        Bill updated = billService.updateBill(id, bill, null, true);
        return Result.ok("已修改", billService.getBillDetail(updated.getId(), bill.getUserId()));
    }

    @Operation(summary = "管理员删除账单（仅超管）")
    @DeleteMapping("/{id}")
    @RequireSuperAdmin
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
    @SensitiveRead("导出全平台账单")
    public void exportAll(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletResponse response) throws IOException {
        // 流式分批写出（每次 5000 行），避免一次性加载全表导致 OOM
        LambdaQueryWrapper<Bill> wrapper = new LambdaQueryWrapper<>();
        if (type != null)
            wrapper.eq(Bill::getType, type);
        if (startDate != null)
            wrapper.ge(Bill::getConsumeTime, startDate + " 00:00:00");
        if (endDate != null)
            wrapper.le(Bill::getConsumeTime, endDate + " 23:59:59");
        wrapper.orderByDesc(Bill::getConsumeTime);
        wrapper.select(Bill::getId, Bill::getUserId, Bill::getAmount, Bill::getType,
                Bill::getCategoryId, Bill::getRemark, Bill::getConsumeTime, Bill::getCreatedAt,
                Bill::getReceiptImage);

        // 设置响应头（必须在创建 Workbook 之前）
        String timestamp = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fullFileName = "全平台账单导出_" + timestamp + ".xlsx";
        String encodedFileName = java.net.URLEncoder.encode(fullFileName, java.nio.charset.StandardCharsets.UTF_8)
                .replace("+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);
        response.setCharacterEncoding("UTF-8");

        try (org.apache.poi.xssf.streaming.SXSSFWorkbook workbook = new org.apache.poi.xssf.streaming.SXSSFWorkbook(
                5000);
                java.io.OutputStream os = response.getOutputStream()) {

            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("账单");
            org.apache.poi.ss.usermodel.CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            String[] headers = { "序号", "账单ID", "用户ID", "金额", "类型", "分类ID", "备注", "消费时间", "是否有小票" };
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            int batchSize = 5000;
            long lastId = Long.MAX_VALUE;
            int rowIdx = 1;
            int seq = 1;

            while (true) {
                // 用 ID 倒序分批查
                LambdaQueryWrapper<Bill> pageWrapper = wrapper.clone()
                        .lt(Bill::getId, lastId)
                        .last("LIMIT " + batchSize);
                List<Bill> batch = billService.list(pageWrapper);
                if (batch.isEmpty())
                    break;

                for (Bill b : batch) {
                    org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(seq++);
                    row.createCell(1).setCellValue(b.getId());
                    row.createCell(2).setCellValue(b.getUserId());
                    row.createCell(3).setCellValue(b.getAmount() != null ? b.getAmount().toPlainString() : "");
                    row.createCell(4).setCellValue(b.getType());
                    row.createCell(5).setCellValue(b.getCategoryId());
                    row.createCell(6).setCellValue(b.getRemark());
                    row.createCell(7).setCellValue(b.getConsumeTime() != null ? b.getConsumeTime().format(fmt) : "");
                    row.createCell(8).setCellValue(b.getReceiptImage() != null ? "是" : "否");
                }
                lastId = batch.get(batch.size() - 1).getId();
                if (batch.size() < batchSize)
                    break;
            }

            workbook.write(os);
            os.flush();
        }
    }
}