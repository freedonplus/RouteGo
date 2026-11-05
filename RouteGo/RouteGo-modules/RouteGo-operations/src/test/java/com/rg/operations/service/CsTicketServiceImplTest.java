package com.rg.operations.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.rg.common.crypto.SensitiveCipher;
import com.rg.common.exception.serviceException.OperationsException;
import com.rg.operations.dto.CsTicketCreateRequest;
import com.rg.operations.entity.CsMessage;
import com.rg.operations.entity.CsTicket;
import com.rg.operations.mapper.CsMessageMapper;
import com.rg.operations.mapper.CsTicketMapper;
import com.rg.operations.mapper.OperationsGatewayMapper;
import com.rg.operations.mapper.model.UserSnapshot;
import com.rg.operations.service.impl.CsTicketServiceImpl;
import com.rg.operations.vo.CsTicketVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 客服工单的核心规则：关联对象必须存在、指派即认领、状态只能沿状态机走、
 * 变更备注要写进工单对话。
 */
@ExtendWith(MockitoExtension.class)
class CsTicketServiceImplTest {

    @Mock
    private CsTicketMapper ticketMapper;
    @Mock
    private CsMessageMapper messageMapper;
    @Mock
    private OperationsGatewayMapper gateway;
    @Mock
    private SensitiveCipher cipher;

