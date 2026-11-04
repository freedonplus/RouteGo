package com.rg.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rg.admin.dto.DictTypeCreateRequest;
import com.rg.admin.dto.DictTypeQuery;
import com.rg.admin.dto.DictTypeUpdateRequest;
import com.rg.admin.entity.SysDictType;
import com.rg.admin.vo.DictTypeVO;
import com.rg.common.web.PageResult;

public interface SysDictTypeService extends IService<SysDictType> {

    PageResult<DictTypeVO> page(DictTypeQuery query);

    DictTypeVO detail(Long id);

    Long create(DictTypeCreateRequest request);

    void update(Long id, DictTypeUpdateRequest request);

    void deleteType(Long id);

    void updateStatus(Long id, Byte status);

    DictTypeVO getByType(String dictType);
}
