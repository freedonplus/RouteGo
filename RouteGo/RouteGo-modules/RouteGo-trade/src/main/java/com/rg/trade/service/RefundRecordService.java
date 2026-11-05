package com.rg.trade.service;

import com.rg.common.web.PageResult;
import com.rg.trade.dto.RefundApplyRequest;
import com.rg.trade.dto.RefundAuditRequest;
import com.rg.trade.dto.RefundQuery;
import com.rg.trade.vo.RefundRecordVO;

import java.util.List;

/**
 * 退款服务。
 * <p>
 * 退款申请把订单置为「退款中」，审核通过后置为「已退款」并回补库存、回退销量与客户消费；
 * 驳回则退回「已支付」。同一订单同时只允许存在一条进行中的退款单。
 */
public interface RefundRecordService {

    PageResult<RefundRecordVO> page(RefundQuery query);

    RefundRecordVO detail(Long id);

    List<RefundRecordVO> listByOrder(Long orderId);

    Long apply(Long orderId, RefundApplyRequest request);

    void audit(Long id, RefundAuditRequest request);
}
