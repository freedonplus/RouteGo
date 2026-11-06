package com.rg.operations.support;

import com.rg.common.redis.RedisService;
import com.rg.operations.entity.CsAssignRule;
import com.rg.operations.mapper.CsAssignRuleMapper;
import com.rg.operations.mapper.CsTicketMapper;
import com.rg.operations.mapper.OperationsGatewayMapper;
import com.rg.operations.mapper.model.HandlerLoad;
import com.rg.operations.mapper.model.UserSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * 自动分配的三条底线：匹配顺序可预期、轮询真的轮起来、没人在岗就把单留在池子里。
 */
@ExtendWith(MockitoExtension.class)
class CsAssignEngineTest {

    @Mock
    private CsAssignRuleMapper ruleMapper;
    @Mock
    private CsTicketMapper ticketMapper;
    @Mock
    private OperationsGatewayMapper gateway;
    @Mock
    private RedisService redisService;

    private CsAssignEngine engine;

    @BeforeEach
    void setUp() {
        engine = new CsAssignEngine(ruleMapper, ticketMapper, gateway, redisService);
    }

    @Test
    @DisplayName("没有任何启用规则时不分配")
    void noRuleMeansNoAssignment() {
        when(ruleMapper.selectList(any())).thenReturn(List.of());

        AssignDecision decision = engine.decide((byte) 1, (byte) 2);

        assertFalse(decision.assigned());
        assertNull(decision.ruleId());
    }

    @Test
    @DisplayName("类型不匹配的规则跳过，命中类型一致的规则")
    void matchesByTicketType() {
        CsAssignRule refund = rule(1L, (byte) 3, null, (byte) 2, "11");
        CsAssignRule presale = rule(2L, (byte) 1, null, (byte) 2, "12");
        when(ruleMapper.selectList(any())).thenReturn(List.of(refund, presale));
        when(gateway.selectEnabledUsers(List.of(12L))).thenReturn(List.of(user(12L)));
        when(ticketMapper.countProcessingByHandlers(List.of(12L))).thenReturn(List.of());

        AssignDecision decision = engine.decide((byte) 1, (byte) 2);

        assertTrue(decision.assigned());
        assertEquals(2L, decision.ruleId());
        assertEquals(12L, decision.handlerId());
    }

    @Test
    @DisplayName("类型为空表示不限，兜底规则接住所有工单")
    void wildcardRuleCatchesEverything() {
        when(ruleMapper.selectList(any())).thenReturn(List.of(rule(9L, null, null, (byte) 2, "21")));
        when(gateway.selectEnabledUsers(List.of(21L))).thenReturn(List.of(user(21L)));
        when(ticketMapper.countProcessingByHandlers(List.of(21L))).thenReturn(List.of());

        AssignDecision decision = engine.decide((byte) 5, (byte) 3);

        assertEquals(9L, decision.ruleId());
        assertEquals(21L, decision.handlerId());
    }

    @Test
    @DisplayName("优先级不匹配时继续往下找")
    void skipsRuleWithDifferentPriority() {
        CsAssignRule urgent = rule(1L, null, (byte) 3, (byte) 2, "31");
        CsAssignRule fallback = rule(2L, null, null, (byte) 2, "32");
        when(ruleMapper.selectList(any())).thenReturn(List.of(urgent, fallback));
        when(gateway.selectEnabledUsers(List.of(32L))).thenReturn(List.of(user(32L)));
        when(ticketMapper.countProcessingByHandlers(List.of(32L))).thenReturn(List.of());

        AssignDecision decision = engine.decide((byte) 1, (byte) 2);

        assertEquals(2L, decision.ruleId());
        assertEquals(32L, decision.handlerId());
    }

