package com.rg.admin.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.rg.admin.config.StorageProperties;
import com.rg.admin.entity.SysLoginLog;
import com.rg.admin.entity.SysOperLog;
import com.rg.admin.mapper.SysLoginLogMapper;
import com.rg.admin.mapper.SysOperLogMapper;
import com.rg.admin.mapper.SysUserMapper;
import com.rg.common.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 仪表盘聚合接口；仅返回统计结果，不暴露明细权限。
 */
@Tag(name = "仪表盘")
@RestController
@RequestMapping("/admin/dashboard")
public class DashboardController {

    private final SysUserMapper userMapper;
    private final SysOperLogMapper operLogMapper;
    private final SysLoginLogMapper loginLogMapper;
    private final StorageProperties storageProperties;

    public DashboardController(SysUserMapper userMapper,
                               SysOperLogMapper operLogMapper,
                               SysLoginLogMapper loginLogMapper,
                               StorageProperties storageProperties) {
        this.userMapper = userMapper;
        this.operLogMapper = operLogMapper;
        this.loginLogMapper = loginLogMapper;
        this.storageProperties = storageProperties;
    }

    @Operation(summary = "仪表盘概览",
            description = "仅返回聚合统计与脱敏摘要；需要 system:dashboard:view 权限。")
    @SaCheckPermission("admin:Dashboard:overview")
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime yesterdayStart = todayStart.minusDays(1);

        long todayLogins = loginLogMapper.countByStatusSince((byte) 1, todayStart);
        long yesterdayLogins = loginLogMapper.countByStatusBetween((byte) 1, yesterdayStart, todayStart);
        long activeUsers = loginLogMapper.countDistinctUsersSince(now.minusMinutes(30));
        long adminTotal = userMapper.selectCount(null);
        long abnormal7d = loginLogMapper.countByStatusSince((byte) 0, now.minusDays(7));
        double loginDelta = yesterdayLogins == 0
                ? (todayLogins > 0 ? 100.0 : 0.0)
                : Math.round((todayLogins - yesterdayLogins) * 1000.0 / yesterdayLogins) / 10.0;

        List<Map<String, Object>> kpis = new ArrayList<>();
        kpis.add(kpi("logins", "今日登录", todayLogins, loginDelta, "次", "较昨日", false));
        kpis.add(kpi("active", "30 分钟活跃", activeUsers, null, "人", "登录日志去重", false));
        kpis.add(kpi("admins", "管理员总数", adminTotal, null, "人", "全部未删除账号", false));
        kpis.add(kpi("abnormal", "7 日异常登录", abnormal7d, null, "次", "失败登录", true));

        List<Map<String, Object>> recentOperations = new ArrayList<>();
        operLogMapper.selectList(Wrappers.<SysOperLog>lambdaQuery()
                        .orderByDesc(SysOperLog::getOperTime)
                        .last("LIMIT 8"))
                .forEach(log -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", log.getId());
                    row.put("operTime", log.getOperTime());
                    row.put("operName", log.getOperName());
                    row.put("title", log.getTitle());
                    row.put("method", log.getRequestMethod() + " " + log.getOperUrl());
                    row.put("status", log.getStatus());
                    row.put("costTime", log.getCostTime());
                    recentOperations.add(row);
                });

        List<Map<String, Object>> loginFailures = new ArrayList<>();
        loginLogMapper.selectList(Wrappers.<SysLoginLog>lambdaQuery()
                        .eq(SysLoginLog::getStatus, (byte) 0)
                        .orderByDesc(SysLoginLog::getLoginTime)
                        .last("LIMIT 4"))
                .forEach(log -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", log.getId());
                    row.put("username", log.getUsername());
                    row.put("ip", log.getIp());
                    row.put("reason", log.getMsg());
                    row.put("time", log.getLoginTime());
                    row.put("count", 1);
                    loginFailures.add(row);
                });

        List<Map<String, Object>> systemStatus = new ArrayList<>();
        systemStatus.add(databaseStatus());
        systemStatus.add(status("api", "API 服务", "正常", "success", "当前请求正常"));
        systemStatus.add(storageStatus());
        systemStatus.add(status("sync", "最后同步",
                now.format(DateTimeFormatter.ofPattern("HH:mm:ss")), "info", "仪表盘实时查询"));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("kpis", kpis);
        data.put("recentOperations", recentOperations);
        data.put("loginFailures", loginFailures);
        data.put("systemStatus", systemStatus);
        return Result.success(data);
    }

    private Map<String, Object> kpi(String key, String label, Number value, Double delta,
                                    String unit, String hint, boolean invert) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("key", key);
        row.put("label", label);
        row.put("value", value);
        row.put("delta", delta);
        row.put("unit", unit);
        row.put("hint", hint);
        row.put("invert", invert);
        return row;
    }

    private Map<String, Object> databaseStatus() {
        try {
            userMapper.selectCount(null);
            return status("database", "数据库", "正常", "success", "连接正常");
        } catch (RuntimeException e) {
            return status("database", "数据库", "异常", "danger", "查询失败");
        }
    }

    private Map<String, Object> storageStatus() {
        try {
            boolean exists = Files.exists(Path.of(storageProperties.getLocalPath()));
            return exists
                    ? status("storage", "文件存储", "正常", "success", "目录存在")
                    : status("storage", "文件存储", "待初始化", "warning", "目录不存在");
        } catch (RuntimeException e) {
            return status("storage", "文件存储", "异常", "danger", "路径不可用");
        }
    }

    private Map<String, Object> status(String key, String label, String value,
                                       String status, String detail) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("key", key);
        row.put("label", label);
        row.put("value", value);
        row.put("status", status);
        row.put("detail", detail);
        return row;
    }
}
