package com.finance.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 文件上传配置
 *
 * @author 胡宪棋
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "file.upload")
public class FileUploadConfig {

    /** 小票图片存储路径 */
    private String receiptPath = "./uploads/receipts/";

    /** 数据库备份存储路径 */
    private String backupPath = "./backups/";
}
