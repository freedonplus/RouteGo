package com.rg.trade.service;

import com.rg.trade.dto.OrderTravelerSaveRequest;
import com.rg.trade.vo.OrderTravelerVO;

import java.util.List;

/**
 * 出行人服务。证件号与手机号落库前加密，返回时脱敏。
 */
public interface OrderTravelerService {

    List<OrderTravelerVO> listByOrder(Long orderId);

    Long create(Long orderId, OrderTravelerSaveRequest request);

    void update(Long id, OrderTravelerSaveRequest request);

    void delete(Long id);

    /**
     * 覆盖式保存：清空后按入参重建，用于表单一次性提交整单出行人。
     */
    void replaceAll(Long orderId, List<OrderTravelerSaveRequest> items);
}
