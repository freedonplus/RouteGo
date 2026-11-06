package com.rg.operations.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.rg.common.constant.CacheKeyConstants;
import com.rg.common.exception.ErrorCode;
import com.rg.common.exception.serviceException.OperationsException;
import com.rg.common.redis.RedisService;
import com.rg.common.util.Result;
import com.rg.operations.support.CsStreamHub;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 客服消息推送（SSE）。
 * <p>
 * 为什么不直接建连：{@code EventSource} 无法携带自定义请求头，带不上
 * {@code Authorization: Bearer}。所以先用正常登录态换一张 60 秒有效、用后即删的票据，
 * 再用票据建连。票据走 query，因此本接口放行给拦截器白名单，
 * 由方法内部自行验票——这与「凭据即凭证」的验证码入口是同一套思路。
 * <p>
 * 一次性票据配合前端短路重连：客户端断开后主动换新票重连，而不是让 EventSource
 * 拿着废票自动重试（那会变成 401 死循环）。
 */
@Tag(name = "客服推送", description = "客服工作台的实时消息通道")
@RestController
@RequestMapping("/operations/cs")
public class CsStreamController {

    /** 票据有效期：只覆盖「换票 → 建连」这一步，不需要更长 */
    private static final Duration TICKET_TTL = Duration.ofSeconds(60);

    private final CsStreamHub hub;
    private final RedisService redisService;

    public CsStreamController(CsStreamHub hub, RedisService redisService) {
        this.hub = hub;
        this.redisService = redisService;
    }

    @Operation(summary = "申请推送票据", description = "用登录态换一张 60 秒内有效、仅可使用一次的建连票据")
    @SaCheckPermission("operations:CsTicket:page")
    @PostMapping("/stream-ticket")
    public Result<Map<String, Object>> issueTicket() {
        Long operatorId = StpUtil.getLoginIdAsLong();
        String ticket = UUID.randomUUID().toString().replace("-", "");
        redisService.set(CacheKeyConstants.Operations.CS_STREAM_TICKET.formatted(ticket),
                operatorId, TICKET_TTL);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("ticket", ticket);
        data.put("expiresIn", TICKET_TTL.toSeconds());
        return Result.success(data);
    }

    @Operation(summary = "建立推送连接",
            description = "text/event-stream 长连接；建连即推 hello（含当前未读总数），"
                    + "之后有新消息推 cs 事件，每 20 秒一次心跳注释帧")
    @GetMapping("/stream")
    public SseEmitter stream(
            @Parameter(description = "申请到的建连票据") @RequestParam String ticket,
            HttpServletResponse response) {
        // 不要直接声明成 Long：Redis 里的数字反序列化回来可能是 Integer，
        // 泛型擦除后这一步会抛 ClassCastException。先接 Object 再按数值解析
        Object raw = redisService.getAndDelete(
                CacheKeyConstants.Operations.CS_STREAM_TICKET.formatted(ticket));
        if (raw == null) {
            // 票据无效、过期或已被使用。前端收到 401 会重新换票再连
            throw new OperationsException(ErrorCode.UNAUTHORIZED, "推送票据无效或已使用");
        }
        Long operatorId = Long.parseLong(raw.toString());
        // nginx 默认会把响应攒在缓冲区里一起发，SSE 就成了「攒够一个包才到」。
        // 这个头让 nginx 对本次响应关闭缓冲，是 SSE 部署最常见的坑
        response.setHeader("X-Accel-Buffering", "no");
        // 流式响应不能被任何中间层缓存，否则第二条连接可能读到上一份
        response.setHeader("Cache-Control", "no-cache");
        return hub.open(operatorId);
    }
}
