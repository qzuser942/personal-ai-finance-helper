# AI智能理财模块重构优化实施计划
## 一、项目背景
当前AI模块仅搭建基础LangChain4j框架，存在以下缺陷：
1. 仅通过本地代理调用OpenAI模型，未接入DeepSeek官方API，无独立API密钥管理；
2. 缺少阿里云百炼DashScope文本嵌入能力，无法生成消费向量；
3. 未集成Qdrant向量数据库，用户个性化记忆接口`resetUserVector`为空实现，无历史消费记忆功能；
4. 模型返回数据使用松散Map接收，无强类型结构化对象约束，解析稳定性差。

本次重构目标：完整切换为**云服务API驱动**架构，接入DeepSeek大模型API、阿里云百炼嵌入API、Docker本地Qdrant向量库，实现带个性化消费记忆的标准化智能理财分析能力。

## 二、核心技术栈
1. 大模型服务：DeepSeek官方API `https://api.deepseek.com/v1`，模型`deepseek-chat`
2. 向量嵌入服务：阿里云百炼 DashScope `text-embedding-v2`，输出1536维向量
3. 向量数据库：Qdrant（Docker本地部署，gRPC端口6334）
4. 开发框架：LangChain4j 0.35.0、Resilience4j（接口重试熔断）
5. 序列化方案：Jackson反序列化为强类型POJO，摒弃`Map<String,Object>`弱类型接收

## 三、分阶段实施步骤
### Phase 1：底层基础设施改造（pom依赖 + 配置文件 + 配置类 + 错误码）
#### 1.1 pom.xml 新增依赖包
```xml
<!-- LangChain4j Qdrant向量存储适配 -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-qdrant</artifactId>
    <version>0.35.0</version>
</dependency>
<!-- Qdrant原生客户端，用于集合创建、删除管理 -->
<dependency>
    <groupId>io.qdrant</groupId>
    <artifactId>client</artifactId>
    <version>1.10.0</version>
</dependency>
<!-- Qdrant gRPC运行时依赖 -->
<dependency>
    <groupId>io.grpc</groupId>
    <artifactId>grpc-netty-shaded</artifactId>
</dependency>
<!-- Resilience4j 重试机制，LLM调用容错 -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-retry</artifactId>
    <version>2.2.0</version>
</dependency>
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.2.0</version>
</dependency>
```

#### 1.2 application.yml 扩展AI全局配置
```yaml
ai:
  # DeepSeek大模型API配置
  deepseek:
    api-key: ${DEEPSEEK_API_KEY:sk-your-key}
    base-url: https://api.deepseek.com/v1
    model-name: deepseek-chat
    temperature: 0.7
    max-tokens: 4096
    timeout: 120s
    max-retries: 3
  # 阿里云百炼嵌入服务配置
  alicloud:
    dashscope-api-key: ${DASHSCOPE_API_KEY:sk-your-key}
    embedding-model: text-embedding-v2
    embedding-dimension: 1536
  # Qdrant向量库配置
  qdrant:
    host: ${QDRANT_HOST:localhost}
    port: ${QDRANT_GRPC_PORT:6334}
    collection-name: user_consumption_vectors
```

#### 1.3 新增&重构配置类
1. `config/AiConfig.java`：扩展字段，注入DeepSeek密钥、超时时间、最大重试次数；
2. 新建 `ai/config/QdrantConfig.java`：绑定`ai.qdrant`配置前缀，读取向量库连接参数；
3. 新建 `ai/config/AliCloudEmbeddingConfig.java`：绑定`ai.alicloud`配置前缀，读取嵌入服务密钥与维度；
4. 新建 `ai/config/AiClientConfig.java`：统一声明DeepSeek、阿里云嵌入、Qdrant全部AI相关Bean。

#### 1.4 全局错误码ErrorCode扩展
新增AI模块专属业务错误码：
- `AI_CONFIG_INVALID(40006)`：AI密钥、服务地址等配置缺失/非法
- `AI_EMBEDDING_ERROR(40005)`：阿里云嵌入接口调用失败
- `AI_VECTOR_STORE_ERROR(40004)`：Qdrant向量库存储/检索异常

---
### Phase 2：AI强类型DTO数据模型新建（`com.finance.ai.dto`）
统一结构化输出实体，替代松散Map返回，所有字段增加Jackson序列化注解：

| DTO类名 | 核心字段说明 |
|--------|-------------|
| FinanceDiagnosisReport | 月度理财诊断总报告：overview(总览)、wasteItems(浪费消费数组)、badHabits(不良习惯数组)、suggestions(省钱方案数组)、nextMonthPlan(下月规划) |
| FinanceOverview | 收支总览子对象：totalIncome总收入、totalExpense总支出、balance结余、healthScore消费健康分、summary文字总结 |
| WasteItem | 浪费消费条目：消费名称、金额、分类、浪费原因、改进建议、严重等级(HIGH/MEDIUM/LOW) |
| BadHabit | 不良消费习惯：习惯描述、负面影响、严重等级 |
| Suggestion | 省钱优化方案：执行计划、详细说明、每月预估节省金额、实施难度(EASY/MODERATE/CHALLENGING) |
| NextMonthPlan | 下月预算规划：总预算、各分类预算分配Map、多条优化贴士 |
| CategoryRecommendation | 备注智能分类推荐：匹配分类ID、分类名称、置信度、推荐理由、Top3备选分类数组 |
| CategoryAlternative | 备选分类子对象：分类ID、分类名称、匹配置信度 |

