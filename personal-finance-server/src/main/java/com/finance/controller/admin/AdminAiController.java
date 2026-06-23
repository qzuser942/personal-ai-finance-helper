package com.finance.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.annotation.AdminLog;
import com.finance.entity.AiConfig;
import com.finance.service.AiConfigService;
import com.finance.service.AiService;
import com.finance.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Tag(name = "管理员-AI运营", description = "AI配置、分析记录、向量重置")
@RestController
@RequestMapping("/api/admin/ai")
@RequiredArgsConstructor
public class AdminAiController {

    private final AiConfigService aiConfigService;
    private final AiService aiService;

    @Operation(summary = "获取AI全部配置")
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

    @Operation(summary = "更新AI配置")
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
        return Result.ok("配置已更新", null);
    }

    @Operation(summary = "全平台AI分析记录")
    @GetMapping("/records")
    public Result<Map<String, Object>> records(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String yearMonth) {
        return Result.ok(aiService.adminGetRecords(page, size, username, yearMonth));
    }

    @Operation(summary = "AI分析记录详情")
    @GetMapping("/records/{id}")
    public Result<Map<String, Object>> recordDetail(@PathVariable Long id) {
        return Result.ok(aiService.adminGetRecordDetail(id));
    }

    @Operation(summary = "重置用户向量数据")
    @PostMapping("/qdrant/reset")
    @AdminLog("重置用户Qdrant向量")
    public Result<Void> resetQdrant(@RequestBody Map<String, Long> body) {
        aiService.resetUserVector(body.get("userId"));
        return Result.ok("用户消费向量数据已清空", null);
    }
}
