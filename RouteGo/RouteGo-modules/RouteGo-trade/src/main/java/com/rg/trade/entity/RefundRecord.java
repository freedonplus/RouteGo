package com.rg.trade.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <p>
 * 退款记录表
 * </p>
 *
 * @author RouteGo
 * @since 2026-11-04
 */
@TableName("refund_record")
public class RefundRecord implements Serializable {

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
     * 退款单号
     */
    @TableField("refund_no")
    private String refundNo;

    /**
     * 退款金额
     */
    @TableField("amount")
    private BigDecimal amount;

    /**
     * 退款原因
     */
    @TableField("reason")
    private String reason;

    /**
     * 状态：0 待审核，1 已通过，2 已驳回，3 已退款
     */
    @TableField("status")
    private Byte status;

    /**
     * 申请时间
     */
    @TableField("apply_time")
    private LocalDateTime applyTime;

    /**
     * 审核时间
     */
    @TableField("audit_time")
    private LocalDateTime auditTime;

    /**
     * 审核人 ID
     */
    @TableField("audit_by")
    private Long auditBy;

    /**
     * 审核意见
     */
    @TableField("audit_remark")
    private String auditRemark;

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

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getRefundNo() {
        return refundNo;
    }

    public void setRefundNo(String refundNo) {
        this.refundNo = refundNo;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Byte getStatus() {
        return status;
    }

    public void setStatus(Byte status) {
        this.status = status;
    }

    public LocalDateTime getApplyTime() {
        return applyTime;
    }

    public void setApplyTime(LocalDateTime applyTime) {
        this.applyTime = applyTime;
    }

    public LocalDateTime getAuditTime() {
        return auditTime;
    }

    public void setAuditTime(LocalDateTime auditTime) {
        this.auditTime = auditTime;
    }

    public Long getAuditBy() {
        return auditBy;
    }

    public void setAuditBy(Long auditBy) {
        this.auditBy = auditBy;
    }

    public String getAuditRemark() {
        return auditRemark;
    }

    public void setAuditRemark(String auditRemark) {
        this.auditRemark = auditRemark;
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
        return "RefundRecord{" +
            "id = " + id +
            ", orderId = " + orderId +
            ", refundNo = " + refundNo +
            ", amount = " + amount +
            ", reason = " + reason +
            ", status = " + status +
            ", applyTime = " + applyTime +
            ", auditTime = " + auditTime +
            ", auditBy = " + auditBy +
            ", auditRemark = " + auditRemark +
            ", version = " + version +
            ", createTime = " + createTime +
            ", updateTime = " + updateTime +
            ", deleted = " + deleted +
        "}";
    }
}
