package com.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.finance.entity.AiAnalysisRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * AI分析记录 Mapper
 *
 * @author 胡宪棋
 */
@Mapper
public interface AiAnalysisRecordMapper extends BaseMapper<AiAnalysisRecord> {

    @Select("SELECT * FROM ai_analysis_record WHERE user_id = #{userId} AND is_deleted = 0 " +
            "ORDER BY created_at DESC LIMIT #{limit}")
    List<AiAnalysisRecord> findRecentByUserId(@Param("userId") Long userId, @Param("limit") int limit);
}
