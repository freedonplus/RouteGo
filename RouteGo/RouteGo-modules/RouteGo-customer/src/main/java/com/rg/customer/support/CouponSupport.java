package com.rg.customer.support;

import com.rg.customer.entity.CouponTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * 优惠券的展示与有效期计算工具。
 * <p>
 * coupon 表没有「过期时间」列——券什么时候失效完全由模板规则推导：
 * 固定区间看模板的 end_time，领取后 N 天看 receive_time + valid_days。
 * 两个服务（模板、用户券）共用这里的算法，避免两处规则算出不同结果。
 */
public final class CouponSupport {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private CouponSupport() {
    }

    /**
     * 计算某张券的过期时间；模板永久有效时返回 null。
     */
    public static LocalDateTime expireTime(CouponTemplate template, LocalDateTime receiveTime) {
        if (template == null) {
            return null;
        }
        if (Objects.equals(template.getValidType(), (byte) 2)) {
            if (template.getValidDays() == null || receiveTime == null) {
                return null;
            }
            return receiveTime.plusDays(template.getValidDays());
        }
        return template.getEndTime();
    }

    /**
     * 判断券是否已过期。已使用的券不再参与过期判断。
     */
    public static boolean expired(CouponTemplate template, LocalDateTime receiveTime, Byte status) {
        if (status != null && status == 1) {
            return false;
        }
        LocalDateTime expire = expireTime(template, receiveTime);
        return expire != null && expire.isBefore(LocalDateTime.now());
    }

    /**
     * 模板本身是否已过有效期（停用之外的另一层判断）。
     */
    public static boolean templateExpired(CouponTemplate template) {
        if (template == null) {
            return false;
        }
        if (Objects.equals(template.getValidType(), (byte) 1)) {
            return template.getEndTime() != null && template.getEndTime().isBefore(LocalDateTime.now());
        }
        return false;
    }

    /**
     * 优惠说明：满 1000.00 减 100.00 / 满 500.00 享 9 折 / 无门槛立减 50.00。
     */
    public static String benefitDesc(CouponTemplate template) {
        if (template == null) {
            return null;
        }
        boolean hasThreshold = template.getThreshold() != null
                && template.getThreshold().compareTo(BigDecimal.ZERO) > 0;
        String threshold = hasThreshold
                ? "满 " + template.getThreshold().setScale(2, RoundingMode.HALF_UP) + " "
                : "无门槛";

        if (Objects.equals(template.getType(), (byte) 2)) {
            BigDecimal discount = template.getDiscount();
            if (discount == null) {
                return threshold + "享折扣";
            }
            BigDecimal rate = discount.multiply(BigDecimal.TEN).stripTrailingZeros();
            return threshold + "享 " + rate.toPlainString() + " 折";
        }
        BigDecimal amount = template.getAmount();
        if (amount == null) {
            return threshold + "立减";
        }
        return threshold + (hasThreshold ? "减 " : "立减 ")
                + amount.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 有效期说明：2026-11-01 ~ 2026-12-31 / 领取后 30 天内有效。
     */
    public static String validDesc(CouponTemplate template) {
        if (template == null) {
            return null;
        }
        if (Objects.equals(template.getValidType(), (byte) 2)) {
            return template.getValidDays() == null ? "长期有效" : "领取后 " + template.getValidDays() + " 天内有效";
        }
        String start = template.getStartTime() == null ? "不限" : template.getStartTime().format(DATE);
        String end = template.getEndTime() == null ? "不限" : template.getEndTime().format(DATE);
        return start + " ~ " + end;
    }
}
