package com.rg.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rg.admin.dto.ConfigBatchRequest;
import com.rg.admin.dto.ConfigCreateRequest;
import com.rg.admin.dto.ConfigQuery;
import com.rg.admin.dto.ConfigUpdateRequest;
import com.rg.admin.entity.SysConfig;
import com.rg.admin.mapper.SysConfigMapper;
import com.rg.admin.service.SysConfigService;
import com.rg.admin.vo.SysConfigVO;
import com.rg.common.cache.CacheHelper;
import com.rg.common.constant.CacheKeyConstants;
import com.rg.common.exception.ErrorCode;
import com.rg.common.exception.serviceException.AdminException;
import com.rg.common.web.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig>
        implements SysConfigService {

    private static final Duration TTL = Duration.ofHours(1);

    private final SysConfigMapper configMapper;
    private final CacheHelper cacheHelper;

    public SysConfigServiceImpl(SysConfigMapper configMapper, CacheHelper cacheHelper) {
        this.configMapper = configMapper;
        this.cacheHelper = cacheHelper;
    }

    @Override
    public PageResult<SysConfigVO> page(ConfigQuery query) {
        LambdaQueryWrapper<SysConfig> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(query.getConfigName())) {
            wrapper.like(SysConfig::getConfigName, query.getConfigName());
        }
        if (StringUtils.hasText(query.getConfigKey())) {
            wrapper.like(SysConfig::getConfigKey, query.getConfigKey());
        }
        wrapper.orderByDesc(SysConfig::getId);
        Page<SysConfig> page = configMapper.selectPage(query.toPage(), wrapper);
        List<SysConfigVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    @Override
    public SysConfigVO detail(Long id) {
        SysConfig config = configMapper.selectById(id);
        if (config == null) {
            throw new AdminException(ErrorCode.NOT_FOUND, "参数不存在");
        }
        return toVO(config);
    }

    @Override
    public SysConfigVO getByKey(String configKey) {
        SysConfig config = configMapper.selectOne(Wrappers.<SysConfig>lambdaQuery()
                .eq(SysConfig::getConfigKey, configKey).last("LIMIT 1"));
        if (config == null) {
            throw new AdminException(ErrorCode.NOT_FOUND, "参数不存在");
        }
        SysConfigVO vo = toVO(config);
        vo.setConfigValue(getConfigValue(configKey));
        return vo;
    }

    @Override
    public String getConfigValue(String configKey) {
        return cacheHelper.getOrLoad(String.format(CacheKeyConstants.Admin.CONFIG_BY_KEY, configKey),
                TTL, () -> {
                    SysConfig config = configMapper.selectOne(Wrappers.<SysConfig>lambdaQuery()
                            .eq(SysConfig::getConfigKey, configKey).last("LIMIT 1"));
                    return config == null ? null : config.getConfigValue();
                });
    }

    @Override
    @Transactional
    public Long create(ConfigCreateRequest request) {
        if (configMapper.countByConfigKeyIncludingDeleted(request.getConfigKey()) > 0) {
            throw new AdminException(ErrorCode.CONFLICT, "参数键已被使用（可能已删除）");
        }
        SysConfig config = new SysConfig();
        config.setConfigName(request.getConfigName());
        config.setConfigKey(request.getConfigKey());
        config.setConfigValue(request.getConfigValue());
        config.setRemark(request.getRemark());
        configMapper.insert(config);
        return config.getId();
    }

    @Override
    @Transactional
    public void update(Long id, ConfigUpdateRequest request) {
        SysConfig existing = configMapper.selectById(id);
        if (existing == null) {
            throw new AdminException(ErrorCode.NOT_FOUND, "参数不存在");
        }
        SysConfig update = new SysConfig();
        update.setId(id);
        update.setConfigName(request.getConfigName());
        update.setConfigValue(request.getConfigValue());
        update.setRemark(request.getRemark());
        configMapper.updateById(update);
        cacheHelper.evictAfterCommit(
                String.format(CacheKeyConstants.Admin.CONFIG_BY_KEY, existing.getConfigKey()));
    }

    @Override
    @Transactional
    public void deleteConfig(Long id) {
        SysConfig existing = configMapper.selectById(id);
        if (existing == null) {
            throw new AdminException(ErrorCode.NOT_FOUND, "参数不存在");
        }
        configMapper.deleteById(id);
        cacheHelper.evictAfterCommit(
                String.format(CacheKeyConstants.Admin.CONFIG_BY_KEY, existing.getConfigKey()));
    }

    @Override
    @Transactional
    public void batchUpdate(ConfigBatchRequest request) {
        List<String> keys = new ArrayList<>();
        for (ConfigBatchRequest.Item item : request.getItems()) {
            SysConfig existing = configMapper.selectOne(Wrappers.<SysConfig>lambdaQuery()
                    .eq(SysConfig::getConfigKey, item.getConfigKey()).last("LIMIT 1"));
            if (existing == null) {
                continue;
            }
            SysConfig update = new SysConfig();
            update.setId(existing.getId());
            update.setConfigValue(item.getConfigValue());
            configMapper.updateById(update);
            keys.add(String.format(CacheKeyConstants.Admin.CONFIG_BY_KEY, existing.getConfigKey()));
        }
        cacheHelper.evictAfterCommit(keys.toArray(String[]::new));
    }

    private SysConfigVO toVO(SysConfig config) {
        SysConfigVO vo = new SysConfigVO();
        vo.setId(config.getId());
        vo.setConfigName(config.getConfigName());
        vo.setConfigKey(config.getConfigKey());
        vo.setConfigValue(config.getConfigValue());
        vo.setRemark(config.getRemark());
        vo.setCreateTime(config.getCreateTime());
        return vo;
    }
}
