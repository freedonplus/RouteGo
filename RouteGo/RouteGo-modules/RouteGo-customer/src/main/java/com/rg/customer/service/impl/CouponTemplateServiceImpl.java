package com.rg.customer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rg.common.exception.ErrorCode;
import com.rg.common.exception.serviceException.CustomerException;
import com.rg.common.web.PageResult;
import com.rg.customer.dto.CouponGrantRequest;
import com.rg.customer.dto.CouponTemplateQuery;
import com.rg.customer.dto.CouponTemplateSaveRequest;
import com.rg.customer.entity.Coupon;
import com.rg.customer.entity.CouponTemplate;
import com.rg.customer.entity.Customer;
import com.rg.customer.enums.CustomerDict;
import com.rg.customer.mapper.CouponMapper;
import com.rg.customer.mapper.CouponTemplateMapper;
import com.rg.customer.mapper.CustomerMapper;
import com.rg.customer.service.CouponTemplateService;
import com.rg.customer.support.CouponSupport;
import com.rg.customer.support.CurrentOperator;
import com.rg.customer.vo.CouponTemplateVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 优惠券模板业务实现。
 * <p>
 * 发券是这里唯一的写热点：先原子占用发行量（条件 UPDATE），再落券记录，
 * 两者在同一事务里，任何一步失败都会把占用回滚掉。
 */
