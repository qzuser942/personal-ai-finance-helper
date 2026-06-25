# 个人智能理财系统 - SpringBoot后端服务

> **作者**：胡宪棋 | **班级**：软件2413 | **学号**：202421332084  
> **技术栈**：SpringBoot 3.2.6 + MyBatis-Plus 3.5.7 + MySQL 8.0 + JWT + Knife4j 4.5.0  
> **配套前端**：Vue3管理后台（`finance-admin-web`）+ HarmonyOS移动端（`harmony-finance-client`）  
> **版本**：V2.0 | **更新日期**：2026-06-25

---

## 一、项目简介

个人智能理财系统后端服务，为**普通用户端**（HarmonyOS App）和**管理员端**（Vue3 Web后台）提供统一的 RESTful API。系统涵盖用户认证、账单管理、预算控制、存钱目标、AI智能分析与向量检索、管理员运营看板、导出报表、操作审计日志、数据库备份等完整功能模块。

---

## 二、技术架构

```
┌─────────────────────────────────────────────────────────┐
│                   客户端层 (Client)                       │
│   HarmonyOS App (ArkTS)  │  Vue3 Admin Web (vite)        │
└──────────────┬──────────────────┬───────────────────────┘
               │  HTTP/JSON       │  HTTP/JSON
┌──────────────▼──────────────────▼───────────────────────┐
│                   控制层 (Controller)                     │
│  ┌──────────────┐  ┌──────────────────────────────────┐ │
│  │ 用户端(8个)  │  │ 管理员端(11个)                    │ │
│  │ /api/user/*  │  │ /api/admin/*                      │ │
│  │ /api/bill/*  │  │ 含@RequireSuperAdmin超管专用      │ │
│  │ /api/ai/*    │  │ 含@SensitiveRead敏感操作审计      │ │
│  └──────────────┘  └──────────────────────────────────┘ │
├─────────────────────────────────────────────────────────┤
│                   拦截器层 (Interceptor)                  │
│  JwtInterceptor (用户Token) │ AdminJwtInterceptor (管理员) │
│  + 运营白名单 DEFAULT_OPERATOR_ALLOWED                    │
├─────────────────────────────────────────────────────────┤
│                   业务层 (Service)                        │
│  11个Service接口 → 11个ServiceImpl                       │
│  AiServiceImpl: LangChain4j + Qdrant + Resilience4j      │
├─────────────────────────────────────────────────────────┤
│                   持久层 (Mapper)                         │
│  MyBatis-Plus BaseMapper + 自定义SQL (注解)              │
├─────────────────────────────────────────────────────────┤
│                   数据层 (Database)                       │
│  MySQL 8.0 + Druid连接池 + MyBatis-Plus逻辑删除         │
│  init.sql: 10张表 + 23条预设分类 + 15条AI配置            │
└─────────────────────────────────────────────────────────┘
```

### 核心依赖

| 依赖 | 版本 | 用途 |
|------|------|------|
| SpringBoot | 3.2.6 | 核心框架 |
| MyBatis-Plus | 3.5.7 | ORM + 分页 + 逻辑删除 |
| MySQL Connector/J | 8.x | 数据库驱动 |
| Druid | 1.2.23 | 连接池 |
| Knife4j | 4.5.0 | API文档（Swagger3） |
| jjwt | 0.12.6 | JWT Token签发/校验 |
| LangChain4j | 0.35.0 | AI大模型调用（DeepSeek兼容） |
| langchain4j-qdrant | 0.35.0 | 向量存储（消费习惯相似检索） |
| Qdrant Client | 1.10.0 | 集合管理 / Schema操作 |
| Resilience4j | 2.2.0 | LLM调用重试 + 超时控制 |
| Apache POI | 5.2.5 | Excel 导入导出 |
| Lombok | 1.18.36 | 简化代码 |
| Spring Security Crypto | — | BCrypt密码加密 |

---

## 三、项目结构

