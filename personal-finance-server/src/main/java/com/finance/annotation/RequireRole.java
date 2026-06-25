package com.finance.annotation;

import java.lang.annotation.*;

/**
 * 权限注解 - 标记Controller方法所需的角色
 *
 * <p>使用示例：
 * <pre>
 *   {@code @RequireSuperAdmin}                       // 仅超管
 *   {@code @RequireRole("SUPER_ADMIN","OPERATOR")}   // 多角色
 * </pre>
 *
 * @author 胡宪棋
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {

    /** 允许访问的角色列表（任一匹配即可） */
    String[] value() default {"SUPER_ADMIN", "OPERATOR"};
}
