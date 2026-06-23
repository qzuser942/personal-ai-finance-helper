package com.finance.ai.exception;

import com.finance.exception.ErrorCode;
import lombok.Getter;

/**
 * AI服务异常
 * 封装LLM调用、向量嵌入、向量数据库等AI相关异常
 *
 * @author 胡宪棋
 */
@Getter
public class AiServiceException extends RuntimeException {

    /** 错误码 */
    private final Integer code;

    /** 是否可重试 */
    private final boolean retryable;

    public AiServiceException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.retryable = false;
    }

    public AiServiceException(ErrorCode errorCode, String detailMessage) {
        super(errorCode.getMessage() + (detailMessage != null ? "：" + detailMessage : ""));
        this.code = errorCode.getCode();
        this.retryable = false;
    }

    public AiServiceException(ErrorCode errorCode, String detailMessage, boolean retryable) {
        super(errorCode.getMessage() + (detailMessage != null ? "：" + detailMessage : ""));
        this.code = errorCode.getCode();
        this.retryable = retryable;
    }

    public AiServiceException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.code = errorCode.getCode();
        this.retryable = cause instanceof java.net.SocketTimeoutException
                || cause instanceof java.net.ConnectException
                || cause instanceof java.io.IOException;
    }
}
