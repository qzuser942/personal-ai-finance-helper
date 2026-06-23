# AI 智能理财模块 - 部署与配置指南

## 一、架构概览

```
┌──────────────┐   ┌─────────────────┐   ┌─────────────────┐
│  用户客户端   │──▶│  SpringBoot后端  │──▶│  DeepSeek 官方API│
│ (鸿蒙/浏览器)│   │  (本服务)        │   │  (deepseek-chat) │
└──────────────┘   │                 │   └─────────────────┘
                   │  AI模块：        │
                   │  ├─ DeepSeekChatClient     │
                   │  ├─ AliCloudEmbeddingClient│──▶ 阿里云百炼
                   │  ├─ BillMarkdownConverter  │   (text-embedding-v2)
                   │  ├─ FinancePromptTemplates │
                   │  └─ ConsumptionVectorStore │──▶ Qdrant 向量库
                   └─────────────────┘           (Docker本地部署)
```

## 二、前置条件

### 2.1 安装 Docker

Qdrant 向量数据库通过 Docker 部署，需先安装 Docker Desktop。

- Windows: https://www.docker.com/products/docker-desktop/
- 安装后确保 Docker 正在运行

### 2.2 Java & Maven

- JDK 17+
- Maven 3.6+

## 三、获取 API 密钥

### 3.1 DeepSeek 官方 API 密钥

1. 访问 https://platform.deepseek.com/
2. 注册并登录
3. 进入「API Keys」页面，点击「创建 API Key」
4. 复制生成的密钥（格式：`sk-xxxx`）
5. **注意**：DeepSeek API 是付费服务，需要充值后才能使用

### 3.2 阿里云百炼 API 密钥

1. 访问 https://bailian.console.aliyun.com/
2. 登录阿里云账号
3. 开通「模型服务灵积」→「文本嵌入」→ `text-embedding-v2`
4. 进入「API-KEY 管理」创建密钥（格式：`sk-xxxx`）
5. 开通后去「模型广场」确认 `text-embedding-v2` 模型已授权

## 四、Qdrant 向量数据库部署

### 4.1 一条命令启动

```bash
docker run -d \
  --name qdrant \
  -p 6333:6333 \
  -p 6334:6334 \
  -v qdrant_storage:/qdrant/storage \
  qdrant/qdrant:latest
```

- `6333`：REST API 端口（可选）
- `6334`：gRPC 端口（**本服务使用**）
- 数据持久化到 Docker Volume `qdrant_storage`

### 4.2 验证是否启动成功

```bash
docker ps | grep qdrant
# 或访问 http://localhost:6333/health
```

## 五、配置 application.yml

打开 `src/main/resources/application.yml`，修改 AI 相关配置：

```yaml
ai:
  deepseek:
    # 填入你的 DeepSeek API Key
    api-key: ${DEEPSEEK_API_KEY:sk-xxxxxxxxxxxxxxxx}
    base-url: https://api.deepseek.com/v1
    model-name: deepseek-chat
    temperature: 0.7
    max-tokens: 4096
    timeout: 120s
    max-retries: 3

  alicloud:
    # 填入你的阿里云百炼 API Key
    dashscope-api-key: ${DASHSCOPE_API_KEY:sk-xxxxxxxxxxxxxxxx}
    embedding-model: text-embedding-v2
    embedding-dimension: 1536

  qdrant:
    host: ${QDRANT_HOST:localhost}
    port: ${QDRANT_GRPC_PORT:6334}
    collection-name: user_consumption_vectors
```

### 推荐方式：使用环境变量

```bash
# Windows PowerShell
$env:DEEPSEEK_API_KEY="sk-your-deepseek-key"
$env:DASHSCOPE_API_KEY="sk-your-dashscope-key"

# Linux/Mac Terminal
export DEEPSEEK_API_KEY="sk-your-deepseek-key"
export DASHSCOPE_API_KEY="sk-your-dashscope-key"
```

然后启动服务，配置会自动从环境变量读取。

