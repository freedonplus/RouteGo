package com.rg.operations.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rg.common.exception.ErrorCode;
import com.rg.common.exception.serviceException.OperationsException;
import com.rg.common.web.PageResult;
import com.rg.operations.dto.CsCustomerMessageSendRequest;
import com.rg.operations.dto.CsCustomerTicketCreateRequest;
import com.rg.operations.entity.CsMessage;
import com.rg.operations.entity.CsTicket;
import com.rg.operations.enums.OperationsDict;
import com.rg.operations.mapper.CsMessageMapper;
import com.rg.operations.mapper.CsTicketMapper;
import com.rg.operations.mapper.OperationsGatewayMapper;
import com.rg.operations.mapper.model.OrderSnapshot;
import com.rg.operations.mapper.model.UserSnapshot;
import com.rg.operations.service.CsChatService;
import com.rg.operations.service.CsMessageService;
import com.rg.operations.support.BizNoGenerator;
import com.rg.operations.support.TicketStateMachine;
import com.rg.operations.vo.CsMessageVO;
import com.rg.operations.vo.CsSessionVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * C 端客服会话实现。
 * <p>
 * 三条硬约束：
 * <ul>
 *   <li>归属校验：所有按 ticketId 的操作都先确认 {@code customer_id} 是本人，越权一律 403。</li>
 *   <li>终态只读：已关闭的会话不能再发消息，客户想继续沟通只能新建会话。</li>
 *   <li>已解决的会话收到客户追问会自动重开，避免客户在「已解决」里自说自话。</li>
 * </ul>
 * 消息的落库、摘要与未读同步统一委托给 {@link CsMessageService#append}，这里只处理工单状态。
 */
@Service
public class CsChatServiceImpl implements CsChatService {

    private static final byte SENDER_CUSTOMER = 1;
    private static final byte SENDER_SYSTEM = 2;
    private static final int TITLE_LENGTH = 30;
    private static final int TICKET_NO_RETRY = 5;
    private static final long MAX_PAGE_SIZE = 50L;
    /** 只发图片时写进工单内容的占位说明，后台列表能看出客户传了图 */
    private static final String IMAGE_ONLY_CONTENT = "[客户上传了图片]";

    private final CsTicketMapper ticketMapper;
    private final CsMessageMapper messageMapper;
    private final CsMessageService messageService;
    private final OperationsGatewayMapper gateway;

    public CsChatServiceImpl(CsTicketMapper ticketMapper,
                             CsMessageMapper messageMapper,
                             CsMessageService messageService,
                             OperationsGatewayMapper gateway) {
        this.ticketMapper = ticketMapper;
        this.messageMapper = messageMapper;
        this.messageService = messageService;
        this.gateway = gateway;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(Long customerId, CsCustomerTicketCreateRequest request) {
        String content = request.getContent() == null ? "" : request.getContent().trim();
        List<String> images = cleanImages(request.getImages());
        if (content.isEmpty() && images.isEmpty()) {
            throw new OperationsException(ErrorCode.BAD_REQUEST, "请填写问题描述或至少上传一张图片");
        }
        // 关联订单必须属于本人，否则等于允许客户把别人的订单挂到自己的会话上
        if (request.getOrderId() != null) {
            OrderSnapshot order = gateway.selectOrder(request.getOrderId());
            if (order == null) {
                throw new OperationsException(ErrorCode.NOT_FOUND, "关联订单不存在或已删除");
            }
            if (!Objects.equals(order.getCustomerId(), customerId)) {
                throw new OperationsException(ErrorCode.FORBIDDEN, "只能关联自己的订单");
            }
        }

        CsTicket ticket = new CsTicket();
        ticket.setTicketNo(nextTicketNo());
        ticket.setCustomerId(customerId);
        ticket.setOrderId(request.getOrderId());
        ticket.setType(normalizeType(request.getType()));
        ticket.setTitle(resolveTitle(request.getTitle(), content));
        ticket.setContent(content.isEmpty() ? IMAGE_ONLY_CONTENT : content);
        ticket.setPriority((byte) 2);
        ticket.setStatus(TicketStateMachine.PENDING);
        ticket.setVersion(0);
        ticket.setCreateBy(customerId);
        ticket.setCreateTime(LocalDateTime.now());
        ticket.setDeleted((byte) 0);
        ticketMapper.insert(ticket);

        messageService.append(ticket.getId(), SENDER_CUSTOMER, customerId, content, images);
        return ticket.getId();
    }

    @Override
    public PageResult<CsSessionVO> myTickets(Long customerId, Integer status, long pageNum, long pageSize) {
        long current = Math.max(1L, pageNum);
        long size = Math.min(Math.max(1L, pageSize), MAX_PAGE_SIZE);
        LambdaQueryWrapper<CsTicket> wrapper = Wrappers.<CsTicket>lambdaQuery()
                .eq(CsTicket::getCustomerId, customerId);
        if (status != null) {
            wrapper.eq(CsTicket::getStatus, status);
        }
        wrapper.orderByDesc(CsTicket::getLastMessageTime).orderByDesc(CsTicket::getId);

        Page<CsTicket> page = ticketMapper.selectPage(new Page<>(current, size), wrapper);
        Map<Long, String> handlerNames = loadHandlerNames(page.getRecords());
        List<CsSessionVO> records = page.getRecords().stream()
                .map(ticket -> toVO(ticket, handlerNames)).toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    @Override
    public CsSessionVO detail(Long customerId, Long ticketId) {
        CsTicket ticket = requireOwned(customerId, ticketId);
        return toVO(ticket, loadHandlerNames(List.of(ticket)));
    }

    @Override
    public List<CsMessageVO> messages(Long customerId, Long ticketId) {
        requireOwned(customerId, ticketId);
        return messageService.listForCustomer(ticketId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long send(Long customerId, CsCustomerMessageSendRequest request) {
        CsTicket ticket = requireOwned(customerId, request.getTicketId());
        if (TicketStateMachine.isTerminal(ticket.getStatus())) {
            throw new OperationsException(ErrorCode.CONFLICT, "会话已关闭，请重新发起咨询");
        }
        Long messageId = messageService.append(ticket.getId(), SENDER_CUSTOMER, customerId,
                request.getContent(), request.getImages());

        // 已解决只是客服标记，客户接着说话说明问题还在，自动回到处理中
        if (ticket.getStatus() == TicketStateMachine.RESOLVED) {
            int rows = ticketMapper.update(null, Wrappers.<CsTicket>lambdaUpdate()
                    .eq(CsTicket::getId, ticket.getId())
                    .eq(CsTicket::getVersion, ticket.getVersion())
                    .set(CsTicket::getStatus, TicketStateMachine.PROCESSING)
                    .set(CsTicket::getVersion, ticket.getVersion() + 1)
                    .setSql("finish_time = NULL"));
            if (rows > 0) {
                appendSystemMessage(ticket.getId(), "客户有新回复，工单自动重新打开");
            }
        }
        return messageId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void close(Long customerId, Long ticketId) {
        CsTicket ticket = requireOwned(customerId, ticketId);
        if (TicketStateMachine.isTerminal(ticket.getStatus())) {
            throw new OperationsException(ErrorCode.CONFLICT, "会话已关闭，无需重复操作");
        }
        int rows = ticketMapper.update(null, Wrappers.<CsTicket>lambdaUpdate()
                .eq(CsTicket::getId, ticket.getId())
                .eq(CsTicket::getVersion, ticket.getVersion())
                .set(CsTicket::getStatus, TicketStateMachine.CLOSED)
                .set(CsTicket::getFinishTime, LocalDateTime.now())
                // 客户主动结束，未读提示没有意义，顺手清零
                .set(CsTicket::getCustomerUnread, 0)
                .set(CsTicket::getVersion, ticket.getVersion() + 1));
        if (rows == 0) {
            throw new OperationsException(ErrorCode.CONFLICT, "会话状态已变化，请刷新后重试");
        }
        appendSystemMessage(ticket.getId(), "客户结束了本次会话");
    }

    @Override
    public long unread(Long customerId) {
        List<CsTicket> tickets = ticketMapper.selectList(Wrappers.<CsTicket>lambdaQuery()
                .eq(CsTicket::getCustomerId, customerId)
                .select(CsTicket::getCustomerUnread));
        return tickets.stream()
                .map(CsTicket::getCustomerUnread)
                .filter(Objects::nonNull)
                .mapToLong(Integer::longValue)
                .sum();
    }

    // ──────────────────────────────────────────────
    // 内部方法
    // ──────────────────────────────────────────────

    private CsTicket requireOwned(Long customerId, Long ticketId) {
        if (ticketId == null) {
            throw new OperationsException("工单 ID 不能为空");
        }
        CsTicket ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            throw new OperationsException(ErrorCode.NOT_FOUND, "会话不存在或已删除");
        }
        if (!Objects.equals(ticket.getCustomerId(), customerId)) {
            throw new OperationsException(ErrorCode.FORBIDDEN, "无权访问该会话");
        }
        return ticket;
    }

    private byte normalizeType(Byte type) {
        if (type == null) {
            return 1;
        }
        if (type < 1 || type > 5) {
            throw new OperationsException(ErrorCode.BAD_REQUEST, "类型取值为 1-5");
        }
        return type;
    }

    private String resolveTitle(String title, String content) {
        if (StringUtils.hasText(title)) {
            return title.trim();
        }
        if (content.isEmpty()) {
            return "图片咨询";
        }
        String flat = content.replaceAll("\\s+", " ").trim();
        return flat.length() > TITLE_LENGTH ? flat.substring(0, TITLE_LENGTH) : flat;
    }

    private String nextTicketNo() {
        for (int i = 0; i < TICKET_NO_RETRY; i++) {
            String no = BizNoGenerator.next(BizNoGenerator.TICKET_NO_PREFIX);
            if (ticketMapper.countByTicketNoIncludingDeleted(no) == 0) {
                return no;
            }
        }
        throw new OperationsException(ErrorCode.INTERNAL_ERROR, "工单号生成失败，请重试");
    }

    /** 系统留痕：不增未读、不改会话摘要，避免客户看到一屏「状态变更」 */
    private void appendSystemMessage(Long ticketId, String content) {
        CsMessage message = new CsMessage();
        message.setTicketId(ticketId);
        message.setSenderType(SENDER_SYSTEM);
        message.setSenderId(0L);
        message.setContent(content);
        message.setCreateTime(LocalDateTime.now());
        messageMapper.insert(message);
    }

    private List<String> cleanImages(List<String> images) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }
        return images.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList();
    }

    private Map<Long, String> loadHandlerNames(List<CsTicket> tickets) {
        Set<Long> handlerIds = tickets.stream()
                .map(CsTicket::getHandlerId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (handlerIds.isEmpty()) {
            return Map.of();
        }
        return gateway.selectUsers(handlerIds).stream().collect(Collectors.toMap(
                UserSnapshot::getId,
                u -> StringUtils.hasText(u.getNickname()) ? u.getNickname() : u.getUsername(),
                (a, b) -> a));
    }

    private CsSessionVO toVO(CsTicket ticket, Map<Long, String> handlerNames) {
        CsSessionVO vo = new CsSessionVO();
        vo.setId(ticket.getId());
        vo.setTicketNo(ticket.getTicketNo());
        vo.setType(ticket.getType());
        vo.setTypeDesc(OperationsDict.of(OperationsDict.TICKET_TYPE, ticket.getType()));
        vo.setTitle(ticket.getTitle());
        vo.setStatus(ticket.getStatus());
        vo.setStatusDesc(OperationsDict.of(OperationsDict.TICKET_STATUS, ticket.getStatus()));
        vo.setLastMessage(ticket.getLastMessage());
        vo.setLastMessageTime(ticket.getLastMessageTime() == null
                ? ticket.getCreateTime() : ticket.getLastMessageTime());
        vo.setUnread(ticket.getCustomerUnread() == null ? 0 : ticket.getCustomerUnread());
        if (ticket.getHandlerId() != null) {
            vo.setHandlerName(handlerNames.get(ticket.getHandlerId()));
        }
        vo.setCreateTime(ticket.getCreateTime());
        return vo;
    }
}
