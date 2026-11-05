package com.rg.trade.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.rg.common.crypto.SensitiveCipher;
import com.rg.common.exception.ErrorCode;
import com.rg.common.exception.serviceException.TradeException;
import com.rg.trade.dto.OrderTravelerSaveRequest;
import com.rg.trade.entity.OrderTraveler;
import com.rg.trade.entity.Orders;
import com.rg.trade.enums.OrderStatus;
import com.rg.trade.enums.TradeDict;
import com.rg.trade.mapper.OrderTravelerMapper;
import com.rg.trade.mapper.OrdersMapper;
import com.rg.trade.service.OrderTravelerService;
import com.rg.trade.vo.OrderTravelerVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 出行人业务实现。
 * <p>
 * order_traveler 没有 deleted 列，覆盖式保存用物理删除，避免残留脏行撑大名单。
 */
@Service
public class OrderTravelerServiceImpl implements OrderTravelerService {

    private final OrderTravelerMapper travelerMapper;
    private final OrdersMapper ordersMapper;
    private final SensitiveCipher cipher;

    public OrderTravelerServiceImpl(OrderTravelerMapper travelerMapper,
                                    OrdersMapper ordersMapper,
                                    SensitiveCipher cipher) {
        this.travelerMapper = travelerMapper;
        this.ordersMapper = ordersMapper;
        this.cipher = cipher;
    }

    @Override
    public List<OrderTravelerVO> listByOrder(Long orderId) {
        List<OrderTraveler> travelers = travelerMapper.selectList(
                Wrappers.<OrderTraveler>lambdaQuery()
                        .eq(OrderTraveler::getOrderId, orderId)
                        .orderByAsc(OrderTraveler::getId));
        List<OrderTravelerVO> result = new ArrayList<>(travelers.size());
        for (OrderTraveler traveler : travelers) {
            result.add(toVO(traveler));
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(Long orderId, OrderTravelerSaveRequest request) {
        requireEditableOrder(orderId);
        OrderTraveler traveler = new OrderTraveler();
        fill(traveler, request);
        traveler.setOrderId(orderId);
        travelerMapper.insert(traveler);
        return traveler.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, OrderTravelerSaveRequest request) {
        OrderTraveler exists = travelerMapper.selectById(id);
        if (exists == null) {
            throw new TradeException(ErrorCode.NOT_FOUND, "出行人不存在");
        }
        requireEditableOrder(exists.getOrderId());
        OrderTraveler traveler = new OrderTraveler();
        traveler.setId(id);
        fill(traveler, request);
        travelerMapper.updateById(traveler);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        OrderTraveler exists = travelerMapper.selectById(id);
        if (exists == null) {
            throw new TradeException(ErrorCode.NOT_FOUND, "出行人不存在");
        }
        requireEditableOrder(exists.getOrderId());
        travelerMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceAll(Long orderId, List<OrderTravelerSaveRequest> items) {
        requireEditableOrder(orderId);
        travelerMapper.delete(Wrappers.<OrderTraveler>lambdaQuery().eq(OrderTraveler::getOrderId, orderId));
        if (CollectionUtils.isEmpty(items)) {
            return;
        }
        for (OrderTravelerSaveRequest item : items) {
            OrderTraveler traveler = new OrderTraveler();
            fill(traveler, item);
            traveler.setOrderId(orderId);
            travelerMapper.insert(traveler);
        }
    }

    /**
     * 终态与退款中的订单不允许再改名单。
     */
    private void requireEditableOrder(Long orderId) {
        Orders order = ordersMapper.selectById(orderId);
        if (order == null) {
            throw new TradeException(ErrorCode.NOT_FOUND, "订单不存在");
        }
        OrderStatus status = OrderStatus.of(order.getStatus());
        if (status.isFinal() || status == OrderStatus.REFUNDING) {
            throw new TradeException(ErrorCode.CONFLICT,
                    "订单处于「" + status.getDesc() + "」，不能再修改出行人");
        }
    }

    private void fill(OrderTraveler traveler, OrderTravelerSaveRequest request) {
        traveler.setName(request.getName().trim());
        traveler.setGender(request.getGender());
        traveler.setTravelerType(request.getTravelerType() == null ? (byte) 1 : request.getTravelerType());
        traveler.setRemark(request.getRemark());
        // 证件号与手机号敏感，落库前统一加密；留空时不写入空串，保持 NULL 语义
        traveler.setIdCard(StringUtils.hasText(request.getIdCard())
                ? cipher.encrypt(request.getIdCard().trim()) : null);
        traveler.setPhone(StringUtils.hasText(request.getPhone())
                ? cipher.encrypt(request.getPhone().trim()) : null);
    }

    private OrderTravelerVO toVO(OrderTraveler traveler) {
        OrderTravelerVO vo = new OrderTravelerVO();
        vo.setId(traveler.getId());
        vo.setOrderId(traveler.getOrderId());
        vo.setName(traveler.getName());
        vo.setIdCard(cipher.maskIdCard(traveler.getIdCard()));
        vo.setPhone(cipher.maskPhone(traveler.getPhone()));
        vo.setGender(traveler.getGender());
        vo.setGenderDesc(TradeDict.genderDesc(traveler.getGender()));
        vo.setTravelerType(traveler.getTravelerType());
        vo.setTravelerTypeDesc(TradeDict.travelerTypeDesc(traveler.getTravelerType()));
        vo.setRemark(traveler.getRemark());
        vo.setCreateTime(traveler.getCreateTime());
        return vo;
    }
}