```
personal-finance-server/
├── db/
│   └── init.sql                          # 数据库初始化脚本（10表+预设数据）
├── src/main/java/com/finance/
│   ├── FinanceApplication.java           # 启动类
│   │
│   ├── ai/                               # AI分析模块
│   │   ├── client/                       # DeepSeek + 阿里云Embedding客户端
│   │   ├── config/                       # AI/Embedding/Qdrant配置
│   │   ├── converter/                    # 账单→Markdown转换器
│   │   ├── dto/                          # 诊断报告/建议/坏习惯等VO
│   │   ├── exception/                    # AiServiceException
│   │   ├── prompt/                       # 提示词模板（分类/诊断/特征提取）
│   │   └── vector/                       # 消费向量存储（Qdrant）
│   │
│   ├── annotation/                       # 自定义注解
│   │   ├── AdminLog.java                 # 操作日志采集
│   │   ├── RequireRole.java              # 角色校验
│   │   ├── RequireSuperAdmin.java        # 超管专用
│   │   └── SensitiveRead.java            # 敏感读操作审计
│   │
│   ├── aspect/
│   │   └── AdminLogAspect.java           # AOP操作日志切面（自动采集admin_role+resource_id）
│   │
│   ├── config/                           # 全局配置
│   │   ├── AiConfig.java                 # AI属性绑定
│   │   ├── DataInitializer.java          # 应用启动数据初始化
│   │   ├── FileUploadConfig.java         # 文件上传配置
│   │   ├── Knife4jConfig.java            # Knife4j分组文档
│   │   ├── MetaObjectHandlerConfig.java  # 自动填充 created_at/updated_at
│   │   ├── MyBatisPlusConfig.java        # 分页插件
│   │   ├── SecurityConfig.java           # BCryptPasswordEncoder
│   │   └── WebMvcConfig.java             # CORS + 拦截器注册
│   │
│   ├── controller/                       # 控制器层
│   │   ├── admin/                        # 管理员端（11个Controller）
│   │   │   ├── AdminAccountController.java    # 管理员账号CRUD
│   │   │   ├── AdminAiController.java         # AI配置/提示词/记录
│   │   │   ├── AdminAuthController.java       # 管理员登录/权限信息
│   │   │   ├── AdminBillController.java       # 账单管理/统计/导出
│   │   │   ├── AdminBudgetController.java     # 预算目标/存钱目标
│   │   │   ├── AdminCategoryController.java   # 分类管理
│   │   │   ├── AdminDashboardController.java  # 运营看板/异常告警/导出CSV
│   │   │   ├── AdminDatabaseController.java   # 数据库备份
│   │   │   ├── AdminFileController.java       # 文件清理
│   │   │   ├── AdminLogController.java        # 操作日志/导出
│   │   │   └── AdminUserController.java       # 普通用户管理
│   │   └── user/                        # 普通用户端（8个Controller）
│   │       ├── AiController.java              # AI分析/分类/历史
│   │       ├── BillController.java            # 账单CRUD/批量同步/搜索/导出
│   │       ├── BudgetController.java          # 预算管理
│   │       ├── CategoryController.java        # 分类查询
│   │       ├── FileController.java            # 文件上传
│   │       ├── SaveTargetController.java      # 存钱目标
│   │       ├── StatisticsController.java      # 月度统计
│   │       └── UserAuthController.java        # 用户注册/登录/资料
│   │
│   ├── entity/                           # 实体类（10个→对应10张表）
│   │   ├── AdminOperationLog.java
│   │   ├── AiAnalysisRecord.java
│   │   ├── AiConfig.java
│   │   ├── Bill.java
│   │   ├── Budget.java
│   │   ├── Category.java
│   │   ├── DatabaseBackupLog.java
│   │   ├── SaveTarget.java
│   │   ├── SysAdmin.java
│   │   └── SysUser.java
│   │
│   ├── exception/                        # 全局异常处理
│   │   ├── BusinessException.java
│   │   ├── ErrorCode.java
│   │   └── GlobalExceptionHandler.java
│   │
│   ├── interceptor/                      # 拦截器
│   │   ├── AdminJwtInterceptor.java      # 管理员JWT + 权限位校验
│   │   └── JwtInterceptor.java           # 普通用户JWT
│   │
│   ├── mapper/                           # MyBatis-Plus Mapper（10个）
│   │
│   ├── service/                          # 业务接口 + 实现（11个）
│   │
│   └── utils/                            # 工具类
│       ├── ExcelUtil.java                # Excel导入导出
│       ├── FileUtil.java                 # 文件操作
│       ├── JwtUtil.java                  # JWT签发/校验
│       ├── MarkdownBuilder.java          # Markdown构建器
│       ├── PageResult.java               # 分页结果封装
│       └── Result.java                   # 统一响应封装
│
├── src/main/resources/
│   ├── application.yml                   # 主配置（数据源/JWT/AI/文件/日志）
│   └── prompts/                          # AI提示词模板
│       ├── category-classify.txt
│       ├── diagnostic-report.txt
│       └── feature-extraction.txt
│
└── pom.xml                               # Maven依赖管理
```

