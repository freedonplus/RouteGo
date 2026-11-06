package com.rg.operations.controller.app;

import com.rg.common.security.StpKit;
import com.rg.common.util.Result;
import com.rg.common.web.PageResult;
import com.rg.operations.dto.CsCustomerMessageSendRequest;
import com.rg.operations.dto.CsCustomerTicketCreateRequest;
import com.rg.operations.service.CsChatService;
import com.rg.operations.vo.CsMessageVO;
import com.rg.operations.vo.CsSessionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * C 端在线客服。
 * <p>
 * 客户与客服围绕一张工单对话：客户可发起咨询、追加文字或图片、结束会话；
 * 客服在后台「客服工单」里看到同一条时间线并回复。
 * <p>
 * {@code /app/**} 在登录白名单内，注解式鉴权不生效，因此每个方法都显式
 * {@link StpKit#CUSTOMER} 校验会员登录，客户 ID 只从登录态取。
 */
@Tag(name = "App-在线客服", description = "发起咨询、会话列表、对话与发图")
@RestController
@RequestMapping("/app/cs")
public class AppCsController {

    private final CsChatService chatService;

    public AppCsController(CsChatService chatService) {
        this.chatService = chatService;
    }

    @Operation(summary = "发起咨询", description = "文字与图片至少一个；返回工单 ID")
    @PostMapping("/tickets")
    public Result<Long> create(@Valid @RequestBody CsCustomerTicketCreateRequest request) {
        StpKit.CUSTOMER.checkLogin();
        return Result.success(chatService.create(StpKit.currentCustomerId(), request));
    }

    @Operation(summary = "我的会话", description = "按最后一条消息时间倒序；status 不传表示全部")
    @GetMapping("/tickets")
    public Result<PageResult<CsSessionVO>> myTickets(
            @Parameter(description = "状态：0 待处理，1 处理中，2 已解决，3 已关闭")
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "20") long pageSize) {
        StpKit.CUSTOMER.checkLogin();
        return Result.success(chatService.myTickets(StpKit.currentCustomerId(), status, pageNum, pageSize));
    }

    @Operation(summary = "会话详情")
    @GetMapping("/tickets/{id}")
    public Result<CsSessionVO> detail(@PathVariable Long id) {
        StpKit.CUSTOMER.checkLogin();
        return Result.success(chatService.detail(StpKit.currentCustomerId(), id));
    }

    @Operation(summary = "会话消息", description = "正序返回；读取后客户未读清零")
    @GetMapping("/tickets/{id}/messages")
    public Result<List<CsMessageVO>> messages(@PathVariable Long id) {
        StpKit.CUSTOMER.checkLogin();
        return Result.success(chatService.messages(StpKit.currentCustomerId(), id));
    }

    @Operation(summary = "发送消息", description = "文字与图片至少一个；已解决的会话会自动重新打开")
    @PostMapping("/messages")
    public Result<Long> send(@Valid @RequestBody CsCustomerMessageSendRequest request) {
        StpKit.CUSTOMER.checkLogin();
        return Result.success(chatService.send(StpKit.currentCustomerId(), request));
    }

    @Operation(summary = "结束会话", description = "置为已关闭，之后只能新建会话")
    @PostMapping("/tickets/{id}/close")
    public Result<Void> close(@PathVariable Long id) {
        StpKit.CUSTOMER.checkLogin();
        chatService.close(StpKit.currentCustomerId(), id);
        return Result.success();
    }

    @Operation(summary = "未读消息数", description = "客服回复累加，进入会话清零")
    @GetMapping("/unread")
    public Result<Long> unread() {
        StpKit.CUSTOMER.checkLogin();
        return Result.success(chatService.unread(StpKit.currentCustomerId()));
    }
}
