package com.rg.operations.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rg.operations.entity.CsTicket;
import com.rg.operations.mapper.model.GroupCount;
import com.rg.operations.mapper.model.HandlerLoad;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * 客服工单 Mapper。
 * <p>
 * 工单号唯一索引不含逻辑删除标记，所以「已删除工单占用的号」也算冲突：
 * 校验用不带 deleted 条件的 COUNT，避免删除后重新生成同一个号撞唯一索引。
 */
public interface CsTicketMapper extends BaseMapper<CsTicket> {

    /**
     * 工单号是否已被占用（含已逻辑删除的记录）。
     */
    @Select("SELECT COUNT(*) FROM cs_ticket WHERE ticket_no = #{ticketNo}")
    int countByTicketNoIncludingDeleted(@Param("ticketNo") String ticketNo);

    @Select("""
            SELECT status AS group_key, COUNT(*) AS total
            FROM cs_ticket
            WHERE deleted = 0
            GROUP BY status
            """)
    List<GroupCount> countByStatus();

    @Select("""
            SELECT type AS group_key, COUNT(*) AS total
            FROM cs_ticket
            WHERE deleted = 0
            GROUP BY type
            """)
    List<GroupCount> countByType();

    /**
     * 未指派且待处理的工单数：客服组长最关心的「没人管」队列。
     */
    @Select("""
            SELECT COUNT(*) FROM cs_ticket
            WHERE deleted = 0 AND handler_id IS NULL AND status = 0
            """)
    long countUnhandled();

    /**
     * 待处理中的高优先级工单数。
     */
    @Select("""
            SELECT COUNT(*) FROM cs_ticket
            WHERE deleted = 0 AND status = 0 AND priority = 3
            """)
    long countUrgentPending();

    /**
     * 客服未读总数：列表页轮询它来决定是否响铃。
     */
    @Select("""
            SELECT COALESCE(SUM(staff_unread), 0) FROM cs_ticket
            WHERE deleted = 0 AND staff_unread > 0
            """)
    long countStaffUnread();

    /**
     * 指定客服各自「处理中」的工单数，用于自动分配的负载均衡。
     * <p>
     * 只统计 status = 1：待处理还没人干活，已解决/已关闭不再占人。
     */
    @Select("""
            <script>
            SELECT handler_id AS handlerId, COUNT(*) AS total
            FROM cs_ticket
            WHERE deleted = 0 AND status = 1 AND handler_id IN
            <foreach collection="handlerIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            GROUP BY handler_id
            </script>
            """)
    List<HandlerLoad> countProcessingByHandlers(@Param("handlerIds") Collection<Long> handlerIds);
}