## 六、启动服务

```bash
# 编译
mvn clean package -DskipTests

# 启动
java -jar target/personal-finance-server-1.0.0.jar

# 或使用 Maven
mvn spring-boot:run
```

启动日志中会显示：
```
初始化 DeepSeekChatClient: model=deepseek-chat, baseUrl=https://api.deepseek.com/v1
初始化 AliCloudEmbeddingClient: model=text-embedding-v2, dimension=1536
初始化 QdrantClient: host=localhost, port=6334
Qdrant集合 user_consumption_vectors 创建成功
```

## 七、API 接口测试

### 7.1 月度财务诊断（核心接口）

```bash
curl -X POST http://localhost:8080/api/ai/analyze \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"yearMonth": "2026-06"}'
```

**返回示例**：
```json
{
  "code": 200,
  "message": "AI财务诊断完成",
  "data": {
    "overview": {
      "totalIncome": 15000.00,
      "totalExpense": 8200.50,
      "balance": 6799.50,
      "healthScore": 82,
      "summary": "本月财务表现良好，储蓄率为45.3%..."
    },
    "wasteItems": [
      {
        "name": "外卖过度消费",
        "amount": 1850.00,
        "category": "餐饮",
        "reason": "本月外卖订单32次，其中有18次非正餐时段的零食饮品消费",
        "suggestion": "自备午餐，设定外卖预算上限800元/月",
        "severity": "HIGH"
      }
    ],
    "badHabits": [...],
    "suggestions": [...],
    "nextMonthPlan": {...},
    "recordId": 42,
    "yearMonth": "2026-06",
    "processingTimeMs": 3200
  }
}
```

### 7.2 智能分类推荐

```bash
curl -X POST http://localhost:8080/api/ai/classify \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"remark": "星巴克拿铁", "type": "expense"}'
```

### 7.3 获取分析历史

```bash
curl -X GET "http://localhost:8080/api/ai/history?page=1&size=10" \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

### 7.4 管理员重置用户向量记忆

```bash
curl -X POST http://localhost:8080/api/admin/ai/qdrant/reset \
  -H "Authorization: Bearer <ADMIN_JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"userId": 1}'
```

## 八、故障排查

### DeepSeek API 调用失败
- 确认 API Key 正确且账户有余额
- 检查网络能否访问 `https://api.deepseek.com/v1`
- 查看日志中的详细错误信息

### 阿里云百炼嵌入失败
- 确认已开通 `text-embedding-v2` 服务
- 确认 API Key 格式正确（以 `sk-` 开头）
- 检查 DashScope 控制台模型授权状态

### Qdrant 连接失败
- 确认 Docker 正在运行：`docker ps | grep qdrant`
- 确认端口 6334 未被占用
- 服务在 Qdrant 不可用时 **不会崩溃**，AI 分析会跳过个性化记忆功能

### 分析返回"该月份无账单数据"
- 确保用户在指定月份有账单记录
- 检查账单类型（只有 `expense` 和 `income` 两种类型会被统计）

## 九、核心优化点（相对旧版）

| 优化项 | 旧版 | 新版 |
|--------|------|------|
| **大模型调用** | 每次 `new OpenAiChatModel()` | 单例复用 HTTP 连接池 |
| **API 认证** | 无 API Key（本地代理） | DeepSeek 官方 API + API Key |
| **向量嵌入** | 未实现 | 阿里云百炼 text-embedding-v2 |
| **用户记忆** | `resetUserVector()` 为空操作 | Qdrant 向量存储 + 检索 |
| **结构化输出** | `Map<String, Object>` 泛型 | 强类型 POJO（8个DTO） |
| **个性化分析** | 无 | 历史画像注入 Prompt |
| **重试机制** | 无 | Resilience4j 3次重试+指数退避 |
| **Prompt 模板** | 硬编码 | classpath文件 → DB回退 → 默认兜底 |
| **API配置** | 硬编码 localhost:11434 | 全配置外部化 + 环境变量 |
