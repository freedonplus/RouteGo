package com.rg.catalog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 线路评价表
 * </p>
 *
 * @author RouteGo
 * @since 2026-11-04
 */
@TableName("route_review")
public class RouteReview implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 线路 ID
     */
    @TableField("route_id")
    private Long routeId;

    /**
     * 订单 ID
     */
    @TableField("order_id")
    private Long orderId;

    /**
     * 客户 ID
     */
    @TableField("customer_id")
    private Long customerId;

    /**
     * 评分 1-5
     */
    @TableField("rating")
    private Byte rating;

    /**
     * 评价内容
     */
    @TableField("content")
    private String content;

    /**
     * 评价图片（JSON 数组）
     */
    @TableField("images")
    private String images;

    /**
     * 后台回复
     */
    @TableField("reply")
    private String reply;

    /**
     * 回复时间
     */
    @TableField("reply_time")
    private LocalDateTime replyTime;

    /**
     * 状态：0 隐藏，1 显示
     */
    @TableField("status")
    private Byte status;

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

    public Long getRouteId() {
        return routeId;
    }

    public void setRouteId(Long routeId) {
        this.routeId = routeId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Byte getRating() {
        return rating;
    }

    public void setRating(Byte rating) {
        this.rating = rating;
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

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public LocalDateTime getReplyTime() {
        return replyTime;
    }

    public void setReplyTime(LocalDateTime replyTime) {
        this.replyTime = replyTime;
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
        return "RouteReview{" +
            "id = " + id +
            ", routeId = " + routeId +
            ", orderId = " + orderId +
            ", customerId = " + customerId +
            ", rating = " + rating +
            ", content = " + content +
            ", images = " + images +
            ", reply = " + reply +
            ", replyTime = " + replyTime +
            ", status = " + status +
            ", version = " + version +
            ", createTime = " + createTime +
            ", updateTime = " + updateTime +
            ", deleted = " + deleted +
        "}";
    }
}
