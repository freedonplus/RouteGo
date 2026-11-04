package com.rg.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rg.admin.dto.ConfigBatchRequest;
import com.rg.admin.dto.ConfigCreateRequest;
import com.rg.admin.dto.ConfigQuery;
import com.rg.admin.dto.ConfigUpdateRequest;
import com.rg.admin.entity.SysConfig;
import com.rg.admin.vo.SysConfigVO;
import com.rg.common.web.PageResult;

public interface SysConfigService extends IService<SysConfig> {

    PageResult<SysConfigVO> page(ConfigQuery query);

    SysConfigVO detail(Long id);

    SysConfigVO getByKey(String configKey);

    String getConfigValue(String configKey);

    Long create(ConfigCreateRequest request);

    void update(Long id, ConfigUpdateRequest request);

    void deleteConfig(Long id);

    void batchUpdate(ConfigBatchRequest request);
}
