package com.rg.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rg.admin.dto.DictTypeCreateRequest;
import com.rg.admin.dto.DictTypeQuery;
import com.rg.admin.dto.DictTypeUpdateRequest;
import com.rg.admin.entity.SysDictData;
import com.rg.admin.entity.SysDictType;
import com.rg.admin.mapper.SysDictDataMapper;
import com.rg.admin.mapper.SysDictTypeMapper;
import com.rg.admin.service.SysDictTypeService;
import com.rg.admin.vo.DictTypeVO;
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
public class SysDictTypeServiceImpl extends ServiceImpl<SysDictTypeMapper, SysDictType>
        implements SysDictTypeService {

    private static final Duration TTL = Duration.ofHours(1);

    private final SysDictTypeMapper typeMapper;
    private final SysDictDataMapper dataMapper;
    private final CacheHelper cacheHelper;

    public SysDictTypeServiceImpl(SysDictTypeMapper typeMapper, SysDictDataMapper dataMapper,
                                  CacheHelper cacheHelper) {
        this.typeMapper = typeMapper;
        this.dataMapper = dataMapper;
        this.cacheHelper = cacheHelper;
    }

    @Override
    public PageResult<DictTypeVO> page(DictTypeQuery query) {
        LambdaQueryWrapper<SysDictType> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(query.getDictName())) {
            wrapper.like(SysDictType::getDictName, query.getDictName());
        }
        if (StringUtils.hasText(query.getDictType())) {
            wrapper.like(SysDictType::getDictType, query.getDictType());
        }
        if (query.getStatus() != null) {
            wrapper.eq(SysDictType::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(SysDictType::getId);
        Page<SysDictType> page = typeMapper.selectPage(query.toPage(), wrapper);
        List<DictTypeVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    @Override
    public DictTypeVO detail(Long id) {
        SysDictType type = typeMapper.selectById(id);
        if (type == null) {
            throw new AdminException(ErrorCode.NOT_FOUND, "字典类型不存在");
        }
        return toVO(type);
    }

    @Override
    @Transactional
    public Long create(DictTypeCreateRequest request) {
        if (typeMapper.countByDictTypeIncludingDeleted(request.getDictType()) > 0) {
            throw new AdminException(ErrorCode.CONFLICT, "字典类型编码已被使用（可能已删除）");
        }
        SysDictType type = new SysDictType();
        type.setDictName(request.getDictName());
        type.setDictType(request.getDictType());
        type.setStatus(request.getStatus() == null ? (byte) 1 : request.getStatus());
        type.setRemark(request.getRemark());
        typeMapper.insert(type);
        return type.getId();
    }

    @Override
    @Transactional
    public void update(Long id, DictTypeUpdateRequest request) {
        SysDictType existing = typeMapper.selectById(id);
        if (existing == null) {
            throw new AdminException(ErrorCode.NOT_FOUND, "字典类型不存在");
        }
        SysDictType update = new SysDictType();
        update.setId(id);
        update.setDictName(request.getDictName());
        update.setStatus(request.getStatus());
        update.setRemark(request.getRemark());
        typeMapper.updateById(update);
        cacheHelper.evictAfterCommit(String.format(CacheKeyConstants.Admin.DICT_TYPE_BY_TYPE,
                existing.getDictType()));
    }

    @Override
    @Transactional
    public void deleteType(Long id) {
        SysDictType type = typeMapper.selectById(id);
        if (type == null) {
            throw new AdminException(ErrorCode.NOT_FOUND, "字典类型不存在");
        }
        Long count = dataMapper.selectCount(Wrappers.<SysDictData>lambdaQuery()
                .eq(SysDictData::getDictType, type.getDictType()));
        if (count != null && count > 0) {
            throw new AdminException(ErrorCode.CONFLICT, "请先删除该类型下的字典数据");
        }
        typeMapper.deleteById(id);
        cacheHelper.evictAfterCommit(
                String.format(CacheKeyConstants.Admin.DICT_TYPE_BY_TYPE, type.getDictType()),
                String.format(CacheKeyConstants.Admin.DICT_DATA_BY_TYPE, type.getDictType()));
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Byte status) {
        SysDictType type = typeMapper.selectById(id);
        if (type == null) {
            throw new AdminException(ErrorCode.NOT_FOUND, "字典类型不存在");
        }
        typeMapper.update(null, Wrappers.<SysDictType>lambdaUpdate()
                .set(SysDictType::getStatus, status).eq(SysDictType::getId, id));
        cacheHelper.evictAfterCommit(String.format(CacheKeyConstants.Admin.DICT_TYPE_BY_TYPE,
                type.getDictType()));
    }

    @Override
    public DictTypeVO getByType(String dictType) {
        return cacheHelper.getOrLoad(String.format(CacheKeyConstants.Admin.DICT_TYPE_BY_TYPE, dictType),
                TTL, () -> {
                    SysDictType type = typeMapper.selectOne(Wrappers.<SysDictType>lambdaQuery()
                            .eq(SysDictType::getDictType, dictType).last("LIMIT 1"));
                    return type == null ? null : toVO(type);
                });
    }

    private DictTypeVO toVO(SysDictType type) {
        DictTypeVO vo = new DictTypeVO();
        vo.setId(type.getId());
        vo.setDictName(type.getDictName());
        vo.setDictType(type.getDictType());
        vo.setStatus(type.getStatus());
        vo.setRemark(type.getRemark());
        vo.setCreateTime(type.getCreateTime());
        return vo;
    }
}
