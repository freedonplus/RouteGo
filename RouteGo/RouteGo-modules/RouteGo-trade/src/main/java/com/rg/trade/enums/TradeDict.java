package com.rg.trade.enums;

/**
 * 交易域轻量字典：支付状态、支付方式、退款状态、订单来源的描述转换。
 * <p>
 * 取值与建表语句注释严格一致，前端不需要再维护一份映射表。
 */
public final class TradeDict {

    private TradeDict() {
    }

    public static String payStatusDesc(Byte code) {
        if (code == null) {
            return null;
        }
        return switch (code) {
            case 0 -> "未支付";
            case 1 -> "已支付";
            case 2 -> "已退款";
            default -> "未知";
        };
    }

    public static String payTypeDesc(Byte code) {
        if (code == null) {
            return null;
        }
        return switch (code) {
            case 1 -> "模拟支付";
            case 2 -> "微信支付";
            default -> "未知";
        };
    }

    public static String refundStatusDesc(Byte code) {
        if (code == null) {
            return null;
        }
        return switch (code) {
            case 0 -> "待审核";
            case 1 -> "已通过";
            case 2 -> "已驳回";
            case 3 -> "已退款";
            default -> "未知";
        };
    }

    public static String sourceDesc(Byte code) {
        if (code == null) {
            return null;
        }
        return switch (code) {
            case 1 -> "后台代录";
            case 2 -> "微信小程序";
            default -> "未知";
        };
    }

    public static String genderDesc(Byte code) {
        if (code == null) {
            return null;
        }
        return switch (code) {
            case 0 -> "未知";
            case 1 -> "男";
            case 2 -> "女";
            default -> "未知";
        };
    }

    public static String travelerTypeDesc(Byte code) {
        if (code == null) {
            return null;
        }
        return switch (code) {
            case 1 -> "成人";
            case 2 -> "儿童";
            default -> "未知";
        };
    }

    public static String paymentStatusDesc(Byte code) {
        if (code == null) {
            return null;
        }
        return switch (code) {
            case 0 -> "失败";
            case 1 -> "成功";
            default -> "未知";
        };
    }
}
