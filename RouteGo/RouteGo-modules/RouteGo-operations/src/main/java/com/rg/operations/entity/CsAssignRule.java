package com.rg.operations.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 客服自动分配规则。
 * <p>
 * 新工单没有指定处理人时，按 {@code sort} 升序找第一条命中的启用规则：
 * {@code matchType} / {@code matchPriority} 为空表示不限，两条都为空即「兜底规则」。
 * 候选客服存在 {@code userIds}（逗号分隔），分配时再过滤掉已删除或停用的账号。
 *
 * @author RouteGo
 */
@TableName("cs_assign_rule")
public class CsAssignRule implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 规则名称 */
    @TableField("rule_name")
    private String ruleName;

    /** 适用工单类型；null 表示不限 */
    @TableField("match_type")
    private Byte matchType;

    /** 适用优先级；null 表示不限 */
    @TableField("match_priority")
    private Byte matchPriority;

    /** 分配策略：1 轮询，2 当前在处理最少 */
    @TableField("strategy")
    private Byte strategy;

    /** 候选客服 ID，逗号分隔 */
    @TableField("user_ids")
    private String userIds;

    /** 是否启用：0 停用，1 启用 */
    @TableField("enabled")
    private Byte enabled;

    /** 匹配顺序，数字小的先命中 */
    @TableField("sort")
    private Integer sort;

    /** 备注 */
    @TableField("remark")
    private String remark;

    @Version
    @TableField("version")
    private Integer version;

    @TableField("create_by")
    private Long createBy;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_by")
    private Long updateBy;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("deleted")
    private Byte deleted;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public Byte getMatchType() {
        return matchType;
    }

    public void setMatchType(Byte matchType) {
        this.matchType = matchType;
    }

    public Byte getMatchPriority() {
        return matchPriority;
    }

    public void setMatchPriority(Byte matchPriority) {
        this.matchPriority = matchPriority;
    }

    public Byte getStrategy() {
        return strategy;
    }

    public void setStrategy(Byte strategy) {
        this.strategy = strategy;
    }

    public String getUserIds() {
        return userIds;
    }

    public void setUserIds(String userIds) {
        this.userIds = userIds;
    }

    public Byte getEnabled() {
        return enabled;
    }

    public void setEnabled(Byte enabled) {
        this.enabled = enabled;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
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
        return "CsAssignRule{" +
            "id = " + id +
            ", ruleName = " + ruleName +
            ", matchType = " + matchType +
            ", matchPriority = " + matchPriority +
            ", strategy = " + strategy +
            ", userIds = " + userIds +
            ", enabled = " + enabled +
            ", sort = " + sort +
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
