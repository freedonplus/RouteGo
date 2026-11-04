package com.rg.admin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rg.admin.entity.SysLoginLog;
import com.rg.admin.mapper.SysLoginLogMapper;
import com.rg.admin.service.SysLoginLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 登录日志实现：异步写入，异常只记日志。
 */
@Slf4j
@Service
public class SysLoginLogServiceImpl extends ServiceImpl<SysLoginLogMapper, SysLoginLog>
        implements SysLoginLogService {

    @Async
    @Override
    public void record(String username, String ip, boolean success, String msg) {
        try {
            SysLoginLog loginLog = new SysLoginLog();
            loginLog.setUsername(username);
            loginLog.setIp(ip);
            loginLog.setStatus((byte) (success ? 1 : 0));
            loginLog.setMsg(msg);
            loginLog.setLoginTime(LocalDateTime.now());
            save(loginLog);
        } catch (RuntimeException e) {
            log.warn("登录日志写入失败: username={}, reason={}", username, e.getMessage());
        }
    }
}
