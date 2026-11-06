package com.rg.operations.support;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.rg.common.constant.CacheKeyConstants;
import com.rg.common.redis.RedisService;
import com.rg.operations.entity.CsAssignRule;
import com.rg.operations.enums.OperationsDict;
import com.rg.operations.mapper.CsAssignRuleMapper;
import com.rg.operations.mapper.CsTicketMapper;
import com.rg.operations.mapper.OperationsGatewayMapper;
import com.rg.operations.mapper.model.HandlerLoad;
import com.rg.operations.mapper.model.UserSnapshot;
import com.rg.operations.vo.AssignPreviewVO;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 工单自动分配引擎。
 * <p>
 * 匹配顺序：规则按 {@code sort} 升序、ID 升序逐条比对，第一条同时满足
 * 「类型不限或等于工单类型」且「优先级不限或等于工单优先级」的启用规则胜出。
 * 因此在最末尾放一条两个条件都为空的规则，就能当兜底规则用。
 * <p>
 * 选人策略：
 * <ul>
 *   <li><b>轮询</b>：Redis 计数器取模，保证单量平均；计数器不设过期，避免每次都从 0 开始。</li>
 *   <li><b>在处理最少</b>：统计候选人当前「处理中」的工单数，取最小的那个；并列时按规则里配置的顺序取前面的。</li>
 * </ul>
 * 候选人里已删除或已停用的账号会被剔除。全部不可用时返回 {@link AssignDecision#none()}，
 * 工单留在未指派池，绝不把单分给一个登录不了的人。
 * <p>
 * 注意：轮询计数器是 Redis 副作用，不受调用方事务回滚影响。跳号只影响分配均匀性，
 * 不影响正确性，所以不做补偿。
 */
@Component
public class CsAssignEngine {

    /** 分配策略：轮询 */
    public static final byte STRATEGY_ROUND_ROBIN = 1;
    /** 分配策略：在当前处理最少 */
    public static final byte STRATEGY_LEAST_BUSY = 2;

    private final CsAssignRuleMapper ruleMapper;
    private final CsTicketMapper ticketMapper;
    private final OperationsGatewayMapper gateway;
    private final RedisService redisService;

    public CsAssignEngine(CsAssignRuleMapper ruleMapper,
                          CsTicketMapper ticketMapper,
                          OperationsGatewayMapper gateway,
                          RedisService redisService) {
        this.ruleMapper = ruleMapper;
        this.ticketMapper = ticketMapper;
        this.gateway = gateway;
        this.redisService = redisService;
    }

    /**
     * 为一张新工单挑选处理人。
     *
     * @param type     工单类型
     * @param priority 工单优先级
     * @return 分配结果；没有任何规则命中时为 {@link AssignDecision#none()}
     */
    public AssignDecision decide(Byte type, Byte priority) {
        CsAssignRule rule = matchRule(type, priority);
        if (rule == null) {
            return AssignDecision.none();
        }
        List<Long> candidates = activeCandidates(rule);
        if (candidates.isEmpty()) {
            // 规则命中但人都不在了：保留规则信息，handlerId 留空，方便日志排查
            return new AssignDecision(rule.getId(), rule.getRuleName(),
                    rule.getStrategy() == null ? null : rule.getStrategy().intValue(), null);
        }
        Long handlerId = pickHandler(rule, candidates);
        return new AssignDecision(rule.getId(), rule.getRuleName(),
                rule.getStrategy() == null ? null : rule.getStrategy().intValue(), handlerId);
    }

    /**
     * 试算：规则配好后先看看会命中哪条、分给谁，不影响计数之外的状态。
     * <p>
     * 轮询策略下会真实推进一次游标 —— 否则试算永远返回同一个人，起不到验证作用；
     * 代价是多跳一格，对均匀性无影响。
     */
    public AssignPreviewVO preview(Byte type, Byte priority) {
        AssignDecision decision = decide(type, priority);
        AssignPreviewVO vo = new AssignPreviewVO();
        vo.setMatched(decision.ruleId() != null);
        vo.setRuleId(decision.ruleId());
        vo.setRuleName(decision.ruleName());
        if (decision.strategy() != null) {
            vo.setStrategy((byte) decision.strategy().intValue());
            vo.setStrategyDesc(OperationsDict.of(OperationsDict.ASSIGN_STRATEGY,
                    (byte) decision.strategy().intValue()));
        }
        if (decision.handlerId() != null) {
            vo.setHandlerId(decision.handlerId());
            vo.setHandlerName(nameOf(decision.handlerId()));
        } else if (decision.ruleId() == null) {
            vo.setMessage("没有匹配的启用规则，工单会留在未指派池");
        } else {
            vo.setMessage("规则已命中，但候选客服都被删除或停用，工单会留在未指派池");
        }
        return vo;
    }

    /**
     * 把逗号分隔的候选客服 ID 解析成列表，去空去重。
     */
    public static List<Long> parseUserIds(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        Set<Long> ids = new LinkedHashSet<>();
        for (String part : raw.split(",")) {
            String text = part.trim();
            if (text.isEmpty()) {
                continue;
            }
            try {
                ids.add(Long.valueOf(text));
            } catch (NumberFormatException ignored) {
                // 脏数据直接跳过，不让一条规则解析失败拖垮建单
            }
        }
        return new ArrayList<>(ids);
    }

    /** 把候选客服 ID 列表转成落库的逗号串 */
    public static String joinUserIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return "";
        }
        return userIds.stream().distinct().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("");
    }

    // ──────────────────────────────────────────────
    // 内部方法
    // ──────────────────────────────────────────────

    private CsAssignRule matchRule(Byte type, Byte priority) {
        List<CsAssignRule> rules = ruleMapper.selectList(Wrappers.<CsAssignRule>lambdaQuery()
                .eq(CsAssignRule::getEnabled, (byte) 1)
                .orderByAsc(CsAssignRule::getSort)
                .orderByAsc(CsAssignRule::getId));
        for (CsAssignRule rule : rules) {
            boolean typeOk = rule.getMatchType() == null || rule.getMatchType().equals(type);
            boolean priorityOk = rule.getMatchPriority() == null || rule.getMatchPriority().equals(priority);
            if (typeOk && priorityOk) {
                return rule;
            }
        }
        return null;
    }

    /** 候选客服中仍然存在且启用的账号，保持规则里配置的顺序 */
    private List<Long> activeCandidates(CsAssignRule rule) {
        List<Long> wanted = parseUserIds(rule.getUserIds());
        if (wanted.isEmpty()) {
            return List.of();
        }
        Map<Long, UserSnapshot> alive = gateway.selectEnabledUsers(wanted).stream()
                .collect(java.util.stream.Collectors.toMap(UserSnapshot::getId, u -> u, (a, b) -> a));
        return wanted.stream().filter(alive::containsKey).toList();
    }

    private Long pickHandler(CsAssignRule rule, List<Long> candidates) {
        byte strategy = rule.getStrategy() == null ? STRATEGY_LEAST_BUSY : rule.getStrategy();
        if (strategy == STRATEGY_ROUND_ROBIN) {
            return roundRobin(rule.getId(), candidates);
        }
        return leastBusy(candidates);
    }

    private Long roundRobin(Long ruleId, List<Long> candidates) {
        String key = CacheKeyConstants.Operations.CS_ASSIGN_ROUND_ROBIN.formatted(ruleId);
        Long cursor = redisService.increment(key, 1L);
        if (cursor == null || cursor < 0) {
            return candidates.get(0);
        }
        int index = (int) ((cursor - 1) % candidates.size());
        return candidates.get(index);
    }

    private Long leastBusy(List<Long> candidates) {
        Map<Long, Long> loads = new LinkedHashMap<>();
        candidates.forEach(id -> loads.put(id, 0L));
        for (HandlerLoad row : ticketMapper.countProcessingByHandlers(candidates)) {
            if (row.getHandlerId() != null) {
                loads.put(row.getHandlerId(), row.getTotal() == null ? 0L : row.getTotal());
            }
        }
        return loads.entrySet().stream()
                .min(Comparator.comparingLong(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private String nameOf(Long userId) {
        List<UserSnapshot> users = gateway.selectUsers(List.of(userId));
        if (users.isEmpty()) {
            return null;
        }
        UserSnapshot user = users.get(0);
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
    }
}
