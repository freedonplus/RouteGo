package com.rg.trade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rg.common.crypto.SensitiveCipher;
import com.rg.common.exception.ErrorCode;
import com.rg.common.exception.serviceException.TradeException;
import com.rg.common.web.PageResult;
import com.rg.trade.dto.OrderCancelRequest;
import com.rg.trade.dto.OrderCreateRequest;
import com.rg.trade.dto.OrderPayRequest;
import com.rg.trade.dto.OrderQuery;
import com.rg.trade.dto.OrderTravelerSaveRequest;
import com.rg.trade.dto.OrderUpdateRequest;
import com.rg.trade.entity.Orders;
import com.rg.trade.entity.PaymentRecord;
import com.rg.trade.enums.OrderStatus;
import com.rg.trade.enums.TradeDict;
import com.rg.trade.mapper.CatalogGatewayMapper;
import com.rg.trade.mapper.CustomerGatewayMapper;
import com.rg.trade.mapper.OrdersMapper;
import com.rg.trade.mapper.PaymentRecordMapper;
import com.rg.trade.mapper.model.CustomerSnapshot;
import com.rg.trade.mapper.model.DepartureSnapshot;
import com.rg.trade.mapper.model.RouteSnapshot;
import com.rg.trade.service.OrderService;
import com.rg.trade.service.OrderStatusLogService;
import com.rg.trade.service.OrderTravelerService;
import com.rg.trade.service.PaymentRecordService;
import com.rg.trade.service.RefundRecordService;
import com.rg.trade.support.CurrentOperator;
import com.rg.trade.support.OrderStateMachine;
import com.rg.trade.vo.OrderVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 订单业务实现。
 */
@Service
public class OrderServiceImpl implements OrderService {

    private static final String ORDER_NO_PREFIX = "RG";
    private static final String PAY_NO_PREFIX = "PAY";
    private static final DateTimeFormatter NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter DATETIME_PARSER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_PARSER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int ORDER_NO_RETRY = 5;

    private final OrdersMapper ordersMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final CatalogGatewayMapper catalogGatewayMapper;
    private final CustomerGatewayMapper customerGatewayMapper;
    private final OrderStateMachine stateMachine;
    private final OrderTravelerService travelerService;
    private final OrderStatusLogService statusLogService;
    private final PaymentRecordService paymentRecordService;
    private final RefundRecordService refundRecordService;
    private final SensitiveCipher cipher;

    public OrderServiceImpl(OrdersMapper ordersMapper,
                            PaymentRecordMapper paymentRecordMapper,
                            CatalogGatewayMapper catalogGatewayMapper,
                            CustomerGatewayMapper customerGatewayMapper,
                            OrderStateMachine stateMachine,
                            OrderTravelerService travelerService,
                            OrderStatusLogService statusLogService,
                            PaymentRecordService paymentRecordService,
                            RefundRecordService refundRecordService,
                            SensitiveCipher cipher) {
        this.ordersMapper = ordersMapper;
        this.paymentRecordMapper = paymentRecordMapper;
        this.catalogGatewayMapper = catalogGatewayMapper;
        this.customerGatewayMapper = customerGatewayMapper;
        this.stateMachine = stateMachine;
        this.travelerService = travelerService;
        this.statusLogService = statusLogService;
        this.paymentRecordService = paymentRecordService;
        this.refundRecordService = refundRecordService;
        this.cipher = cipher;
    }

