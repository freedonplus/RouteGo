package com.rg.trade.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 出行人表
 * </p>
 *
 * @author RouteGo
 * @since 2026-11-04
 */
@TableName("order_traveler")
public class OrderTraveler implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 订单 ID
     */
    @TableField("order_id")
    private Long orderId;

    /**
     * 姓名
     */
    @TableField("name")
    private String name;

    /**
     * 证件号（AES 密文）
     */
    @TableField("id_card")
    private String idCard;

    /**
     * 手机号（AES 密文）
     */
    @TableField("phone")
    private String phone;

    /**
     * 性别：0 未知，1 男，2 女
     */
    @TableField("gender")
    private Byte gender;

    /**
     * 类型：1 成人，2 儿童
     */
    @TableField("traveler_type")
    private Byte travelerType;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIdCard() {
        return idCard;
    }

    public void setIdCard(String idCard) {
        this.idCard = idCard;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Byte getGender() {
        return gender;
    }

    public void setGender(Byte gender) {
        this.gender = gender;
    }

    public Byte getTravelerType() {
        return travelerType;
    }

    public void setTravelerType(Byte travelerType) {
        this.travelerType = travelerType;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "OrderTraveler{" +
            "id = " + id +
            ", orderId = " + orderId +
            ", name = " + name +
            ", idCard = " + idCard +
            ", phone = " + phone +
            ", gender = " + gender +
            ", travelerType = " + travelerType +
            ", remark = " + remark +
            ", createTime = " + createTime +
        "}";
    }
}
