package com.rg.common.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.regex.Pattern;

/**
 * 日志参数脱敏与截断。
 */
public final class LogSanitizer {

    private static final int DEFAULT_MAX_LENGTH = 2000;

    private static final String SENSITIVE_WORDS =
            "password|passwd|pwd|token|secret|phone|idcard|bankcard|credential";

    /**
     * 匹配“字段名包含敏感词”的 JSON 字符串值；值部分支持转义引号，
     * 避免 \{"password":"p\"ss"\} 这类内容绕过脱敏。
     */
    private static final Pattern SENSITIVE_JSON_FIELD = Pattern.compile(
            "(\"[^\"]*(?:" + SENSITIVE_WORDS + ")[^\"]*\"\\s*:\\s*)\"(?:\\\\.|[^\"\\\\])*\"",
            Pattern.CASE_INSENSITIVE);

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build();

    private LogSanitizer() {
    }

    public static String toJson(Object value, int maxLength) {
        if (value == null) {
            return null;
        }
        try {
            return maskAndTruncate(MAPPER.writeValueAsString(value), maxLength);
        } catch (Exception e) {
            // 不用 toString 兜底：未知对象的 toString 可能带出密码/令牌
            return maskAndTruncate("<unserializable:" + value.getClass().getName() + ">", maxLength);
        }
    }

    public static String toJson(Object value) {
        return toJson(value, DEFAULT_MAX_LENGTH);
    }

    public static String maskAndTruncate(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        String masked = SENSITIVE_JSON_FIELD.matcher(text).replaceAll("$1\"***\"");
        return masked.length() <= maxLength ? masked : masked.substring(0, maxLength);
    }
}
