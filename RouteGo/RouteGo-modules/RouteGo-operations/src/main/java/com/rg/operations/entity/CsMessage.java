package com.rg.operations.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 工单消息表
 * </p>
 *
 * @author RouteGo
 * @since 2026-11-04
 */
@TableName("cs_message")
public class CsMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 工单 ID
     */
    @TableField("ticket_id")
    private Long ticketId;

    /**
     * 发送方：1 客户，2 系统用户
     */
    @TableField("sender_type")
    private Byte senderType;

    /**
     * 发送方 ID
     */
    @TableField("sender_id")
    private Long senderId;

    /**
     * 消息内容
     */
    @TableField("content")
    private String content;

    /**
     * 图片（JSON 数组）
     */
    @TableField("images")
    private String images;

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

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public Byte getSenderType() {
        return senderType;
    }

    public void setSenderType(Byte senderType) {
        this.senderType = senderType;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getImages() {
        return images;
    }

    public void setImages(String images) {
        this.images = images;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "CsMessage{" +
            "id = " + id +
            ", ticketId = " + ticketId +
            ", senderType = " + senderType +
            ", senderId = " + senderId +
            ", content = " + content +
            ", images = " + images +
            ", createTime = " + createTime +
        "}";
    }
}
