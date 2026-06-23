package com.finance.controller.admin;

import com.finance.annotation.AdminLog;
import com.finance.entity.AiConfig;
import com.finance.service.AiConfigService;
import com.finance.service.AiService;
import com.finance.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 管理员 - AI运营管理控制器
 *
 * @author 胡宪棋
 */
@Slf4j
@Tag(name = "管理员-AI运营", description = "AI配置管理、全平台分析记录、向量记忆重置")
@RestController
@RequestMapping("/api/admin/ai")
@RequiredArgsConstructor
public class AdminAiController {

    private final AiConfigService aiConfigService;
    private final AiService aiService;

    @Operation(summary = "获取AI全部配置", description = "获取数据库中所有AI配置项（Prompt模板、模型参数等）")
    @GetMapping("/config")
    public Result<Map<String, Object>> getConfig() {
        List<AiConfig> configs = aiConfigService.list();
        List<Map<String, Object>> list = configs.stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("configKey", c.getConfigKey());
            m.put("configValue", c.getConfigValue());
            m.put("configType", c.getConfigType());
            m.put("description", c.getDescription());
            return m;
        }).toList();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("configs", list);
        return Result.ok(data);
    }

    @Operation(summary = "更新AI配置", description = "批量更新AI配置项（Prompt模板、模型参数等）")
    @PutMapping("/config")
    @AdminLog("更新AI配置")
    public Result<Void> updateConfig(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Map<String, String>> configs = (List<Map<String, String>>) body.get("configs");
        if (configs != null) {
            for (Map<String, String> item : configs) {
                AiConfig config = aiConfigService.getOne(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiConfig>()
                                .eq(AiConfig::getConfigKey, item.get("configKey")));
                if (config != null) {
                    config.setConfigValue(item.get("configValue"));
                    aiConfigService.updateById(config);
                }
            }
        }
        return Result.ok("AI配置已更新", null);
    }

    @Operation(summary = "全平台AI分析记录", description = "分页查询全平台所有AI分析记录，支持按月份筛选")
    @GetMapping("/records")
    public Result<Map<String, Object>> records(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "用户名筛选") @RequestParam(required = false) String username,
            @Parameter(description = "月份筛选（YYYY-MM）") @RequestParam(required = false) String yearMonth) {
        return Result.ok(aiService.adminGetRecords(page, size, username, yearMonth));
    }

    @Operation(summary = "AI分析记录详情", description = "查看指定AI分析记录的完整诊断报告")
    @GetMapping("/records/{id}")
    public Result<Map<String, Object>> recordDetail(
            @Parameter(description = "分析记录ID") @PathVariable Long id) {
        return Result.ok(aiService.adminGetRecordDetail(id));
    }

    @Operation(summary = "重置用户向量记忆", description = "清空指定用户在Qdrant向量库中的消费记忆数据，下次分析将以全新状态进行")
    @PostMapping("/qdrant/reset")
    @AdminLog("重置用户Qdrant向量记忆")
    public Result<Void> resetQdrant(
            @Parameter(description = "{\"userId\": 用户ID}") @RequestBody Map<String, Long> body) {
        Long userId = body.get("userId");
        log.info("管理员请求重置用户{}的向量记忆数据", userId);
        aiService.resetUserVector(userId);
        return Result.ok("用户消费向量记忆已清空，下次分析将以全新状态进行", null);
    }
}
