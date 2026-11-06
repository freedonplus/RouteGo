package com.rg.customer.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 会员视图对象。
 * <p>
 * 手机号与证件号都是密文落库，出口一律给脱敏值，后台只能看到 138****8000 这种形式。
 */
@Schema(description = "会员信息")
@Data
public class CustomerVO {

    @Schema(description = "会员 ID")
    private Long id;

    @Schema(description = "手机号（脱敏）", example = "138****8000")
    private String phone;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像地址")
    private String avatar;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "证件号（脱敏）")
    private String idCard;

    @Schema(description = "性别：0 未知，1 男，2 女")
    private Byte gender;

    @Schema(description = "性别描述")
    private String genderDesc;

    @Schema(description = "生日")
    private LocalDate birthday;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "会员等级：1 普通，2 银卡，3 金卡，4 钻石")
    private Byte level;

    @Schema(description = "会员等级描述")
    private String levelDesc;

    @Schema(description = "积分")
    private Integer points;

    @Schema(description = "累计订单数")
    private Integer orderCount;

    @Schema(description = "累计消费金额")
    private BigDecimal totalAmount;

    @Schema(description = "状态：0 禁用，1 正常")
    private Byte status;

    @Schema(description = "状态描述")
    private String statusDesc;

    @Schema(description = "是否已绑定微信")
    private Boolean wechatBound;

    @Schema(description = "注册时间")
    private LocalDateTime registerTime;

    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginTime;

    @Schema(description = "可用优惠券数量，列表与详情都会返回")
    private Long availableCouponCount;

    @Schema(description = "已使用优惠券数量，仅详情返回")
    private Long usedCouponCount;
}
