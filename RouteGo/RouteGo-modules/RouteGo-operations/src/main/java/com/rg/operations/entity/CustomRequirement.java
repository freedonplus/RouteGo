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
 * 定制需求表
 * </p>
 *
 * @author RouteGo
 * @since 2026-11-04
 */
@TableName("custom_requirement")
public class CustomRequirement implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 需求单号
     */
    @TableField("requirement_no")
    private String requirementNo;

    /**
     * 客户 ID
     */
    @TableField("customer_id")
    private Long customerId;

    /**
     * 联系人
     */
    @TableField("contact_name")
    private String contactName;

    /**
     * 联系电话（AES 密文）
     */
    @TableField("contact_phone")
    private String contactPhone;

    /**
     * 目的地
     */
    @TableField("destination")
    private String destination;

    /**
     * 期望出发日期
     */
    @TableField("depart_date")
    private LocalDate departDate;

    /**
     * 天数
     */
    @TableField("days")
    private Integer days;

    /**
     * 人数
     */
    @TableField("people_count")
    private Integer peopleCount;

    /**
     * 预算
     */
    @TableField("budget")
    private BigDecimal budget;

    /**
     * 偏好说明
     */
    @TableField("preferences")
    private String preferences;

    /**
     * 状态：0 待跟进，1 已报价，2 已成交，3 已关闭
     */
    @TableField("status")
    private Byte status;

    /**
     * 跟进人 ID
     */
    @TableField("handler_id")
    private Long handlerId;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;

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

    public String getRequirementNo() {
        return requirementNo;
    }

    public void setRequirementNo(String requirementNo) {
        this.requirementNo = requirementNo;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public LocalDate getDepartDate() {
        return departDate;
    }

    public void setDepartDate(LocalDate departDate) {
        this.departDate = departDate;
    }

    public Integer getDays() {
        return days;
    }

    public void setDays(Integer days) {
        this.days = days;
    }

    public Integer getPeopleCount() {
        return peopleCount;
    }

    public void setPeopleCount(Integer peopleCount) {
        this.peopleCount = peopleCount;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public void setBudget(BigDecimal budget) {
        this.budget = budget;
    }

    public String getPreferences() {
        return preferences;
    }

    public void setPreferences(String preferences) {
        this.preferences = preferences;
    }

    public Byte getStatus() {
        return status;
    }

    public void setStatus(Byte status) {
        this.status = status;
    }

    public Long getHandlerId() {
        return handlerId;
    }

    public void setHandlerId(Long handlerId) {
        this.handlerId = handlerId;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
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
        return "CustomRequirement{" +
            "id = " + id +
            ", requirementNo = " + requirementNo +
            ", customerId = " + customerId +
            ", contactName = " + contactName +
            ", contactPhone = " + contactPhone +
            ", destination = " + destination +
            ", departDate = " + departDate +
            ", days = " + days +
            ", peopleCount = " + peopleCount +
            ", budget = " + budget +
            ", preferences = " + preferences +
            ", status = " + status +
            ", handlerId = " + handlerId +
            ", remark = " + remark +
            ", version = " + version +
            ", createBy = " + createBy +
            ", createTime = " + createTime +
            ", updateBy = " + updateBy +
            ", updateTime = " + updateTime +
            ", deleted = " + deleted +
        "}";
    }
}
