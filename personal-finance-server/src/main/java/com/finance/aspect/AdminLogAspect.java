package com.finance.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.annotation.AdminLog;
import com.finance.entity.AdminOperationLog;
import com.finance.interceptor.AdminJwtInterceptor;
import com.finance.service.AdminOperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 管理员操作日志AOP切面
 * 自动记录所有 /api/admin/** 下 POST/PUT/DELETE 请求
 *
 * @author 胡宪棋
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AdminLogAspect {

    private final AdminOperationLogService adminOperationLogService;
    private final ObjectMapper objectMapper;

    /**
     * 切点：所有管理员Controller
     */
    @Pointcut("execution(* com.finance.controller.admin..*.*(..))")
    public void adminControllerPointcut() {}

    @Around("adminControllerPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = attributes.getRequest();
        String method = request.getMethod();

        // 仅记录 POST/PUT/DELETE
        if (!"POST".equalsIgnoreCase(method)
                && !"PUT".equalsIgnoreCase(method)
                && !"DELETE".equalsIgnoreCase(method)) {
            return joinPoint.proceed();
        }

        // 排除登录接口
        if (request.getRequestURI().contains("/login")) {
            return joinPoint.proceed();
        }

        Long adminId = (Long) request.getAttribute(AdminJwtInterceptor.ADMIN_ID_ATTR);
        String username = (String) request.getAttribute(AdminJwtInterceptor.ADMIN_USERNAME_ATTR);

        AdminOperationLog operationLog = new AdminOperationLog();
        operationLog.setAdminId(adminId != null ? adminId : 0L);
        operationLog.setAdminUsername(username != null ? username : "unknown");
        operationLog.setMethod(method);
        operationLog.setRequestUrl(request.getRequestURI());
        operationLog.setIpAddress(getIpAddress(request));
        operationLog.setCreatedAt(LocalDateTime.now());

        // 获取操作描述（优先使用 @AdminLog 注解）
        AdminLog adminLog = getAdminLogAnnotation(joinPoint);
        if (adminLog != null && !adminLog.value().isEmpty()) {
            operationLog.setOperation(adminLog.value());
        } else {
            operationLog.setOperation(buildDefaultOperation(method, request.getRequestURI()));
        }

        // 记录请求参数（脱敏）
        try {
            Object[] args = joinPoint.getArgs();
            String paramsJson = objectMapper.writeValueAsString(args);
            // 截断过长参数
            if (paramsJson.length() > 2000) {
                paramsJson = paramsJson.substring(0, 2000) + "...";
            }
            operationLog.setRequestParams(paramsJson);
        } catch (Exception e) {
            operationLog.setRequestParams("参数序列化失败");
        }

        try {
            Object result = joinPoint.proceed();
            operationLog.setStatus(1);
            return result;
        } catch (Throwable e) {
            operationLog.setStatus(0);
            operationLog.setErrorMsg(e.getMessage() != null
                    ? e.getMessage().substring(0, Math.min(e.getMessage().length(), 1000))
                    : "未知错误");
            throw e;
        } finally {
            // 异步保存操作日志
            try {
                adminOperationLogService.save(operationLog);
            } catch (Exception e) {
                log.error("保存操作日志失败: ", e);
            }
        }
    }

    private AdminLog getAdminLogAnnotation(ProceedingJoinPoint joinPoint) {
        try {
            return joinPoint.getTarget().getClass()
                    .getMethod(joinPoint.getSignature().getName(),
                            ((org.aspectj.lang.reflect.MethodSignature) joinPoint.getSignature())
                                    .getParameterTypes())
                    .getAnnotation(AdminLog.class);
        } catch (Exception e) {
            return null;
        }
    }

    private String buildDefaultOperation(String method, String uri) {
        if (uri.contains("/bill")) {
            return switch (method.toUpperCase()) {
                case "POST" -> "新增账单";
                case "PUT" -> "修改账单";
                case "DELETE" -> "删除账单";
                default -> "账单操作";
            };
        } else if (uri.contains("/user")) {
            return switch (method.toUpperCase()) {
                case "PUT" -> "修改用户状态/信息";
                case "DELETE" -> "删除用户";
                default -> "用户管理";
            };
        } else if (uri.contains("/category")) {
            return switch (method.toUpperCase()) {
                case "POST" -> "新增分类";
                case "PUT" -> "修改分类";
                case "DELETE" -> "删除分类";
                default -> "分类管理";
            };
        } else if (uri.contains("/account")) {
            return switch (method.toUpperCase()) {
                case "POST" -> "新增管理员";
                case "PUT" -> "修改管理员";
                case "DELETE" -> "删除管理员";
                default -> "管理员账号管理";
            };
        }
        return "管理员操作";
    }

    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
