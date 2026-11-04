package com.rg.admin.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.rg.admin.config.StorageProperties;
import com.rg.admin.entity.SysFile;
import com.rg.admin.mapper.SysFileMapper;
import com.rg.admin.service.impl.SysFileServiceImpl;
import com.rg.admin.vo.SysFileVO;
import com.rg.common.cache.CacheHelper;
import com.rg.common.exception.serviceException.AdminException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysFileServiceImplTest {

    @Mock
    private SysFileMapper fileMapper;
    @Mock
    private CacheHelper cacheHelper;

    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), SysFile.class);
    }

    @Test
    void uploadRejectsDisallowedExtensionAndOversize(@TempDir Path tmp) {
        SysFileServiceImpl service = service(tmp);

        MockMultipartFile exe = new MockMultipartFile("file", "a.exe", null, new byte[]{1});
        assertThatThrownBy(() -> service.upload(exe)).hasMessageContaining("类型");

        MockMultipartFile big = new MockMultipartFile("file", "a.png", null, new byte[1024]);
        StorageProperties props = new StorageProperties();
        props.setLocalPath(tmp.toString());
        props.setMaxSize(10);
        SysFileServiceImpl smallLimit = new SysFileServiceImpl(fileMapper, props, cacheHelper);
        assertThatThrownBy(() -> smallLimit.upload(big)).hasMessageContaining("大小");
    }

    @Test
    void uploadStoresUnderDateFolderWithUuidName(@TempDir Path tmp) throws Exception {
        SysFileServiceImpl service = service(tmp);
        MockMultipartFile file = new MockMultipartFile("file", "照片.png", "image/png",
                new byte[]{1, 2, 3});
        when(fileMapper.insert(any(SysFile.class))).thenAnswer(invocation -> {
            ((SysFile) invocation.getArgument(0)).setId(7L);
            return 1;
        });

        SysFileVO vo = service.upload(file);

        assertThat(vo.getFileName()).endsWith(".png").doesNotContain("照片");
        assertThat(Files.exists(tmp.resolve(vo.getFilePath()))).isTrue();
        verify(fileMapper).updateById(any(SysFile.class));
    }

    @Test
    void deleteRemovesPhysicalFileBestEffort(@TempDir Path tmp) throws Exception {
        SysFileServiceImpl service = service(tmp);
        Path file = tmp.resolve("2026/11/04/a.png");
        Files.createDirectories(file.getParent());
        Files.write(file, new byte[]{1});
        SysFile record = new SysFile();
        record.setId(3L);
        record.setFilePath("2026/11/04/a.png");
        when(fileMapper.selectById(3L)).thenReturn(record);
        org.mockito.Mockito.lenient()
                .when(cacheHelper.getOrLoad(any(), any(), any()))
                .thenAnswer(invocation -> ((java.util.function.Supplier<?>) invocation.getArgument(2)).get());

        service.deleteFile(3L);

        assertThat(Files.exists(file)).isFalse();
        verify(fileMapper).deleteById(3L);
    }

    private SysFileServiceImpl service(Path tmp) {
        StorageProperties props = new StorageProperties();
        props.setLocalPath(tmp.toString());
        return new SysFileServiceImpl(fileMapper, props, cacheHelper);
    }
}
