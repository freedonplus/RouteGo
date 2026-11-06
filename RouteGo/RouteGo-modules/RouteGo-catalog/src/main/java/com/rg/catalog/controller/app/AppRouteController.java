package com.rg.catalog.controller.app;

import com.rg.catalog.dto.RouteQuery;
import com.rg.catalog.service.RouteCategoryService;
import com.rg.catalog.service.RouteDepartureService;
import com.rg.catalog.service.RouteService;
import com.rg.catalog.vo.RouteCategoryVO;
import com.rg.catalog.vo.RouteDepartureVO;
import com.rg.catalog.vo.RouteVO;
import com.rg.common.util.Result;
import com.rg.common.storage.TempUrlSigner;
import com.rg.common.web.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * C 端线路浏览（只读）。
 * <p>
 * 后台的 {@code /catalog/**} 带 {@code @SaCheckPermission}，App 用户没有后台角色，
 * 不能直接复用；这里以 {@code /app/**} 暴露只读视图，逻辑仍走同一套 service。
 * 写操作不在此暴露，后台接口保持原有的权限校验。
 */
@Tag(name = "App-线路浏览", description = "C 端只读：分类 / 线路列表 / 详情 / 班期")
@RestController
@RequestMapping("/app")
public class AppRouteController {

    private final RouteService routeService;
    private final RouteDepartureService routeDepartureService;
    private final RouteCategoryService routeCategoryService;
    private final TempUrlSigner tempUrlSigner;

    public AppRouteController(RouteService routeService,
                              RouteDepartureService routeDepartureService,
                              RouteCategoryService routeCategoryService,
                              TempUrlSigner tempUrlSigner) {
        this.routeService = routeService;
        this.routeDepartureService = routeDepartureService;
        this.routeCategoryService = routeCategoryService;
        this.tempUrlSigner = tempUrlSigner;
    }

    @Operation(summary = "线路分页", description = "只返回已上架线路；支持目的地、出发地、分类、关键字筛选。")
    @GetMapping("/routes")
    public Result<PageResult<RouteVO>> routes(@Valid RouteQuery query, HttpServletRequest request) {
        // C 端固定只看上架内容，忽略客户端传入的状态，避免读到下架线路
        query.setStatus((byte) 1);
        PageResult<RouteVO> page = routeService.page(query);
        if (page != null && page.getRecords() != null) {
            page.getRecords().forEach(vo -> normalizeImageUrls(vo, request));
        }
        return Result.success(page);
    }

    @Operation(summary = "线路详情", description = "含分类名称、标签、图片与每日行程。")
    @GetMapping("/routes/{id}")
    public Result<RouteVO> routeDetail(@Parameter(description = "线路 ID") @PathVariable Long id,
                                       HttpServletRequest request) {
        RouteVO vo = routeService.detail(id);
        normalizeImageUrls(vo, request);
        return Result.success(vo);
    }

    @Operation(summary = "可售班期", description = "按出发日期升序返回该线路的班期与余位。")
    @GetMapping("/routes/{id}/departures")
    public Result<List<RouteDepartureVO>> departures(@Parameter(description = "线路 ID") @PathVariable Long id) {
        return Result.success(routeDepartureService.listByRoute(id));
    }

    @Operation(summary = "线路分类", description = "树形分类，用于首页与列表页筛选。")
    @GetMapping("/categories")
    public Result<List<RouteCategoryVO>> categories() {
        return Result.success(routeCategoryService.tree());
    }

    /**
     * 把数据库里的图片地址补成当前请求可直接访问的绝对地址。
     * <p>
     * 两类来源分开处理：
     * <ul>
     *   <li>上传文件（{@code /admin/system/file/{id}/download}）：该地址要求登录态，
     *       {@code <img>} 标签带不上 token 会 401——这里换算成带 HMAC 签名和过期时间的
     *       临时预览链接（{@code /app/file/preview}），公开可访问，过期自动失效。</li>
     *   <li>随包静态资源（如 {@code /covers/rg0.png}）：本身公开，只补主机名。</li>
     * </ul>
     * 数据库刻意不写死主机名：同一份数据，H5 拿到的地址是 127.0.0.1，
     * 真机拿到的是局域网 IP，换环境不需要改数据。
     */
    private void normalizeImageUrls(RouteVO vo, HttpServletRequest request) {
        if (vo == null) {
            return;
        }
        vo.setCoverImage(absoluteUrl(vo.getCoverImage(), request));
        if (vo.getImages() != null) {
            vo.getImages().forEach(img -> img.setImageUrl(absoluteUrl(img.getImageUrl(), request)));
        }
    }

    private String absoluteUrl(String url, HttpServletRequest request) {
        if (url == null || url.isEmpty()) {
            return url;
        }
        // 已是绝对地址（以后接 MinIO 等外链）原样返回
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        if (!url.startsWith("/")) {
            return url;
        }
        // 上传文件 → 临时签名预览链接；其余（/covers/** 等静态资源）保持原路径
        Long fileId = TempUrlSigner.extractFileId(url);
        String resolved = fileId != null ? tempUrlSigner.build(fileId) : url;
        return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort()
                + request.getContextPath() + resolved;
    }
}
