package com.rg.trade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rg.common.web.PageResult;
import com.rg.trade.dto.OrderStatusLogQuery;
import com.rg.trade.entity.OrderStatusLog;
import com.rg.trade.enums.OrderStatus;
import com.rg.trade.mapper.OrderStatusLogMapper;
import com.rg.trade.service.OrderStatusLogService;
import com.rg.trade.vo.OrderStatusLogVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单状态流水查询实现。流水只读，不提供增删改接口。
 */
@Service
public class OrderStatusLogServiceImpl implements OrderStatusLogService {

    private static final DateTimeFormatter DATETIME_PARSER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_PARSER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final OrderStatusLogMapper statusLogMapper;

    public OrderStatusLogServiceImpl(OrderStatusLogMapper statusLogMapper) {
        this.statusLogMapper = statusLogMapper;
    }

    @Override
    public PageResult<OrderStatusLogVO> page(OrderStatusLogQuery query) {
        LambdaQueryWrapper<OrderStatusLog> wrapper = Wrappers.lambdaQuery();
        if (query.getOrderId() != null) {
            wrapper.eq(OrderStatusLog::getOrderId, query.getOrderId());
        }
        if (query.getToStatus() != null) {
            wrapper.eq(OrderStatusLog::getToStatus, query.getToStatus());
        }
        if (StringUtils.hasText(query.getOperatorName())) {
            wrapper.like(OrderStatusLog::getOperatorName, query.getOperatorName().trim());
        }
        LocalDateTime begin = parseDateTime(query.getCreateTimeBegin());
        if (begin != null) {
            wrapper.ge(OrderStatusLog::getCreateTime, begin);
        }
        LocalDateTime end = parseDateTime(query.getCreateTimeEnd());
        if (end != null) {
            wrapper.le(OrderStatusLog::getCreateTime, end);
        }
        wrapper.orderByDesc(OrderStatusLog::getId);

        Page<OrderStatusLog> page = statusLogMapper.selectPage(query.toPage(), wrapper);
        List<OrderStatusLogVO> records = new ArrayList<>(page.getRecords().size());
        for (OrderStatusLog log : page.getRecords()) {
            records.add(toVO(log));
        }
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    @Override
    public List<OrderStatusLogVO> listByOrder(Long orderId) {
        List<OrderStatusLog> logs = statusLogMapper.selectList(
                Wrappers.<OrderStatusLog>lambdaQuery()
                        .eq(OrderStatusLog::getOrderId, orderId)
                        .orderByAsc(OrderStatusLog::getId));
        List<OrderStatusLogVO> result = new ArrayList<>(logs.size());
        for (OrderStatusLog log : logs) {
            result.add(toVO(log));
        }
        return result;
    }

    private OrderStatusLogVO toVO(OrderStatusLog log) {
        OrderStatusLogVO vo = new OrderStatusLogVO();
        vo.setId(log.getId());
        vo.setOrderId(log.getOrderId());
        vo.setFromStatus(log.getFromStatus());
        vo.setFromStatusDesc(log.getFromStatus() == null ? null : OrderStatus.describe(log.getFromStatus()));
        vo.setToStatus(log.getToStatus());
        vo.setToStatusDesc(OrderStatus.describe(log.getToStatus()));
        vo.setOperatorId(log.getOperatorId());
        vo.setOperatorName(log.getOperatorName());
        vo.setRemark(log.getRemark());
        vo.setCreateTime(log.getCreateTime());
        return vo;
    }

    private LocalDateTime parseDateTime(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String value = text.trim();
        try {
            if (value.length() == 10) {
                return LocalDate.parse(value, DATE_PARSER).atStartOfDay();
            }
            return LocalDateTime.parse(value, DATETIME_PARSER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
