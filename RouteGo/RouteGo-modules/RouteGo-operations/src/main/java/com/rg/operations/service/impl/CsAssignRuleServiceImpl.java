package com.rg.operations.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rg.common.exception.ErrorCode;
import com.rg.common.exception.serviceException.OperationsException;
import com.rg.common.web.PageResult;
import com.rg.operations.dto.CsAssignRuleQuery;
import com.rg.operations.dto.CsAssignRuleSaveRequest;
import com.rg.operations.entity.CsAssignRule;
import com.rg.operations.enums.OperationsDict;
import com.rg.operations.mapper.CsAssignRuleMapper;
import com.rg.operations.mapper.CsTicketMapper;
import com.rg.operations.mapper.OperationsGatewayMapper;
import com.rg.operations.mapper.model.HandlerLoad;
import com.rg.operations.mapper.model.UserSnapshot;
import com.rg.operations.service.CsAssignRuleService;
import com.rg.operations.support.CsAssignEngine;
import com.rg.operations.support.CurrentOperator;
import com.rg.operations.vo.CsAssignRuleVO;
import com.rg.operations.vo.HandlerWorkloadVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 客服自动分配规则实现。
 * <p>
 * 两条硬约束：
 * <ul>
 *   <li>候选客服必须至少有一个「未删除且启用」的账号，否则规则配了也分不出去；
 *       保存时直接拒绝，避免上线后才发现工单全堆在未指派池。</li>
 *   <li>规则按 sort 升序匹配，同 sort 按 ID 升序，保证匹配结果稳定可预期。</li>
 * </ul>
 */
@Service
public class CsAssignRuleServiceImpl implements CsAssignRuleService {

    private final CsAssignRuleMapper ruleMapper;
    private final CsTicketMapper ticketMapper;
    private final OperationsGatewayMapper gateway;

    public CsAssignRuleServiceImpl(CsAssignRuleMapper ruleMapper,
                                   CsTicketMapper ticketMapper,
                                   OperationsGatewayMapper gateway) {
        this.ruleMapper = ruleMapper;
        this.ticketMapper = ticketMapper;
        this.gateway = gateway;
    }

