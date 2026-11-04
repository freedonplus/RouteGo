package com.rg.common.log;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LogSanitizerTest {

    @Test
    void masksSensitiveKeys() {
        String json = "{\"username\":\"a\",\"password\":\"p@ss\",\"phone\":\"13812348000\"}";

        String out = LogSanitizer.maskAndTruncate(json, 200);

        assertThat(out).doesNotContain("p@ss")
                .doesNotContain("13812348000")
                .contains("\"password\":\"***\"")
                .contains("\"phone\":\"***\"");
    }

    @Test
    void truncatesLongText() {
        assertThat(LogSanitizer.maskAndTruncate("x".repeat(100), 10)).hasSize(10);
    }

    @Test
    void nullPassesThrough() {
        assertThat(LogSanitizer.maskAndTruncate(null, 10)).isNull();
    }

    @Test
    void masksKeysContainingSensitiveWords() {
        String json = "{\"tokenValue\":\"secret-token\",\"userName\":\"alice\"}";

        String out = LogSanitizer.maskAndTruncate(json, 200);

        assertThat(out).doesNotContain("secret-token")
                .contains("\"tokenValue\":\"***\"")
                .contains("\"userName\":\"alice\"");
    }

    @Test
    void masksEscapedQuotesInsideSensitiveValues() {
        String json = "{\"password\":\"p\\\"ss\",\"userName\":\"alice\"}";

        String out = LogSanitizer.maskAndTruncate(json, 200);

        assertThat(out).doesNotContain("p\\\"ss")
                .contains("\"password\":\"***\"")
                .contains("\"userName\":\"alice\"");
    }

    @Test
    void toJsonFallbackNeverLeaksToStringContent() {
        Object object = new Object() {
            @Override
            public String toString() {
                return "password=secret-value";
            }
        };

        String out = LogSanitizer.toJson(object, 100);

        assertThat(out).doesNotContain("secret-value").contains("<unserializable");
    }
}
