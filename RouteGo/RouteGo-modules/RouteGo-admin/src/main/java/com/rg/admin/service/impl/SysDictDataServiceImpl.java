package com.rg.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rg.admin.dto.DictDataCreateRequest;
import com.rg.admin.dto.DictDataQuery;
import com.rg.admin.dto.DictDataUpdateRequest;
import com.rg.admin.entity.SysDictData;
import com.rg.admin.entity.SysDictType;
import com.rg.admin.mapper.SysDictDataMapper;
import com.rg.admin.mapper.SysDictTypeMapper;
import com.rg.admin.service.SysDictDataService;
import com.rg.admin.vo.DictDataVO;
import com.rg.common.cache.CacheHelper;
import com.rg.common.constant.CacheKeyConstants;
import com.rg.common.exception.ErrorCode;
import com.rg.common.exception.serviceException.AdminException;
import com.rg.common.web.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.List;

@Service
public class SysDictDataServiceImpl extends ServiceImpl<SysDictDataMapper, SysDictData>
        implements SysDictDataService {

    private static final Duration TTL = Duration.ofHours(1);

    private final SysDictDataMapper dataMapper;
    private final SysDictTypeMapper typeMapper;
    private final CacheHelper cacheHelper;

    public SysDictDataServiceImpl(SysDictDataMapper dataMapper, SysDictTypeMapper typeMapper,
                                  CacheHelper cacheHelper) {
        this.dataMapper = dataMapper;
        this.typeMapper = typeMapper;
        this.cacheHelper = cacheHelper;
    }

    @Override
    public PageResult<DictDataVO> page(DictDataQuery query) {
        LambdaQueryWrapper<SysDictData> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(query.getDictType())) {
            wrapper.eq(SysDictData::getDictType, query.getDictType());
        }
        if (StringUtils.hasText(query.getDictLabel())) {
            wrapper.like(SysDictData::getDictLabel, query.getDictLabel());
        }
        if (query.getStatus() != null) {
            wrapper.eq(SysDictData::getStatus, query.getStatus());
        }
        wrapper.orderByAsc(SysDictData::getSort).orderByAsc(SysDictData::getId);
        Page<SysDictData> page = dataMapper.selectPage(query.toPage(), wrapper);
        List<DictDataVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    @Override
    public List<DictDataVO> listByType(String dictType) {
        return cacheHelper.getOrLoad(String.format(CacheKeyConstants.Admin.DICT_DATA_BY_TYPE, dictType),
                TTL, () -> dataMapper.selectList(Wrappers.<SysDictData>lambdaQuery()
                                .eq(SysDictData::getDictType, dictType)
                                .eq(SysDictData::getStatus, 1)
                                .orderByAsc(SysDictData::getSort)
                                .orderByAsc(SysDictData::getId))
                        .stream().map(this::toVO).toList());
    }

    @Override
    public DictDataVO detail(Long id) {
        return toVO(requireData(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(DictDataCreateRequest request) {
        requireEnabledType(request.getDictType());
        if (dataMapper.countByTypeAndValueIncludingDeleted(request.getDictType(), request.getDictValue()) > 0) {
            throw new AdminException(ErrorCode.CONFLICT, "该字典值已存在（可能已删除）");
        }
        SysDictData data = new SysDictData();
        data.setDictType(request.getDictType());
        data.setDictLabel(request.getDictLabel());
        data.setDictValue(request.getDictValue());
        data.setSort(request.getSort() == null ? 0 : request.getSort());
        data.setCssClass(request.getCssClass());
        data.setIsDefault(request.getIsDefault() == null ? (byte) 0 : request.getIsDefault());
        data.setStatus(request.getStatus() == null ? (byte) 1 : request.getStatus());
        data.setRemark(request.getRemark());
        dataMapper.insert(data);
        evict(request.getDictType());
        return data.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, DictDataUpdateRequest request) {
        SysDictData existing = requireData(id);
        if (StringUtils.hasText(request.getDictValue())
                && !request.getDictValue().equals(existing.getDictValue())
                && dataMapper.countByTypeAndValueIncludingDeleted(existing.getDictType(),
                request.getDictValue()) > 0) {
            throw new AdminException(ErrorCode.CONFLICT, "该字典值已存在（可能已删除）");
        }
        SysDictData update = new SysDictData();
        update.setId(id);
        update.setDictLabel(request.getDictLabel());
        update.setDictValue(request.getDictValue());
        update.setSort(request.getSort());
        update.setCssClass(request.getCssClass());
        update.setIsDefault(request.getIsDefault());
        update.setStatus(request.getStatus());
        update.setRemark(request.getRemark());
        dataMapper.updateById(update);
        evict(existing.getDictType());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteData(Long id) {
        SysDictData data = requireData(id);
        dataMapper.deleteById(id);
        evict(data.getDictType());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Byte status) {
        SysDictData data = requireData(id);
        dataMapper.update(null, Wrappers.<SysDictData>lambdaUpdate()
                .set(SysDictData::getStatus, status).eq(SysDictData::getId, id));
        evict(data.getDictType());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long id) {
        SysDictData data = requireData(id);
        dataMapper.update(null, Wrappers.<SysDictData>lambdaUpdate()
                .set(SysDictData::getIsDefault, 0)
                .eq(SysDictData::getDictType, data.getDictType()));
        SysDictData update = new SysDictData();
        update.setId(id);
        update.setIsDefault((byte) 1);
        dataMapper.updateById(update);
        evict(data.getDictType());
    }

    private SysDictData requireData(Long id) {
        SysDictData data = dataMapper.selectById(id);
        if (data == null) {
            throw new AdminException(ErrorCode.NOT_FOUND, "字典数据不存在");
        }
        return data;
    }

    private void requireEnabledType(String dictType) {
        SysDictType type = typeMapper.selectOne(Wrappers.<SysDictType>lambdaQuery()
                .eq(SysDictType::getDictType, dictType).last("LIMIT 1"));
        if (type == null || type.getStatus() == null || type.getStatus() == 0) {
            throw new AdminException("字典类型不存在或已禁用");
        }
    }

    private void evict(String dictType) {
        cacheHelper.evictAfterCommit(String.format(CacheKeyConstants.Admin.DICT_DATA_BY_TYPE, dictType));
    }

    private DictDataVO toVO(SysDictData data) {
        DictDataVO vo = new DictDataVO();
        vo.setId(data.getId());
        vo.setDictType(data.getDictType());
        vo.setDictLabel(data.getDictLabel());
        vo.setDictValue(data.getDictValue());
        vo.setSort(data.getSort());
        vo.setCssClass(data.getCssClass());
        vo.setIsDefault(data.getIsDefault());
        vo.setStatus(data.getStatus());
        vo.setRemark(data.getRemark());
        return vo;
    }
}
