package com.finance.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finance.annotation.AdminLog;
import com.finance.annotation.RequireSuperAdmin;
import com.finance.annotation.SensitiveRead;
import com.finance.entity.SysAdmin;
import com.finance.interceptor.AdminJwtInterceptor;
import com.finance.service.SysAdminService;
import com.finance.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.*;

@Tag(name = "管理员-账号管理", description = "管理员CRUD、角色分配（仅超管）")
@RestController
@RequestMapping("/api/admin/account")
@RequiredArgsConstructor
public class AdminAccountController {

    private final SysAdminService sysAdminService;
    private final BCryptPasswordEncoder passwordEncoder;

    @Operation(summary = "管理员列表（仅超管）")
    @GetMapping("/page")
    @RequireSuperAdmin
    @SensitiveRead("查看管理员列表")
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

    @Operation(summary = "新增管理员（仅超管）")
    @PostMapping
    @RequireSuperAdmin
    @AdminLog("新增管理员账号")
    public Result<Map<String, Object>> add(@RequestBody Map<String, String> body) {
        // 关键修复：密码强度校验
        String username = body.get("username");
        String password = body.get("password");
        String role = body.get("role");
        if (username == null || password == null) {
            return Result.fail(90001, "用户名和密码不能为空");
        }
        if (password.length() < 8) {
            return Result.fail(90001, "密码至少8位");
        }
        SysAdmin admin = sysAdminService.register(username, password, role);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", admin.getId());
        data.put("username", admin.getUsername());
        data.put("role", admin.getRole());
        return Result.ok("管理员创建成功", data);
    }

    @Operation(summary = "修改管理员角色（仅超管）")
    @PutMapping("/{id}/role")
    @RequireSuperAdmin
    @AdminLog("修改管理员角色")
    public Result<Void> updateRole(@PathVariable Long id, @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        Long currentAdminId = (Long) request.getAttribute(AdminJwtInterceptor.ADMIN_ID_ATTR);
        if (id.equals(currentAdminId)) {
            return Result.fail(60002, "不能操作自身账号");
        }
        String newRole = body.get("role");
        if (!"SUPER_ADMIN".equals(newRole) && !"OPERATOR".equals(newRole)) {
            return Result.fail(90001, "角色值不合法");
        }
        // 关键修复：至少保留 1 个超管的保护
        if (!"SUPER_ADMIN".equals(newRole)) {
            // 准备把该管理员降级 → 检查是否还有其他超管
            SysAdmin target = sysAdminService.getById(id);
            if (target != null && "SUPER_ADMIN".equals(target.getRole())) {
                Long superCount = sysAdminService.count(
                        new LambdaQueryWrapper<SysAdmin>().eq(SysAdmin::getRole, "SUPER_ADMIN"));
                if (superCount <= 1) {
                    return Result.fail(60006, "系统至少保留1名超级管理员");
                }
            }
        }
        SysAdmin admin = sysAdminService.getById(id);
        if (admin != null) {
            admin.setRole(newRole);
            sysAdminService.updateById(admin);
        }
        return Result.ok("已修改", null);
    }

    @Operation(summary = "修改管理员密码（仅超管，可改自己）")
    @PutMapping("/{id}/password")
    @RequireSuperAdmin
    @AdminLog("修改管理员密码")
    public Result<Void> updatePassword(@PathVariable Long id, @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        String newPwd = body.get("password");
        if (newPwd == null || newPwd.length() < 8) {
            return Result.fail(90001, "密码至少8位");
        }
        // 关键修复（Bug 3）：强制要求"旧密码"校验，防误操作 / 防 session 冒用
        String oldPwd = body.get("oldPassword");
        Long currentAdminId = (Long) request.getAttribute(AdminJwtInterceptor.ADMIN_ID_ATTR);

        SysAdmin admin = sysAdminService.getById(id);
        if (admin == null) {
            return Result.fail(60005, "目标管理员不存在");
        }

        // 自己改自己：必须传 oldPassword 并校验通过
        if (currentAdminId != null && currentAdminId.equals(id)) {
            if (oldPwd == null || oldPwd.isEmpty()) {
                return Result.fail(90002, "修改自己密码时必须填写旧密码");
            }
            // 防止"用 DB 中的 bcrypt 直接 matches"——SysAdmin 存的是 bcrypt 后的密码
            if (!passwordEncoder.matches(oldPwd, admin.getPassword())) {
                return Result.fail(90003, "旧密码错误");
            }
        } else {
            // 超管改别人：可传 confirmToken（此处简化为强制传 newPwd 二次确认）
            String confirm = body.get("confirmPassword");
            if (confirm == null || !confirm.equals(newPwd)) {
                return Result.fail(90004, "超管修改他人密码时必须二次确认密码一致");
            }
        }

        admin.setPassword(passwordEncoder.encode(newPwd));
        sysAdminService.updateById(admin);
        return Result.ok("密码已修改", null);
    }

    @Operation(summary = "重置管理员密码（仅超管，生成随机密码）")
    @PutMapping("/{id}/reset-password")
    @RequireSuperAdmin
    @AdminLog("重置管理员密码")
    public Result<Map<String, String>> resetPassword(@PathVariable Long id) {
        String newPassword = sysAdminService.resetPassword(id);
        Map<String, String> data = new LinkedHashMap<>();
        data.put("newPassword", newPassword);
        return Result.ok("密码已重置", data);
    }

    @Operation(summary = "删除管理员（仅超管）")
    @DeleteMapping("/{id}")
    @RequireSuperAdmin
    @AdminLog("删除管理员账号")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        Long currentAdminId = (Long) request.getAttribute(AdminJwtInterceptor.ADMIN_ID_ATTR);
        if (id.equals(currentAdminId)) {
            return Result.fail(60002, "不能操作自身账号");
        }
        SysAdmin target = sysAdminService.getById(id);
        if (target == null) {
            return Result.fail(60005, "目标管理员不存在");
        }
        // 关键修复：删除超管时检查至少保留 1 名
        if ("SUPER_ADMIN".equals(target.getRole())) {
            Long superCount = sysAdminService.count(
                    new LambdaQueryWrapper<SysAdmin>().eq(SysAdmin::getRole, "SUPER_ADMIN"));
            if (superCount <= 1) {
                return Result.fail(60006, "系统至少保留1名超级管理员");
            }
        }
        sysAdminService.removeById(id);
        return Result.ok("管理员已删除", null);
    }
}