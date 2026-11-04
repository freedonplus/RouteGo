package com.rg.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 字典数据表
 * </p>
 *
 * @author RouteGo
 * @since 2026-11-04
 */
@TableName("sys_dict_data")
public class SysDictData implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 字典类型编码
     */
    @TableField("dict_type")
    private String dictType;

    /**
     * 字典标签
     */
    @TableField("dict_label")
    private String dictLabel;

    /**
     * 字典值
     */
    @TableField("dict_value")
    private String dictValue;

    /**
     * 排序
     */
    @TableField("sort")
    private Integer sort;

    /**
     * 样式类名
     */
    @TableField("css_class")
    private String cssClass;

    /**
     * 是否默认：0 否，1 是
     */
    @TableField("is_default")
    private Byte isDefault;

    /**
     * 状态：0 禁用，1 启用
     */
    @TableField("status")
    private Byte status;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;

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

    public String getDictType() {
        return dictType;
    }

    public void setDictType(String dictType) {
        this.dictType = dictType;
    }

    public String getDictLabel() {
        return dictLabel;
    }

    public void setDictLabel(String dictLabel) {
        this.dictLabel = dictLabel;
    }

    public String getDictValue() {
        return dictValue;
    }

    public void setDictValue(String dictValue) {
        this.dictValue = dictValue;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public String getCssClass() {
        return cssClass;
    }

    public void setCssClass(String cssClass) {
        this.cssClass = cssClass;
    }

    public Byte getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Byte isDefault) {
        this.isDefault = isDefault;
    }

    public Byte getStatus() {
        return status;
    }

    public void setStatus(Byte status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
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
        return "SysDictData{" +
            "id = " + id +
            ", dictType = " + dictType +
            ", dictLabel = " + dictLabel +
            ", dictValue = " + dictValue +
            ", sort = " + sort +
            ", cssClass = " + cssClass +
            ", isDefault = " + isDefault +
            ", status = " + status +
            ", remark = " + remark +
            ", createBy = " + createBy +
            ", createTime = " + createTime +
            ", updateBy = " + updateBy +
            ", updateTime = " + updateTime +
            ", deleted = " + deleted +
        "}";
    }
}
