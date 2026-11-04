package com.rg.app.config;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 从 Sa-Token 的登录态派生当前用户 ID，写入响应头 X-User-Id 和 MDC。
 * <p>
 * 注意：只使用服务端 Sa-Token 会话里的 ID，绝不读取客户端传入的
 * X-User-Id，避免身份伪造。
 */
public class UserIdentityInterceptor implements HandlerInterceptor {

    public static final String USER_ID_HEADER = "X-User-Id";
    private static final String MDC_USER_ID = "userId";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (StpUtil.isLogin()) {
            String userId = String.valueOf(StpUtil.getLoginIdAsLong());
            response.setHeader(USER_ID_HEADER, userId);
            MDC.put(MDC_USER_ID, userId);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        MDC.remove(MDC_USER_ID);
    }
}
