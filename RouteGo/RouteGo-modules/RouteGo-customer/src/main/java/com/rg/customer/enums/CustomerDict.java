package com.rg.customer.enums;

/**
 * 客户域轻量字典：会员等级、性别、券类型、券状态、有效期类型的描述转换。
 * <p>
 * 取值与建表语句注释、sys_dict_data 种子数据保持一致，前端不必再维护一份映射。
 */
public final class CustomerDict {

    private CustomerDict() {
    }

    public static String levelDesc(Byte level) {
        if (level == null) {
            return null;
        }
        return switch (level) {
            case 1 -> "普通会员";
            case 2 -> "银卡会员";
            case 3 -> "金卡会员";
            case 4 -> "钻石会员";
            default -> "未知";
        };
    }

    public static String genderDesc(Byte gender) {
        if (gender == null) {
            return null;
        }
        return switch (gender) {
            case 1 -> "男";
            case 2 -> "女";
            case 0 -> "未知";
            default -> "未知";
        };
    }

    public static String customerStatusDesc(Byte status) {
        if (status == null) {
            return null;
        }
        return status == 1 ? "正常" : "禁用";
    }

    public static String couponTypeDesc(Byte type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case 1 -> "满减券";
            case 2 -> "折扣券";
            default -> "未知";
        };
    }

    public static String couponStatusDesc(Byte status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case 0 -> "未使用";
            case 1 -> "已使用";
            case 2 -> "已过期";
            default -> "未知";
        };
    }

    public static String validTypeDesc(Byte validType) {
        if (validType == null) {
            return null;
        }
        return validType == 1 ? "固定区间" : "领取后计算";
    }
}