    @Test
    @DisplayName("轮询策略按计数器依次分给候选中的人")
    void roundRobinRotatesCandidates() {
        when(ruleMapper.selectList(any())).thenReturn(List.of(rule(1L, null, null, (byte) 1, "41,42,43")));
        when(gateway.selectEnabledUsers(List.of(41L, 42L, 43L)))
                .thenReturn(List.of(user(41L), user(42L), user(43L)));
        when(redisService.increment(any(), eq(1L))).thenReturn(1L, 2L, 3L, 4L);

        assertEquals(41L, engine.decide((byte) 1, (byte) 2).handlerId());
        assertEquals(42L, engine.decide((byte) 1, (byte) 2).handlerId());
        assertEquals(43L, engine.decide((byte) 1, (byte) 2).handlerId());
        // 第 4 次回到第一个人，说明取模没有越界
        assertEquals(41L, engine.decide((byte) 1, (byte) 2).handlerId());
    }

    @Test
    @DisplayName("「在处理最少」把单分给当前最闲的人，并列时取配置靠前的")
    void leastBusyPicksLightestHandler() {
        when(ruleMapper.selectList(any())).thenReturn(List.of(rule(1L, null, null, (byte) 2, "51,52")));
        when(gateway.selectEnabledUsers(List.of(51L, 52L))).thenReturn(List.of(user(51L), user(52L)));
        when(ticketMapper.countProcessingByHandlers(List.of(51L, 52L)))
                .thenReturn(List.of(load(51L, 5L), load(52L, 2L)));

        assertEquals(52L, engine.decide((byte) 1, (byte) 2).handlerId());
    }

    @Test
    @DisplayName("候选人全被删除或停用时不分单，但保留命中的规则")
    void unassignedWhenAllCandidatesDisabled() {
        when(ruleMapper.selectList(any())).thenReturn(List.of(rule(1L, null, null, (byte) 2, "61,62")));
        when(gateway.selectEnabledUsers(List.of(61L, 62L))).thenReturn(List.of());

        AssignDecision decision = engine.decide((byte) 1, (byte) 2);

        assertFalse(decision.assigned());
        assertEquals(1L, decision.ruleId());
    }

    @Test
    @DisplayName("候选客服 ID 解析：去空白、去重、脏数据跳过")
    void parseUserIdsIsForgiving() {
        assertEquals(List.of(1L, 2L, 3L), CsAssignEngine.parseUserIds(" 1, 2 ,2,3, x,,"));
        assertEquals(List.of(), CsAssignEngine.parseUserIds("  "));
        assertEquals(List.of(), CsAssignEngine.parseUserIds(null));
        assertEquals("1,2,3", CsAssignEngine.joinUserIds(List.of(1L, 2L, 2L, 3L)));
    }

    @Test
    @DisplayName("试算返回命中规则与客服姓名")
    void previewReportsMatch() {
        when(ruleMapper.selectList(any())).thenReturn(List.of(rule(1L, null, null, (byte) 2, "71")));
        when(gateway.selectEnabledUsers(List.of(71L))).thenReturn(List.of(user(71L)));
        when(ticketMapper.countProcessingByHandlers(List.of(71L))).thenReturn(List.of());
        when(gateway.selectUsers(List.of(71L))).thenReturn(List.of(user(71L)));

        var preview = engine.preview((byte) 2, (byte) 2);

        assertTrue(preview.isMatched());
        assertEquals(71L, preview.getHandlerId());
        assertEquals("客服71", preview.getHandlerName());
        assertEquals("在处理最少", preview.getStrategyDesc());
    }

    // ──────────────────────────────────────────────
    // 构造用的辅助方法
    // ──────────────────────────────────────────────

    private CsAssignRule rule(Long id, Byte type, Byte priority, byte strategy, String userIds) {
        CsAssignRule rule = new CsAssignRule();
        rule.setId(id);
        rule.setRuleName("规则" + id);
        rule.setMatchType(type);
        rule.setMatchPriority(priority);
        rule.setStrategy(strategy);
        rule.setUserIds(userIds);
        rule.setEnabled((byte) 1);
        rule.setSort(1);
        return rule;
    }

    private UserSnapshot user(Long id) {
        UserSnapshot user = new UserSnapshot();
        user.setId(id);
        user.setUsername("u" + id);
        user.setNickname("客服" + id);
        return user;
    }

    private HandlerLoad load(Long handlerId, Long total) {
        HandlerLoad load = new HandlerLoad();
        load.setHandlerId(handlerId);
        load.setTotal(total);
        return load;
    }
}
