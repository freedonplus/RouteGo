package com.rg.common.exception;

/**
 * 统一错误码。
 * <p>
 * 约定：Result.code 与 HTTP 状态码保持一致，统一取 4xx/5xx。
 * 细化业务失败原因请通过 message 描述，不要再自造 1001 这类非 HTTP 状态码。
 */
public enum ErrorCode {

    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未认证"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "资源状态冲突"),
    TOO_MANY_REQUESTS(429, "操作过于频繁"),
    INTERNAL_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务暂不可用，请稍后重试");

    private final int code;
    private final String defaultMessage;

    ErrorCode(int code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public int getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
