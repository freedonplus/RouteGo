package com.rg.common.exception;

import cn.dev33.satoken.exception.DisableServiceException;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.rg.common.util.Result;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Sa-Token 异常统一转换为 HTTP 状态码 + Result 体。
 * <p>
 * 优先级高于全局兜底异常处理器，保证未登录/无权限不会被翻译成 500。
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class SaTokenExceptionHandler {

    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<Result<Void>> handleNotLogin(NotLoginException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Result.error(HttpStatus.UNAUTHORIZED.value(), "未登录或登录已过期"));
    }

    @ExceptionHandler({NotPermissionException.class, NotRoleException.class, DisableServiceException.class})
    public ResponseEntity<Result<Void>> handleNoPermission(Exception e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Result.error(HttpStatus.FORBIDDEN.value(), "无权限访问该资源"));
    }
}
