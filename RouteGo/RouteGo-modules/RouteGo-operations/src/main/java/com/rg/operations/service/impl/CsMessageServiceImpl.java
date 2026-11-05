package com.rg.operations.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rg.common.exception.ErrorCode;
import com.rg.common.exception.serviceException.OperationsException;
import com.rg.common.web.PageResult;
import com.rg.operations.dto.CsMessageQuery;
import com.rg.operations.dto.CsMessageSendRequest;
import com.rg.operations.entity.CsMessage;
import com.rg.operations.entity.CsTicket;
import com.rg.operations.enums.OperationsDict;
import com.rg.operations.mapper.CsMessageMapper;
import com.rg.operations.mapper.CsTicketMapper;
import com.rg.operations.mapper.OperationsGatewayMapper;
import com.rg.operations.mapper.model.CustomerSnapshot;
import com.rg.operations.mapper.model.UserSnapshot;
import com.rg.operations.service.CsMessageService;
import com.rg.operations.support.CurrentOperator;
import com.rg.operations.support.TicketStateMachine;
import com.rg.operations.vo.CsMessageVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 工单消息业务实现。
 * <p>
 * 消息只追加、不修改、不删除，因此没有乐观锁与逻辑删除。图片列表以 JSON 数组落库
 * （cs_message.images），出口再解析成数组，前端可以直接拿到 URL 列表。
 * <p>
 * 发送消息会顺带推进工单：待处理的工单收到客服回复后自动变成「处理中」，
 * 这一步不加系统消息，避免每条回复下面都挂一行状态变更。
 */
@Service
public class CsMessageServiceImpl extends ServiceImpl<CsMessageMapper, CsMessage> implements CsMessageService {

    private static final byte SENDER_CUSTOMER = 1;
    private static final byte SENDER_STAFF = 2;

    private final CsMessageMapper messageMapper;
    private final CsTicketMapper ticketMapper;
    private final OperationsGatewayMapper gateway;
    private final ObjectMapper objectMapper;

    public CsMessageServiceImpl(CsMessageMapper messageMapper,
                               CsTicketMapper ticketMapper,
                               OperationsGatewayMapper gateway,
                               ObjectMapper objectMapper) {
        this.messageMapper = messageMapper;
        this.ticketMapper = ticketMapper;
        this.gateway = gateway;
        this.objectMapper = objectMapper;
    }

    @Override
    public PageResult<CsMessageVO> page(CsMessageQuery query) {
        LambdaQueryWrapper<CsMessage> wrapper = Wrappers.lambdaQuery();
        if (query.getTicketId() != null) {
            wrapper.eq(CsMessage::getTicketId, query.getTicketId());
        }
        if (query.getSenderType() != null) {
            wrapper.eq(CsMessage::getSenderType, query.getSenderType());
        }
        if (query.getSenderId() != null) {
            wrapper.eq(CsMessage::getSenderId, query.getSenderId());
        }
        if (query.getCreateTimeStart() != null) {
            wrapper.ge(CsMessage::getCreateTime, query.getCreateTimeStart());
        }
        if (query.getCreateTimeEnd() != null) {
            wrapper.le(CsMessage::getCreateTime, query.getCreateTimeEnd());
        }
        wrapper.orderByDesc(CsMessage::getCreateTime).orderByDesc(CsMessage::getId);
        Page<CsMessage> page = messageMapper.selectPage(query.toPage(), wrapper);
        return new PageResult<>(toVOs(page.getRecords()), page.getTotal(), page.getCurrent(),
                page.getSize(), page.getPages());
    }

