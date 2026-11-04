package com.rg.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rg.admin.dto.NoticeCreateRequest;
import com.rg.admin.dto.NoticeQuery;
import com.rg.admin.dto.NoticeUpdateRequest;
import com.rg.admin.entity.SysNotice;
import com.rg.admin.mapper.SysNoticeMapper;
import com.rg.admin.service.SysNoticeService;
import com.rg.admin.vo.SysNoticeVO;
import com.rg.common.cache.CacheHelper;
import com.rg.common.constant.CacheKeyConstants;
import com.rg.common.exception.ErrorCode;
import com.rg.common.exception.serviceException.AdminException;
import com.rg.common.web.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SysNoticeServiceImpl extends ServiceImpl<SysNoticeMapper, SysNotice>
        implements SysNoticeService {

    private static final Duration TTL = Duration.ofMinutes(5);

    private final SysNoticeMapper noticeMapper;
    private final CacheHelper cacheHelper;

    public SysNoticeServiceImpl(SysNoticeMapper noticeMapper, CacheHelper cacheHelper) {
        this.noticeMapper = noticeMapper;
        this.cacheHelper = cacheHelper;
    }

    @Override
    public PageResult<SysNoticeVO> page(NoticeQuery query) {
        LambdaQueryWrapper<SysNotice> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(query.getTitle())) {
            wrapper.like(SysNotice::getTitle, query.getTitle());
        }
        if (query.getType() != null) {
            wrapper.eq(SysNotice::getType, query.getType());
        }
        if (query.getStatus() != null) {
            wrapper.eq(SysNotice::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(SysNotice::getIsTop).orderByDesc(SysNotice::getPublishTime)
                .orderByDesc(SysNotice::getId);
        Page<SysNotice> page = noticeMapper.selectPage(query.toPage(), wrapper);
        List<SysNoticeVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    @Override
    public List<SysNoticeVO> listPublished() {
        return cacheHelper.getOrLoad(CacheKeyConstants.Admin.NOTICE_PUBLISHED, TTL,
                () -> noticeMapper.selectList(Wrappers.<SysNotice>lambdaQuery()
                                .eq(SysNotice::getStatus, 1)
                                .orderByDesc(SysNotice::getIsTop)
                                .orderByDesc(SysNotice::getPublishTime)
                                .orderByDesc(SysNotice::getId))
                        .stream().map(this::toVO).toList());
    }

    @Override
    public SysNoticeVO detail(Long id) {
        return toVO(requireNotice(id));
    }

    @Override
    @Transactional
    public Long create(NoticeCreateRequest request) {
        SysNotice notice = new SysNotice();
        notice.setTitle(request.getTitle());
        notice.setContent(request.getContent());
        notice.setType(request.getType() == null ? (byte) 1 : request.getType());
        notice.setStatus(request.getStatus() == null ? (byte) 0 : request.getStatus());
        notice.setIsTop(request.getIsTop() == null ? (byte) 0 : request.getIsTop());
        if (notice.getStatus() == 1) {
            notice.setPublishTime(LocalDateTime.now());
        }
        noticeMapper.insert(notice);
        cacheHelper.evictAfterCommit(CacheKeyConstants.Admin.NOTICE_PUBLISHED);
        return notice.getId();
    }

    @Override
    @Transactional
    public void update(Long id, NoticeUpdateRequest request) {
        requireNotice(id);
        SysNotice update = new SysNotice();
        update.setId(id);
        update.setTitle(request.getTitle());
        update.setContent(request.getContent());
        update.setType(request.getType());
        update.setIsTop(request.getIsTop());
        if (noticeMapper.updateById(update) == 0) {
            throw new AdminException(ErrorCode.CONFLICT, "数据已被修改，请刷新后重试");
        }
        cacheHelper.evictAfterCommit(CacheKeyConstants.Admin.NOTICE_PUBLISHED);
    }

    @Override
    @Transactional
    public void deleteNotice(Long id) {
        requireNotice(id);
        noticeMapper.deleteById(id);
        cacheHelper.evictAfterCommit(CacheKeyConstants.Admin.NOTICE_PUBLISHED);
    }

    @Override
    @Transactional
    public void publish(Long id) {
        requireNotice(id);
        SysNotice update = new SysNotice();
        update.setId(id);
        update.setStatus((byte) 1);
        update.setPublishTime(LocalDateTime.now());
        noticeMapper.updateById(update);
        cacheHelper.evictAfterCommit(CacheKeyConstants.Admin.NOTICE_PUBLISHED);
    }

    @Override
    @Transactional
    public void unpublish(Long id) {
        requireNotice(id);
        SysNotice update = new SysNotice();
        update.setId(id);
        update.setStatus((byte) 0);
        noticeMapper.updateById(update);
        cacheHelper.evictAfterCommit(CacheKeyConstants.Admin.NOTICE_PUBLISHED);
    }

    @Override
    @Transactional
    public void setTop(Long id, boolean top) {
        requireNotice(id);
        SysNotice update = new SysNotice();
        update.setId(id);
        update.setIsTop((byte) (top ? 1 : 0));
        noticeMapper.updateById(update);
        cacheHelper.evictAfterCommit(CacheKeyConstants.Admin.NOTICE_PUBLISHED);
    }

    private SysNotice requireNotice(Long id) {
        SysNotice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new AdminException(ErrorCode.NOT_FOUND, "公告不存在");
        }
        return notice;
    }

    private SysNoticeVO toVO(SysNotice notice) {
        SysNoticeVO vo = new SysNoticeVO();
        vo.setId(notice.getId());
        vo.setTitle(notice.getTitle());
        vo.setContent(notice.getContent());
        vo.setType(notice.getType());
        vo.setStatus(notice.getStatus());
        vo.setIsTop(notice.getIsTop());
        vo.setPublishTime(notice.getPublishTime());
        vo.setCreateTime(notice.getCreateTime());
        return vo;
    }
}
