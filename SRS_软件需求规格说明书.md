# 软件需求规格说明书（SRS）

## 个人理财助手应用系统

| 文档属性 | 内容 |
|----------|------|
| 文档编号 | SRS-PFM-2026-001 |
| 版本号 | V1.0 |
| 编制人 | 胡宪棋 |
| 班级 | 软件2413 |
| 学号 | 202421332084 |
| 编制日期 | 2026年6月23日 |
| 文档状态 | 初始版本 |
| 密级 | 内部 |

---

## 修订记录

| 版本 | 日期 | 修订人 | 修订说明 |
|------|------|--------|----------|
| V1.0 | 2026-06-23 | 胡宪棋 | 初始版本，完整SRS |

---

## 目录

1. [引言](#1-引言)
   - 1.1 [目的](#11-目的)
   - 1.2 [范围](#12-范围)
   - 1.3 [产品概述](#13-产品概述)
   - 1.4 [运行环境](#14-运行环境)
   - 1.5 [定义与缩略语](#15-定义与缩略语)
   - 1.6 [参考资料](#16-参考资料)
2. [整体产品架构](#2-整体产品架构)
   - 2.1 [三层架构说明](#21-三层架构说明)
   - 2.2 [技术选型总览](#22-技术选型总览)
   - 2.3 [数据流说明](#23-数据流说明)
3. [功能需求](#3-功能需求)
   - 3.1 [鸿蒙用户客户端功能需求](#31-鸿蒙用户客户端功能需求)
   - 3.2 [SpringBoot后端服务功能需求](#32-springboot后端服务功能需求)
   - 3.3 [Vue管理员后台功能需求](#33-vue管理员后台功能需求)
4. [数据需求](#4-数据需求)
   - 4.1 [数据库表结构设计](#41-数据库表结构设计)
   - 4.2 [数据存储规则](#42-数据存储规则)
   - 4.3 [数据隔离规则](#43-数据隔离规则)
5. [非功能需求](#5-非功能需求)
   - 5.1 [性能需求](#51-性能需求)
   - 5.2 [安全需求](#52-安全需求)
   - 5.3 [兼容性需求](#53-兼容性需求)
   - 5.4 [可扩展性需求](#54-可扩展性需求)
   - 5.5 [可用性需求](#55-可用性需求)
6. [特殊机制需求](#6-特殊机制需求)
   - 6.1 [离线缓存机制](#61-离线缓存机制)
   - 6.2 [联网自动同步机制](#62-联网自动同步机制)
   - 6.3 [JWT权限隔离机制](#63-jwt权限隔离机制)
   - 6.4 [AI Markdown结构化分析机制](#64-ai-markdown结构化分析机制)
7. [接口需求概述](#7-接口需求概述)
   - 7.1 [接口设计原则](#71-接口设计原则)
   - 7.2 [接口分类](#72-接口分类)
   - 7.3 [统一响应格式](#73-统一响应格式)
   - 7.4 [接口清单](#74-接口清单)
8. [运行部署需求](#8-运行部署需求)
   - 8.1 [局域网部署架构](#81-局域网部署架构)
   - 8.2 [端口与防火墙配置](#82-端口与防火墙配置)
   - 8.3 [部署步骤](#83-部署步骤)
   - 8.4 [跨域配置说明](#84-跨域配置说明)
9. [需求验收标准](#9-需求验收标准)
   - 9.1 [鸿蒙客户端验收标准](#91-鸿蒙客户端验收标准)
   - 9.2 [后端服务验收标准](#92-后端服务验收标准)
   - 9.3 [管理员后台验收标准](#93-管理员后台验收标准)
   - 9.4 [特殊机制验收标准](#94-特殊机制验收标准)
   - 9.5 [部署验收标准](#95-部署验收标准)

---

## 1. 引言

### 1.1 目的

本文档旨在对"个人理财助手应用系统"（以下简称"本系统"）进行完整、严谨的软件需求规格说明。文档详细描述了系统的功能需求、数据需求、非功能需求、特殊机制需求、接口需求、运行部署需求以及验收标准，为后续的概要设计、详细设计、编码实现、测试验证及项目验收提供唯一且权威的依据。

本文档的预期读者包括：
- 软件开发人员（鸿蒙前端、SpringBoot后端、Vue前端）
- 软件测试人员
- 项目指导教师及评审教师
- 系统最终用户及管理员

### 1.2 范围

本系统涵盖以下三大子系统：

1. **鸿蒙ArkTS用户客户端**：面向普通用户，提供记账录入、账单流水查询、月度统计图表、预算管理、存钱目标管理、AI智能理财分析、个人设置等功能。
2. **SpringBoot后端服务**：运行于个人电脑（Windows），提供全部业务逻辑处理、数据持久化、AI模型调用、文件存储、接口文档生成等功能。
3. **Vue3管理员后台**：面向系统管理员，提供全平台用户管理、账单管理、分类与预算管理、AI运营配置、系统运维等功能。

本系统**不涉及**的功能范围：
- 第三方支付集成（微信支付、支付宝等）
- 银行账户直连
- 云端服务器部署
- 多语言国际化
- 社交分享功能
- 消息推送服务

### 1.3 产品概述

本系统是一套基于局域网环境的个人理财管理解决方案。用户通过鸿蒙手机客户端进行日常记账、预算管理、存钱目标追踪，并可借助AI大模型对消费数据进行分析获得个性化理财建议。系统管理员通过Web后台进行全平台运营管理。全部业务数据持久化存储于个人电脑MySQL数据库，手机客户端不保留独立本地数据库，仅在无网络时使用Preferences做临时离线缓存，恢复网络后自动同步至服务端。

**产品核心价值**：
- 为用户提供便捷的移动端记账体验
- 通过AI分析帮助用户识别不良消费习惯
- 提供预算预警与存钱目标追踪，辅助用户达成财务目标
- 为管理员提供全平台数据运营管理能力

### 1.4 运行环境

#### 1.4.1 鸿蒙手机客户端

| 项目 | 要求 |
|------|------|
| 操作系统 | HarmonyOS 4.0 及以上 |
| 开发SDK | HarmonyOS SDK API 10+ |
| 开发语言 | ArkTS（声明式UI） |
| 设备要求 | 支持HarmonyOS的智能手机 |
| 网络权限 | INTERNET权限（仅局域网HTTP通信） |
| 本地存储 | Preferences API（仅用于临时缓存） |

#### 1.4.2 SpringBoot后端服务

| 项目 | 要求 |
|------|------|
| 操作系统 | Windows 10/11 64位 |
| JDK版本 | JDK 17 及以上 |
| 框架 | SpringBoot 3.x |
| 数据库 | MySQL 8.0 |
| AI框架 | LangChain4j |
| 向量数据库 | Qdrant（本地部署） |
| 大模型 | DeepSeek（本地部署或API接入） |
| 构建工具 | Maven 3.8+ |

#### 1.4.3 Vue管理员后台

| 项目 | 要求 |
|------|------|
| 运行环境 | 现代浏览器（Chrome 90+、Edge 90+、Firefox 88+） |
| 框架 | Vue3 + Element Plus |
| 构建工具 | Vite |
| 访问方式 | 浏览器访问 `http://<电脑局域网IP>:8080/admin` |

#### 1.4.4 网络环境

| 项目 | 要求 |
|------|------|
| 网络类型 | 局域网（WiFi） |
| 连通性 | 手机与电脑连接同一WiFi网络 |
| 后端端口 | 8080（HTTP） |
| 跨域 | 后端配置CORS允许鸿蒙客户端和浏览器端跨域 |

### 1.5 定义与缩略语

| 术语/缩略语 | 全称 | 说明 |
|------------|------|------|
| SRS | Software Requirements Specification | 软件需求规格说明书 |
| PMF | Product-Market Fit | 产品市场契合度 |
| ArkTS | Ark TypeScript | 鸿蒙应用开发语言 |
| JWT | JSON Web Token | 基于JSON的开放标准令牌，用于身份认证 |
| CORS | Cross-Origin Resource Sharing | 跨域资源共享 |
| BCrypt | Blowfish Crypt | 密码哈希加密算法 |
| CRUD | Create, Read, Update, Delete | 增删改查操作 |
| Preferences | — | 鸿蒙平台提供的轻量级键值对存储API |
| LangChain4j | — | Java生态的大模型应用开发框架 |
| Qdrant | — | 开源向量相似度搜索引擎 |
| SLF4J | Simple Logging Facade for Java | Java日志门面框架 |
| Knife4j | — | Java接口文档生成工具（Swagger增强版） |

### 1.6 参考资料

| 编号 | 参考资料 | 说明 |
|------|----------|------|
| [1] | demand.md | 项目PMF完整需求清单，V1.0 |
| [2] | IEEE 830-1998 | IEEE推荐软件需求规格说明书实践标准 |
| [3] | HarmonyOS应用开发文档 | 华为开发者联盟官方文档 |
| [4] | SpringBoot 3.x官方文档 | Spring官方参考文档 |
| [5] | Vue3官方文档 | Vue.js 3.x 官方指南 |
| [6] | LangChain4j官方文档 | LangChain4j框架参考 |
| [7] | Qdrant官方文档 | Qdrant向量数据库参考 |
| [8] | JWT RFC 7519 | JSON Web Token标准规范 |

---

## 2. 整体产品架构

### 2.1 三层架构说明

本系统采用严格的三层物理架构，由鸿蒙客户端（表现层）、SpringBoot后端（业务逻辑层）、MySQL数据库（数据持久层）组成，辅以Vue管理员后台（Web表现层）。

```
┌─────────────────────────────────────────────────────────────────────┐
│                        表 现 层                                      │
│  ┌──────────────────────────┐  ┌──────────────────────────────┐    │
│  │  鸿蒙ArkTS手机客户端      │  │  Vue3管理员后台（浏览器）      │    │
│  │  - 用户记账UI             │  │  - 超级管理员/运营管理员       │    │
│  │  - 账单流水展示           │  │  - 全平台数据管理              │    │
│  │  - 统计图表渲染(ECharts)   │  │  - 系统运维操作               │    │
│  │  - 预算/目标可视化        │  │  - AI运营配置                 │    │
│  │  - AI分析结果展示         │  │  - 操作日志审计               │    │
│  │  - Preferences离线缓存    │  │  - Element Plus UI组件库      │    │
│  └───────────┬──────────────┘  └──────────────┬───────────────┘    │
│              │  HTTP (局域网)                  │  HTTP (局域网)     │
│              │  http://<IP>:8080/api/*         │  http://<IP>:8080/admin/* │
└──────────────┼─────────────────────────────────┼────────────────────┘
               │                                 │
┌──────────────┴─────────────────────────────────┴────────────────────┐
│                   业 务 逻 辑 层 (SpringBoot)                         │
│  ┌──────────┐ ┌──────────┐ ┌────────────┐ ┌──────────────────┐     │
│  │用户模块   │ │账单模块   │ │AI分析模块   │ │管理员模块         │     │
│  │- 注册登录 │ │- 账单CRUD │ │- LangChain4j│ │- 管理员CRUD      │     │
│  │- JWT鉴权 │ │- 分类管理 │ │- DeepSeek   │ │- 权限角色管理     │     │
│  │- BCrypt  │ │- 预算管理 │ │- Qdrant向量库│ │- 操作日志记录    │     │
│  │- 数据隔离 │ │- 统计聚合 │ │- MD结构Prompt│ │- 用户管理         │     │
│  └──────────┘ └──────────┘ └────────────┘ └──────────────────┘     │
│  ┌──────────┐ ┌──────────┐ ┌────────────┐ ┌──────────────────┐     │
│  │文件模块   │ │安全模块   │ │预算/目标模块│ │接口文档模块       │     │
│  │- 图片存储 │ │- 参数校验 │ │- 月度预算   │ │- Knife4j         │     │
│  │- Excel导出│ │- 异常捕获 │ │- 存钱目标   │ │- 接口分组        │     │
│  │- 文件管理 │ │- CORS配置 │ │- 预算预警   │ │- 权限区分        │     │
│  │           │ │- SLF4J日志│ │            │ │                  │     │
│  └──────────┘ └──────────┘ └────────────┘ └──────────────────┘     │
└──────────────────────────────────────────────────────────────────────┘
               │
┌──────────────┴──────────────────────────────────────────────────────┐
│                   数 据 持 久 层                                      │
│  ┌──────────┐ ┌──────────┐ ┌────────────┐ ┌──────────────────┐     │
│  │ MySQL 8.0│ │ 文件系统  │ │ Qdrant     │ │ JWT Token        │     │
│  │- sys_user│ │- 小票图片 │ │- 消费向量   │ │- 短期内存存储    │     │
│  │- sys_admin│ │- Excel文件│ │- 习惯画像   │ │- 无持久化        │     │
│  │- bill    │ │          │ │            │ │                  │     │
│  │- category│ │          │ │            │ │                  │     │
│  │- budget  │ │          │ │            │ │                  │     │
│  │- save_target│       │ │            │ │                  │     │
│  └──────────┘ └──────────┘ └────────────┘ └──────────────────┘     │
└──────────────────────────────────────────────────────────────────────┘
```

**三层架构职责划分**：

| 层级 | 组成 | 职责 | 约束 |
|------|------|------|------|
| 表现层 | 鸿蒙客户端 + Vue后台 | UI渲染、用户交互、本地缓存、图表展示 | 不直接访问数据库；不包含业务逻辑 |
| 业务逻辑层 | SpringBoot后端 | 业务处理、权限校验、数据聚合、AI调用、文件管理 | 唯一有权访问MySQL；全部计算在此层完成 |
| 数据持久层 | MySQL + 文件系统 + Qdrant | 数据存储、文件持久化、向量索引 | 仅被业务逻辑层访问；手机端不直接连接 |

### 2.2 技术选型总览

| 层次 | 技术 | 版本 | 选型理由 |
|------|------|------|----------|
| 手机端UI | ArkTS声明式UI | API 10+ | 鸿蒙原生开发，组件化、高性能 |
| 手机端存储 | Preferences | 系统内置 | 轻量级键值对存储，符合无本地RDB约束 |
| 手机端图表 | ECharts（鸿蒙适配） | 5.x | 强大的移动端图表渲染能力 |
| 后端框架 | SpringBoot | 3.x | Java生态成熟，集成MyBatis-Plus高效开发 |
| 数据库 | MySQL | 8.0 | 成熟的关系型数据库，支持复杂聚合查询 |
| ORM | MyBatis-Plus | 3.5+ | 简化CRUD开发，支持分页、条件查询 |
| 接口文档 | Knife4j | 4.x | 自动生成接口文档，支持权限分组 |
| AI框架 | LangChain4j | 0.35+ | Java原生LLM集成框架，支持DeepSeek |
| 大模型 | DeepSeek | — | 本地部署，成本可控，中文理解能力强 |
| 向量库 | Qdrant | 1.x | 高性能向量检索，支持本地部署 |
| 管理后台 | Vue3 + Element Plus | 3.x | 组件库丰富，开发效率高 |
| 构建工具 | Vite | 5.x | 快速冷启动，HMR热更新 |
| 安全 | JWT + BCrypt | jjwt 0.12 / Spring Security | 无状态鉴权，密码安全存储 |
| Excel | Apache POI / EasyExcel | — | Java Excel生成，支持大数据量 |

### 2.3 数据流说明

#### 2.3.1 正常联网流程

```
[手机客户端] --HTTP POST/GET--> [SpringBoot后端] --SQL--> [MySQL]
     │                               │
     │  JSON响应（业务数据）           │ 文件I/O
     │  <-------------------------    │
     │                                ├──> [本地磁盘] (小票图片/Excel文件)
     │                                │
     │                                ├──> [DeepSeek] (AI分析)
     │                                │
     │                                └──> [Qdrant] (向��存储/检索)
```

#### 2.3.2 离线缓存流程

```
[手机客户端]
     │
     ├── 无网络 ──> [Preferences离线队列] (本地临时存储)
     │
     └── 恢复网络 ──> [自动批量同步] ──> [SpringBoot后端] ──> [MySQL]
```

#### 2.3.3 管理员后台流程

```
[浏览器] --HTTP--> [SpringBoot后端(管理员接口)]
                      │
                      ├── JWT校验（管理员角色）
                      ├── 操作日志记录（增删改操作）
                      └── MySQL / 文件系统
```

---

## 3. 功能需求

### 3.1 鸿蒙用户客户端功能需求

#### 3.1.1 首页记账录入页 (F-M-001)

| 需求编号 | F-M-001 | 需求名称 | 首页记账录入 |
|----------|---------|----------|-------------|
| 优先级 | Must-Have | 所属模块 | 记账录入 |

**功能描述**：
首页作为用户主要的记账录入界面，支持用户快速录入收支账单，包括金额输入、消费分类选择、文字备注填写、小票图片上传等功能。

**详细需求**：

| 子编号 | 需求项 | 详细说明 |
|--------|--------|----------|
| F-M-001-01 | 收支类型切换 | 提供收入/支出两种类型切换按钮，默认选中"支出"；UI风格明显区分（如颜色不同） |
| F-M-001-02 | 金额输入 | 支持数字键盘输入金额；提供常用快捷金额按钮（如10、20、50、100、200、500等），点击自动填充金额输入框 |
| F-M-001-03 | 消费分类选择 | 从后端拉取当前用户的消费分类列表（含系统内置分类+用户自定义分类）；支持网格或列表方式展示分类图标及名称供用户点选 |
| F-M-001-04 | 文字备注 | 提供文本输入框，支持用户填写消费备注说明（最大200字符）；可为空 |
| F-M-001-05 | 小票图片上传 | 可选功能，用户拍摄或从相册选择消费小票图片；选择后先行压缩再上传至后端；支持上传前预览 |
| F-M-001-06 | 联网提交 | 网络正常时，点击"提交"按钮，直接将账单数据POST至后端接口 `/api/bill/submit`；提交成功后清空表单并提示"记账成功" |
| F-M-001-07 | 离线缓存提交 | 无网络时，点击"提交"按钮，将账单数据序列化后存入Preferences离线队列；本地提示"已离线保存，联网后自动同步" |
| F-M-001-08 | 草稿保存 | 用户录入但未完成后可保存为草稿（存入Preferences）；后续可从草稿箱恢复继续编辑 |
| F-M-001-09 | 取消录入 | 点击"取消"按钮，清空当前表单所有已填内容，恢复至初始状态（可选确认弹窗防止误触） |

---

#### 3.1.2 账单流水页面 (F-M-002)

| 需求编号 | F-M-002 | 需求名称 | 账单流水查询与管理 |
|----------|---------|----------|-------------------|
| 优先级 | Must-Have | 所属模块 | 账单管理 |

**功能描述**：
提供完整的账单流水展示，支持下拉刷新、分页加载、账单详情查看、编辑、删除、多维度筛选、Excel导出等功能。

**详细需求**：

| 子编号 | 需求项 | 详细说明 |
|--------|--------|----------|
| F-M-002-01 | 分页加载 | 默认从后端分页拉取账单列表（每页20条），按消费时间倒序排列（最新的在最上方） |
| F-M-002-02 | 下拉刷新 | 用户下拉页面触发重新加载最新账单数据 |
| F-M-002-03 | 上滑加载更多 | 用户滚动至列表底部自动加载下一页数据（无限滚动或显示"加载更多"按钮） |
| F-M-002-04 | 账单详情查看 | 点击单条账单，弹出详情面板/跳转详情页；展示：金额、收支类型、分类、备注、消费时间、小票图片（如有） |
| F-M-002-05 | 账单编辑 | 详情面板内提供"编辑"按钮；弹出编辑表单，可修改金额、分类、备注；保存后调用后端更新接口实时同步MySQL |
| F-M-002-06 | 账单删除 | 详情面板内提供"删除"按钮；弹窗二次确认后调用后端删除接口；删除操作实时同步MySQL |
| F-M-002-07 | 月份筛选 | 提供月份选择器，按指定月份（如2026年6月）筛选账单 |
| F-M-002-08 | 收支类型筛选 | 提供收入/支出/全部三个Tab快速切换 |
| F-M-002-09 | 消费分类筛选 | 按消费分类（如餐饮、交通、购物等）下拉选择筛选 |
| F-M-002-10 | 关键词检索 | 提供搜索框，按备注文字关键词模糊检索账单 |
| F-M-002-11 | Excel导出 | 提供"导出Excel"按钮；请求后端 `/api/bill/export` 接口生成账单Excel文件；下载完成后提示保存至手机本地 |

---

#### 3.1.3 月度统计图表页 (F-M-003)

| 需求编号 | F-M-003 | 需求名称 | 月度统计图表 |
|----------|---------|----------|-------------|
| 优先级 | Must-Have | 所属模块 | 统计分析 |

**功能描述**：
展示后端聚合计算后的月度财务统计数据，包括收入/支出/结余概览、支出分类占比饼图、每日收支趋势柱状图。

**详细需求**：

| 子编号 | 需求项 | 详细说明 |
|--------|--------|----------|
| F-M-003-01 | 月度概览卡片 | 页面顶部展示当月总收入、总支出、月度结余三个数据卡片（金额格式化显示，如¥1,234.56） |
| F-M-003-02 | 支出分类占比饼图 | 使用ECharts渲染饼图，展示当月各支出分类的金额占比；数据由后端聚合计算返回 |
| F-M-003-03 | 每日收支趋势柱状图 | 使用ECharts渲染柱状图，X轴为日期（1日-31日），显示每日收入/支出对比；数据由后端聚合返回 |
| F-M-003-04 | 月份切换 | 提供月份切换控件（左右箭头或月份选择器）；切换后重新请求对应月份统计数据并重新渲染图表 |
| F-M-003-05 | 无数据提示 | 当月无账单数据时，图表区域展示"暂无数据"占位提示 |

---

#### 3.1.4 预算与存钱目标页面 (F-M-004)

| 需求编号 | F-M-004 | 需求名称 | 预算管理与存钱目标 |
|----------|---------|----------|-------------------|
| 优先级 | Should-Have | 所属模块 | 预算与目标 |

**功能描述**：
支持用户设置月度总预算及各分类子预算，可视化展示预算消耗进度；支持创建存钱目标并追踪存款进度。

**详细需求**：

| 子编号 | 需求项 | 详细说明 |
|--------|--------|----------|
| F-M-004-01 | 月度总预算展示 | 展示当月总预算金额、已消费金额、剩余金额；进度条可视化（绿色→黄色→红色渐变，表示消耗程度） |
| F-M-004-02 | 分类子预算展示 | 展示各消费分类的子预算金额、已消费金额及进度条 |
| F-M-004-03 | 预算创建/修改 | 提供预算设置页面，用户可创建或修改当月总预算及各分类子预算；保存后实时提交后端 |
| F-M-004-04 | 预算预警 | 当某分类消费金额达到或超过预算的80%时，APP弹窗预警提示；超过100%时弹窗警告；预警逻辑在后端计算，客户端轮询或数据返回时触发 |
| F-M-004-05 | 存钱目标列表 | 展示用户当前所有存钱目标卡片（目标名称、目标总金额、已存金额、完成百分比、进度条） |
| F-M-004-06 | 新建存钱目标 | 提供"新建目标"入口；填写目标名称、目标总金额；创建后实时提交后端 |
| F-M-004-07 | 追加存款 | 每个目标卡片提供"存入"按钮；输入追加金额；提交后端更新已存金额 |
| F-M-004-08 | 目标完成标记 | 已存金额≥目标金额时，自动标记为"已完成"；已完成目标展示完成时间 |
| F-M-004-09 | 归档查看 | 可切换查看"进行中"和"已完成/归档"目标列表 |

---

#### 3.1.5 AI智能理财分析页 (F-M-005)

| 需求编号 | F-M-005 | 需求名称 | AI智能理财分析 |
|----------|---------|----------|---------------|
| 优先级 | Should-Have | 所属模块 | AI分析 |

**功能描述**：
调用后端DeepSeek大模型对用户当月账单数据进行AI分析，生成个性化理财诊断报告；支持消费分类智能推荐。

**详细需求**：

| 子编号 | 需求项 | 详细说明 |
|--------|--------|----------|
| F-M-005-01 | 一键分析 | 提供"开始AI分析"按钮；点击后请求后端 `/api/ai/analyze` 接口；后端收集当前用户当月全部账单数据，组装为Markdown结构化表格，送入DeepSeek大模型分析 |
| F-M-005-02 | 分析结果展示 | 后端返回分析结果后，客户端以富文本/结构化样式渲染展示，包含以下区块：冗余消费项列表、不良消费习惯描述、个性化省钱方案建议、月度财务复盘文案 |
| F-M-005-03 | 加载状态 | AI分析过程可能耗时较长（预估5-30秒），前端展示加载动画及进度提示文字（"正在分析您的消费数据..."） |
| F-M-005-04 | 智能分类推荐 | 在账单录入备注输入框旁，提供"AI推荐分类"按钮或自动触发；用户输入消费备注文字后，请求后端 `/api/ai/classify` 接口；后端使用大模型解析文本，返回推荐的最优收支分类；前端展示推荐分类供用户确认或手动修改 |
| F-M-005-05 | 分析历史 | 展示用户历史AI分析记录列表，可查看过往分析结果（由后端存储分析记录） |

---

#### 3.1.6 设置与关于页面 (F-M-006)

| 需求编号 | F-M-006 | 需求名称 | 设置与关于 |
|----------|---------|----------|-----------|
| 优先级 | Must-Have | 所属模块 | 系统设置 |

**功能描述**：
提供主题切换、密码锁、手动同步、备份等系统设置功能，以及关于页面固定信息展示。

**详细需求**：

| 子编号 | 需求项 | 详细说明 |
|--------|--------|----------|
| F-M-006-01 | 主题切换 | 提供浅色/深色两种主题模式一键切换；偏好存入Preferences；切换即时生效，全局应用 |
| F-M-006-02 | 应用密码锁 | 提供4-6位数字密码锁开关及密码设置功能；密码存入Preferences；开启后每次进入APP需验证密码 |
| F-M-006-03 | 手动同步 | 提供"手动同步"按钮；触发离线缓存账单批量同步至后端；同步完成后提示"同步完成，共同步N条记录" |
| F-M-006-04 | 本地文件备份 | 提供"账单备份"按钮；下载后端生成的完整账单Excel文件保存至手机本地 |
| F-M-006-05 | 关于页面 | 固定信息展示：姓名：胡宪棋，班级：软件2413，学号：202421332084，软件功能介绍（300-500字功能介绍文案） |
| F-M-006-06 | 退出登录 | 提供"退出登录"按钮；清除本地Token及缓存数据；跳转至登录页 |

---

#### 3.1.7 用户注册与登录页 (F-M-007)

| 需求编号 | F-M-007 | 需求名称 | 用户注册与登录 |
|----------|---------|----------|---------------|
| 优先级 | Must-Have | 所属模块 | 用户认证 |

**功能描述**：
提供用户注册账号和密码登录功能，支持JWT令牌自动续期。

**详细需求**：

| 子编号 | 需求项 | 详细说明 |
|--------|--------|----------|
| F-M-007-01 | 用户注册 | 注册页面；填写用户名（4-20字符，字母数字下划线）、密码（6-20字符）、确认密码；前端校验格式后提交后端 |
| F-M-007-02 | 用户登录 | 登录页面；输入用户名+密码；请求后端 `/api/user/login` 接口；成功返回JWT令牌，客户端存入Preferences |
| F-M-007-03 | 自动登录 | APP启动时检查Preferences中是否存在有效JWT令牌；存在则自动登录跳过登录页；不存在或令牌过期则跳转登录页 |
| F-M-007-04 | Token携带 | 所有需要鉴权的接口请求自动在HTTP Header中携带 `Authorization: Bearer <token>` |

---

### 3.2 SpringBoot后端服务功能需求

#### 3.2.1 用户账户模块（普通用户） (F-B-001)

| 需求编号 | F-B-001 | 需求名称 | 用户账户管理 |
|----------|---------|----------|-------------|
| 优先级 | Must-Have | 所属模块 | 用户模块 |

**详细需求**：

| 子编号 | 需求项 | 详细说明 |
|--------|--------|----------|
| F-B-001-01 | 用户注册接口 | `POST /api/user/register`；接收用户名、密码；校验用户名唯一性；密码BCrypt加密后存入 `sys_user` 表；返回注册结果 |
| F-B-001-02 | 用户登录接口 | `POST /api/user/login`；接收用户名、密码；验证BCrypt密码；签发JWT令牌（有效期24小时，包含userId、username）；返回令牌 |
| F-B-001-03 | JWT鉴权拦截器 | 实现JWT过滤器/拦截器，拦截 `/api/**` 路径；校验Token有效性、过期状态；解析用户身份信息注入请求上下文 |
| F-B-001-04 | 用户信息查询 | `GET /api/user/profile`；返回当前登录用户基本信息（用户名、注册时间、账号状态） |
| F-B-001-05 | 用户信息修改 | `PUT /api/user/profile`；支持修改用户名等基本信息（不可修改userId） |
| F-B-001-06 | 密码修改 | `PUT /api/user/password`；需提供旧密码验证；新密码BCrypt加密更新 |
| F-B-001-07 | 数据隔离 | 所有业务接口查询时自动根据JWT中的userId过滤数据，确保用户只能操作自己的账单、预算、目标数据 |

---

#### 3.2.2 管理员账号模块 (F-B-002)

| 需求编号 | F-B-002 | 需求名称 | 管理员账号管理 |
|----------|---------|----------|---------------|
| 优先级 | Must-Have | 所属模块 | 管理员模块 |

**详细需求**：

| 子编号 | 需求项 | 详细说明 |
|--------|--------|----------|
| F-B-002-01 | 管理员登录接口 | `POST /api/admin/login`；独立于普通用户的登录接口；签发管理员JWT令牌（包含adminId、角色权限标识） |
| F-B-002-02 | 管理员JWT鉴权 | 独立的管理员JWT拦截器，拦截 `/api/admin/**` 路径；校验管理员身份及角色权限 |
| F-B-002-03 | 超级管理员角色 | 系统初始化一个超级管理员账号；拥有全部权限；不可删除超级管理员角色 |
| F-B-002-04 | 运营管理员角色 | 超级管理员可创建运营管理员账号；运营管理员权限受限（如不可管理其他管理员、不可操作系统运维模块） |
| F-B-002-05 | 管理员CRUD | `POST/GET/PUT/DELETE /api/admin/account`；仅超级管理员可操作；支持新增、查询、修改、删除管理员账号 |
| F-B-002-06 | 密码重置 | `PUT /api/admin/account/{id}/reset-password`；超级管理员可重置其他管理员密码 |
| F-B-002-07 | 权限分配 | `PUT /api/admin/account/{id}/role`；超级管理员可修改管理员角色（超级管理员/运营管理员） |

---

#### 3.2.3 账单核心业务模块 (F-B-003)

| 需求编号 | F-B-003 | 需求名称 | 账单核心业务 |
|----------|---------|----------|-------------|
| 优先级 | Must-Have | 所属模块 | 账单模块 |

**详细需求**：

| 子编号 | 需求项 | 详细说明 |
|--------|--------|----------|
| F-B-003-01 | 账单新增 | `POST /api/bill`；接收账单JSON数据（金额、收支类型、分类ID、备注、消费时间）；关联JWT中的userId；写入 `bill` 表 |
| F-B-003-02 | 账单修改 | `PUT /api/bill/{id}`；校验账单归属用户；更新可修改字段（金额、分类、备注、消费时间）；记录更新时间 |
| F-B-003-03 | 账单删除 | `DELETE /api/bill/{id}`；校验账单归属用户；物理删除记录 |
| F-B-003-04 | 账单分页查询 | `GET /api/bill/page`；支持分页参数（pageNum, pageSize）；按消费时间倒序；返回Page结果 |
| F-B-003-05 | 账单多条件筛选 | `GET /api/bill/search`；支持：月份范围、收支类型、分类ID、关键词（模糊匹配备注）、用户ID（仅管理员） |
| F-B-003-06 | 消费分类管理 | `GET /api/category/list` 查询用户分类列表（含系统内置+自定义）；`POST /api/category` 创建自定义分类；`PUT /api/category/{id}` 修改；`DELETE /api/category/{id}` 删除自定义分类（系统内置分类不可删除） |
| F-B-003-07 | 预算管理 | `POST /api/budget` 创建/覆盖月度预算；`GET /api/budget/current` 获取当月预算；`GET /api/budget/{yearMonth}` 获取指定月份预算 |
| F-B-003-08 | 存钱目标管理 | `POST /api/save-target` 创建；`PUT /api/save-target/{id}` 修改/追加；`DELETE /api/save-target/{id}` 删除；`GET /api/save-target/list` 查询列表（支持按状态筛选：进行中/已完成） |
| F-B-003-09 | 月度统计聚合 | `GET /api/statistics/monthly?yearMonth=2026-06`；返回：总收入、总支出、结余、各分类金额及占比、每日收支明细数组 |
| F-B-003-10 | 预算预警计算 | 统计接口返回时自动对比预算数据；标注超预算分类（80%警告、100%超过）；客户端据此弹窗提示 |

---

#### 3.2.4 AI智能分析模块 (F-B-004)

| 需求编号 | F-B-004 | 需求名称 | AI智能分析 |
|----------|---------|----------|-----------|
| 优先级 | Should-Have | 所属模块 | AI模块 |

**详细需求**：

| 子编号 | 需求项 | 详细说明 |
|--------|--------|----------|
| F-B-004-01 | Markdown结构化Prompt | 后端收集用户当月账单数据后，构造为Markdown表格格式（含：日期、金额、收支类型、分类、备注列）；在Prompt中嵌入该表格，附加角色设定和分析指令（"你是一名专业的理财顾问，请分析以下用户的月度消费数据..."） |
| F-B-004-02 | 大模型调用 | 使用LangChain4j集成DeepSeek模型；设置temperature=0.7；max_tokens=2048；将Markdown账单数据作为user message送入模型 |
| F-B-004-03 | 分析结果解析 | 模型返回文本按章节解析为结构化JSON：冗余消费项数组（每项含名称、金额、建议）、不良消费习惯数组、省钱方案数组、月度复盘文案 |
| F-B-004-04 | Qdrant向量存储 | 每次分析完成后，将用户当月消费特征向量化（基于分类金额分布、消费频率等）存入Qdrant；后续分析时可检索历史向量进行趋势对比 |
| F-B-004-05 | 个性化优化 | 检索用户历史消费向量，与当月向量拼接后喂入大模型，实现"对比上月/历史消费习惯"的个性化分析 |
| F-B-004-06 | 智能分类接口 | `POST /api/ai/classify`；接收备注文本；构造分类Prompt（"以下消费备注属于哪个分类：[餐饮/交通/购物/娱乐/住房/医疗/教育/其他]"）；返回推荐分类ID及匹配度 |
| F-B-004-07 | 分析记录存储 | AI分析结果存入数据库（可新增 `ai_analysis_record` 辅助表或存储于分析记录关联表）；支持历史查询 |
| F-B-004-08 | Prompt模板管理 | 支持管理员后台配置Prompt模板文本；默认Prompt模板内置于配置文件/数据库 |

> **备注**：F-B-004-07 中所述 `ai_analysis_record` 表为增强功能，用于持久化存储AI分析历史记录。该功能在原始PMF需求中未明确要求单独建表，但为实现"查看全平台AI分析记录"（demand.md 第102行）及"用户分析历史"（demand.md 第104行）功能，新增此表作为必要的辅助数据表，符合"有必要的增强功能可新增"的约束。

---

#### 3.2.5 文件与图片存储模块 (F-B-005)

| 需求编号 | F-B-005 | 需求名称 | 文件与图片存储 |
|----------|---------|----------|---------------|
| 优先级 | Must-Have | 所属模块 | 文件模块 |

**详细需求**：

| 子编号 | 需求项 | 详细说明 |
|--------|--------|----------|
| F-B-005-01 | 小票图片接收 | `POST /api/file/upload/receipt`；接收multipart/form-data图片文件；校验文件类型（仅允许jpg/png/webp）、大小（≤10MB）；生成UUID文件名保存至本地磁盘目录 `{user.dir}/uploads/receipts/` |
| F-B-005-02 | 图片路径存储 | 上传成功后，将文件访问路径（如 `/uploads/receipts/uuid.jpg`）存入 `bill` 表对应账单记录的 `receipt_image` 字段 |
| F-B-005-03 | 图片访问 | `GET /uploads/receipts/{filename}`；静态资源映射，返回图片文件流 |
| F-B-005-04 | Excel导出 | `GET /api/bill/export`；后端使用Apache POI或EasyExcel动态生成账单Excel文件；返回文件下载流（Content-Type: application/vnd.ms-excel）；文件名含导出时间戳 |
| F-B-005-05 | Excel全量导出（管理员） | `GET /api/admin/bill/export-all`；管理员接口；支持按用户、时间等条件全量导出平台账单Excel |

---

#### 3.2.6 安全与通用工具模块 (F-B-006)

| 需求编号 | F-B-006 | 需求名称 | 安全与通用工具 |
|----------|---------|----------|---------------|
| 优先级 | Must-Have | 所属模块 | 安全/基础设施 |

**详细需求**：

| 子编号 | 需求项 | 详细说明 |
|--------|--------|----------|
| F-B-006-01 | 全局参数校验 | 使用Spring Validation（@Valid/@Validated）对所有接口入参进行校验；校验失败返回统一错误响应（含字段级错误详情） |
| F-B-006-02 | 统一异常捕获 | 使用@ControllerAdvice全局异常处理器；捕获并处理：参数校验异常、业务异常（自定义BusinessException）、权限异常、系统异常；返回统一错误JSON格式 |
| F-B-006-03 | SLF4J日志记录 | 关键操作记录INFO日志（用户登录、账单增删改、管理员操作等）；异常记录ERROR日志（含堆栈信息）；DEBUG日志用于开发调试 |
| F-B-006-04 | CORS跨域配置 | 配置CorsFilter/WebMvcConfigurer；允许鸿蒙客户端（任意来源）和浏览器端（`http://<内网IP>:8080`）跨域请求；允许携带Authorization Header |
| F-B-006-05 | Knife4j接口文档 | 配置Knife4j/Swagger；自动生成接口文档页面（`/doc.html`）；使用@Tag注解区分"普通用户接口"和"管理员接口"分组；文档包含接口路径、参数说明、响应示例 |
| F-B-006-06 | 管理员操作日志 | 实现AOP切面拦截所有 `/api/admin/**` 路径的POST/PUT/DELETE请求；自动记录：操作人、操作时间、操作接口、请求参数、操作结果；写入操作日志表（`admin_operation_log`） |

> **备注**：F-B-006-06 中所述 `admin_operation_log` 操作日志表为增强功能。虽然原始PMF需求6张核心数据表未包含此表，但demand.md第80行明确要求"全局操作日志：记录管理员后台所有修改、删除操作"以及第142行"后端增加管理员操作日志记录，所有删除、修改操作留痕，后台可查询导出"。该功能无法在不建表的情况下实现，因此新增第7张数据表 `admin_operation_log`，符合"有必要的增强功能可新增"的约束。

---

### 3.3 Vue管理员后台功能需求

#### 3.3.1 登录与权限首页 (F-A-001)

| 需求编号 | F-A-001 | 需求名称 | 管理员登录与首页看板 |
|----------|---------|----------|---------------------|
| 优先级 | Must-Have | 所属模块 | 管理员后台 |

**详细需求**：

| 子编号 | 需求项 | 详细说明 |
|--------|--------|----------|
| F-A-001-01 | 管理员登录 | 独立管理员登录页面（样式区别于普通用户端）；输入管理员账号、密码；登录成功后存储管理员JWT令牌至浏览器localStorage |
| F-A-001-02 | 权限路由守卫 | Vue Router全局前置守卫；未登录或Token过期重定向至登录页；角色权限不足的路由自动拦截，页面不可访问 |
| F-A-001-03 | 后台总看板 | 首页仪表盘展示：平台总注册用户数、当月总账单量、活跃用户数（当月有账单记录的用户）、近6个月数据趋势折线图 |
| F-A-001-04 | 角色菜单控制 | 根据管理员角色（超级管理员/运营管理员）动态渲染左侧菜单；运营管理员不可见"管理员账号管理"和"系统运维"菜单项 |

---

#### 3.3.2 用户管理模块 (F-A-002)

| 需求编号 | F-A-002 | 需求名称 | 用户管理 |
|----------|---------|----------|---------|
| 优先级 | Must-Have | 所属模块 | 管理员后台 |

**详细需求**：

| 子编号 | 需求项 | 详细说明 |
|--------|--------|----------|
| F-A-002-01 | 用户列表 | 分页表格展示全部普通用户：用户ID、用户名、注册时间、账号状态（正常/冻结）、最近活跃时间 |
| F-A-002-02 | 用户检索 | 支持按用户名搜索、按账号状态筛选（正常/冻结/全部） |
| F-A-002-03 | 查看用户账单 | 列表操作列"查看账单"按钮；点击后跳转至该用户的账单列表视图（复用账单管理页面，自动以该用户ID筛选） |
| F-A-002-04 | 冻结/解冻账号 | "冻结"按钮将用户账号状态设为"冻结"（冻结后该用户无法登录）；"解冻"按钮恢复为"正常" |
| F-A-002-05 | 重置密码 | "重置密码"按钮；弹窗二次确认；后端生成随机新密码并BCrypt加密更新；提示新密码给管理员 |
| F-A-002-06 | 批量导出用户数据 | "导出"按钮；导出全量或筛选后用户列表为Excel文件下载 |

---

#### 3.3.3 账单全局管理模块 (F-A-003)

| 需求编号 | F-A-003 | 需求名称 | 账单全局管理 |
|----------|---------|----------|-------------|
| 优先级 | Must-Have | 所属模块 | 管理员后台 |

**详细需求**：

| 子编号 | 需求项 | 详细说明 |
|--------|--------|----------|
| F-A-003-01 | 全平台账单检索 | 分页表格展示所有用户账单：账单ID、所属用户、金额、收支类型、分类、备注摘要、消费时间；支持多条件组合筛选（用户名、时间范围、金额范围、收支类型） |
| F-A-003-02 | 查看小票原图 | 账单有上传小票时，列表提供缩略图预览或"查看原图"按钮；点击弹窗展示原图或新标签页打开图片 |
| F-A-003-03 | 后台编辑账单 | 提供"编辑"按钮；弹出编辑表单修改金额、分类、备注；保存后更新MySQL，并记录操作日志 |
| F-A-003-04 | 后台删除账单 | 提供"删除"按钮；二次确认后删除；记录操作日志 |
| F-A-003-05 | 批量导出全量账单 | "导出Excel"按钮；支持按当前筛选条件导出；生成全平台账单Excel文件下载 |
| F-A-003-06 | 全平台消费统计 | 统计页面展示：全平台月度收入/支出汇总柱状图、用户消费金额排名（TOP N用户饼图/条形图）、分类消费分布图 |

---

#### 3.3.4 分类&预算&存钱目标管理 (F-A-004)

| 需求编号 | F-A-004 | 需求名称 | 分类预算目标管理 |
|----------|---------|----------|-----------------|
| 优先级 | Should-Have | 所属模块 | 管理员后台 |

**详细需求**：

| 子编号 | 需求项 | 详细说明 |
|--------|--------|----------|
| F-A-004-01 | 分类管理 | 全量展示系统分类列表；支持"新增"全局默认分类（对所有用户可见）；支持"编辑"分类名称/图标；支持"删除"分类（有账单关联的分类提示不可删除或需要迁移关联） |
| F-A-004-02 | 批量操作分类 | 支持批量勾选删除分类；支持从Excel批量导入新分类 |
| F-A-004-03 | 查看用户预算 | 按用户搜索查看其月度预算数据（当前及历史）；支持管理员手动修正预算金额 |
| F-A-004-04 | 查看用户存钱目标 | 按用户搜索查看其存钱目标列表及进度；支持管理员手动修正目标数据 |

---

#### 3.3.5 AI运营模块 (F-A-005)

| 需求编号 | F-A-005 | 需求名称 | AI运营管理 |
|----------|---------|----------|-----------|
| 优先级 | Should-Have | 所属模块 | 管理员后台 |

**详细需求**：

| 子编号 | 需求项 | 详细说明 |
|--------|--------|----------|
| F-A-005-01 | Prompt模板配置 | 提供文本编辑器界面，管理员可查看和修改AI分析使用的Prompt模板文本；保存后存储至数据库或配置文件（后续分析使用新模板） |
| F-A-005-02 | 模型参数调整 | 提供大模型调用参数配置界面（temperature、max_tokens、top_p等）；仅超级管理员可修改 |
| F-A-005-03 | AI分析记录查看 | 分页列表展示全平台AI分析记录：用户、分析时间、分析摘要；点击可查看完整分析结果 |
| F-A-005-04 | 向量库管理 | 提供Qdrant向量数据清空/重置按钮（如清空某用户消费向量记忆）；操作需二次确认并记录日志 |

---

#### 3.3.6 系统运维模块 (F-A-006)

| 需求编号 | F-A-006 | 需求名称 | 系统运维管理 |
|----------|---------|----------|-------------|
| 优先级 | Should-Have | 所属模块 | 管理员后台 |

**详细需求**：

| 子编号 | 需求项 | 详细说明 |
|--------|--------|----------|
| F-A-006-01 | 管理员账号管理 | 仅超级管理员可访问；列表展示全部管理员（账号、角色、创建时间）；支持新增运营管理员、修改角色、删除管理员（超级管理员不可删除自身） |
| F-A-006-02 | 操作日志查询 | 分页列表展示所有管理员操作日志：操作时间、操作人、操作接口、操作描述；支持按操作人、时间范围、操作类型筛选 |
| F-A-006-03 | 操作日志导出 | 支持将当前筛选的操作日志导出为Excel文件 |
| F-A-006-04 | 文件存储管理 | 展示小票图片存储总占用空间、文件总数；提供扫描无效图片功能（无账单关联的孤儿文件）；支持清理无效文件 |
| F-A-006-05 | 数据库备份 | 提供"一键备份"按钮；后端执行MySQL导出（mysqldump）；生成SQL备份文件保存至本地磁盘；提供备份文件下载链接 |

---

### 3.4 功能优先级汇总（MoSCoW分级）

| 优先级 | 功能列表 |
|--------|----------|
| **Must-Have**（必须交付） | 记账录入、账单流水（含CRUD）、月度统计图表、用户注册登录、JWT鉴权、预算管理、存钱目标、主题切换、密码锁、关于页面、后端全部CRUD接口、管理员登录、用户管理、账单管理、操作日志、文件上传/下载、Excel导出、CORS配置 |
| **Should-Have**（应该交付） | AI智能分析、智能分类推荐、预算预警、管理员后台分类/预算/目标管理、AI运营配置、系统运维、数据库备份、Qdrant向量分析、分析历史 |
| **Could-Have**（可延后） | 批量导入分类、数据趋势高级图表、消费画像深度分析 |
| **Won't-Have**（本期不做） | 第三方支付、云同步、多语言、社交分享、消息推送 |

---

## 4. 数据需求

### 4.1 数据库表结构设计

#### 4.1.1 系统内置分类初始化说明

系统部署时需要在 `category` 表中预置以下基础收支分类数据（`user_id = 0` 表示系统内置分类，所有用户共享可见）：

**支出分类（type = 'expense'）**：
餐饮、交通、购物、娱乐、住房、医疗、教育、通讯、服饰、日用品、丽人、运动、旅行、宠物、数码、其他支出

**收入分类（type = 'income'）**：
工资、奖金、投资收益、兼职、红包、退款、其他收入

#### 4.1.2 sys_user 普通用户表

```sql
CREATE TABLE `sys_user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户主键ID',
    `username`    VARCHAR(64)  NOT NULL COMMENT '用户名，唯一',
    `password`    VARCHAR(255) NOT NULL COMMENT 'BCrypt加密密码',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '账号状态：1-正常，0-冻结',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    `updated_at`  DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_status` (`status`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='普通用户表';
```

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 用户唯一标识 |
| username | VARCHAR(64) | NOT NULL, UNIQUE | 登录用户名，4-20字符 |
| password | VARCHAR(255) | NOT NULL | BCrypt加密后的密码哈希值 |
| status | TINYINT | NOT NULL, DEFAULT 1 | 1=正常，0=冻结 |
| created_at | DATETIME | NOT NULL, DEFAULT NOW() | 账号注册时间 |
| updated_at | DATETIME | ON UPDATE | 最近更新时间 |

---

#### 4.1.3 sys_admin 管理员表

```sql
CREATE TABLE `sys_admin` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '管理员主键ID',
    `username`    VARCHAR(64)  NOT NULL COMMENT '管理员账号，唯一',
    `password`    VARCHAR(255) NOT NULL COMMENT 'BCrypt加密密码',
    `role`        VARCHAR(32)  NOT NULL DEFAULT 'OPERATOR' COMMENT '角色权限：SUPER_ADMIN-超级管理员，OPERATOR-运营管理员',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_admin_username` (`username`),
    KEY `idx_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员表';
```

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 管理员唯一标识 |
| username | VARCHAR(64) | NOT NULL, UNIQUE | 管理员登录账号 |
| password | VARCHAR(255) | NOT NULL | BCrypt加密密码哈希 |
| role | VARCHAR(32) | NOT NULL, DEFAULT 'OPERATOR' | SUPER_ADMIN / OPERATOR |
| created_at | DATETIME | NOT NULL, DEFAULT NOW() | 创建时间 |
| updated_at | DATETIME | ON UPDATE | 更新时间 |

---

#### 4.1.4 bill 账单表

```sql
CREATE TABLE `bill` (
    `id`             BIGINT         NOT NULL AUTO_INCREMENT COMMENT '账单主键ID',
    `user_id`        BIGINT         NOT NULL COMMENT '所属用户ID',
    `amount`         DECIMAL(12,2)  NOT NULL COMMENT '金额（正数）',
    `type`           VARCHAR(16)    NOT NULL COMMENT '收支类型：income-收入，expense-支出',
    `category_id`    BIGINT         NOT NULL COMMENT '消费分类ID，外键关联category表',
    `remark`         VARCHAR(500)   DEFAULT NULL COMMENT '文字备注，最大500字符',
    `receipt_image`  VARCHAR(500)   DEFAULT NULL COMMENT '小票图片文件路径（服务端相对路径）',
    `consume_time`   DATETIME       NOT NULL COMMENT '消费/收入发生时间',
    `created_at`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    `updated_at`     DATETIME       DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_type` (`type`),
    KEY `idx_consume_time` (`consume_time`),
    KEY `idx_user_consume` (`user_id`, `consume_time`),
    CONSTRAINT `fk_bill_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_bill_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账单表';
```

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 账单唯一标识 |
| user_id | BIGINT | NOT NULL, FK→sys_user.id | 所属用户ID |
| amount | DECIMAL(12,2) | NOT NULL | 金额，正数（收入和支出金额均为正数，由type字段区分） |
| type | VARCHAR(16) | NOT NULL | income / expense |
| category_id | BIGINT | NOT NULL, FK→category.id | 消费分类ID |
| remark | VARCHAR(500) | NULLABLE | 文字备注 |
| receipt_image | VARCHAR(500) | NULLABLE | 小票图片文件路径 |
| consume_time | DATETIME | NOT NULL | 消费发生时间 |
| created_at | DATETIME | NOT NULL, DEFAULT NOW() | 记录创建时间 |
| updated_at | DATETIME | ON UPDATE | 记录更新时间 |

**索引设计说明**：
- `idx_user_id`：按用户查询账单高频使用
- `idx_category_id`：分类筛选和外键关联
- `idx_type`：收支类型筛选
- `idx_consume_time`：按时间排序和范围筛选
- `idx_user_consume`：联合索引，覆盖"查询某用户某时间段账单"的最常见查询场景

---

#### 4.1.5 category 消费分类表

```sql
CREATE TABLE `category` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '分类主键ID',
    `user_id`     BIGINT       NOT NULL DEFAULT 0 COMMENT '所属用户ID：0表示系统内置分类，>0表示用户自定义分类',
    `name`        VARCHAR(64)  NOT NULL COMMENT '分类名称',
    `icon`        VARCHAR(128) DEFAULT NULL COMMENT '分类图标标识/图标名称',
    `type`        VARCHAR(16)  NOT NULL COMMENT '分类类型：income-收入分类，expense-支出分类',
    `sort_order`  INT          NOT NULL DEFAULT 0 COMMENT '排序序号（升序排列）',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_type` (`type`),
    UNIQUE KEY `uk_user_name_type` (`user_id`, `name`, `type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消费分类表';
```

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 分类唯一标识 |
| user_id | BIGINT | NOT NULL, DEFAULT 0 | 0=系统内置，>0=用户自定义 |
| name | VARCHAR(64) | NOT NULL | 分类名称 |
| icon | VARCHAR(128) | NULLABLE | 图标标识 |
| type | VARCHAR(16) | NOT NULL | income / expense |
| sort_order | INT | NOT NULL, DEFAULT 0 | 排序序号 |
| created_at | DATETIME | NOT NULL, DEFAULT NOW() | 创建时间 |

**唯一约束说明**：`uk_user_name_type (user_id, name, type)` 确保同一用户（或系统）在同一分类类型下不会创建同名分类。

---

#### 4.1.6 budget 月度预算表

```sql
CREATE TABLE `budget` (
    `id`              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '预算主键ID',
    `user_id`         BIGINT         NOT NULL COMMENT '所属用户ID',
    `year_month`      VARCHAR(7)     NOT NULL COMMENT '统计年月，格式：YYYY-MM',
    `total_budget`    DECIMAL(12,2)  NOT NULL DEFAULT 0.00 COMMENT '月度总预算金额',
    `category_budgets` JSON          DEFAULT NULL COMMENT '各分类子预算JSON：{"category_id": amount, ...}',
    `created_at`      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME       DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_year_month` (`user_id`, `year_month`),
    CONSTRAINT `fk_budget_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='月度预算表';
```

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 预算唯一标识 |
| user_id | BIGINT | NOT NULL, FK→sys_user.id | 所属用户ID |
| year_month | VARCHAR(7) | NOT NULL | 统计年月（YYYY-MM） |
| total_budget | DECIMAL(12,2) | NOT NULL, DEFAULT 0 | 月度总预算 |
| category_budgets | JSON | NULLABLE | 各分类子预算，JSON格式 |
| created_at | DATETIME | NOT NULL, DEFAULT NOW() | 创建时间 |
| updated_at | DATETIME | ON UPDATE | 更新时间 |

**category_budgets JSON字段格式示例**：
```json
{
  "1": 2000.00,
  "2": 500.00,
  "3": 1000.00,
  "5": 3000.00
}
```
键为category_id，值为该分类的月度预算金额。

---

#### 4.1.7 save_target 存钱目标表

```sql
CREATE TABLE `save_target` (
    `id`              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '目标主键ID',
    `user_id`         BIGINT         NOT NULL COMMENT '所属用户ID',
    `name`            VARCHAR(128)   NOT NULL COMMENT '目标名称',
    `target_amount`   DECIMAL(12,2)  NOT NULL COMMENT '目标总金额',
    `saved_amount`    DECIMAL(12,2)  NOT NULL DEFAULT 0.00 COMMENT '当前已存金额',
    `status`          TINYINT        NOT NULL DEFAULT 0 COMMENT '完成状态：0-进行中，1-已完成',
    `created_at`      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `completed_at`    DATETIME       DEFAULT NULL COMMENT '完成时间',
    `updated_at`      DATETIME       DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_user_status` (`user_id`, `status`),
    CONSTRAINT `fk_target_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='存钱目标表';
```

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 目标唯一标识 |
| user_id | BIGINT | NOT NULL, FK→sys_user.id | 所属用户ID |
| name | VARCHAR(128) | NOT NULL | 目标名称 |
| target_amount | DECIMAL(12,2) | NOT NULL | 目标总金额 |
| saved_amount | DECIMAL(12,2) | NOT NULL, DEFAULT 0 | 已存金额 |
| status | TINYINT | NOT NULL, DEFAULT 0 | 0=进行中，1=已完成 |
| created_at | DATETIME | NOT NULL, DEFAULT NOW() | 创建时间 |
| completed_at | DATETIME | NULLABLE | 完成时间（status变更为1时自动记录） |
| updated_at | DATETIME | ON UPDATE | 更新时间 |

---

#### 4.1.8 admin_operation_log 管理员操作日志表（增强功能）

> **新增说明**：本表为满足 demand.md 第80行和第142行要求而新增的第7张数据表，用于记录所有管理员增删改操作留痕。原始6张核心表（sys_user、sys_admin、bill、category、budget、save_target）无法实现此功能，属于必要的增强。

```sql
CREATE TABLE `admin_operation_log` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '日志主键ID',
    `admin_id`       BIGINT        NOT NULL COMMENT '操作管理员ID',
    `admin_username` VARCHAR(64)   NOT NULL COMMENT '操作管理员账号（冗余字段，便于查询）',
    `operation`      VARCHAR(128)  NOT NULL COMMENT '操作类型描述（如：删除账单、冻结用户、修改分类）',
    `method`         VARCHAR(16)   NOT NULL COMMENT 'HTTP请求方法（POST/PUT/DELETE）',
    `request_url`    VARCHAR(255)  NOT NULL COMMENT '请求接口路径',
    `request_params` TEXT          DEFAULT NULL COMMENT '请求参数（JSON序列化，敏感字段脱敏）',
    `ip_address`     VARCHAR(64)   DEFAULT NULL COMMENT '操作IP地址',
    `status`         TINYINT       NOT NULL DEFAULT 1 COMMENT '操作结果：1-成功，0-失败',
    `error_msg`      VARCHAR(1000) DEFAULT NULL COMMENT '失败时的错误信息',
    `created_at`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_admin_id` (`admin_id`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_operation` (`operation`),
    KEY `idx_admin_time` (`admin_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员操作日志表';
```

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 日志唯一标识 |
| admin_id | BIGINT | NOT NULL | 操作管理员ID |
| admin_username | VARCHAR(64) | NOT NULL | 冗余字段，便于直接展示 |
| operation | VARCHAR(128) | NOT NULL | 操作类型描述 |
| method | VARCHAR(16) | NOT NULL | HTTP方法 |
| request_url | VARCHAR(255) | NOT NULL | 请求路径 |
| request_params | TEXT | NULLABLE | 请求参数（脱敏后） |
| ip_address | VARCHAR(64) | NULLABLE | 操作IP |
| status | TINYINT | NOT NULL, DEFAULT 1 | 1=成功，0=失败 |
| error_msg | VARCHAR(1000) | NULLABLE | 失败错误信息 |
| created_at | DATETIME | NOT NULL, DEFAULT NOW() | 操作时间 |

---

#### 4.1.9 ai_analysis_record AI分析记录表（增强功能）

> **新增说明**：本表为满足 demand.md 第104行（"查看全平台AI分析记录、用户理财诊断历史"）和功能需求 F-M-005-05（"展示用户历史AI分析记录"）而新增的第8张数据表，用于持久化存储每次AI分析的结果，支持历史查看功能。

```sql
CREATE TABLE `ai_analysis_record` (
    `id`               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '分析记录主键ID',
    `user_id`          BIGINT        NOT NULL COMMENT '所属用户ID',
    `year_month`       VARCHAR(7)    NOT NULL COMMENT '分析账单所属月份，YYYY-MM',
    `result_json`      MEDIUMTEXT    NOT NULL COMMENT 'AI分析结果JSON（结构化存储）',
    `prompt_template`  TEXT          DEFAULT NULL COMMENT '使用的Prompt模板文本（快照）',
    `model_name`       VARCHAR(64)   NOT NULL DEFAULT 'DeepSeek' COMMENT '调用的大模型名称',
    `processing_time_ms` BIGINT      DEFAULT NULL COMMENT 'AI处理耗时（毫秒）',
    `created_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '分析时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_year_month` (`year_month`),
    KEY `idx_user_month` (`user_id`, `year_month`),
    CONSTRAINT `fk_analysis_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI分析记录表';
```

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 记录唯一标识 |
| user_id | BIGINT | NOT NULL, FK→sys_user.id | 所属用户ID |
| year_month | VARCHAR(7) | NOT NULL | 分析月份 |
| result_json | MEDIUMTEXT | NOT NULL | AI分析结果JSON |
| prompt_template | TEXT | NULLABLE | Prompt快照 |
| model_name | VARCHAR(64) | NOT NULL | 模型名称 |
| processing_time_ms | BIGINT | NULLABLE | 处理耗时 |
| created_at | DATETIME | NOT NULL, DEFAULT NOW() | 分析时间 |

---

### 4.2 数据存储规则

#### 4.2.1 持久化规则

| 规则编号 | 规则描述 |
|----------|----------|
| DS-001 | 全部业务数据唯一持久化位置为电脑端MySQL数据库 |
| DS-002 | 小票图片文件持久化存储于电脑本地磁盘目录 `{user.dir}/uploads/receipts/`，MySQL仅存储文件访问相对路径 |
| DS-003 | 生成的Excel文件存储于临时目录，下载完成后可定期清理 |
| DS-004 | Qdrant向量数据存储于本地Qdrant实例，随服务端启动 |
| DS-005 | JWT令牌不持久化，仅存在于客户端内存/Preferences中 |
| DS-006 | 数据库备份SQL文件存储于电脑本地磁盘 `{user.dir}/backups/` 目录 |

#### 4.2.2 手机端存储规则

| 规则编号 | 规则描述 |
|----------|----------|
| MS-001 | 手机端**禁止**使用任何本地RDB数据库（如SQLite、Room、RelationalStore等） |
| MS-002 | 手机端仅使用鸿蒙 `Preferences` API 存储以下数据：JWT登录令牌、离线待同步账单队列、用户主题偏好（浅色/深色）、应用密码锁配置及密码、草稿账单数据 |
| MS-003 | Preferences 存储的离线账单队列在成功同步至服务端后自动清除对应记录 |
| MS-004 | Preferences 存储的JWT令牌在用户主动退出登录时清除 |
| MS-005 | 手机端不缓存账单列表数据（每次下拉刷新从后端重新拉取，保证数据一致性） |

#### 4.2.3 数据删除规则

| 规则编号 | 规则描述 |
|----------|----------|
| DD-001 | 用户删除账单：物理删除 `bill` 表记录；若有关联小票图片，同步删除磁盘文件 |
| DD-002 | 用户删除自定义分类：若该分类已有关联账单，拒绝删除并提示"该分类下有N条账单记录，无法删除" |
| DD-003 | 管理员删除用户：级联删除该用户所有账单、预算、目标、自定义分类数据（通过外键CASCADE约束自动实现） |
| DD-004 | 管理员删除分类：若该分类已有关联账单，提示迁移或保留分类 |

---

### 4.3 数据隔离规则

| 规则编号 | 规则描述 |
|----------|----------|
| DI-001 | 所有普通用户接口查询数据时，后端自动在SQL WHERE条件中追加 `user_id = <当前登录用户ID>`，确保数据隔离 |
| DI-002 | 不同普通用户之间的账单、预算、目标、自定义分类数据完全隔离，互不可见 |
| DI-003 | 系统内置分类（`category.user_id = 0`）对所有用户可见，但不可被普通用户修改或删除 |
| DI-004 | 管理员通过管理员专用接口（`/api/admin/**`）可查看全平台数据，与普通用户接口（`/api/**`）物理路径隔离 |

---

## 5. 非功能需求

### 5.1 性能需求

| 编号 | 指标 | 要求 | 测试方法 |
|------|------|------|----------|
| NF-P-001 | 接口响应时间（P95） | 普通CRUD接口 ≤ 500ms | 压力测试工具（JMeter） |
| NF-P-002 | 接口响应时间（P95） | AI分析接口 ≤ 30s | 实际调用测量 |
| NF-P-003 | 客户端页面加载时间 | 首屏加载 ≤ 2s | 性能分析工具 |
| NF-P-004 | 图表渲染时间 | 数据返回后图表渲染 ≤ 1s | 客户端性能打点 |
| NF-P-005 | 并发用户支持 | 局域网内支持至少5台手机+1个浏览器同时正常操作 | 手动多设备测试 |
| NF-P-006 | 数据库连接池 | 最大连接数 ≥ 20 | Druid/HikariCP配置 |
| NF-P-007 | 文件上传大小 | 小票图片单文件 ≤ 10MB | 后端校验+前端压缩 |
| NF-P-008 | 离线同步吞吐量 | 单次批量同步100条账单 ≤ 5s | 批量同步测试 |
| NF-P-009 | 分页查询默认大小 | 20条/页，最大100条/页 | 后端参数校验 |

### 5.2 安全需求

| 编号 | 指标 | 要求 | 实现方式 |
|------|------|------|----------|
| NF-S-001 | 密码存储 | 所有密码必须BCrypt加密存储，禁止明文 | Spring Security BCryptPasswordEncoder |
| NF-S-002 | 身份认证 | 所有业务接口（除注册/登录）必须验证JWT令牌 | JWT过滤器/拦截器 |
| NF-S-003 | 令牌有效期 | JWT令牌有效期24小时 | JWT exp claim |
| NF-S-004 | 权限隔离 | 普通用户令牌不可访问管理员接口；运营管理员不可访问超管专属接口 | 拦截器+角色注解 |
| NF-S-005 | SQL注入防护 | 使用MyBatis参数化查询（#{}），禁止拼接SQL | MyBatis `#{}` 占位符 |
| NF-S-006 | XSS防护 | 用户输入的文本字段（备注、分类名等）后端存储前需转义 | Spring HtmlUtils / 前端输出编码 |
| NF-S-007 | 敏感信息脱敏 | 操作日志中不记录明文密码 | AOP日志切面排除password字段 |
| NF-S-008 | 文件上传安全 | 校验文件类型（仅允许jpg/png/webp）、大小、内容魔数 | 后端白名单校验 |
| NF-S-009 | 账户安全 | 登录失败5次后临时锁定30分钟（可选实现） | 可配置 |
| NF-S-010 | 局域网安全 | 仅局域网可达，不暴露外网端口映射 | Windows防火墙配置 |

### 5.3 兼容性需求

| 编号 | 指标 | 要求 |
|------|------|------|
| NF-C-001 | 鸿蒙系统版本 | 兼容 HarmonyOS 4.0 及以上版本 |
| NF-C-002 | 鸿蒙SDK版本 | 基于 HarmonyOS SDK API 10+ 开发 |
| NF-C-003 | 浏览器兼容 | Vue后台兼容 Chrome 90+、Edge 90+、Firefox 88+ |
| NF-C-004 | Java版本 | 后端基于 JDK 17 LTS 运行 |
| NF-C-005 | MySQL版本 | 兼容 MySQL 8.0+ |
| NF-C-006 | 屏幕适配 | 鸿蒙客户端适配主流手机屏幕尺寸（5.0-6.8英寸） |
| NF-C-007 | 横竖屏 | 默认竖屏使用，统计图表页支持横屏查看（可选） |

### 5.4 可扩展性需求

| 编号 | 指标 | 要求 |
|------|------|------|
| NF-E-001 | 模块化设计 | 后端按功能模块（用户、账单、AI、文件、管理员）分包，模块间低耦合 |
| NF-E-002 | 接口版本管理 | API路径预留版本号机制（如 `/api/v1/`），后续升级不影响旧版客户端 |
| NF-E-003 | 分类扩展 | 支持管理员动态新增系统分类，用户动态新增自定义分类 |
| NF-E-004 | AI模型可切换 | LangChain4j抽象层支持切换不同大模型提供商（DeepSeek→其他），仅需修改配置 |
| NF-E-005 | 数据库扩展 | 预留分库分表能力（按user_id分片），当前单库模式满足需求 |
| NF-E-006 | 接口文档自动更新 | Knife4j自动扫描Controller注解，新增接口无需手动更新文档 |

### 5.5 可用性需求

| 编号 | 指标 | 要求 |
|------|------|------|
| NF-U-001 | 错误提示友好 | 接口异常时返回中文错误描述，客户端友好展示 |
| NF-U-002 | 加载状态 | 所有网络请求期间展示加载状态（进度条/骨架屏/加载动画） |
| NF-U-003 | 空状态 | 无数据时展示"暂无数据"占位提示，而非空白页面 |
| NF-U-004 | 操作确认 | 删除等不可逆操作需二次确认弹窗 |
| NF-U-005 | 操作反馈 | 提交/修改/删除操作成功后展示Toast/Message提示 |
| NF-U-006 | 网络异常提示 | 网络不可用时客户端明确提示"网络连接失败，已切换至离线模式" |

---

## 6. 特殊机制需求

### 6.1 离线缓存机制

#### 6.1.1 机制概述

本系统的离线缓存机制仅在鸿蒙手机客户端实现。当手机与电脑后端服务之间网络不可达时，用户仍可进行记账录入操作，账单数据临时存储于手机Preferences离线队列；待网络恢复后自动或手动触发批量同步至服务端MySQL。

#### 6.1.2 离线触发条件

| 条件 | 判断方式 |
|------|----------|
| 网络请求超时 | HTTP请求超时（连接超时5s，读取超时10s） |
| 网络请求异常 | HTTP请求抛出IOException/网络不可达异常 |
| 服务端不可达 | HTTP返回非200状态码且非业务异常 |

上述任一条件满足时，客户端判定为"离线状态"，切换至离线记账模式。

#### 6.1.3 离线数据存储结构

离线账单队列存储于 Preferences，Key为 `offline_bill_queue`，Value为JSON数组字符串。

**单条离线账单JSON结构**：

```json
{
  "uuid": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "amount": 35.50,
  "type": "expense",
  "categoryId": 3,
  "categoryName": "餐饮",
  "remark": "午餐外卖",
  "consumeTime": "2026-06-23T12:30:00",
  "createdAt": "2026-06-23T12:31:00",
  "offlineCreatedAt": "2026-06-23T12:31:00",
  "syncStatus": "pending",
  "hasImage": false
}
```

**字段说明**：

| 字段 | 说明 |
|------|------|
| uuid | 客户端生成的唯一标识（UUID v4），用于去重和同步追踪 |
| amount/tpye/categoryId/categoryName/remark/consumeTime | 与线上账单字段对应 |
| createdAt | 账单创建时间 |
| offlineCreatedAt | 离线创建时间戳 |
| syncStatus | 同步状态：`pending`（待同步）、`syncing`（同步中）、`synced`（已同步） |
| hasImage | 是否有离线小票图片（离线状态暂不支持上传图片，标记为false） |

#### 6.1.4 离线功能范围

| 功能 | 离线是否可用 | 说明 |
|------|-------------|------|
| 记账录入 | ✅ 可用 | 账单存入离线队列 |
| 账单流水查看 | ❌ 不可用 | 需从后端拉取数据，离线时展示最后缓存页面+离线提示 |
| 统计图表 | ❌ 不可用 | 需后端聚合计算 |
| 预算管理 | ❌ 不可用 | 需从后端拉取预算数据 |
| 存钱目标 | ❌ 不可用 | 需从后端拉取目标数据 |
| AI分析 | ❌ 不可用 | 需调用后端大模型 |
| 主题切换 | ✅ 可用 | 纯本地操作 |
| 密码锁 | ✅ 可用 | 纯本地操作 |
| 查看关于页面 | ✅ 可用 | 纯本地操作 |
| 查看离线队列 | ✅ 可用 | 展示待同步账单数量及列表 |
| 图片上传 | ❌ 不可用 | 离线状态下不支持 |
| 用户注册/登录 | ❌ 不可用 | 需后端验证（已登录状态下Token未过期时维持登录态） |

#### 6.1.5 Preferences存储键定义

| Key | 用途 | 数据类型 |
|-----|------|----------|
| `auth_token` | JWT登录令牌 | String |
| `offline_bill_queue` | 离线待同步账单队列 | String (JSON Array) |
| `draft_bill` | 草稿账单数据 | String (JSON) |
| `theme_mode` | 主题偏好（"light"/"dark"） | String |
| `passcode_enabled` | 密码锁开关 | Boolean |
| `passcode_value` | 密码锁密码（4-6位数字） | String |

---

### 6.2 联网自动同步机制

#### 6.2.1 同步触发方式

| 触发方式 | 触发条件 | 说明 |
|----------|----------|------|
| 自动触发（APP启动） | APP每次冷启动/热启动时，检测网络可达且存在待同步数据 | 静默后台同步，不阻塞UI |
| 自动触发（网络恢复） | APP运行期间检测到网络从不可达变为可达（通过定时心跳检测或系统网络状态监听） | 自动触发同步 |
| 手动触发 | 用户在"设置"页面点击"手动同步"按钮 | 显示同步进度提示 |

#### 6.2.2 同步流程

```
┌─────────────────────────────────────────────────────────────────────┐
│                     联网自动同步机制流程图                             │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  [检测网络可达]                                                       │
│       │                                                             │
│       ▼                                                             │
│  [读取Preferences离线队列]                                            │
│       │                                                             │
│       ├── 队列为空 ──> [结束，无需同步]                                 │
│       │                                                             │
│       ▼ 队列非空                                                     │
│  [过滤已同步/同步中的记录，仅处理syncStatus=pending的记录]                │
│       │                                                             │
│       ▼                                                             │
│  [按createdAt升序排列（先创建先同步）]                                   │
│       │                                                             │
│       ▼                                                             │
│  [逐条POST /api/bill/sync-batch 或批量POST]                           │
│       │                                                             │
│       ├── 单条同步成功 ──> 标记该记录 syncStatus = "synced"             │
│       │                                                             │
│       ├── 同步失败(网络中断) ──> 停止同步，记录断点，下次恢复               │
│       │                                                             │
│       └── 同步失败(业务异常,如数据校验失败) ──> 标记失败，记录错误信息        │
│       │                                                             │
│       ▼                                                             │
│  [全部同步完成后，清除队列中所有syncStatus="synced"的记录]                 │
│       │                                                             │
│       ▼                                                             │
│  [通知UI刷新账单列表，展示同步结果Toast]                                 │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

#### 6.2.3 同步冲突处理

| 冲突场景 | 处理策略 |
|----------|----------|
| 离线账单与已存在账单重复 | 以 `uuid` 为去重键；后端检查 `uuid` 是否已处理过，重复的返回"已存在"并跳过 |
| 同步过程中网络再次中断 | 已成功的标记为synced；未成功的保留pending状态；下次触发时断点续传 |
| 离线期间该账单被其他设备删除/修改 | 当前系统为单用户单设备场景，暂不考虑多设备冲突；若后期扩展，以服务端数据为准 |

#### 6.2.4 同步状态提示

| 场景 | UI提示 |
|------|--------|
| 自动同步成功 | Toast："已自动同步 N 条离线账单"（N为本次同步成功数量） |
| 自动同步部分失败 | Toast："已同步 M/N 条，请检查网络后重试" |
| 手动同步成功 | Toast："同步完成，共同步 N 条记录" |
| 手动同步无数据 | Toast："暂无待同步数据" |
| 同步全部失败 | Toast："同步失败，请检查网络连接" |

---

### 6.3 JWT权限隔离机制

#### 6.3.1 三层权限体系

```
                    ┌──────────────────────────┐
                    │      超级管理员 (Super Admin)  │
                    │  权限：全部权限，不可删除        │
                    │  接口前缀：/api/admin/**       │
                    │  可管理管理员+全部业务数据+系统运维 │
                    └────────────┬─────────────┘
                                 │ 创建/管理
                    ┌────────────▼─────────────┐
                    │    运营管理员 (Operator)      │
                    │  权限：受限管理权限              │
                    │  接口前缀：/api/admin/**       │
                    │  不可管理管理员、不可系统运维      │
                    └──────────────────────────┘

                    ┌──────────────────────────┐
                    │      普通用户 (User)          │
                    │  权限：仅操作自身数据            │
                    │  接口前缀：/api/**             │
                    │  数据隔离：按user_id过滤        │
                    └──────────────────────────┘
```

#### 6.3.2 JWT令牌格式

**普通用户JWT Payload**：
```json
{
  "sub": "user",
  "userId": 10001,
  "username": "zhangsan",
  "role": "USER",
  "iat": 1719145600,
  "exp": 1719232000
}
```

**管理员JWT Payload**：
```json
{
  "sub": "admin",
  "adminId": 1,
  "username": "admin",
  "role": "SUPER_ADMIN",
  "iat": 1719145600,
  "exp": 1719232000
}
```

#### 6.3.3 鉴权拦截规则

| 请求路径 | 所需身份 | 鉴权方式 |
|----------|----------|----------|
| `/api/user/register` | 无需认证 | 直接放行 |
| `/api/user/login` | 无需认证 | 直接放行 |
| `/api/admin/login` | 无需认证 | 直接放行 |
| `/api/**`（除上述外） | 普通用户 | JWT Token校验（sub=user） |
| `/api/admin/**`（除登录外） | 管理员 | 管理员JWT Token校验（sub=admin） |
| `/api/admin/account/**` | 超级管理员 | 管理员JWT + role=SUPER_ADMIN |
| `/api/admin/system/**` | 超级管理员 | 管理员JWT + role=SUPER_ADMIN |
| `/uploads/**` | 无需认证（静态资源） | 直接放行 |
| `/doc.html` | 无需认证（Knife4j文档页） | 直接放行（仅开发环境） |

#### 6.3.4 权限验证流程

```
[HTTP请求到达]
       │
       ▼
[匹配路径前缀]
       │
       ├── /api/admin/** ──> [提取Authorization Header]
       │                         │
       │                         ├── Token缺失/格式错误 ──> 401 Unauthorized
       │                         ├── Token过期 ──> 401 Token Expired
       │                         ├── sub != "admin" ──> 403 Forbidden
       │                         ├── 需要超级管理员但role != "SUPER_ADMIN" ──> 403 Forbidden
       │                         └── ✓ 验证通过 ──> 注入管理员上下文，放行
       │
       ├── /api/** ──> [提取Authorization Header]
       │                   │
       │                   ├── Token缺失/格式错误 ──> 401
       │                   ├── Token过期 ──> 401
       │                   ├── sub != "user" ──> 403
       │                   └── ✓ 验证通过 ──> 注入用户上下文，放行
       │
       └── 其他路径 ──> 放行
```

---

### 6.4 AI Markdown结构化分析机制

#### 6.4.1 机制概述

AI分析模块的核心设计原则是将用户账单数据组装为Markdown结构化表格后再送入大语言模型，从而提高AI对财务数据的理解准确率和分析输出质量。所有AI接口统一使用此机制。

#### 6.4.2 Prompt构造流程

```
[前端请求AI分析] ──> [后端收集当月账单数据]
                          │
                          ▼
                  [按分类/日期组织账单数据]
                          │
                          ▼
                  [构造Markdown表格]
                          │
                          ▼
                  [组装完整Prompt（角色设定 + Markdown表格 + 分析指令 + 输出格式要求）]
                          │
                          ▼
                  [调用DeepSeek模型]
                          │
                          ▼
                  [解析返回文本 → 结构化JSON]
                          │
                          ▼
                  [存储分析记录 + 返回前端]
```

#### 6.4.3 Markdown账单表格模板

后端构造的Markdown账单结构化数据格式如下：

```markdown
## 用户月度账单数据

**统计月份**：2026年6月
**总收入**：¥15,000.00
**总支出**：¥8,350.50
**月度结余**：¥6,649.50

### 支出明细

| 日期 | 金额 | 分类 | 备注 |
|------|------|------|------|
| 2026-06-01 | ¥35.50 | 餐饮 | 午餐外卖 |
| 2026-06-02 | ¥200.00 | 交通 | 加油 |
| 2026-06-03 | ¥150.00 | 购物 | 超市采购日用品 |
| ... | ... | ... | ... |

### 收入明细

| 日期 | 金额 | 分类 | 备注 |
|------|------|------|------|
| 2026-06-01 | ¥15,000.00 | 工资 | 6月工资 |
| ... | ... | ... | ... |

### 分类支出汇总

| 分类 | 金额 | 占比 |
|------|------|------|
| 餐饮 | ¥2,500.00 | 29.9% |
| 交通 | ¥800.00 | 9.6% |
| 购物 | ¥3,000.00 | 35.9% |
| 娱乐 | ¥1,200.00 | 14.4% |
| 住房 | ¥500.00 | 6.0% |
| 其他 | ¥350.50 | 4.2% |
```

#### 6.4.4 完整Prompt模板

```
你是一名专业的个人理财顾问，具备丰富的消费行为分析和财务规划经验。
请基于以下用户的月度账单数据，进行全面的财务分析。

{## Markdown账单数据表格（如上节模板）}

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
```

#### 6.4.5 大模型调用参数

| 参数 | 默认值 | 说明 |
|------|--------|------|
| model | deepseek-chat | DeepSeek对话模型 |
| temperature | 0.7 | 输出创造性控制 |
| max_tokens | 2048 | 最大输出令牌数 |
| top_p | 0.9 | 核采样参数 |
| stream | false | 非流式输出（等待完整结果） |

以上参数可在管理员后台AI运营模块进行配置调整。

#### 6.4.6 智能分类Prompt模板

```
你是一个消费分类助手。请根据以下消费备注文字，判断它最可能属于哪个消费分类。

可用分类列表：{从数据库动态获取分类名称列表}

消费备注："{用户输入的备注文本}"

请返回JSON格式：
{
  "categoryName": "推荐的分类名称",
  "confidence": 0.95,
  "reason": "判断依据简述"
}
```

#### 6.4.7 Qdrant向量化流程

1. 每月AI分析完成后，将用户当月消费特征提取为向量：`[餐饮占比, 交通占比, 购物占比, 娱乐占比, 住房占比, 其他占比, 总支出, 收入, 交易频次, 平均单笔金额]`
2. 使用DeepSeek Embedding或内置Embedding模型将特征转换为向量
3. 向量存入Qdrant，metadata包含 `userId` 和 `yearMonth`
4. 下次分析时检索该用户最近6个月的向量，计算消费趋势变化
5. 将趋势变化描述追加至Prompt中，实现个性化持续分析

---

## 7. 接口需求概述

### 7.1 接口设计原则

| 原则 | 说明 |
|------|------|
| RESTful风格 | URL使用名词表示资源，HTTP方法表示操作 |
| 统一响应格式 | 所有接口返回统一的JSON响应结构 |
| 权限区分 | 普通用户接口（`/api/**`）和管理员接口（`/api/admin/**`）物理路径隔离 |
| 参数校验 | 后端对所有入参进行格式和业务校验 |
| 分页规范 | 分页参数统一使用 `pageNum`（页码，从1开始）、`pageSize`（每页条数，默认20，最大100） |
| 接口文档 | 所有接口通过Knife4j自动生成文档，访问地址 `/doc.html` |

### 7.2 接口分类

| 接口分组 | 路径前缀 | 认证要求 | 说明 |
|----------|----------|----------|------|
| 公共接口 | `/api/user/register`, `/api/user/login`, `/api/admin/login` | 无 | 注册和登录 |
| 普通用户接口 | `/api/**` | 普通用户JWT | 账单、分类、预算、目标、统计、AI、文件 |
| 管理员接口 | `/api/admin/**` | 管理员JWT | 用户管理、账单管理、分类管理、AI运营、系统运维 |
| 静态资源 | `/uploads/**` | 无 | 小票图片、Excel文件等静态资源访问 |

### 7.3 统一响应格式

**成功响应**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": { ... }
}
```

**分页响应**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [ ... ],
    "total": 100,
    "pageNum": 1,
    "pageSize": 20,
    "totalPages": 5
  }
}
```

**错误响应**：
```json
{
  "code": 400,
  "message": "参数校验失败",
  "errors": [
    {"field": "username", "message": "用户名不能为空"}
  ]
}
```

**HTTP状态码约定**：

| 状态码 | 含义 |
|--------|------|
| 200 | 请求成功 |
| 400 | 参数校验失败/业务异常 |
| 401 | 未认证（Token缺失/无效/过期） |
| 403 | 无权限（角色不匹配） |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

### 7.4 接口清单

#### 7.4.1 用户认证接口

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/user/register` | 用户注册 | 无 |
| POST | `/api/user/login` | 用户登录 | 无 |
| GET | `/api/user/profile` | 获取用户信息 | 用户 |
| PUT | `/api/user/profile` | 修改用户信息 | 用户 |
| PUT | `/api/user/password` | 修改密码 | 用户 |

#### 7.4.2 账单接口

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/bill` | 新增账单 | 用户 |
| POST | `/api/bill/sync-batch` | 离线批量同步账单 | 用户 |
| PUT | `/api/bill/{id}` | 修改账单 | 用户 |
| DELETE | `/api/bill/{id}` | 删除账单 | 用户 |
| GET | `/api/bill/page` | 分页查询账单 | 用户 |
| GET | `/api/bill/search` | 多条件筛选账单 | 用户 |
| GET | `/api/bill/{id}` | 账单详情 | 用户 |
| GET | `/api/bill/export` | 导出个人账单Excel | 用户 |

#### 7.4.3 分类接口

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/api/category/list` | 获取用户可用分类列表 | 用户 |
| POST | `/api/category` | 创建自定义分类 | 用户 |
| PUT | `/api/category/{id}` | 修改自定义分类 | 用户 |
| DELETE | `/api/category/{id}` | 删除自定义分类 | 用户 |

#### 7.4.4 预算与存钱目标接口

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/api/budget/current` | 获取当月预算 | 用户 |
| GET | `/api/budget/{yearMonth}` | 获取指定月份预算 | 用户 |
| POST | `/api/budget` | 创建/覆盖月度预算 | 用户 |
| GET | `/api/save-target/list` | 获取存钱目标列表 | 用户 |
| POST | `/api/save-target` | 创建存钱目标 | 用户 |
| PUT | `/api/save-target/{id}` | 更新存钱目标（含追加存款） | 用户 |
| DELETE | `/api/save-target/{id}` | 删除存钱目标 | 用户 |

#### 7.4.5 统计接口

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/api/statistics/monthly?yearMonth=2026-06` | 月度收支统计 | 用户 |

#### 7.4.6 AI分析接口

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/ai/analyze` | AI月度账单分析 | 用户 |
| POST | `/api/ai/classify` | AI消费分类推荐 | 用户 |
| GET | `/api/ai/history` | 个人AI分析历史 | 用户 |

#### 7.4.7 文件接口

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/file/upload/receipt` | 上传小票图片 | 用户 |
| GET | `/uploads/receipts/{filename}` | 查看小票图片 | 无 |

#### 7.4.8 管理员认证接口

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/admin/login` | 管理员登录 | 无 |

#### 7.4.9 管理员用户管理接口

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/api/admin/user/page` | 全部用户分页列表 | 管理员 |
| GET | `/api/admin/user/{id}/bills` | 查看某用户账单 | 管理员 |
| PUT | `/api/admin/user/{id}/status` | 冻结/解冻用户 | 管理员 |
| PUT | `/api/admin/user/{id}/reset-password` | 重置用户密码 | 管理员 |
| GET | `/api/admin/user/export` | 导出用户数据Excel | 管理员 |

#### 7.4.10 管理员账单管理接口

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/api/admin/bill/page` | 全平台账单分页 | 管理员 |
| PUT | `/api/admin/bill/{id}` | 编辑账单 | 管理员 |
| DELETE | `/api/admin/bill/{id}` | 删除账单 | 管理员 |
| GET | `/api/admin/bill/export-all` | 全量导出账单Excel | 管理员 |
| GET | `/api/admin/bill/statistics` | 全平台消费统计 | 管理员 |

#### 7.4.11 管理员分类/预算/目标接口

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/api/admin/category/list` | 全部分类列表 | 管理员 |
| POST | `/api/admin/category` | 新增全局分类 | 管理员 |
| PUT | `/api/admin/category/{id}` | 修改分类 | 管理员 |
| DELETE | `/api/admin/category/{id}` | 删除分类 | 管理员 |
| GET | `/api/admin/budget/{userId}` | 查看用户预算 | 管理员 |
| PUT | `/api/admin/budget/{id}` | 修正用户预算 | 管理员 |
| GET | `/api/admin/save-target/{userId}` | 查看用户存钱目标 | 管理员 |
| PUT | `/api/admin/save-target/{id}` | 修正用户目标 | 管理员 |

#### 7.4.12 管理员AI运营接口

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/api/admin/ai/config` | 获取AI配置（Prompt模板、模型参数） | 管理员 |
| PUT | `/api/admin/ai/config` | 更新AI配置 | 超级管理员 |
| GET | `/api/admin/ai/records` | 全平台AI分析记录 | 管理员 |
| POST | `/api/admin/ai/qdrant/reset` | 重置用户向量数据 | 超级管理员 |

#### 7.4.13 管理员系统运维接口

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/api/admin/account/page` | 管理员列表 | 超级管理员 |
| POST | `/api/admin/account` | 新增管理员 | 超级管理员 |
| PUT | `/api/admin/account/{id}` | 修改管理员信息/角色 | 超级管理员 |
| DELETE | `/api/admin/account/{id}` | 删除管理员 | 超级管理员 |
| GET | `/api/admin/log/page` | 操作日志列表 | 管理员 |
| GET | `/api/admin/log/export` | 导出操作日志 | 管理员 |
| GET | `/api/admin/file/overview` | 文件存储概览 | 管理员 |
| DELETE | `/api/admin/file/clean` | 清理无效文件 | 超级管理员 |
| POST | `/api/admin/database/backup` | 数据库备份 | 超级管理员 |
| GET | `/api/admin/dashboard` | 后台总看板数据 | 管理员 |

---

## 8. 运行部署需求

### 8.1 局域网部署架构

```
┌─────────────────────────────────────────────────────────────────────┐
│                      WiFi 局域网 (192.168.x.x)                        │
│                                                                     │
│  ┌──────────────┐    ┌──────────────────┐    ┌──────────────┐      │
│  │ 鸿蒙手机客户端 │    │  Windows个人电脑    │    │ 浏览器客户端   │      │
│  │              │    │                  │    │              │      │
│  │ App发送HTTP  │───>│ SpringBoot:8080  │<───│ Vue管理后台   │      │
│  │ 请求至后端   │    │                  │    │ http://IP:8080│      │
│  │              │    │ MySQL:3306      │    │ /admin       │      │
│  └──────────────┘    │ Qdrant:6333     │    └──────────────┘      │
│                       │ DeepSeek(本地)  │                          │
│                       │ 文件存储(磁盘)   │                          │
│                       └──────────────────┘                          │
│                                                                     │
│  访问地址：                                                           │
│  - 手机端: http://<WLAN_IPv4>:8080/api/*                            │
│  - 后台:   http://<WLAN_IPv4>:8080/admin                            │
│  - 接口文档: http://<WLAN_IPv4>:8080/doc.html                       │
└─────────────────────────────────────────────────────────────────────┘
```

### 8.2 端口与防火墙配置

#### 8.2.1 端口规划

| 端口 | 服务 | 协议 | 说明 |
|------|------|------|------|
| 8080 | SpringBoot应用 | HTTP/TCP | 后端主服务端口，对外暴露 |
| 3306 | MySQL数据库 | TCP | 数据库服务端口，仅localhost访问 |
| 6333 | Qdrant向量库 | HTTP/TCP | Qdrant服务端口，仅localhost访问 |
| 11434 | DeepSeek(可选) | HTTP/TCP | 本地大模型API端口，仅localhost访问 |

#### 8.2.2 Windows防火墙配置

**必须执行的防火墙操作**：

1. **放行8080端口**（入站规则）：
   - 打开 Windows Defender 防火墙 → 高级设置 → 入站规则 → 新建规则
   - 规则类型：端口 → TCP → 特定本地端口：`8080`
   - 操作：允许连接
   - 配置文件：域、专用、公用（全部勾选）
   - 名称：`个人理财助手 - 后端服务端口 8080`

2. **关闭或配置第三方杀毒软件**：
   - 如果安装了360安全卫士、腾讯电脑管家、火绒等第三方安全软件
   - 检查是否拦截了8080端口的入站连接
   - 如有拦截，添加信任规则或暂时关闭端口拦截功能

3. **确认网络配置文件**：
   - 确保当前WiFi网络被Windows识别为"专用网络"（而非"公用网络"）
   - 公用网络默认限制入站连接

4. **MySQL端口（3306）不需放行**：
   - MySQL仅由本地SpringBoot应用通过localhost访问
   - 无需对外暴露3306端口，保障数据安全

#### 8.2.3 网络连通性验证

| 验证步骤 | 命令/操作 | 预期结果 |
|----------|----------|----------|
| 查看电脑WLAN IPv4地址 | `ipconfig` | 获取如 192.168.1.100 |
| 手机Ping电脑 | 手机终端工具 `ping 192.168.1.100` | 可通，延迟 < 10ms |
| 浏览器访问后端 | `http://192.168.1.100:8080/doc.html` | 可打开Knife4j文档页 |
| 手机HTTP请求 | 鸿蒙应用发起GET请求 | 返回200状态码 |

### 8.3 部署步骤

#### 8.3.1 环境准备

| 步骤 | 操作 | 说明 |
|------|------|------|
| 1 | 安装 JDK 17 | 配置 JAVA_HOME 环境变量 |
| 2 | 安装 MySQL 8.0 | 创建数据库 `finance_db`，字符集 utf8mb4 |
| 3 | 安装 Maven 3.8+ | 配置 MAVEN_HOME 环境变量 |
| 4 | 安装 Qdrant（Docker或本地二进制） | 启动Qdrant服务，监听6333端口 |
| 5 | 配置 DeepSeek 本地服务 | 确保DeepSeek API可用 |
| 6 | 构建 Vue 管理后台 | `npm run build`，输出至SpringBoot静态资源目录 |

#### 8.3.2 数据库初始化

1. 执行建表SQL脚本（包含本文档4.1节全部8张表的CREATE TABLE语句）
2. 执行系统内置分类数据初始化INSERT语句
3. 执行超级管理员初始化INSERT语句（默认账号密码通过配置文件指定）

#### 8.3.3 应用配置

SpringBoot `application.yml` 关键配置项：

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/finance_db?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai
    username: root
    password: ${DB_PASSWORD}
  servlet:
    multipart:
      max-file-size: 10MB

jwt:
  secret: ${JWT_SECRET}
  expiration: 86400000  # 24小时

langchain4j:
  deepseek:
    api-key: ${DEEPSEEK_API_KEY}
    base-url: http://localhost:11434
    model: deepseek-chat

qdrant:
  host: localhost
  port: 6333

file:
  upload:
    receipt-dir: ./uploads/receipts
  backup-dir: ./backups
```

#### 8.3.4 启动流程

1. 启动MySQL服务
2. 启动Qdrant服务
3. 启动DeepSeek服务（如为本地部署）
4. 启动SpringBoot后端服务：`java -jar finance-server.jar`
5. 确认后端启动成功：访问 `http://localhost:8080/doc.html`
6. 手机连接同一WiFi，配置手机端API地址为 `http://<电脑WLAN_IPv4>:8080`
7. 管理员浏览器访问 `http://<电脑WLAN_IPv4>:8080/admin`

### 8.4 跨域配置说明

SpringBoot后端需配置CORS允许以下来源的跨域请求：

1. 鸿蒙手机客户端HTTP请求（任意来源Origin）
2. Vue管理后台（`http://<电脑IP>:8080` 及开发时的 `http://localhost:5173`）

**CORS配置要求**：
- 允许的来源：`*`（局域网环境安全可控，使用通配符）
- 允许的方法：GET, POST, PUT, DELETE, OPTIONS
- 允许的请求头：Content-Type, Authorization
- 允许携带凭证：true（`allowCredentials = true` 时不允许 `allowedOrigins = *`，需配置具体来源列表或使用 `allowedOriginPatterns`）

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOriginPatterns("*")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);
    }
}
```

---

## 9. 需求验收标准

### 9.1 鸿蒙客户端验收标准

| 编号 | 验收项 | 验收标准 | 测试方法 |
|------|--------|----------|----------|
| AC-M-001 | 首页记账录入 | 1. 收入/支出切换正常 2. 快捷金额按钮正常填充 3. 分类列表正常加载 4. 联网提交成功，账单存入MySQL 5. 离线提交存入Preferences队列 6. 草稿保存及恢复正常 | 手动测试（联网+离线双场景） |
| AC-M-002 | 账单流水 | 1. 分页加载正常，按时间倒序 2. 下拉刷新正常 3. 上滑加载更多正常 4. 详情查看展示完整 5. 编辑保存后MySQL数据同步 6. 删除操作二次确认且MySQL同步 | 手动测试 |
| AC-M-003 | 账单筛选 | 1. 月份筛选结果正确 2. 收支类型筛选正确 3. 分类筛选正确 4. 关键词搜索匹配备注内容 | 手动测试+数据库验证 |
| AC-M-004 | Excel导出 | 1. 点击导出按钮触发下载 2. 下载的Excel文件格式正确 3. Excel数据与账单列表一致 | 手动测试+文件校验 |
| AC-M-005 | 月度统计 | 1. 概览卡片数据正确 2. 饼图分类占比与数据一致 3. 柱状图每日趋势正确 4. 月份切换数据更新 | 手动测试+数据比对 |
| AC-M-006 | 预算管理 | 1. 预算数据从后端正确加载 2. 进度条颜色按消耗比例变化 3. 接近/超出预算弹窗预警 | 手动测试+模拟数据 |
| AC-M-007 | 存钱目标 | 1. 目标列表展示正确 2. 新建目标成功存入后端 3. 追加存款金额正确累加 4. 目标完成后自动标记 | 手动测试 |
| AC-M-008 | AI分析 | 1. 点击分析后展示加载状态 2. 分析结果返回展示完整（冗余消费、坏习惯、省钱方案、复盘文案各区块） 3. 智能分类推荐正确匹配 4. 分析历史可查看 | 手动测试 |
| AC-M-009 | 主题切换 | 1. 浅色/深色切换即时生效 2. 偏好持久化（重启APP保持） | 手动测试 |
| AC-M-010 | 密码锁 | 1. 开启后进入APP需输入密码 2. 密码正确方可进入 3. 密码错误拒绝进入 | 手动测试 |
| AC-M-011 | 关于页面 | 1. 姓名：胡宪棋 2. 班级：软件2413 3. 学号：202421332084 4. 功能介绍正确展示 | 手动检查 |
| AC-M-012 | 用户注册登录 | 1. 注册成功后可登录 2. 重复用户名注册被拒绝 3. 自动登录正常（Token未过期） 4. Token过期跳转登录页 | 手动测试 |
| AC-M-013 | 无本地RDB | 代码检查确认无RelationalStore/SQLite/任何本地数据库依赖 | 代码审查 |

### 9.2 后端服务验收标准

| 编号 | 验收项 | 验收标准 | 测试方法 |
|------|--------|----------|----------|
| AC-B-001 | 用户注册/登录 | 1. 注册成功返回200 2. 登录成功返回JWT 3. 密码BCrypt加密存储 4. 重复注册返回错误 | Postman/Knife4j接口测试 |
| AC-B-002 | JWT鉴权 | 1. 无Token返回401 2. 过期Token返回401 3. 有效Token正常访问 4. 普通用户Token不能访问/admin接口 | 接口测试 |
| AC-B-003 | 账单CRUD | 1. 新增成功写入bill表 2. 修改正确更新 3. 删除物理删除 4. 分页正确 5. 数据隔离（用户A看不到用户B的账单） | 接口测试+数据库验证 |
| AC-B-004 | 账单筛选 | 1. 月份、类型、分类、关键词多条件筛选准确 2. 组合筛选结果正确 | 接口测试+SQL验证 |
| AC-B-005 | 分类管理 | 1. 用户创建自定义分类成功 2. 删除有账单关联的分类被拒绝 3. 系统分类不可删除 | 接口测试 |
| AC-B-006 | 预算管理 | 1. 创建/覆盖月度预算成功 2. 查询指定月份预算正确 3. 预算预警计算正确（80%/100%阈值） | 接口测试+计算验证 |
| AC-B-007 | 存钱目标 | 1. 创建目标成功 2. 追加存款累加正确 3. 达标自动标记完成 | 接口测试 |
| AC-B-008 | 月度统计 | 1. 总收入/总支出/结余计算正确 2. 分类占比计算正确 3. 每日收支聚合正确 | 接口测试+SQL计算比对 |
| AC-B-009 | AI分析 | 1. Markdown表格格式正确构造 2. DeepSeek调用成功 3. 返回JSON格式符合预期 4. 向量存入Qdrant成功 | 接口测试+日志验证 |
| AC-B-010 | AI分类推荐 | 1. 传入备注文本返回分类推荐 2. 推荐分类在分类列表中 | 接口测试 |
| AC-B-011 | 文件上传 | 1. 图片上传成功存入磁盘 2. 路径写入bill表 3. 图片可通过URL访问 4. 非法文件类型被拒绝 | 接口测试+文件系统检查 |
| AC-B-012 | Excel导出 | 1. Excel文件正确生成 2. 列和数据与账单一致 3. 支持中文不乱码 | 接口测试+文件下载验证 |
| AC-B-013 | 管理员登录 | 1. 管理员独立登录成功 2. 普通用户凭据无法登录管理员接口 | 接口测试 |
| AC-B-014 | 操作日志 | 1. 管理员增删改操作全部记录 2. 日志含操作人、时间、接口、参数 3. 日志可查询可导出 | 接口测试+数据库验证 |
| AC-B-015 | CORS | 1. 鸿蒙端HTTP请求正常 2. 浏览器跨域请求正常 3. OPTIONS预检请求正常 | 接口测试 |
| AC-B-016 | Knife4j文档 | 1. 访问/doc.html可打开接口文档 2. 接口按分组正确分类 3. 接口描述、参数、响应正确 | 手动检查 |
| AC-B-017 | 异常处理 | 1. 业务异常返回友好中文提示 2. 参数校验失败返回字段级错误 3. 系统异常不暴露堆栈给客户端 | 接口测试 |
| AC-B-018 | 离线同步接口 | 1. /api/bill/sync-batch 接收批量账单 2. uuid去重 3. 批量写入MySQL成功 | 接口测试 |

### 9.3 管理员后台验收标准

| 编号 | 验收项 | 验收标准 | 测试方法 |
|------|--------|----------|----------|
| AC-A-001 | 管理员登录 | 1. 账号密码登录成功 2. 角色权限正确识别 3. 路由守卫正常拦截 | 手动测试 |
| AC-A-002 | 后台看板 | 1. 总用户数正确 2. 当月账单量正确 3. 活跃用户统计正确 4. 趋势图表渲染正确 | 手动测试+数据验证 |
| AC-A-003 | 用户管理 | 1. 用户列表加载正常 2. 搜索/筛选正常 3. 冻结/解冻生效（冻结用户无法登录） 4. 重置密码后新密码可登录 5. 导出Excel正确 | 手动测试 |
| AC-A-004 | 账单管理 | 1. 全平台账单列表加载正常 2. 多条件筛选正确 3. 小票原图可查看 4. 编辑/删除生效 5. 全量导出Excel正确 | 手动测试 |
| AC-A-005 | 消费统计 | 1. 全平台统计图表渲染正确 2. 用户消费排名正确 | 手动测试+数据验证 |
| AC-A-006 | 分类管理 | 1. 分类列表正确 2. 新增/编辑/删除分类正常 | 手动测试 |
| AC-A-007 | AI运营 | 1. Prompt模板可编辑保存 2. 模型参数可调整 3. AI分析记录可查看 | 手动测试 |
| AC-A-008 | 管理员账号管理 | 1. 仅超级管理员可访问 2. 新增/编辑/删除管理员正常 3. 超级管理员不可删除自身 | 手动测试 |
| AC-A-009 | 操作日志 | 1. 日志列表正确 2. 可按条件筛选 3. 可导出Excel | 手动测试 |
| AC-A-010 | 文件管理 | 1. 文件概览（空间、数量）正确 2. 无效文件清理正常 | 手动测试 |
| AC-A-011 | 数据库备份 | 1. 备份触发正确 2. 备份文件可下载 3. SQL文件可导入恢复 | 手动测试 |
| AC-A-012 | 角色菜单控制 | 1. 运营管理员不可见"管理员账号管理"菜单 2. 运营管理员不可见"系统运维"部分菜单 | 手动测试 |

### 9.4 特殊机制验收标准

| 编号 | 验收项 | 验收标准 | 测试方法 |
|------|--------|----------|----------|
| AC-S-001 | 离线缓存 | 1. 断网后记账成功存入Preferences 2. 离线队列数据格式正确 3. 离线时可用功能范围符合设计 | 手动测试（断开WiFi） |
| AC-S-002 | 联网同步 | 1. 恢复网络后自动触发同步 2. 同步成功后MySQL数据正确 3. 同步完成后Preferences队列清除 4. 手动同步按钮功能正常 5. 同步断点续传 | 手动测试（模拟断网→联网） |
| AC-S-003 | JWT权限隔离 | 1. 普通用户Token不可访问/api/admin/** 2. 管理员Token不可访问/api/**（普通用户接口） 3. 运营管理员不可访问超管接口 | 自动化接口测试 |
| AC-S-004 | AI Markdown分析 | 1. 检查服务端日志确认Prompt包含Markdown表格 2. AI返回结果格式符合预期JSON结构 3. Qdrant向量写入可验证 | 日志检查+接口测试 |
| AC-S-005 | 数据隔离 | 1. 用户A的账单查询不包含用户B的数据 2. 用户A的预算/目标不包含用户B的数据 | 接口测试+数据库验证 |

### 9.5 部署验收标准

| 编号 | 验收项 | 验收标准 | 测试方法 |
|------|--------|----------|----------|
| AC-D-001 | 局域网连通 | 1. 手机→电脑Ping通 2. 手机HTTP请求返回正常 3. 浏览器访问/admin正常 | 实际网络环境测试 |
| AC-D-002 | 端口放行 | 1. 8080端口防火墙规则已添加 2. 手机可访问8080端口 | telnet测试 |
| AC-D-003 | 跨域访问 | 1. 鸿蒙客户端跨域请求正常 2. 浏览器跨域请求正常 | 接口测试 |
| AC-D-004 | 文件访问 | 1. 小票图片可通过URL访问 2. Excel文件可下载 | 手动测试 |
| AC-D-005 | 数据库初始化 | 1. 8张表全部创建成功 2. 系统分类初始化数据正确 3. 超级管理员账号初始化正确 | 数据库检查 |

---

## 附录A：自检审查确认清单

本文档在编写完成后，已依据用户要求的自检审查机制逐条核查，确认结果如下：

| 编号 | 自检项 | 核查结果 | 说明 |
|------|--------|----------|------|
| SC-001 | 是否遗漏任何PMF功能 | ✅ 通过 | 已对照demand.md逐行核查，全部功能需求均已覆盖 |
| SC-002 | 是否错误加入手机本地数据库RDB | ✅ 通过 | 全文无SQLite/RelationalStore/Room等本地数据库依赖；手机端仅使用Preferences做临时缓存 |
| SC-003 | 是否区分管理员/普通用户权限 | ✅ 通过 | 明确三层权限体系：超级管理员→运营管理员→普通用户；接口物理路径隔离（/api/** 与 /api/admin/**） |
| SC-004 | 是否完整写清离线同步逻辑 | ✅ 通过 | 第6.1节详细描述离线缓存机制，第6.2节详细描述联网自动同步机制，含存储结构、同步流程、冲突处理、状态提示 |
| SC-005 | 是否明确AI使用MD结构化输入 | ✅ 通过 | 第6.4节详细描述AI Markdown结构化分析机制，含Prompt模板、表格格式、向量化流程 |
| SC-006 | 是否全部数据持久化在MySQL | ✅ 通过 | 第4章明确全部8张数据表在MySQL，第4.2节明确"全部业务数据唯一持久化位置为MySQL" |
| SC-007 | 是否包含部署、安全、性能规范 | ✅ 通过 | 第5章非功能需求（性能/安全/兼容性/可扩展性），第8章运行部署需求（局域网/端口/防火墙/跨域） |

---

## 附录B：数据表汇总

| 序号 | 表名 | 说明 | 数据表类型 |
|------|------|------|-----------|
| 1 | sys_user | 普通用户表 | 核心表 |
| 2 | sys_admin | 管理员表 | 核心表 |
| 3 | bill | 账单表 | 核心表 |
| 4 | category | 消费分类表 | 核心表 |
| 5 | budget | 月度预算表 | 核心表 |
| 6 | save_target | 存钱目标表 | 核心表 |
| 7 | admin_operation_log | 管理员操作日志表 | 增强表（必要） |
| 8 | ai_analysis_record | AI分析记录表 | 增强表（必要） |

---

## 附录C：接口总量统计

| 接口分组 | 接口数量 | 路径前缀 |
|----------|----------|----------|
| 用户认证 | 5 | /api/user/** |
| 账单 | 8 | /api/bill/** |
| 分类 | 4 | /api/category/** |
| 预算 | 3 | /api/budget/** |
| 存钱目标 | 4 | /api/save-target/** |
| 统计 | 1 | /api/statistics/** |
| AI分析 | 3 | /api/ai/** |
| 文件 | 2 | /api/file/** |
| 管理员认证 | 1 | /api/admin/login |
| 管理员用户管理 | 5 | /api/admin/user/** |
| 管理员账单管理 | 5 | /api/admin/bill/** |
| 管理员分类预算 | 8 | /api/admin/category|budget|save-target/** |
| 管理员AI运营 | 4 | /api/admin/ai/** |
| 管理员系统运维 | 7 | /api/admin/account|log|file|database|dashboard/** |
| **总计** | **60** | |

---

*文档结束*

| 编制 | 审核 | 批准 |
|------|------|------|
| 胡宪棋 | — | — |
| 2026-06-23 | — | — |
