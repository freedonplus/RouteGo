package com.rg.operations.service;

import com.rg.common.web.PageResult;
import com.rg.operations.dto.CsMessageQuery;
import com.rg.operations.dto.CsMessageSendRequest;
import com.rg.operations.vo.CsMessageVO;

import java.util.List;

/**
 * 工单消息服务。
 * <p>
 * 后台只能以「客服」身份发言，发送人取当前登录账号；客户侧消息由小程序写入。
 * 给已关闭的工单发消息会被拒绝（409），需要继续沟通请重新开工单。
 * <p>
 * 所有新增消息统一走 {@link #append}，由它同步工单上的「最后一条消息」与双向未读数，
 * 避免后台回复、客户回复各写一套导致未读统计对不上。
 */
public interface CsMessageService {

    PageResult<CsMessageVO> page(CsMessageQuery query);

    /**
     * 某工单的完整对话，按时间正序，供后台详情页时间线使用；读取即清空客服未读数。
     */
    List<CsMessageVO> listByTicket(Long ticketId);

    /**
     * 某工单的完整对话，供 C 端使用；读取即清空客户未读数（归属校验由调用方负责）。
     */
    List<CsMessageVO> listForCustomer(Long ticketId);

    Long send(CsMessageSendRequest request);

    /**
     * 追加一条消息，并刷新工单的最后消息摘要与对方未读数。
     *
     * @param senderType 1 客户，2 客服/系统
     * @param images     图片地址，可为空；与 content 至少有一个
     * @return 新消息 ID
     */
    Long append(Long ticketId, byte senderType, Long senderId, String content, List<String> images);
}
