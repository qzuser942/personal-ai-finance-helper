package com.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.finance.entity.Budget;
import org.apache.ibatis.annotations.Mapper;

/**
 * 月度预算 Mapper
 *
 * @author 胡宪棋
 */
@Mapper
public interface BudgetMapper extends BaseMapper<Budget> {
}
