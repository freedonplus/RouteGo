package com.rg.common.sensitive;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveSerializerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    static class Fixture {
        @Sensitive(SensitiveType.PHONE)
        public String phone;
        @Sensitive(SensitiveType.EMAIL)
        public String email;
        @Sensitive(SensitiveType.NAME)
        public String name;
        @Sensitive(SensitiveType.ID_CARD)
        public String idCard;
        @Sensitive(SensitiveType.BANK_CARD)
        public String bankCard;
        public String plain;

        Fixture(String phone, String email, String name, String idCard, String bankCard, String plain) {
            this.phone = phone;
            this.email = email;
            this.name = name;
            this.idCard = idCard;
            this.bankCard = bankCard;
            this.plain = plain;
        }
    }

    @Test
    void masksAllTypes() throws Exception {
        String json = mapper.writeValueAsString(new Fixture(
                "13812348000", "alice@example.com", "张三",
                "110101199001011234", "6222021234567890123", "visible"));

        assertThat(json).contains("\"phone\":\"138****8000\"");
        assertThat(json).contains("\"email\":\"a***@example.com\"");
        assertThat(json).contains("\"name\":\"张*\"");
        assertThat(json).contains("\"idCard\":\"110***********1234\"");
        assertThat(json).contains("\"bankCard\":\"**** **** **** 0123\"");
        assertThat(json).contains("\"plain\":\"visible\"");
    }

    @Test
    void nullAndShortValuesPassThrough() throws Exception {
        String json = mapper.writeValueAsString(new Fixture(
                null, "", "李", "123", null, null));

        assertThat(json).contains("\"phone\":null");
        assertThat(json).contains("\"email\":\"\"");
        assertThat(json).contains("\"name\":\"李\"");
        assertThat(json).contains("\"idCard\":\"123\"");
        assertThat(json).contains("\"bankCard\":null");
    }

    @Test
    void fourCharNameKeepsFirstCharOnly() throws Exception {
        String json = mapper.writeValueAsString(new Fixture(
                null, null, "欧阳娜娜", null, null, null));
        assertThat(json).contains("\"name\":\"欧***\"");
    }
}
