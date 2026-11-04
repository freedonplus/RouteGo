package com.rg.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 操作日志表
 * </p>
 *
 * @author RouteGo
 * @since 2026-11-04
 */
@TableName("sys_oper_log")
public class SysOperLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 操作模块
     */
    @TableField("title")
    private String title;

    /**
     * 业务类型：1 新增，2 修改，3 删除，4 查询，5 导出，6 其他
     */
    @TableField("business_type")
    private Byte businessType;

    /**
     * 方法全限定名
     */
    @TableField("method")
    private String method;

    /**
     * 请求方法
     */
    @TableField("request_method")
    private String requestMethod;

    /**
     * 操作人
     */
    @TableField("oper_name")
    private String operName;

    /**
     * 请求 URL
     */
    @TableField("oper_url")
    private String operUrl;

    /**
     * 操作 IP
     */
    @TableField("oper_ip")
    private String operIp;

    /**
     * 请求参数（脱敏、截断）
     */
    @TableField("oper_param")
    private String operParam;

    /**
     * 返回结果（截断）
     */
    @TableField("json_result")
    private String jsonResult;

    /**
     * 状态：0 失败，1 成功
     */
    @TableField("status")
    private Byte status;

    /**
     * 异常信息
     */
    @TableField("error_msg")
    private String errorMsg;

    /**
     * 耗时（毫秒）
     */
    @TableField("cost_time")
    private Long costTime;

    /**
     * 操作时间
     */
    @TableField("oper_time")
    private LocalDateTime operTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Byte getBusinessType() {
        return businessType;
    }

    public void setBusinessType(Byte businessType) {
        this.businessType = businessType;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    public String getOperName() {
        return operName;
    }

    public void setOperName(String operName) {
        this.operName = operName;
    }

    public String getOperUrl() {
        return operUrl;
    }

    public void setOperUrl(String operUrl) {
        this.operUrl = operUrl;
    }

    public String getOperIp() {
        return operIp;
    }

    public void setOperIp(String operIp) {
        this.operIp = operIp;
    }

    public String getOperParam() {
        return operParam;
    }

    public void setOperParam(String operParam) {
        this.operParam = operParam;
    }

    public String getJsonResult() {
        return jsonResult;
    }

    public void setJsonResult(String jsonResult) {
        this.jsonResult = jsonResult;
    }

    public Byte getStatus() {
        return status;
    }

    public void setStatus(Byte status) {
        this.status = status;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public Long getCostTime() {
        return costTime;
    }

    public void setCostTime(Long costTime) {
        this.costTime = costTime;
    }

    public LocalDateTime getOperTime() {
        return operTime;
    }

    public void setOperTime(LocalDateTime operTime) {
        this.operTime = operTime;
    }

    @Override
    public String toString() {
        return "SysOperLog{" +
            "id = " + id +
            ", title = " + title +
            ", businessType = " + businessType +
            ", method = " + method +
            ", requestMethod = " + requestMethod +
            ", operName = " + operName +
            ", operUrl = " + operUrl +
            ", operIp = " + operIp +
            ", operParam = " + operParam +
            ", jsonResult = " + jsonResult +
            ", status = " + status +
            ", errorMsg = " + errorMsg +
            ", costTime = " + costTime +
            ", operTime = " + operTime +
        "}";
    }
}
