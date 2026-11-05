package com.rg.operations.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.rg.common.annotation.BusinessType;
import com.rg.common.annotation.OperLog;
import com.rg.common.util.Result;
import com.rg.common.web.PageResult;
import com.rg.operations.dto.StatDailyQuery;
import com.rg.operations.dto.StatRebuildRequest;
import com.rg.operations.service.StatDailyService;
import com.rg.operations.vo.StatDailyVO;
import com.rg.operations.vo.StatSummaryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 每日经营统计。
 * <p>
 * 明细读 stat_daily（由凌晨定时任务生成），区间汇总与趋势直接聚合原始流水，
 * 因此「报表没跑」也不会让汇总数字错。
 * <p>
 * 权限码规范：{@code operations:StatDaily:<method>}。
 */
@Tag(name = "经营统计", description = "每日订单、支付、退款与新增会员统计")
@RestController
@RequestMapping("/operations/stat-daily")
public class StatDailyController {

    private final StatDailyService statDailyService;

    public StatDailyController(StatDailyService statDailyService) {
        this.statDailyService = statDailyService;
    }

    @Operation(summary = "统计分页", description = "按统计日期倒序，可按区间筛选")
    @SaCheckPermission("operations:StatDaily:page")
    @GetMapping("/page")
    public Result<PageResult<StatDailyVO>> page(@Valid StatDailyQuery query) {
        return Result.success(statDailyService.page(query));
    }

    @Operation(summary = "单日明细", description = "日期格式 yyyy-MM-dd；该日未生成统计时返回 404")
    @SaCheckPermission("operations:StatDaily:detail")
    @GetMapping("/{statDate}")
    public Result<StatDailyVO> detail(
            @Parameter(description = "统计日期 yyyy-MM-dd") @PathVariable
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate statDate) {
        return Result.success(statDailyService.detail(statDate));
    }

    @Operation(summary = "区间汇总", description = "直接聚合原始流水，不依赖 stat_daily 是否已重算")
    @SaCheckPermission("operations:StatDaily:summary")
    @GetMapping("/summary")
    public Result<StatSummaryVO> summary(
            @Parameter(description = "起始日期 yyyy-MM-dd") @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期 yyyy-MM-dd") @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(statDailyService.summary(startDate, endDate));
    }

    @Operation(summary = "趋势数据", description = "按日期升序的逐日记录，供折线图使用")
    @SaCheckPermission("operations:StatDaily:trend")
    @GetMapping("/trend")
    public Result<List<StatDailyVO>> trend(
            @Parameter(description = "起始日期 yyyy-MM-dd") @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期 yyyy-MM-dd") @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(statDailyService.trend(startDate, endDate));
    }

    @Operation(summary = "重算统计", description = "按天覆盖写入，幂等；单次最多 366 天。返回实际写入天数")
    @SaCheckPermission("operations:StatDaily:rebuild")
    @OperLog(title = "重算经营统计", type = BusinessType.UPDATE)
    @PostMapping("/rebuild")
    public Result<Integer> rebuild(@Valid @RequestBody StatRebuildRequest request) {
        return Result.success(statDailyService.rebuild(request));
    }
}
