package com.finance.controller.user;

import com.finance.config.FileUploadConfig;
import com.finance.utils.FileUtil;
import com.finance.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Tag(name = "文件管理", description = "小票图片上传")
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final FileUploadConfig fileUploadConfig;

    @Operation(summary = "上传小票图片")
    @PostMapping("/upload/receipt")
    public Result<Map<String, Object>> uploadReceipt(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.fail(50003, "文件为空");
        }
        // 校验文件类型
        if (!FileUtil.isValidImageType(file.getContentType())
                || !FileUtil.isValidExtension(file.getOriginalFilename())) {
            return Result.fail(50001, "文件类型不允许，仅支持 jpg/png/webp");
        }
        // 校验文件大小
        if (file.getSize() > FileUtil.MAX_FILE_SIZE) {
            return Result.fail(50002, "文件大小超过限制（10MB）");
        }

        try {
            String dirPath = fileUploadConfig.getReceiptPath();
            FileUtil.createDirs(dirPath);
            String newFileName = FileUtil.generateFileName(file.getOriginalFilename());
            File dest = new File(dirPath + newFileName);
            file.transferTo(dest);

            String filePath = "/uploads/receipts/" + newFileName;
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("fileName", newFileName);
            data.put("filePath", filePath);
            data.put("fileUrl", filePath);
            data.put("fileSize", file.length());
            log.info("小票图片上传成功: {}", newFileName);
            return Result.ok("上传成功", data);
        } catch (Exception e) {
            log.error("文件上传失败: ", e);
            return Result.fail(50003, "文件上传失败");
        }
    }
}
