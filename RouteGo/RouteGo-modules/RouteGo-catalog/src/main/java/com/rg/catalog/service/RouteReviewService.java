package com.rg.catalog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rg.catalog.dto.RouteReviewQuery;
import com.rg.catalog.dto.RouteReviewReplyRequest;
import com.rg.catalog.entity.RouteReview;
import com.rg.catalog.vo.RouteReviewVO;
import com.rg.common.web.PageResult;

/**
 * 线路评价服务。
 */
public interface RouteReviewService extends IService<RouteReview> {

    PageResult<RouteReviewVO> page(RouteReviewQuery query);

    RouteReviewVO detail(Long id);

    void reply(Long id, RouteReviewReplyRequest request);

    void updateStatus(Long id, Byte status);

    void deleteReview(Long id);
}
