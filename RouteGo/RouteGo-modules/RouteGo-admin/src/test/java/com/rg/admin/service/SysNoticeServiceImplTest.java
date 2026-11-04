package com.rg.admin.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.rg.admin.entity.SysNotice;
import com.rg.admin.mapper.SysNoticeMapper;
import com.rg.admin.service.impl.SysNoticeServiceImpl;
import com.rg.common.cache.CacheHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysNoticeServiceImplTest {

    @Mock
    private SysNoticeMapper noticeMapper;
    @Mock
    private CacheHelper cacheHelper;

    private SysNoticeServiceImpl service;

    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), SysNotice.class);
    }

    @BeforeEach
    void setUp() {
        service = new SysNoticeServiceImpl(noticeMapper, cacheHelper);
    }

    @Test
    void publishSetsTimeAndEvictsPublishedCache() {
        SysNotice notice = new SysNotice();
        notice.setId(5L);
        notice.setTitle("公告");
        when(noticeMapper.selectById(5L)).thenReturn(notice);
        when(noticeMapper.updateById(any(SysNotice.class))).thenReturn(1);


        service.publish(5L);

        verify(noticeMapper).updateById(
                org.mockito.ArgumentMatchers.<SysNotice>argThat(
                        n -> n.getStatus() == 1 && n.getPublishTime() != null));
        verify(cacheHelper).evictAfterCommit("admin:SysNotice:listPublished");
    }
}
