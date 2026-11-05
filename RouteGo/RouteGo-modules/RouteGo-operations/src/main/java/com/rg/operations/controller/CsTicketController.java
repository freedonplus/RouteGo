package com.rg.operations.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.rg.common.annotation.BusinessType;
import com.rg.common.annotation.OperLog;
import com.rg.common.util.Result;
import com.rg.common.web.PageResult;
import com.rg.operations.dto.CsTicketCreateRequest;
import com.rg.operations.dto.CsTicketQuery;
import com.rg.operations.dto.CsTicketUpdateRequest;
import com.rg.operations.service.CsTicketService;
import com.rg.operations.vo.CsTicketVO;
import com.rg.operations.vo.TicketStatisticsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 客服工单管理。
 * <p>
 * 状态流转单独开接口（{@code PUT /{id}/status}），与普通资料编辑区分开，
 * 这样操作日志里能清楚看到「谁在什么时候把工单从处理中改成已解决」。
 * <p>
 * 权限码规范：{@code operations:CsTicket:<method>}。
 */
@Tag(name = "客服工单", description = "工单受理、指派、状态流转与看板统计")
@RestController
@RequestMapping("/operations/cs-ticket")
public class CsTicketController {

    private final CsTicketService ticketService;

    public CsTicketController(CsTicketService ticketService) {
        this.ticketService = ticketService;
    }

    @Operation(summary = "工单分页", description = "支持工单号、标题关键词、客户、订单、类型、优先级、状态与处理人筛选")
    @SaCheckPermission("operations:CsTicket:page")
    @GetMapping("/page")
    public Result<PageResult<CsTicketVO>> page(@Valid CsTicketQuery query) {
        return Result.success(ticketService.page(query));
    }

    @Operation(summary = "工单详情", description = "附带该工单的消息条数；完整对话走消息接口")
    @SaCheckPermission("operations:CsTicket:detail")
    @GetMapping("/{id}")
    public Result<CsTicketVO> detail(@Parameter(description = "工单 ID") @PathVariable Long id) {
        return Result.success(ticketService.detail(id));
    }

    @Operation(summary = "新建工单", description = "传了订单会自动带出下单会员；指派了处理人则直接进入处理中")
    @SaCheckPermission("operations:CsTicket:create")
    @OperLog(title = "新建客服工单", type = BusinessType.INSERT)
    @PostMapping
    public Result<Long> create(@Valid @RequestBody CsTicketCreateRequest request) {
        return Result.success(ticketService.create(request));
    }

    @Operation(summary = "编辑工单", description = "可改关联客户/订单、类型、标题与描述；已关闭工单不可编辑")
    @SaCheckPermission("operations:CsTicket:update")
    @OperLog(title = "编辑客服工单", type = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CsTicketUpdateRequest request) {
        ticketService.update(id, request);
        return Result.success();
    }

    @Operation(summary = "指派处理人", description = "待处理工单被指派后自动进入处理中；已关闭工单不可改派")
    @SaCheckPermission("operations:CsTicket:assign")
    @OperLog(title = "指派客服工单", type = BusinessType.UPDATE)
    @PutMapping("/{id}/assign")
    public Result<Void> assign(@PathVariable Long id,
                               @Parameter(description = "处理人（后台用户 ID）") @RequestParam Long handlerId) {
        ticketService.assign(id, handlerId);
        return Result.success();
    }

    @Operation(summary = "调整优先级", description = "1 低、2 中、3 高")
    @SaCheckPermission("operations:CsTicket:updatePriority")
    @OperLog(title = "调整工单优先级", type = BusinessType.UPDATE)
    @PutMapping("/{id}/priority")
    public Result<Void> updatePriority(@PathVariable Long id,
                                       @Parameter(description = "优先级 1~3") @RequestParam Byte priority) {
        ticketService.updatePriority(id, priority);
        return Result.success();
    }

    @Operation(summary = "状态流转",
            description = "0 待处理、1 处理中、2 已解决、3 已关闭；只允许状态机定义的路径，备注会写入工单消息")
    @SaCheckPermission("operations:CsTicket:updateStatus")
    @OperLog(title = "工单状态流转", type = BusinessType.UPDATE)
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @Parameter(description = "目标状态 0~3") @RequestParam Byte status,
                                     @Parameter(description = "变更备注，可空") @RequestParam(required = false) String remark) {
        ticketService.updateStatus(id, status, remark);
        return Result.success();
    }

    @Operation(summary = "删除工单", description = "逻辑删除工单并清理其对话消息，不可恢复")
    @SaCheckPermission("operations:CsTicket:delete")
    @OperLog(title = "删除客服工单", type = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        ticketService.delete(id);
        return Result.success();
    }

    @Operation(summary = "工单看板统计", description = "按状态与类型计数，附未指派数与高优先级待处理数")
    @SaCheckPermission("operations:CsTicket:statistics")
    @GetMapping("/statistics")
    public Result<TicketStatisticsVO> statistics() {
        return Result.success(ticketService.statistics());
    }
}
