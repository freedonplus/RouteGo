package com.rg.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rg.admin.dto.DictDataCreateRequest;
import com.rg.admin.dto.DictDataQuery;
import com.rg.admin.dto.DictDataUpdateRequest;
import com.rg.admin.entity.SysDictData;
import com.rg.admin.vo.DictDataVO;
import com.rg.common.web.PageResult;

import java.util.List;

public interface SysDictDataService extends IService<SysDictData> {

    PageResult<DictDataVO> page(DictDataQuery query);

    List<DictDataVO> listByType(String dictType);

    DictDataVO detail(Long id);

    Long create(DictDataCreateRequest request);

    void update(Long id, DictDataUpdateRequest request);

    void deleteData(Long id);

    void updateStatus(Long id, Byte status);

    void setDefault(Long id);
}
