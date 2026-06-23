package com.finance.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finance.annotation.AdminLog;
import com.finance.entity.SysUser;
import com.finance.service.SysUserService;
import com.finance.utils.ExcelUtil;
import com.finance.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Tag(name = "管理员-用户管理", description = "平台用户管理、冻结解冻、重置密码")
@RestController
@RequestMapping("/api/admin/user")
@RequiredArgsConstructor
public class AdminUserController {

    private final SysUserService sysUserService;

    @Operation(summary = "用户分页列表")
    @GetMapping("/page")
    public Result<Map<String, Object>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (username != null) wrapper.like(SysUser::getUsername, username);
        if (status != null) wrapper.eq(SysUser::getStatus, status);
        wrapper.orderByDesc(SysUser::getCreatedAt);
        Page<SysUser> pageResult = sysUserService.page(new Page<>(page, size), wrapper);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        List<Map<String, Object>> records = pageResult.getRecords().stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("userId", u.getId());
            m.put("username", u.getUsername());
            m.put("status", u.getStatus());
            m.put("lastLoginTime", u.getLastLoginTime() != null ? u.getLastLoginTime().format(fmt) : null);
            m.put("createdAt", u.getCreatedAt() != null ? u.getCreatedAt().format(fmt) : null);
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

    @Operation(summary = "冻结/解冻用户")
    @PutMapping("/{userId}/status")
    @AdminLog("冻结/解冻用户")
    public Result<Void> updateStatus(@PathVariable Long userId, @RequestBody Map<String, Integer> body) {
        sysUserService.updateStatus(userId, body.get("status"));
        return Result.ok(body.get("status") == 1 ? "用户已解冻" : "用户已冻结", null);
    }

    @Operation(summary = "重置用户密码")
    @PutMapping("/{userId}/reset-password")
    @AdminLog("重置用户密码")
    public Result<Map<String, String>> resetPassword(@PathVariable Long userId) {
        String newPassword = sysUserService.resetPassword(userId);
        Map<String, String> data = new LinkedHashMap<>();
        data.put("newPassword", newPassword);
        return Result.ok("密码已重置", data);
    }

    @Operation(summary = "导出用户数据")
    @GetMapping("/export")
    public void export(@RequestParam(required = false) String username,
                       @RequestParam(required = false) Integer status,
                       HttpServletResponse response) throws IOException {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (username != null) wrapper.like(SysUser::getUsername, username);
        if (status != null) wrapper.eq(SysUser::getStatus, status);
        List<SysUser> users = sysUserService.list(wrapper);

        String[] headers = {"序号","用户ID","用户名","状态","注册时间","最近登录"};
        List<Map<String, Object>> dataList = new ArrayList<>();
        int idx = 1;
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (SysUser u : users) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("col0", idx++);
            row.put("col1", u.getId());
            row.put("col2", u.getUsername());
            row.put("col3", u.getStatus() == 1 ? "正常" : "冻结");
            row.put("col4", u.getCreatedAt() != null ? u.getCreatedAt().format(fmt) : "");
            row.put("col5", u.getLastLoginTime() != null ? u.getLastLoginTime().format(fmt) : "");
            dataList.add(row);
        }
        ExcelUtil.exportToResponse(response, "用户数据导出", "用户", headers, dataList);
    }
}
