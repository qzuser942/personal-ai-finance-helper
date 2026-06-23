package com.finance.exception;

import com.finance.ai.exception.AiServiceException;
import com.finance.utils.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;

/**
 * 全局异常处理器
 *
 * @author 胡宪棋
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("[业务异常] {} - {}: {}", request.getRequestURI(), e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * AI服务异常
     */
    @ExceptionHandler(AiServiceException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleAiServiceException(AiServiceException e, HttpServletRequest request) {
        String retryHint = e.isRetryable() ? "（可重试）" : "";
        log.warn("[AI异常] {} - {}: {} {}", request.getRequestURI(), e.getCode(), e.getMessage(), retryHint);
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 参数校验异常 (@Valid)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidException(MethodArgumentNotValidException e, HttpServletRequest request) {
        List<Result.FieldError> errors = new ArrayList<>();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            errors.add(Result.fieldError(fieldError.getField(), fieldError.getDefaultMessage()));
        }
        log.warn("[参数校验失败] {}: {}", request.getRequestURI(), errors);
        return Result.fail(ErrorCode.PARAM_ERROR.getCode(), ErrorCode.PARAM_ERROR.getMessage(), errors);
    }

    /**
     * 绑定参数异常
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBindException(BindException e, HttpServletRequest request) {
        List<Result.FieldError> errors = new ArrayList<>();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            errors.add(Result.fieldError(fieldError.getField(), fieldError.getDefaultMessage()));
        }
        log.warn("[参数绑定失败] {}: {}", request.getRequestURI(), errors);
        return Result.fail(ErrorCode.PARAM_ERROR.getCode(), ErrorCode.PARAM_ERROR.getMessage(), errors);
    }

    /**
     * 系统异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("[系统异常] {}: ", request.getRequestURI(), e);
        return Result.error("服务器内部错误: " + e.getMessage());
    }
}
