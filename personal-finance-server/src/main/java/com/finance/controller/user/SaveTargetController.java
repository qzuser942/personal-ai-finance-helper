package com.finance.controller.user;

import com.finance.entity.SaveTarget;
import com.finance.interceptor.JwtInterceptor;
import com.finance.service.SaveTargetService;
import com.finance.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@Tag(name = "存钱目标", description = "存钱目标CRUD、追加入款")
@RestController
@RequestMapping("/api/save-target")
@RequiredArgsConstructor
public class SaveTargetController {

    private final SaveTargetService saveTargetService;

    @Operation(summary = "获取存钱目标列表")
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list(@RequestParam(required = false) Integer status,
                                                   HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.USER_ID_ATTR);
        return Result.ok(saveTargetService.getUserTargets(userId, status));
    }

    @Operation(summary = "创建存钱目标")
    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody SaveTarget target, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.USER_ID_ATTR);
        SaveTarget saved = saveTargetService.createTarget(target, userId);
        return Result.ok("目标创建成功", saveTargetService.getUserTargets(userId, null)
                .stream().filter(t -> t.get("id").equals(saved.getId())).findFirst().orElse(null));
    }

    @Operation(summary = "更新/追加存款")
    @PutMapping("/{id}")
    public Result<Map<String, Object>> update(@PathVariable Long id,
                                               @RequestBody Map<String, Object> body,
                                               HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.USER_ID_ATTR);
        String name = (String) body.get("name");
        BigDecimal targetAmount = body.get("targetAmount") != null ? new BigDecimal(body.get("targetAmount").toString()) : null;
        BigDecimal addAmount = body.get("addAmount") != null ? new BigDecimal(body.get("addAmount").toString()) : null;
        SaveTarget updated = saveTargetService.updateTarget(id, name, targetAmount, addAmount, userId, false);
        return Result.ok("已更新", saveTargetService.getUserTargets(userId, null)
                .stream().filter(t -> t.get("id").equals(updated.getId())).findFirst().orElse(null));
    }

    @Operation(summary = "删除存钱目标")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.USER_ID_ATTR);
        saveTargetService.deleteTarget(id, userId, false);
        return Result.ok("目标已删除", null);
    }
}
