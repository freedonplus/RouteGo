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
 * 线路主表
 * </p>
 *
 * @author RouteGo
 * @since 2026-11-04
 */
@TableName("route")
public class Route implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 分类 ID
     */
    @TableField("category_id")
    private Long categoryId;

    /**
     * 线路编号
     */
    @TableField("route_no")
    private String routeNo;

    /**
     * 线路标题
     */
    @TableField("title")
    private String title;

    /**
     * 副标题/卖点
     */
    @TableField("subtitle")
    private String subtitle;

    /**
     * 封面图
     */
    @TableField("cover_image")
    private String coverImage;

    /**
     * 目的地
     */
    @TableField("destination")
    private String destination;

    /**
     * 出发城市
     */
    @TableField("departure_city")
    private String departureCity;

    /**
     * 目的地经度（GCJ-02）
     */
    @TableField("longitude")
    private BigDecimal longitude;

    /**
     * 目的地纬度（GCJ-02）
     */
    @TableField("latitude")
    private BigDecimal latitude;

    /**
     * 目的地地址
     */
    @TableField("address")
    private String address;

    /**
     * 行程天数
     */
    @TableField("days")
    private Integer days;

    /**
     * 住宿晚数
     */
    @TableField("nights")
    private Integer nights;

    /**
     * 起价
     */
    @TableField("min_price")
    private BigDecimal minPrice;

    /**
     * 原价
     */
    @TableField("original_price")
    private BigDecimal originalPrice;

    /**
     * 线路亮点
     */
    @TableField("highlights")
    private String highlights;

    /**
     * 费用包含
     */
    @TableField("include_items")
    private String includeItems;

    /**
     * 费用不含
     */
    @TableField("exclude_items")
    private String excludeItems;

    /**
     * 预订须知
     */
    @TableField("booking_notice")
    private String bookingNotice;

    /**
     * 销量
     */
    @TableField("sales")
    private Integer sales;

    /**
     * 平均评分
     */
    @TableField("rating")
    private BigDecimal rating;

    /**
     * 状态：0 下架，1 上架
     */
    @TableField("status")
    private Byte status;

    /**
     * 排序
     */
    @TableField("sort")
    private Integer sort;

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

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getRouteNo() {
        return routeNo;
    }

    public void setRouteNo(String routeNo) {
        this.routeNo = routeNo;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getCoverImage() {
        return coverImage;
    }

    public void setCoverImage(String coverImage) {
        this.coverImage = coverImage;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getDepartureCity() {
        return departureCity;
    }

    public void setDepartureCity(String departureCity) {
        this.departureCity = departureCity;
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

    public Integer getDays() {
        return days;
    }

    public void setDays(Integer days) {
        this.days = days;
    }

    public Integer getNights() {
        return nights;
    }

    public void setNights(Integer nights) {
        this.nights = nights;
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(BigDecimal minPrice) {
        this.minPrice = minPrice;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = originalPrice;
    }

    public String getHighlights() {
        return highlights;
    }

    public void setHighlights(String highlights) {
        this.highlights = highlights;
    }

    public String getIncludeItems() {
        return includeItems;
    }

    public void setIncludeItems(String includeItems) {
        this.includeItems = includeItems;
    }

    public String getExcludeItems() {
        return excludeItems;
    }

    public void setExcludeItems(String excludeItems) {
        this.excludeItems = excludeItems;
    }

    public String getBookingNotice() {
        return bookingNotice;
    }

    public void setBookingNotice(String bookingNotice) {
        this.bookingNotice = bookingNotice;
    }

    public Integer getSales() {
        return sales;
    }

    public void setSales(Integer sales) {
        this.sales = sales;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public void setRating(BigDecimal rating) {
        this.rating = rating;
    }

    public Byte getStatus() {
        return status;
    }

    public void setStatus(Byte status) {
        this.status = status;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
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
        return "Route{" +
            "id = " + id +
            ", categoryId = " + categoryId +
            ", routeNo = " + routeNo +
            ", title = " + title +
            ", subtitle = " + subtitle +
            ", coverImage = " + coverImage +
            ", destination = " + destination +
            ", departureCity = " + departureCity +
            ", longitude = " + longitude +
            ", latitude = " + latitude +
            ", address = " + address +
            ", days = " + days +
            ", nights = " + nights +
            ", minPrice = " + minPrice +
            ", originalPrice = " + originalPrice +
            ", highlights = " + highlights +
            ", includeItems = " + includeItems +
            ", excludeItems = " + excludeItems +
            ", bookingNotice = " + bookingNotice +
            ", sales = " + sales +
            ", rating = " + rating +
            ", status = " + status +
            ", sort = " + sort +
            ", version = " + version +
            ", createBy = " + createBy +
            ", createTime = " + createTime +
            ", updateBy = " + updateBy +
            ", updateTime = " + updateTime +
            ", deleted = " + deleted +
        "}";
    }
}
