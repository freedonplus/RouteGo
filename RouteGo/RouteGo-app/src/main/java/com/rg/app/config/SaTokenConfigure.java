package com.rg.app.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 拦截器：deny-by-default 鉴权模型。
 * <p>
 * 除下方白名单外，所有接口默认要求登录；{@code @SaCheckPermission} /
 * {@code @SaCheckRole} 注解在登录校验之上继续生效。新增公开接口时必须
 * 显式加入白名单并说明理由，避免接口意外裸奔。
 */
@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 登录前必须可访问的接口 + 基础设施端点
        registry.addInterceptor(new SaInterceptor(handle -> {
                    // CORS 预检不携带凭证，统一放行，交给 MVC 层的 CORS 配置处理；
                    // 注意 handle 是 HandlerMethod 而非 HttpServletRequest，方法只能从 SaHolder 取
                    if ("OPTIONS".equalsIgnoreCase(SaHolder.getRequest().getMethod())) {
                        return;
                    }
                    StpUtil.checkLogin();
                }))
                .addPathPatterns("/**")
                .excludePathPatterns(
                        // 登录认证入口
                        "/admin/auth/captcha",
                        "/admin/auth/login",
                        // 注册入口（手机号验证码注册，内部限流且验证码即凭证）
                        "/admin/auth/register",
                        // 验证码发送与验证码登录（本身即认证入口，内部有限流）
                        "/admin/auth/code",
                        "/admin/auth/login/code",
                        // 第三方登录：取授权地址与平台回调都是未登录态必经路径
                        "/admin/auth/oauth/providers",
                        "/admin/auth/oauth/*/url",
                        "/admin/auth/oauth/*/callback",
                        // C 端 App 接口：使用独立的 customer 账号体系（StpKit.CUSTOMER），
                        // 不走后台 StpUtil；需要登录的接口在方法内显式 checkLogin
                        "/app/**",
                        // 图片等静态资源：线路封面/图集由后端随包提供，本身就是公开内容
                        "/covers/**",
                        // 运维与可观测端点（独立网关/内网访问）
                        "/actuator/**",
                        // API 文档（routego.openapi.enabled=false 时这些路径本身 404）
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        // Spring 错误转发路径
                        "/error")
                .order(0);
        // 登录后从 Sa-Token 派生 X-User-Id，不读取客户端传入的同名头
        registry.addInterceptor(new UserIdentityInterceptor()).addPathPatterns("/**").order(1);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
