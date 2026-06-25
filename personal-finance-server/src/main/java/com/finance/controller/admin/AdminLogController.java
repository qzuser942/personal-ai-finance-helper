package com.finance.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finance.annotation.RequireSuperAdmin;
import com.finance.annotation.SensitiveRead;
import com.finance.entity.AdminOperationLog;
import com.finance.service.AdminOperationLogService;
import com.finance.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Tag(name = "管理员-操作日志", description = "操作日志查询（导出仅超管）")
@RestController
@RequestMapping("/api/admin/log")
@RequiredArgsConstructor
public class AdminLogController {

    private final AdminOperationLogService logService;

    @Operation(summary = "操作日志分页")
    @GetMapping("/page")
    @SensitiveRead("查看操作日志")
    public Result<Map<String, Object>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            HttpServletRequest request) {
        LambdaQueryWrapper<AdminOperationLog> wrapper = new LambdaQueryWrapper<>();
        // 关键修复：operator 只能看自己产生的日志，超管才能看全部（防 P0-2 越权 + Bug 1）
        Long currentAdminId = (Long) request.getAttribute(com.finance.interceptor.AdminJwtInterceptor.ADMIN_ID_ATTR);
        String currentRole = (String) request.getAttribute(com.finance.interceptor.AdminJwtInterceptor.ADMIN_ROLE_ATTR);
        if (!"SUPER_ADMIN".equals(currentRole)) {
            wrapper.eq(AdminOperationLog::getAdminId, currentAdminId);
        }
        if (username != null)
            wrapper.eq(AdminOperationLog::getAdminUsername, username);
        if (operation != null)
            wrapper.like(AdminOperationLog::getOperation, operation);
        if (startTime != null)
            wrapper.ge(AdminOperationLog::getCreatedAt, startTime);
        if (endTime != null)
            wrapper.le(AdminOperationLog::getCreatedAt, endTime);
        wrapper.orderByDesc(AdminOperationLog::getCreatedAt);
        Page<AdminOperationLog> pageResult = logService.page(new Page<>(page, size), wrapper);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        List<Map<String, Object>> records = pageResult.getRecords().stream().map(l -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", l.getId());
            m.put("adminUsername", l.getAdminUsername());
            m.put("adminRole", l.getAdminRole());
            m.put("operation", l.getOperation());
            m.put("method", l.getMethod());
            m.put("requestUrl", l.getRequestUrl());
            m.put("resourceId", l.getResourceId());
            m.put("requestParams", l.getRequestParams());
            m.put("status", l.getStatus());
            m.put("errorMsg", l.getErrorMsg());
            m.put("ipAddress", l.getIpAddress());
            m.put("createdAt", l.getCreatedAt() != null ? l.getCreatedAt().format(fmt) : null);
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

    @Operation(summary = "导出操作日志（仅超管）")
    @GetMapping("/export")
    @RequireSuperAdmin
    @SensitiveRead("导出操作日志")
    public void export(@RequestParam(required = false) String username,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            HttpServletResponse response) throws IOException {
        // 关键修复（P2-2）：强制要求时间范围，防止误操作一次性导出全平台日志
        if (startTime == null || endTime == null
                || startTime.isEmpty() || endTime.isEmpty()) {
            response.setStatus(400);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"code\":90001,\"message\":\"导出操作日志必须指定 startTime 与 endTime 范围（防止误操作全量导出）\",\"data\":null}");
            return;
        }
        // 时间范围上限：90 天，超出强制截断（防 P2-2 误下载海量日志）
        try {
            java.time.LocalDateTime start = java.time.LocalDateTime.parse(startTime,
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            java.time.LocalDateTime end = java.time.LocalDateTime.parse(endTime,
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            if (java.time.Duration.between(start, end).toDays() > 90) {
                response.setStatus(400);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":90001,\"message\":\"导出时间范围不能超过 90 天，请分批导出\",\"data\":null}");
                return;
            }
        } catch (Exception e) {
            response.setStatus(400);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":90001,\"message\":\"时间格式错误，应为 yyyy-MM-dd HH:mm:ss\",\"data\":null}");
            return;
        }

        LambdaQueryWrapper<AdminOperationLog> wrapper = new LambdaQueryWrapper<>();
        if (username != null)
            wrapper.eq(AdminOperationLog::getAdminUsername, username);
        if (startTime != null)
            wrapper.ge(AdminOperationLog::getCreatedAt, startTime);
        if (endTime != null)
            wrapper.le(AdminOperationLog::getCreatedAt, endTime);
        wrapper.orderByDesc(AdminOperationLog::getCreatedAt);
        wrapper.select(AdminOperationLog::getId, AdminOperationLog::getAdminUsername,
                AdminOperationLog::getOperation, AdminOperationLog::getMethod,
                AdminOperationLog::getRequestUrl, AdminOperationLog::getStatus,
                AdminOperationLog::getIpAddress, AdminOperationLog::getCreatedAt);

        // 设置响应头
        String timestamp = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fullFileName = "操作日志导出_" + timestamp + ".xlsx";
        String encodedFileName = java.net.URLEncoder.encode(fullFileName, java.nio.charset.StandardCharsets.UTF_8)
                .replace("+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);
        response.setCharacterEncoding("UTF-8");

        // 流式分批导出，避免 OOM
        try (org.apache.poi.xssf.streaming.SXSSFWorkbook workbook = new org.apache.poi.xssf.streaming.SXSSFWorkbook(
                5000);
                java.io.OutputStream os = response.getOutputStream()) {

            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("操作日志");
            org.apache.poi.ss.usermodel.CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            String[] headers = { "序号", "操作人", "操作", "方法", "请求路径", "结果", "IP", "操作时间" };
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
                LambdaQueryWrapper<AdminOperationLog> pageWrapper = wrapper.clone()
                        .lt(AdminOperationLog::getId, lastId)
                        .last("LIMIT " + batchSize);
                List<AdminOperationLog> batch = logService.list(pageWrapper);
                if (batch.isEmpty())
                    break;

                for (AdminOperationLog l : batch) {
                    org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(seq++);
                    row.createCell(1).setCellValue(l.getAdminUsername());
                    row.createCell(2).setCellValue(l.getOperation());
                    row.createCell(3).setCellValue(l.getMethod());
                    row.createCell(4).setCellValue(l.getRequestUrl());
                    row.createCell(5).setCellValue(l.getStatus() != null && l.getStatus() == 1 ? "成功" : "失败");
                    row.createCell(6).setCellValue(l.getIpAddress());
                    row.createCell(7).setCellValue(l.getCreatedAt() != null ? l.getCreatedAt().format(fmt) : "");
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