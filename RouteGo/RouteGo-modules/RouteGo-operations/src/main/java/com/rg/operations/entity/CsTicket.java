package com.rg.operations.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 客服工单表
 * </p>
 *
 * @author RouteGo
 * @since 2026-11-04
 */
@TableName("cs_ticket")
public class CsTicket implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 工单号
     */
    @TableField("ticket_no")
    private String ticketNo;

    /**
     * 客户 ID
     */
    @TableField("customer_id")
    private Long customerId;

    /**
     * 关联订单 ID
     */
    @TableField("order_id")
    private Long orderId;

    /**
     * 类型：1 售前，2 订单，3 退款，4 投诉，5 其他
     */
    @TableField("type")
    private Byte type;

    /**
     * 工单标题
     */
    @TableField("title")
    private String title;

    /**
     * 问题描述
     */
    @TableField("content")
    private String content;

    /**
     * 优先级：1 低，2 中，3 高
     */
    @TableField("priority")
    private Byte priority;

    /**
     * 状态：0 待处理，1 处理中，2 已解决，3 已关闭
     */
    @TableField("status")
    private Byte status;

    /**
     * 处理人 ID
     */
    @TableField("handler_id")
    private Long handlerId;

    /**
     * 完成时间
     */
    @TableField("finish_time")
    private LocalDateTime finishTime;

    /**
     * 最后一条消息时间，会话列表按它倒序
     */
    @TableField("last_message_time")
    private LocalDateTime lastMessageTime;

    /**
     * 最后一条消息摘要（最多 200 字），会话列表直接展示，避免每行再查一次消息表
     */
    @TableField("last_message")
    private String lastMessage;

    /**
     * 客户未读数：客服回复后 +1，客户打开会话清零
     */
    @TableField("customer_unread")
    private Integer customerUnread;

    /**
     * 客服未读数：客户发消息后 +1，客服打开会话清零
     */
    @TableField("staff_unread")
    private Integer staffUnread;

    /**
     * 乐观锁版本号
     */
    @Version
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

    public String getTicketNo() {
        return ticketNo;
    }

    public void setTicketNo(String ticketNo) {
        this.ticketNo = ticketNo;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Byte getType() {
        return type;
    }

    public void setType(Byte type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Byte getPriority() {
        return priority;
    }

    public void setPriority(Byte priority) {
        this.priority = priority;
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

    public LocalDateTime getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(LocalDateTime finishTime) {
        this.finishTime = finishTime;
    }

    public LocalDateTime getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(LocalDateTime lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public Integer getCustomerUnread() {
        return customerUnread;
    }

    public void setCustomerUnread(Integer customerUnread) {
        this.customerUnread = customerUnread;
    }

    public Integer getStaffUnread() {
        return staffUnread;
    }

    public void setStaffUnread(Integer staffUnread) {
        this.staffUnread = staffUnread;
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
        return "CsTicket{" +
            "id = " + id +
            ", ticketNo = " + ticketNo +
            ", customerId = " + customerId +
            ", orderId = " + orderId +
            ", type = " + type +
            ", title = " + title +
            ", content = " + content +
            ", priority = " + priority +
            ", status = " + status +
            ", handlerId = " + handlerId +
            ", finishTime = " + finishTime +
            ", lastMessageTime = " + lastMessageTime +
            ", lastMessage = " + lastMessage +
            ", customerUnread = " + customerUnread +
            ", staffUnread = " + staffUnread +
            ", version = " + version +
            ", createBy = " + createBy +
            ", createTime = " + createTime +
            ", updateBy = " + updateBy +
            ", updateTime = " + updateTime +
            ", deleted = " + deleted +
        "}";
    }
}