---

## 四、接口总览（74个REST API）

### 4.1 普通用户端（30个接口）

| 模块 | 接口 | 方法 | 说明 |
|------|------|------|------|
| **认证** | `/api/user/register` | POST | 用户注册 |
| | `/api/user/login` | POST | 用户登录 |
| | `/api/user/profile` | GET | 获取个人资料 |
| | `/api/user/profile` | PUT | 更新个人资料 |
| | `/api/user/password` | PUT | 修改密码 |
| **账单** | `/api/bill` | POST | 新增账单 |
| | `/api/bill/sync-batch` | POST | 批量同步账单 |
| | `/api/bill/{id}` | GET | 账单详情 |
| | `/api/bill/page` | GET | 分页查询 |
| | `/api/bill/search` | GET | 全文搜索 |
| | `/api/bill/{id}` | PUT | 更新账单 |
| | `/api/bill/{id}` | DELETE | 删除账单 |
| | `/api/bill/export` | GET | 导出账单Excel |
| **分类** | `/api/category/list` | GET | 分类列表 |
| | `/api/category` | POST | 新增分类 |
| | `/api/category/{id}` | PUT | 更新分类 |
| | `/api/category/{id}` | DELETE | 删除分类 |
| **预算** | `/api/budget/current` | GET | 当月预算 |
| | `/api/budget/{yearMonth}` | GET | 指定月份预算 |
| | `/api/budget` | POST | 创建/更新预算 |
| **存钱目标** | `/api/save-target/list` | GET | 目标列表 |
| | `/api/save-target` | POST | 新增目标 |
| | `/api/save-target/{id}` | PUT | 更新目标 |
| | `/api/save-target/{id}` | DELETE | 删除目标 |
| **统计** | `/api/statistics/monthly` | GET | 月度消费统计 |
| **AI分析** | `/api/ai/analyze` | POST | 财务诊断分析 |
| | `/api/ai/classify` | POST | 账单智能分类 |
| | `/api/ai/history` | GET | AI分析历史 |
| | `/api/ai/history/{id}` | GET | 分析详情 |
| **文件** | `/api/file/upload/receipt` | POST | 上传小票 |

### 4.2 管理员端（44个接口）

| 模块 | 接口 | 方法 | 说明 |
|------|------|------|------|
| **认证** | `/api/admin/login` | POST | 管理员登录 |
| | `/api/admin/auth/info` | GET | 管理员信息（含权限位） |
| **看板** | `/api/admin/dashboard` | GET | 运营看板总览（支持months=3/6/12） |
| | `/api/admin/dashboard/alerts` | GET | 异常检测告警 |
| | `/api/admin/dashboard/export` | GET | 导出看板报表CSV |
| **用户管理** | `/api/admin/user/page` | GET | 分页查询 |
| | `/api/admin/user/{userId}/status` | PUT | 冻结/解冻 |
| | `/api/admin/user/{userId}/reset-password` | PUT | 重置密码 |
| | `/api/admin/user/export` | GET | 导出Excel |
| **账单管理** | `/api/admin/bill/page` | GET | 分页查询 |
| | `/api/admin/bill/{id}` | PUT | 更新账单 |
| | `/api/admin/bill/{id}` | DELETE | 删除账单 |
| | `/api/admin/bill/statistics` | GET | 全平台消费统计 |
| | `/api/admin/bill/export-all` | GET | 导出全量Excel |
| **分类管理** | `/api/admin/category/list` | GET | 分类列表 |
| | `/api/admin/category` | POST | 新增分类 |
| | `/api/admin/category/{id}` | PUT | 更新分类 |
| | `/api/admin/category/{id}` | DELETE | 删除分类 |
| **预算/目标** | `/api/admin/budget/{userId}` | GET | 查看用户预算 |
| | `/api/admin/budget/{id}` | PUT | 修改用户预算 |
| | `/api/admin/save-target/{userId}` | GET | 查看存钱目标 |
| | `/api/admin/save-target/{id}` | PUT | 修改存钱目标 |
| **AI配置** | `/api/admin/ai/config` | GET | 获取AI配置 |
| | `/api/admin/ai/config` | PUT | 更新AI配置 |
| | `/api/admin/ai/config/test` | POST | 测试API连接 |
| | `/api/admin/ai/config/reset` | POST | 重置为默认 |
| | `/api/admin/ai/prompt/template` | GET | 获取提示词模板 |
| | `/api/admin/ai/prompt/preview-data` | GET | 预览提示词数据 |
| | `/api/admin/ai/prompt/variables` | GET | 可用变量列表 |
| | `/api/admin/ai/records` | GET | AI分析记录 |
| | `/api/admin/ai/records/{id}` | GET | 分析详情 |
| | `/api/admin/ai/qdrant/reset` | POST | 重置向量库（超管专用） |
| **操作日志** | `/api/admin/log/page` | GET | 分页查询 |
| | `/api/admin/log/export` | GET | 导出Excel |
| **管理员** | `/api/admin/account/page` | GET | 分页查询 |
| | `/api/admin/account` | POST | 新增管理员 |
| | `/api/admin/account/{id}/role` | PUT | 修改角色 |
| | `/api/admin/account/{id}/password` | PUT | 修改密码 |
| | `/api/admin/account/{id}/reset-password` | PUT | 重置密码 |
| | `/api/admin/account/{id}` | DELETE | 删除管理员 |
| **文件运维** | `/api/admin/file/overview` | GET | 文件概览 |
| | `/api/admin/file/clean` | DELETE | 清理临时文件 |
| **数据库** | `/api/admin/database/backup` | POST | 手动备份 |
| | `/api/admin/database/backup/log` | GET | 备份日志 |

