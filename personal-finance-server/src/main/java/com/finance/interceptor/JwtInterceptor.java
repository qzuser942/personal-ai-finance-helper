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
 * 普通用户JWT拦截器 - 校验 /api/** 请求
 *
 * @author 胡宪棋
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    /** 存储当前请求用户ID的Attribute Key */
    public static final String USER_ID_ATTR = "currentUserId";
    public static final String USERNAME_ATTR = "currentUsername";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        // OPTIONS 预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeError(response, 401, "未登录，请先登录");
            return false;
        }

        String token = authHeader.substring(7);
        try {
            if (!jwtUtil.validateToken(token)) {
                writeError(response, 401, "登录已过期，请重新登录");
                return false;
            }

            // 校验是否为普通用户Token
            if (jwtUtil.isAdminToken(token)) {
                writeError(response, 403, "请使用普通用户账号登录");
                return false;
            }

            // 注入用户上下文
            Long userId = jwtUtil.getUserIdFromToken(token);
            String username = jwtUtil.getUsernameFromToken(token);
            request.setAttribute(USER_ID_ATTR, userId);
            request.setAttribute(USERNAME_ATTR, username);

            return true;
        } catch (ExpiredJwtException e) {
            writeError(response, 401, "登录已过期，请重新登录");
            return false;
        } catch (Exception e) {
            log.error("JWT校验异常: ", e);
            writeError(response, 401, "令牌无效");
            return false;
        }
    }

    private void writeError(HttpServletResponse response, int httpStatus, String message) throws Exception {
        response.setStatus(httpStatus);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                Result.fail(httpStatus == 401 ? 10004 : 90003, message)));
    }
}
