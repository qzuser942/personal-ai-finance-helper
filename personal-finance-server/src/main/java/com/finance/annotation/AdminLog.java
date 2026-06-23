package com.finance.annotation;

import java.lang.annotation.*;

/**
 * 管理员操作日志注解 - 标记需要记录操作日志的Controller方法
 *
 * @author 胡宪棋
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AdminLog {

    /** 操作类型描述（如：删除账单、冻结用户） */
    String value() default "";
}
