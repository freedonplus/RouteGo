package com.rg.app.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 注解鉴权。
 * <p>
 * 只对标注了 {@code @SaCheckLogin} / {@code @SaCheckPermission} /
 * {@code @SaCheckRole} 的接口生效，未标注的接口默认放行。
 */
@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 只校验 @SaCheckLogin / @SaCheckPermission / @SaCheckRole
        registry.addInterceptor(new SaInterceptor()).addPathPatterns("/**").order(0);
        // 登录后从 Sa-Token 派生 X-User-Id，不读取客户端传入的同名头
        registry.addInterceptor(new UserIdentityInterceptor()).addPathPatterns("/**").order(1);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
