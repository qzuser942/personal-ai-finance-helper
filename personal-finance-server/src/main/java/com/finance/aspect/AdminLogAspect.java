package com.finance.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.entity.AdminOperationLog;
import com.finance.interceptor.AdminJwtInterceptor;
import com.finance.service.AdminOperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 管理员操作日志AOP切面
 * <p>
 * 自动记录所有 /api/admin/** 下：
 * <ul>
 * <li>POST/PUT/DELETE 写操作</li>
 * <li>高敏读操作（带 @Sensitive 注解）</li>
 * </ul>
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

    @Pointcut("execution(* com.finance.controller.admin..*.*(..))")
    public void adminControllerPointcut() {
    }

    @Around("adminControllerPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = attributes.getRequest();
        String method = request.getMethod();

        // 排除登录接口 + 管理员信息自查询（/info 每次路由切换都会调用，不能污染审计日志）
        // 排除操作日志查询本身（避免运营查看日志时产生自引用日志）
        String uri = request.getRequestURI();
        if (uri.contains("/login") || uri.endsWith("/info") || uri.contains("/admin/log")) {
            return joinPoint.proceed();
        }

        // 仅记录写操作 或 高敏读操作
        boolean isWrite = "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method);
        boolean isSensitive = isSensitiveRead(joinPoint);
        if (!isWrite && !isSensitive) {
            return joinPoint.proceed();
        }

        Long adminId = (Long) request.getAttribute(AdminJwtInterceptor.ADMIN_ID_ATTR);
        String username = (String) request.getAttribute(AdminJwtInterceptor.ADMIN_USERNAME_ATTR);
        String role = (String) request.getAttribute(AdminJwtInterceptor.ADMIN_ROLE_ATTR);

        AdminOperationLog operationLog = new AdminOperationLog();
        operationLog.setAdminId(adminId != null ? adminId : 0L);
        operationLog.setAdminUsername(username != null ? username : "unknown");
        operationLog.setAdminRole(role);
        operationLog.setMethod(method);
        operationLog.setRequestUrl(request.getRequestURI());
        operationLog.setResourceId(extractResourceId(request.getRequestURI()));
        operationLog.setIpAddress(getIpAddress(request));
        operationLog.setCreatedAt(LocalDateTime.now());

        com.finance.annotation.AdminLog adminLog = getAdminLogAnnotation(joinPoint);
        if (adminLog != null && !adminLog.value().isEmpty()) {
            operationLog.setOperation(adminLog.value());
        } else {
            // 关键修复：@SensitiveRead 注解的 value 也作为操作描述（如"导出全平台账单"）
            com.finance.annotation.SensitiveRead sensitive = getSensitiveReadAnnotation(joinPoint);
            if (sensitive != null && !sensitive.value().isEmpty()) {
                operationLog.setOperation(sensitive.value());
            } else {
                operationLog.setOperation(buildDefaultOperation(method, request.getRequestURI()));
            }
        }

        // 记录请求参数（脱敏密码字段，过滤不可序列化的Servlet对象）
        try {
            Object[] args = joinPoint.getArgs();
            // 过滤掉 HttpServletRequest/HttpServletResponse 等不可序列化的参数
            List<Object> serializableArgs = new ArrayList<>();
            for (Object arg : args) {
                if (arg == null)
                    continue;
                if (arg instanceof HttpServletRequest || arg instanceof HttpServletResponse)
                    continue;
                serializableArgs.add(arg);
            }
            String paramsJson = objectMapper.writeValueAsString(serializableArgs);
            // 脱敏：密码字段统一替换
            paramsJson = paramsJson.replaceAll("\"(password|newPassword)\"\\s*:\\s*\"[^\"]*\"",
                    "\"$1\":\"******\"");
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
            try {
                adminOperationLogService.save(operationLog);
            } catch (Exception e) {
                log.error("保存操作日志失败: ", e);
            }
        }
    }

    private boolean isSensitiveRead(ProceedingJoinPoint joinPoint) {
        Method m = getMethod(joinPoint);
        return m != null && m.isAnnotationPresent(com.finance.annotation.SensitiveRead.class);
    }

    private Method getMethod(ProceedingJoinPoint joinPoint) {
        try {
            MethodSignature ms = (MethodSignature) joinPoint.getSignature();
            return joinPoint.getTarget().getClass()
                    .getMethod(ms.getName(), ms.getParameterTypes());
        } catch (Exception e) {
            return null;
        }
    }

    private com.finance.annotation.AdminLog getAdminLogAnnotation(ProceedingJoinPoint joinPoint) {
        Method m = getMethod(joinPoint);
        return m == null ? null : m.getAnnotation(com.finance.annotation.AdminLog.class);
    }

    private com.finance.annotation.SensitiveRead getSensitiveReadAnnotation(ProceedingJoinPoint joinPoint) {
        Method m = getMethod(joinPoint);
        return m == null ? null : m.getAnnotation(com.finance.annotation.SensitiveRead.class);
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

    private String extractResourceId(String uri) {
        // 从URL路径中提取资源ID，如 /api/admin/user/123/status → userId:123
        if (uri == null)
            return null;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("/(\\d+)(?:/|$)").matcher(uri);
        if (m.find()) {
            String id = m.group(1);
            if (uri.contains("/user"))
                return "userId:" + id;
            if (uri.contains("/bill"))
                return "billId:" + id;
            if (uri.contains("/category"))
                return "categoryId:" + id;
            if (uri.contains("/budget"))
                return "budgetId:" + id;
            if (uri.contains("/account"))
                return "adminId:" + id;
            if (uri.contains("/file"))
                return "fileId:" + id;
            return "resourceId:" + id;
        }
        return null;
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