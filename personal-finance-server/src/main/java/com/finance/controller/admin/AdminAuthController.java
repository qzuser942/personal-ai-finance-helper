package com.finance.controller.admin;

import com.finance.entity.SysAdmin;
import com.finance.interceptor.AdminJwtInterceptor;
import com.finance.service.SysAdminService;
import com.finance.utils.JwtUtil;
import com.finance.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@Tag(name = "管理员认证", description = "管理员登录")
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

    @Data
    public static class LoginRequest {
        @NotBlank
        private String username;
        @NotBlank
        private String password;
    }
}
