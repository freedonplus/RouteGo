package com.rg.admin.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.rg.admin.entity.SysDictData;
import com.rg.admin.entity.SysDictType;
import com.rg.admin.mapper.SysDictDataMapper;
import com.rg.admin.mapper.SysDictTypeMapper;
import com.rg.admin.service.impl.SysDictDataServiceImpl;
import com.rg.admin.service.impl.SysDictTypeServiceImpl;
import com.rg.admin.vo.DictDataVO;
import com.rg.common.cache.CacheHelper;
import com.rg.common.exception.serviceException.AdminException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysDictServiceImplTest {

    @Mock
    private SysDictTypeMapper dictTypeMapper;
    @Mock
    private SysDictDataMapper dictDataMapper;
    @Mock
    private CacheHelper cacheHelper;

    private SysDictTypeServiceImpl typeService;
    private SysDictDataServiceImpl dataService;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, SysDictType.class);
        TableInfoHelper.initTableInfo(assistant, SysDictData.class);
    }

    @BeforeEach
    void setUp() {
        typeService = new SysDictTypeServiceImpl(dictTypeMapper, dictDataMapper, cacheHelper);
        dataService = new SysDictDataServiceImpl(dictDataMapper, dictTypeMapper, cacheHelper);
        lenient().when(cacheHelper.getOrLoad(any(), any(), any()))
                .thenAnswer(invocation -> ((java.util.function.Supplier<?>) invocation.getArgument(2)).get());
    }

    @Test
    void dictTypeWithDataCannotBeDeleted() {
        when(dictTypeMapper.selectById(1L)).thenReturn(dictType(1L, "sys_status"));
        when(dictDataMapper.selectCount(any())).thenReturn(2L);

        assertThatThrownBy(() -> typeService.deleteType(1L))
                .isInstanceOf(AdminException.class)
                .hasMessageContaining("字典数据");
    }

    @Test
    void settingDefaultClearsOtherDefaultsAndEvictsCache() {
        when(dictDataMapper.selectById(9L)).thenReturn(dictData(9L, "sys_status"));
        when(dictDataMapper.updateById(any(SysDictData.class))).thenReturn(1);

        dataService.setDefault(9L);

        verify(dictDataMapper).update(any(), any());
        verify(cacheHelper).evictAfterCommit("admin:SysDictData:listByType:sys_status");
    }

    @Test
    void listByTypeUsesCacheKeyAndSortsBySort() {
        when(dictDataMapper.selectList(any())).thenReturn(List.of(dictData(1L, "sys_status")));

        List<DictDataVO> result = dataService.listByType("sys_status");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDictValue()).isEqualTo("1");
    }

    private SysDictType dictType(Long id, String type) {
        SysDictType dictType = new SysDictType();
        dictType.setId(id);
        dictType.setDictName("状态");
        dictType.setDictType(type);
        dictType.setStatus((byte) 1);
        return dictType;
    }

    private SysDictData dictData(Long id, String type) {
        SysDictData data = new SysDictData();
        data.setId(id);
        data.setDictType(type);
        data.setDictLabel("启用");
        data.setDictValue("1");
        data.setSort(0);
        data.setIsDefault((byte) 0);
        data.setStatus((byte) 1);
        return data;
    }
}
