package com.rg.customer.dto;

import com.rg.common.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 会员分页查询条件。
 * <p>
 * 手机号是 AES 密文存储，只能按盲索引精确匹配，因此这里区分了两个关键词：
 * {@link #phone} 精确查，{@link #keyword} 模糊查昵称与真实姓名。
 * {@link #keyword} 里如果填的是完整 11 位手机号，也会按盲索引精确命中一次，
 * 让后台「一个搜索框搜昵称/手机号」的交互能真正生效。
 */
@Schema(description = "会员分页查询条件")
@Data
@EqualsAndHashCode(callSuper = true)
public class CustomerQuery extends PageQuery {

    @Schema(description = "手机号，精确匹配（服务端转盲索引查询）")
    private String phone;

    @Schema(description = "昵称或真实姓名模糊匹配；填完整手机号时按盲索引精确匹配")
    private String keyword;

    @Schema(description = "会员等级：1 普通，2 银卡，3 金卡，4 钻石")
    private Byte level;

    @Schema(description = "状态：0 禁用，1 正常")
    private Byte status;

    @Schema(description = "注册时间起（含）")
    private LocalDateTime registerTimeStart;

    @Schema(description = "注册时间止（含）")
    private LocalDateTime registerTimeEnd;
}
