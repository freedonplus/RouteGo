package com.rg.trade.service.impl;

import com.rg.common.exception.serviceException.TradeException;
import com.rg.trade.dto.RefundApplyRequest;
import com.rg.trade.dto.RefundAuditRequest;
import com.rg.trade.entity.Orders;
import com.rg.trade.entity.RefundRecord;
import com.rg.trade.enums.OrderStatus;
import com.rg.trade.mapper.OrdersMapper;
import com.rg.trade.mapper.RefundRecordMapper;
import com.rg.trade.support.OrderStateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 退款累计校验单测（审核报告 P0-3）：
 * 多次部分退款累计不得超过实付金额；累计退满时订单必须进入「已退款」终态。
 */
@ExtendWith(MockitoExtension.class)
class RefundRecordServiceImplTest {

    @Mock
    private RefundRecordMapper refundRecordMapper;
    @Mock
    private OrdersMapper ordersMapper;
    @Mock
    private OrderStateMachine stateMachine;
    @InjectMocks
    private RefundRecordServiceImpl service;

    private Orders order;

    @BeforeEach
    void setUp() {
        order = new Orders();
        order.setId(100L);
        order.setStatus(OrderStatus.PAID.getCode());
        order.setPayAmount(new BigDecimal("100.00"));
        when(ordersMapper.selectById(100L)).thenReturn(order);
    }

    @Test
    void applyRejectsWhenSingleAmountExceedsPayAmount() {
        RefundApplyRequest request = new RefundApplyRequest();
        request.setAmount(new BigDecimal("120.00"));
        request.setReason("测试");
        when(refundRecordMapper.selectList(any())).thenReturn(List.of());

        assertThrows(TradeException.class, () -> service.apply(100L, request));
    }

    @Test
    void applyRejectsWhenHistoryPlusCurrentExceedsPayAmount() {
        // 历史：已退 40 + 待审核在途 10；本次申请 60 → 累计 110 > 100，必须拒绝
        RefundRecord refunded = refund(3, new BigDecimal("40.00"));
        RefundRecord pending = refund(0, new BigDecimal("10.00"));
        when(refundRecordMapper.selectList(any())).thenReturn(List.of(refunded, pending));

        RefundApplyRequest request = new RefundApplyRequest();
        request.setAmount(new BigDecimal("60.00"));
        request.setReason("第二次部分退款");

        TradeException ex = assertThrows(TradeException.class, () -> service.apply(100L, request));
        assertEquals(400, ex.getCode());
    }

    @Test
    void applyAllowsPartialRefundWithinRemainingBalance() {
        RefundRecord refunded = refund(3, new BigDecimal("40.00"));
        when(refundRecordMapper.selectList(any())).thenReturn(List.of(refunded));
        when(refundRecordMapper.insert(any(RefundRecord.class))).thenReturn(1);

        RefundApplyRequest request = new RefundApplyRequest();
        request.setAmount(new BigDecimal("50.00"));
        request.setReason("部分退款");

        service.apply(100L, request);

        ArgumentCaptor<RefundRecord> captor = ArgumentCaptor.forClass(RefundRecord.class);
        verify(refundRecordMapper).insert(captor.capture());
        assertEquals(0, new BigDecimal("50.00").compareTo(captor.getValue().getAmount()));
        verify(stateMachine).transition(eq(order), eq(OrderStatus.REFUNDING), anyString(), any());
    }

    @Test
    void auditTreatsCumulativeFullRefundAsFinalState() {
        order.setStatus(OrderStatus.REFUNDING.getCode()); // audit 前置：订单须处于「退款中」
        RefundRecord pending = refund(0, new BigDecimal("60.00"));
        pending.setId(9L);
        pending.setOrderId(100L);
        when(refundRecordMapper.selectById(9L)).thenReturn(pending);
        when(refundRecordMapper.updateById(any(RefundRecord.class))).thenReturn(1);
        // 本单通过后 status=3 计入累计：60 + 历史已退 40 = 100 = 实付 → 应走「已退款」终态
        RefundRecord updatedSelf = refund(3, new BigDecimal("60.00"));
        RefundRecord history = refund(3, new BigDecimal("40.00"));
        when(refundRecordMapper.selectList(any())).thenReturn(List.of(updatedSelf, history));

        RefundAuditRequest request = new RefundAuditRequest();
        request.setApproved(Boolean.TRUE);
        request.setAuditRemark("同意");

        service.audit(9L, request);

        verify(stateMachine).transition(eq(order), eq(OrderStatus.REFUNDED), anyString(), any());
    }

    @Test
    void auditKeepsOrderPaidWhenPartialRefundNotFull() {
        order.setStatus(OrderStatus.REFUNDING.getCode()); // audit 前置：订单须处于「退款中」
        RefundRecord pending = refund(0, new BigDecimal("30.00"));
        pending.setId(9L);
        pending.setOrderId(100L);
        when(refundRecordMapper.selectById(9L)).thenReturn(pending);
        when(refundRecordMapper.updateById(any(RefundRecord.class))).thenReturn(1);
        // 累计 30 + 40 = 70 < 100 → 部分退款，订单回「已支付」
        RefundRecord updatedSelf = refund(3, new BigDecimal("30.00"));
        RefundRecord history = refund(3, new BigDecimal("40.00"));
        when(refundRecordMapper.selectList(any())).thenReturn(List.of(updatedSelf, history));

        RefundAuditRequest request = new RefundAuditRequest();
        request.setApproved(Boolean.TRUE);

        service.audit(9L, request);

        verify(stateMachine).transition(eq(order), eq(OrderStatus.PAID), anyString(), any());
    }

    private RefundRecord refund(int status, BigDecimal amount) {
        RefundRecord record = new RefundRecord();
        record.setOrderId(100L);
        record.setStatus((byte) status);
        record.setAmount(amount);
        return record;
    }
}
