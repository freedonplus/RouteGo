package com.rg.customer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rg.common.crypto.SensitiveCipher;
import com.rg.common.exception.ErrorCode;
import com.rg.common.exception.serviceException.CustomerException;
import com.rg.common.web.PageResult;
import com.rg.customer.dto.CouponQuery;
import com.rg.customer.dto.CouponUseRequest;
import com.rg.customer.entity.Coupon;
import com.rg.customer.entity.CouponTemplate;
import com.rg.customer.entity.Customer;
import com.rg.customer.enums.CustomerDict;
import com.rg.customer.mapper.CouponMapper;
import com.rg.customer.mapper.CouponTemplateMapper;
import com.rg.customer.mapper.CustomerMapper;
import com.rg.customer.mapper.OrderGatewayMapper;
import com.rg.customer.mapper.model.OrderSnapshot;
import com.rg.customer.service.CouponService;
import com.rg.customer.support.CouponSupport;
import com.rg.customer.vo.CouponVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 用户优惠券业务实现。
 * <p>
 * 券的过期时间不在表里，按模板规则实时推导；核销用条件 UPDATE 保证并发下只成功一次。
 * 核销只建立「券 ↔ 订单」的关联，不修改订单金额——金额计算属于交易域，
 * 跨域改金额会让两个域的对账口径不一致。
 */
@Service
public class CouponServiceImpl extends ServiceImpl<CouponMapper, Coupon> implements CouponService {

    private final CouponMapper couponMapper;
    private final CouponTemplateMapper templateMapper;
    private final CustomerMapper customerMapper;
    private final OrderGatewayMapper orderGatewayMapper;
    private final SensitiveCipher cipher;

    public CouponServiceImpl(CouponMapper couponMapper,
                             CouponTemplateMapper templateMapper,
                             CustomerMapper customerMapper,
                             OrderGatewayMapper orderGatewayMapper,
                             SensitiveCipher cipher) {
        this.couponMapper = couponMapper;
        this.templateMapper = templateMapper;
        this.customerMapper = customerMapper;
        this.orderGatewayMapper = orderGatewayMapper;
        this.cipher = cipher;
    }

