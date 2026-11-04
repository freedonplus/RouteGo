package com.rg.customer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 用户优惠券表
 * </p>
 *
 * @author RouteGo
 * @since 2026-11-04
 */
@TableName("coupon")
public class Coupon implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 模板 ID
     */
    @TableField("template_id")
    private Long templateId;

    /**
     * 客户 ID
     */
    @TableField("customer_id")
    private Long customerId;

    /**
     * 优惠券码
     */
    @TableField("code")
    private String code;

    /**
     * 状态：0 未使用，1 已使用，2 已过期
     */
    @TableField("status")
    private Byte status;

    /**
     * 领取时间
     */
    @TableField("receive_time")
    private LocalDateTime receiveTime;

    /**
     * 使用时间
     */
    @TableField("use_time")
    private LocalDateTime useTime;

    /**
     * 使用的订单 ID
     */
    @TableField("order_id")
    private Long orderId;

    /**
     * 乐观锁版本号
     */
    @TableField("version")
    private Integer version;

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

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Byte getStatus() {
        return status;
    }

    public void setStatus(Byte status) {
        this.status = status;
    }

    public LocalDateTime getReceiveTime() {
        return receiveTime;
    }

    public void setReceiveTime(LocalDateTime receiveTime) {
        this.receiveTime = receiveTime;
    }

    public LocalDateTime getUseTime() {
        return useTime;
    }

    public void setUseTime(LocalDateTime useTime) {
        this.useTime = useTime;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
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
        return "Coupon{" +
            "id = " + id +
            ", templateId = " + templateId +
            ", customerId = " + customerId +
            ", code = " + code +
            ", status = " + status +
            ", receiveTime = " + receiveTime +
            ", useTime = " + useTime +
            ", orderId = " + orderId +
            ", version = " + version +
            ", createTime = " + createTime +
            ", updateTime = " + updateTime +
            ", deleted = " + deleted +
        "}";
    }
}
