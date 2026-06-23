package com.finance.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finance.annotation.AdminLog;
import com.finance.entity.SysAdmin;
import com.finance.interceptor.AdminJwtInterceptor;
import com.finance.service.SysAdminService;
import com.finance.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.*;

@Tag(name = "管理员-账号管理", description = "管理员CRUD、角色分配（仅超管）")
@RestController
@RequestMapping("/api/admin/account")
@RequiredArgsConstructor
public class AdminAccountController {

    private final SysAdminService sysAdminService;

    @Operation(summary = "管理员列表")
    @GetMapping("/page")
    public Result<Map<String, Object>> page(@RequestParam(defaultValue = "1") Integer page,
                                             @RequestParam(defaultValue = "20") Integer size) {
        Page<SysAdmin> pageResult = sysAdminService.page(new Page<>(page, size));
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        List<Map<String, Object>> records = pageResult.getRecords().stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("username", a.getUsername());
            m.put("role", a.getRole());
            m.put("lastLoginTime", a.getLastLoginTime() != null ? a.getLastLoginTime().format(fmt) : null);
            m.put("createdAt", a.getCreatedAt() != null ? a.getCreatedAt().format(fmt) : null);
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

    @Operation(summary = "新增管理员")
    @PostMapping
    @AdminLog("新增管理员账号")
    public Result<Map<String, Object>> add(@RequestBody Map<String, String> body) {
        SysAdmin admin = sysAdminService.register(body.get("username"), body.get("password"), body.get("role"));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", admin.getId());
        data.put("username", admin.getUsername());
        data.put("role", admin.getRole());
        return Result.ok("管理员创建成功", data);
    }

    @Operation(summary = "修改管理员角色")
    @PutMapping("/{id}")
    @AdminLog("修改管理员角色")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, String> body, HttpServletRequest request) {
        Long currentAdminId = (Long) request.getAttribute(AdminJwtInterceptor.ADMIN_ID_ATTR);
        if (id.equals(currentAdminId)) {
            return Result.fail(60002, "不能操作自身账号");
        }
        SysAdmin admin = sysAdminService.getById(id);
        if (admin != null) {
            if (body.containsKey("role")) admin.setRole(body.get("role"));
            if (body.containsKey("password") && body.get("password") != null && !body.get("password").isEmpty()) {
                admin.setPassword(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(body.get("password")));
            }
            sysAdminService.updateById(admin);
        }
        return Result.ok("已修改", null);
    }

    @Operation(summary = "重置管理员密码")
    @PutMapping("/{id}/reset-password")
    @AdminLog("重置管理员密码")
    public Result<Map<String, String>> resetPassword(@PathVariable Long id) {
        String newPassword = sysAdminService.resetPassword(id);
        Map<String, String> data = new LinkedHashMap<>();
        data.put("newPassword", newPassword);
        return Result.ok("密码已重置", data);
    }

    @Operation(summary = "删除管理员")
    @DeleteMapping("/{id}")
    @AdminLog("删除管理员账号")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        Long currentAdminId = (Long) request.getAttribute(AdminJwtInterceptor.ADMIN_ID_ATTR);
        if (id.equals(currentAdminId)) {
            return Result.fail(60002, "不能操作自身账号");
        }
        sysAdminService.removeById(id);
        return Result.ok("管理员已删除", null);
    }
}