    @Override
    public PageResult<CsAssignRuleVO> page(CsAssignRuleQuery query) {
        LambdaQueryWrapper<CsAssignRule> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(CsAssignRule::getRuleName, query.getKeyword().trim());
        }
        if (query.getEnabled() != null) {
            wrapper.eq(CsAssignRule::getEnabled, query.getEnabled().byteValue());
        }
        wrapper.orderByAsc(CsAssignRule::getSort).orderByAsc(CsAssignRule::getId);
        Page<CsAssignRule> page = ruleMapper.selectPage(query.toPage(), wrapper);
        return new PageResult<>(toVOs(page.getRecords()), page.getTotal(), page.getCurrent(),
                page.getSize(), page.getPages());
    }

    @Override
    public List<CsAssignRuleVO> listEnabled() {
        List<CsAssignRule> rules = ruleMapper.selectList(Wrappers.<CsAssignRule>lambdaQuery()
                .eq(CsAssignRule::getEnabled, (byte) 1)
                .orderByAsc(CsAssignRule::getSort)
                .orderByAsc(CsAssignRule::getId));
        return toVOs(rules);
    }

    @Override
    public CsAssignRuleVO detail(Long id) {
        return toVOs(List.of(requireRule(id))).get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(CsAssignRuleSaveRequest request) {
        List<Long> userIds = requireUsableUsers(request.getUserIds());
        CsAssignRule rule = new CsAssignRule();
        applyRequest(rule, request, userIds);
        rule.setVersion(0);
        rule.setCreateBy(CurrentOperator.id());
        rule.setCreateTime(LocalDateTime.now());
        rule.setDeleted((byte) 0);
        ruleMapper.insert(rule);
        return rule.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, CsAssignRuleSaveRequest request) {
        CsAssignRule existing = requireRule(id);
        List<Long> userIds = requireUsableUsers(request.getUserIds());

        CsAssignRule update = new CsAssignRule();
        update.setId(existing.getId());
        applyRequest(update, request, userIds);
        update.setVersion(existing.getVersion());
        update.setUpdateBy(CurrentOperator.id());
        update.setUpdateTime(LocalDateTime.now());
        if (ruleMapper.updateById(update) == 0) {
            throw new OperationsException(ErrorCode.CONFLICT, "规则已被其他人修改，请刷新后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer enabled) {
        CsAssignRule existing = requireRule(id);
        if (enabled == null || (enabled != 0 && enabled != 1)) {
            throw new OperationsException("状态取值只能是 0 或 1");
        }
        CsAssignRule update = new CsAssignRule();
        update.setId(existing.getId());
        update.setEnabled(enabled.byteValue());
        update.setVersion(existing.getVersion());
        update.setUpdateBy(CurrentOperator.id());
        update.setUpdateTime(LocalDateTime.now());
        if (ruleMapper.updateById(update) == 0) {
            throw new OperationsException(ErrorCode.CONFLICT, "规则已被其他人修改，请刷新后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireRule(id);
        // 规则只影响「以后怎么分」，逻辑删除即可，历史工单里的 assign_rule_id 仍可追溯
        ruleMapper.deleteById(id);
    }

    @Override
    public List<HandlerWorkloadVO> workload(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        List<Long> wanted = userIds.stream().filter(Objects::nonNull).distinct().toList();
        if (wanted.isEmpty()) {
            return List.of();
        }
        Map<Long, UserSnapshot> users = gateway.selectUsers(wanted).stream()
                .collect(Collectors.toMap(UserSnapshot::getId, u -> u, (a, b) -> a));
        Map<Long, Long> loads = new LinkedHashMap<>();
        wanted.forEach(id -> loads.put(id, 0L));
        for (HandlerLoad row : ticketMapper.countProcessingByHandlers(wanted)) {
            if (row.getHandlerId() != null) {
                loads.put(row.getHandlerId(), row.getTotal() == null ? 0L : row.getTotal());
            }
        }
        List<HandlerWorkloadVO> list = new ArrayList<>(wanted.size());
        for (Long userId : wanted) {
            HandlerWorkloadVO vo = new HandlerWorkloadVO();
            vo.setHandlerId(userId);
            vo.setProcessing(loads.getOrDefault(userId, 0L));
            UserSnapshot user = users.get(userId);
            if (user == null) {
                vo.setAvailable(0);
                vo.setHandlerName("账号已删除");
            } else {
                vo.setAvailable(1);
                vo.setUsername(user.getUsername());
                vo.setHandlerName(StringUtils.hasText(user.getNickname())
                        ? user.getNickname() : user.getUsername());
            }
            list.add(vo);
        }
        return list;
    }

    // ──────────────────────────────────────────────
    // 内部方法
    // ──────────────────────────────────────────────

    private CsAssignRule requireRule(Long id) {
        if (id == null) {
            throw new OperationsException("规则 ID 不能为空");
        }
        CsAssignRule rule = ruleMapper.selectById(id);
        if (rule == null) {
            throw new OperationsException(ErrorCode.NOT_FOUND, "分配规则不存在或已删除");
        }
        return rule;
    }

    /** 校验候选客服：至少一个可用的账号，否则规则等于没配 */
    private List<Long> requireUsableUsers(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            throw new OperationsException("至少选择一个客服");
        }
        List<Long> wanted = userIds.stream().filter(Objects::nonNull).distinct().toList();
        if (wanted.size() > 50) {
            throw new OperationsException("单条规则最多 50 个客服");
        }
        Set<Long> alive = gateway.selectEnabledUsers(wanted).stream()
                .map(UserSnapshot::getId).collect(Collectors.toSet());
        if (alive.isEmpty()) {
            throw new OperationsException("所选客服都不存在或已停用，请重新选择");
        }
        return wanted;
    }

    private void applyRequest(CsAssignRule rule, CsAssignRuleSaveRequest request, List<Long> userIds) {
        rule.setRuleName(request.getRuleName().trim());
        rule.setMatchType(request.getMatchType() == null ? null : request.getMatchType().byteValue());
        rule.setMatchPriority(request.getMatchPriority() == null ? null : request.getMatchPriority().byteValue());
        rule.setStrategy(request.getStrategy() == null
                ? CsAssignEngine.STRATEGY_LEAST_BUSY : request.getStrategy().byteValue());
        rule.setUserIds(CsAssignEngine.joinUserIds(userIds));
        rule.setEnabled(request.getEnabled() == null ? (byte) 1 : request.getEnabled().byteValue());
        rule.setSort(request.getSort() == null ? 100 : request.getSort());
        rule.setRemark(StringUtils.hasText(request.getRemark()) ? request.getRemark().trim() : null);
    }

    private List<CsAssignRuleVO> toVOs(List<CsAssignRule> rules) {
        if (rules.isEmpty()) {
            return List.of();
        }
        Set<Long> userIds = rules.stream()
                .flatMap(rule -> CsAssignEngine.parseUserIds(rule.getUserIds()).stream())
                .collect(Collectors.toSet());
        Map<Long, UserSnapshot> users = userIds.isEmpty() ? Map.of()
                : gateway.selectUsers(userIds).stream()
                .collect(Collectors.toMap(UserSnapshot::getId, u -> u, (a, b) -> a));

        List<CsAssignRuleVO> list = new ArrayList<>(rules.size());
        for (CsAssignRule rule : rules) {
            CsAssignRuleVO vo = new CsAssignRuleVO();
            vo.setId(rule.getId());
            vo.setRuleName(rule.getRuleName());
            vo.setMatchType(rule.getMatchType());
            vo.setMatchTypeDesc(rule.getMatchType() == null ? "不限"
                    : OperationsDict.of(OperationsDict.TICKET_TYPE, rule.getMatchType()));
            vo.setMatchPriority(rule.getMatchPriority());
            vo.setMatchPriorityDesc(rule.getMatchPriority() == null ? "不限"
                    : OperationsDict.of(OperationsDict.TICKET_PRIORITY, rule.getMatchPriority()));
            vo.setStrategy(rule.getStrategy());
            vo.setStrategyDesc(OperationsDict.of(OperationsDict.ASSIGN_STRATEGY, rule.getStrategy()));

            List<Long> ids = CsAssignEngine.parseUserIds(rule.getUserIds());
            vo.setUserIds(ids);
            vo.setUserNames(ids.stream().map(id -> {
                UserSnapshot user = users.get(id);
                if (user == null) {
                    return "账号已删除";
                }
                return StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
            }).toList());

            vo.setEnabled(rule.getEnabled());
            vo.setSort(rule.getSort());
            vo.setRemark(rule.getRemark());
            vo.setCreateTime(rule.getCreateTime());
            vo.setUpdateTime(rule.getUpdateTime());
            list.add(vo);
        }
        return list;
    }
}
