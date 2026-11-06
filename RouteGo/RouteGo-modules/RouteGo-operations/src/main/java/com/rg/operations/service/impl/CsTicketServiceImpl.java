package com.rg.operations.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rg.common.crypto.SensitiveCipher;
import com.rg.common.exception.ErrorCode;
import com.rg.common.exception.serviceException.OperationsException;
import com.rg.common.web.PageResult;
import com.rg.operations.dto.CsTicketCreateRequest;
import com.rg.operations.dto.CsTicketQuery;
import com.rg.operations.dto.CsTicketUpdateRequest;
import com.rg.operations.entity.CsMessage;
import com.rg.operations.entity.CsTicket;
import com.rg.operations.enums.OperationsDict;
import com.rg.operations.mapper.CsMessageMapper;
import com.rg.operations.mapper.CsTicketMapper;
import com.rg.operations.mapper.OperationsGatewayMapper;
import com.rg.operations.mapper.model.CustomerSnapshot;
import com.rg.operations.mapper.model.GroupCount;
import com.rg.operations.mapper.model.OrderSnapshot;
import com.rg.operations.mapper.model.UserSnapshot;
import com.rg.operations.service.CsTicketService;
import com.rg.operations.support.BizNoGenerator;
import com.rg.operations.support.CurrentOperator;
import com.rg.operations.support.TicketStateMachine;
import com.rg.operations.vo.CsTicketVO;
import com.rg.operations.vo.TicketStatisticsVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 客服工单业务实现。
 * <p>
 * 两条硬约束：
 * <ul>
 *   <li>状态只能沿状态机走，且每次流转都带乐观锁版本号，防止两个客服同时点「解决」与「关闭」把状态改乱。</li>
 *   <li>工单号靠唯一索引兜底，生成后先查一次「含已删除」的占用，避免删除后重建撞号。</li>
 * </ul>
 * 关联的会员、订单、处理人都可能已被删除，列表补名称时一律做空值兜底，不让整页查询因为一条脏关联失败。
 */
@Service
public class CsTicketServiceImpl extends ServiceImpl<CsTicketMapper, CsTicket> implements CsTicketService {

    private static final int NO_RETRY = 5;

    private final CsTicketMapper ticketMapper;
    private final CsMessageMapper messageMapper;
    private final OperationsGatewayMapper gateway;
    private final SensitiveCipher cipher;

    public CsTicketServiceImpl(CsTicketMapper ticketMapper,
                               CsMessageMapper messageMapper,
                               OperationsGatewayMapper gateway,
                               SensitiveCipher cipher) {
        this.ticketMapper = ticketMapper;
        this.messageMapper = messageMapper;
        this.gateway = gateway;
        this.cipher = cipher;
    }

