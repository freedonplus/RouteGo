package com.rg.operations.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rg.common.exception.ErrorCode;
import com.rg.common.exception.serviceException.OperationsException;
import com.rg.common.storage.TempUrlSigner;
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
    /** 会话列表摘要长度：cs_ticket.last_message 列宽 200，留出余量 */
    private static final int PREVIEW_LENGTH = 100;

    private final CsMessageMapper messageMapper;
    private final CsTicketMapper ticketMapper;
    private final OperationsGatewayMapper gateway;
    private final ObjectMapper objectMapper;
    private final TempUrlSigner tempUrlSigner;

    public CsMessageServiceImpl(CsMessageMapper messageMapper,
                               CsTicketMapper ticketMapper,
                               OperationsGatewayMapper gateway,
                               ObjectMapper objectMapper,
                               TempUrlSigner tempUrlSigner) {
        this.messageMapper = messageMapper;
        this.ticketMapper = ticketMapper;
        this.gateway = gateway;
        this.objectMapper = objectMapper;
        this.tempUrlSigner = tempUrlSigner;
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
        List<CsMessageVO> messages = loadThread(ticketId);
        // 客服打开对话即视为已读，列表上的未读红点随之消失
        ticketMapper.update(null, Wrappers.<CsTicket>lambdaUpdate()
                .eq(CsTicket::getId, ticketId)
                .set(CsTicket::getStaffUnread, 0));
        return messages;
    }

    @Override
    public List<CsMessageVO> listForCustomer(Long ticketId) {
        List<CsMessageVO> messages = loadThread(ticketId);
        ticketMapper.update(null, Wrappers.<CsTicket>lambdaUpdate()
                .eq(CsTicket::getId, ticketId)
                .set(CsTicket::getCustomerUnread, 0));
        return messages;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long send(CsMessageSendRequest request) {
        CsTicket ticket = requireTicket(request.getTicketId());
        if (TicketStateMachine.isTerminal(ticket.getStatus())) {
            throw new OperationsException(ErrorCode.CONFLICT, "工单已关闭，如需继续沟通请新建工单");
        }

        Long operatorId = CurrentOperator.id();
        Long messageId = append(ticket.getId(), SENDER_STAFF, operatorId == null ? 0L : operatorId,
                request.getContent(), request.getImages());

        // 客服首次回复即视为接手：待处理推进到处理中，未指派的顺手认领，
        // 这样客户侧会话页能看到接待人姓名，后台也少一步手动指派
        boolean needAdvance = ticket.getStatus() == TicketStateMachine.PENDING;
        boolean needClaim = ticket.getHandlerId() == null && operatorId != null && operatorId > 0;
        if (needAdvance || needClaim) {
            var update = Wrappers.<CsTicket>lambdaUpdate()
                    .eq(CsTicket::getId, ticket.getId())
                    .eq(CsTicket::getVersion, ticket.getVersion())
                    .set(CsTicket::getUpdateBy, operatorId)
                    .set(CsTicket::getVersion, ticket.getVersion() + 1);
            if (needAdvance) {
                update.set(CsTicket::getStatus, TicketStateMachine.PROCESSING);
            }
            if (needClaim) {
                update.set(CsTicket::getHandlerId, operatorId);
            }
            ticketMapper.update(null, update);
        }
        return messageId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long append(Long ticketId, byte senderType, Long senderId, String content, List<String> images) {
        CsTicket ticket = requireTicket(ticketId);
        String text = content == null ? "" : content.trim();
        List<String> cleaned = cleanImages(images);
        // 允许「只发图片」，但文字和图片一个都不能少
        if (text.isEmpty() && cleaned.isEmpty()) {
            throw new OperationsException("消息内容不能为空");
        }

        CsMessage message = new CsMessage();
        message.setTicketId(ticket.getId());
        message.setSenderType(senderType);
        message.setSenderId(senderId == null ? 0L : senderId);
        message.setContent(text);
        message.setImages(writeImages(cleaned));
        message.setCreateTime(LocalDateTime.now());
        messageMapper.insert(message);

        String preview = text.isEmpty() ? "[图片]" : text.replaceAll("\\s+", " ");
        if (preview.length() > PREVIEW_LENGTH) {
            preview = preview.substring(0, PREVIEW_LENGTH);
        }
        var unreadUpdate = Wrappers.<CsTicket>lambdaUpdate()
                .eq(CsTicket::getId, ticket.getId())
                .set(CsTicket::getLastMessage, preview)
                .set(CsTicket::getLastMessageTime, message.getCreateTime());
        if (senderType == SENDER_STAFF) {
            // 客服发言说明当前对话已被看到，客服侧未读归零，客户侧未读累加
            unreadUpdate.setSql("customer_unread = customer_unread + 1")
                    .set(CsTicket::getStaffUnread, 0);
        } else {
            unreadUpdate.setSql("staff_unread = staff_unread + 1");
        }
        ticketMapper.update(null, unreadUpdate);
        return message.getId();
    }

    // ──────────────────────────────────────────────
    // 内部方法
    // ──────────────────────────────────────────────

    private CsTicket requireTicket(Long ticketId) {
        if (ticketId == null) {
            throw new OperationsException("工单 ID 不能为空");
        }
        CsTicket ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            throw new OperationsException(ErrorCode.NOT_FOUND, "工单不存在或已删除");
        }
        return ticket;
    }

    /** 读取整段对话，正序，供时间线渲染 */
    private List<CsMessageVO> loadThread(Long ticketId) {
        requireTicket(ticketId);
        List<CsMessage> messages = messageMapper.selectList(Wrappers.<CsMessage>lambdaQuery()
                .eq(CsMessage::getTicketId, ticketId)
                .orderByAsc(CsMessage::getCreateTime)
                .orderByAsc(CsMessage::getId));
        return toVOs(messages);
    }

    /** 去空白、去空串、把临时签名链接还原成标准下载地址再落库（签名会过期，不能存库） */
    private List<String> cleanImages(List<String> images) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }
        return images.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .map(tempUrlSigner::canonicalize)
                .toList();
    }

    private String writeImages(List<String> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(images);
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

    /**
     * 出口重签图片链接。
     * <p>
     * 聊天记录的生命周期远长于临时链接（默认 2 小时），库里存的是标准下载地址，
     * 每次读取时按当前密钥重新签发，历史消息里的图片才不会过期变成 401。
     * 非文件地址（如 /covers/*.png）原样返回。
     */
    private List<String> refreshImages(List<String> images) {
        if (images.isEmpty()) {
            return images;
        }
        return images.stream()
                .map(url -> {
                    Long fileId = TempUrlSigner.extractFileId(url);
                    return fileId == null ? url : tempUrlSigner.build(fileId);
                })
                .toList();
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
            vo.setImages(refreshImages(readImages(message.getImages())));
            vo.setCreateTime(message.getCreateTime());
            list.add(vo);
        }
        return list;
    }
}
