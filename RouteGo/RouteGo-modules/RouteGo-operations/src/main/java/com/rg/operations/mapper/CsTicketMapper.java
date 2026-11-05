package com.rg.operations.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rg.operations.entity.CsTicket;
import com.rg.operations.mapper.model.GroupCount;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
}
