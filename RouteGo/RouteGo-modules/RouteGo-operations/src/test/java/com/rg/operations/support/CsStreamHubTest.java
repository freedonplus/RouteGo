package com.rg.operations.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rg.operations.mapper.CsTicketMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 推送中心的几条底线：没人在线时不做无谓查询、每条广播只查一次未读总数、
 * 事务没提交不推、停机必须把连接断干净。
 */
@ExtendWith(MockitoExtension.class)
class CsStreamHubTest {

    @Mock
    private CsTicketMapper ticketMapper;

    private CsStreamHub hub;

    @BeforeEach
    void setUp() {
        hub = new CsStreamHub(ticketMapper, new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        hub.shutdown();
    }

    @Test
    @DisplayName("没有在线客服时直接返回，不查库")
    void skipWhenNobodyOnline() {
        hub.publish("message", 1L, 1, "在吗");

        verifyNoInteractions(ticketMapper);
    }

    @Test
    @DisplayName("一次广播只查一次未读总数，不随连接数放大")
    void queryOncePerBroadcast() {
        hub.open(1L);
        hub.open(2L);
        hub.open(3L);
        clearInvocations(ticketMapper);

        hub.publish("message", 9L, 1, "在吗");

        verify(ticketMapper, times(1)).countStaffUnread();
    }

    @Test
    @DisplayName("同一操作人开多个标签页，各占一条连接")
    void countEachTabSeparately() {
        hub.open(1L);
        hub.open(1L);
        hub.open(2L);

        assertEquals(3, hub.onlineConnections());
    }

    @Test
    @DisplayName("建连即推当前未读，前端首屏不必再拉一次统计")
    void pushUnreadOnOpen() {
        when(ticketMapper.countStaffUnread()).thenReturn(7L);

        hub.open(1L);

        verify(ticketMapper, times(1)).countStaffUnread();
    }

    @Test
    @DisplayName("事务未提交不推送，提交后才发")
    void deferUntilCommit() {
        hub.open(1L);
        clearInvocations(ticketMapper);

        TransactionSynchronizationManager.initSynchronization();
        try {
            hub.publishAfterCommit("message", 1L, 1, "刚发的那条");
            verify(ticketMapper, never()).countStaffUnread();

            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);
            verify(ticketMapper, times(1)).countStaffUnread();
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("没有事务上下文时立即推送，不静默丢弃")
    void publishImmediatelyWithoutTransaction() {
        hub.open(1L);
        clearInvocations(ticketMapper);

        hub.publishAfterCommit("message", 1L, 1, "定时任务里发的");

        verify(ticketMapper, times(1)).countStaffUnread();
    }

    @Test
    @DisplayName("停机断开全部连接，避免优雅停机被长连接拖满")
    void shutdownClosesAll() {
        hub.open(1L);
        hub.open(2L);

        assertDoesNotThrow(() -> hub.shutdown());
        assertEquals(0, hub.onlineConnections());
    }
}
