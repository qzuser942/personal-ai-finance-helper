package com.finance.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 统一返回体 Result<T>
 *
 * @author 胡宪棋
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> {

    /** 状态码 */
    private Integer code;

    /** 提示信息 */
    private String message;

    /** 返回数据 */
    private T data;

    /** 字段校验错误详情 */
    private List<FieldError> errors;

    /** 时间戳 */
    private Long timestamp;

    // ==================== 成功响应 ====================

    public static <T> Result<T> ok() {
        return new Result<>(200, "操作成功", null, null, System.currentTimeMillis());
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(200, "操作成功", data, null, System.currentTimeMillis());
    }

    public static <T> Result<T> ok(String message, T data) {
        return new Result<>(200, message, data, null, System.currentTimeMillis());
    }

    // ==================== 失败响应 ====================

    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null, null, System.currentTimeMillis());
    }

    public static <T> Result<T> fail(Integer code, String message, List<FieldError> errors) {
        return new Result<>(code, message, null, errors, System.currentTimeMillis());
    }

    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null, null, System.currentTimeMillis());
    }

    // ==================== 字段错误 ====================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldError {
        private String field;
        private String message;
    }

    public static FieldError fieldError(String field, String message) {
        return new FieldError(field, message);
    }
}
