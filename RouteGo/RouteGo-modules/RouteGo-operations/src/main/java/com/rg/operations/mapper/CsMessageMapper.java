package com.rg.operations.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rg.operations.entity.CsMessage;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 工单消息 Mapper。
 * <p>
 * 消息是追加型数据，没有逻辑删除列：工单删除时连物理删除一起清掉，
 * 避免留下永远查不到的孤儿记录。
 */
public interface CsMessageMapper extends BaseMapper<CsMessage> {

    @Select("SELECT COUNT(*) FROM cs_message WHERE ticket_id = #{ticketId}")
    long countByTicketId(@Param("ticketId") Long ticketId);

    /**
     * 物理删除某工单下的全部消息。
     */
    @Delete("DELETE FROM cs_message WHERE ticket_id = #{ticketId}")
    int deleteByTicketId(@Param("ticketId") Long ticketId);
}
