package com.rg.operations.service;

import com.rg.common.web.PageResult;
import com.rg.operations.dto.CsTicketCreateRequest;
import com.rg.operations.dto.CsTicketQuery;
import com.rg.operations.dto.CsTicketUpdateRequest;
import com.rg.operations.vo.CsTicketVO;
import com.rg.operations.vo.HandlerWorkloadVO;
import com.rg.operations.vo.TicketStatisticsVO;

import java.util.List;

/**
 * 客服工单服务。
 * <p>
 * 工单生命周期：待处理 → 处理中 → 已解决 → 已关闭，流转规则集中在
 * {@link com.rg.operations.support.TicketStateMachine}。指派处理人会让待处理工单
 * 自动进入处理中；状态变更的备注会作为一条「客服」消息追加到工单对话里，便于复盘。
 */
public interface CsTicketService {

    PageResult<CsTicketVO> page(CsTicketQuery query);

    /**
     * 可指派的处理人候选人：拥有后台角色的账号（排除 App 端会员账号），按在处理单量升序。
     *
     * @param keyword 用户名或昵称关键字，可空
     */
    List<HandlerWorkloadVO> handlers(String keyword);

    CsTicketVO detail(Long id);

    Long create(CsTicketCreateRequest request);

    void update(Long id, CsTicketUpdateRequest request);

    /**
     * 指派处理人。已关闭的工单不允许改派。
     */
    void assign(Long id, Long handlerId);

    void updatePriority(Long id, Byte priority);

    /**
     * 状态流转，可附一句备注，备注会写入工单消息。
     */
    void updateStatus(Long id, Byte status, String remark);

    /**
     * 逻辑删除工单，同时清理其对话消息。
     */
    void delete(Long id);

    TicketStatisticsVO statistics();
}
