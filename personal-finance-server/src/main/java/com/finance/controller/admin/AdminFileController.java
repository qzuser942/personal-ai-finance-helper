package com.finance.controller.admin;

import com.finance.annotation.AdminLog;
import com.finance.config.FileUploadConfig;
import com.finance.utils.FileUtil;
import com.finance.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

@Tag(name = "管理员-文件管理", description = "文件概览、清理无效文件")
@RestController
@RequestMapping("/api/admin/file")
@RequiredArgsConstructor
public class AdminFileController {

    private final FileUploadConfig fileUploadConfig;

    @Operation(summary = "文件存储概览")
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        String dirPath = fileUploadConfig.getReceiptPath();
        File[] files = FileUtil.listFiles(dirPath);
        long totalFiles = files != null ? files.length : 0;
        long totalSize = FileUtil.getDirSize(dirPath);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalFileCount", totalFiles);
        data.put("totalSizeBytes", totalSize);
        data.put("totalSizeFormatted", FileUtil.formatFileSize(totalSize));
        data.put("storageDir", new File(dirPath).getAbsolutePath());
        return Result.ok(data);
    }

    @Operation(summary = "清理无效文件")
    @DeleteMapping("/clean")
    @AdminLog("清理无效文件")
    public Result<Map<String, Object>> clean() {
        // 简化版：统计并清理uploads目录下所有文件
        String dirPath = fileUploadConfig.getReceiptPath();
        File[] files = FileUtil.listFiles(dirPath);
        long deletedCount = 0;
        long freedBytes = 0;
        if (files != null) {
            for (File f : files) {
                freedBytes += f.length();
                f.delete();
                deletedCount++;
            }
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("deletedCount", deletedCount);
        data.put("freedSpaceBytes", freedBytes);
        data.put("freedSpaceFormatted", FileUtil.formatFileSize(freedBytes));
        return Result.ok("已清理", data);
    }
}
