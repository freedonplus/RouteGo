package com.rg.admin.service;

import java.util.Map;

/**
 * 仪表盘聚合统计。
 */
public interface DashboardService {

    /**
     * 概览数据：KPI、最近操作、异常登录与系统状态。
     */
    Map<String, Object> overview();
}
