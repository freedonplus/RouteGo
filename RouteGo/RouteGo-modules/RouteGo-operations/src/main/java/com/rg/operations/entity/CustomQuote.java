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
 * 定制报价表
 * </p>
 *
 * @author RouteGo
 * @since 2026-11-04
 */
@TableName("custom_quote")
public class CustomQuote implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 需求单 ID
     */
    @TableField("requirement_id")
    private Long requirementId;

    /**
     * 报价单号
     */
    @TableField("quote_no")
    private String quoteNo;

    /**
     * 报价金额
     */
    @TableField("amount")
    private BigDecimal amount;

    /**
     * 报价明细
     */
    @TableField("detail")
    private String detail;

    /**
     * 有效期至
     */
    @TableField("valid_until")
    private LocalDate validUntil;

    /**
     * 状态：0 待确认，1 已采纳，2 已失效
     */
    @TableField("status")
    private Byte status;

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRequirementId() {
        return requirementId;
    }

    public void setRequirementId(Long requirementId) {
        this.requirementId = requirementId;
    }

    public String getQuoteNo() {
        return quoteNo;
    }

    public void setQuoteNo(String quoteNo) {
        this.quoteNo = quoteNo;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public LocalDate getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDate validUntil) {
        this.validUntil = validUntil;
    }

    public Byte getStatus() {
        return status;
    }

    public void setStatus(Byte status) {
        this.status = status;
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

    @Override
    public String toString() {
        return "CustomQuote{" +
            "id = " + id +
            ", requirementId = " + requirementId +
            ", quoteNo = " + quoteNo +
            ", amount = " + amount +
            ", detail = " + detail +
            ", validUntil = " + validUntil +
            ", status = " + status +
            ", createBy = " + createBy +
            ", createTime = " + createTime +
        "}";
    }
}
