package com.rg.trade.job;

import com.rg.common.redis.DistributedLock;
import com.rg.trade.entity.Orders;
import com.rg.trade.enums.OrderStatus;
import com.rg.trade.mapper.OrdersMapper;
import com.rg.trade.support.OrderStateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 超时关单任务单测：开关、选主锁、逐单隔离三类关键行为。
 */
@ExtendWith(MockitoExtension.class)
class OrderTimeoutJobTest {

    @Mock
    private OrdersMapper ordersMapper;
    @Mock
    private OrderStateMachine stateMachine;
    @Mock
    private DistributedLock distributedLock;
    @InjectMocks
    private OrderTimeoutJob job;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(job, "enabled", true);
        ReflectionTestUtils.setField(job, "timeoutMinutes", 30);
        ReflectionTestUtils.setField(job, "scanLimit", 200);
        ReflectionTestUtils.setField(job, "leaseSeconds", 300L);
    }

    @Test
    void disabledJobDoesNothing() {
        ReflectionTestUtils.setField(job, "enabled", false);
        job.closeTimeoutOrders();
        verifyNoInteractions(ordersMapper, stateMachine, distributedLock);
    }

    @Test
    void skipsScanWhenLockNotAcquired() throws InterruptedException {
        when(distributedLock.tryLock(eq(OrderTimeoutJob.LOCK_KEY), anyLong(), anyLong(), any(TimeUnit.class)))
                .thenReturn(false);
        job.closeTimeoutOrders();
        verify(ordersMapper, never()).selectList(any());
        verify(stateMachine, never()).transition(any(), any(), anyString(), any());
        verify(distributedLock, never()).unlock(anyString());
    }

    @Test
    void noTimeoutOrdersMeansNoTransitionButUnlock() throws InterruptedException {
        when(distributedLock.tryLock(anyString(), anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(ordersMapper.selectList(any())).thenReturn(List.of());
        job.closeTimeoutOrders();
        verify(stateMachine, never()).transition(any(), any(), anyString(), any());
        verify(distributedLock).unlock(OrderTimeoutJob.LOCK_KEY);
    }

    @Test
    void closesEachTimeoutOrderIndependently() throws InterruptedException {
        Orders first = order(1L);
        Orders second = order(2L);
        when(distributedLock.tryLock(anyString(), anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(ordersMapper.selectList(any())).thenReturn(List.of(first, second));

        job.closeTimeoutOrders();

        verify(stateMachine).transition(eq(first), eq(OrderStatus.CANCELED),
                eq(OrderTimeoutJob.CANCEL_REASON), any());
        verify(stateMachine).transition(eq(second), eq(OrderStatus.CANCELED),
                eq(OrderTimeoutJob.CANCEL_REASON), any());
        verify(distributedLock).unlock(OrderTimeoutJob.LOCK_KEY);
    }

    @Test
    void oneFailureDoesNotStopTheBatch() throws InterruptedException {
        Orders first = order(1L);
        Orders second = order(2L);
        when(distributedLock.tryLock(anyString(), anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(ordersMapper.selectList(any())).thenReturn(List.of(first, second));
        org.mockito.Mockito.doThrow(new RuntimeException("乐观锁冲突"))
                .when(stateMachine).transition(eq(first), any(), anyString(), any());

        job.closeTimeoutOrders();

        verify(stateMachine).transition(eq(second), eq(OrderStatus.CANCELED),
                eq(OrderTimeoutJob.CANCEL_REASON), any());
        verify(distributedLock).unlock(OrderTimeoutJob.LOCK_KEY);
    }

    private Orders order(Long id) {
        Orders order = new Orders();
        order.setId(id);
        order.setStatus(OrderStatus.UNPAID.getCode());
        order.setVersion(1);
        return order;
    }
}
