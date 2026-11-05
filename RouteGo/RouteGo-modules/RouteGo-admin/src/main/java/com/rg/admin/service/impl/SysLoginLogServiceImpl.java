package com.rg.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rg.admin.dto.LoginLogQuery;
import com.rg.admin.entity.SysLoginLog;
import com.rg.admin.mapper.SysLoginLogMapper;
import com.rg.admin.service.SysLoginLogService;
import com.rg.admin.vo.SysLoginLogVO;
import com.rg.common.ip.IpLocationService;
import com.rg.common.web.PageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 登录日志实现：异步写入，异常只记日志。
 * <p>
 * 归属地解析也放在异步线程里：接口慢或不可用时最多拖慢日志落库，
 * 不会影响登录响应。
 */
@Slf4j
@Service
public class SysLoginLogServiceImpl extends ServiceImpl<SysLoginLogMapper, SysLoginLog>
        implements SysLoginLogService {

    private final IpLocationService ipLocationService;

    public SysLoginLogServiceImpl(IpLocationService ipLocationService) {
        this.ipLocationService = ipLocationService;
    }

    @Async
    @Override
    public void record(String username, String ip, boolean success, String msg) {
        try {
            SysLoginLog loginLog = new SysLoginLog();
            loginLog.setUsername(username);
            loginLog.setIp(ip);
            loginLog.setLocation(resolveLocation(ip));
            loginLog.setStatus((byte) (success ? 1 : 0));
            loginLog.setMsg(msg);
            loginLog.setLoginTime(LocalDateTime.now());
            save(loginLog);
        } catch (RuntimeException e) {
            log.warn("登录日志写入失败: username={}, reason={}", username, e.getMessage());
        }
    }

    private String resolveLocation(String ip) {
        try {
            return ipLocationService.describe(ip);
        } catch (RuntimeException e) {
            log.debug("IP 归属地解析失败 ip={}: {}", ip, e.getMessage());
            return null;
        }
    }

    @Override
    public PageResult<SysLoginLogVO> page(LoginLogQuery query) {
        LambdaQueryWrapper<SysLoginLog> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(query.getUsername())) {
            wrapper.like(SysLoginLog::getUsername, query.getUsername());
        }
        if (StringUtils.hasText(query.getIp())) {
            wrapper.like(SysLoginLog::getIp, query.getIp());
        }
        if (query.getStatus() != null) {
            wrapper.eq(SysLoginLog::getStatus, query.getStatus());
        }
        if (query.getStartTime() != null) {
            wrapper.ge(SysLoginLog::getLoginTime, query.getStartTime());
        }
        if (query.getEndTime() != null) {
            wrapper.le(SysLoginLog::getLoginTime, query.getEndTime());
        }
        wrapper.orderByDesc(SysLoginLog::getId);
        Page<SysLoginLog> page = baseMapper.selectPage(query.toPage(), wrapper);
        List<SysLoginLogVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    private SysLoginLogVO toVO(SysLoginLog log) {
        SysLoginLogVO vo = new SysLoginLogVO();
        vo.setId(log.getId());
        vo.setUsername(log.getUsername());
        vo.setIp(log.getIp());
        vo.setLocation(log.getLocation());
        vo.setBrowser(log.getBrowser());
        vo.setOs(log.getOs());
        vo.setStatus(log.getStatus());
        vo.setMsg(log.getMsg());
        vo.setLoginTime(log.getLoginTime());
        return vo;
    }
}
