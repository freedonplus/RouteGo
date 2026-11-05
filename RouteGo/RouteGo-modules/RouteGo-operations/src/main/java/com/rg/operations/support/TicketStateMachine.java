package com.rg.operations.support;

import com.rg.common.exception.ErrorCode;
import com.rg.common.exception.serviceException.OperationsException;
import com.rg.operations.enums.OperationsDict;

/**
 * 客服工单状态机。
 * <pre>
 *   0 待处理 ──指派/回复──▶ 1 处理中 ──解决──▶ 2 已解决 ──归档──▶ 3 已关闭
 *      └──────────── 直接解决 ─────────────▶ 2
 *      └──────────── 直接关闭 ─────────────▶ 3
 *                                           2 ──重新打开──▶ 1
 * </pre>
 * 约定：
 * <ul>
 *   <li>不允许「原地流转」（把处理中改成处理中），这类请求一律 409，避免刷出无意义的状态变更。</li>
 *   <li>已关闭是终态，不能改回任何状态；需要继续跟进请新建工单。</li>
 *   <li>进入「已解决」或「已关闭」时由 Service 补 finish_time。</li>
 * </ul>
 */
public final class TicketStateMachine {

    public static final byte PENDING = 0;
    public static final byte PROCESSING = 1;
    public static final byte RESOLVED = 2;
    public static final byte CLOSED = 3;

    private TicketStateMachine() {
    }

    /**
     * 判断流转是否合法。
     */
    public static boolean canTransit(byte from, byte to) {
        if (from == to) {
            return false;
        }
        return switch (from) {
            case PENDING -> to == PROCESSING || to == RESOLVED || to == CLOSED;
            case PROCESSING -> to == RESOLVED || to == CLOSED;
            case RESOLVED -> to == PROCESSING || to == CLOSED;
            default -> false;
        };
    }

    /**
     * 校验流转，非法时抛 409。
     */
    public static void assertTransit(byte from, byte to) {
        if (from == to) {
            throw new OperationsException(ErrorCode.CONFLICT,
                    "工单已处于「" + describe(to) + "」状态，无需重复操作");
        }
        if (!canTransit(from, to)) {
            throw new OperationsException(ErrorCode.CONFLICT,
                    "工单不能从「" + describe(from) + "」变更为「" + describe(to) + "」");
        }
    }

    /**
     * 是否为终态（已关闭）。
     */
    public static boolean isTerminal(byte status) {
        return status == CLOSED;
    }

    private static String describe(byte status) {
        String desc = OperationsDict.TICKET_STATUS.get((int) status);
        return desc == null ? "未知(" + status + ")" : desc;
    }
}
