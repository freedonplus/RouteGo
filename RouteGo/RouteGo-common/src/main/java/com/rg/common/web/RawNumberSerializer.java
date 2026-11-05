package com.rg.common.web;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * 强制按 JSON 数值输出的整型序列化器。
 * <p>
 * 全局约定把 {@code Long}/{@code long} 序列化为字符串（防雪花 ID 精度丢失），
 * 分页的 {@code total/pageNum/pageSize/pages} 需要保持数值类型，因此需要在字段上覆盖。
 * <p>
 * 这里<b>不能</b>直接用 Jackson 自带的 {@code NumberSerializer}：它只有一个
 * {@code NumberSerializer(Class<? extends Number>)} 构造器，而 Spring Boot 的
 * {@code SpringHandlerInstantiator} 会用 Spring 容器实例化注解声明的序列化器，
 * 容器里没有 {@code Class<Number>} 类型的 bean，于是即使业务成功，写响应时也会抛
 * {@code HttpMessageNotWritableException}（Unsatisfied dependency ... Class<java.lang.Number>）。
 * 本类提供无参构造，容器可正常创建，也可在独立 ObjectMapper 下直接反射创建。
 */
public class RawNumberSerializer extends JsonSerializer<Number> {

    @Override
    public void serialize(Number value, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
        if (value == null) {
            gen.writeNull();
        } else {
            gen.writeNumber(value.longValue());
        }
    }
}
