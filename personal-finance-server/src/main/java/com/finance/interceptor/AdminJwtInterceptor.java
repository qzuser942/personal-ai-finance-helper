package com.finance.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.annotation.RequireRole;
import com.finance.annotation.RequireSuperAdmin;
import com.finance.utils.JwtUtil;
import com.finance.utils.Result;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/**
 * 管理员JWT拦截器 - 校验 /api/admin/** 请求
 * <p>
 * 权限判断优先级（避免 startsWith 误匹配，使用精确路径/注解匹配）：
 * <ol>
 * <li>方法上有 @RequireSuperAdmin 注解 → 仅超管（最高优先级）</li>
 * <li>方法上有 @RequireRole 注解 → 按注解 value 列表匹配</li>
 * <li>方法所属 Controller 在 DEFAULT_OPERATOR_ALLOWED 集合内 → 运营可访问</li>
 * <li>其他所有 /api/admin/** → 默认仅超管（最小权限原则）</li>
 * </ol>
 *
 * @author 胡宪棋
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminJwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    /** 存储当前管理员信息的Attribute Key */
    public static final String ADMIN_ID_ATTR = "currentAdminId";
    public static final String ADMIN_USERNAME_ATTR = "currentAdminUsername";
    public static final String ADMIN_ROLE_ATTR = "currentAdminRole";

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    /**
     * 默认运营管理员可见的Controller白名单（精确匹配Controller类名）
     * <p>
     * 这些Controller里的无 @RequireSuperAdmin 注解的方法对运营+超管均开放。
     * <p>
     * 被 @RequireSuperAdmin 标记的方法对运营仍拦截（最高优先级）。
     * <p>
     * 关键修复：AdminFileController 移出白名单（修复 operator 通过 /api/admin/file/overview 越权问题，
     * 配合前端 router /files needSuper + sidebar v-permission 双重屏蔽）。
     */
    private static final Set<String> DEFAULT_OPERATOR_ALLOWED = Set.of(
            "AdminDashboardController", // 后台总看板
            "AdminUserController", // 用户管理（冻结/解冻/导出）
            "AdminBillController", // 账单管理（仅读 + 导出）
            "AdminLogController", // 操作日志（仅查自己）
            "AdminCategoryController", // 分类管理（仅查，POST/PUT/DELETE 已 @RequireSuperAdmin）
            "AdminBudgetController", // 预算目标（仅查，PUT 已 @RequireSuperAdmin）
            "AdminAuthController" // 认证/当前管理员信息（登录、info等，非 @RequireSuperAdmin 方法对运营开放）
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
            Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeError(response, 401, "请先登录管理员账号");
            return false;
        }

        String token = authHeader.substring(7);
        try {
            if (!jwtUtil.validateToken(token)) {
                writeError(response, 401, "登录已过期，请重新登录");
                return false;
            }
            if (!jwtUtil.isAdminToken(token)) {
                writeError(response, 403, "请使用管理员账号登录");
                return false;
            }

            Long adminId = jwtUtil.getAdminIdFromToken(token);
            String username = jwtUtil.getUsernameFromToken(token);
            String role = jwtUtil.getRoleFromToken(token);

            // === 角色权限判断 ===
            if (!hasPermission(handler, role)) {
                writeError(response, 403, "需要超级管理员权限");
                return false;
            }

            // 注入上下文
            request.setAttribute(ADMIN_ID_ATTR, adminId);
            request.setAttribute(ADMIN_USERNAME_ATTR, username);
            request.setAttribute(ADMIN_ROLE_ATTR, role);

            return true;
        } catch (ExpiredJwtException e) {
            writeError(response, 401, "登录已过期，请重新登录");
            return false;
        } catch (Exception e) {
            log.error("管理员JWT校验异常: ", e);
            writeError(response, 401, "令牌无效");
            return false;
        }
    }

    /**
     * 判断当前角色是否有权限访问
     * <p>
     * 优先级：@RequireSuperAdmin 注解 > @RequireRole 注解 > Controller 默认白名单 > 默认仅超管
     * <p>
     * 关键修复：使用精确的注解/Controller 路径判断，避免 startsWith 误匹配。
     */
    private boolean hasPermission(Object handler, String role) {
        // 超管直接放行
        if ("SUPER_ADMIN".equals(role)) {
            return true;
        }
        // 非超管：检查方法上的注解与 Controller 白名单
        if (handler instanceof HandlerMethod hm) {
            // 1. @RequireSuperAdmin：最严格的标记，等价 SUPER_ADMIN only
            if (hm.getMethodAnnotation(RequireSuperAdmin.class) != null) {
                return false;
            }
            // 2. @RequireRole：按注解 value 列表匹配
            RequireRole ann = hm.getMethodAnnotation(RequireRole.class);
            if (ann != null) {
                for (String allowed : ann.value()) {
                    if (allowed.equalsIgnoreCase(role)) {
                        return true;
                    }
                }
                return false;
            }
            // 3. 方法无注解：检查 Controller 类是否在运营白名单（精确匹配）
            String controllerName = hm.getBeanType().getSimpleName();
            if (DEFAULT_OPERATOR_ALLOWED.contains(controllerName)) {
                return true;
            }
        }
        // 默认：仅超管
        return false;
    }

    private void writeError(HttpServletResponse response, int httpStatus, String message) throws Exception {
        response.setStatus(httpStatus);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                Result.fail(httpStatus == 401 ? 10004 : 60001, message)));
    }
}