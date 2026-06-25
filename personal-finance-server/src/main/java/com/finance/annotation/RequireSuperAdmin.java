package com.finance.annotation;

import java.lang.annotation.*;

/**
 * 仅超级管理员可访问的方法标记
 * <p>等价于 @RequireRole("SUPER_ADMIN")，但语义更清晰。
 * <p>AdminJwtInterceptor 在判断权限时优先识别此注解。
 *
 * <p>使用示例：
 * <pre>
 *   {@code @RequireSuperAdmin}   // 标记方法仅超级管理员可访问
 * </pre>
 *
 * @author 胡宪棋
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireSuperAdmin {
}