    @Override
    public PageResult<OrderVO> page(OrderQuery query) {
        LambdaQueryWrapper<Orders> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(query.getOrderNo())) {
            wrapper.like(Orders::getOrderNo, query.getOrderNo().trim());
        }
        if (query.getCustomerId() != null) {
            wrapper.eq(Orders::getCustomerId, query.getCustomerId());
        }
        if (query.getRouteId() != null) {
            wrapper.eq(Orders::getRouteId, query.getRouteId());
        }
        if (query.getDepartureId() != null) {
            wrapper.eq(Orders::getDepartureId, query.getDepartureId());
        }
        if (query.getStatus() != null) {
            wrapper.eq(Orders::getStatus, query.getStatus());
        }
        if (query.getPayStatus() != null) {
            wrapper.eq(Orders::getPayStatus, query.getPayStatus());
        }
        if (query.getSource() != null) {
            wrapper.eq(Orders::getSource, query.getSource());
        }
        if (StringUtils.hasText(query.getContactName())) {
            wrapper.like(Orders::getContactName, query.getContactName().trim());
        }
        if (StringUtils.hasText(query.getContactPhone())) {
            // 手机号是 AES 密文，等值查询必须走盲索引
            wrapper.eq(Orders::getContactPhoneIndex, cipher.blindIndex(query.getContactPhone().trim()));
        }
        if (query.getDepartDateBegin() != null) {
            wrapper.ge(Orders::getDepartDate, query.getDepartDateBegin());
        }
        if (query.getDepartDateEnd() != null) {
            wrapper.le(Orders::getDepartDate, query.getDepartDateEnd());
        }
        LocalDateTime begin = parseDateTime(query.getCreateTimeBegin());
        if (begin != null) {
            wrapper.ge(Orders::getCreateTime, begin);
        }
        LocalDateTime end = parseDateTime(query.getCreateTimeEnd());
        if (end != null) {
            wrapper.le(Orders::getCreateTime, end);
        }
        wrapper.orderByDesc(Orders::getId);

