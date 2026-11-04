package com.rg.common.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;

import java.time.format.DateTimeFormatter;

/**
 * 全局 Jackson 约定。
 * <p>
 * 1. {@code Long}/{@code long} 统一序列化为字符串，避免前端 JavaScript 在
 *    超过 2^53 后丢精度（雪花 ID 尤其明显）。
 * 2. {@code LocalDateTime}/{@code LocalDate}/{@code LocalTime} 使用固定格式，
 *    前后端接口里不再出现数组形式的时间。
 * <p>
 * 反序列化侧保持宽松：Jackson 能把数字字符串还原成数值类型，因此
 * Long 转字符串不会影响服务间调用。
 */
@AutoConfiguration
@ConditionalOnClass(ObjectMapper.class)
@ConditionalOnProperty(prefix = "routego.jackson", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RouteGoJacksonAutoConfiguration {

    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final String DATE_PATTERN = "yyyy-MM-dd";
    public static final String TIME_PATTERN = "HH:mm:ss";

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer routeGoJacksonCustomizer() {
        DateTimeFormatter dateTime = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
        DateTimeFormatter date = DateTimeFormatter.ofPattern(DATE_PATTERN);
        DateTimeFormatter time = DateTimeFormatter.ofPattern(TIME_PATTERN);

        return builder -> {
            builder.serializerByType(Long.class, ToStringSerializer.instance);
            builder.serializerByType(Long.TYPE, ToStringSerializer.instance);

            builder.serializers(new LocalDateTimeSerializer(dateTime));
            builder.deserializers(new LocalDateTimeDeserializer(dateTime));
            builder.serializers(new LocalDateSerializer(date));
            builder.deserializers(new LocalDateDeserializer(date));
            builder.serializers(new LocalTimeSerializer(time));
            builder.deserializers(new LocalTimeDeserializer(time));

            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            builder.failOnUnknownProperties(false);
        };
    }
}
