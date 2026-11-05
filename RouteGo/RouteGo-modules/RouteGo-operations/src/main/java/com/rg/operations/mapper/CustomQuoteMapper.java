package com.rg.operations.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rg.operations.entity.CustomQuote;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 定制报价 Mapper。
 * <p>
 * 报价表没有逻辑删除列，删除即物理删除；只有「待确认」的报价允许删，
 * 已采纳的报价是成交凭据必须保留，这一点由 Service 把关。
 */
public interface CustomQuoteMapper extends BaseMapper<CustomQuote> {

    @Select("SELECT COUNT(*) FROM custom_quote WHERE quote_no = #{quoteNo}")
    int countByQuoteNoIncludingDeleted(@Param("quoteNo") String quoteNo);

    @Select("SELECT COUNT(*) FROM custom_quote WHERE requirement_id = #{requirementId}")
    long countByRequirementId(@Param("requirementId") Long requirementId);

    /**
     * 需求单被删除时连带清理其报价版本。
     */
    @Delete("DELETE FROM custom_quote WHERE requirement_id = #{requirementId}")
    int deleteByRequirementId(@Param("requirementId") Long requirementId);
}
