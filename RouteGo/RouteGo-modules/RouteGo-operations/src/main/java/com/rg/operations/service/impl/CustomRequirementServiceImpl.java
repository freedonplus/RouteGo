package com.rg.operations.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rg.common.crypto.SensitiveCipher;
import com.rg.common.exception.ErrorCode;
import com.rg.common.exception.serviceException.OperationsException;
import com.rg.common.web.PageResult;
import com.rg.operations.dto.CustomRequirementCreateRequest;
import com.rg.operations.dto.CustomRequirementQuery;
import com.rg.operations.dto.CustomRequirementUpdateRequest;
import com.rg.operations.entity.CustomQuote;
import com.rg.operations.entity.CustomRequirement;
import com.rg.operations.enums.OperationsDict;
import com.rg.operations.mapper.CustomQuoteMapper;
import com.rg.operations.mapper.CustomRequirementMapper;
import com.rg.operations.mapper.OperationsGatewayMapper;
import com.rg.operations.mapper.model.CustomerSnapshot;
import com.rg.operations.mapper.model.UserSnapshot;
import com.rg.operations.service.CustomRequirementService;
import com.rg.operations.support.BizNoGenerator;
import com.rg.operations.support.CurrentOperator;
import com.rg.operations.vo.CustomRequirementVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 定制需求业务实现。
 * <p>
 * 联系电话入口加密、出口脱敏，与会员模块保持同一套做法（AES/GCM + 脱敏）。
 * 由于表上没有手机号盲索引列，后台不支持按号码精确检索，这一点在接口说明里已注明。
 * <p>
 * 状态推进规则：报价创建 → 已报价，采纳报价 → 已成交，这两步由报价服务驱动；
 * 本服务的人工状态接口只处理「线下成交」与「关闭」，避免状态与报价版本对不上。
 */