---

## 五、数据库设计（10张表）

| 表名 | 说明 | 关键字段 |
|------|------|----------|
| `sys_user` | 普通用户表 | id, username, password, email, status, role |
| `sys_admin` | 管理员表 | id, username, password, role(SUPER_ADMIN/OPERATOR), permission_bits |
| `category` | 消费分类表 | id, name, icon, type, sort_order, user_id |
| `bill` | 账单表 | id, user_id, category_id, amount, type, bill_date, sync_uuid |
| `budget` | 预算目标表 | id, user_id, yearmonth, total_budget, category_budgets(JSON) |
| `save_target` | 存钱目标表 | id, user_id, name, target_amount, saved_amount, deadline |
| `ai_analysis_record` | AI分析记录表 | id, user_id, yearmonth, result_json(MEDIUMTEXT), model_name |
| `ai_config` | AI配置表 | id, config_key, config_value, config_type, description |
| `admin_operation_log` | 操作日志表（V2.0） | id, admin_id, admin_role, operation, resource_id, ip_address, status |
| `database_backup_log` | 备份日志表 | id, file_name, file_size, status, error_msg |

---

## 六、核心特性

### 6.1 权限控制（RBAC）

```
┌──────────────────────────────────────────────────┐
│  超级管理员 (SUPER_ADMIN)                          │
│  ├── 所有管理员接口可见                            │
│  ├── @RequireSuperAdmin 标注的接口（如重置向量库）  │
│  └── 可管理其他管理员账号                           │
├──────────────────────────────────────────────────┤
│  运营管理员 (OPERATOR)                             │
│  ├── 可访问大部分管理员接口                         │
│  ├── 不能访问 @RequireSuperAdmin 标注的接口        │
│  ├── 前端不渲染无权限按钮/页面（v-permission指令）  │
│  └── 白名单：AdminAuthController（认证/权限信息）   │
└──────────────────────────────────────────────────┘
```

### 6.2 操作审计日志（AOP自动采集）

- `@AdminLog` 注解 → `AdminLogAspect` 切面自动记录
- `@SensitiveRead` 注解 → 敏感读取操作（如导出报表）也记录
- 字段：管理员ID、用户名、**角色（admin_role）**、操作、方法、URL、参数、**关联资源ID（resource_id）**、IP、状态
- 参数过滤：自动排除 `HttpServletRequest`/`HttpServletResponse` 不可序列化对象

### 6.3 AI智能分析（LangChain4j + Qdrant）

