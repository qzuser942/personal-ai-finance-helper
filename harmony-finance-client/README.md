# 个人智能理财系统 - 鸿蒙客户端

> **作者**：胡宪棋 | **班级**：软件2413 | **学号**：202421332084  
> **技术栈**：HarmonyOS ArkTS + Router + Preferences + Canvas图表 + Axios网络封装  
> **后端**：SpringBoot 3.x (对接项目 `personal-finance-server`)

---

## 一、项目简介

本应用是基于华为鸿蒙HarmonyOS NEXT平台开发的个人智能理财系统客户端，为用户提供移动端记账、账单管理、统计分析、预算管理、存钱目标追踪和AI智能理财分析等完整功能。

### 核心特性
- ✅ **快速记账**：金额快捷输入、分类选择、备注、拍照上传
- ✅ **离线记账**：无网络时自动离线缓存，联网后自动同步至后端
- ✅ **账单流水**：分页加载、多维度筛选、Excel导出
- ✅ **月度统计**：饼图（Canvas）+ 柱状图（Canvas）直观展示财务数据
- ✅ **预算预警**：可视化进度条，超80%预警
- ✅ **存钱目标**：创建目标、追加存款、进度追踪
- ✅ **AI理财分析**：DeepSeek大模型月度财务诊断
- ✅ **深浅主题**：一键切换浅色/深色主题
- ✅ **密码锁**：4-6位数字密码保护隐私
- ✅ **JWT鉴权**：自动携带Token、过期自动跳转登录

---

## 二、如何修改后端IP地址

打开文件 `entry/src/main/ets/store/AppState.ets`，修改第23行：

```typescript
// 修改为你电脑的实际局域网IPv4地址（运行 ipconfig 查看）
static BASE_URL: string = 'http://192.168.1.100:8080';
```

> **重要**：手机和电脑必须连接同一WiFi网络。
> Windows查看IP命令：`ipconfig` → 找到"无线局域网适配器 WLAN"的IPv4地址。

**所有需要修改IP地址的文件**（统一替换为你的电脑IP）：
1. `entry/src/main/ets/store/AppState.ets` - 第23行
2. `entry/src/main/ets/network/api/fileApi.ts` - 第18行（图片URL拼接）

---

## 三、导入与运行

### 3.1 开发环境要求
- **DevEco Studio**：4.0 Release 及以上
- **HarmonyOS SDK**：API 10+
- **测试设备**：HarmonyOS 4.0+ 真机或模拟器

### 3.2 导入步骤
1. 启动 DevEco Studio
2. 文件 → 打开 → 选择本项目根目录 `harmony-finance-client`
3. 等待 Gradle/Hvigor 同步完成
4. 修改后端IP地址（见第二章）
5. 连接鸿蒙手机或启动模拟器
6. 点击运行（▶）按钮

### 3.3 运行前提
- 后端SpringBoot服务已启动（电脑上运行 `personal-finance-server`）
- MySQL数据库已初始化（`finance_db`）
- 手机与电脑在同一WiFi网络下
- 电脑防火墙允许8080端口入站连接

---

## 四、项目工程结构

```
harmony-finance-client/
├── build-profile.json5          # 项目构建配置
├── hvigorfile.ts                # Hvigor构建脚本
├── oh-package.json5             # 项目依赖
├── README.md                    # 本说明文档
├── AppScope/
│   └── app.json5                # 应用配置（包名、版本）
└── entry/
    ├── build-profile.json5      # Entry模块构建配置
    ├── hvigorfile.ts
    ├── oh-package.json5
    └── src/main/
        ├── module.json5         # 模块配置（权限、Ability）
        ├── ets/
        │   ├── entryability/
        │   │   └── EntryAbility.ets    # 应用入口
        │   ├── pages/                   # 业务页面
        │   │   ├── auth/                # 登录/注册
        │   │   │   ├── LoginPage.ets
        │   │   │   └── RegisterPage.ets
        │   │   ├── index/               # 主页（Tab导航+记账）
        │   │   │   ├── MainPage.ets
        │   │   │   └── IndexPage.ets
        │   │   ├── bill/                # 账单流水/详情
        │   │   │   ├── BillListPage.ets
        │   │   │   └── BillDetailPage.ets
        │   │   ├── statistics/          # 月度统计图表
        │   │   │   └── StatisticsPage.ets
        │   │   ├── budget/              # 预算与存钱目标
        │   │   │   └── BudgetPage.ets
        │   │   ├── ai/                  # AI理财分析
        │   │   │   └── AiAnalysisPage.ets
        │   │   └── settings/            # 设置与关于
        │   │       └── SettingsPage.ets
        │   ├── components/              # 复用组件
        │   │   ├── common/              # 通用组件
        │   │   │   ├── LoadingDialog.ets
        │   │   │   ├── ConfirmDialog.ets
        │   │   │   └── EmptyView.ets
        │   │   ├── bill/                # 账单Item组件
        │   │   │   └── BillItem.ets
        │   │   ├── chart/               # Canvas图表
        │   │   │   ├── PieChart.ets
        │   │   │   └── BarChart.ets
        │   │   └── budget/              # 预算进度
        │   │       └── BudgetProgress.ets
        │   ├── model/                   # 数据模型
        │   │   ├── UserModel.ets
        │   │   ├── BillModel.ets
        │   │   ├── CategoryModel.ets
        │   │   ├── BudgetModel.ets
        │   │   ├── SaveTargetModel.ets
        │   │   ├── StatisticsModel.ets
        │   │   └── AiModel.ets
        │   ├── network/                 # 网络请求层
        │   │   ├── request.ts           # 统一封装（JWT、超时、拦截）
        │   │   └── api/                 # 接口调用
        │   │       ├── authApi.ts
        │   │       ├── billApi.ts
        │   │       ├── categoryApi.ts
        │   │       ├── budgetApi.ts
        │   │       ├── saveTargetApi.ts
        │   │       ├── statisticsApi.ts
        │   │       ├── aiApi.ts
        │   │       └── fileApi.ts
        │   ├── utils/                   # 工具类
        │   │   ├── storage.ts           # Preferences封装
        │   │   ├── date.ts              # 日期金额格式化
        │   │   └── toast.ts             # 消息提示
        │   └── store/                   # 全局状态
        │       └── AppState.ets
        └── resources/
            └── base/
                ├── element/
                │   ├── string.json
                │   └── color.json
                └── profile/
                    └── main_pages.json  # 路由注册
```

