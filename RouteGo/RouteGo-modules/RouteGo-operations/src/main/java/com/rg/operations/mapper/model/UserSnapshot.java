package com.rg.operations.mapper.model;

import lombok.Data;

/**
 * 后台用户只读快照，用于把 handler_id 翻译成处理人姓名。
 */
@Data
public class UserSnapshot {

    private Long id;

    private String username;

    private String nickname;
}
