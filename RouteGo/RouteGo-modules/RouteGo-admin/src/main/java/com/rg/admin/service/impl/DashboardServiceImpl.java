package com.rg.admin.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.rg.admin.config.StorageProperties;
import com.rg.admin.entity.SysLoginLog;
import com.rg.admin.entity.SysOperLog;
import com.rg.admin.mapper.DashboardMapper;
import com.rg.admin.mapper.SysLoginLogMapper;
import com.rg.admin.mapper.SysOperLogMapper;
import com.rg.admin.mapper.SysUserMapper;
import com.rg.admin.service.DashboardService;
import com.rg.admin.storage.FileStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 仪表盘聚合实现：仅返回统计结果与脱敏摘要，不暴露明细数据。
 * <p>
 * KPI 与趋势来自 sys_login_log / sys_oper_log / sys_user / customer 的真实聚合；
 * 跨域只读查询统一走 {@link DashboardMapper}。
 */
@Slf4j
@Service
public class DashboardServiceImpl implements DashboardService {

    private static final DateTimeFormatter MMDD = DateTimeFormatter.ofPattern("MM-dd");

    private final SysUserMapper userMapper;
    private final SysOperLogMapper operLogMapper;
    private final SysLoginLogMapper loginLogMapper;
    private final DashboardMapper dashboardMapper;
    private final StorageProperties storageProperties;
    private final FileStorage fileStorage;

    public DashboardServiceImpl(SysUserMapper userMapper,
                                SysOperLogMapper operLogMapper,
                                SysLoginLogMapper loginLogMapper,
                                DashboardMapper dashboardMapper,
                                StorageProperties storageProperties,
                                FileStorage fileStorage) {
        this.userMapper = userMapper;
        this.operLogMapper = operLogMapper;
        this.loginLogMapper = loginLogMapper;
        this.dashboardMapper = dashboardMapper;
        this.storageProperties = storageProperties;
        this.fileStorage = fileStorage;
    }

    @Override
    public Map<String, Object> overview() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime yesterdayStart = todayStart.minusDays(1);
        LocalDateTime trendSince = today.minusDays(29).atStartOfDay();

        long todayLogins = loginLogMapper.countByStatusSince((byte) 1, todayStart);
        long yesterdayLogins = loginLogMapper.countByStatusBetween((byte) 1, yesterdayStart, todayStart);
        long adminTotal = userMapper.selectCount(null);
        long memberTotal = dashboardMapper.countCustomers();
        long abnormal7d = loginLogMapper.countByStatusSince((byte) 0, now.minusDays(7));
        double loginDelta = yesterdayLogins == 0
                ? (todayLogins > 0 ? 100.0 : 0.0)
                : Math.round((todayLogins - yesterdayLogins) * 1000.0 / yesterdayLogins) / 10.0;

        List<Map<String, Object>> trend = buildTrend(trendSince);
        List<Integer> loginSpark = trend.subList(Math.max(0, trend.size() - 14), trend.size())
                .stream()
                .map(point -> ((Number) point.get("logins")).intValue())
                .toList();

        List<Map<String, Object>> kpis = new ArrayList<>();
        // KPI 数值用 Integer：全局配置会把 Long 序列化成字符串
        kpis.add(kpi("logins", "今日登录", (int) todayLogins, loginDelta, "次", "较昨日", false,
                loginSpark, "#a8435f"));
        kpis.add(kpi("members", "会员用户", (int) memberTotal, null, "人", "累计注册会员", false,
                List.of(), "#3b82f6"));
        kpis.add(kpi("admins", "管理员总数", (int) adminTotal, null, "人", "全部未删除账号", false,
                List.of(), "#15803d"));
        kpis.add(kpi("abnormal", "7 日异常登录", (int) abnormal7d, null, "次", "失败登录", true,
                List.of(), "#b45309"));

        List<Map<String, Object>> moduleUsage = new ArrayList<>();
        dashboardMapper.moduleUsage(trendSince).forEach(row ->
                moduleUsage.add(Map.of("name", String.valueOf(row.get("name")),
                        "value", ((Number) row.get("total")).intValue())));

