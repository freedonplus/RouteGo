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
    void toJsonFallsBackToStringValue() {
        Object circular = new Object() {
            @Override
            public String toString() {
                return "fallback";
            }
        };
        assertThat(LogSanitizer.toJson(circular, 100)).isEqualTo("fallback");
    }
}
