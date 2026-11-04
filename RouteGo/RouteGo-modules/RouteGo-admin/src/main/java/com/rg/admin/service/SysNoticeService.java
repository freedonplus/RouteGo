package com.rg.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rg.admin.dto.NoticeCreateRequest;
import com.rg.admin.dto.NoticeQuery;
import com.rg.admin.dto.NoticeUpdateRequest;
import com.rg.admin.entity.SysNotice;
import com.rg.admin.vo.SysNoticeVO;
import com.rg.common.web.PageResult;

import java.util.List;

public interface SysNoticeService extends IService<SysNotice> {

    PageResult<SysNoticeVO> page(NoticeQuery query);

    List<SysNoticeVO> listPublished();

    SysNoticeVO detail(Long id);

    Long create(NoticeCreateRequest request);

    void update(Long id, NoticeUpdateRequest request);

    void deleteNotice(Long id);

    void publish(Long id);

    void unpublish(Long id);

    void setTop(Long id, boolean top);
}
