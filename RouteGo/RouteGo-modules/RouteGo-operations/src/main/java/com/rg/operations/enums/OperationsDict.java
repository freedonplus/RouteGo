package com.rg.operations.enums;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 运营域字典：把 tinyint 状态翻译成中文，接口出口统一带上 *Desc 字段。
 * <p>
 * 用 {@link LinkedHashMap} 而不是 {@code Map.of}：后台下拉框与统计图例直接按这里的
 * 声明顺序渲染，{@code Map.of} 的迭代顺序不保证稳定，会让前端图例每次刷新都在跳。
 * <p>
 * 这些值同时是数据库注释与前端下拉框的取值来源，改动时三处要一起改：
 * 建表脚本注释、本类、{@code sys_dict_data}（若有对应字典）。
 */
public final class OperationsDict {

    private OperationsDict() {
    }

    /** 工单类型：1 售前，2 订单，3 退款，4 投诉，5 其他 */
    public static final Map<Integer, String> TICKET_TYPE = ordered(
            1, "售前咨询",
            2, "订单问题",
            3, "退款售后",
            4, "投诉建议",
            5, "其他");

    /** 工单状态：0 待处理，1 处理中，2 已解决，3 已关闭 */
    public static final Map<Integer, String> TICKET_STATUS = ordered(
            0, "待处理",
            1, "处理中",
            2, "已解决",
            3, "已关闭");

    /** 工单优先级：1 低，2 中，3 高 */
    public static final Map<Integer, String> TICKET_PRIORITY = ordered(
            1, "低",
            2, "中",
            3, "高");

    /** 工单消息发送方：1 客户，2 系统用户 */
    public static final Map<Integer, String> MESSAGE_SENDER_TYPE = ordered(
            1, "客户",
            2, "客服");

    /** 自动分配策略：1 轮询，2 当前在处理最少 */
    public static final Map<Integer, String> ASSIGN_STRATEGY = ordered(
            1, "轮询",
            2, "在处理最少");

    /** 定制需求状态：0 待跟进，1 已报价，2 已成交，3 已关闭 */
    public static final Map<Integer, String> REQUIREMENT_STATUS = ordered(
            0, "待跟进",
            1, "已报价",
            2, "已成交",
            3, "已关闭");

    /** 定制报价状态：0 待确认，1 已采纳，2 已失效 */
    public static final Map<Integer, String> QUOTE_STATUS = ordered(
            0, "待确认",
            1, "已采纳",
            2, "已失效");

    public static String of(Map<Integer, String> dict, Byte value) {
        if (value == null) {
            return null;
        }
        return dict.get(value.intValue());
    }

    private static Map<Integer, String> ordered(Object... keyValues) {
        Map<Integer, String> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            map.put((Integer) keyValues[i], (String) keyValues[i + 1]);
        }
        return Collections.unmodifiableMap(map);
    }
}
