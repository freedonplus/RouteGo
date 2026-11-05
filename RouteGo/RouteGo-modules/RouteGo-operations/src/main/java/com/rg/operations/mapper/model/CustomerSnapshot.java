package com.rg.operations.mapper.model;

import lombok.Data;

/**
 * 会员只读快照，用于工单/定制需求列表补昵称与手机号。
 * <p>
 * {@code phone} 落库是 AES 密文，这里不做解密，由 Service 用 {@code SensitiveCipher} 脱敏后输出。
 */
@Data
public class CustomerSnapshot {

    private Long id;

    private String nickname;

    private String phone;

    private Byte status;
}
