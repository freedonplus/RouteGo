package com.rg.common.captcha;

import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.SecureRandom;

/**
 * 4 位字符验证码生成器（字符 + 干扰线 + 噪点）。
 * <p>
 * 字符集去掉了 0/O/1/I 等易混淆字符，只依赖 JDK 的 Java2D，不引入第三方依赖。
 */
@Component
public class CaptchaGenerator {

    public static final int WIDTH = 120;
    public static final int HEIGHT = 40;
    public static final int LENGTH = 4;

    private static final char[] CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int NOISE_LINES = 5;
    private static final int NOISE_DOTS = 30;

    private final SecureRandom random = new SecureRandom();

    public CaptchaResult generate() {
        String code = randomCode();
        BufferedImage image = draw(code);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", out);
        } catch (IOException e) {
            throw new UncheckedIOException("验证码图片生成失败", e);
        }
        return new CaptchaResult(code, out.toByteArray());
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(CHARS[random.nextInt(CHARS.length)]);
        }
        return sb.toString();
    }

    private BufferedImage draw(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setPaint(new GradientPaint(0, 0, new Color(238, 244, 255),
                    WIDTH, HEIGHT, new Color(214, 228, 250)));
            g.fillRect(0, 0, WIDTH, HEIGHT);

            for (int i = 0; i < NOISE_LINES; i++) {
                g.setColor(randomColor(120, 200));
                g.setStroke(new BasicStroke(1.2f));
                g.drawLine(random.nextInt(WIDTH), random.nextInt(HEIGHT),
                        random.nextInt(WIDTH), random.nextInt(HEIGHT));
            }

            for (int i = 0; i < NOISE_DOTS; i++) {
                g.setColor(randomColor(100, 210));
                int x = random.nextInt(WIDTH);
                int y = random.nextInt(HEIGHT);
                g.fillOval(x, y, 2, 2);
            }

            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
            int step = WIDTH / (LENGTH + 1);
            for (int i = 0; i < code.length(); i++) {
                AffineTransform original = g.getTransform();
                double angle = Math.toRadians(random.nextInt(41) - 20);
                int x = step * (i + 1) - 8;
                int y = HEIGHT / 2 + 10 + random.nextInt(6) - 3;
                g.rotate(angle, x, y);
                g.setColor(randomColor(20, 120));
                g.drawString(String.valueOf(code.charAt(i)), x, y);
                g.setTransform(original);
            }
        } finally {
            g.dispose();
        }
        return image;
    }

    private Color randomColor(int min, int max) {
        int bound = max - min;
        return new Color(min + random.nextInt(bound),
                min + random.nextInt(bound),
                min + random.nextInt(bound));
    }
}
