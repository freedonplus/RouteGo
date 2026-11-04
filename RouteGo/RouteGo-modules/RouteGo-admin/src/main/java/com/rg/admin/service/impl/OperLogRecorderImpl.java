package com.rg.admin.service.impl;

import com.rg.admin.entity.SysOperLog;
import com.rg.admin.mapper.SysOperLogMapper;
import com.rg.common.log.OperLogRecord;
import com.rg.common.log.OperLogRecorder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 操作日志异步落库。
 */
@Slf4j
@Component
public class OperLogRecorderImpl implements OperLogRecorder {

    private final SysOperLogMapper operLogMapper;

    public OperLogRecorderImpl(SysOperLogMapper operLogMapper) {
        this.operLogMapper = operLogMapper;
    }

    @Async
    @Override
    public void record(OperLogRecord record) {
        try {
            SysOperLog entity = new SysOperLog();
            entity.setTitle(record.getTitle());
            entity.setBusinessType(record.getBusinessType() == null
                    ? 6 : (byte) record.getBusinessType().getCode());
            entity.setMethod(record.getMethod());
            entity.setRequestMethod(record.getRequestMethod());
            entity.setOperName(record.getOperName());
            entity.setOperUrl(record.getOperUrl());
            entity.setOperIp(record.getOperIp());
            entity.setOperParam(record.getOperParam());
            entity.setJsonResult(record.getJsonResult());
            entity.setStatus(record.getStatus() == null ? (byte) 1 : record.getStatus().byteValue());
            entity.setErrorMsg(record.getErrorMsg());
            entity.setCostTime(record.getCostTime());
            entity.setOperTime(record.getOperTime());
            operLogMapper.insert(entity);
        } catch (RuntimeException e) {
            log.warn("操作日志写入失败: {}", e.getMessage());
        }
    }
}