@Service
public class CouponTemplateServiceImpl extends ServiceImpl<CouponTemplateMapper, CouponTemplate>
        implements CouponTemplateService {

    /** 券码字符集：去掉 0/O/1/I 等易混字符，方便客服口述。 */
    private static final char[] CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int CODE_RANDOM_LEN = 6;
    private static final DateTimeFormatter CODE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final CouponTemplateMapper templateMapper;
    private final CouponMapper couponMapper;
    private final CustomerMapper customerMapper;
    private final SecureRandom random = new SecureRandom();

    public CouponTemplateServiceImpl(CouponTemplateMapper templateMapper,
                                     CouponMapper couponMapper,
                                     CustomerMapper customerMapper) {
        this.templateMapper = templateMapper;
        this.couponMapper = couponMapper;
        this.customerMapper = customerMapper;
    }

    @Override
    public PageResult<CouponTemplateVO> page(CouponTemplateQuery query) {
        LambdaQueryWrapper<CouponTemplate> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(query.getName())) {
            wrapper.like(CouponTemplate::getName, query.getName().trim());
        }
        if (query.getType() != null) {
            wrapper.eq(CouponTemplate::getType, query.getType());
        }
        if (query.getStatus() != null) {
            wrapper.eq(CouponTemplate::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(CouponTemplate::getId);
        Page<CouponTemplate> page = templateMapper.selectPage(query.toPage(), wrapper);
        List<CouponTemplateVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    @Override
    public CouponTemplateVO detail(Long id) {
        return toVO(requireTemplate(id));
    }

    @Override
    public List<CouponTemplateVO> listEnabled() {
        return templateMapper.selectList(Wrappers.<CouponTemplate>lambdaQuery()
                        .eq(CouponTemplate::getStatus, (byte) 1)
                        .orderByDesc(CouponTemplate::getId)).stream()
                .filter(t -> !CouponSupport.templateExpired(t))
                .map(this::toVO)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(CouponTemplateSaveRequest request) {
        validate(request, 0);
        CouponTemplate template = new CouponTemplate();
        applyRequest(template, request);
        template.setReceivedCount(0);
        template.setVersion(0);
        template.setCreateBy(CurrentOperator.id());
        templateMapper.insert(template);
        return template.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, CouponTemplateSaveRequest request) {
        CouponTemplate existing = requireTemplate(id);
        validate(request, existing.getReceivedCount() == null ? 0 : existing.getReceivedCount());
        boolean amountType = Objects.equals(request.getType(), (byte) 1);
        boolean fixedValid = Objects.equals(request.getValidType(), (byte) 1);

        // 这里必须用 UpdateWrapper 显式 set：满减券与折扣券的互斥字段需要被置回 NULL，
        // 而 updateById 默认跳过 null 字段，直接传实体会导致类型切换后残留旧值。
        LambdaUpdateWrapper<CouponTemplate> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(CouponTemplate::getId, id)
                .set(CouponTemplate::getName, request.getName().trim())
                .set(CouponTemplate::getType, request.getType())
                .set(CouponTemplate::getThreshold,
                        request.getThreshold() == null ? BigDecimal.ZERO : request.getThreshold())
                .set(CouponTemplate::getTotalCount, request.getTotalCount())
                .set(CouponTemplate::getValidType, request.getValidType())
                .set(CouponTemplate::getStatus,
                        request.getStatus() == null ? existing.getStatus() : request.getStatus())
                .set(CouponTemplate::getAmount, amountType ? request.getAmount() : null)
                .set(CouponTemplate::getDiscount, amountType ? null : request.getDiscount())
                .set(CouponTemplate::getStartTime, fixedValid ? request.getStartTime() : null)
                .set(CouponTemplate::getEndTime, fixedValid ? request.getEndTime() : null)
                .set(CouponTemplate::getValidDays, fixedValid ? null : request.getValidDays())
                .set(CouponTemplate::getUpdateBy, CurrentOperator.id());
        templateMapper.update(null, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Byte status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new CustomerException(ErrorCode.BAD_REQUEST, "状态只能是 0（停用）或 1（启用）");
        }
        CouponTemplate existing = requireTemplate(id);
        if (status == 1 && CouponSupport.templateExpired(existing)) {
            throw new CustomerException(ErrorCode.CONFLICT, "该券已过有效期，请先调整有效期再启用");
        }
        CouponTemplate update = new CouponTemplate();
        update.setId(id);
        update.setStatus(status);
        update.setUpdateBy(CurrentOperator.id());
        templateMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireTemplate(id);
        long issued = couponMapper.countByTemplate(id);
        if (issued > 0) {
            throw new CustomerException(ErrorCode.CONFLICT,
                    "该券已发放 " + issued + " 张，无法删除；如需停止发放请改为「停用」");
        }
        templateMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int grant(CouponGrantRequest request) {
        CouponTemplate template = requireTemplate(request.getTemplateId());
        if (template.getStatus() == null || template.getStatus() != 1) {
            throw new CustomerException(ErrorCode.CONFLICT, "该券已停用，不能发放");
        }
        if (CouponSupport.templateExpired(template)) {
            throw new CustomerException(ErrorCode.CONFLICT, "该券已过有效期，不能发放");
        }

        // 去重后再校验会员，避免同一个人被重复发券（前端多选也可能带上重复 ID）
        List<Long> customerIds = new ArrayList<>(new LinkedHashSet<>(request.getCustomerIds()));
        List<Customer> customers = customerMapper.selectBatchIds(customerIds);
        Map<Long, Customer> customerMap = customers.stream()
                .collect(Collectors.toMap(Customer::getId, Function.identity(), (a, b) -> a));
        for (Long customerId : customerIds) {
            Customer customer = customerMap.get(customerId);
            if (customer == null) {
                throw new CustomerException(ErrorCode.NOT_FOUND, "会员 " + customerId + " 不存在或已删除");
            }
            if (customer.getStatus() == null || customer.getStatus() != 1) {
                throw new CustomerException(ErrorCode.CONFLICT,
                        "会员「" + Objects.toString(customer.getNickname(), customerId.toString()) + "」已被禁用，不能发券");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        int granted = 0;
        for (Long customerId : customerIds) {
            if (templateMapper.increaseReceived(template.getId()) == 0) {
                throw new CustomerException(ErrorCode.CONFLICT,
                        "「" + template.getName() + "」已发放完毕，本次发放在第 " + (granted + 1) + " 位会员处中止，已回滚");
            }
            Coupon coupon = new Coupon();
            coupon.setTemplateId(template.getId());
            coupon.setCustomerId(customerId);
            coupon.setCode(nextCode());
            coupon.setStatus((byte) 0);
            coupon.setReceiveTime(now);
            coupon.setVersion(0);
            couponMapper.insert(coupon);
            granted++;
        }
        return granted;
    }

    /**
     * 生成券码：CP + 日期 + 6 位随机，撞码时重试（唯一索引不含 deleted，必须连回收站一起查）。
     */
    private String nextCode() {
        String prefix = "CP" + LocalDate.now().format(CODE_DATE);
        for (int attempt = 0; attempt < 5; attempt++) {
            StringBuilder sb = new StringBuilder(prefix);
            for (int i = 0; i < CODE_RANDOM_LEN; i++) {
                sb.append(CODE_CHARS[random.nextInt(CODE_CHARS.length)]);
            }
            String code = sb.toString();
            if (couponMapper.countByCodeIncludingDeleted(code) == 0) {
                return code;
            }
        }
        throw new CustomerException(ErrorCode.INTERNAL_ERROR, "券码生成失败，请重试");
    }

    /**
     * 类型、金额、有效期三段校验。
     *
     * @param receivedCount 已发放数量，编辑时不允许把发行总量改到比它还小
     */
    private void validate(CouponTemplateSaveRequest request, int receivedCount) {
        if (Objects.equals(request.getType(), (byte) 1)) {
            if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CustomerException(ErrorCode.BAD_REQUEST, "满减券必须填写大于 0 的减免金额");
            }
        } else if (Objects.equals(request.getType(), (byte) 2)) {
            if (request.getDiscount() == null
                    || request.getDiscount().compareTo(BigDecimal.ZERO) <= 0
                    || request.getDiscount().compareTo(BigDecimal.ONE) >= 0) {
                throw new CustomerException(ErrorCode.BAD_REQUEST, "折扣券的折扣率需在 0 ~ 1 之间，例如 0.90 表示九折");
            }
        }
        if (request.getTotalCount() != null && request.getTotalCount() > 0
                && request.getTotalCount() < receivedCount) {
            throw new CustomerException(ErrorCode.BAD_REQUEST,
                    "发行总量不能小于已发放数量（当前已发 " + receivedCount + " 张）");
        }
        if (Objects.equals(request.getValidType(), (byte) 1)) {
            if (request.getStartTime() == null || request.getEndTime() == null) {
                throw new CustomerException(ErrorCode.BAD_REQUEST, "固定区间有效期必须填写开始与结束时间");
            }
            if (!request.getEndTime().isAfter(request.getStartTime())) {
                throw new CustomerException(ErrorCode.BAD_REQUEST, "有效期结束时间必须晚于开始时间");
            }
        } else if (Objects.equals(request.getValidType(), (byte) 2)) {
            if (request.getValidDays() == null || request.getValidDays() < 1) {
                throw new CustomerException(ErrorCode.BAD_REQUEST, "领取后有效期至少 1 天");
            }
        }
    }

    /**
     * 按类型清理互斥字段：满减券不存折扣率，折扣券不存减免金额，避免脏数据干扰判断。
     */
    private void applyRequest(CouponTemplate template, CouponTemplateSaveRequest request) {
        template.setName(request.getName().trim());
        template.setType(request.getType());
        template.setThreshold(request.getThreshold() == null ? BigDecimal.ZERO : request.getThreshold());
        template.setTotalCount(request.getTotalCount());
        template.setValidType(request.getValidType());
        template.setStatus(request.getStatus() == null ? (byte) 1 : request.getStatus());
        if (Objects.equals(request.getType(), (byte) 1)) {
            template.setAmount(request.getAmount());
            template.setDiscount(null);
        } else {
            template.setDiscount(request.getDiscount());
            template.setAmount(null);
        }
        if (Objects.equals(request.getValidType(), (byte) 1)) {
            template.setStartTime(request.getStartTime());
            template.setEndTime(request.getEndTime());
            template.setValidDays(null);
        } else {
            template.setValidDays(request.getValidDays());
            template.setStartTime(null);
            template.setEndTime(null);
        }
    }

    private CouponTemplate requireTemplate(Long id) {
        CouponTemplate template = templateMapper.selectById(id);
        if (template == null) {
            throw new CustomerException(ErrorCode.NOT_FOUND, "优惠券模板不存在");
        }
        return template;
    }

    private CouponTemplateVO toVO(CouponTemplate template) {
        CouponTemplateVO vo = new CouponTemplateVO();
        vo.setId(template.getId());
        vo.setName(template.getName());
        vo.setType(template.getType());
        vo.setTypeDesc(CustomerDict.couponTypeDesc(template.getType()));
        vo.setThreshold(template.getThreshold());
        vo.setAmount(template.getAmount());
        vo.setDiscount(template.getDiscount());
        vo.setBenefitDesc(CouponSupport.benefitDesc(template));
        vo.setTotalCount(template.getTotalCount());
        vo.setReceivedCount(template.getReceivedCount());
        if (template.getTotalCount() != null && template.getTotalCount() > 0) {
            int received = template.getReceivedCount() == null ? 0 : template.getReceivedCount();
            vo.setRemainCount(Math.max(template.getTotalCount() - received, 0));
        }
        vo.setValidType(template.getValidType());
        vo.setValidTypeDesc(CustomerDict.validTypeDesc(template.getValidType()));
        vo.setStartTime(template.getStartTime());
        vo.setEndTime(template.getEndTime());
        vo.setValidDays(template.getValidDays());
        vo.setValidDesc(CouponSupport.validDesc(template));
        vo.setStatus(template.getStatus());
        vo.setStatusDesc(CustomerDict.customerStatusDesc(template.getStatus()));
        vo.setExpired(CouponSupport.templateExpired(template));
        vo.setCreateTime(template.getCreateTime());
        vo.setUpdateTime(template.getUpdateTime());
        return vo;
    }
}
