package com.finance.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finance.entity.AiConfig;
import com.finance.entity.Category;
import com.finance.entity.SysAdmin;
import com.finance.mapper.AiConfigMapper;
import com.finance.service.CategoryService;
import com.finance.service.SysAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 系统数据初始化器 - 在应用启动时自动执行
 * 初始化：系统默认分类、超级管理员、AI默认配置
 *
 * @author 胡宪棋
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CategoryService categoryService;
    private final SysAdminService sysAdminService;
    private final AiConfigMapper aiConfigMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("========== 系统数据初始化开始 ==========");

        // 1. 初始化系统默认分类
        initCategories();

        // 2. 初始化超级管理员
        initSuperAdmin();

        // 3. 初始化AI默认配置
        initAiConfig();

        log.info("========== 系统数据初始化完成 ==========");
    }

    private void initCategories() {
        long count = categoryService.count(new LambdaQueryWrapper<Category>().eq(Category::getUserId, 0L));
        if (count > 0) {
            log.info("系统分类已存在，跳过初始化。现有{}条系统分类", count);
            return;
        }
        categoryService.initSystemCategories();
    }

    private void initSuperAdmin() {
        SysAdmin existing = sysAdminService.getOne(
                new LambdaQueryWrapper<SysAdmin>().eq(SysAdmin::getUsername, "admin"));
        if (existing != null) {
            log.info("超级管理员已存在，跳过初始化");
            return;
        }
        SysAdmin admin = new SysAdmin();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRole("SUPER_ADMIN");
        sysAdminService.save(admin);
        log.info("超级管理员初始化完成: admin/admin123");
    }

    private void initAiConfig() {
        Long count = aiConfigMapper.selectCount(
                new LambdaQueryWrapper<com.finance.entity.AiConfig>().eq(
                        com.finance.entity.AiConfig::getConfigKey, "model_name"));
        if (count > 0) {
            log.info("AI配置已存在，跳过初始化");
            return;
        }

        // Prompt模板 - 分析
        insertAiConfig("prompt_template_analysis",
                "你是一名专业的个人理财顾问，具备丰富的消费行为分析和财务规划经验。\n" +
                "请基于以下用户的月度账单数据，进行全面的财务分析。\n\n" +
                "{markdown_bill_data}\n\n" +
                "请严格按照以下四个维度进行分析，并以JSON格式返回分析结果：\n\n" +
                "1. **冗余消费项**：识别不必要的、可削减的消费项目（最多列出5项）\n" +
                "2. **不良消费习惯**：分析消费行为中存在的问题模式（如冲动消费、外卖依赖等）\n" +
                "3. **个性化省钱方案**：基于用户消费画像，提出具体可行的省钱建议（至少3条）\n" +
                "4. **月度财务复盘**：对当月财务状况进行综合评价，给出改进方向（200-300字）\n\n" +
                "返回的JSON格式必须严格遵循以下结构：\n" +
                "{\n" +
                "  \"redundantItems\": [\n" +
                "    {\"name\": \"项目名称\", \"amount\": 金额, \"reason\": \"冗余原因\", \"suggestion\": \"改进建议\"}\n" +
                "  ],\n" +
                "  \"badHabits\": [\n" +
                "    {\"habit\": \"习惯名称\", \"description\": \"具体描述\", \"impact\": \"财务影响评估\"}\n" +
                "  ],\n" +
                "  \"savingPlans\": [\n" +
                "    {\"plan\": \"方案名称\", \"description\": \"具体做法\", \"estimatedSave\": \"预估节省金额/月\"}\n" +
                "  ],\n" +
                "  \"monthlyReview\": \"月度财务复盘文案...\"\n" +
                "}",
                "TEXT", "AI月度账单分析Prompt模板，{markdown_bill_data}为账单数据占位符");

        // Prompt模板 - 分类
        insertAiConfig("prompt_template_classify",
                "你是一个消费分类助手。请根据以下消费备注文字，判断它最可能属于哪个消费分类。\n\n" +
                "可用分类列表：{category_list}\n\n" +
                "消费备注：\"{remark_text}\"\n\n" +
                "请返回JSON格式：\n" +
                "{\n  \"categoryName\": \"推荐的分类名称\",\n  \"confidence\": 0.95,\n  \"reason\": \"判断依据简述\"\n}",
                "TEXT", "AI消费分类推荐Prompt模板");

        // 模型参数
        insertAiConfig("model_name", "deepseek-chat", "STRING", "大模型名称");
        insertAiConfig("model_temperature", "0.7", "NUMBER", "模型temperature参数");
        insertAiConfig("model_max_tokens", "2048", "NUMBER", "模型max_tokens参数");
        insertAiConfig("model_top_p", "0.9", "NUMBER", "模型top_p参数");
        insertAiConfig("model_base_url", "http://localhost:11434", "STRING", "DeepSeek大模型API地址");

        log.info("AI默认配置初始化完成");
    }

    private void insertAiConfig(String key, String value, String type, String desc) {
        AiConfig config = new AiConfig();
        config.setConfigKey(key);
        config.setConfigValue(value);
        config.setConfigType(type);
        config.setDescription(desc);
        aiConfigMapper.insert(config);
    }
}
