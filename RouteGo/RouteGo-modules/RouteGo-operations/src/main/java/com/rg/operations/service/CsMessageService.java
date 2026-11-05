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
 */
public interface CsMessageService {

    PageResult<CsMessageVO> page(CsMessageQuery query);

    /**
     * 某工单的完整对话，按时间正序，供详情页时间线使用。
     */
    List<CsMessageVO> listByTicket(Long ticketId);

    Long send(CsMessageSendRequest request);
}