    @Override
    public List<CsMessageVO> listByTicket(Long ticketId) {
        if (ticketId == null) {
            throw new OperationsException("工单 ID 不能为空");
        }
        if (ticketMapper.selectById(ticketId) == null) {
            throw new OperationsException(ErrorCode.NOT_FOUND, "工单不存在或已删除");
        }
        List<CsMessage> messages = messageMapper.selectList(Wrappers.<CsMessage>lambdaQuery()
                .eq(CsMessage::getTicketId, ticketId)
                .orderByAsc(CsMessage::getCreateTime)
                .orderByAsc(CsMessage::getId));
        return toVOs(messages);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long send(CsMessageSendRequest request) {
        CsTicket ticket = ticketMapper.selectById(request.getTicketId());
        if (ticket == null) {
            throw new OperationsException(ErrorCode.NOT_FOUND, "工单不存在或已删除");
        }
        if (TicketStateMachine.isTerminal(ticket.getStatus())) {
            throw new OperationsException(ErrorCode.CONFLICT, "工单已关闭，如需继续沟通请新建工单");
        }

        CsMessage message = new CsMessage();
        message.setTicketId(ticket.getId());
        message.setSenderType(SENDER_STAFF);
        Long operatorId = CurrentOperator.id();
        message.setSenderId(operatorId == null ? 0L : operatorId);
        message.setContent(request.getContent().trim());
        message.setImages(writeImages(request.getImages()));
        message.setCreateTime(LocalDateTime.now());
        messageMapper.insert(message);

        // 客服首次回复即视为接手，把「待处理」推进到「处理中」
        if (ticket.getStatus() == TicketStateMachine.PENDING) {
            ticketMapper.update(null, Wrappers.<CsTicket>lambdaUpdate()
                    .eq(CsTicket::getId, ticket.getId())
                    .eq(CsTicket::getVersion, ticket.getVersion())
                    .set(CsTicket::getStatus, TicketStateMachine.PROCESSING)
                    .set(CsTicket::getUpdateBy, operatorId)
                    .set(CsTicket::getVersion, ticket.getVersion() + 1));
        }
        return message.getId();
    }

    // ──────────────────────────────────────────────
    // 内部方法
    // ──────────────────────────────────────────────

    private String writeImages(List<String> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        List<String> cleaned = images.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList();
        if (cleaned.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(cleaned);
        } catch (Exception e) {
            throw new OperationsException("图片地址序列化失败");
        }
    }

    private List<String> readImages(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            // 历史脏数据不应该让整页 500，解析失败就当没有图片
            return List.of();
        }
    }

    private List<CsMessageVO> toVOs(List<CsMessage> messages) {
        if (messages.isEmpty()) {
            return List.of();
        }
        Set<Long> staffIds = messages.stream()
                .filter(m -> m.getSenderType() != null && m.getSenderType() == SENDER_STAFF)
                .map(CsMessage::getSenderId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> staffNames = staffIds.isEmpty() ? Map.of()
                : gateway.selectUsers(staffIds).stream().collect(Collectors.toMap(
                UserSnapshot::getId,
                u -> StringUtils.hasText(u.getNickname()) ? u.getNickname() : u.getUsername(),
                (a, b) -> a));

        Set<Long> customerIds = messages.stream()
                .filter(m -> m.getSenderType() != null && m.getSenderType() == SENDER_CUSTOMER)
                .map(CsMessage::getSenderId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> customerNames = customerIds.isEmpty() ? Map.of()
                : gateway.selectCustomers(customerIds).stream().collect(Collectors.toMap(
                CustomerSnapshot::getId,
                c -> c.getNickname() == null ? "客户" + c.getId() : c.getNickname(),
                (a, b) -> a));

        List<CsMessageVO> list = new ArrayList<>(messages.size());
        for (CsMessage message : messages) {
            CsMessageVO vo = new CsMessageVO();
            vo.setId(message.getId());
            vo.setTicketId(message.getTicketId());
            vo.setSenderType(message.getSenderType());
            vo.setSenderTypeDesc(OperationsDict.of(OperationsDict.MESSAGE_SENDER_TYPE, message.getSenderType()));
            vo.setSenderId(message.getSenderId());
            boolean staff = message.getSenderType() != null && message.getSenderType() == SENDER_STAFF;
            vo.setSenderName(staff ? staffNames.getOrDefault(message.getSenderId(), "客服")
                    : customerNames.getOrDefault(message.getSenderId(), "客户"));
            vo.setContent(message.getContent());
            vo.setImages(readImages(message.getImages()));
            vo.setCreateTime(message.getCreateTime());
            list.add(vo);
        }
        return list;
    }
}
