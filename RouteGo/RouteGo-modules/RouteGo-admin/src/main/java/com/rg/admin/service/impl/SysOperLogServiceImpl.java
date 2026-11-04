package com.rg.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rg.admin.dto.OperLogQuery;
import com.rg.admin.entity.SysOperLog;
import com.rg.admin.mapper.SysOperLogMapper;
import com.rg.admin.service.SysOperLogService;
import com.rg.admin.vo.SysOperLogVO;
import com.rg.common.exception.ErrorCode;
import com.rg.common.exception.serviceException.AdminException;
import com.rg.common.web.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class SysOperLogServiceImpl extends ServiceImpl<SysOperLogMapper, SysOperLog>
        implements SysOperLogService {

    @Override
    public PageResult<SysOperLogVO> page(OperLogQuery query) {
        LambdaQueryWrapper<SysOperLog> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(query.getTitle())) {
            wrapper.like(SysOperLog::getTitle, query.getTitle());
        }
        if (StringUtils.hasText(query.getOperName())) {
            wrapper.like(SysOperLog::getOperName, query.getOperName());
        }
        if (query.getStatus() != null) {
            wrapper.eq(SysOperLog::getStatus, query.getStatus());
        }
        if (query.getStartTime() != null) {
            wrapper.ge(SysOperLog::getOperTime, query.getStartTime());
        }
        if (query.getEndTime() != null) {
            wrapper.le(SysOperLog::getOperTime, query.getEndTime());
        }
        wrapper.orderByDesc(SysOperLog::getId);
        Page<SysOperLog> page = baseMapper.selectPage(query.toPage(), wrapper);
        List<SysOperLogVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    @Override
    public SysOperLogVO detail(Long id) {
        SysOperLog log = baseMapper.selectById(id);
        if (log == null) {
            throw new AdminException(ErrorCode.NOT_FOUND, "操作日志不存在");
        }
        return toVO(log);
    }

    private SysOperLogVO toVO(SysOperLog log) {
        SysOperLogVO vo = new SysOperLogVO();
        vo.setId(log.getId());
        vo.setTitle(log.getTitle());
        vo.setBusinessType(log.getBusinessType());
        vo.setMethod(log.getMethod());
        vo.setRequestMethod(log.getRequestMethod());
        vo.setOperName(log.getOperName());
        vo.setOperUrl(log.getOperUrl());
        vo.setOperIp(log.getOperIp());
        vo.setOperParam(log.getOperParam());
        vo.setJsonResult(log.getJsonResult());
        vo.setStatus(log.getStatus());
        vo.setErrorMsg(log.getErrorMsg());
        vo.setCostTime(log.getCostTime());
        vo.setOperTime(log.getOperTime());
        return vo;
    }
}
