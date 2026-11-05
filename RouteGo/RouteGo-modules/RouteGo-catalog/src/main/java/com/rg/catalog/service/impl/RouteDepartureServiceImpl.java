package com.rg.catalog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rg.catalog.dto.RouteDepartureQuery;
import com.rg.catalog.dto.RouteDepartureSaveRequest;
import com.rg.catalog.entity.Route;
import com.rg.catalog.entity.RouteDeparture;
import com.rg.catalog.mapper.RouteDepartureMapper;
import com.rg.catalog.mapper.RouteMapper;
import com.rg.catalog.service.RouteDepartureService;
import com.rg.catalog.vo.RouteDepartureVO;
import com.rg.common.exception.ErrorCode;
import com.rg.common.exception.serviceException.CatalogException;
import com.rg.common.web.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 出发班期业务实现。
 */
@Service
public class RouteDepartureServiceImpl extends ServiceImpl<RouteDepartureMapper, RouteDeparture>
        implements RouteDepartureService {

    private final RouteDepartureMapper departureMapper;
    private final RouteMapper routeMapper;

    public RouteDepartureServiceImpl(RouteDepartureMapper departureMapper, RouteMapper routeMapper) {
        this.departureMapper = departureMapper;
        this.routeMapper = routeMapper;
    }

    @Override
    public PageResult<RouteDepartureVO> page(RouteDepartureQuery query) {
        LambdaQueryWrapper<RouteDeparture> wrapper = Wrappers.lambdaQuery();
        if (query.getRouteId() != null) {
            wrapper.eq(RouteDeparture::getRouteId, query.getRouteId());
        }
        if (query.getStatus() != null) {
            wrapper.eq(RouteDeparture::getStatus, query.getStatus());
        }
        if (query.getStartDate() != null) {
            wrapper.ge(RouteDeparture::getDepartDate, query.getStartDate());
        }
        if (query.getEndDate() != null) {
            wrapper.le(RouteDeparture::getDepartDate, query.getEndDate());
        }
        wrapper.orderByAsc(RouteDeparture::getDepartDate);
        Page<RouteDeparture> page = departureMapper.selectPage(query.toPage(), wrapper);
        List<RouteDepartureVO> records = page.getRecords().stream().map(this::toVO).toList();

        Set<Long> routeIds = page.getRecords().stream()
                .map(RouteDeparture::getRouteId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (!routeIds.isEmpty()) {
            Map<Long, String> titleById = routeMapper.selectBatchIds(routeIds).stream()
                    .collect(Collectors.toMap(Route::getId, Route::getTitle));
            records.forEach(vo -> vo.setRouteTitle(titleById.get(vo.getRouteId())));
        }
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    @Override
    public List<RouteDepartureVO> listByRoute(Long routeId) {
        return departureMapper.selectList(Wrappers.<RouteDeparture>lambdaQuery()
                        .eq(RouteDeparture::getRouteId, routeId)
                        .orderByAsc(RouteDeparture::getDepartDate)).stream()
                .map(this::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(RouteDepartureSaveRequest request) {
        requireRoute(request.getRouteId());
        if (departureMapper.countByRouteAndDateIncludingDeleted(
                request.getRouteId(), request.getDepartDate()) > 0) {
            throw new CatalogException(ErrorCode.CONFLICT, "该线路在此日期已有班期");
        }
        RouteDeparture departure = new RouteDeparture();
        departure.setRouteId(request.getRouteId());
        departure.setDepartDate(request.getDepartDate());
        departure.setAdultPrice(request.getAdultPrice());
        departure.setChildPrice(request.getChildPrice());
        departure.setStock(request.getStock());
        departure.setSold(0);
        departure.setStatus(request.getStatus() == null ? (byte) 1 : request.getStatus());
        departure.setVersion(0);
        departureMapper.insert(departure);
        return departure.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, RouteDepartureSaveRequest request) {
        RouteDeparture existing = requireDeparture(id);
        if (departureMapper.countByRouteAndDateExcludingId(
                existing.getRouteId(), request.getDepartDate(), id) > 0) {
            throw new CatalogException(ErrorCode.CONFLICT, "该线路在此日期已有班期");
        }
        if (request.getStock() != null && existing.getSold() != null && request.getStock() < existing.getSold()) {
            throw new CatalogException(ErrorCode.BAD_REQUEST,
                    "库存不能小于已售数量（当前已售 " + existing.getSold() + "）");
        }

        RouteDeparture update = new RouteDeparture();
        update.setId(existing.getId());
        update.setVersion(existing.getVersion());
        update.setDepartDate(request.getDepartDate());
        update.setAdultPrice(request.getAdultPrice());
        update.setChildPrice(request.getChildPrice());
        update.setStock(request.getStock());
        if (request.getStatus() != null) {
            update.setStatus(request.getStatus());
        }
        if (departureMapper.updateById(update) == 0) {
            throw new CatalogException(ErrorCode.CONFLICT, "数据已被修改，请刷新后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Byte status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new CatalogException(ErrorCode.BAD_REQUEST, "状态只能是 0（停售）或 1（可售）");
        }
        RouteDeparture existing = requireDeparture(id);
        RouteDeparture update = new RouteDeparture();
        update.setId(existing.getId());
        update.setVersion(existing.getVersion());
        update.setStatus(status);
        if (departureMapper.updateById(update) == 0) {
            throw new CatalogException(ErrorCode.CONFLICT, "数据已被修改，请刷新后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDeparture(Long id) {
        RouteDeparture existing = requireDeparture(id);
        if (existing.getSold() != null && existing.getSold() > 0) {
            throw new CatalogException(ErrorCode.CONFLICT, "该班期已有成交，不能删除，请改为停售");
        }
        // 唯一索引不含逻辑删除列，这里物理删除以便重新创建同一天的班期
        departureMapper.deletePhysicallyById(id);
    }

    private void requireRoute(Long routeId) {
        Route route = routeId == null ? null : routeMapper.selectById(routeId);
        if (route == null) {
            throw new CatalogException(ErrorCode.NOT_FOUND, "线路不存在");
        }
    }

    private RouteDeparture requireDeparture(Long id) {
        RouteDeparture departure = id == null ? null : departureMapper.selectById(id);
        if (departure == null) {
            throw new CatalogException(ErrorCode.NOT_FOUND, "班期不存在");
        }
        return departure;
    }

    private RouteDepartureVO toVO(RouteDeparture departure) {
        RouteDepartureVO vo = new RouteDepartureVO();
        vo.setId(departure.getId());
        vo.setRouteId(departure.getRouteId());
        vo.setDepartDate(departure.getDepartDate());
        vo.setAdultPrice(departure.getAdultPrice());
        vo.setChildPrice(departure.getChildPrice());
        vo.setStock(departure.getStock());
        vo.setSold(departure.getSold());
        int stock = departure.getStock() == null ? 0 : departure.getStock();
        int sold = departure.getSold() == null ? 0 : departure.getSold();
        vo.setRemaining(Math.max(stock - sold, 0));
        vo.setStatus(departure.getStatus());
        vo.setVersion(departure.getVersion());
        vo.setCreateTime(departure.getCreateTime());
        return vo;
    }
}
