package com.finance.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * AI Markdown 账单表格构造工具
 * 将账单数据组装为结构化 Markdown 表格，送入大模型分析
 *
 * @author 胡宪棋
 */
public class MarkdownBuilder {

    /**
     * 构造完整的AI分析Prompt（含账单Markdown表格）
     */
    public static String buildAnalysisPrompt(
            String yearMonth,
            BigDecimal totalIncome,
            BigDecimal totalExpense,
            BigDecimal balance,
            List<Map<String, Object>> expenseDetails,
            List<Map<String, Object>> incomeDetails,
            List<Map<String, Object>> categoryBreakdown,
            String promptTemplate) {

        StringBuilder md = new StringBuilder();

        md.append("## 用户月度账单数据\n\n");
        md.append("**统计月份**：").append(yearMonth).append("\n");
        md.append("**总收入**：¥").append(totalIncome.setScale(2, RoundingMode.HALF_UP)).append("\n");
        md.append("**总支出**：¥").append(totalExpense.setScale(2, RoundingMode.HALF_UP)).append("\n");
        md.append("**月度结余**：¥").append(balance.setScale(2, RoundingMode.HALF_UP)).append("\n\n");

        // 支出明细表格
        md.append("### 支出明细\n\n");
        md.append("| 日期 | 金额 | 分类 | 备注 |\n");
        md.append("|------|------|------|------|\n");
        if (expenseDetails != null && !expenseDetails.isEmpty()) {
            for (Map<String, Object> bill : expenseDetails) {
                md.append("| ").append(bill.get("consumeTime"))
                        .append(" | ¥").append(bill.get("amount"))
                        .append(" | ").append(bill.get("categoryName"))
                        .append(" | ").append(bill.getOrDefault("remark", ""))
                        .append(" |\n");
            }
        } else {
            md.append("| - | - | 无支出记录 | - |\n");
        }

        // 收入明细表格
        md.append("\n### 收入明细\n\n");
        md.append("| 日期 | 金额 | 分类 | 备注 |\n");
        md.append("|------|------|------|------|\n");
        if (incomeDetails != null && !incomeDetails.isEmpty()) {
            for (Map<String, Object> bill : incomeDetails) {
                md.append("| ").append(bill.get("consumeTime"))
                        .append(" | ¥").append(bill.get("amount"))
                        .append(" | ").append(bill.get("categoryName"))
                        .append(" | ").append(bill.getOrDefault("remark", ""))
                        .append(" |\n");
            }
        } else {
            md.append("| - | - | 无收入记录 | - |\n");
        }

        // 分类支出汇总
        md.append("\n### 分类支出汇总\n\n");
        md.append("| 分类 | 金额 | 占比 |\n");
        md.append("|------|------|------|\n");
        if (categoryBreakdown != null && !categoryBreakdown.isEmpty()) {
            for (Map<String, Object> cat : categoryBreakdown) {
                md.append("| ").append(cat.get("categoryName"))
                        .append(" | ¥").append(cat.get("totalAmount"))
                        .append(" | ").append(cat.get("percentage"))
                        .append("% |\n");
            }
        }

        String markdownData = md.toString();

        // 替换Prompt模板中的占位符
        if (promptTemplate != null && promptTemplate.contains("{markdown_bill_data}")) {
            return promptTemplate.replace("{markdown_bill_data}", markdownData);
        }

        // 默认模板
        return defaultPromptTemplate(markdownData);
    }

    /**
     * 构造分类推荐Prompt
     */
    public static String buildClassifyPrompt(String remarkText, String categoryListJson) {
        return """
                你是一个消费分类助手。请根据以下消费备注文字，判断它最可能属于哪个消费分类。

                可用分类列表：%s

                消费备注："%s"

                请返回JSON格式：
                {
                  "categoryName": "推荐的分类名称",
                  "confidence": 0.95,
                  "reason": "判断依据简述"
                }
                """.formatted(categoryListJson, remarkText);
    }

    /**
     * 默认Prompt模板
     */
    private static String defaultPromptTemplate(String markdownData) {
        return """
                你是一名专业的个人理财顾问，具备丰富的消费行为分析和财务规划经验。
                请基于以下用户的月度账单数据，进行全面的财务分析。

                %s

                请严格按照以下四个维度进行分析，并以JSON格式返回分析结果：

                1. **冗余消费项**：识别不必要的、可削减的消费项目（最多列出5项）
                2. **不良消费习惯**：分析消费行为中存在的问题模式（如冲动消费、外卖依赖等）
                3. **个性化省钱方案**：基于用户消费画像，提出具体可行的省钱建议（至少3条）
                4. **月度财务复盘**：对当月财务状况进行综合评价，给出改进方向（200-300字）

                返回的JSON格式必须严格遵循以下结构：
                {
                  "redundantItems": [
                    {"name": "项目名称", "amount": 金额, "reason": "冗余原因", "suggestion": "改进建议"}
                  ],
                  "badHabits": [
                    {"habit": "习惯名称", "description": "具体描述", "impact": "财务影响评估"}
                  ],
                  "savingPlans": [
                    {"plan": "方案名称", "description": "具体做法", "estimatedSave": "预估节省金额/月"}
                  ],
                  "monthlyReview": "月度财务复盘文案..."
                }
                """.formatted(markdownData);
    }
}