---
### Phase 3：AI底层客户端封装
#### 3.1 新建 `ai/client/DeepSeekChatClient.java`（单例Spring Bean）
1. 构造方法全局仅初始化一次`OpenAiChatModel`，修复原代码每次调用新建实例的性能Bug；
2. 读取配置文件API密钥完成鉴权，消除本地代理硬编码逻辑；
3. 提供两层核心方法：
   - `String chat(String systemPrompt, String userMessage)`：普通文本对话，添加`@Retry(name="llmService")`重试注解；
   - `<T> T chatStructured(String systemPrompt, String userMessage, Class<T> clazz)`：结构化对话，自动将模型JSON返回反序列化为指定DTO；
4. 封装超时、异常捕获，向上抛出统一AI业务异常。

#### 3.2 新建 `ai/client/AliCloudEmbeddingClient.java`（实现LangChain4j `EmbeddingModel` 接口）
1. 适配阿里云DashScope嵌入接口 `https://dashscope.aliyuncs.com/api/v1/services/embeddings/text-embedding/text-embedding`；
2. 实现标准接口方法：
   - `Response<Embedding> embed(String text)`：单文本向量化；
   - `Response<List<Embedding>> embedAll(List<TextSegment>)`：批量文本向量化；
   - `int dimension()`：固定返回1536，匹配`text-embedding-v2`输出规格；
3. 内置请求重试、密钥鉴权、响应校验逻辑。

---
### Phase 4：账单Markdown转换器 + 全局Prompt模板管理
#### 4.1 新建 `ai/converter/BillMarkdownConverter.java`（可单元测试实例类）
废弃原静态工具类`utils/MarkdownBuilder.java`，新增实例化转换器：
1. 方法：`buildDiagnosticMarkdown(年月,收支统计,分类占比,每日趋势,预算数据,用户历史画像): String`；
2. 自动生成标准化Markdown表格，包含6大模块：收支概况、消费明细、分类汇总、日消费趋势、预算对比、历史消费画像；
3. 做空集合、null值边界兼容，避免表格渲染错乱；
4. 原`MarkdownBuilder`添加`@Deprecated`标记，保留兼容过渡期。

#### 4.2 新建 `ai/prompt/FinancePromptTemplates.java` 模板加载器
1. 加载优先级：`classpath:prompts/*.txt`本地资源文件 > 数据库`ai_config`配置表兜底 > 代码内置默认文本；
2. 采用`{{变量名}}`占位符语法，运行时动态替换账单、历史画像数据；
3. 配套3份模板资源文件存放于`resources/prompts/`：
   - `diagnostic-report.txt`：月度理财诊断系统提示词；
   - `category-classify.txt`：消费备注智能分类提示词；
   - `feature-extraction.txt`：消费特征提取提示词（用于向量入库）。

---
### Phase 5：Qdrant用户个性化向量记忆库实现
#### 5.1 新建 `ai/vector/ConsumptionVectorStore.java`
彻底重构空实现的记忆重置接口，完整实现向量全生命周期管理：
1. `@PostConstruct ensureCollectionExists()`：项目启动自动校验向量集合，不存在则创建1536维、余弦距离匹配的集合；
2. `storeUserFeature(Long userId, String featureText, Map<String,Object> metadata)`：文本向量化后存入Qdrant，绑定用户ID元数据；
3. `List<UserVectorMatch> searchSimilar(Long userId, String queryText, int topK)`：检索用户历史相似消费特征；
4. `deleteUserVectors(Long userId)`：批量清空指定用户全部向量（实现原空接口`resetUserVector`逻辑）；
5. `List<String> retrieveUserProfile(Long userId)`：读取用户全部历史消费特征文本，拼接后注入AI提示词实现个性化分析。

---
### Phase 6：业务服务层重构
#### 6.1 顶层接口 `service/AiService.java` 签名更新
1. `FinanceDiagnosisReport analyzeMonthly(Long userId, String yearMonth)`：月度诊断返回强类型报告DTO，替换原Map返回值；
2. `CategoryRecommendation classifyRemark(String remark)`：备注智能分类返回分类推荐DTO，替换原Map返回值。

#### 6.2 实现类 `service/impl/AiServiceImpl.java` 核心重构
1. 删除私有原生模型调用方法`callDeepSeekModel()`，注入`DeepSeekChatClient`统一调用；
2. 删除静态`MarkdownBuilder`调用，注入`BillMarkdownConverter`实例；
3. 注入依赖：提示模板工具、向量存储工具；
4. 标准化个性化记忆完整业务流程：
   1. 分析前置：调用`ConsumptionVectorStore.retrieveUserProfile()`拉取用户历史消费画像，拼入Prompt；
   2. 模型分析：传入结构化Markdown账单+历史画像，调用DeepSeek生成诊断报告；
   3. 分析后置：从报告提取用户消费特征文本，调用向量库存入Qdrant；
