package com.rg.operations.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.rg.common.annotation.BusinessType;
import com.rg.common.annotation.OperLog;
import com.rg.common.util.Result;
import com.rg.common.web.PageResult;
import com.rg.operations.dto.CsMessageQuery;
import com.rg.operations.dto.CsMessageSendRequest;
import com.rg.operations.service.CsMessageService;
import com.rg.operations.vo.CsMessageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 工单消息。
 * <p>
 * 后台只能以「客服」身份发言；客户侧消息由小程序写入，后台只读。
 * 消息不可编辑、不可删除，属于沟通留痕。
 * <p>
 * 权限码规范：{@code operations:CsMessage:<method>}。
 */
@Tag(name = "工单消息", description = "工单对话的查询与客服回复")
@RestController
@RequestMapping("/operations/cs-message")
public class CsMessageController {

    private final CsMessageService messageService;

    public CsMessageController(CsMessageService messageService) {
        this.messageService = messageService;
    }

    @Operation(summary = "消息分页", description = "按工单、发送方与时间范围查询，倒序返回")
    @SaCheckPermission("operations:CsMessage:page")
    @GetMapping("/page")
    public Result<PageResult<CsMessageVO>> page(@Valid CsMessageQuery query) {
        return Result.success(messageService.page(query));
    }

    @Operation(summary = "工单对话", description = "某工单的完整消息，按时间正序，供详情页时间线渲染")
    @SaCheckPermission("operations:CsMessage:listByTicket")
    @GetMapping("/list")
    public Result<List<CsMessageVO>> listByTicket(
            @Parameter(description = "工单 ID") @RequestParam Long ticketId) {
        return Result.success(messageService.listByTicket(ticketId));
    }

    @Operation(summary = "回复工单", description = "以当前登录客服身份发送；待处理工单会在回复后自动转为处理中")
    @SaCheckPermission("operations:CsMessage:create")
    @OperLog(title = "回复客服工单", type = BusinessType.INSERT)
    @PostMapping
    public Result<Long> send(@Valid @RequestBody CsMessageSendRequest request) {
        return Result.success(messageService.send(request));
    }
}
