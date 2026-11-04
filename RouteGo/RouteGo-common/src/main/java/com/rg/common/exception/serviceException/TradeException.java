package com.rg.common.exception.serviceException;

import com.rg.common.exception.BusinessException;
import com.rg.common.exception.ErrorCode;

/**
 * trade 服务业务异常。
 * <p>
 * 默认 HTTP 400，可传入 {@link ErrorCode} 或 4xx/5xx 状态码和业务提示。
 */
public class TradeException extends BusinessException {

    public TradeException(String message) {
        super(message);
    }

    public TradeException(int code, String message) {
        super(code, message);
    }

    public TradeException(ErrorCode errorCode) {
        super(errorCode);
    }

    public TradeException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}

