package com.rg.operations.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.rg.common.exception.serviceException.OperationsException;
import com.rg.operations.dto.CsCustomerMessageSendRequest;
import com.rg.operations.dto.CsCustomerTicketCreateRequest;
import com.rg.operations.entity.CsMessage;
import com.rg.operations.entity.CsTicket;
import com.rg.operations.mapper.CsMessageMapper;
import com.rg.operations.mapper.CsTicketMapper;
import com.rg.operations.mapper.OperationsGatewayMapper;
import com.rg.operations.mapper.model.OrderSnapshot;
import com.rg.operations.service.impl.CsChatServiceImpl;
import com.rg.operations.support.AssignDecision;
import com.rg.operations.support.CsAssignEngine;
import com.rg.operations.support.CsStreamHub;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyByte;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * C 端会话的三条底线：归属不能越权、空消息不能发、已关闭不能再发。
 * 另外覆盖「客户追问已解决工单自动重开」这条容易漏的规则。
 */
@ExtendWith(MockitoExtension.class)
class CsChatServiceImplTest {

    private static final long CUSTOMER_ID = 7L;

    @Mock
    private CsTicketMapper ticketMapper;
    @Mock
    private CsMessageMapper messageMapper;
    @Mock
    private CsMessageService messageService;
    @Mock
    private OperationsGatewayMapper gateway;
    @Mock
    private CsAssignEngine assignEngine;
    @Mock
    private CsStreamHub hub;