5. 原有AI分析记录持久化、历史查询、管理员数据查看逻辑保留不变。

---
### Phase 7：Controller控制层适配
#### 7.1 用户端 `controller/user/AiController.java`
1. 统一返回包装类型 `Result<FinanceDiagnosisReport>`、`Result<CategoryRecommendation>`，移除Map泛型；
2. 更新Knife4j接口注解`@Operation`，补充强类型返回字段说明。

#### 7.2 管理员端 `controller/admin/AdminAiController.java`
1. 向量重置接口`resetQdrant()`不再空实现，底层调用`ConsumptionVectorStore.deleteUserVectors()`彻底清空用户向量数据。

---
### Phase 8：数据初始化 & 全局异常适配
1. `config/DataInitializer.java`：更新AI模块默认配置种子数据；
2. `exception/GlobalExceptionHandler.java`：新增自定义`AiServiceException`全局捕获分支，统一返回AI模块错误码；
3. 新增`ai/exception/AiServiceException.java`：AI模块专属运行时异常，携带自定义ErrorCode。

## 四、完整业务数据流（月度财务诊断主链路）
```
前端请求 → AiController → AiServiceImpl.analyzeMonthly(userId, yearMonth)
    1. BillMapper 查询当月账单统计、分类占比、每日消费趋势
    2. BudgetService 查询用户当月预算数据，用于预算对比
    3. ConsumptionVectorStore.retrieveUserProfile() 读取历史消费画像
    4. BillMarkdownConverter 拼接完整结构化Markdown账单表格
    5. FinancePromptTemplates 加载诊断模板，替换账单、历史画像变量
    6. DeepSeekChatClient.chatStructured() 调用DeepSeek API，反序列为FinanceDiagnosisReport
    7. AiAnalysisRecord 写入数据库，保存本次诊断记录
    8. 从报告提取用户消费特征文本，调用ConsumptionVectorStore.storeUserFeature()存入Qdrant向量库
    9. 向前端返回标准化FinanceDiagnosisReport对象
```

## 五、文件变更清单
### （一）18个全新创建文件
1. ai/client/DeepSeekChatClient.java
2. ai/client/AliCloudEmbeddingClient.java
3. ai/converter/BillMarkdownConverter.java
4. ai/dto/FinanceDiagnosisReport.java
5. ai/dto/FinanceOverview.java
6. ai/dto/WasteItem.java
7. ai/dto/BadHabit.java
8. ai/dto/Suggestion.java
9. ai/dto/NextMonthPlan.java
10. ai/dto/CategoryRecommendation.java
11. ai/dto/CategoryAlternative.java
12. ai/prompt/FinancePromptTemplates.java
13. ai/vector/ConsumptionVectorStore.java
14. ai/exception/AiServiceException.java
15. ai/config/QdrantConfig.java
16. ai/config/AliCloudEmbeddingConfig.java
17. ai/config/AiClientConfig.java
18. resources/prompts/ 下3份txt模板文件

### （二）11个修改文件
1. pom.xml
2. application.yml
3. config/AiConfig.java
4. config/DataInitializer.java
5. service/AiService.java
6. service/impl/AiServiceImpl.java
7. controller/user/AiController.java
8. controller/admin/AdminAiController.java
9. exception/ErrorCode.java
10. exception/GlobalExceptionHandler.java
11. entity/AiAnalysisRecord.java

### （三）废弃标记文件
`utils/MarkdownBuilder.java`：添加`@Deprecated`注解，过渡期保留，后续版本可删除，全部业务迁移至`BillMarkdownConverter`

## 六、全流程验证方案
1. **编译校验**：执行`mvn compile`，无编译报错、依赖无冲突；
2. **单元测试：Markdown转换器**：输入模拟账单数据，校验输出表格格式完整、空值无错乱；
3. **集成测试：DeepSeek客户端**：填入有效API密钥，验证对话调用、重试机制、结构化JSON解析；
4. **集成测试：阿里云嵌入客户端**：传入文本，校验返回1536维向量数组；
5. **集成测试：Qdrant向量库（依赖Docker启动Qdrant）**：完整验证「写入向量→相似检索→删除用户向量」全生命周期；
6. **业务单元测试：AiServiceImpl**：Mock全部外部客户端，验证完整诊断业务流程、历史画像拼接逻辑；
7. **端到端接口测试**：调用`POST /api/ai/analyze`，校验返回DTO所有字段完整、层级正常；
8. **个性化记忆功能验证**：同一用户连续两次发起月度分析，第二次AI提示词自动携带上一轮提取的历史消费特征；
9. **管理员重置接口验证**：调用`POST /api/admin/ai/qdrant/reset`，用户向量全部清空，下一次分析无历史画像数据。