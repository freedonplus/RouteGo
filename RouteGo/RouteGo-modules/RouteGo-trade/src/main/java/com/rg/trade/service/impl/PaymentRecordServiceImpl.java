package com.rg.trade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rg.common.web.PageResult;
import com.rg.trade.dto.PaymentQuery;
import com.rg.trade.entity.Orders;
import com.rg.trade.entity.PaymentRecord;
import com.rg.trade.enums.TradeDict;
import com.rg.trade.mapper.OrdersMapper;
import com.rg.trade.mapper.PaymentRecordMapper;
import com.rg.trade.service.PaymentRecordService;
import com.rg.trade.vo.PaymentRecordVO;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 支付记录查询实现。
 */
@Service
public class PaymentRecordServiceImpl implements PaymentRecordService {

    private static final DateTimeFormatter DATETIME_PARSER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_PARSER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final PaymentRecordMapper paymentRecordMapper;
    private final OrdersMapper ordersMapper;

    public PaymentRecordServiceImpl(PaymentRecordMapper paymentRecordMapper,
                                    OrdersMapper ordersMapper) {
        this.paymentRecordMapper = paymentRecordMapper;
        this.ordersMapper = ordersMapper;
    }

    @Override
    public PageResult<PaymentRecordVO> page(PaymentQuery query) {
        LambdaQueryWrapper<PaymentRecord> wrapper = Wrappers.lambdaQuery();
        if (query.getOrderId() != null) {
            wrapper.eq(PaymentRecord::getOrderId, query.getOrderId());
        }
        if (StringUtils.hasText(query.getPayNo())) {
            wrapper.like(PaymentRecord::getPayNo, query.getPayNo().trim());
        }
        if (query.getPayType() != null) {
            wrapper.eq(PaymentRecord::getPayType, query.getPayType());
        }
        if (query.getStatus() != null) {
            wrapper.eq(PaymentRecord::getStatus, query.getStatus());
        }
        LocalDateTime begin = parseDateTime(query.getPayTimeBegin());
        if (begin != null) {
            wrapper.ge(PaymentRecord::getPayTime, begin);
        }
        LocalDateTime end = parseDateTime(query.getPayTimeEnd());
        if (end != null) {
            wrapper.le(PaymentRecord::getPayTime, end);
        }
        wrapper.orderByDesc(PaymentRecord::getId);

        Page<PaymentRecord> page = paymentRecordMapper.selectPage(query.toPage(), wrapper);
        Map<Long, String> orderNos = loadOrderNos(page.getRecords());
        List<PaymentRecordVO> records = new ArrayList<>(page.getRecords().size());
        for (PaymentRecord record : page.getRecords()) {
            records.add(toVO(record, orderNos.get(record.getOrderId())));
        }
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    @Override
    public List<PaymentRecordVO> listByOrder(Long orderId) {
        List<PaymentRecord> records = paymentRecordMapper.selectList(
                Wrappers.<PaymentRecord>lambdaQuery()
                        .eq(PaymentRecord::getOrderId, orderId)
                        .orderByAsc(PaymentRecord::getId));
        String orderNo = null;
        if (!records.isEmpty()) {
            Orders order = ordersMapper.selectById(orderId);
            orderNo = order == null ? null : order.getOrderNo();
        }
        List<PaymentRecordVO> result = new ArrayList<>(records.size());
        for (PaymentRecord record : records) {
            result.add(toVO(record, orderNo));
        }
        return result;
    }

    private Map<Long, String> loadOrderNos(List<PaymentRecord> records) {
        if (CollectionUtils.isEmpty(records)) {
            return Collections.emptyMap();
        }
        List<Long> ids = records.stream()
                .map(PaymentRecord::getOrderId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return ordersMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Orders::getId, Orders::getOrderNo, (a, b) -> a));
    }

    private PaymentRecordVO toVO(PaymentRecord record, String orderNo) {
        PaymentRecordVO vo = new PaymentRecordVO();
        vo.setId(record.getId());
        vo.setOrderId(record.getOrderId());
        vo.setOrderNo(orderNo);
        vo.setPayNo(record.getPayNo());
        vo.setPayType(record.getPayType());
        vo.setPayTypeDesc(TradeDict.payTypeDesc(record.getPayType()));
        vo.setAmount(record.getAmount());
        vo.setStatus(record.getStatus());
        vo.setStatusDesc(TradeDict.paymentStatusDesc(record.getStatus()));
        vo.setPayTime(record.getPayTime());
        vo.setTransactionId(record.getTransactionId());
        vo.setCreateTime(record.getCreateTime());
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
