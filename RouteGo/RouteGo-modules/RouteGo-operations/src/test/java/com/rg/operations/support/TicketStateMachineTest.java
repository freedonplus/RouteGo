package com.rg.operations.support;

import com.rg.common.exception.serviceException.OperationsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 工单状态机流转矩阵。
 * <p>
 * 这里覆盖的是「哪些边合法」，Service 层只负责把非法流转翻译成 409，
 * 所以规则测试放在状态机自己身上，改流转规则时先看这组用例。
 */
class TicketStateMachineTest {

    private static final byte PENDING = 0;
    private static final byte PROCESSING = 1;
    private static final byte RESOLVED = 2;
    private static final byte CLOSED = 3;

    @Test
    @DisplayName("待处理可以进入处理中、已解决、已关闭")
    void pendingCanMoveForward() {
        assertTrue(TicketStateMachine.canTransit(PENDING, PROCESSING));
        assertTrue(TicketStateMachine.canTransit(PENDING, RESOLVED));
        assertTrue(TicketStateMachine.canTransit(PENDING, CLOSED));
    }

    @Test
    @DisplayName("处理中只能解决或关闭，不能退回待处理")
    void processingCannotGoBack() {
        assertFalse(TicketStateMachine.canTransit(PROCESSING, PENDING));
        assertTrue(TicketStateMachine.canTransit(PROCESSING, RESOLVED));
        assertTrue(TicketStateMachine.canTransit(PROCESSING, CLOSED));
    }

    @Test
    @DisplayName("已解决可以重新打开或归档")
    void resolvedCanReopenOrClose() {
        assertTrue(TicketStateMachine.canTransit(RESOLVED, PROCESSING));
        assertTrue(TicketStateMachine.canTransit(RESOLVED, CLOSED));
        assertFalse(TicketStateMachine.canTransit(RESOLVED, PENDING));
    }

    @Test
    @DisplayName("已关闭是终态，任何流转都不允许")
    void closedIsTerminal() {
        assertTrue(TicketStateMachine.isTerminal(CLOSED));
        assertFalse(TicketStateMachine.canTransit(CLOSED, PENDING));
        assertFalse(TicketStateMachine.canTransit(CLOSED, PROCESSING));
        assertFalse(TicketStateMachine.canTransit(CLOSED, RESOLVED));
    }

    @Test
    @DisplayName("原地流转视为冲突，不产生无意义的状态变更")
    void sameStatusIsRejected() {
        assertFalse(TicketStateMachine.canTransit(PENDING, PENDING));
        assertFalse(TicketStateMachine.canTransit(CLOSED, CLOSED));

        OperationsException ex = assertThrows(OperationsException.class,
                () -> TicketStateMachine.assertTransit(PROCESSING, PROCESSING));
        assertTrue(ex.getMessage().contains("无需重复操作"));
    }

    @Test
    @DisplayName("非法流转抛出带中文说明的冲突异常")
    void illegalTransitMessageIsReadable() {
        OperationsException ex = assertThrows(OperationsException.class,
                () -> TicketStateMachine.assertTransit(CLOSED, PROCESSING));
        assertTrue(ex.getMessage().contains("已关闭"));
        assertTrue(ex.getMessage().contains("处理中"));
    }

    @Test
    @DisplayName("合法流转不抛异常")
    void legalTransitPasses() {
        assertDoesNotThrow(() -> TicketStateMachine.assertTransit(PENDING, PROCESSING));
        assertDoesNotThrow(() -> TicketStateMachine.assertTransit(RESOLVED, CLOSED));
    }
}
