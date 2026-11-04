package com.rg.common.aspect;

import cn.dev33.satoken.stp.StpUtil;
import com.rg.common.annotation.OperLog;
import com.rg.common.log.LogSanitizer;
import com.rg.common.log.OperLogRecord;
import com.rg.common.log.OperLogRecorder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 采集 {@link OperLog} 标注接口的操作信息，交给 {@link OperLogRecorder} 异步落库。
 */
@Slf4j
@Aspect
@Component
public class OperLogAspect {

    private static final int MAX_TEXT_LENGTH = 2000;

    private final ObjectProvider<OperLogRecorder> recorderProvider;

    public OperLogAspect(ObjectProvider<OperLogRecorder> recorderProvider) {
        this.recorderProvider = recorderProvider;
    }

    @Around("@annotation(operLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperLog operLog) throws Throwable {
        long start = System.currentTimeMillis();
        OperLogRecord record = new OperLogRecord();
        record.setTitle(operLog.title());
        record.setBusinessType(operLog.type());
        record.setMethod(joinPoint.getSignature().toLongString());
        record.setOperTime(LocalDateTime.now());
        fillRequest(record);
        fillOperator(record);

        try {
            Object result = joinPoint.proceed();
            record.setStatus(1);
            record.setJsonResult(LogSanitizer.toJson(result, MAX_TEXT_LENGTH));
            return result;
        } catch (Throwable e) {
            record.setStatus(0);
            record.setErrorMsg(LogSanitizer.maskAndTruncate(e.getMessage(), 500));
            throw e;
        } finally {
            record.setCostTime(System.currentTimeMillis() - start);
            record.setOperParam(LogSanitizer.toJson(joinPoint.getArgs(), MAX_TEXT_LENGTH));
            persist(record);
        }
    }

    private void fillRequest(OperLogRecord record) {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            record.setRequestMethod(request.getMethod());
            record.setOperUrl(request.getRequestURI());
            record.setOperIp(request.getRemoteAddr());
        }
    }

    private void fillOperator(OperLogRecord record) {
        try {
            if (StpUtil.isLogin()) {
                record.setOperId(StpUtil.getLoginIdAsLong());
                Object username = StpUtil.getSession().get("username");
                record.setOperName(username == null ? null : String.valueOf(username));
            }
        } catch (RuntimeException ignored) {
            // 无 Sa-Token 上下文（定时任务/内部调用）时留空
        }
    }

    private void persist(OperLogRecord record) {
        try {
            recorderProvider.ifAvailable(recorder -> recorder.record(record));
        } catch (RuntimeException e) {
            log.warn("操作日志落库失败: {}", e.getMessage());
        }
    }
}
