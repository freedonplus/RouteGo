package com.rg.operations.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rg.operations.entity.CustomRequirement;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 定制需求 Mapper。
 * <p>
 * 需求单号唯一索引不含逻辑删除条件，占用校验同样要带上已删除记录。
 */
public interface CustomRequirementMapper extends BaseMapper<CustomRequirement> {

    @Select("SELECT COUNT(*) FROM custom_requirement WHERE requirement_no = #{requirementNo}")
    int countByRequirementNoIncludingDeleted(@Param("requirementNo") String requirementNo);
}
