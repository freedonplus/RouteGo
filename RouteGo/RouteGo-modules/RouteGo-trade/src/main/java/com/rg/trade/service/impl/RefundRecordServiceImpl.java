package com.rg.trade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rg.common.exception.ErrorCode;
import com.rg.common.exception.serviceException.TradeException;
import com.rg.common.web.PageResult;
import com.rg.trade.dto.RefundApplyRequest;
import com.rg.trade.dto.RefundAuditRequest;
import com.rg.trade.dto.RefundQuery;
import com.rg.trade.entity.Orders;
import com.rg.trade.entity.RefundRecord;
import com.rg.trade.enums.OrderStatus;
import com.rg.trade.enums.TradeDict;
import com.rg.trade.mapper.OrdersMapper;
import com.rg.trade.mapper.RefundRecordMapper;
import com.rg.trade.service.RefundRecordService;
import com.rg.trade.support.CurrentOperator;
import com.rg.trade.support.OrderStateMachine;
import com.rg.trade.vo.RefundRecordVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 退款业务实现。
 * <p>
 * 退款只针对已支付订单；审核通过后：
 * <ul>
 *   <li>全额退款：订单转「已退款」，回补班期库存、回退线路销量与客户消费额</li>
 *   <li>部分退款：金额记账后订单回到「已支付」，统计口径按整单处理，不做扣减</li>
 * </ul>
 */
@Service
public class RefundRecordServiceImpl implements RefundRecordService {

    private static final String REFUND_NO_PREFIX = "RF";
    private static final DateTimeFormatter NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter DATETIME_PARSER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_PARSER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 退款单状态：0 待审核，1 已通过，2 已驳回，3 已退款。 */
    private static final byte STATUS_PENDING = 0;
    private static final byte STATUS_REJECTED = 2;
    private static final byte STATUS_REFUNDED = 3;

    private final RefundRecordMapper refundRecordMapper;
    private final OrdersMapper ordersMapper;
    private final OrderStateMachine stateMachine;

    public RefundRecordServiceImpl(RefundRecordMapper refundRecordMapper,
                                   OrdersMapper ordersMapper,
                                   OrderStateMachine stateMachine) {
        this.refundRecordMapper = refundRecordMapper;
        this.ordersMapper = ordersMapper;
        this.stateMachine = stateMachine;
    }

