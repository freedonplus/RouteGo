package com.rg.common.exception.serviceException;

import com.rg.common.exception.BusinessException;
import com.rg.common.exception.ErrorCode;

/**
 * admin 服务业务异常。
 * <p>
 * 默认 HTTP 400，可传入 {@link ErrorCode} 或 4xx/5xx 状态码和业务提示。
 */
public class AdminException extends BusinessException {

    public AdminException(String message) {
        super(message);
    }

    public AdminException(int code, String message) {
        super(code, message);
    }

    public AdminException(ErrorCode errorCode) {
        super(errorCode);
    }

    public AdminException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}