    @Override
    public PageResult<CsTicketVO> page(CsTicketQuery query) {
        LambdaQueryWrapper<CsTicket> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(query.getTicketNo())) {
            wrapper.like(CsTicket::getTicketNo, query.getTicketNo().trim());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(CsTicket::getTitle, keyword).or().like(CsTicket::getContent, keyword));
        }
        if (query.getCustomerId() != null) {
            wrapper.eq(CsTicket::getCustomerId, query.getCustomerId());
        }
        if (query.getOrderId() != null) {
            wrapper.eq(CsTicket::getOrderId, query.getOrderId());
        }
        if (query.getType() != null) {
            wrapper.eq(CsTicket::getType, query.getType());
        }
        if (query.getPriority() != null) {
            wrapper.eq(CsTicket::getPriority, query.getPriority());
        }
        if (query.getStatus() != null) {
            wrapper.eq(CsTicket::getStatus, query.getStatus());
        }
        if (query.getHandlerId() != null) {
            wrapper.eq(CsTicket::getHandlerId, query.getHandlerId());
        }
        if (Boolean.TRUE.equals(query.getUnhandled())) {
            wrapper.isNull(CsTicket::getHandlerId).eq(CsTicket::getStatus, TicketStateMachine.PENDING);
        }
        if (query.getCreateTimeStart() != null) {
            wrapper.ge(CsTicket::getCreateTime, query.getCreateTimeStart());
        }
        if (query.getCreateTimeEnd() != null) {
            wrapper.le(CsTicket::getCreateTime, query.getCreateTimeEnd());
        }
        wrapper.orderByDesc(CsTicket::getCreateTime).orderByDesc(CsTicket::getId);
        Page<CsTicket> page = ticketMapper.selectPage(query.toPage(), wrapper);
        return new PageResult<>(toVOs(page.getRecords()), page.getTotal(), page.getCurrent(),
                page.getSize(), page.getPages());
    }

    @Override
    public CsTicketVO detail(Long id) {
        CsTicket ticket = requireTicket(id);
        CsTicketVO vo = toVOs(List.of(ticket)).get(0);
        vo.setMessageCount(messageMapper.countByTicketId(id));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(CsTicketCreateRequest request) {
        Long customerId = request.getCustomerId();
        if (request.getOrderId() != null) {
            OrderSnapshot order = gateway.selectOrder(request.getOrderId());
            if (order == null) {
                throw new OperationsException(ErrorCode.NOT_FOUND, "关联订单不存在或已删除");
            }
            // 只传了订单时自动带出下单会员，省得客服再查一遍
            if (customerId == null) {
                customerId = order.getCustomerId();
            }
        }
        if (customerId != null && gateway.selectCustomer(customerId) == null) {
            throw new OperationsException(ErrorCode.NOT_FOUND, "关联会员不存在或已删除");
        }
        Long handlerId = request.getHandlerId();
        if (handlerId != null) {
            requireHandler(handlerId);
        }

        CsTicket ticket = new CsTicket();
        ticket.setTicketNo(nextTicketNo());
        ticket.setCustomerId(customerId);
        ticket.setOrderId(request.getOrderId());
        ticket.setType(request.getType());
        ticket.setTitle(request.getTitle().trim());
        ticket.setContent(request.getContent());
        ticket.setPriority(request.getPriority() == null ? (byte) 2 : request.getPriority());
        ticket.setStatus(handlerId == null ? TicketStateMachine.PENDING : TicketStateMachine.PROCESSING);
        ticket.setHandlerId(handlerId);
        ticket.setVersion(0);
        ticket.setCreateBy(CurrentOperator.id());
        ticket.setCreateTime(LocalDateTime.now());
        ticket.setDeleted((byte) 0);
        ticketMapper.insert(ticket);
        return ticket.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, CsTicketUpdateRequest request) {
        CsTicket ticket = requireTicket(id);
        if (TicketStateMachine.isTerminal(ticket.getStatus())) {
            throw new OperationsException(ErrorCode.CONFLICT, "工单已关闭，不能再修改");
        }
        if (request.getCustomerId() != null && gateway.selectCustomer(request.getCustomerId()) == null) {
            throw new OperationsException(ErrorCode.NOT_FOUND, "关联会员不存在或已删除");
        }
        if (request.getOrderId() != null && gateway.selectOrder(request.getOrderId()) == null) {
            throw new OperationsException(ErrorCode.NOT_FOUND, "关联订单不存在或已删除");
        }
        if (request.getTitle() != null && !StringUtils.hasText(request.getTitle())) {
            throw new OperationsException("工单标题不能为空");
        }

        CsTicket update = new CsTicket();
        update.setId(id);
        update.setCustomerId(request.getCustomerId());
        update.setOrderId(request.getOrderId());
        update.setType(request.getType());
        update.setTitle(request.getTitle() == null ? null : request.getTitle().trim());
        update.setContent(request.getContent());
        update.setVersion(ticket.getVersion());
        update.setUpdateBy(CurrentOperator.id());
        update.setUpdateTime(LocalDateTime.now());
        assertUpdated(ticketMapper.updateById(update));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assign(Long id, Long handlerId) {
        CsTicket ticket = requireTicket(id);
        if (TicketStateMachine.isTerminal(ticket.getStatus())) {
            throw new OperationsException(ErrorCode.CONFLICT, "工单已关闭，不能改派");
        }
        requireHandler(handlerId);
        if (Objects.equals(ticket.getHandlerId(), handlerId)) {
            throw new OperationsException(ErrorCode.CONFLICT, "工单已由该处理人跟进，无需重复指派");
        }

        byte target = ticket.getStatus();
        // 指派即认领：待处理的工单直接进入处理中，避免出现「有人跟但状态还是待处理」的假象
        if (target == TicketStateMachine.PENDING) {
            target = TicketStateMachine.PROCESSING;
        }
        String handler = handlerName(handlerId);
        int rows = ticketMapper.update(null, Wrappers.<CsTicket>lambdaUpdate()
                .eq(CsTicket::getId, id)
                .eq(CsTicket::getVersion, ticket.getVersion())
                .set(CsTicket::getHandlerId, handlerId)
                .set(CsTicket::getStatus, target)
                .set(CsTicket::getUpdateBy, CurrentOperator.id())
                .set(CsTicket::getVersion, ticket.getVersion() + 1));
        assertUpdated(rows);
        appendSystemMessage(id, "工单已指派给 " + handler + "，状态：" + statusText(target));
    }

    @Override
    public void updatePriority(Long id, Byte priority) {
        if (priority == null || priority < 1 || priority > 3) {
            throw new OperationsException("优先级取值为 1（低）、2（中）、3（高）");
        }
        CsTicket ticket = requireTicket(id);
        if (TicketStateMachine.isTerminal(ticket.getStatus())) {
            throw new OperationsException(ErrorCode.CONFLICT, "工单已关闭，不能再调整优先级");
        }
        int rows = ticketMapper.update(null, Wrappers.<CsTicket>lambdaUpdate()
                .eq(CsTicket::getId, id)
                .eq(CsTicket::getVersion, ticket.getVersion())
                .set(CsTicket::getPriority, priority)
                .set(CsTicket::getUpdateBy, CurrentOperator.id())
                .set(CsTicket::getVersion, ticket.getVersion() + 1));
        assertUpdated(rows);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Byte status, String remark) {
        if (status == null || status < 0 || status > 3) {
            throw new OperationsException("状态取值为 0 待处理、1 处理中、2 已解决、3 已关闭");
        }
        CsTicket ticket = requireTicket(id);
        byte from = ticket.getStatus();
        TicketStateMachine.assertTransit(from, status);

        // 进入已解决/已关闭要补完成时间；从已解决重新打开则清空，否则重开的工单会带着旧的完成时间
        LocalDateTime finishTime = null;
        if (status == TicketStateMachine.RESOLVED || status == TicketStateMachine.CLOSED) {
            finishTime = ticket.getFinishTime() == null ? LocalDateTime.now() : ticket.getFinishTime();
        }
        int rows = ticketMapper.update(null, Wrappers.<CsTicket>lambdaUpdate()
                .eq(CsTicket::getId, id)
                .eq(CsTicket::getVersion, ticket.getVersion())
                .set(CsTicket::getStatus, status)
                .set(CsTicket::getFinishTime, finishTime)
                .set(CsTicket::getUpdateBy, CurrentOperator.id())
                .set(CsTicket::getVersion, ticket.getVersion() + 1));
        assertUpdated(rows);

        StringBuilder text = new StringBuilder("状态变更：")
                .append(statusText(from)).append(" → ").append(statusText(status));
        if (StringUtils.hasText(remark)) {
            text.append("，备注：").append(remark.trim());
        }
        appendSystemMessage(id, text.toString());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireTicket(id);
        // 消息是追加型的，工单删了就没有查询入口，直接物理清理，避免留下孤儿数据
        messageMapper.deleteByTicketId(id);
        ticketMapper.deleteById(id);
    }

    @Override
    public TicketStatisticsVO statistics() {
        List<GroupCount> byStatus = ticketMapper.countByStatus();
        Map<Integer, Long> statusMap = byStatus.stream()
                .collect(Collectors.toMap(GroupCount::getGroupKey, GroupCount::getTotal));
        TicketStatisticsVO vo = new TicketStatisticsVO();
        vo.setPending(statusMap.getOrDefault(0, 0L));
        vo.setProcessing(statusMap.getOrDefault(1, 0L));
        vo.setResolved(statusMap.getOrDefault(2, 0L));
        vo.setClosed(statusMap.getOrDefault(3, 0L));
        vo.setTotal(statusMap.values().stream().mapToLong(Long::longValue).sum());
        vo.setUnhandled(ticketMapper.countUnhandled());
        vo.setUrgentPending(ticketMapper.countUrgentPending());

        Map<Integer, Long> typeMap = ticketMapper.countByType().stream()
                .collect(Collectors.toMap(GroupCount::getGroupKey, GroupCount::getTotal));
        Map<String, Long> typeCounts = new LinkedHashMap<>();
        OperationsDict.TICKET_TYPE.forEach((type, label) -> typeCounts.put(label, typeMap.getOrDefault(type, 0L)));
        vo.setTypeCounts(typeCounts);
        return vo;
    }

    // ──────────────────────────────────────────────
    // 内部方法
    // ──────────────────────────────────────────────

    private CsTicket requireTicket(Long id) {
        CsTicket ticket = ticketMapper.selectById(id);
        if (ticket == null) {
            throw new OperationsException(ErrorCode.NOT_FOUND, "工单不存在或已删除");
        }
        return ticket;
    }

    private void requireHandler(Long handlerId) {
        if (handlerId == null) {
            throw new OperationsException("处理人不能为空");
        }
        if (gateway.selectUsers(List.of(handlerId)).isEmpty()) {
            throw new OperationsException(ErrorCode.NOT_FOUND, "处理人不存在或已删除");
        }
    }

    private String handlerName(Long handlerId) {
        List<UserSnapshot> users = gateway.selectUsers(List.of(handlerId));
        if (users.isEmpty()) {
            return String.valueOf(handlerId);
        }
        UserSnapshot user = users.get(0);
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
    }

    private String nextTicketNo() {
        for (int i = 0; i < NO_RETRY; i++) {
            String no = BizNoGenerator.next(BizNoGenerator.TICKET_NO_PREFIX);
            if (ticketMapper.countByTicketNoIncludingDeleted(no) == 0) {
                return no;
            }
        }
        throw new OperationsException(ErrorCode.INTERNAL_ERROR, "工单号生成失败，请重试");
    }

    private void assertUpdated(int rows) {
        if (rows == 0) {
            throw new OperationsException(ErrorCode.CONFLICT, "工单已被其他客服修改，请刷新后重试");
        }
    }

    private void appendSystemMessage(Long ticketId, String content) {
        CsMessage message = new CsMessage();
        message.setTicketId(ticketId);
        message.setSenderType((byte) 2);
        Long operatorId = CurrentOperator.id();
        message.setSenderId(operatorId == null ? 0L : operatorId);
        message.setContent(content);
        message.setCreateTime(LocalDateTime.now());
        messageMapper.insert(message);
    }

    private String statusText(byte status) {
        String desc = OperationsDict.TICKET_STATUS.get((int) status);
        return desc == null ? String.valueOf(status) : desc;
    }

    private List<CsTicketVO> toVOs(List<CsTicket> tickets) {
        if (tickets.isEmpty()) {
            return List.of();
        }
        Map<Long, CustomerSnapshot> customers = loadCustomers(tickets.stream()
                .map(CsTicket::getCustomerId).filter(Objects::nonNull).collect(Collectors.toSet()));
        Map<Long, OrderSnapshot> orders = loadOrders(tickets.stream()
                .map(CsTicket::getOrderId).filter(Objects::nonNull).collect(Collectors.toSet()));
        Map<Long, UserSnapshot> users = loadUsers(tickets.stream()
                .map(CsTicket::getHandlerId).filter(Objects::nonNull).collect(Collectors.toSet()));
        return tickets.stream().map(ticket -> toVO(ticket, customers, orders, users)).toList();
    }

    private CsTicketVO toVO(CsTicket ticket,
                            Map<Long, CustomerSnapshot> customers,
                            Map<Long, OrderSnapshot> orders,
                            Map<Long, UserSnapshot> users) {
        CsTicketVO vo = new CsTicketVO();
        vo.setId(ticket.getId());
        vo.setTicketNo(ticket.getTicketNo());
        vo.setCustomerId(ticket.getCustomerId());
        // 注意：Map.of() 不接受 null 键查询，未关联会员时必须先判空，否则直接 NPE
        if (ticket.getCustomerId() != null) {
            CustomerSnapshot customer = customers.get(ticket.getCustomerId());
            if (customer != null) {
                vo.setCustomerNickname(customer.getNickname());
                vo.setCustomerPhone(cipher.maskPhone(customer.getPhone()));
            }
        }
        vo.setOrderId(ticket.getOrderId());
        if (ticket.getOrderId() != null) {
            OrderSnapshot order = orders.get(ticket.getOrderId());
            if (order != null) {
                vo.setOrderNo(order.getOrderNo());
            }
        }
        vo.setType(ticket.getType());
        vo.setTypeDesc(OperationsDict.of(OperationsDict.TICKET_TYPE, ticket.getType()));
        vo.setTitle(ticket.getTitle());
        vo.setContent(ticket.getContent());
        vo.setPriority(ticket.getPriority());
        vo.setPriorityDesc(OperationsDict.of(OperationsDict.TICKET_PRIORITY, ticket.getPriority()));
        vo.setStatus(ticket.getStatus());
        vo.setStatusDesc(OperationsDict.of(OperationsDict.TICKET_STATUS, ticket.getStatus()));
        vo.setHandlerId(ticket.getHandlerId());
        if (ticket.getHandlerId() != null) {
            UserSnapshot handler = users.get(ticket.getHandlerId());
            if (handler != null) {
                vo.setHandlerName(StringUtils.hasText(handler.getNickname())
                        ? handler.getNickname() : handler.getUsername());
            }
        }
        vo.setFinishTime(ticket.getFinishTime());
        vo.setLastMessageTime(ticket.getLastMessageTime());
        vo.setLastMessage(ticket.getLastMessage());
        vo.setCustomerUnread(ticket.getCustomerUnread() == null ? 0 : ticket.getCustomerUnread());
        vo.setStaffUnread(ticket.getStaffUnread() == null ? 0 : ticket.getStaffUnread());
        vo.setCreateBy(ticket.getCreateBy());
        vo.setCreateTime(ticket.getCreateTime());
        vo.setUpdateTime(ticket.getUpdateTime());
        return vo;
    }

    private Map<Long, CustomerSnapshot> loadCustomers(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return gateway.selectCustomers(ids).stream()
                .collect(Collectors.toMap(CustomerSnapshot::getId, c -> c));
    }

    private Map<Long, OrderSnapshot> loadOrders(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return gateway.selectOrders(ids).stream()
                .collect(Collectors.toMap(OrderSnapshot::getId, o -> o));
    }

    private Map<Long, UserSnapshot> loadUsers(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return gateway.selectUsers(ids).stream()
                .collect(Collectors.toMap(UserSnapshot::getId, u -> u));
    }
}