@Service
public class CustomRequirementServiceImpl extends ServiceImpl<CustomRequirementMapper, CustomRequirement>
        implements CustomRequirementService {

    private static final byte REQUIREMENT_PENDING = 0;
    private static final byte REQUIREMENT_QUOTED = 1;
    private static final byte REQUIREMENT_DEAL = 2;
    private static final byte REQUIREMENT_CLOSED = 3;

    private static final int NO_RETRY = 5;

    private final CustomRequirementMapper requirementMapper;
    private final CustomQuoteMapper quoteMapper;
    private final OperationsGatewayMapper gateway;
    private final SensitiveCipher cipher;

    public CustomRequirementServiceImpl(CustomRequirementMapper requirementMapper,
                                        CustomQuoteMapper quoteMapper,
                                        OperationsGatewayMapper gateway,
                                        SensitiveCipher cipher) {
        this.requirementMapper = requirementMapper;
        this.quoteMapper = quoteMapper;
        this.gateway = gateway;
        this.cipher = cipher;
    }

    @Override
    public PageResult<CustomRequirementVO> page(CustomRequirementQuery query) {
        LambdaQueryWrapper<CustomRequirement> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(query.getRequirementNo())) {
            wrapper.like(CustomRequirement::getRequirementNo, query.getRequirementNo().trim());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(CustomRequirement::getContactName, keyword)
                    .or().like(CustomRequirement::getDestination, keyword)
                    .or().like(CustomRequirement::getPreferences, keyword));
        }
        if (query.getCustomerId() != null) {
            wrapper.eq(CustomRequirement::getCustomerId, query.getCustomerId());
        }
        if (query.getStatus() != null) {
            wrapper.eq(CustomRequirement::getStatus, query.getStatus());
        }
        if (query.getHandlerId() != null) {
            wrapper.eq(CustomRequirement::getHandlerId, query.getHandlerId());
        }
        if (query.getDepartDateStart() != null) {
            wrapper.ge(CustomRequirement::getDepartDate, query.getDepartDateStart());
        }
        if (query.getDepartDateEnd() != null) {
            wrapper.le(CustomRequirement::getDepartDate, query.getDepartDateEnd());
        }
        if (query.getCreateTimeStart() != null) {
            wrapper.ge(CustomRequirement::getCreateTime, query.getCreateTimeStart());
        }
        if (query.getCreateTimeEnd() != null) {
            wrapper.le(CustomRequirement::getCreateTime, query.getCreateTimeEnd());
        }
        wrapper.orderByDesc(CustomRequirement::getCreateTime).orderByDesc(CustomRequirement::getId);
        Page<CustomRequirement> page = requirementMapper.selectPage(query.toPage(), wrapper);
        return new PageResult<>(toVOs(page.getRecords()), page.getTotal(), page.getCurrent(),
                page.getSize(), page.getPages());
    }

    @Override
    public CustomRequirementVO detail(Long id) {
        CustomRequirement requirement = requireRequirement(id);
        CustomRequirementVO vo = toVOs(List.of(requirement)).get(0);
        vo.setQuoteCount(quoteMapper.countByRequirementId(id));
        CustomQuote latest = quoteMapper.selectOne(Wrappers.<CustomQuote>lambdaQuery()
                .eq(CustomQuote::getRequirementId, id)
                .orderByDesc(CustomQuote::getId)
                .last("LIMIT 1"));
        if (latest != null) {
            vo.setLatestQuoteAmount(latest.getAmount());
            vo.setLatestQuoteStatus(latest.getStatus());
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(CustomRequirementCreateRequest request) {
        if (request.getCustomerId() != null && gateway.selectCustomer(request.getCustomerId()) == null) {
            throw new OperationsException(ErrorCode.NOT_FOUND, "关联会员不存在或已删除");
        }
        if (request.getHandlerId() != null) {
            requireHandler(request.getHandlerId());
        }

        CustomRequirement requirement = new CustomRequirement();
        requirement.setRequirementNo(nextRequirementNo());
        requirement.setCustomerId(request.getCustomerId());
        requirement.setContactName(request.getContactName().trim());
        requirement.setContactPhone(cipher.encrypt(request.getContactPhone().trim()));
        requirement.setDestination(request.getDestination().trim());
        requirement.setDepartDate(request.getDepartDate());
        requirement.setDays(request.getDays());
        requirement.setPeopleCount(request.getPeopleCount());
        requirement.setBudget(request.getBudget());
        requirement.setPreferences(request.getPreferences());
        requirement.setStatus(REQUIREMENT_PENDING);
        requirement.setHandlerId(request.getHandlerId());
        requirement.setRemark(request.getRemark());
        requirement.setVersion(0);
        requirement.setCreateBy(CurrentOperator.id());
        requirement.setCreateTime(LocalDateTime.now());
        requirement.setDeleted((byte) 0);
        requirementMapper.insert(requirement);
        return requirement.getId();
    }

    @Override
    public void update(Long id, CustomRequirementUpdateRequest request) {
        CustomRequirement requirement = requireRequirement(id);
        if (requirement.getStatus() == REQUIREMENT_CLOSED) {
            throw new OperationsException(ErrorCode.CONFLICT, "需求已关闭，不能再修改");
        }
        if (request.getCustomerId() != null && gateway.selectCustomer(request.getCustomerId()) == null) {
            throw new OperationsException(ErrorCode.NOT_FOUND, "关联会员不存在或已删除");
        }
        if (request.getHandlerId() != null) {
            requireHandler(request.getHandlerId());
        }

        CustomRequirement update = new CustomRequirement();
        update.setId(id);
        update.setCustomerId(request.getCustomerId());
        update.setContactName(request.getContactName() == null ? null : request.getContactName().trim());
        // 电话留空表示不改：联系电话是必填项，清空没有业务含义
        if (StringUtils.hasText(request.getContactPhone())) {
            update.setContactPhone(cipher.encrypt(request.getContactPhone().trim()));
        }
        update.setDestination(request.getDestination() == null ? null : request.getDestination().trim());
        update.setDepartDate(request.getDepartDate());
        update.setDays(request.getDays());
        update.setPeopleCount(request.getPeopleCount());
        update.setBudget(request.getBudget());
        update.setPreferences(request.getPreferences());
        update.setHandlerId(request.getHandlerId());
        update.setRemark(request.getRemark());
        update.setVersion(requirement.getVersion());
        update.setUpdateBy(CurrentOperator.id());
        update.setUpdateTime(LocalDateTime.now());
        if (requirementMapper.updateById(update) == 0) {
            throw new OperationsException(ErrorCode.CONFLICT, "需求已被其他人修改，请刷新后重试");
        }
    }

    @Override
    public void assign(Long id, Long handlerId) {
        CustomRequirement requirement = requireRequirement(id);
        if (requirement.getStatus() == REQUIREMENT_CLOSED) {
            throw new OperationsException(ErrorCode.CONFLICT, "需求已关闭，不能指派跟进人");
        }
        requireHandler(handlerId);
        if (Objects.equals(requirement.getHandlerId(), handlerId)) {
            throw new OperationsException(ErrorCode.CONFLICT, "需求已由该跟进人负责，无需重复指派");
        }
        int rows = requirementMapper.update(null, Wrappers.<CustomRequirement>lambdaUpdate()
                .eq(CustomRequirement::getId, id)
                .eq(CustomRequirement::getVersion, requirement.getVersion())
                .set(CustomRequirement::getHandlerId, handlerId)
                .set(CustomRequirement::getUpdateBy, CurrentOperator.id())
                .set(CustomRequirement::getVersion, requirement.getVersion() + 1));
        assertUpdated(rows);
    }

    @Override
    public void updateStatus(Long id, Byte status, String remark) {
        if (status == null) {
            throw new OperationsException("目标状态不能为空");
        }
        CustomRequirement requirement = requireRequirement(id);
        byte from = requirement.getStatus();

        if (from == status) {
            throw new OperationsException(ErrorCode.CONFLICT,
                    "需求已处于「" + statusText(status) + "」状态，无需重复操作");
        }
        if (from == REQUIREMENT_CLOSED) {
            throw new OperationsException(ErrorCode.CONFLICT, "需求已关闭，不能再变更状态");
        }
        if (status == REQUIREMENT_QUOTED) {
            throw new OperationsException(ErrorCode.CONFLICT, "「已报价」由报价单自动驱动，请通过新建报价推进");
        }
        if (status == REQUIREMENT_PENDING) {
            throw new OperationsException(ErrorCode.CONFLICT, "不能把需求退回「待跟进」");
        }
        if (status != REQUIREMENT_DEAL && status != REQUIREMENT_CLOSED) {
            throw new OperationsException("状态只能改为 2 已成交（线下成交）或 3 已关闭");
        }
        if (status == REQUIREMENT_DEAL && quoteMapper.countByRequirementId(id) > 0
                && from == REQUIREMENT_QUOTED) {
            // 有报价就必须走采纳流程，否则「已成交」的金额与报价版本对不上
            throw new OperationsException(ErrorCode.CONFLICT, "该需求已有报价，请通过采纳报价完成成交");
        }

        String note = StringUtils.hasText(remark) ? remark.trim() : null;
        int rows = requirementMapper.update(null, Wrappers.<CustomRequirement>lambdaUpdate()
                .eq(CustomRequirement::getId, id)
                .eq(CustomRequirement::getVersion, requirement.getVersion())
                .set(CustomRequirement::getStatus, status)
                .set(CustomRequirement::getRemark, note == null ? requirement.getRemark() : note)
                .set(CustomRequirement::getUpdateBy, CurrentOperator.id())
                .set(CustomRequirement::getVersion, requirement.getVersion() + 1));
        assertUpdated(rows);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        CustomRequirement requirement = requireRequirement(id);
        if (requirement.getStatus() == REQUIREMENT_DEAL) {
            throw new OperationsException(ErrorCode.CONFLICT, "已成交的需求是业务凭据，不能删除，请改为关闭");
        }
        // 报价没有独立查询入口，需求删了就成孤儿，这里一并物理清理
        quoteMapper.deleteByRequirementId(id);
        requirementMapper.deleteById(id);
    }

    // ──────────────────────────────────────────────
    // 内部方法
    // ──────────────────────────────────────────────

    private CustomRequirement requireRequirement(Long id) {
        CustomRequirement requirement = requirementMapper.selectById(id);
        if (requirement == null) {
            throw new OperationsException(ErrorCode.NOT_FOUND, "定制需求不存在或已删除");
        }
        return requirement;
    }

    private void requireHandler(Long handlerId) {
        if (handlerId == null) {
            throw new OperationsException("跟进人不能为空");
        }
        if (gateway.selectUsers(List.of(handlerId)).isEmpty()) {
            throw new OperationsException(ErrorCode.NOT_FOUND, "跟进人不存在或已删除");
        }
    }

    private void assertUpdated(int rows) {
        if (rows == 0) {
            throw new OperationsException(ErrorCode.CONFLICT, "需求已被其他人修改，请刷新后重试");
        }
    }

    private String nextRequirementNo() {
        for (int i = 0; i < NO_RETRY; i++) {
            String no = BizNoGenerator.next(BizNoGenerator.REQUIREMENT_NO_PREFIX);
            if (requirementMapper.countByRequirementNoIncludingDeleted(no) == 0) {
                return no;
            }
        }
        throw new OperationsException(ErrorCode.INTERNAL_ERROR, "需求单号生成失败，请重试");
    }

    private String statusText(byte status) {
        String desc = OperationsDict.REQUIREMENT_STATUS.get((int) status);
        return desc == null ? String.valueOf(status) : desc;
    }

    private List<CustomRequirementVO> toVOs(List<CustomRequirement> requirements) {
        if (requirements.isEmpty()) {
            return List.of();
        }
        Set<Long> customerIds = requirements.stream()
                .map(CustomRequirement::getCustomerId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, CustomerSnapshot> customers = customerIds.isEmpty() ? Map.of()
                : gateway.selectCustomers(customerIds).stream()
                .collect(Collectors.toMap(CustomerSnapshot::getId, c -> c));

        Set<Long> handlerIds = requirements.stream()
                .map(CustomRequirement::getHandlerId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, UserSnapshot> handlers = handlerIds.isEmpty() ? Map.of()
                : gateway.selectUsers(handlerIds).stream()
                .collect(Collectors.toMap(UserSnapshot::getId, u -> u));

        return requirements.stream().map(requirement -> {
            CustomRequirementVO vo = new CustomRequirementVO();
            vo.setId(requirement.getId());
            vo.setRequirementNo(requirement.getRequirementNo());
            vo.setCustomerId(requirement.getCustomerId());
            // Map.of() 不接受 null 键，未关联会员时必须先判空
            if (requirement.getCustomerId() != null) {
                CustomerSnapshot customer = customers.get(requirement.getCustomerId());
                if (customer != null) {
                    vo.setCustomerNickname(customer.getNickname());
                }
            }
            vo.setContactName(requirement.getContactName());
            vo.setContactPhone(cipher.maskPhone(requirement.getContactPhone()));
            vo.setDestination(requirement.getDestination());
            vo.setDepartDate(requirement.getDepartDate());
            vo.setDays(requirement.getDays());
            vo.setPeopleCount(requirement.getPeopleCount());
            vo.setBudget(requirement.getBudget());
            vo.setPreferences(requirement.getPreferences());
            vo.setStatus(requirement.getStatus());
            vo.setStatusDesc(OperationsDict.of(OperationsDict.REQUIREMENT_STATUS, requirement.getStatus()));
            vo.setHandlerId(requirement.getHandlerId());
            if (requirement.getHandlerId() != null) {
                UserSnapshot handler = handlers.get(requirement.getHandlerId());
                if (handler != null) {
                    vo.setHandlerName(StringUtils.hasText(handler.getNickname())
                            ? handler.getNickname() : handler.getUsername());
                }
            }
            vo.setRemark(requirement.getRemark());
            vo.setCreateTime(requirement.getCreateTime());
            vo.setUpdateTime(requirement.getUpdateTime());
            return vo;
        }).toList();
    }
}
