package com.rg.common.captcha;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;

class CaptchaGeneratorTest {

    @Test
    void generatesFourCharsAndValidPng() throws Exception {
        CaptchaResult result = new CaptchaGenerator().generate();

        assertThat(result.code()).hasSize(4).matches("[A-HJ-NP-Z2-9]{4}");

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(result.png()));
        assertThat(image).isNotNull();
        assertThat(image.getWidth()).isEqualTo(120);
        assertThat(image.getHeight()).isEqualTo(40);
    }
}
