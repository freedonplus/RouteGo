package com.rg.operations.support;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 业务单号生成器。
 * <p>
 * 规则：前缀 + 日期（yyyyMMdd）+ 5 位随机数，例如 {@code TK2026110501732}。
 * 随机段用 {@link SecureRandom}，不可预测，避免外部按单号顺序推测业务量。
 * <p>
 * 生成器不保证唯一：调用方必须拿唯一索引兜底，撞号时重新生成（见各 Service 的重试循环）。
 */
public final class BizNoGenerator {

    /** 客服工单号前缀 */
    public static final String TICKET_NO_PREFIX = "TK";
    /** 定制需求单号前缀 */
    public static final String REQUIREMENT_NO_PREFIX = "CR";
    /** 定制报价单号前缀 */
    public static final String QUOTE_NO_PREFIX = "CQ";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int RANDOM_BOUND = 100_000;

    private BizNoGenerator() {
    }

    public static String next(String prefix) {
        return prefix + LocalDate.now().format(DATE_FORMATTER) + String.format("%05d", RANDOM.nextInt(RANDOM_BOUND));
    }
}