---

## 五、模块功能说明

### 5.1 认证模块（auth）
- **LoginPage**：账号密码登录、记住密码、JWT自动缓存
- **RegisterPage**：新用户注册、前端格式校验

### 5.2 首页记账（index）
- **MainPage**：底部Tab导航容器（记账/账单/统计/目标/AI/设置）
- **离线记账核心逻辑**：
  - 网络正常 → 直接提交后端
  - 网络异常 → 存入Preferences离线队列（max 100条）
  - 联网恢复 → 自动批量同步（APP启动时触发）

### 5.3 账单流水（bill）
- **BillListPage**：分页加载、下拉刷新、月份/类型/关键词筛选、长按删除、Excel导出
- **BillDetailPage**：查看详情、编辑账单、删除账单

### 5.4 月度统计（statistics）
- **StatisticsPage**：收入/支出/结余卡片、支出分类饼图（Canvas）、每日趋势柱状图（Canvas）、月份切换

### 5.5 预算与存钱目标（budget）
- **BudgetPage**：月度总预算+分类子预算设置、可视化进度条、预算预警（≥80%）、存钱目标创建/追加/归档

### 5.6 AI分析（ai）
- **AiAnalysisPage**：一键月度财务诊断、AI分析结果展示（冗余消费/不良习惯/省钱方案/复盘文案）、分析历史查看

### 5.7 设置（settings）
- **SettingsPage**：深浅色主题切换、数字密码锁、手动离线同步、账单备份、关于页面（作者信息+功能介绍）、退出登录

---

## 六、技术要求遵守情况

| 要求 | 状态 | 说明 |
|------|------|------|
| 无本地RDB数据库 | ✅ | 仅使用Preferences缓存Token/离线队列/主题/密码 |
| INTERNET权限声明 | ✅ | module.json5中声明所有必要权限 |
| JWT自动携带 | ✅ | 请求拦截器自动添加Authorization Header |
| Token过期处理 | ✅ | 自动清除登录态并跳转登录页 |
| 离线记账+自动同步 | ✅ | Preferences队列 + APP启动自动同步 + 手动同步 |
| 统一错误处理 | ✅ | request.ts统一拦截、Toast提示 |
| 深浅色主题 | ✅ | AppState管理 + Preferences持久化 |
| 密码锁 | ✅ | 4-6位数字密码 + Preferences存储 |
| 标准分层架构 | ✅ | pages/components/model/network/utils/store |

---

## 七、后端接口对照

所有接口调用严格对照 `API接口文档说明书.md`，涉及以下35个普通用户端接口：

| 模块 | 接口数 | 核心接口 |
|------|--------|----------|
| 认证 | 5 | register, login, profile, update, changePwd |
| 离线同步 | 1 | sync-batch |
| 账单 | 7 | CRUD, page, search, export |
| 分类 | 4 | list, create, update, delete |
| 预算 | 3 | current, byMonth, create |
| 存钱目标 | 4 | list, create, update, delete |
| 统计 | 1 | monthly |
| AI | 4 | analyze, classify, history, historyDetail |
| 文件 | 1 | upload receipt |

---

*文档版本：V1.0 | 2026-06-23 | 胡宪棋*
