package com.rg.catalog.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rg.catalog.dto.RouteImageSaveRequest;
import com.rg.catalog.entity.Route;
import com.rg.catalog.entity.RouteImage;
import com.rg.catalog.mapper.RouteImageMapper;
import com.rg.catalog.mapper.RouteMapper;
import com.rg.catalog.service.RouteImageService;
import com.rg.catalog.vo.RouteImageVO;
import com.rg.common.exception.ErrorCode;
import com.rg.common.exception.serviceException.CatalogException;
import com.rg.common.storage.TempUrlSigner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 线路图片业务实现。
 */
@Service
public class RouteImageServiceImpl extends ServiceImpl<RouteImageMapper, RouteImage>
        implements RouteImageService {

    private final RouteImageMapper imageMapper;
    private final RouteMapper routeMapper;
    private final TempUrlSigner tempUrlSigner;

    public RouteImageServiceImpl(RouteImageMapper imageMapper, RouteMapper routeMapper,
                                 TempUrlSigner tempUrlSigner) {
        this.imageMapper = imageMapper;
        this.routeMapper = routeMapper;
        this.tempUrlSigner = tempUrlSigner;
    }

    @Override
    public List<RouteImageVO> listByRoute(Long routeId) {
        return imageMapper.selectList(Wrappers.<RouteImage>lambdaQuery()
                        .eq(RouteImage::getRouteId, routeId)
                        .orderByAsc(RouteImage::getSort)
                        .orderByAsc(RouteImage::getId)).stream()
                .map(this::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(RouteImageSaveRequest request) {
        Route route = request.getRouteId() == null ? null : routeMapper.selectById(request.getRouteId());
        if (route == null) {
            throw new CatalogException(ErrorCode.NOT_FOUND, "线路不存在");
        }
        RouteImage image = new RouteImage();
        image.setRouteId(request.getRouteId());
        // 还原为标准下载地址：临时签名链接有过期时间，不能落库
        image.setImageUrl(tempUrlSigner.canonicalize(request.getImageUrl().trim()));
        image.setSort(request.getSort() == null ? 0 : request.getSort());
        imageMapper.insert(image);
        return image.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteImage(Long id) {
        RouteImage image = id == null ? null : imageMapper.selectById(id);
        if (image == null) {
            throw new CatalogException(ErrorCode.NOT_FOUND, "图片不存在");
        }
        imageMapper.deleteById(id);
    }

    private RouteImageVO toVO(RouteImage image) {
        RouteImageVO vo = new RouteImageVO();
        vo.setId(image.getId());
        vo.setRouteId(image.getRouteId());
        vo.setImageUrl(image.getImageUrl());
        vo.setSort(image.getSort());
        vo.setCreateTime(image.getCreateTime());
        return vo;
    }
}
