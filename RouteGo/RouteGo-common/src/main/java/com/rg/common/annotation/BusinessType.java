package com.rg.common.annotation;

/**
 * 操作日志业务类型，code 与 sys_oper_log.business_type 对应。
 */
public enum BusinessType {

    INSERT(1),
    UPDATE(2),
    DELETE(3),
    QUERY(4),
    EXPORT(5),
    OTHER(6);

    private final int code;

    BusinessType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
