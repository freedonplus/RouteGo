package com.rg.operations.service;

import com.rg.common.web.PageResult;
import com.rg.operations.dto.StatDailyQuery;
import com.rg.operations.dto.StatRebuildRequest;
import com.rg.operations.vo.StatDailyVO;
import com.rg.operations.vo.StatSummaryVO;

import java.time.LocalDate;
import java.util.List;

/**
 * 每日经营统计服务。
 * <p>
 * 数据来源：订单、支付流水、退款单、会员注册记录。定时任务每天凌晨跑一次前一天的
 * 重算，后台也可以手工选区间重算（幂等，可反复执行）。
 * <p>
 * 区间汇总不读 stat_daily，而是直接聚合原始流水，避免某天漏跑任务时汇总数字失真。
 */
public interface StatDailyService {

    PageResult<StatDailyVO> page(StatDailyQuery query);

    StatDailyVO detail(LocalDate statDate);

    StatSummaryVO summary(LocalDate startDate, LocalDate endDate);

    /**
     * 趋势数据：按日期升序的逐日记录，用于折线图。
     */
    List<StatDailyVO> trend(LocalDate startDate, LocalDate endDate);

    /**
     * 重算区间内的统计数据，返回实际写入的天数。
     */
    int rebuild(StatRebuildRequest request);
}
