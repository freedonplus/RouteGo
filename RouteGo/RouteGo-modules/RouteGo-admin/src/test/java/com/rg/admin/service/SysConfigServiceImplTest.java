package com.rg.admin.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.rg.admin.dto.ConfigUpdateRequest;
import com.rg.admin.entity.SysConfig;
import com.rg.admin.mapper.SysConfigMapper;
import com.rg.admin.service.impl.SysConfigServiceImpl;
import com.rg.common.cache.CacheHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysConfigServiceImplTest {

    @Mock
    private SysConfigMapper configMapper;
    @Mock
    private CacheHelper cacheHelper;

    private SysConfigServiceImpl service;

    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), SysConfig.class);
    }

    @BeforeEach
    void setUp() {
        service = new SysConfigServiceImpl(configMapper, cacheHelper);
    }

    @Test
    void configGetUsesCacheAndEvictsAfterUpdate() {
        when(cacheHelper.getOrLoad(org.mockito.ArgumentMatchers.eq("admin:SysConfig:getByKey:site.name"),
                org.mockito.ArgumentMatchers.eq(java.time.Duration.ofHours(1)),
                org.mockito.ArgumentMatchers.any())).thenReturn("RouteGo");
        when(configMapper.selectById(1L)).thenReturn(config(1L, "site.name", "RouteGo"));
        when(configMapper.updateById(any(SysConfig.class))).thenReturn(1);

        assertThat(service.getConfigValue("site.name")).isEqualTo("RouteGo");

        ConfigUpdateRequest request = new ConfigUpdateRequest();
        request.setConfigName("站点名称");
        request.setConfigValue("新名称");
        service.update(1L, request);

        verify(cacheHelper).evictAfterCommit("admin:SysConfig:getByKey:site.name");
    }

    private SysConfig config(Long id, String key, String value) {
        SysConfig config = new SysConfig();
        config.setId(id);
        config.setConfigName("站点名称");
        config.setConfigKey(key);
        config.setConfigValue(value);
        return config;
    }
}
