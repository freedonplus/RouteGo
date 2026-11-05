package com.rg.operations.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rg.common.exception.ErrorCode;
import com.rg.common.exception.serviceException.OperationsException;
import com.rg.common.web.PageResult;
import com.rg.operations.dto.StatDailyQuery;
import com.rg.operations.dto.StatRebuildRequest;
import com.rg.operations.entity.StatDaily;
import com.rg.operations.mapper.StatAggregateMapper;
import com.rg.operations.mapper.StatDailyMapper;
import com.rg.operations.mapper.model.StatAggRow;
import com.rg.operations.service.StatDailyService;
import com.rg.operations.vo.StatDailyVO;
import com.rg.operations.vo.StatSummaryVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 每日经营统计实现。
 * <p>
 * 重算流程：按区间一次性拉出 4 组聚合（订单数、支付额、退款额、新增会员），
 * 在内存里按日期归并，再逐天 upsert。区间内没有数据的日子也要写 0，
 * 否则折线图会断点、日环比也会算错。
 * <p>
 * 单次重算上限 366 天：真需要补历史请分批跑，别让一条请求锁住几万行。
 */
@Service
public class StatDailyServiceImpl extends ServiceImpl<StatDailyMapper, StatDaily> implements StatDailyService {

    private static final int MAX_REBUILD_DAYS = 366;

    private final StatDailyMapper statDailyMapper;
    private final StatAggregateMapper aggregateMapper;

    public StatDailyServiceImpl(StatDailyMapper statDailyMapper, StatAggregateMapper aggregateMapper) {
        this.statDailyMapper = statDailyMapper;
        this.aggregateMapper = aggregateMapper;
    }

    @Override
    public PageResult<StatDailyVO> page(StatDailyQuery query) {
        LambdaQueryWrapper<StatDaily> wrapper = Wrappers.lambdaQuery();
        if (query.getStartDate() != null) {
            wrapper.ge(StatDaily::getStatDate, query.getStartDate());
        }
        if (query.getEndDate() != null) {
            wrapper.le(StatDaily::getStatDate, query.getEndDate());
        }
        wrapper.orderByDesc(StatDaily::getStatDate);
        Page<StatDaily> page = statDailyMapper.selectPage(query.toPage(), wrapper);
        return new PageResult<>(page.getRecords().stream().map(this::toVO).toList(), page.getTotal(),
                page.getCurrent(), page.getSize(), page.getPages());
    }

    @Override
    public StatDailyVO detail(LocalDate statDate) {
        StatDaily stat = statDailyMapper.selectOne(Wrappers.<StatDaily>lambdaQuery()
                .eq(StatDaily::getStatDate, statDate));
        if (stat == null) {
            throw new OperationsException(ErrorCode.NOT_FOUND, "该日期还没有统计数据，请先执行重算");
        }
        return toVO(stat);
    }

    @Override
    public StatSummaryVO summary(LocalDate startDate, LocalDate endDate) {
        assertRange(startDate, endDate);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime endExclusive = endDate.plusDays(1).atStartOfDay();

        StatSummaryVO vo = new StatSummaryVO();
        vo.setStartDate(startDate);
        vo.setEndDate(endDate);
        List<StatAggRow> paid = aggregateMapper.sumPaid(start, endExclusive);
        vo.setOrderCount(sum(aggregateMapper.countOrders(start, endExclusive), StatAggRow::getOrderCount));
        vo.setPaidOrderCount(sum(paid, StatAggRow::getPaidOrderCount));
        vo.setSalesAmount(amount(paid, StatAggRow::getSalesAmount));
        vo.setRefundAmount(amount(aggregateMapper.sumRefund(start, endExclusive), StatAggRow::getRefundAmount));
        vo.setNewCustomerCount(sum(aggregateMapper.countNewCustomers(start, endExclusive),
                StatAggRow::getNewCustomerCount));
        vo.setNetAmount(vo.getSalesAmount().subtract(vo.getRefundAmount()));
        vo.setAvgOrderAmount(divide(vo.getSalesAmount(), vo.getPaidOrderCount()));
        return vo;
    }

