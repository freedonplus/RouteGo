package com.rg.common.sensitive;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

import java.io.IOException;

/**
 * 按 {@link Sensitive} 上声明的类型输出掩码。
 */
public class SensitiveSerializer extends JsonSerializer<String> implements ContextualSerializer {

    private final SensitiveType type;

    public SensitiveSerializer() {
        this(SensitiveType.PHONE);
    }

    private SensitiveSerializer(SensitiveType type) {
        this.type = type;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(mask(value, type));
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) {
        Sensitive annotation = property == null ? null : property.getAnnotation(Sensitive.class);
        return annotation == null ? this : new SensitiveSerializer(annotation.value());
    }

    static String mask(String value, SensitiveType type) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return switch (type) {
            case PHONE -> value.length() < 7
                    ? value
                    : value.substring(0, 3) + "****" + value.substring(value.length() - 4);
            case EMAIL -> {
                int at = value.indexOf('@');
                yield at <= 0 ? value : value.charAt(0) + "***" + value.substring(at);
            }
            case NAME -> value.length() <= 1
                    ? value
                    : value.charAt(0) + "*".repeat(value.length() - 1);
            case ID_CARD -> value.length() <= 7
                    ? value
                    : value.substring(0, 3) + "*".repeat(value.length() - 7)
                            + value.substring(value.length() - 4);
            case BANK_CARD -> value.length() <= 4
                    ? value
                    : "**** **** **** " + value.substring(value.length() - 4);
        };
    }
}