        Page<Orders> page = ordersMapper.selectPage(query.toPage(), wrapper);
        Map<Long, CustomerSnapshot> customers = loadCustomers(page.getRecords());
        List<OrderVO> records = page.getRecords().stream()
                .map(order -> toVO(order, customers.get(order.getCustomerId())))
                .collect(Collectors.toList());
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    @Override
    public OrderVO detail(Long id) {
        Orders order = requireOrder(id);
        CustomerSnapshot customer = customerGatewayMapper.selectCustomer(order.getCustomerId());
        OrderVO vo = toVO(order, customer);
        vo.setTravelers(travelerService.listByOrder(id));
        vo.setStatusLogs(statusLogService.listByOrder(id));
        vo.setPaymentRecords(paymentRecordService.listByOrder(id));
        vo.setRefundRecords(refundRecordService.listByOrder(id));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(OrderCreateRequest request) {
        CustomerSnapshot customer = customerGatewayMapper.selectCustomer(request.getCustomerId());
        if (customer == null) {
            throw new TradeException(ErrorCode.NOT_FOUND, "客户不存在");
        }
        if (customer.getStatus() != null && customer.getStatus() == 0) {
            throw new TradeException(ErrorCode.CONFLICT, "客户已被禁用，无法下单");
        }

        RouteSnapshot route = catalogGatewayMapper.selectRoute(request.getRouteId());
        if (route == null) {
            throw new TradeException(ErrorCode.NOT_FOUND, "线路不存在");
        }
        if (route.getStatus() == null || route.getStatus() != 1) {
            throw new TradeException(ErrorCode.CONFLICT, "线路已下架，不能下单");
        }

        int count = request.getTravelerCount();
        LocalDate departDate = request.getDepartDate();
        BigDecimal unitPrice = request.getUnitPrice();

        if (request.getDepartureId() != null) {
            DepartureSnapshot departure = catalogGatewayMapper.selectDeparture(request.getDepartureId());
            if (departure == null || !request.getRouteId().equals(departure.getRouteId())) {
                throw new TradeException(ErrorCode.NOT_FOUND, "班期不存在或不属于该线路");
            }
            if (departure.getStatus() == null || departure.getStatus() != 1) {
                throw new TradeException(ErrorCode.CONFLICT, "该班期已停售，请选择其他日期");
            }
            if (departure.getRemaining() < count) {
                throw new TradeException(ErrorCode.CONFLICT,
                        "该班期余位不足，剩余 " + departure.getRemaining() + " 个");
            }
            // 原子占位：并发下单时只有一条能扣成功
            if (catalogGatewayMapper.occupyStock(departure.getId(), count) == 0) {
                throw new TradeException(ErrorCode.CONFLICT, "该班期余位刚被抢完，请刷新后重试");
            }
            departDate = departure.getDepartDate();
            if (unitPrice == null) {
                unitPrice = departure.getAdultPrice();
            }
        } else if (departDate != null && departDate.isBefore(LocalDate.now())) {
            throw new TradeException(ErrorCode.BAD_REQUEST, "出发日期不能早于今天");
        }

        if (unitPrice == null) {
            unitPrice = route.getMinPrice();
        }
        if (unitPrice == null) {
            throw new TradeException(ErrorCode.BAD_REQUEST, "线路未设置价格，请先在班期里配置成人价");
        }

        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(count))
                .setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal discount = request.getDiscountAmount() == null
                ? BigDecimal.ZERO.setScale(2) : request.getDiscountAmount();
        if (discount.compareTo(totalAmount) > 0) {
            throw new TradeException(ErrorCode.BAD_REQUEST, "优惠金额不能大于订单总额");
        }
        BigDecimal payAmount = totalAmount.subtract(discount);

        Orders order = new Orders();
        order.setOrderNo(generateOrderNo());
        order.setCustomerId(request.getCustomerId());
        order.setRouteId(request.getRouteId());
        order.setDepartureId(request.getDepartureId());
        order.setRouteTitle(route.getTitle());
        order.setDepartDate(departDate);
        order.setTravelerCount(count);
        order.setUnitPrice(unitPrice);
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(discount);
        order.setPayAmount(payAmount);
        order.setContactName(request.getContactName().trim());
        order.setContactPhone(cipher.encrypt(request.getContactPhone().trim()));
        order.setContactPhoneIndex(cipher.blindIndex(request.getContactPhone().trim()));
        order.setStatus(OrderStatus.UNPAID.getCode());
        order.setPayStatus((byte) 0);
        order.setSource(request.getSource() == null ? (byte) 1 : request.getSource());
        order.setRemark(request.getRemark());
        order.setVersion(0);
        order.setCreateBy(CurrentOperator.id());
        ordersMapper.insert(order);

        stateMachine.writeLog(order.getId(), null, OrderStatus.UNPAID, "后台代录下单");

        List<OrderTravelerSaveRequest> travelers = request.getTravelers();
        if (!CollectionUtils.isEmpty(travelers)) {
            travelerService.replaceAll(order.getId(), travelers);
        }
        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, OrderUpdateRequest request) {
        Orders order = requireOrder(id);
        OrderStatus status = OrderStatus.of(order.getStatus());
        if (status.isFinal() || status == OrderStatus.REFUNDING) {
            throw new TradeException(ErrorCode.CONFLICT,
                    "订单处于「" + status.getDesc() + "」，不能再修改联系信息");
        }
        Orders update = new Orders();
        update.setId(id);
        update.setContactName(request.getContactName().trim());
        update.setContactPhone(cipher.encrypt(request.getContactPhone().trim()));
        update.setContactPhoneIndex(cipher.blindIndex(request.getContactPhone().trim()));
        update.setRemark(request.getRemark());
        update.setVersion(order.getVersion());
        update.setUpdateBy(CurrentOperator.id());
        if (ordersMapper.updateById(update) == 0) {
            throw new TradeException(ErrorCode.CONFLICT, "订单已被其他人修改，请刷新后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id, OrderCancelRequest request) {
        Orders order = requireOrder(id);
        LocalDateTime now = LocalDateTime.now();
        stateMachine.transition(order, OrderStatus.CANCELED, "取消订单：" + request.getReason().trim(),
                update -> {
                    update.setCancelTime(now);
                    update.setCancelReason(request.getReason().trim());
                });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pay(Long id, OrderPayRequest request) {
        Orders order = requireOrder(id);
        LocalDateTime now = LocalDateTime.now();
        byte payType = request.getPayType() == null ? (byte) 1 : request.getPayType();

        stateMachine.transition(order, OrderStatus.PAID, "订单支付成功", update -> {
            update.setPayStatus((byte) 1);
            update.setPayTime(now);
        });
        // 收到钱的统计累加只在这一次做，退款驳回回到「已支付」时不会重复计数
        stateMachine.onPaid(order);

        PaymentRecord record = new PaymentRecord();
        record.setOrderId(order.getId());
        record.setPayNo(generatePayNo());
        record.setPayType(payType);
        record.setAmount(order.getPayAmount());
        record.setStatus((byte) 1);
        record.setPayTime(now);
        record.setTransactionId(StringUtils.hasText(request.getTransactionId())
                ? request.getTransactionId().trim() : null);
        paymentRecordMapper.insert(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startTravel(Long id) {
        Orders order = requireOrder(id);
        stateMachine.transition(order, OrderStatus.TRAVELING, "行程开始", null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void complete(Long id) {
        Orders order = requireOrder(id);
        stateMachine.transition(order, OrderStatus.COMPLETED, "行程结束，订单完成", null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Orders order = requireOrder(id);
        OrderStatus status = OrderStatus.of(order.getStatus());
        if (!status.isFinal()) {
            throw new TradeException(ErrorCode.CONFLICT,
                    "订单处于「" + status.getDesc() + "」，请先取消或完成后再删除");
        }
        Orders update = new Orders();
        update.setId(id);
        update.setVersion(order.getVersion());
        update.setUpdateBy(CurrentOperator.id());
        update.setDeleted((byte) 1);
        if (ordersMapper.updateById(update) == 0) {
            throw new TradeException(ErrorCode.CONFLICT, "订单已被其他人修改，请刷新后重试");
        }
    }

    private Orders requireOrder(Long id) {
        Orders order = ordersMapper.selectById(id);
        if (order == null) {
            throw new TradeException(ErrorCode.NOT_FOUND, "订单不存在");
        }
        return order;
    }

    /**
     * 批量补客户信息，避免列表接口 N+1。
     */
    private Map<Long, CustomerSnapshot> loadCustomers(List<Orders> orders) {
        if (CollectionUtils.isEmpty(orders)) {
            return Collections.emptyMap();
        }
        Collection<Long> ids = orders.stream()
                .map(Orders::getCustomerId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<CustomerSnapshot> customers = customerGatewayMapper.selectCustomers(ids);
        if (CollectionUtils.isEmpty(customers)) {
            return Collections.emptyMap();
        }
        return customers.stream().collect(Collectors.toMap(CustomerSnapshot::getId, Function.identity(),
                (a, b) -> a));
    }

    private OrderVO toVO(Orders order, CustomerSnapshot customer) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setCustomerId(order.getCustomerId());
        if (customer != null) {
            vo.setCustomerNickname(customer.getNickname());
            vo.setCustomerPhone(cipher.maskPhone(customer.getPhone()));
        }
        vo.setRouteId(order.getRouteId());
        vo.setDepartureId(order.getDepartureId());
        vo.setRouteTitle(order.getRouteTitle());
        vo.setDepartDate(order.getDepartDate());
        vo.setTravelerCount(order.getTravelerCount());
        vo.setUnitPrice(order.getUnitPrice());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setDiscountAmount(order.getDiscountAmount());
        vo.setPayAmount(order.getPayAmount());
        vo.setContactName(order.getContactName());
        vo.setContactPhone(cipher.maskPhone(order.getContactPhone()));
        vo.setStatus(order.getStatus());
        vo.setStatusDesc(OrderStatus.describe(order.getStatus()));
        vo.setPayStatus(order.getPayStatus());
        vo.setPayStatusDesc(TradeDict.payStatusDesc(order.getPayStatus()));
        vo.setPayTime(order.getPayTime());
        vo.setCancelTime(order.getCancelTime());
        vo.setCancelReason(order.getCancelReason());
        vo.setRemark(order.getRemark());
        vo.setSource(order.getSource());
        vo.setSourceDesc(TradeDict.sourceDesc(order.getSource()));
        vo.setVersion(order.getVersion());
        vo.setCreateTime(order.getCreateTime());
        vo.setUpdateTime(order.getUpdateTime());
        return vo;
    }

    /**
     * 订单号：RG + 秒级时间 + 4 位随机。撞号时重试，重试仍冲突交给数据库唯一索引兜底。
     */
    private String generateOrderNo() {
        for (int i = 0; i < ORDER_NO_RETRY; i++) {
            String candidate = ORDER_NO_PREFIX + LocalDateTime.now().format(NO_FORMATTER)
                    + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
            if (ordersMapper.countByOrderNoIncludingDeleted(candidate) == 0) {
                return candidate;
            }
        }
        throw new TradeException(ErrorCode.INTERNAL_ERROR, "订单号生成冲突，请稍后重试");
    }

    private String generatePayNo() {
        return PAY_NO_PREFIX + LocalDateTime.now().format(NO_FORMATTER)
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    /**
     * 兼容 {@code yyyy-MM-dd HH:mm:ss} 与 {@code yyyy-MM-dd} 两种写法；空值与格式错误按未传处理。
     */
    private LocalDateTime parseDateTime(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String value = text.trim();
        try {
            if (value.length() == 10) {
                return LocalDate.parse(value, DATE_PARSER).atStartOfDay();
            }
            return LocalDateTime.parse(value, DATETIME_PARSER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