| 能力 | 实现 |
|------|------|
| 财务诊断报告 | DeepSeek-V4-Flash + 提示词模板 → 结构化诊断报告 |
| 账单智能分类 | 基于账单描述的AI自动分类 |
| 消费向量检索 | 阿里云 text-embedding-v2 → Qdrant向量库 → 相似用户消费习惯 |
| LLM调用保障 | Resilience4j 重试3次（指数退避2s） + 超时120s |
| 提示词模板 | 3个模板文件（category-classify / diagnostic-report / feature-extraction） |

### 6.4 导出报表

| 导出类型 | 格式 | 包含内容 |
|----------|------|----------|
| 看板导出 | CSV | 月度总览 + 趋势数据 + 用户排行TOP10 + 分类分布 |
| 用户/账单/日志 | Excel (POI) | 分页数据全量导出 |
| 账单详情 | Excel | 用户端按月导出 |

### 6.5 数据库备份

- 手动触发：`POST /api/admin/database/backup`
- 存储路径：`./backups/`（可配置）
- 自动记录备份日志（文件名、大小、状态）

---

## 七、快速开始

### 7.1 环境要求

| 组件 | 版本 |
|------|------|
| JDK | 21+ |
| Maven | 3.8+ |
| MySQL | 8.0+ |
| Qdrant | 1.10+（可选，AI向量检索需要） |

### 7.2 启动步骤

```bash
# 1. 创建数据库
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS finance_db DEFAULT CHARACTER SET utf8mb4;"

# 2. 执行初始化脚本
mysql -u root -p finance_db < db/init.sql

# 3. 修改 application.yml 中的数据库密码

# 4. 编译打包
cd personal-finance-server
mvn clean package -DskipTests

# 5. 启动服务
java -jar target/personal-finance-server-1.0.0.jar
```

### 7.3 启动成功标志

```
========================================
  个人智能理财系统后端服务启动成功！
  Knife4j文档: http://localhost:8080/doc.html
========================================
```

### 7.4 访问入口

| 入口 | 地址 |
|------|------|
| Knife4j API文档 | `http://localhost:8080/doc.html` |
| API基础路径 | `http://localhost:8080` |
| 默认超管账号 | `admin` / `admin123` |

---

## 八、配置说明

### 关键配置项（application.yml）

```yaml
server.port: 8080                          # 服务端口

# 数据源（需修改 password）
spring.datasource.url: jdbc:mysql://localhost:3306/finance_db
spring.datasource.username: root
spring.datasource.password: 13725763363     # ← 修改为你的密码

# JWT
jwt.secret: ...                             # JWT签名密钥
jwt.expiration: 86400000                    # 用户Token有效期（24h）
jwt.admin-expiration: 86400000              # 管理员Token有效期（24h）

# AI（需配置API Key）
ai.deepseek.api-key: ${DEEPSEEK_API_KEY:}   # DeepSeek API Key
ai.alicloud.dashscope-api-key: ${ALIYUN_API_KEY:}  # 阿里云百炼 API Key

# Qdrant（可选）
ai.qdrant.host: ${QDRANT_HOST:localhost}
ai.qdrant.port: ${QDRANT_GRPC_PORT:6334}

# 文件上传
file.upload.receipt-path: ./uploads/receipts/
file.upload.backup-path: ./backups/
```

### 环境变量（推荐用于生产环境）

```bash
export DEEPSEEK_API_KEY=sk-xxxxx
export ALIYUN_API_KEY=sk-xxxxx
export QDRANT_HOST=192.168.1.100
export QDRANT_GRPC_PORT=6334
```

---

## 九、Knife4j API文档分组

| 分组 | 匹配路径 | 说明 |
|------|----------|------|
| **用户端接口** | `/api/user/*` `/api/bill/*` `/api/category/*` `/api/budget/*` `/api/save-target/*` `/api/statistics/*` `/api/ai/*` `/api/file/*` | 30个接口 |
| **管理员端接口** | `/api/admin/*` | 44个接口 |

访问 `http://localhost:8080/doc.html`，左上角可切换分组，所有接口均已标注 `@Tag`、`@Operation`、`@Parameter` 注解。

---

## 十、统一响应格式

```json
{
  "code": 200,
  "message": "操作成功",
  "data": { ... }
}
```

| 状态码 | 含义 |
|--------|------|
| 200 | 成功 |
| 400 | 参数错误 |
| 401 | 未登录/Token过期 |
| 403 | 无权限（超管专用接口） |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

---

> **文档版本**：V2.0 | 2026-06-25 | 胡宪棋（202421332084 / 软件2413）