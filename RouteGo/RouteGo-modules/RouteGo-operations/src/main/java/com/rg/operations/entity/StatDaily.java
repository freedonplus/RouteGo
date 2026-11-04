package com.rg.operations.entity;

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
 * 每日经营统计表
 * </p>
 *
 * @author RouteGo
 * @since 2026-11-04
 */
@TableName("stat_daily")
public class StatDaily implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 统计日期
     */
    @TableField("stat_date")
    private LocalDate statDate;

    /**
     * 订单数
     */
    @TableField("order_count")
    private Integer orderCount;

    /**
     * 已支付订单数
     */
    @TableField("paid_order_count")
    private Integer paidOrderCount;

    /**
     * 销售额
     */
    @TableField("sales_amount")
    private BigDecimal salesAmount;

    /**
     * 退款金额
     */
    @TableField("refund_amount")
    private BigDecimal refundAmount;

    /**
     * 新增客户数
     */
    @TableField("new_customer_count")
    private Integer newCustomerCount;

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getStatDate() {
        return statDate;
    }

    public void setStatDate(LocalDate statDate) {
        this.statDate = statDate;
    }

    public Integer getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(Integer orderCount) {
        this.orderCount = orderCount;
    }

    public Integer getPaidOrderCount() {
        return paidOrderCount;
    }

    public void setPaidOrderCount(Integer paidOrderCount) {
        this.paidOrderCount = paidOrderCount;
    }

    public BigDecimal getSalesAmount() {
        return salesAmount;
    }

    public void setSalesAmount(BigDecimal salesAmount) {
        this.salesAmount = salesAmount;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
    }

    public Integer getNewCustomerCount() {
        return newCustomerCount;
    }

    public void setNewCustomerCount(Integer newCustomerCount) {
        this.newCustomerCount = newCustomerCount;
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

    @Override
    public String toString() {
        return "StatDaily{" +
            "id = " + id +
            ", statDate = " + statDate +
            ", orderCount = " + orderCount +
            ", paidOrderCount = " + paidOrderCount +
            ", salesAmount = " + salesAmount +
            ", refundAmount = " + refundAmount +
            ", newCustomerCount = " + newCustomerCount +
            ", createTime = " + createTime +
            ", updateTime = " + updateTime +
        "}";
    }
}