    @Override
    public List<StatDailyVO> trend(LocalDate startDate, LocalDate endDate) {
        assertRange(startDate, endDate);
        List<StatDaily> list = statDailyMapper.selectList(Wrappers.<StatDaily>lambdaQuery()
                .ge(StatDaily::getStatDate, startDate)
                .le(StatDaily::getStatDate, endDate)
                .orderByAsc(StatDaily::getStatDate));
        return list.stream().map(this::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int rebuild(StatRebuildRequest request) {
        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();
        assertRange(startDate, endDate);
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (days > MAX_REBUILD_DAYS) {
            throw new OperationsException("单次最多重算 " + MAX_REBUILD_DAYS + " 天，请分批执行");
        }

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime endExclusive = endDate.plusDays(1).atStartOfDay();
        Map<LocalDate, StatAggRow> merged = new HashMap<>();
        aggregateMapper.countOrders(start, endExclusive).forEach(row ->
                row(merged, row.getStatDate()).setOrderCount(row.getOrderCount()));
        aggregateMapper.sumPaid(start, endExclusive).forEach(row -> {
            StatAggRow target = row(merged, row.getStatDate());
            target.setPaidOrderCount(row.getPaidOrderCount());
            target.setSalesAmount(row.getSalesAmount());
        });
        aggregateMapper.sumRefund(start, endExclusive).forEach(row ->
                row(merged, row.getStatDate()).setRefundAmount(row.getRefundAmount()));
        aggregateMapper.countNewCustomers(start, endExclusive).forEach(row ->
                row(merged, row.getStatDate()).setNewCustomerCount(row.getNewCustomerCount()));

        List<LocalDate> dates = new ArrayList<>((int) days);
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            dates.add(date);
        }
        for (LocalDate date : dates) {
            StatAggRow row = merged.get(date);
            statDailyMapper.upsert(
                    date,
                    row == null || row.getOrderCount() == null ? 0 : row.getOrderCount().intValue(),
                    row == null || row.getPaidOrderCount() == null ? 0 : row.getPaidOrderCount().intValue(),
                    row == null || row.getSalesAmount() == null ? BigDecimal.ZERO : row.getSalesAmount(),
                    row == null || row.getRefundAmount() == null ? BigDecimal.ZERO : row.getRefundAmount(),
                    row == null || row.getNewCustomerCount() == null ? 0 : row.getNewCustomerCount().intValue());
        }
        return dates.size();
    }

    // ──────────────────────────────────────────────
    // 内部方法
    // ──────────────────────────────────────────────

    private void assertRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new OperationsException("起止日期不能为空");
        }
        if (endDate.isBefore(startDate)) {
            throw new OperationsException("结束日期不能早于起始日期");
        }
    }

    private StatAggRow row(Map<LocalDate, StatAggRow> merged, LocalDate date) {
        return merged.computeIfAbsent(date, d -> {
            StatAggRow row = new StatAggRow();
            row.setStatDate(d);
            return row;
        });
    }

    private long sum(List<StatAggRow> rows, Function<StatAggRow, Long> getter) {
        return rows.stream().map(getter).filter(v -> v != null).mapToLong(Long::longValue).sum();
    }

    private BigDecimal amount(List<StatAggRow> rows, Function<StatAggRow, BigDecimal> getter) {
        return rows.stream().map(getter).filter(v -> v != null).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal divide(BigDecimal total, long divisor) {
        if (divisor <= 0) {
            return BigDecimal.ZERO;
        }
        return total.divide(BigDecimal.valueOf(divisor), 2, RoundingMode.HALF_UP);
    }

    private StatDailyVO toVO(StatDaily stat) {
        StatDailyVO vo = new StatDailyVO();
        vo.setId(stat.getId());
        vo.setStatDate(stat.getStatDate());
        vo.setOrderCount(stat.getOrderCount());
        vo.setPaidOrderCount(stat.getPaidOrderCount());
        BigDecimal sales = stat.getSalesAmount() == null ? BigDecimal.ZERO : stat.getSalesAmount();
        BigDecimal refund = stat.getRefundAmount() == null ? BigDecimal.ZERO : stat.getRefundAmount();
        vo.setSalesAmount(sales);
        vo.setRefundAmount(refund);
        vo.setNetAmount(sales.subtract(refund));
        vo.setNewCustomerCount(stat.getNewCustomerCount());
        vo.setAvgOrderAmount(divide(sales, stat.getPaidOrderCount() == null ? 0 : stat.getPaidOrderCount()));
        vo.setUpdateTime(stat.getUpdateTime());
        return vo;
    }
}
