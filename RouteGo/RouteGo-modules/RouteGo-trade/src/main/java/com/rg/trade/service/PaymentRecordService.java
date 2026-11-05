package com.rg.trade.service;

import com.rg.common.web.PageResult;
import com.rg.trade.dto.PaymentQuery;
import com.rg.trade.vo.PaymentRecordVO;

import java.util.List;

/**
 * 支付记录服务。
 * <p>
 * 支付动作本身由订单服务完成，这里只提供查询，避免出现「能单独补一条支付记录」的入口。
 */
public interface PaymentRecordService {

    PageResult<PaymentRecordVO> page(PaymentQuery query);

    List<PaymentRecordVO> listByOrder(Long orderId);
}
