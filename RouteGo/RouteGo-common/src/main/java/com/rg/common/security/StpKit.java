package com.rg.common.security;

import cn.dev33.satoken.stp.StpLogic;

/**
 * Sa-Token 多账号体系。
 * <p>
 * 后端存在两套互不干扰的身份：
 * <ul>
 *   <li>管理后台：默认 {@code StpUtil}，loginType = login</li>
 *   <li>C 端会员：本类的 {@code CUSTOMER}，loginType = customer</li>
 * </ul>
 * 两套会话的 Token 存储、过期时间、并发策略完全独立：后台登录不会把 App 用户顶下线。
 * <p>
 * 关于请求头：Sa-Token 的 Token 名称取自全局 {@code sa-token.token-name}（本项目为
 * {@code Authorization}），两套体系共用同一个头。由于 Redis 里 Token 的 key 带 loginType
 * （{@code satoken:login:token:customer:xxx}），同一个头值在两套体系内各自查找、
 * 互不命中，不会出现 App 用户被当成后台用户的情况。
 */
public final class StpKit {

    /** C 端会员账号体系标识 */
    public static final String CUSTOMER_TYPE = "customer";

    /** C 端会员专用 StpLogic */
    public static final StpLogic CUSTOMER = new StpLogic(CUSTOMER_TYPE);

    private StpKit() {
    }

    /** 当前登录会员 ID；未登录抛 NotLoginException（全局异常处理转 401） */
    public static Long currentCustomerId() {
        return CUSTOMER.getLoginIdAsLong();
    }

    public static boolean isCustomerLogin() {
        return CUSTOMER.isLogin();
    }
}
