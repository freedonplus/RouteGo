package com.rg.common.sensitive;

/**
 * 脱敏数据类型。
 */
public enum SensitiveType {

    /** 手机号：138****8000。 */
    PHONE,
    /** 邮箱：a***@example.com。 */
    EMAIL,
    /** 姓名：张*。 */
    NAME,
    /** 身份证号：110***********1234。 */
    ID_CARD,
    /** 银行卡：只保留后 4 位。 */
    BANK_CARD
}
