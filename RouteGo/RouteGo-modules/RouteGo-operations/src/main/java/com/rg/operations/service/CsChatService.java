package com.rg.operations.service;

import com.rg.common.web.PageResult;
import com.rg.operations.dto.CsCustomerMessageSendRequest;
import com.rg.operations.dto.CsCustomerTicketCreateRequest;
import com.rg.operations.vo.CsMessageVO;
import com.rg.operations.vo.CsSessionVO;

import java.util.List;

/**
 * C 端客服会话服务。
 * <p>
 * 与后台工单服务的分工：后台负责派单、流转、统计；这里只负责「客户这一侧的对话」。
 * 所有方法都以 {@code customerId} 作为归属边界，越权访问一律 403，
 * 客户 ID 必须由 Controller 从登录态注入，不能来自请求体。
 */
public interface CsChatService {

    /**
     * 发起咨询：建工单 + 落首条消息（文字 / 图片至少一个），返回工单 ID。
     */
    Long create(Long customerId, CsCustomerTicketCreateRequest request);

    /**
     * 我的会话列表，按最后一条消息时间倒序。
     */
    PageResult<CsSessionVO> myTickets(Long customerId, Integer status, long pageNum, long pageSize);

    CsSessionVO detail(Long customerId, Long ticketId);

    /**
     * 会话消息，正序返回；读取后客户未读清零。
     */
    List<CsMessageVO> messages(Long customerId, Long ticketId);

    /**
     * 客户发送消息（文字 / 图片至少一个）；已解决的工单会自动重新打开。
     */
    Long send(Long customerId, CsCustomerMessageSendRequest request);

    /**
     * 客户结束会话：置为已关闭（终态），之后只能新建会话。
     */
    void close(Long customerId, Long ticketId);

    /**
     * 我的未读总数，供「我的」页与首页入口做红点。
     */
    long unread(Long customerId);
}
