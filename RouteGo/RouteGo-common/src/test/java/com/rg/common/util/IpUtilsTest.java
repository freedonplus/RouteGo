package com.rg.common.util;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IpUtilsTest {

    @Test
    void ignoresForwardedForWhenPeerNotTrusted() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5");
        request.addHeader("X-Forwarded-For", "1.2.3.4");

        assertThat(IpUtils.resolveClientIp(request, List.of("10.0.0.1"))).isEqualTo("10.0.0.5");
    }

    @Test
    void usesLastHopWhenPeerIsTrusted() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "1.2.3.4, 10.0.0.9");

        assertThat(IpUtils.resolveClientIp(request, List.of("10.0.0.1"))).isEqualTo("10.0.0.9");
    }

    @Test
    void fallsBackToRemoteWhenHeaderMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");

        assertThat(IpUtils.resolveClientIp(request, List.of("10.0.0.1"))).isEqualTo("10.0.0.1");
    }

    @Test
    void skipsTrustedProxyHopsFromTheRight() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "1.2.3.4, 10.0.0.9, 10.0.0.2");

        assertThat(IpUtils.resolveClientIp(request, List.of("10.0.0.1", "10.0.0.2")))
                .isEqualTo("10.0.0.9");
    }
}
