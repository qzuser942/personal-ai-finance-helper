package com.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.finance.entity.AiConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * AI配置 Mapper
 *
 * @author 胡宪棋
 */
@Mapper
public interface AiConfigMapper extends BaseMapper<AiConfig> {

    @Select("SELECT config_value FROM ai_config WHERE config_key = #{configKey} AND is_deleted = 0")
    String findValueByKey(@Param("configKey") String configKey);
}
