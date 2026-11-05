package com.rg.admin.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 仪表盘聚合查询（只读直查）。
 * <p>
 * 单体共用一个库，仪表盘需要跨登录日志 / 操作日志 / 用户 / 会员几个域的
 * 只读汇总，统一收口在这里，避免散落到各业务 Service。
 */
public interface DashboardMapper {

    /** 近 N 日每日登录成功次数（label = MM-dd） */
    @Select("""
            SELECT DATE_FORMAT(login_time, '%m-%d') AS label, COUNT(*) AS total
            FROM sys_login_log
            WHERE status = 1 AND login_time >= #{since}
            GROUP BY label
            """)
    List<Map<String, Object>> loginTrend(@Param("since") LocalDateTime since);

    /** 近 N 日每日后台操作次数（label = MM-dd） */
    @Select("""
            SELECT DATE_FORMAT(oper_time, '%m-%d') AS label, COUNT(*) AS total
            FROM sys_oper_log
            WHERE oper_time >= #{since}
            GROUP BY label
            """)
    List<Map<String, Object>> operTrend(@Param("since") LocalDateTime since);

    /** 区间内登录日志条数（status: 1 成功 / 0 失败） */
    @Select("""
            SELECT COUNT(*)
            FROM sys_login_log
            WHERE status = #{status} AND login_time >= #{since} AND login_time < #{until}
            """)
    long countLogins(@Param("status") int status,
                     @Param("since") LocalDateTime since,
                     @Param("until") LocalDateTime until);

    /** 管理账号总数 */
    @Select("SELECT COUNT(*) FROM sys_user WHERE deleted = 0")
    long countUsers();

    /** 会员总数（customer 域只读快照） */
    @Select("SELECT COUNT(*) FROM customer WHERE deleted = 0")
    long countCustomers();

    /** 近 N 日操作日志按模块（title）分布 Top 5 */
    @Select("""
            SELECT title AS name, COUNT(*) AS total
            FROM sys_oper_log
            WHERE oper_time >= #{since}
            GROUP BY title
            ORDER BY total DESC
            LIMIT 5
            """)
    List<Map<String, Object>> moduleUsage(@Param("since") LocalDateTime since);

    /** 近 N 日失败登录按账号 + IP 聚合（reason 取最近一条） */
    @Select("""
            SELECT username, ip,
                   COUNT(*) AS count,
                   MAX(login_time) AS time,
                   SUBSTRING_INDEX(GROUP_CONCAT(msg ORDER BY login_time DESC SEPARATOR '||'), '||', 1) AS reason
            FROM sys_login_log
            WHERE status = 0 AND login_time >= #{since}
            GROUP BY username, ip
            ORDER BY count DESC, time DESC
            LIMIT 5
            """)
    List<Map<String, Object>> loginFailures(@Param("since") LocalDateTime since);
}