    private CsChatServiceImpl service;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, CsTicket.class);
        TableInfoHelper.initTableInfo(assistant, CsMessage.class);
    }

    @BeforeEach
    void setUp() {
        service = new CsChatServiceImpl(ticketMapper, messageMapper, messageService, gateway,
                assignEngine, hub);
        // 默认不命中任何分配规则：这些用例聚焦会话本身，分配行为单独在 CsAssignEngineTest 覆盖
        lenient().when(assignEngine.decide(any(), any())).thenReturn(AssignDecision.none());
    }

    @Test
    @DisplayName("文字和图片都为空时拒绝发起咨询")
    void createWithoutContentOrImagesIsRejected() {
        CsCustomerTicketCreateRequest request = new CsCustomerTicketCreateRequest();
        request.setContent("   ");

        OperationsException ex = assertThrows(OperationsException.class,
                () -> service.create(CUSTOMER_ID, request));
        assertTrue(ex.getMessage().contains("图片"));
        verify(ticketMapper, never()).insert(any(CsTicket.class));
    }

    @Test
    @DisplayName("关联别人的订单被拒绝")
    void createWithForeignOrderIsRejected() {
        when(gateway.selectOrder(5L)).thenReturn(order(5L, 99L));
        CsCustomerTicketCreateRequest request = createRequest("订单有问题", 5L);

        OperationsException ex = assertThrows(OperationsException.class,
                () -> service.create(CUSTOMER_ID, request));
        assertTrue(ex.getMessage().contains("自己的订单"));
        verify(ticketMapper, never()).insert(any(CsTicket.class));
    }

    @Test
    @DisplayName("发起咨询：建待处理工单并落首条消息")
    void createStoresTicketAndFirstMessage() {
        when(ticketMapper.countByTicketNoIncludingDeleted(any())).thenReturn(0);
        when(ticketMapper.insert(any(CsTicket.class))).thenAnswer(invocation -> {
            CsTicket ticket = invocation.getArgument(0);
            ticket.setId(100L);
            return 1;
        });

        CsCustomerTicketCreateRequest request = createRequest("酒店没有热水，想换房", null);
        Long ticketId = service.create(CUSTOMER_ID, request);

        assertEquals(100L, ticketId);
        ArgumentCaptor<CsTicket> captor = ArgumentCaptor.forClass(CsTicket.class);
        verify(ticketMapper).insert(captor.capture());
        CsTicket saved = captor.getValue();
        assertEquals((byte) 0, saved.getStatus());
        assertEquals(CUSTOMER_ID, saved.getCustomerId());
        assertTrue(saved.getTicketNo().startsWith("TK"));
        // 标题没填时取描述前 30 字，列表里才有东西可展示
        assertEquals("酒店没有热水，想换房", saved.getTitle());
        verify(messageService).append(eq(100L), eq((byte) 1), eq(CUSTOMER_ID), eq("酒店没有热水，想换房"), anyList());
    }

    @Test
    @DisplayName("只发图片也能发起咨询，标题回落为「图片咨询」")
    void createWithImagesOnly() {
        when(ticketMapper.countByTicketNoIncludingDeleted(any())).thenReturn(0);
        when(ticketMapper.insert(any(CsTicket.class))).thenAnswer(invocation -> {
            CsTicket ticket = invocation.getArgument(0);
            ticket.setId(101L);
            return 1;
        });

        CsCustomerTicketCreateRequest request = new CsCustomerTicketCreateRequest();
        request.setImages(List.of("/admin/system/file/9/download"));

        service.create(CUSTOMER_ID, request);

        ArgumentCaptor<CsTicket> captor = ArgumentCaptor.forClass(CsTicket.class);
        verify(ticketMapper).insert(captor.capture());
        assertEquals("图片咨询", captor.getValue().getTitle());
        assertEquals("[客户上传了图片]", captor.getValue().getContent());
    }

    @Test
    @DisplayName("访问别人的会话返回无权限")
    void detailOfOthersTicketIsRejected() {
        when(ticketMapper.selectById(1L)).thenReturn(ticket(1L, 99L, (byte) 1));

        OperationsException ex = assertThrows(OperationsException.class,
                () -> service.detail(CUSTOMER_ID, 1L));
        assertTrue(ex.getMessage().contains("无权"));
    }

    @Test
    @DisplayName("已关闭的会话不能继续发消息")
    void sendToClosedSessionIsRejected() {
        when(ticketMapper.selectById(1L)).thenReturn(ticket(1L, CUSTOMER_ID, (byte) 3));

        OperationsException ex = assertThrows(OperationsException.class,
                () -> service.send(CUSTOMER_ID, sendRequest(1L, "还在吗")));
        assertTrue(ex.getMessage().contains("已关闭"));
        verify(messageService, never()).append(any(), anyByte(), any(), any(), anyList());
    }

    @Test
    @DisplayName("客户在已解决会话里追问，工单自动回到处理中")
    void sendToResolvedTicketReopensIt() {
        when(ticketMapper.selectById(1L)).thenReturn(ticket(1L, CUSTOMER_ID, (byte) 2));
        when(messageService.append(eq(1L), eq((byte) 1), eq(CUSTOMER_ID), eq("问题还没解决"), any()))
                .thenReturn(88L);
        when(ticketMapper.update(isNull(), any())).thenReturn(1);
        when(messageMapper.insert(any(CsMessage.class))).thenReturn(1);

        Long messageId = service.send(CUSTOMER_ID, sendRequest(1L, "问题还没解决"));

        assertEquals(88L, messageId);
        verify(ticketMapper).update(isNull(), any());
        // 重开要留痕，否则后台看到状态回退会以为有人误操作
        verify(messageMapper).insert(org.mockito.ArgumentMatchers.<CsMessage>argThat(m ->
                m.getContent().contains("重新打开") && m.getSenderType() == 2));
    }

    @Test
    @DisplayName("未读数按会话汇总")
    void unreadSumsCustomerUnread() {
        CsTicket first = ticket(1L, CUSTOMER_ID, (byte) 1);
        first.setCustomerUnread(2);
        CsTicket second = ticket(2L, CUSTOMER_ID, (byte) 1);
        second.setCustomerUnread(3);
        when(ticketMapper.selectList(any())).thenReturn(List.of(first, second));

        assertEquals(5L, service.unread(CUSTOMER_ID));
    }

    @Test
    @DisplayName("结束会话写入关闭状态与完成时间")
    void closeSetsClosedStatus() {
        when(ticketMapper.selectById(1L)).thenReturn(ticket(1L, CUSTOMER_ID, (byte) 1));
        when(ticketMapper.update(isNull(), any())).thenReturn(1);
        when(messageMapper.insert(any(CsMessage.class))).thenReturn(1);

        service.close(CUSTOMER_ID, 1L);

        verify(ticketMapper).update(isNull(), any());
        verify(messageMapper).insert(org.mockito.ArgumentMatchers.<CsMessage>argThat(m ->
                m.getContent().contains("结束")));
    }

    // ──────────────────────────────────────────────
    // 测试数据
    // ──────────────────────────────────────────────

    private CsTicket ticket(Long id, Long customerId, byte status) {
        CsTicket ticket = new CsTicket();
        ticket.setId(id);
        ticket.setTicketNo("TK2026110600001");
        ticket.setCustomerId(customerId);
        ticket.setTitle("酒店没有热水");
        ticket.setContent("酒店没有热水，想换房");
        ticket.setType((byte) 2);
        ticket.setPriority((byte) 2);
        ticket.setStatus(status);
        ticket.setVersion(0);
        return ticket;
    }

    private CsCustomerTicketCreateRequest createRequest(String content, Long orderId) {
        CsCustomerTicketCreateRequest request = new CsCustomerTicketCreateRequest();
        request.setType((byte) 2);
        request.setContent(content);
        request.setOrderId(orderId);
        return request;
    }

    private CsCustomerMessageSendRequest sendRequest(Long ticketId, String content) {
        CsCustomerMessageSendRequest request = new CsCustomerMessageSendRequest();
        request.setTicketId(ticketId);
        request.setContent(content);
        return request;
    }

    private OrderSnapshot order(Long id, Long customerId) {
        OrderSnapshot snapshot = new OrderSnapshot();
        snapshot.setId(id);
        snapshot.setOrderNo("RG2026110600001");
        snapshot.setCustomerId(customerId);
        snapshot.setStatus((byte) 1);
        return snapshot;
    }
}
