package com.rg.customer.service;

import com.rg.common.web.PageResult;
import com.rg.customer.dto.CouponGrantRequest;
import com.rg.customer.dto.CouponTemplateQuery;
import com.rg.customer.dto.CouponTemplateSaveRequest;
import com.rg.customer.vo.CouponTemplateVO;

import java.util.List;

/**
 * 优惠券模板服务。
 * <p>
 * 模板只定义「券长什么样、发多少、什么时候失效」，具体到某位会员手上的券由 {@link CouponService} 负责。
 */
public interface CouponTemplateService {

    PageResult<CouponTemplateVO> page(CouponTemplateQuery query);

    CouponTemplateVO detail(Long id);

    /**
     * 启用中的模板列表，供发券下拉框使用。
     */
    List<CouponTemplateVO> listEnabled();

    Long create(CouponTemplateSaveRequest request);

    /**
     * 编辑模板。已发出的券沿用新规则计算有效期，因此改有效期会影响存量未使用券，
     * 控制台需要在界面上给出提示。
     */
    void update(Long id, CouponTemplateSaveRequest request);

    void updateStatus(Long id, Byte status);

    /**
     * 删除模板。已发过券的模板不允许删除，只能停用，避免存量券丢失券面信息。
     */
    void delete(Long id);

    /**
     * 批量发券，整批事务；返回实际发放数量。
     */
    int grant(CouponGrantRequest request);
}
