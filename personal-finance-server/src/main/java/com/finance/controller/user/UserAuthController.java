package com.finance.controller.user;

import com.finance.entity.SysUser;
import com.finance.interceptor.JwtInterceptor;
import com.finance.service.SysUserService;
import com.finance.utils.JwtUtil;
import com.finance.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Tag(name = "用户认证", description = "注册、登录、个人信息")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserAuthController {

    private final SysUserService sysUserService;
    private final JwtUtil jwtUtil;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<Map<String, Object>> register(@Valid @RequestBody RegisterRequest req) {
        SysUser user = sysUserService.register(req.getUsername(), req.getPassword(), req.getConfirmPassword());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        return Result.ok("注册成功", data);
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest req) {
        String token = sysUserService.login(req.getUsername(), req.getPassword());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", token);
        data.put("tokenType", "Bearer");
        data.put("expiresIn", 86400);
        data.put("userId", jwtUtil.getUserIdFromToken(token));
        data.put("username", jwtUtil.getUsernameFromToken(token));
        return Result.ok("登录成功", data);
    }

    @Operation(summary = "获取用户信息")
    @GetMapping("/profile")
    public Result<Map<String, Object>> profile(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.USER_ID_ATTR);
        SysUser user = sysUserService.getById(userId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("status", user.getStatus());
        data.put("lastLoginTime", user.getLastLoginTime() != null
                ? user.getLastLoginTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null);
        data.put("createdAt", user.getCreatedAt() != null
                ? user.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null);
        return Result.ok(data);
    }

    @Operation(summary = "修改用户信息")
    @PutMapping("/profile")
    public Result<Map<String, Object>> updateProfile(@RequestBody UpdateProfileRequest req, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.USER_ID_ATTR);
        SysUser user = sysUserService.updateProfile(userId, req.getUsername());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        return Result.ok("修改成功", data);
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest req, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.USER_ID_ATTR);
        sysUserService.changePassword(userId, req.getOldPassword(), req.getNewPassword(), req.getConfirmPassword());
        return Result.ok("密码修改成功", null);
    }

    @Data
    public static class RegisterRequest {
        @NotBlank @Pattern(regexp = "^[a-zA-Z0-9_]{4,20}$", message = "用户名4-20字符，字母数字下划线")
        private String username;
        @NotBlank @Pattern(regexp = "^.{6,20}$", message = "密码6-20字符")
        private String password;
        @NotBlank
        private String confirmPassword;
    }

    @Data
    public static class LoginRequest {
        @NotBlank
        private String username;
        @NotBlank
        private String password;
    }

    @Data
    public static class UpdateProfileRequest {
        private String username;
    }

    @Data
    public static class ChangePasswordRequest {
        @NotBlank
        private String oldPassword;
        @NotBlank
        private String newPassword;
        @NotBlank
        private String confirmPassword;
    }
}
