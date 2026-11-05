package com.rg.operations.service;

import com.rg.common.web.PageResult;
import com.rg.operations.dto.CustomQuoteCreateRequest;
import com.rg.operations.dto.CustomQuoteQuery;
import com.rg.operations.dto.CustomQuoteUpdateRequest;
import com.rg.operations.vo.CustomQuoteVO;

import java.util.List;

/**
 * 定制报价服务。
 * <p>
 * 一个需求可以有多版报价，但同一时间只允许一版被采纳：
 * 采纳时会把同需求下其它「待确认」报价全部置为「已失效」，并把需求推进到已成交。
 * 只有待确认的报价可以编辑、失效或删除。
 */
public interface CustomQuoteService {

    PageResult<CustomQuoteVO> page(CustomQuoteQuery query);

    CustomQuoteVO detail(Long id);

    /**
     * 某需求的全部报价版本，按版本从新到旧。
     */
    List<CustomQuoteVO> listByRequirement(Long requirementId);

    Long create(CustomQuoteCreateRequest request);

    void update(Long id, CustomQuoteUpdateRequest request);

    /**
     * 采纳报价：本单置已采纳、同需求其它待确认报价置已失效、需求置已成交，整体一个事务。
     */
    void accept(Long id);

    /**
     * 作废报价：仅待确认的报价可作废。
     */
    void invalidate(Long id);

    /**
     * 删除报价：仅待确认的报价可删除，已采纳的报价是成交凭据必须保留。
     */
    void delete(Long id);
}