    @Override
    public PageResult<RefundRecordVO> page(RefundQuery query) {
        LambdaQueryWrapper<RefundRecord> wrapper = Wrappers.lambdaQuery();
        if (query.getOrderId() != null) {
            wrapper.eq(RefundRecord::getOrderId, query.getOrderId());
        }
        if (StringUtils.hasText(query.getRefundNo())) {
            wrapper.like(RefundRecord::getRefundNo, query.getRefundNo().trim());
        }
        if (query.getStatus() != null) {
            wrapper.eq(RefundRecord::getStatus, query.getStatus());
        }
        LocalDateTime begin = parseDateTime(query.getApplyTimeBegin());
        if (begin != null) {
            wrapper.ge(RefundRecord::getApplyTime, begin);
        }
        LocalDateTime end = parseDateTime(query.getApplyTimeEnd());
        if (end != null) {
            wrapper.le(RefundRecord::getApplyTime, end);
        }
        wrapper.orderByDesc(RefundRecord::getId);

        Page<RefundRecord> page = refundRecordMapper.selectPage(query.toPage(), wrapper);
        Map<Long, String> orderNos = loadOrderNos(page.getRecords());
        List<RefundRecordVO> records = new ArrayList<>(page.getRecords().size());
        for (RefundRecord record : page.getRecords()) {
            records.add(toVO(record, orderNos.get(record.getOrderId())));
        }
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    @Override
    public RefundRecordVO detail(Long id) {
        RefundRecord record = requireRefund(id);
        Orders order = ordersMapper.selectById(record.getOrderId());
        return toVO(record, order == null ? null : order.getOrderNo());
    }

    @Override
    public List<RefundRecordVO> listByOrder(Long orderId) {
        List<RefundRecord> records = refundRecordMapper.selectList(
                Wrappers.<RefundRecord>lambdaQuery()
                        .eq(RefundRecord::getOrderId, orderId)
                        .orderByAsc(RefundRecord::getId));
        String orderNo = null;
        if (!records.isEmpty()) {
            Orders order = ordersMapper.selectById(orderId);
            orderNo = order == null ? null : order.getOrderNo();
        }
        List<RefundRecordVO> result = new ArrayList<>(records.size());
        for (RefundRecord record : records) {
            result.add(toVO(record, orderNo));
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long apply(Long orderId, RefundApplyRequest request) {
        Orders order = ordersMapper.selectById(orderId);
        if (order == null) {
            throw new TradeException(ErrorCode.NOT_FOUND, "订单不存在");
        }
        OrderStatus status = OrderStatus.of(order.getStatus());
        if (status == OrderStatus.REFUNDING) {
            throw new TradeException(ErrorCode.CONFLICT, "该订单已有待审核的退款申请");
        }
        if (status != OrderStatus.PAID) {
            throw new TradeException(ErrorCode.CONFLICT,
                    "只有「已支付」订单可以申请退款，当前状态为「" + status.getDesc() + "」");
        }
        // 累计校验：历史已退款 + 在途（待审核）+ 本次 不能超过实付金额。
        // 只比对单笔会让多次部分退款累计超过实付（审核报告 P0-3），这里以订单为口径重新计算。
        java.math.BigDecimal refundedOrPending = refundedAmountOf(orderId);
        java.math.BigDecimal refundable = order.getPayAmount().subtract(refundedOrPending);
        if (refundable.signum() < 0) {
            refundable = java.math.BigDecimal.ZERO;
        }
        if (request.getAmount().compareTo(refundable) > 0) {
            throw new TradeException(ErrorCode.BAD_REQUEST,
                    "退款金额超过可退余额 " + refundable.toPlainString() + " 元（实付 "
                            + order.getPayAmount().toPlainString()
                            + " 元，历史退款及在途 " + refundedOrPending.toPlainString() + " 元）");
        }

        RefundRecord record = new RefundRecord();
        record.setOrderId(orderId);
        record.setRefundNo(generateRefundNo());
        record.setAmount(request.getAmount());
        record.setReason(request.getReason().trim());
        record.setStatus(STATUS_PENDING);
        record.setApplyTime(LocalDateTime.now());
        record.setVersion(0);
        refundRecordMapper.insert(record);

        stateMachine.transition(order, OrderStatus.REFUNDING, "申请退款：" + request.getReason().trim(), null);
        return record.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long id, RefundAuditRequest request) {
        RefundRecord record = requireRefund(id);
        if (record.getStatus() == null || record.getStatus() != STATUS_PENDING) {
            throw new TradeException(ErrorCode.CONFLICT, "该退款单已审核，不能重复操作");
        }
        Orders order = ordersMapper.selectById(record.getOrderId());
        if (order == null) {
            throw new TradeException(ErrorCode.NOT_FOUND, "退款单关联的订单不存在");
        }
        if (OrderStatus.of(order.getStatus()) != OrderStatus.REFUNDING) {
            throw new TradeException(ErrorCode.CONFLICT, "订单状态已变化，请刷新后退款单重试");
        }

        LocalDateTime now = LocalDateTime.now();
        boolean approved = Boolean.TRUE.equals(request.getApproved());
        RefundRecord update = new RefundRecord();
        update.setId(id);
        update.setAuditTime(now);
        update.setAuditBy(CurrentOperator.id());
        update.setAuditRemark(request.getAuditRemark());
        update.setVersion(record.getVersion());

        if (!approved) {
            update.setStatus(STATUS_REJECTED);
            if (refundRecordMapper.updateById(update) == 0) {
                throw new TradeException(ErrorCode.CONFLICT, "退款单已被其他人处理，请刷新后重试");
            }
            stateMachine.transition(order, OrderStatus.PAID, "退款驳回：" + safeRemark(request.getAuditRemark()), null);
            return;
        }

        update.setStatus(STATUS_REFUNDED);
        if (refundRecordMapper.updateById(update) == 0) {
            throw new TradeException(ErrorCode.CONFLICT, "退款单已被其他人处理，请刷新后重试");
        }

        // 退款单已更新为本单终态后再统计累计：累计达到实付即视为整单退完，
        // 进入「已退款」终态触发库存/统计回补；否则部分退款回「已支付」。
        java.math.BigDecimal refundedTotal = refundedAmountOf(order.getId());
        boolean fullRefund = refundedTotal.compareTo(order.getPayAmount()) >= 0;
        if (fullRefund) {
            stateMachine.transition(order, OrderStatus.REFUNDED, "退款完成，全额退还 " + record.getAmount().toPlainString() + " 元",
                    patch -> patch.setPayStatus((byte) 2));
        } else {
            // 部分退款：订单回到已支付，金额已在退款单里留痕
            stateMachine.transition(order, OrderStatus.PAID,
                    "部分退款 " + record.getAmount().toPlainString() + " 元", null);
        }
    }

    private String safeRemark(String remark) {
        return StringUtils.hasText(remark) ? remark.trim() : "无";
    }

    /**
     * 某订单「已占用」的退款金额：已退款 + 待审核（在途）。
     * 已驳回的退款单不占额度。用于防多次部分退款累计超额。
     */
    private java.math.BigDecimal refundedAmountOf(Long orderId) {
        List<RefundRecord> records = refundRecordMapper.selectList(
                Wrappers.<RefundRecord>lambdaQuery()
                        .eq(RefundRecord::getOrderId, orderId)
                        .ne(RefundRecord::getStatus, STATUS_REJECTED));
        return records.stream()
                .map(RefundRecord::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
    }

    private RefundRecord requireRefund(Long id) {
        RefundRecord record = refundRecordMapper.selectById(id);
        if (record == null) {
            throw new TradeException(ErrorCode.NOT_FOUND, "退款单不存在");
        }
        return record;
    }

    private Map<Long, String> loadOrderNos(List<RefundRecord> records) {
        if (CollectionUtils.isEmpty(records)) {
            return Collections.emptyMap();
        }
        List<Long> ids = records.stream()
                .map(RefundRecord::getOrderId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return ordersMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Orders::getId, Orders::getOrderNo, (a, b) -> a));
    }

    private RefundRecordVO toVO(RefundRecord record, String orderNo) {
        RefundRecordVO vo = new RefundRecordVO();
        vo.setId(record.getId());
        vo.setOrderId(record.getOrderId());
        vo.setOrderNo(orderNo);
        vo.setRefundNo(record.getRefundNo());
        vo.setAmount(record.getAmount());
        vo.setReason(record.getReason());
        vo.setStatus(record.getStatus());
        vo.setStatusDesc(TradeDict.refundStatusDesc(record.getStatus()));
        vo.setApplyTime(record.getApplyTime());
        vo.setAuditTime(record.getAuditTime());
        vo.setAuditBy(record.getAuditBy());
        vo.setAuditRemark(record.getAuditRemark());
        vo.setCreateTime(record.getCreateTime());
        return vo;
    }

    private String generateRefundNo() {
        return REFUND_NO_PREFIX + LocalDateTime.now().format(NO_FORMATTER)
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
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
