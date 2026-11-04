package com.rg.customer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <p>
 * 优惠券模板表
 * </p>
 *
 * @author RouteGo
 * @since 2026-11-04
 */
@TableName("coupon_template")
public class CouponTemplate implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 优惠券名称
     */
    @TableField("name")
    private String name;

    /**
     * 类型：1 满减，2 折扣
     */
    @TableField("type")
    private Byte type;

    /**
     * 使用门槛
     */
    @TableField("threshold")
    private BigDecimal threshold;

    /**
     * 减免金额
     */
    @TableField("amount")
    private BigDecimal amount;

    /**
     * 折扣率（如 0.90）
     */
    @TableField("discount")
    private BigDecimal discount;

    /**
     * 发行总量
     */
    @TableField("total_count")
    private Integer totalCount;

    /**
     * 已领取数量
     */
    @TableField("received_count")
    private Integer receivedCount;

    /**
     * 有效期类型：1 固定区间，2 领取后 N 天
     */
    @TableField("valid_type")
    private Byte validType;

    /**
     * 有效期开始
     */
    @TableField("start_time")
    private LocalDateTime startTime;

    /**
     * 有效期结束
     */
    @TableField("end_time")
    private LocalDateTime endTime;

    /**
     * 领取后有效天数
     */
    @TableField("valid_days")
    private Integer validDays;

    /**
     * 状态：0 停用，1 启用
     */
    @TableField("status")
    private Byte status;

    /**
     * 乐观锁版本号
     */
    @TableField("version")
    private Integer version;

    /**
     * 创建人 ID
     */
    @TableField("create_by")
    private Long createBy;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新人 ID
     */
    @TableField("update_by")
    private Long updateBy;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Byte getType() {
        return type;
    }

    public void setType(Byte type) {
        this.type = type;
    }

    public BigDecimal getThreshold() {
        return threshold;
    }

    public void setThreshold(BigDecimal threshold) {
        this.threshold = threshold;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public Integer getReceivedCount() {
        return receivedCount;
    }

    public void setReceivedCount(Integer receivedCount) {
        this.receivedCount = receivedCount;
    }

    public Byte getValidType() {
        return validType;
    }

    public void setValidType(Byte validType) {
        this.validType = validType;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Integer getValidDays() {
        return validDays;
    }

    public void setValidDays(Integer validDays) {
        this.validDays = validDays;
    }

    public Byte getStatus() {
        return status;
    }

    public void setStatus(Byte status) {
        this.status = status;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Long getCreateBy() {
        return createBy;
    }

    public void setCreateBy(Long createBy) {
        this.createBy = createBy;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public Long getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(Long updateBy) {
        this.updateBy = updateBy;
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
        return "CouponTemplate{" +
            "id = " + id +
            ", name = " + name +
            ", type = " + type +
            ", threshold = " + threshold +
            ", amount = " + amount +
            ", discount = " + discount +
            ", totalCount = " + totalCount +
            ", receivedCount = " + receivedCount +
            ", validType = " + validType +
            ", startTime = " + startTime +
            ", endTime = " + endTime +
            ", validDays = " + validDays +
            ", status = " + status +
            ", version = " + version +
            ", createBy = " + createBy +
            ", createTime = " + createTime +
            ", updateBy = " + updateBy +
            ", updateTime = " + updateTime +
            ", deleted = " + deleted +
        "}";
    }
}
