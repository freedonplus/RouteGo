package com.rg.operations.support;

import cn.dev33.satoken.stp.StpUtil;

/**
 * 当前操作人。
 * <p>
 * 与 trade 域保持同一套约定：定时任务、补数脚本等非请求线程取不到 Sa-Token 上下文，
 * 这里统一兜底成「系统」，避免审计字段因为拿不到上下文而抛异常。
 */
public final class CurrentOperator {

    public static final String SYSTEM = "系统";

    private CurrentOperator() {
    }

    public static Long id() {
        try {
            return StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
        } catch (Exception e) {
            return null;
        }
    }

    public static String name() {
        try {
            if (!StpUtil.isLogin()) {
                return SYSTEM;
            }
            Object username = StpUtil.getSession().get("username");
            return username == null ? SYSTEM : username.toString();
        } catch (Exception e) {
            return SYSTEM;
        }
    }
}
