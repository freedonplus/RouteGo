package com.rg.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rg.admin.dto.FileQuery;
import com.rg.admin.entity.SysFile;
import com.rg.admin.vo.SysFileVO;
import com.rg.common.web.PageResult;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface SysFileService extends IService<SysFile> {

    PageResult<SysFileVO> page(FileQuery query);

    SysFileVO detail(Long id);

    SysFileVO upload(MultipartFile file);

    Resource download(Long id);

    void deleteFile(Long id);
}
