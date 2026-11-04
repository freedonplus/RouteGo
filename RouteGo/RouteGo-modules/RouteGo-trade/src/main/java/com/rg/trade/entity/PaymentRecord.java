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
 * 支付记录表
 * </p>
 *
 * @author RouteGo
 * @since 2026-11-04
 */
@TableName("payment_record")
public class PaymentRecord implements Serializable {

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
     * 支付单号
     */
    @TableField("pay_no")
    private String payNo;

    /**
     * 支付方式：1 模拟，2 微信支付
     */
    @TableField("pay_type")
    private Byte payType;

    /**
     * 支付金额
     */
    @TableField("amount")
    private BigDecimal amount;

    /**
     * 状态：0 失败，1 成功
     */
    @TableField("status")
    private Byte status;

    /**
     * 支付时间
     */
    @TableField("pay_time")
    private LocalDateTime payTime;

    /**
     * 第三方交易号
     */
    @TableField("transaction_id")
    private String transactionId;

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

    public String getPayNo() {
        return payNo;
    }

    public void setPayNo(String payNo) {
        this.payNo = payNo;
    }

    public Byte getPayType() {
        return payType;
    }

    public void setPayType(Byte payType) {
        this.payType = payType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Byte getStatus() {
        return status;
    }

    public void setStatus(Byte status) {
        this.status = status;
    }

    public LocalDateTime getPayTime() {
        return payTime;
    }

    public void setPayTime(LocalDateTime payTime) {
        this.payTime = payTime;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "PaymentRecord{" +
            "id = " + id +
            ", orderId = " + orderId +
            ", payNo = " + payNo +
            ", payType = " + payType +
            ", amount = " + amount +
            ", status = " + status +
            ", payTime = " + payTime +
            ", transactionId = " + transactionId +
            ", createTime = " + createTime +
        "}";
    }
}
