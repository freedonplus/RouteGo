package com.rg.common.ip;

/**
 * IP 归属地查询客户端。
 */
public interface IpLocationClient {

    /**
     * 查询归属地，失败返回 {@code null}（调用方不得因归属地失败影响主流程）。
     */
    IpLocation lookup(String ip);
}
