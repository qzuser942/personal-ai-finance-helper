# 轻账智财 — 个人智能理财系统

> **作者**：qzuser942
> **版本**：V2.0 | **日期**：2026-06-27

---

## 项目简介

**轻账智财** 是一套全栈个人智能理财管理系统，由 **三大子系统** 协同构成：

- 🧠 **SpringBoot 后端服务** — 统一 RESTful API，集成 DeepSeek 大模型 AI 财务诊断
- 🌐 **Vue3 管理后台** — 管理员运营看板，全平台数据监控与审计
- 📱 **HarmonyOS 手机客户端** — 用户移动端记账、预算追踪、AI 理财分析

系统以 **AI 大模型** 为核心亮点，基于用户的月度账单数据，通过 DeepSeek 进行智能财务诊断，结合 Qdrant 向量数据库存储用户消费画像，实现个性化理财建议。

---

## 系统架构

```
┌──────────────────┐     ┌───────────────────────────────────────┐     ┌──────────────────┐
│   HarmonyOS手机   │     │         Windows/Mac/Linux PC          │     │   浏览器访问      │
│   (鸿蒙客户端)     │     │                                       │     │  (管理后台)       │
│                  │     │  ┌─────────────────────────────┐      │     │                  │
│  ArkTS声明式UI    │     │  │   SpringBoot 3.2.6 后端服务  │      │     │  Vue3 + Element  │
│  离线Preferences  │◄───►│  │   端口: 8080                 │◄────►│  │  Vite构建        │
│  记账→直连PC后端  │     │  │   Knife4j: /doc.html        │      │     │  端口: 3000(dev) │
│                  │     │  │   JWT双令牌鉴权               │      │     │  Nginx(prod)     │
│                  │     │  └──────────┬──────────────────┘      │     └──────────────────┘
└──────────────────┘     │             │                          │
                         │    ┌────────┼────────┐                │
                         │    │        │        │                │
                         │    ▼        ▼        ▼                │
                         │ ┌──────┐ ┌──────┐ ┌────────┐         │
                         │ │MySQL │ │Qdrant│ │DeepSeek│         │
                         │ │8.0+  │ │向量库 │ │大模型   │         │
                         │ │:3306 │ │:6334 │ │云端API  │         │
                         │ └──────┘ └──────┘ └────────┘         │
                         │                                       │
                         │  ┌────────────────────────┐          │
                         │  │   阿里云百炼 DashScope   │          │
                         │  │   text-embedding-v2    │          │
                         │  └────────────────────────┘          │
                         └───────────────────────────────────────┘
```

---

## 项目结构

```
personal-ai-finance-helper/
├── README.md                          # 项目总览（本文件）
├── 项目运行部署手册.md                 # 全环境部署指南
├── 流程图.puml                         # 系统总览流程图（PlantUML）
├── .gitignore                         # Git忽略规则
│
├── db/                                # 数据库设计
│   ├── init.sql                       # 完整建库建表脚本（10张表 + 预设数据）
│   └── er-diagram.puml                # 数据库ER图（PlantUML）
│
├── personal-finance-server/           # SpringBoot 后端服务
│   ├── README.md                      # 后端详细文档
│   ├── pom.xml                        # Maven依赖配置
│   └── src/main/java/com/finance/     # Java源码
│       ├── FinanceApplication.java    # 启动类
│       ├── ai/                        # AI分析模块（DeepSeek + 百炼Embedding + Qdrant）
│       ├── controller/                # 控制器（user/ 8个 + admin/ 11个）
│       ├── service/                   # 业务层（11个Service）
│       ├── mapper/                    # MyBatis数据访问层
│       ├── entity/                    # 实体类（10个）
│       ├── config/                    # 全局配置
│       ├── interceptor/               # JWT拦截器
│       ├── aspect/                    # AOP操作日志切面
│       ├── annotation/                # 自定义注解
│       └── utils/                     # 工具类
│
├── finance-admin-web/                 # Vue3 管理后台
│   ├── README.md                      # 管理后台详细文档
│   ├── package.json                   # 依赖配置
│   ├── vite.config.js                 # Vite构建配置
│   └── src/
│       ├── main.js                    # 应用入口
│       ├── App.vue                    # 根组件
│       ├── router/index.js            # 路由 + 权限守卫
│       ├── store/                     # Pinia状态管理
│       ├── api/                       # 接口模块（9个）
│       ├── layout/                    # 布局组件
│       ├── views/                     # 页面（9个功能模块）
│       ├── directives/                # 自定义指令（v-permission）
│       └── utils/                     # 工具函数
│
└── harmony-finance-client/            # HarmonyOS 鸿蒙客户端
    ├── README.md                      # 鸿蒙客户端详细文档
    ├── AppScope/                      # 应用全局配置
    └── entry/src/main/ets/
        ├── entryability/              # 应用入口
        ├── pages/                     # 页面（auth/bill/statistics/ai/budget/settings）
        ├── components/                # 通用组件（chart/bill/budget/common）
        ├── network/                   # 网络请求封装
        ├── model/                     # 数据模型
        ├── store/                     # 全局状态
        └── utils/                     # 工具函数
```

