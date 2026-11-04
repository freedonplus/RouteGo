package com.rg.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rg.admin.dto.LoginLogQuery;
import com.rg.admin.entity.SysLoginLog;
import com.rg.admin.vo.SysLoginLogVO;
import com.rg.common.web.PageResult;

/**
 * 登录日志服务。
 */
public interface SysLoginLogService extends IService<SysLoginLog> {

    /**
     * 异步记录一次登录结果，失败不影响登录主流程。
     */
    void record(String username, String ip, boolean success, String msg);

    PageResult<SysLoginLogVO> page(LoginLogQuery query);
}
