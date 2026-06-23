package com.finance.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.utils.JwtUtil;
import com.finance.utils.Result;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 管理员JWT拦截器 - 校验 /api/admin/** 请求
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

    /** 需要超级管理员权限的路径 */
    private static final String[] SUPER_ADMIN_PATHS = {
            "/api/admin/account",
            "/api/admin/ai/config",
            "/api/admin/ai/qdrant/reset",
            "/api/admin/file/clean",
            "/api/admin/database/backup",
            "/api/admin/database/backup/log"
    };

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

            // 校验是否为管理员Token
            if (!jwtUtil.isAdminToken(token)) {
                writeError(response, 403, "请使用管理员账号登录");
                return false;
            }

            Long adminId = jwtUtil.getAdminIdFromToken(token);
            String username = jwtUtil.getUsernameFromToken(token);
            String role = jwtUtil.getRoleFromToken(token);

            // 超级管理员路径权限校验
            String requestUri = request.getRequestURI();
            if (requiresSuperAdmin(requestUri) && !"SUPER_ADMIN".equals(role)) {
                writeError(response, 403, "需要超级管理员权限");
                return false;
            }

            // 注入管理员上下文
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

    private boolean requiresSuperAdmin(String requestUri) {
        for (String path : SUPER_ADMIN_PATHS) {
            if (requestUri.startsWith(path)) {
                return true;
            }
        }
        return false;
    }

    private void writeError(HttpServletResponse response, int httpStatus, String message) throws Exception {
        response.setStatus(httpStatus);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                Result.fail(httpStatus == 401 ? 10004 : 60001, message)));
    }
}