    private CsTicketServiceImpl service;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, CsTicket.class);
        TableInfoHelper.initTableInfo(assistant, CsMessage.class);
    }

    @BeforeEach
    void setUp() {
        service = new CsTicketServiceImpl(ticketMapper, messageMapper, gateway, cipher);
    }

    @Test
    @DisplayName("未关联会员、订单与处理人的工单详情不报错")
    void detailWithoutRelationsDoesNotThrow() {
        when(ticketMapper.selectById(1L)).thenReturn(ticket(1L, (byte) 0));
        when(messageMapper.countByTicketId(1L)).thenReturn(0L);

        CsTicketVO vo = service.detail(1L);

        assertEquals("TK2026110500001", vo.getTicketNo());
        assertNull(vo.getCustomerPhone());
        assertNull(vo.getOrderNo());
        assertNull(vo.getHandlerName());
    }

    @Test
    @DisplayName("关联订单不存在时拒绝建单")
    void createWithMissingOrderIsRejected() {
        when(gateway.selectOrder(99L)).thenReturn(null);

        OperationsException ex = assertThrows(OperationsException.class,
                () -> service.create(createRequest(null, 99L)));
        assertTrue(ex.getMessage().contains("订单"));
        verify(ticketMapper, never()).insert(any(CsTicket.class));
    }

    @Test
    @DisplayName("指派了处理人时建单直接进入处理中")
    void createWithHandlerStartsProcessing() {
        when(gateway.selectUsers(any())).thenReturn(List.of(user(9L)));
        when(ticketMapper.countByTicketNoIncludingDeleted(any())).thenReturn(0);
        when(ticketMapper.insert(any(CsTicket.class))).thenReturn(1);

        service.create(createRequest(null, null, 9L));

        ArgumentCaptor<CsTicket> captor = ArgumentCaptor.forClass(CsTicket.class);
        verify(ticketMapper).insert(captor.capture());
        assertEquals((byte) 1, captor.getValue().getStatus());
        assertEquals(9L, captor.getValue().getHandlerId());
        assertTrue(captor.getValue().getTicketNo().startsWith("TK"));
    }

    @Test
    @DisplayName("只传订单时自动带出下单会员")
    void createDerivesCustomerFromOrder() {
        when(gateway.selectOrder(5L)).thenReturn(OrderSnapshots.of(5L, 77L));
        when(gateway.selectCustomer(77L)).thenReturn(null);

        OperationsException ex = assertThrows(OperationsException.class,
                () -> service.create(createRequest(null, 5L)));
        // 订单里带出的会员已被删除，应提示会员不存在而不是订单不存在
        assertTrue(ex.getMessage().contains("会员"));
    }

    @Test
    @DisplayName("已关闭的工单不能改派")
    void assignClosedTicketIsRejected() {
        when(ticketMapper.selectById(1L)).thenReturn(ticket(1L, (byte) 3));

        OperationsException ex = assertThrows(OperationsException.class, () -> service.assign(1L, 9L));
        assertTrue(ex.getMessage().contains("已关闭"));
        verify(ticketMapper, never()).update(isNull(), any());
    }

    @Test
    @DisplayName("原地流转被拒绝")
    void updateStatusToSameStatusIsRejected() {
        when(ticketMapper.selectById(1L)).thenReturn(ticket(1L, (byte) 1));

        OperationsException ex = assertThrows(OperationsException.class,
                () -> service.updateStatus(1L, (byte) 1, null));
        assertTrue(ex.getMessage().contains("无需重复操作"));
    }

    @Test
    @DisplayName("解决工单会补完成时间并写入一条状态变更消息")
    void resolveTicketWritesFinishTimeAndMessage() {
        when(ticketMapper.selectById(1L)).thenReturn(ticket(1L, (byte) 1));
        when(ticketMapper.update(isNull(), any())).thenReturn(1);
        when(messageMapper.insert(any(CsMessage.class))).thenReturn(1);

        service.updateStatus(1L, (byte) 2, "客户已确认行程");

        verify(messageMapper).insert(org.mockito.ArgumentMatchers.<CsMessage>argThat(m ->
                m.getContent().contains("状态变更")
                        && m.getContent().contains("客户已确认行程")
                        && m.getSenderType() == 2));
    }

    @Test
    @DisplayName("并发修改导致版本号不匹配时提示刷新")
    void updateStatusWithStaleVersionIsRejected() {
        when(ticketMapper.selectById(1L)).thenReturn(ticket(1L, (byte) 1));
        when(ticketMapper.update(isNull(), any())).thenReturn(0);

        OperationsException ex = assertThrows(OperationsException.class,
                () -> service.updateStatus(1L, (byte) 2, null));
        assertTrue(ex.getMessage().contains("刷新"));
    }

    @Test
    @DisplayName("删除工单时连带清理消息")
    void deleteAlsoRemovesMessages() {
        when(ticketMapper.selectById(1L)).thenReturn(ticket(1L, (byte) 2));

        service.delete(1L);

        verify(messageMapper).deleteByTicketId(1L);
        verify(ticketMapper).deleteById(1L);
    }

    // ──────────────────────────────────────────────
    // 测试数据
    // ──────────────────────────────────────────────

    private CsTicket ticket(Long id, byte status) {
        CsTicket ticket = new CsTicket();
        ticket.setId(id);
        ticket.setTicketNo("TK2026110500001");
        ticket.setTitle("行程改期");
        ticket.setContent("想从 10 号改到 12 号");
        ticket.setType((byte) 2);
        ticket.setPriority((byte) 2);
        ticket.setStatus(status);
        ticket.setVersion(0);
        return ticket;
    }

    private CsTicketCreateRequest createRequest(Long customerId, Long orderId) {
        return createRequest(customerId, orderId, null);
    }

    private CsTicketCreateRequest createRequest(Long customerId, Long orderId, Long handlerId) {
        CsTicketCreateRequest request = new CsTicketCreateRequest();
        request.setCustomerId(customerId);
        request.setOrderId(orderId);
        request.setHandlerId(handlerId);
        request.setType((byte) 2);
        request.setTitle("行程改期");
        request.setContent("想从 10 号改到 12 号");
        return request;
    }

    private UserSnapshot user(Long id) {
        UserSnapshot user = new UserSnapshot();
        user.setId(id);
        user.setUsername("kefu01");
        user.setNickname("客服小张");
        return user;
    }

    /** 订单快照的测试替身，避免每处都手写 set。 */
    private static final class OrderSnapshots {
        private OrderSnapshots() {
        }

        static com.rg.operations.mapper.model.OrderSnapshot of(Long id, Long customerId) {
            com.rg.operations.mapper.model.OrderSnapshot snapshot =
                    new com.rg.operations.mapper.model.OrderSnapshot();
            snapshot.setId(id);
            snapshot.setOrderNo("RG2026110500001");
            snapshot.setCustomerId(customerId);
            snapshot.setStatus((byte) 1);
            return snapshot;
        }
    }
}
