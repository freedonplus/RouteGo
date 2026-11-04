package com.rg.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要写入操作日志的 Controller 方法。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperLog {

    /** 操作模块，如“用户管理”。 */
    String title();

    /** 业务类型。 */
    BusinessType type() default BusinessType.OTHER;
}
