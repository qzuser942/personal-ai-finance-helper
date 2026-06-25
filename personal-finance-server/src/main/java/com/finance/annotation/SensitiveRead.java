package com.finance.annotation;

import java.lang.annotation.*;

/**
 * 敏感读操作 - 标记需要记录审计日志的读取接口
 * <p>使用场景：文件概览、账号列表、日志导出、AI分析记录查询等
 *
 * @author 胡宪棋
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SensitiveRead {

    /** 操作描述 */
    String value() default "";
}
