package com.rg.common.log;

/**
 * 操作日志落库 SPI，由业务模块提供实现。
 */
public interface OperLogRecorder {

    void record(OperLogRecord record);
}
