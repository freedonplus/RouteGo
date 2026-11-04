package com.rg.common.exception;

/**
 * 业务异常基类。
 * <p>
 * 在 Service 层抛出，由 GlobalExceptionHandler 统一捕获后返回 {@link com.rg.common.util.Result}。
 * <b>HTTP 状态码就等于这里的 code</b> —— 由 GlobalExceptionHandler 保证两者一致，
 * 抛 400 出去客户端收到的就是 400，不会被包装成 200。
 * <pre>{@code
 *     throw new BusinessException("库存不足");          // HTTP 400
 *     throw new BusinessException(409, "商品已下架");    // HTTP 409
 * }</pre>
 * ⚠️ 所以 code 必须是<b>合法且为错误态（4xx/5xx）的 HTTP 状态码</b>：填 200/201 这类成功码
 * 或 999 这种不存在的码，GlobalExceptionHandler 会把它降级成 400/500 —— 别指望自定义码。
 * 各服务的具体异常继承它（如 AdminException、TradeException），默认 code 取 400。
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    public BusinessException(String message) {
        super(message);
        this.code = 400;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

}