        List<Map<String, Object>> recentOperations = new ArrayList<>();
        operLogMapper.selectList(Wrappers.<SysOperLog>lambdaQuery()
                        .orderByDesc(SysOperLog::getOperTime)
                        .last("LIMIT 8"))
                .forEach(operLog -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", operLog.getId());
                    row.put("operTime", operLog.getOperTime());
                    row.put("operName", operLog.getOperName());
                    row.put("title", operLog.getTitle());
                    row.put("method", operLog.getRequestMethod() + " " + operLog.getOperUrl());
                    row.put("status", operLog.getStatus());
                    row.put("costTime", operLog.getCostTime());
                    recentOperations.add(row);
                });

        List<Map<String, Object>> loginFailures = new ArrayList<>();
        dashboardMapper.loginFailures(now.minusDays(7)).forEach(row -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("username", row.get("username"));
            item.put("ip", row.get("ip"));
            item.put("reason", row.get("reason"));
            item.put("time", row.get("time"));
            item.put("count", ((Number) row.get("count")).intValue());
            loginFailures.add(item);
        });

        List<Map<String, Object>> systemStatus = new ArrayList<>();
        systemStatus.add(databaseStatus());
        systemStatus.add(status("api", "API 服务", "正常", "success", "当前请求正常"));
        systemStatus.add(storageStatus());
        systemStatus.add(status("sync", "最后同步",
                now.format(DateTimeFormatter.ofPattern("HH:mm:ss")), "info", "仪表盘实时查询"));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("kpis", kpis);
        data.put("trend", trend);
        data.put("moduleUsage", moduleUsage);
        data.put("recentOperations", recentOperations);
        data.put("loginFailures", loginFailures);
        data.put("systemStatus", systemStatus);
        return data;
    }

    /** 近 30 天登录 / 操作双序列，缺口补零，保证图表 x 轴连续 */
    private List<Map<String, Object>> buildTrend(LocalDateTime since) {
        Map<String, Long> loginsByDay = new HashMap<>();
        dashboardMapper.loginTrend(since).forEach(row ->
                loginsByDay.put(String.valueOf(row.get("label")), ((Number) row.get("total")).longValue()));
        Map<String, Long> opersByDay = new HashMap<>();
        dashboardMapper.operTrend(since).forEach(row ->
                opersByDay.put(String.valueOf(row.get("label")), ((Number) row.get("total")).longValue()));

        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDate start = LocalDate.now().minusDays(29);
        for (int i = 0; i < 30; i++) {
            String label = start.plusDays(i).format(MMDD);
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("label", label);
            // 用 Integer：全局配置会把 Long 序列化成字符串，图表需要数字
            point.put("logins", loginsByDay.getOrDefault(label, 0L).intValue());
            point.put("operations", opersByDay.getOrDefault(label, 0L).intValue());
            trend.add(point);
        }
        return trend;
    }

    private Map<String, Object> kpi(String key, String label, Number value, Double delta,
                                    String unit, String hint, boolean invert,
                                    List<Integer> spark, String sparkColor) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("key", key);
        row.put("label", label);
        row.put("value", value);
        row.put("delta", delta);
        row.put("unit", unit);
        row.put("hint", hint);
        row.put("invert", invert);
        row.put("spark", spark);
        row.put("sparkColor", sparkColor);
        return row;
    }

    private Map<String, Object> databaseStatus() {
        try {
            userMapper.selectCount(null);
            return status("database", "数据库", "正常", "success", "连接正常");
        } catch (RuntimeException e) {
            // 探活失败必须留下原因，否则连接池耗尽、超时等真实故障无从排查
            log.warn("数据库探活失败", e);
            return status("database", "数据库", "异常", "danger", "查询失败");
        }
    }

    private Map<String, Object> storageStatus() {
        String label = "minio".equals(storageProperties.getType()) ? "MinIO" : "本地磁盘";
        String detail = "minio".equals(storageProperties.getType())
                ? "bucket " + storageProperties.getMinio().getBucket()
                : Path.of(storageProperties.getLocalPath()).toString();
        try {
            return fileStorage.healthy()
                    ? status("storage", "文件存储", label, "success", detail)
                    : status("storage", "文件存储", label, "danger", "服务不可达");
        } catch (RuntimeException e) {
            log.warn("文件存储探活失败: {}", label, e);
            return status("storage", "文件存储", label, "danger", "探活失败");
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
