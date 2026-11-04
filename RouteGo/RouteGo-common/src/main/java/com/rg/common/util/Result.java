package com.rg.common.util;

import com.rg.common.exception.BusinessException;

/**
 * 统一返回体 —— {code, msg, data}。
 * <p>
 * <b>约定：出错时这里的 {@code code} 必须等于 HTTP 状态码。</b> 这条由
 * {@link com.rg.common.exception.GlobalExceptionHandler} 强制执行（它把同一个状态码既写进
 * {@code Result}、又写进响应行），所以业务异常里给的码要是合法的 4xx/5xx ——
 * 详见 {@link BusinessException}。
 * <p>
 * 成功路径仍然是 200，与 HTTP 200 天然一致，无需额外处理。
 */
public class Result<T> {

    private int code;
    private String msg;
    private T data;

    // ──────────────────────────────────────────────
    // 构造（private，不对外暴露）
    // ──────────────────────────────────────────────

    private Result() {}

    private Result(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    // ──────────────────────────────────────────────
    // 成功
    // ──────────────────────────────────────────────

    public static <T> Result<T> success() {
        return new Result<>(200, "success", null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }

    public static <T> Result<T> success(String msg, T data) {
        return new Result<>(200, msg, data);
    }

    /**
     * 自定义成功码。
     * <p>
     * ⚠️ 只用 2xx（201 已创建、206 部分成功…）。{@link #isSuccess()} 认的是 2xx 区间，
     * 你要是填个 1000 这种「非 2xx 的成功码」，序列化出去会带着 {@code "success":false}，
     * 调用方按 success 判断就会当成失败。
     */
    public static <T> Result<T> success(int code, String msg, T data) {
        return new Result<>(code, msg, data);
    }

    // ──────────────────────────────────────────────
    // 失败
    // ──────────────────────────────────────────────

    public static <T> Result<T> error(int code, String msg) {
        return new Result<>(code, msg, null);
    }

    public static <T> Result<T> error(String msg) {
        return new Result<>(500, msg, null);
    }

    public static <T> Result<T> error() {
        return new Result<>(500, "服务器内部错误", null);
    }

    // ──────────────────────────────────────────────
    // 便捷判断
    // ──────────────────────────────────────────────

    /**
     * 是否成功。按 2xx 区间判断，这样 {@link #success(int, String, Object)} 传 201 之类
     * 的自定义成功码也能被认出来。
     * <p>
     * 这个方法同时是 JSON 里的 {@code success} 字段（Jackson 把 {@code isXxx()} 当属性读）
     * —— 所以它一旦判断错，前端拿到的 success 也是错的。
     */
    public boolean isSuccess() {
        return code >= 200 && code < 300;
    }

    // ──────────────────────────────────────────────
    // Getter / Setter
    // ──────────────────────────────────────────────

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "Result{code=" + code + ", msg='" + msg + "', data=" + data + "}";
    }
}
