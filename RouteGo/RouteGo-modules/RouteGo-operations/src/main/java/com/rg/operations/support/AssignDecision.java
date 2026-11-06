package com.rg.operations.support;

/**
 * 自动分配结果。
 * <p>
 * 命中了规则但候选客服全部离职/停用时，{@code handlerId} 为空、
 * {@code ruleId} 不为空 —— 建单时据此把 {@code assign_rule_id} 也留空，
 * 让工单干净地回到「未指派」池子里等人工认领。
 *
 * @param ruleId    命中的规则 ID
 * @param ruleName  命中的规则名称
 * @param strategy  分配策略：1 轮询，2 当前在处理最少
 * @param handlerId 最终分配到的客服 ID，可能为空
 */
public record AssignDecision(Long ruleId, String ruleName, Integer strategy, Long handlerId) {

    public static AssignDecision none() {
        return new AssignDecision(null, null, null, null);
    }

    public boolean assigned() {
        return handlerId != null;
    }
}
