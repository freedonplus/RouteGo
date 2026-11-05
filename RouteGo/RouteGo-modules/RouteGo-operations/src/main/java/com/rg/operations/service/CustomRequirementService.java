package com.rg.operations.service;

import com.rg.common.web.PageResult;
import com.rg.operations.dto.CustomRequirementCreateRequest;
import com.rg.operations.dto.CustomRequirementQuery;
import com.rg.operations.dto.CustomRequirementUpdateRequest;
import com.rg.operations.vo.CustomRequirementVO;

/**
 * 定制需求服务。
 * <p>
 * 状态语义：0 待跟进 → 1 已报价 → 2 已成交 → 3 已关闭。
 * 其中「已报价」由报价单的创建自动驱动，「已成交」由采纳报价驱动或线下成交手工确认，
 * 因此人工状态接口只接受 2（线下成交）与 3（关闭），不接受把状态改回 1 或 0。
 */
public interface CustomRequirementService {

    PageResult<CustomRequirementVO> page(CustomRequirementQuery query);

    /**
     * 详情附带报价版本数与最新一版报价金额，前端不必再额外请求一次。
     */
    CustomRequirementVO detail(Long id);

    Long create(CustomRequirementCreateRequest request);

    void update(Long id, CustomRequirementUpdateRequest request);

    /**
     * 指派跟进人。已关闭的需求不可指派。
     */
    void assign(Long id, Long handlerId);

    /**
     * 人工状态变更，仅支持 2 已成交（线下成交）与 3 已关闭。
     */
    void updateStatus(Long id, Byte status, String remark);

    void delete(Long id);
}
