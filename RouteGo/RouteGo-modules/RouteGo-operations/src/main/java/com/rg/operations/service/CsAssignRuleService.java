package com.rg.operations.service;

import com.rg.common.web.PageResult;
import com.rg.operations.dto.CsAssignRuleQuery;
import com.rg.operations.dto.CsAssignRuleSaveRequest;
import com.rg.operations.vo.CsAssignRuleVO;
import com.rg.operations.vo.HandlerWorkloadVO;

import java.util.List;

/**
 * 客服自动分配规则。
 */
public interface CsAssignRuleService {

    PageResult<CsAssignRuleVO> page(CsAssignRuleQuery query);

    /** 启用中的规则，按匹配顺序返回，供前端「当前生效顺序」预览 */
    List<CsAssignRuleVO> listEnabled();

    CsAssignRuleVO detail(Long id);

    Long create(CsAssignRuleSaveRequest request);

    void update(Long id, CsAssignRuleSaveRequest request);

    /** 启停规则；停用后不再参与新工单分配，已分的工单不受影响 */
    void updateStatus(Long id, Integer enabled);

    void delete(Long id);

    /** 指定客服当前在手工单量，用于配置「在处理最少」策略时参考 */
    List<HandlerWorkloadVO> workload(List<Long> userIds);
}
