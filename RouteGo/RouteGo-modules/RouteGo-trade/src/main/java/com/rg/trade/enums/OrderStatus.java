package com.rg.trade.enums;

/**
 * 订单状态。
 * <p>
 * 状态流转是单向的，由 {@code OrderServiceImpl} 统一裁决：
 * <pre>
 * 0 待支付 ──支付──> 1 已支付 ──开始行程──> 2 出行中 ──完成──> 3 已完成
 *    └──取消──> 4 已取消
 * 1 已支付 ──申请退款──> 5 退款中 ──通过──> 6 已退款
 *                          └──驳回──> 1 已支付
 * </pre>
 * 3 / 4 / 6 为终态，不能再流转。
 */
public enum OrderStatus {

    UNPAID(0, "待支付"),
    PAID(1, "已支付"),
    TRAVELING(2, "出行中"),
    COMPLETED(3, "已完成"),
    CANCELED(4, "已取消"),
    REFUNDING(5, "退款中"),
    REFUNDED(6, "已退款");

    private final byte code;
    private final String desc;

    OrderStatus(int code, String desc) {
        this.code = (byte) code;
        this.desc = desc;
    }

    public byte getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 按状态码取描述，未知状态返回「未知」。
     */
    public static String describe(Byte code) {
        if (code == null) {
            return null;
        }
        for (OrderStatus status : values()) {
            if (status.code == code) {
                return status.desc;
            }
        }
        return "未知";
    }

    /**
     * 按状态码取枚举，未知状态抛出业务异常交给上层处理。
     */
    public static OrderStatus of(Byte code) {
        if (code == null) {
            throw new IllegalArgumentException("订单状态不能为空");
        }
        for (OrderStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的订单状态：" + code);
    }

    /**
     * 是否终态。
     */
    public boolean isFinal() {
        return this == COMPLETED || this == CANCELED || this == REFUNDED;
    }
}
