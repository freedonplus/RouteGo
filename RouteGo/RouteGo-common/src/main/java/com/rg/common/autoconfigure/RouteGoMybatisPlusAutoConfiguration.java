package com.rg.common.autoconfigure;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 通用能力：
 * <ul>
 *   <li>分页拦截器（MySQL，单页上限 500）</li>
 *   <li>乐观锁拦截器（实体存在 {@code @Version} 字段时生效）</li>
 *   <li>createTime / updateTime 自动填充（不依赖实体上的 fill 注解）</li>
 * </ul>
 * 逻辑删除通过各服务的 {@code mybatis-plus.global-config.db-config} 配置启用。
 */
@AutoConfiguration(after = DataSourceAutoConfiguration.class)
@ConditionalOnClass({MybatisPlusInterceptor.class, SqlSessionFactory.class})
@ConditionalOnProperty(prefix = "routego.mybatis-plus", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RouteGoMybatisPlusAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        pagination.setMaxLimit(500L);
        pagination.setOverflow(false);
        interceptor.addInnerInterceptor(pagination);

        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }

    @Bean
    @ConditionalOnMissingBean
    public MetaObjectHandler routeGoMetaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                LocalDateTime now = LocalDateTime.now();
                fillIfNull(metaObject, "createTime", now);
                fillIfNull(metaObject, "updateTime", now);
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                fillIfNull(metaObject, "updateTime", LocalDateTime.now());
            }

            private void fillIfNull(MetaObject metaObject, String field, LocalDateTime value) {
                if (metaObject.hasSetter(field) && metaObject.getValue(field) == null) {
                    metaObject.setValue(field, value);
                }
            }
        };
    }
}
