package com.rg.common.exception;

import com.rg.common.util.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理器 —— 把抛出的异常翻译成「HTTP 状态码 + Result 体」。
 * <p>
 * <b>契约：HTTP 状态码就等于 {@code Result.code}，两者永远一致。</b>
 * 这一点和之前的版本相反（以前一律 HTTP 200、状态只写在 body 里）。改成真状态码的原因：
 * 网关、nginx、监控、浏览器 fetch 都是按 HTTP 状态码决策的，业务码藏在 body 里它们看不见 ——
 * 一个 401 和一个 200 在日志里长得一模一样，排查和告警都没法做。
 * <p>
 * 由此推出一条约束：<b>业务码必须落在 4xx/5xx 里</b>。想表达「成功但不是 200」用
 * {@link Result#success(int, String, Object)}；想表达「失败的原因细分」，请用 msg 而不是编个
 * 1001 这种非 HTTP 码 —— 那样 {@link #toHttpStatus} 只能兜底成 400，body 和状态码就对不上了。
 * <p>
 * 认证（401）与授权（403）里由 <b>Spring Security 过滤器</b>抛出的那部分不走这里，走
 * {@code RestAuthenticationEntryPoint} / {@code RestAccessDeniedHandler}；
 * 而 {@code @PreAuthorize} 在 Controller 方法内抛出的越权异常会被本类下面的
 * {@code SecurityExceptionAdvice}（由各服务按需处理）接住；服务自定义业务异常统一放在
 * {@code com.rg.common.exception.serviceException}。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ──────────────────────────────────────────────
    // 业务异常（Service 层手动抛出的已知业务错误）
    // ──────────────────────────────────────────────

    /**
     * 状态码取自 {@link BusinessException#getCode()}。
     * <p>
     * 于是 {@code new UserException(403, "账号已被禁用")} 会真的回 HTTP 403，
     * {@code new UserException("邮箱已被占用")}（默认 400）会真的回 HTTP 400。
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusiness(BusinessException e) {
        HttpStatusCode status = toHttpStatus(e.getCode());
        log.warn("业务异常 [{}]: {}", status.value(), e.getMessage());
        return body(status, e.getMessage());
    }


    // ──────────────────────────────────────────────
    // 参数校验失败（@Valid / @Validated 未通过）
    // ──────────────────────────────────────────────

    /**
     * 请求体上的 {@code @NotBlank} / {@code @Email} / {@code @Size} 等没通过时抛这个。
     * <p>
     * ⚠️ 没有这个处理器的话会掉进下面的兜底分支变成 500「服务器内部错误」——
     * 客户端看到的是"服务炸了"而不是"你参数填错了"，DTO 上的校验注解等于白写。
     * <p>
     * 只取第一条字段错误的 message：校验注解上都写了中文提示，透出去比拼接一堆更有用。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(m -> m != null && !m.isBlank())
                .findFirst()
                .orElse(ErrorCode.BAD_REQUEST.getDefaultMessage());
        log.warn("参数校验失败: {}", message);
        return body(HttpStatus.BAD_REQUEST, message);
    }


    // ──────────────────────────────────────────────
    // 请求本身是坏的（body 不是合法 JSON、路径变量类型不对）
    // ──────────────────────────────────────────────

    /**
     * 请求体读不出来：不是合法 JSON、body 为空但参数是 {@code @RequestBody} 必填、字段类型对不上等。
     * <p>
     * 不接住的话它掉进兜底变 500 —— 但客户端明明只是发了个坏请求。这类错误在日志里和真正的
     * 服务端故障长得一样，会把真问题淹掉，所以必须单独归到 400。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleUnreadableBody(HttpMessageNotReadableException e) {
        log.warn("请求体无法解析: {}", e.getMessage());
        return body(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST.getDefaultMessage());
    }

    /**
     * 路径变量 / 查询参数的类型转换失败，例如 {@code GET /api/user/abc}（id 字段是 Long）。
     * <p>
     * 同样是"调用方把 URL 写错了"，给 400。注意异常里的 {@code value} 会进日志但<b>不回显给客户端</b>：
     * 把用户输入原样拼进响应体是反射型 XSS 的经典入口。
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("参数类型不匹配: name={}, value={}", e.getName(), e.getValue());
        return body(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST.getDefaultMessage());
    }


    // ──────────────────────────────────────────────
    // 静态资源不存在
    // ──────────────────────────────────────────────

    /**
     * 没匹配到任何 Controller、也没找到静态资源时抛这个（favicon.ico 是最常见的来源）。
     * <p>
     * 回真 404 —— 以前它会掉进兜底变成 500，语义完全错（客户端没问题，是资源不存在）。
     * favicon 的请求不打日志：浏览器每次开页面都要一次，记下来只会把日志刷满。
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNoResource(NoResourceFoundException e,
                                                         HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null || !path.contains("favicon.ico")) {
            log.warn("资源不存在: {}", path);
        }
        return body(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND.getDefaultMessage());
    }


    // ──────────────────────────────────────────────
    // 兜底 —— 未知异常
    // ──────────────────────────────────────────────

    /** 没被上面任何一条接住的异常。记完整栈，但只把一句无信息量的提示透给客户端 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {
        log.error("系统异常", e);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR.getDefaultMessage());
    }


    // ──────────────────────────────────────────────
    // 私有辅助
    // ──────────────────────────────────────────────

    /** 统一出口：HTTP 状态与 body 里的 code 强制取同一个值，杜绝两者漂移 */
    private static ResponseEntity<Result<Void>> body(HttpStatusCode status, String message) {
        return ResponseEntity.status(status).body(Result.error(status.value(), message));
    }

    /**
     * 把业务码映射成合法 HTTP 状态码。
     * <p>
     * 只认 4xx/5xx。0、200、1001 这类值直接交给 Tomcat 写会被拒（甚至抛 IllegalArgumentException），
     * 所以统一兜底：≥500 归 500，其余归 400。
     */
    private static HttpStatusCode toHttpStatus(int code) {
        HttpStatus status = HttpStatus.resolve(code);
        if (status != null && status.isError()) {
            return status;
        }
        return code >= 500 ? HttpStatus.INTERNAL_SERVER_ERROR : HttpStatus.BAD_REQUEST;
    }
}
