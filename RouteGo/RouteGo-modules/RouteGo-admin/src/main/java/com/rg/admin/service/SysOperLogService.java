package com.rg.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rg.admin.dto.OperLogQuery;
import com.rg.admin.entity.SysOperLog;
import com.rg.admin.vo.SysOperLogVO;
import com.rg.common.web.PageResult;

public interface SysOperLogService extends IService<SysOperLog> {

    PageResult<SysOperLogVO> page(OperLogQuery query);

    SysOperLogVO detail(Long id);
}
