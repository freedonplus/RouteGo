package com.rg.catalog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <p>
 * 行程明细表
 * </p>
 *
 * @author RouteGo
 * @since 2026-11-04
 */
@TableName("route_itinerary")
public class RouteItinerary implements Serializable {

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
     * 第几天
     */
    @TableField("day_no")
    private Integer dayNo;

    /**
     * 当日主题
     */
    @TableField("title")
    private String title;

    /**
     * 行程描述
     */
    @TableField("description")
    private String description;

    /**
     * 用餐说明
     */
    @TableField("meals")
    private String meals;

    /**
     * 住宿说明
     */
    @TableField("hotel")
    private String hotel;

    /**
     * 交通方式
     */
    @TableField("transport")
    private String transport;

    /**
     * 配图
     */
    @TableField("image")
    private String image;

    /**
     * 当日地点经度（GCJ-02）
     */
    @TableField("longitude")
    private BigDecimal longitude;

    /**
     * 当日地点纬度（GCJ-02）
     */
    @TableField("latitude")
    private BigDecimal latitude;

    /**
     * 当日地点地址
     */
    @TableField("address")
    private String address;

    /**
     * 排序
     */
    @TableField("sort")
    private Integer sort;

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

    public Long getRouteId() {
        return routeId;
    }

    public void setRouteId(Long routeId) {
        this.routeId = routeId;
    }

    public Integer getDayNo() {
        return dayNo;
    }

    public void setDayNo(Integer dayNo) {
        this.dayNo = dayNo;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMeals() {
        return meals;
    }

    public void setMeals(String meals) {
        this.meals = meals;
    }

    public String getHotel() {
        return hotel;
    }

    public void setHotel(String hotel) {
        this.hotel = hotel;
    }

    public String getTransport() {
        return transport;
    }

    public void setTransport(String transport) {
        this.transport = transport;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
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
        return "RouteItinerary{" +
            "id = " + id +
            ", routeId = " + routeId +
            ", dayNo = " + dayNo +
            ", title = " + title +
            ", description = " + description +
            ", meals = " + meals +
            ", hotel = " + hotel +
            ", transport = " + transport +
            ", image = " + image +
            ", longitude = " + longitude +
            ", latitude = " + latitude +
            ", address = " + address +
            ", sort = " + sort +
            ", createBy = " + createBy +
            ", createTime = " + createTime +
            ", updateBy = " + updateBy +
            ", updateTime = " + updateTime +
            ", deleted = " + deleted +
        "}";
    }
}
