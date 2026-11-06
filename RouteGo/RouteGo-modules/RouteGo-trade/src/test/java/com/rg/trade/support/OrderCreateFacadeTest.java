package com.rg.trade.support;

import com.rg.common.exception.serviceException.TradeException;
import com.rg.common.redis.DistributedLock;
import com.rg.common.redis.RedisService;
import com.rg.trade.dto.OrderCreateRequest;
import com.rg.trade.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderCreateFacadeTest {

    @Mock
    private OrderService orderService;
    @Mock
    private DistributedLock distributedLock;
    @Mock
    private RedisService redisService;

    private OrderCreateFacade facade;

    @BeforeEach
    void setUp() {
        facade = new OrderCreateFacade(orderService, distributedLock, redisService);
        // 锁在这里不是被测对象，默认放行；个别用例再单独校验调用次数
        Mockito.lenient().when(distributedLock.execute(anyString(), anyLong(), anyLong(),
                        any(TimeUnit.class), ArgumentMatchers.<Supplier<Long>>any()))
                .thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(4)).get());
    }

    private OrderCreateRequest request(Long departureId) {
        OrderCreateRequest request = new OrderCreateRequest();
        request.setCustomerId(11L);
        request.setRouteId(7L);
        request.setDepartureId(departureId);
        request.setTravelerCount(2);
        request.setContactName("张三");
        request.setContactPhone("13912340001");
        return request;
    }

    @Test
    void firstSubmitCreatesOrderAndMarksDone() {
        when(redisService.setIfAbsent(anyString(), any(), any(Duration.class))).thenReturn(true);
        when(orderService.create(any())).thenReturn(99L);

        Long orderId = facade.create(request(5L), "key-1");

        assertThat(orderId).isEqualTo(99L);
        verify(redisService).set(eq("trade:order:idem:11:key-1"), eq("DONE:99"), any(Duration.class));
    }

    @Test
    void duplicateSubmitReturnsSameOrderWithoutCallingService() {
        when(redisService.setIfAbsent(anyString(), any(), any(Duration.class))).thenReturn(false);
        when(redisService.get("trade:order:idem:11:key-1")).thenReturn("DONE:99");

        Long orderId = facade.create(request(5L), "key-1");

        assertThat(orderId).isEqualTo(99L);
        verify(orderService, never()).create(any());
        verify(redisService, never()).set(anyString(), any(), any(Duration.class));
    }

    @Test
    void submitWhilePendingIsRejected() {
        when(redisService.setIfAbsent(anyString(), any(), any(Duration.class))).thenReturn(false);
        when(redisService.get(anyString())).thenReturn("PENDING");

        assertThatThrownBy(() -> facade.create(request(5L), "key-1"))
                .isInstanceOf(TradeException.class)
                .hasMessageContaining("正在处理中");
        verify(orderService, never()).create(any());
    }

    @Test
    void failureReleasesPlaceholderSoUserCanRetry() {
        when(redisService.setIfAbsent(anyString(), any(), any(Duration.class))).thenReturn(true);
        when(orderService.create(any())).thenThrow(new TradeException(
                com.rg.common.exception.ErrorCode.CONFLICT, "该班期余位不足"));

        assertThatThrownBy(() -> facade.create(request(5L), "key-1"))
                .isInstanceOf(TradeException.class);

        verify(redisService).delete("trade:order:idem:11:key-1");
        verify(redisService, never()).set(anyString(), any(), any(Duration.class));
    }

    @Test
    void withoutIdempotencyKeyFallsBackToWindowDedupKey() {
        when(redisService.setIfAbsent(anyString(), any(), any(Duration.class))).thenReturn(true);
        when(orderService.create(any())).thenReturn(1L);

        facade.create(request(5L), null);

        // 未传幂等键时用「客户 + 线路 + 班期 + 人数」做 30 秒窗口去重
        verify(redisService).setIfAbsent(eq("trade:order:dup:11:7:5:2"), any(), any(Duration.class));
    }

    @Test
    void orderWithoutDepartureSkipsLock() {
        when(redisService.setIfAbsent(anyString(), any(), any(Duration.class))).thenReturn(true);
        when(orderService.create(any())).thenReturn(3L);

        facade.create(request(null), "key-2");

        verify(distributedLock, never()).execute(anyString(), anyLong(), anyLong(),
                any(TimeUnit.class), ArgumentMatchers.<Supplier<Object>>any());
    }

    @Test
    void orderWithDepartureTakesLockOnDeparture() {
        when(redisService.setIfAbsent(anyString(), any(), any(Duration.class))).thenReturn(true);
        when(orderService.create(any())).thenReturn(4L);

        facade.create(request(5L), "key-3");

        verify(distributedLock).execute(eq("lock:departure:5"), anyLong(), anyLong(),
                any(TimeUnit.class), ArgumentMatchers.<Supplier<Long>>any());
    }
}
