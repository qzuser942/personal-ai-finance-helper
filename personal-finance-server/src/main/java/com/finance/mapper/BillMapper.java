package com.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.finance.entity.Bill;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 账单 Mapper
 *
 * @author 胡宪棋
 */
@Mapper
public interface BillMapper extends BaseMapper<Bill> {

    @Select("SELECT * FROM bill WHERE sync_uuid = #{syncUuid} AND is_deleted = 0")
    Bill findBySyncUuid(@Param("syncUuid") String syncUuid);

    /**
     * 按月份统计用户收支
     */
    @Select("SELECT " +
            "COALESCE(SUM(CASE WHEN type = 'income' THEN amount ELSE 0 END), 0) AS totalIncome, " +
            "COALESCE(SUM(CASE WHEN type = 'expense' THEN amount ELSE 0 END), 0) AS totalExpense, " +
            "COUNT(*) AS billCount " +
            "FROM bill " +
            "WHERE user_id = #{userId} AND is_deleted = 0 " +
            "AND DATE_FORMAT(consume_time, '%Y-%m') = #{yearMonth}")
    Map<String, Object> monthlyStats(@Param("userId") Long userId, @Param("yearMonth") String yearMonth);

    /**
     * 按分类统计支出（用于饼图）
     */
    @Select("SELECT b.category_id AS categoryId, c.name AS categoryName, c.icon, " +
            "COALESCE(SUM(b.amount), 0) AS totalAmount, COUNT(*) AS count " +
            "FROM bill b LEFT JOIN category c ON b.category_id = c.id " +
            "WHERE b.user_id = #{userId} AND b.type = 'expense' AND b.is_deleted = 0 " +
            "AND DATE_FORMAT(b.consume_time, '%Y-%m') = #{yearMonth} " +
            "GROUP BY b.category_id, c.name, c.icon ORDER BY totalAmount DESC")
    List<Map<String, Object>> categoryBreakdown(@Param("userId") Long userId, @Param("yearMonth") String yearMonth);

    /**
     * 按日统计收支趋势
     */
    @Select("SELECT DATE_FORMAT(consume_time, '%Y-%m-%d') AS date, " +
            "COALESCE(SUM(CASE WHEN type = 'income' THEN amount ELSE 0 END), 0) AS income, " +
            "COALESCE(SUM(CASE WHEN type = 'expense' THEN amount ELSE 0 END), 0) AS expense " +
            "FROM bill WHERE user_id = #{userId} AND is_deleted = 0 " +
            "AND DATE_FORMAT(consume_time, '%Y-%m') = #{yearMonth} " +
            "GROUP BY DATE_FORMAT(consume_time, '%Y-%m-%d') ORDER BY date")
    List<Map<String, Object>> dailyBreakdown(@Param("userId") Long userId, @Param("yearMonth") String yearMonth);

    /**
     * 管理员仪表盘统计
     */
    @Select("SELECT COUNT(*) FROM bill WHERE is_deleted = 0 " +
            "AND DATE_FORMAT(consume_time, '%Y-%m') = #{yearMonth}")
    Long countMonthlyBills(@Param("yearMonth") String yearMonth);

    @Select("SELECT COUNT(DISTINCT user_id) FROM bill WHERE is_deleted = 0 " +
            "AND DATE_FORMAT(consume_time, '%Y-%m') = #{yearMonth}")
    Long countMonthlyActiveUsers(@Param("yearMonth") String yearMonth);

    @Select("SELECT COALESCE(SUM(amount), 0) FROM bill WHERE is_deleted = 0 " +
            "AND DATE_FORMAT(consume_time, '%Y-%m') = #{yearMonth}")
    BigDecimal sumMonthlyAmount(@Param("yearMonth") String yearMonth);

    /**
     * 近6个月趋势
     */
    @Select("SELECT DATE_FORMAT(consume_time, '%Y-%m') AS yearMonth, " +
            "COUNT(*) AS billCount, COUNT(DISTINCT user_id) AS userCount, " +
            "COALESCE(SUM(amount), 0) AS totalAmount " +
            "FROM bill WHERE is_deleted = 0 " +
            "AND consume_time >= DATE_SUB(NOW(), INTERVAL 6 MONTH) " +
            "GROUP BY DATE_FORMAT(consume_time, '%Y-%m') ORDER BY yearMonth")
    List<Map<String, Object>> recent6MonthTrend();

    /**
     * 全平台用户消费排名
     */
    @Select("SELECT b.user_id AS userId, u.username, " +
            "SUM(b.amount) AS totalAmount, COUNT(*) AS billCount " +
            "FROM bill b LEFT JOIN sys_user u ON b.user_id = u.id " +
            "WHERE b.is_deleted = 0 AND DATE_FORMAT(b.consume_time, '%Y-%m') = #{yearMonth} " +
            "GROUP BY b.user_id, u.username ORDER BY totalAmount DESC LIMIT 20")
    List<Map<String, Object>> userRanking(@Param("yearMonth") String yearMonth);

    /**
     * 全平台分类分布
     */
    @Select("SELECT c.name AS categoryName, COALESCE(SUM(b.amount), 0) AS totalAmount " +
            "FROM bill b LEFT JOIN category c ON b.category_id = c.id " +
            "WHERE b.is_deleted = 0 AND b.type = 'expense' " +
            "AND DATE_FORMAT(b.consume_time, '%Y-%m') = #{yearMonth} " +
            "GROUP BY c.name ORDER BY totalAmount DESC")
    List<Map<String, Object>> categoryDistribution(@Param("yearMonth") String yearMonth);
}
