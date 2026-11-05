package com.rg.trade.support;

import cn.dev33.satoken.stp.StpUtil;

/**
 * 当前操作人，用于状态流水与操作日志。
 * <p>
 * 交易域会被定时任务、补偿脚本等非请求线程调用，此时 Sa-Token 上下文不存在，
 * 这里统一兜底成「系统」，不要让审计字段因为拿不到上下文而报错。
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
