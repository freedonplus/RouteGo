package com.rg.common.sensitive;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要脱敏输出的字符串字段。
 * <p>
 * 只影响 JSON 序列化结果，不影响内存中的真实值；因此可以安全地放在
 * 返回给前端的 VO 上，而缓存对象与数据库仍然保存明文。
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = SensitiveSerializer.class)
public @interface Sensitive {

    SensitiveType value();
}
