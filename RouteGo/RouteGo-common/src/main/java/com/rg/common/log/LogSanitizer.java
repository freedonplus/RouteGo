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
    /** 进入正则前先截断，避免超长输入触发回溯开销。 */
    private static final int MAX_SCAN_LENGTH = 10000;

    private static final String SENSITIVE_WORDS =
            "password|passwd|pwd|token|secret|phone|idcard|bankcard|credential";

    /**
     * 匹配“字段名包含敏感词”的 JSON 字符串值。字段名长度受限、量词使用
     * possessive 形式，值支持转义引号并以 JSON 分隔符收尾，避免回溯与误吞下一个字段。
     */
    private static final Pattern SENSITIVE_JSON_FIELD = Pattern.compile(
            "(\"(?=[^\"]{0,64}(?:" + SENSITIVE_WORDS + "))[^\"]{0,64}\"\\s*:\\s*)"
                    + "\"(?:\\\\.|[^\"\\\\])*+\"(?=\\s*[,}\\]\\s]|\\s*$)",
            Pattern.CASE_INSENSITIVE);

    /**
     * 非 JSON 文本里的 key=value / key: value 形式；负向后行避免误伤 JSON 键。
     */
    private static final Pattern SENSITIVE_PLAIN_FIELD = Pattern.compile(
            "(?i)(?<![\\w\"])(password|passwd|pwd|token|secret|api[_-]?key|access[_-]?key"
                    + "|authorization|cookie|session|salt)\\s*[=:]\\s*[^\\s,;\"']+");

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
        String bounded = text.length() > MAX_SCAN_LENGTH ? text.substring(0, MAX_SCAN_LENGTH) : text;
        String masked = SENSITIVE_JSON_FIELD.matcher(bounded).replaceAll("$1\"***\"");
        masked = SENSITIVE_PLAIN_FIELD.matcher(masked).replaceAll("$1=***");
        return masked.length() <= maxLength ? masked : masked.substring(0, maxLength);
    }
}
