package com.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.finance.entity.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 分类 Mapper
 *
 * @author 胡宪棋
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {

    /**
     * 统计某分类下的账单数
     */
    @Select("SELECT COUNT(*) FROM bill WHERE category_id = #{categoryId} AND is_deleted = 0")
    Long countBillsByCategory(@Param("categoryId") Long categoryId);
}
