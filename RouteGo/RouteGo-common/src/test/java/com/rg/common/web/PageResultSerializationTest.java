package com.rg.common.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rg.common.autoconfigure.RouteGoJacksonAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.SpringHandlerInstantiator;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 分页返回体的序列化约定。
 * <p>
 * 这是线上踩过的坑：{@code @JsonSerialize(using = NumberSerializer.class)} 会被
 * Spring 容器实例化逻辑判定为需要注入 {@code Class<Number>}，导致所有分页接口
 * 在写响应时抛 {@code HttpMessageNotWritableException}。这里锁住两条不变量：
 * 分页字段是数值、ID 类 Long 仍是字符串。
 */
class PageResultSerializationTest {

    private final ObjectMapper mapper = buildMapper();

    private static ObjectMapper buildMapper() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        new RouteGoJacksonAutoConfiguration().routeGoJacksonCustomizer().customize(builder);
        return builder.build();
    }

    @Test
    void pageFieldsAreNumbersWhileLongIdsStayStrings() throws Exception {
        PageResult<Map<String, Object>> result = new PageResult<>(
                List.of(Map.of("id", 1234567890123456789L)), 3L, 1L, 10L, 1L);

        String json = mapper.writeValueAsString(result);

        assertThat(json).contains("\"total\":3");
        assertThat(json).contains("\"pageNum\":1");
        assertThat(json).contains("\"pageSize\":10");
        assertThat(json).contains("\"pages\":1");
        // 全局约定：Long 仍按字符串输出，防止前端精度丢失
        assertThat(json).contains("\"id\":\"1234567890123456789\"");
    }

    @Test
    void emptyPageSerializesWithoutError() throws Exception {
        String json = mapper.writeValueAsString(new PageResult<>(List.of(), 0L, 1L, 10L, 0L));

        assertThat(json).contains("\"total\":0").contains("\"records\":[]");
    }

    /**
     * 复现线上环境：Spring 通过 SpringHandlerInstantiator 用容器创建注解声明的序列化器。
     * 若字段上误用 {@code NumberSerializer}，这里会抛 UnsatisfiedDependencyException。
     */
    @Test
    void serializerCanBeInstantiatedBySpringContainer() throws Exception {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        ObjectMapper springMapper = buildMapper();
        springMapper.setHandlerInstantiator(new SpringHandlerInstantiator(beanFactory));

        String json = springMapper.writeValueAsString(new PageResult<>(List.of(), 7L, 1L, 10L, 1L));

        assertThat(json).contains("\"total\":7");
    }
}
