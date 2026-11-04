package com.rg.customer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <p>
 * 会员表
 * </p>
 *
 * @author RouteGo
 * @since 2026-11-04
 */
@TableName("customer")
public class Customer implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 手机号（AES 密文）
     */
    @TableField("phone")
    private String phone;

    /**
     * 手机号盲索引（HMAC-SHA256）
     */
    @TableField("phone_index")
    private String phoneIndex;

    /**
     * 微信小程序 openid
     */
    @TableField("openid")
    private String openid;

    /**
     * 微信开放平台 unionid
     */
    @TableField("unionid")
    private String unionid;

    /**
     * 密码（BCrypt，可空）
     */
    @TableField("password")
    private String password;

    /**
     * 昵称
     */
    @TableField("nickname")
    private String nickname;

    /**
     * 头像
     */
    @TableField("avatar")
    private String avatar;

    /**
     * 真实姓名
     */
    @TableField("real_name")
    private String realName;

    /**
     * 证件号（AES 密文）
     */
    @TableField("id_card")
    private String idCard;

    /**
     * 性别：0 未知，1 男，2 女
     */
    @TableField("gender")
    private Byte gender;

    /**
     * 生日
     */
    @TableField("birthday")
    private LocalDate birthday;

    /**
     * 邮箱
     */
    @TableField("email")
    private String email;

    /**
     * 会员等级
     */
    @TableField("level")
    private Byte level;

    /**
     * 积分
     */
    @TableField("points")
    private Integer points;

    /**
     * 累计订单数
     */
    @TableField("order_count")
    private Integer orderCount;

    /**
     * 累计消费金额
     */
    @TableField("total_amount")
    private BigDecimal totalAmount;

    /**
     * 状态：0 禁用，1 正常
     */
    @TableField("status")
    private Byte status;

    /**
     * 注册时间
     */
    @TableField("register_time")
    private LocalDateTime registerTime;

    /**
     * 最后登录时间
     */
    @TableField("last_login_time")
    private LocalDateTime lastLoginTime;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;

    /**
     * 逻辑删除：0 正常，1 删除
     */
    @TableField("deleted")
    private Byte deleted;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPhoneIndex() {
        return phoneIndex;
    }

    public void setPhoneIndex(String phoneIndex) {
        this.phoneIndex = phoneIndex;
    }

    public String getOpenid() {
        return openid;
    }

    public void setOpenid(String openid) {
        this.openid = openid;
    }

    public String getUnionid() {
        return unionid;
    }

    public void setUnionid(String unionid) {
        this.unionid = unionid;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getIdCard() {
        return idCard;
    }

    public void setIdCard(String idCard) {
        this.idCard = idCard;
    }

    public Byte getGender() {
        return gender;
    }

    public void setGender(Byte gender) {
        this.gender = gender;
    }

    public LocalDate getBirthday() {
        return birthday;
    }

    public void setBirthday(LocalDate birthday) {
        this.birthday = birthday;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Byte getLevel() {
        return level;
    }

    public void setLevel(Byte level) {
        this.level = level;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public Integer getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(Integer orderCount) {
        this.orderCount = orderCount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Byte getStatus() {
        return status;
    }

    public void setStatus(Byte status) {
        this.status = status;
    }

    public LocalDateTime getRegisterTime() {
        return registerTime;
    }

    public void setRegisterTime(LocalDateTime registerTime) {
        this.registerTime = registerTime;
    }

    public LocalDateTime getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(LocalDateTime lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Byte getDeleted() {
        return deleted;
    }

    public void setDeleted(Byte deleted) {
        this.deleted = deleted;
    }

    @Override
    public String toString() {
        return "Customer{" +
            "id = " + id +
            ", phone = " + phone +
            ", phoneIndex = " + phoneIndex +
            ", openid = " + openid +
            ", unionid = " + unionid +
            ", password = " + password +
            ", nickname = " + nickname +
            ", avatar = " + avatar +
            ", realName = " + realName +
            ", idCard = " + idCard +
            ", gender = " + gender +
            ", birthday = " + birthday +
            ", email = " + email +
            ", level = " + level +
            ", points = " + points +
            ", orderCount = " + orderCount +
            ", totalAmount = " + totalAmount +
            ", status = " + status +
            ", registerTime = " + registerTime +
            ", lastLoginTime = " + lastLoginTime +
            ", createTime = " + createTime +
            ", updateTime = " + updateTime +
            ", deleted = " + deleted +
        "}";
    }
}
