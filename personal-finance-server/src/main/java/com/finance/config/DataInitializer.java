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
                new LambdaQueryWrapper<AiConfig>().eq(
                        AiConfig::getConfigKey, "deepseek_api_key"));
        if (count > 0) {
            log.info("AI配置已存在，跳过初始化");
            return;
        }

        // === DeepSeek大模型配置 ===
        insertAiConfig("deepseek_api_key",
                "${DEEPSEEK_API_KEY:sk-your-deepseek-api-key}",
                "STRING", "DeepSeek官方API密钥（通过环境变量DEEPSEEK_API_KEY配置）");
        insertAiConfig("deepseek_base_url",
                "https://api.deepseek.com/v1",
                "STRING", "DeepSeek官方API地址（OpenAI兼容接口）");
        insertAiConfig("model_name",
                "deepseek-chat",
                "STRING", "大模型名称：deepseek-chat / deepseek-reasoner");
        insertAiConfig("model_temperature",
                "0.7",
                "NUMBER", "模型temperature参数（0-2），控制输出随机性");
        insertAiConfig("model_max_tokens",
                "4096",
                "NUMBER", "模型max_tokens参数，最大输出长度");
        insertAiConfig("model_top_p",
                "0.9",
                "NUMBER", "模型top_p核采样参数");

        // === 阿里云百炼嵌入配置 ===
        insertAiConfig("dashscope_api_key",
                "${DASHSCOPE_API_KEY:sk-your-dashscope-api-key}",
                "STRING", "阿里云百炼DashScope API密钥（通过环境变量DASHSCOPE_API_KEY配置）");
        insertAiConfig("embedding_model",
                "text-embedding-v2",
                "STRING", "阿里云百炼文本嵌入模型名称");
        insertAiConfig("embedding_dimension",
                "1536",
                "NUMBER", "向量维度（text-embedding-v2 = 1536）");

        // === Qdrant向量数据库配置 ===
        insertAiConfig("qdrant_host",
                "localhost",
                "STRING", "Qdrant向量数据库服务地址");
        insertAiConfig("qdrant_port",
                "6334",
                "NUMBER", "Qdrant gRPC通信端口");
        insertAiConfig("qdrant_collection",
                "user_consumption_vectors",
                "STRING", "Qdrant向量集合名称");

        // === Prompt模板 ===
        insertAiConfig("prompt_template_analysis",
                "你是一名专业的个人理财顾问，持有CFP认证。\n" +
                "请基于以下用户月度账单数据进行全面的财务诊断分析。\n\n" +
                "{{markdown_bill_data}}\n\n" +
                "请严格按以下JSON格式返回（不要包含```json```代码块）：\n" +
                "{\n" +
                "  \"overview\": {\"totalIncome\": 0, \"totalExpense\": 0, \"balance\": 0, " +
                "\"healthScore\": 0, \"summary\": \"\"},\n" +
                "  \"wasteItems\": [{\"name\": \"\", \"amount\": 0, \"category\": \"\", " +
                "\"reason\": \"\", \"suggestion\": \"\", \"severity\": \"MEDIUM\"}],\n" +
                "  \"badHabits\": [{\"habit\": \"\", \"description\": \"\", \"impact\": \"\", \"severity\": \"MEDIUM\"}],\n" +
                "  \"suggestions\": [{\"plan\": \"\", \"description\": \"\", \"estimatedMonthlySave\": \"\", " +
                "\"difficulty\": \"MODERATE\"}],\n" +
                "  \"nextMonthPlan\": {\"totalBudget\": 0, \"categoryAllocations\": {}, \"tips\": []}\n" +
                "}",
                "TEXT", "AI月度账单分析Prompt模板，{{markdown_bill_data}}为账单数据占位符");

        insertAiConfig("prompt_template_classify",
                "你是一个智能消费分类助手。根据消费备注自动匹配最合适的分类。\n\n" +
                "{{markdown_classify_data}}\n\n" +
                "返回JSON格式（Top3备选）：\n" +
                "{\"categoryName\": \"\", \"confidence\": 0.0, \"reason\": \"\", " +
                "\"top3Alternatives\": [{\"categoryName\": \"\", \"confidence\": 0.0}]}",
                "TEXT", "AI消费分类推荐Prompt模板");

        insertAiConfig("prompt_template_feature_extraction",
                "从以下AI诊断报告中提取用户消费行为特征。\n" +
                "{{diagnosis_summary}}\n\n" +
                "返回2-3句简洁的特征描述文本（50-100字）。",
                "TEXT", "AI消费特征提取Prompt模板（用于Qdrant向量存储）");

        log.info("AI默认配置初始化完成（共{}条配置）", 14);
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
