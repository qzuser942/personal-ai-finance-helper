package com.finance.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finance.entity.AdminOperationLog;
import com.finance.service.AdminOperationLogService;
import com.finance.utils.ExcelUtil;
import com.finance.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Tag(name = "管理员-操作日志", description = "操作日志查询、导出")
@RestController
@RequestMapping("/api/admin/log")
@RequiredArgsConstructor
public class AdminLogController {

    private final AdminOperationLogService logService;

    @Operation(summary = "操作日志分页")
    @GetMapping("/page")
    public Result<Map<String, Object>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        LambdaQueryWrapper<AdminOperationLog> wrapper = new LambdaQueryWrapper<>();
        if (username != null) wrapper.eq(AdminOperationLog::getAdminUsername, username);
        if (operation != null) wrapper.like(AdminOperationLog::getOperation, operation);
        if (startTime != null) wrapper.ge(AdminOperationLog::getCreatedAt, startTime);
        if (endTime != null) wrapper.le(AdminOperationLog::getCreatedAt, endTime);
        wrapper.orderByDesc(AdminOperationLog::getCreatedAt);
        Page<AdminOperationLog> pageResult = logService.page(new Page<>(page, size), wrapper);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        List<Map<String, Object>> records = pageResult.getRecords().stream().map(l -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", l.getId());
            m.put("adminUsername", l.getAdminUsername());
            m.put("operation", l.getOperation());
            m.put("method", l.getMethod());
            m.put("requestUrl", l.getRequestUrl());
            m.put("status", l.getStatus());
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

    @Operation(summary = "导出操作日志")
    @GetMapping("/export")
    public void export(@RequestParam(required = false) String username,
                       @RequestParam(required = false) String startTime,
                       @RequestParam(required = false) String endTime,
                       HttpServletResponse response) throws IOException {
        LambdaQueryWrapper<AdminOperationLog> wrapper = new LambdaQueryWrapper<>();
        if (username != null) wrapper.eq(AdminOperationLog::getAdminUsername, username);
        if (startTime != null) wrapper.ge(AdminOperationLog::getCreatedAt, startTime);
        if (endTime != null) wrapper.le(AdminOperationLog::getCreatedAt, endTime);
        wrapper.orderByDesc(AdminOperationLog::getCreatedAt);
        List<AdminOperationLog> logs = logService.list(wrapper);

        String[] headers = {"序号","操作人","操作","方法","请求路径","结果","IP","操作时间"};
        List<Map<String, Object>> dataList = new ArrayList<>();
        int idx = 1;
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (AdminOperationLog l : logs) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("col0", idx++);
            row.put("col1", l.getAdminUsername());
            row.put("col2", l.getOperation());
            row.put("col3", l.getMethod());
            row.put("col4", l.getRequestUrl());
            row.put("col5", l.getStatus() == 1 ? "成功" : "失败");
            row.put("col6", l.getIpAddress());
            row.put("col7", l.getCreatedAt() != null ? l.getCreatedAt().format(fmt) : "");
            dataList.add(row);
        }
        ExcelUtil.exportToResponse(response, "操作日志导出", "操作日志", headers, dataList);
    }
}
