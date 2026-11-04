package com.rg.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 后台用户第三方登录绑定表
 * </p>
 *
 * @author RouteGo
 * @since 2026-11-04
 */
@TableName("sys_user_oauth")
public class SysUserOauth implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户 ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 平台：wechat、qq、apple、dingtalk
     */
    @TableField("provider")
    private String provider;

    /**
     * 平台用户唯一标识
     */
    @TableField("openid")
    private String openid;

    /**
     * 开放平台统一标识
     */
    @TableField("unionid")
    private String unionid;

    /**
     * 平台昵称
     */
    @TableField("nickname")
    private String nickname;

    /**
     * 平台头像
     */
    @TableField("avatar")
    private String avatar;

    /**
     * 绑定时间
     */
    @TableField("bind_time")
    private LocalDateTime bindTime;

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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getOpenid() {
        return openid;
    }

    public void setOpenid(String openid) {
        this.openid = openid;
    }

    public String getUnionid() {
        return unionid;
    }

    public void setUnionid(String unionid) {
        this.unionid = unionid;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public LocalDateTime getBindTime() {
        return bindTime;
    }

    public void setBindTime(LocalDateTime bindTime) {
        this.bindTime = bindTime;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "SysUserOauth{" +
            "id = " + id +
            ", userId = " + userId +
            ", provider = " + provider +
            ", openid = " + openid +
            ", unionid = " + unionid +
            ", nickname = " + nickname +
            ", avatar = " + avatar +
            ", bindTime = " + bindTime +
            ", createTime = " + createTime +
        "}";
    }
}
