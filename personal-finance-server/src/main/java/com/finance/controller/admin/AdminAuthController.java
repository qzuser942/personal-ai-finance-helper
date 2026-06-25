package com.finance.controller.admin;

import com.finance.entity.SysAdmin;
import com.finance.interceptor.AdminJwtInterceptor;
import com.finance.service.SysAdminService;
import com.finance.utils.JwtUtil;
import com.finance.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "管理员认证", description = "管理员登录、当前信息")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminAuthController {

    private final SysAdminService sysAdminService;
    private final JwtUtil jwtUtil;

    @Operation(summary = "管理员登录")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest req) {
        String token = sysAdminService.login(req.getUsername(), req.getPassword());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", token);
        data.put("tokenType", "Bearer");
        data.put("expiresIn", 86400);
        data.put("adminId", jwtUtil.getAdminIdFromToken(token));
        data.put("username", jwtUtil.getUsernameFromToken(token));
        data.put("role", jwtUtil.getRoleFromToken(token));
        return Result.ok("登录成功", data);
    }

    /**
     * 关键修复：当前管理员信息接口（自调用、非敏感读，移除 @SensitiveRead 避免污染审计日志）
     * <p>前端每次进入管理后台或路由切换时调用此接口，刷新本地 role/userId 等信息。
     * 这样超管被降级后，刷新页面即可立刻生效（不再依赖过期的 localStorage）。
     * <p>同时返回权限位 permissions 列表，前端 v-permission 指令统一从该字段渲染按钮。
     */
    @Operation(summary = "获取当前管理员信息")
    @GetMapping("/info")
    public Result<Map<String, Object>> info(HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute(AdminJwtInterceptor.ADMIN_ID_ATTR);
        String username = (String) request.getAttribute(AdminJwtInterceptor.ADMIN_USERNAME_ATTR);
        String role = (String) request.getAttribute(AdminJwtInterceptor.ADMIN_ROLE_ATTR);

        // 关键修复（Bug 2）：adminId 有效性二次校验，admin 已删除则视为已登出
        SysAdmin admin = adminId != null ? sysAdminService.getById(adminId) : null;
        if (admin == null) {
            return Result.fail(10004, "管理员账号不存在或已被删除，请重新登录");
        }
        // 实时从数据库读取最新 role（防 JWT 签发后被改）
        role = admin.getRole();
        username = admin.getUsername();

        // 关键修复：根据 role 计算权限位列表（前端按钮统一从这里取）
        List<String> permissions = computePermissions(role);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("adminId", adminId);
        data.put("username", username);
        data.put("role", role);
        data.put("isSuperAdmin", "SUPER_ADMIN".equals(role));
        data.put("permissions", permissions);
        return Result.ok(data);
    }

    /**
     * 根据角色计算权限位列表
     * <p>SUPER_ADMIN 拥有全部权限位；OPERATOR 仅可访问运营白名单内的功能。
     */
    private List<String> computePermissions(String role) {
        // 全部权限位（与 AdminJwtInterceptor.DEFAULT_OPERATOR_ALLOWED 保持一致）
        List<String> all = Arrays.asList(
                "bill:read", "bill:export",
                "user:read", "user:toggle", "user:export",
                "log:read",
                "dashboard:read",
                // 仅超管
                "bill:write", "bill:delete",
                "budget:write",
                "category:write", "category:delete",
                "user:resetPassword",
                "ai:read", "ai:write",
                "log:export",
                "account:manage",
                "file:clean", "file:overview",
                "database:backup"
        );
        if ("SUPER_ADMIN".equals(role)) {
            return new ArrayList<>(all);
        }
        // 运营仅可读 + 冻结用户 + 导出账单/用户
        // 关键修复：移除 file:overview（修复 P0-1 文件管理越权）
        // 关键修复：移除 category:read / budget:read（修复 P2-3 未使用权限位，运营走 Controller 白名单自动放行）
        return Arrays.asList("bill:read", "bill:export",
                "user:read", "user:toggle", "user:export",
                "log:read",
                "dashboard:read");
    }

    @Data
    public static class LoginRequest {
        @NotBlank
        private String username;
        @NotBlank
        private String password;
    }
}