---

## 技术栈

### 后端

| 技术 | 版本 | 用途 |
|------|------|------|
| SpringBoot | 3.2.6 | 核心框架 |
| MyBatis-Plus | 3.5.7 | ORM + 分页 + 逻辑删除 |
| MySQL | 8.0+ | 关系型数据库 |
| Druid | 1.2.23 | 数据库连接池 |
| JWT (jjwt) | 0.12.6 | 双令牌鉴权 |
| Knife4j | 4.5.0 | API 在线文档 |
| LangChain4j | 0.35.0 | AI 大模型编排 |
| Qdrant Client | 1.10.0 | 向量数据库 gRPC |
| Resilience4j | 2.2.0 | 熔断重试 |
| Apache POI | 5.2.5 | Excel 导入导出 |

### 管理后台

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.4.x | 前端框架 |
| Vite | 5.x | 构建工具 |
| Element Plus | 2.5.x | UI 组件库 |
| ECharts | 5.5.x | 数据可视化 |
| Pinia | 2.1.x | 状态管理 |
| Axios | 1.6.x | HTTP 请求 |
| Day.js | 1.11.x | 日期处理 |
| SheetJS (xlsx) | 0.18.x | Excel 导出 |

### 鸿蒙客户端

| 技术 | 版本 | 用途 |
|------|------|------|
| HarmonyOS ArkTS | API 10+ | 声明式 UI |
| DevEco Studio | 5.0.3+ | 开发 IDE |
| Preferences | — | 本地持久化 |
| Canvas | — | 饼图/柱状图绘制 |

### AI 基础设施

| 组件 | 用途 |
|------|------|
| DeepSeek (deepseek-chat) | 大模型推理：月度财务诊断、消费分类 |
| 阿里云百炼 (text-embedding-v2) | 文本向量化：用户消费行为嵌入 |
| Qdrant | 向量数据库：用户消费画像存储与检索 |

---

## 功能模块

### 用户端（HarmonyOS App）

| 模块 | 功能描述 |
|------|----------|
| 记账 | 快速录入收入/支出、分类选择、备注、拍照上传小票 |
| 离线记账 | 无网络时本地缓存，联网后自动同步至后端 |
| 账单流水 | 分页查询、多维度筛选、Excel 导出 |
| 月度统计 | 饼图 + 柱状图直观展示收支结构 |
| 预算管理 | 月度总预算 + 分类子预算，可视化进度预警 |
| 存钱目标 | 创建目标、追加存款、进度追踪、自动完成 |
| AI 分析 | DeepSeek 大模型月度财务诊断，识别浪费、坏习惯、给出建议 |
| 密码锁 | 4-6 位数字密码保护隐私 |
| 深浅主题 | 一键切换浅色/深色主题 |

### 管理端（Vue3 Web 后台）

| 模块 | 功能描述 | 权限 |
|------|----------|------|
| 首页看板 | 总用户/活跃/账单/交易额统计、趋势图（业务规模+营收）、异常告警、自动刷新、导出 | 运营/超管 |
| 用户管理 | 分页列表、冻结/解冻、重置密码、Excel 导出 | 运营/超管 |
| 账单管理 | 全平台账单分页、多条件筛选、编辑修正、删除、全量导出 | 运营/超管 |
| 分类管理 | 全局分类 CRUD，系统内置/用户自定义分类展示 | 运营/超管 |
| 预算目标 | 查看/修正用户月度预算、查看/修正存钱目标进度 | 运营/超管 |
| AI 配置 | 模型参数调整、Prompt 模板管理、测试连接、全平台 AI 分析记录 | 仅超管 |
| 操作日志 | 所有管理员操作留痕审计、时间筛选、关键词搜索、Excel 导出 | 运营/超管 |
| 管理员账号 | 新增/编辑/删除管理员、角色分配、密码重置 | 仅超管 |
| 文件管理 | 存储概览、数据库一键备份、备份历史、文件清理 | 仅超管 |

---

## 数据库设计

系统使用 **finance_db** 数据库，共 **10 张表**：