    @Override
    public PageResult<CouponVO> page(CouponQuery query) {
        LambdaQueryWrapper<Coupon> wrapper = Wrappers.lambdaQuery();
        if (query.getCustomerId() != null) {
            wrapper.eq(Coupon::getCustomerId, query.getCustomerId());
        }
        if (query.getTemplateId() != null) {
            wrapper.eq(Coupon::getTemplateId, query.getTemplateId());
        }
        if (StringUtils.hasText(query.getCode())) {
            wrapper.eq(Coupon::getCode, query.getCode().trim());
        }
        if (query.getStatus() != null) {
            wrapper.eq(Coupon::getStatus, query.getStatus());
        }
        if (query.getReceiveTimeStart() != null) {
            wrapper.ge(Coupon::getReceiveTime, query.getReceiveTimeStart());
        }
        if (query.getReceiveTimeEnd() != null) {
            wrapper.le(Coupon::getReceiveTime, query.getReceiveTimeEnd());
        }
        wrapper.orderByDesc(Coupon::getReceiveTime).orderByDesc(Coupon::getId);
        Page<Coupon> page = couponMapper.selectPage(query.toPage(), wrapper);
        List<CouponVO> records = assemble(page.getRecords());
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    @Override
    public CouponVO detail(Long id) {
        return assemble(Collections.singletonList(requireCoupon(id))).get(0);
    }

    @Override
    public List<CouponVO> listByCustomer(Long customerId, Byte status) {
        LambdaQueryWrapper<Coupon> wrapper = Wrappers.<Coupon>lambdaQuery()
                .eq(Coupon::getCustomerId, customerId);
        if (status != null) {
            wrapper.eq(Coupon::getStatus, status);
        }
        wrapper.orderByAsc(Coupon::getStatus).orderByDesc(Coupon::getReceiveTime);
        return assemble(couponMapper.selectList(wrapper));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void use(Long id, CouponUseRequest request) {
        Coupon coupon = requireCoupon(id);
        if (coupon.getStatus() != null && coupon.getStatus() == 1) {
            throw new CustomerException(ErrorCode.CONFLICT, "该券已使用，不能重复核销");
        }
        if (coupon.getStatus() != null && coupon.getStatus() == 2) {
            throw new CustomerException(ErrorCode.CONFLICT, "该券已过期，不能核销");
        }
        CouponTemplate template = templateMapper.selectById(coupon.getTemplateId());
        if (template == null) {
            throw new CustomerException(ErrorCode.CONFLICT, "券模板已删除，无法核销");
        }
        if (CouponSupport.expired(template, coupon.getReceiveTime(), coupon.getStatus())) {
            // 顺手把状态修正成已过期，避免同一张券反复走到这里
            couponMapper.markExpired(id);
            throw new CustomerException(ErrorCode.CONFLICT, "该券已超过有效期，已自动置为「已过期」");
        }

        Long orderId = request == null ? null : request.getOrderId();
        if (orderId != null) {
            validateOrder(coupon, template, orderId);
        }

        if (couponMapper.markUsed(id, orderId, LocalDateTime.now()) == 0) {
            // 并发核销：另一笔请求已经把这行从「未使用」改走了
            throw new CustomerException(ErrorCode.CONFLICT, "该券已被其他操作使用，请刷新后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revoke(Long id) {
        Coupon coupon = requireCoupon(id);
        if (coupon.getStatus() != null && coupon.getStatus() == 1) {
            throw new CustomerException(ErrorCode.CONFLICT, "已使用的券不能作废，如需退还请走订单退款流程");
        }
        if (coupon.getStatus() != null && coupon.getStatus() == 2) {
            throw new CustomerException(ErrorCode.CONFLICT, "该券已过期，无需重复作废");
        }
        if (couponMapper.markExpired(id) == 0) {
            throw new CustomerException(ErrorCode.CONFLICT, "券状态已变化，请刷新后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Coupon coupon = requireCoupon(id);
        if (coupon.getStatus() != null && coupon.getStatus() == 1) {
            throw new CustomerException(ErrorCode.CONFLICT, "已使用的券是核销凭证，不允许删除");
        }
        couponMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int refreshExpired() {
        return couponMapper.expireOutdated();
    }

    /**
     * 核销时的订单校验：归属、状态、使用门槛。
     */
    private void validateOrder(Coupon coupon, CouponTemplate template, Long orderId) {
        OrderSnapshot order = orderGatewayMapper.selectOrder(orderId);
        if (order == null) {
            throw new CustomerException(ErrorCode.NOT_FOUND, "订单不存在或已删除");
        }
        if (!Objects.equals(order.getCustomerId(), coupon.getCustomerId())) {
            throw new CustomerException(ErrorCode.CONFLICT, "该券属于其他会员，不能用于此订单");
        }
        if (order.getStatus() != null && (order.getStatus() == 4 || order.getStatus() == 6)) {
            throw new CustomerException(ErrorCode.CONFLICT, "订单已取消或已退款，不能核销优惠券");
        }
        if (template.getThreshold() != null
                && template.getThreshold().signum() > 0
                && order.getPayAmount() != null
                && order.getPayAmount().compareTo(template.getThreshold()) < 0) {
            throw new CustomerException(ErrorCode.CONFLICT,
                    "订单金额 " + order.getPayAmount() + " 未达到使用门槛 " + template.getThreshold());
        }
    }

    private Coupon requireCoupon(Long id) {
        Coupon coupon = couponMapper.selectById(id);
        if (coupon == null) {
            throw new CustomerException(ErrorCode.NOT_FOUND, "优惠券不存在");
        }
        return coupon;
    }

    /**
     * 批量补全模板、会员与订单号，避免列表逐条查询。
     */
    private List<CouponVO> assemble(List<Coupon> coupons) {
        if (coupons == null || coupons.isEmpty()) {
            return List.of();
        }
        Map<Long, CouponTemplate> templates = batchTemplates(coupons.stream()
                .map(Coupon::getTemplateId).filter(Objects::nonNull).collect(Collectors.toSet()));
        Map<Long, Customer> customers = batchCustomers(coupons.stream()
                .map(Coupon::getCustomerId).filter(Objects::nonNull).collect(Collectors.toSet()));
        Map<Long, String> orderNos = batchOrderNos(coupons.stream()
                .map(Coupon::getOrderId).filter(Objects::nonNull).collect(Collectors.toSet()));

        return coupons.stream().map(coupon -> {
            CouponTemplate template = templates.get(coupon.getTemplateId());
            Customer customer = customers.get(coupon.getCustomerId());
            return toVO(coupon, template, customer,
                    coupon.getOrderId() == null ? null : orderNos.get(coupon.getOrderId()));
        }).toList();
    }

    private Map<Long, CouponTemplate> batchTemplates(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return templateMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(CouponTemplate::getId, Function.identity(), (a, b) -> a));
    }

    private Map<Long, Customer> batchCustomers(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return customerMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Customer::getId, Function.identity(), (a, b) -> a));
    }

    private Map<Long, String> batchOrderNos(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return orderGatewayMapper.selectOrders(ids).stream()
                .filter(o -> o.getOrderNo() != null)
                .collect(Collectors.toMap(OrderSnapshot::getId, OrderSnapshot::getOrderNo, (a, b) -> a));
    }

    private CouponVO toVO(Coupon coupon, CouponTemplate template, Customer customer, String orderNo) {
        CouponVO vo = new CouponVO();
        vo.setId(coupon.getId());
        vo.setCode(coupon.getCode());
        vo.setTemplateId(coupon.getTemplateId());
        vo.setCustomerId(coupon.getCustomerId());
        vo.setStatus(coupon.getStatus());
        vo.setStatusDesc(CustomerDict.couponStatusDesc(coupon.getStatus()));
        vo.setReceiveTime(coupon.getReceiveTime());
        vo.setUseTime(coupon.getUseTime());
        vo.setOrderId(coupon.getOrderId());
        vo.setOrderNo(orderNo);

        if (template != null) {
            vo.setTemplateName(template.getName());
            vo.setType(template.getType());
            vo.setTypeDesc(CustomerDict.couponTypeDesc(template.getType()));
            vo.setThreshold(template.getThreshold());
            vo.setAmount(template.getAmount());
            vo.setDiscount(template.getDiscount());
            vo.setBenefitDesc(CouponSupport.benefitDesc(template));
            vo.setExpireTime(CouponSupport.expireTime(template, coupon.getReceiveTime()));
        }
        boolean expired = CouponSupport.expired(template, coupon.getReceiveTime(), coupon.getStatus());
        vo.setExpired(expired);
        vo.setUsable(coupon.getStatus() != null && coupon.getStatus() == 0 && !expired);
        if (customer != null) {
            vo.setCustomerNickname(customer.getNickname());
            vo.setCustomerPhone(cipher.maskPhone(customer.getPhone()));
        }
        return vo;
    }
}
