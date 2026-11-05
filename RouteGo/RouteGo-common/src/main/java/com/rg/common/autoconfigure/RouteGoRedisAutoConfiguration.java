package com.rg.common.autoconfigure;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rg.common.redis.RedisService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * RedisTemplate JSON 序列化。
 * <p>
 * 使用独立的 {@code routeGoRedisTemplate} + {@code @Primary}，避免与
 * Redisson starter / Spring Boot 自带模板产生 Bean 名称与泛型冲突。
 * key 使用字符串，value 使用带类型信息的 JSON，支持 LocalDateTime。
 */
@AutoConfiguration
@ConditionalOnClass({RedisTemplate.class, RedisConnectionFactory.class})
@ConditionalOnProperty(prefix = "routego.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RouteGoRedisAutoConfiguration {

    public static final String REDIS_TEMPLATE_BEAN_NAME = "routeGoRedisTemplate";

    @Bean(name = REDIS_TEMPLATE_BEAN_NAME)
    @Primary
    @ConditionalOnMissingBean(name = REDIS_TEMPLATE_BEAN_NAME)
    public RedisTemplate<String, Object> routeGoRedisTemplate(RedisConnectionFactory connectionFactory) {
        ObjectMapper mapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        // 多态反序列化白名单：仅放行业务包与 JDK 常用类型，防止恶意 payload 触发 gadget 链
        PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .allowIfSubType("com.rg.")
                .allowIfSubType("java.util.")
                .allowIfSubType("java.time.")
                .allowIfSubType("java.lang.")
                .allowIfSubType("java.math.")
                .build();
        mapper.activateDefaultTyping(typeValidator,
                ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);

        GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer(mapper);
        StringRedisSerializer keySerializer = new StringRedisSerializer();

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(keySerializer);
        template.setHashKeySerializer(keySerializer);
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    @ConditionalOnMissingBean
    public RedisService redisService(
            @Qualifier(REDIS_TEMPLATE_BEAN_NAME) RedisTemplate<String, Object> redisTemplate,
            StringRedisTemplate stringRedisTemplate) {
        return new RedisService(redisTemplate, stringRedisTemplate);
    }
}