| 序号 | 表名 | 说明 | 所属模块 |
|------|------|------|----------|
| 1 | `sys_user` | 普通用户表 | 用户认证 |
| 2 | `sys_admin` | 管理员表（物理分表） | 管理员认证 |
| 3 | `category` | 消费分类表（16 支出 + 7 收入） | 账单业务 |
| 4 | `bill` | 核心账单表 | 账单业务 |
| 5 | `budget` | 月度预算表（JSON 分类子预算） | 预算目标 |
| 6 | `save_target` | 存钱目标表 | 预算目标 |
| 7 | `ai_analysis_record` | AI 分析记录表 | AI 分析 |
| 8 | `ai_config` | AI 配置表（key-value 热更新） | AI 分析 |
| 9 | `admin_operation_log` | 管理员操作日志表（AOP 自动采集） | 审计运维 |
| 10 | `database_backup_log` | 数据库备份日志表 | 审计运维 |

> 数据库 ER 图详见 [db/er-diagram.puml](db/er-diagram.puml)，完整建表脚本见 [db/init.sql](db/init.sql)。

---

## 快速开始

### 1. 环境准备

- **JDK 17+**（推荐 Eclipse Temurin 17 LTS）
- **MySQL 8.0+**
- **Docker**（用于运行 Qdrant 向量数据库）
- **Node.js 18+**（管理后台开发/构建）
- **DevEco Studio 5.0.3+**（鸿蒙客户端编译）

### 2. 数据库初始化

```bash
# 连接 MySQL 并执行建库脚本
mysql -u root -p < db/init.sql
```

### 3. 启动 Qdrant 向量数据库

```bash
docker run -d --name qdrant --restart unless-stopped \
  -p 6333:6333 -p 6334:6334 \
  -v qdrant_storage:/qdrant/storage \
  qdrant/qdrant:latest
```

### 4. 启动后端服务

```bash
cd personal-finance-server
./mvnw spring-boot:run
# 或
mvn spring-boot:run
```

服务启动后访问：
- API 文档：http://localhost:8080/doc.html
- 健康检查：http://localhost:8080/actuator/health

### 5. 启动管理后台

```bash
cd finance-admin-web
npm install
npm run dev
```

访问 http://localhost:3000，使用默认管理员账号登录：
- 账号：`admin`
- 密码：`admin123`

### 6. 运行鸿蒙客户端

1. 使用 DevEco Studio 打开 `harmony-finance-client` 目录
2. 修改 `entry/src/main/ets/store/AppState.ets` 中的后端 IP 地址
3. 连接真机或启动模拟器，点击 Run

---

## 默认账号

| 角色 | 账号 | 密码 | 说明 |
|------|------|------|------|
| 超级管理员 | `admin` | `admin123` | 拥有全部权限 |
| 运营管理员 | — | — | 由超管在后台创建 |

> ⚠️ **安全提醒**：生产环境部署后请立即修改默认密码。

---

## 环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `DEEPSEEK_API_KEY` | DeepSeek API 密钥 | 无（必须配置） |
| `DASHSCOPE_API_KEY` | 阿里云百炼 API 密钥 | 无（必须配置） |
| `QDRANT_HOST` | Qdrant 服务地址 | `localhost` |
| `QDRANT_GRPC_PORT` | Qdrant gRPC 端口 | `6334` |

---

## 文档索引

| 文档 | 路径 | 说明 |
|------|------|------|
| 项目总览 | [README.md](README.md) | 本文件 |
| 运行部署手册 | [项目运行部署手册.md](项目运行部署手册.md) | 全环境部署，开箱即用 |
| 系统流程图 | [流程图.puml](流程图.puml) | PlantUML 系统总览 |
| 数据库 ER 图 | [db/er-diagram.puml](db/er-diagram.puml) | 10 表实体关系 |
| 数据库初始化 | [db/init.sql](db/init.sql) | 完整建库建表脚本 |
| 后端文档 | [personal-finance-server/README.md](personal-finance-server/README.md) | 后端架构与 API |
| 管理后台文档 | [finance-admin-web/README.md](finance-admin-web/README.md) | 前端架构与模块 |
| 鸿蒙客户端文档 | [harmony-finance-client/README.md](harmony-finance-client/README.md) | 移动端开发指南 |

---

## 通信端口

| 组件 | 端口 | 协议 | 用途 |
|------|------|------|------|
| SpringBoot 后端 | `8080` | HTTP | 全部 API 服务、Knife4j 文档 |
| MySQL | `3306` | TCP | 关系型数据持久化 |
| Qdrant gRPC | `6334` | gRPC | 向量存储与语义检索 |
| Qdrant REST | `6333` | HTTP | 向量库健康检查 |
| Vue3 管理后台 (Dev) | `3000` | HTTP | Vite 开发服务器 |
| DeepSeek API | `443` | HTTPS | 大模型推理（云端） |
| 阿里云百炼 | `443` | HTTPS | 文本嵌入向量化（云端） |

---

## License

本项目为个人课程设计作品，仅供学习参考。
