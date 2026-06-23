package com.finance.ai.converter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * 账单数据 → Markdown 结构化表格转换器
 * 将数据库查询结果转换为标准Markdown格式，送入大模型分析
 *
 * 设计原则：
 * - 实例化组件（可测试、可注入）
 * - 所有输入为强类型/标准集合，输出为完整Markdown字符串
 * - 空值安全处理，不会因数据缺失抛异常
 *
 * @author 胡宪棋
 */
@Slf4j
@Component
public class BillMarkdownConverter {

    /**
     * 构建完整的月度诊断Markdown文档
     *
     * @param yearMonth         分析月份（YYYY-MM）
     * @param monthlyStats      月度统计数据（totalIncome, totalExpense, billCount）
     * @param categoryBreakdown 分类支出汇总（categoryName, totalAmount, percentage）
     * @param allBills          全部账单明细（consumeTime, amount, categoryName, remark, type）
     * @param historicalProfile 用户历史消费画像文本（来自Qdrant向量检索），可为null
     * @return 完整的Markdown文档
     */
    public String buildDiagnosticMarkdown(
            String yearMonth,
            Map<String, Object> monthlyStats,
            List<Map<String, Object>> categoryBreakdown,
            List<Map<String, Object>> allBills,
            String historicalProfile) {

        StringBuilder md = new StringBuilder();

        // === 用户历史消费画像 ===
        if (historicalProfile != null && !historicalProfile.isBlank()) {
            md.append("## 用户历史消费画像\n\n");
            md.append(historicalProfile).append("\n\n");
            md.append("---\n\n");
        }

        // === 月度收支概况 ===
        md.append("## 月度收支概况\n\n");

        BigDecimal totalIncome = safeBigDecimal(monthlyStats, "totalIncome");
        BigDecimal totalExpense = safeBigDecimal(monthlyStats, "totalExpense");
        BigDecimal balance = totalIncome.subtract(totalExpense);

        md.append("| 指标 | 金额 |\n");
        md.append("|------|------|\n");
        md.append("| 统计月份 | ").append(yearMonth).append(" |\n");
        md.append("| 总收入 | ¥").append(formatMoney(totalIncome)).append(" |\n");
        md.append("| 总支出 | ¥").append(formatMoney(totalExpense)).append(" |\n");
        md.append("| 月度结余 | ¥").append(formatMoney(balance)).append(" |\n");
        if (totalIncome.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal savingRate = balance.divide(totalIncome, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            md.append("| 储蓄率 | ").append(formatMoney(savingRate)).append("% |\n");
        }
        md.append("\n");

        // === 支出明细表 ===
        md.append("## 支出明细\n\n");
        List<Map<String, Object>> expenses = allBills != null
                ? allBills.stream().filter(b -> "expense".equals(b.get("type"))).toList()
                : List.of();
        if (!expenses.isEmpty()) {
            md.append("| 消费时间 | 金额 | 分类 | 备注 |\n");
            md.append("|----------|------|------|------|\n");
            for (Map<String, Object> bill : expenses) {
                md.append("| ").append(safeStr(bill, "consumeTime"))
                        .append(" | ¥").append(safeStr(bill, "amount"))
                        .append(" | ").append(safeStr(bill, "categoryName"))
                        .append(" | ").append(safeStr(bill, "remark"))
                        .append(" |\n");
            }
        } else {
            md.append("> 本月无支出记录\n");
        }
        md.append("\n");

        // === 收入明细表 ===
        md.append("## 收入明细\n\n");
        List<Map<String, Object>> incomes = allBills != null
                ? allBills.stream().filter(b -> "income".equals(b.get("type"))).toList()
                : List.of();
        if (!incomes.isEmpty()) {
            md.append("| 日期 | 金额 | 分类 | 备注 |\n");
            md.append("|------|------|------|------|\n");
            for (Map<String, Object> bill : incomes) {
                md.append("| ").append(safeStr(bill, "consumeTime"))
                        .append(" | ¥").append(safeStr(bill, "amount"))
                        .append(" | ").append(safeStr(bill, "categoryName"))
                        .append(" | ").append(safeStr(bill, "remark"))
                        .append(" |\n");
            }
        } else {
            md.append("> 本月无收入记录\n");
        }
        md.append("\n");

        // === 分类支出汇总 ===
        md.append("## 分类支出汇总\n\n");
        if (categoryBreakdown != null && !categoryBreakdown.isEmpty()) {
            md.append("| 分类 | 金额 | 笔数 |\n");
            md.append("|------|------|------|\n");
            for (Map<String, Object> cat : categoryBreakdown) {
                md.append("| ").append(safeStr(cat, "categoryName"))
                        .append(" | ¥").append(safeStr(cat, "totalAmount"))
                        .append(" | ").append(safeStr(cat, "count"))
                        .append(" 笔 |\n");
            }
        } else {
            md.append("> 无分类汇总数据\n");
        }

        return md.toString();
    }

    /**
     * 构建消费分类推荐的Markdown请求
     *
     * @param remark     用户输入的消费备注
     * @param categories 系统可用分类列表
     * @return Markdown格式的分类推荐请求
     */
    public String buildClassifyMarkdown(String remark, List<Map<String, Object>> categories) {
        StringBuilder md = new StringBuilder();

        md.append("## 消费分类推荐请求\n\n");
        md.append("**消费备注**：").append(remark != null ? remark : "").append("\n\n");

        md.append("### 可用分类列表\n\n");
        md.append("| 分类ID | 分类名称 |\n");
        md.append("|--------|----------|\n");
        if (categories != null && !categories.isEmpty()) {
            for (Map<String, Object> cat : categories) {
                md.append("| ").append(cat.get("id"))
                        .append(" | ").append(cat.get("name"))
                        .append(" |\n");
            }
        }

        return md.toString();
    }

    /**
     * 构建用户消费特征提取文本（用于向量化存储到Qdrant）
     *
     * @param report 诊断报告中的关键信息
     * @return 特征描述文本
     */
    public String buildFeatureExtractionText(Map<String, Object> report) {
        StringBuilder sb = new StringBuilder();

        // 提取冗余消费项名称
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> wasteItems = (List<Map<String, Object>>) report.get("wasteItems");
        if (wasteItems != null && !wasteItems.isEmpty()) {
            sb.append("冗余消费：");
            for (Map<String, Object> item : wasteItems) {
                sb.append(item.get("name")).append("（").append(item.get("category")).append("）、");
            }
            sb.setLength(sb.length() - 1); // 删除末尾顿号
            sb.append("。");
        }

        // 提取不良习惯
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> badHabits = (List<Map<String, Object>>) report.get("badHabits");
        if (badHabits != null && !badHabits.isEmpty()) {
            sb.append("不良习惯：");
            for (Map<String, Object> habit : badHabits) {
                sb.append(habit.get("habit")).append("、");
            }
            sb.setLength(sb.length() - 1);
            sb.append("。");
        }

        return sb.toString().trim();
    }

    // ==================== 安全取值工具方法 ====================

    private BigDecimal safeBigDecimal(Map<String, Object> map, String key) {
        if (map == null || map.get(key) == null) return BigDecimal.ZERO;
        Object val = map.get(key);
        if (val instanceof BigDecimal bd) return bd;
        try {
            return new BigDecimal(val.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private String safeStr(Map<String, Object> map, String key) {
        if (map == null || map.get(key) == null) return "-";
        return map.get(key).toString();
    }

    private String formatMoney(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toString();
    }
}
