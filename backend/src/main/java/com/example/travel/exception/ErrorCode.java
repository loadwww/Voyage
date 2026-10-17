package com.example.travel.exception;

/**
 * 统一业务错误码。命名规则：领域_原因，全部大写下划线。
 */
public enum ErrorCode {
    PARAM_INVALID(400, "请求参数不合法"),
    VALIDATION_FAILED(400, "参数校验失败"),
    NOT_FOUND(404, "资源不存在"),
    IDEMPOTENCY_CONFLICT(409, "重复请求"),
    RATE_LIMITED(429, "请求过于频繁，请稍后再试"),
    INTERNAL_ERROR(500, "服务器内部错误");

    private final int httpStatus;
    private final String defaultMessage;

    ErrorCode(int httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public int httpStatus() {
        return httpStatus;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
