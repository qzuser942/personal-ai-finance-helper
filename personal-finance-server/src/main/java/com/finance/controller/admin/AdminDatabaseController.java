package com.finance.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finance.annotation.AdminLog;
import com.finance.annotation.RequireSuperAdmin;
import com.finance.annotation.SensitiveRead;
import com.finance.entity.DatabaseBackupLog;
import com.finance.interceptor.AdminJwtInterceptor;
import com.finance.service.DatabaseBackupLogService;
import com.finance.utils.FileUtil;
import com.finance.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Tag(name = "管理员-数据库运维", description = "数据库备份、备份历史（仅超管）")
@RestController
@RequestMapping("/api/admin/database")
@RequiredArgsConstructor
public class AdminDatabaseController {

    private final DatabaseBackupLogService backupLogService;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;
    @Value("${spring.datasource.username}")
    private String dbUsername;
    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Operation(summary = "执行数据库备份（仅超管）")
    @PostMapping("/backup")
    @RequireSuperAdmin
    @AdminLog("执行数据库备份")
    public Result<Map<String, Object>> backup(HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute(AdminJwtInterceptor.ADMIN_ID_ATTR);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "backup_" + timestamp + ".sql";
        String backupDir = "./backups/";
        String filePath = backupDir + fileName;

        try {
            FileUtil.createDirs(backupDir);

            // 关键修复：用正则从 jdbc URL 里精确提取数据库名
            String dbName = "finance_db";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("jdbc:[^/]+://[^/]+/([^?]+)");
            java.util.regex.Matcher m = p.matcher(datasourceUrl);
            if (m.find()) {
                dbName = m.group(1);
            }

            ProcessBuilder pb = new ProcessBuilder(
                    "mysqldump",
                    "-u" + dbUsername,
                    "-p" + dbPassword,
                    "--databases", dbName,
                    "--result-file=" + new File(filePath).getAbsolutePath()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            int exitCode = process.waitFor();

            DatabaseBackupLog logEntry = new DatabaseBackupLog();
            logEntry.setAdminId(adminId);
            logEntry.setFileName(fileName);
            logEntry.setFilePath(filePath);
            logEntry.setFileSize(FileUtil.getFileSize(filePath));
            logEntry.setStatus(exitCode == 0 ? 1 : 0);
            logEntry.setCreatedAt(LocalDateTime.now());
            backupLogService.save(logEntry);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("backupId", logEntry.getId());
            data.put("fileName", fileName);
            data.put("filePath", filePath);
            data.put("fileSize", logEntry.getFileSize());
            data.put("createdAt", logEntry.getCreatedAt() != null
                    ? logEntry.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null);
            return Result.ok("数据库备份成功", data);
        } catch (Exception e) {
            log.error("数据库备份失败: ", e);
            DatabaseBackupLog logEntry = new DatabaseBackupLog();
            logEntry.setAdminId(adminId);
            logEntry.setFileName(fileName);
            logEntry.setFilePath(filePath);
            logEntry.setStatus(0);
            logEntry.setErrorMsg(e.getMessage());
            logEntry.setCreatedAt(LocalDateTime.now());
            backupLogService.save(logEntry);
            return Result.fail(60004, "数据库备份失败: " + e.getMessage());
        }
    }

    @Operation(summary = "备份历史查询（仅超管）")
    @GetMapping("/backup/log")
    @RequireSuperAdmin
    @SensitiveRead("查看备份历史")
    public Result<Map<String, Object>> backupLog(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        Page<DatabaseBackupLog> pageResult = backupLogService.page(new Page<>(page, size),
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DatabaseBackupLog>()
                        .orderByDesc(DatabaseBackupLog::getCreatedAt));

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        List<Map<String, Object>> records = pageResult.getRecords().stream().map(l -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", l.getId());
            m.put("fileName", l.getFileName());
            m.put("fileSize", l.getFileSize());
            m.put("status", l.getStatus());
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
}
