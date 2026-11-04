package com.rg.trade.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 订单状态流水表
 * </p>
 *
 * @author RouteGo
 * @since 2026-11-04
 */
@TableName("order_status_log")
public class OrderStatusLog implements Serializable {

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
     * 原状态
     */
    @TableField("from_status")
    private Byte fromStatus;

    /**
     * 新状态
     */
    @TableField("to_status")
    private Byte toStatus;

    /**
     * 操作人 ID
     */
    @TableField("operator_id")
    private Long operatorId;

    /**
     * 操作人名称
     */
    @TableField("operator_name")
    private String operatorName;

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

    public Byte getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(Byte fromStatus) {
        this.fromStatus = fromStatus;
    }

    public Byte getToStatus() {
        return toStatus;
    }

    public void setToStatus(Byte toStatus) {
        this.toStatus = toStatus;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
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
        return "OrderStatusLog{" +
            "id = " + id +
            ", orderId = " + orderId +
            ", fromStatus = " + fromStatus +
            ", toStatus = " + toStatus +
            ", operatorId = " + operatorId +
            ", operatorName = " + operatorName +
            ", remark = " + remark +
            ", createTime = " + createTime +
        "}";
    }
}
