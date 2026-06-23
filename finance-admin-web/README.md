# 个人智能理财系统 - Vue3管理后台

> **作者**：胡宪棋 | **班级**：软件2413 | **学号**：202421332084  
> **技术栈**：Vue3 + Vite + Element Plus + ECharts + Axios + Pinia  
> **对接后端**：SpringBoot 3.x `http://<电脑IP>:8080`

---

## 一、快速启动

### 1.1 安装依赖
```bash
cd finance-admin-web
npm install
```

### 1.2 修改后端地址
编辑 `vite.config.js`，修改代理目标IP为你电脑的局域网IPv4地址：
```js
server: {
  proxy: {
    '/api': { target: 'http://你的电脑IP:8080', changeOrigin: true },
    '/uploads': { target: 'http://你的电脑IP:8080', changeOrigin: true }
  }
}
```
也可修改 `src/api/request.js` 第9行的 `BASE_URL` 变量。

### 1.3 启动开发服务器
```bash
npm run dev
```
访问 `http://localhost:3000` 即可进入管理后台。

### 1.4 默认管理员账号
- 账号：`admin`
- 密码：`admin123`

---

## 二、功能模块一览

| 模块 | 功能 | 接口前缀 |
|------|------|----------|
| **登录** | 管理员独立登录，JWT存储 | `/api/admin/login` |
| **首页看板** | 总用户/活跃/账单量/交易额统计、ECharts趋势图、消费排行 | `/api/admin/dashboard` |
| **用户管理** | 分页列表、冻结/解冻、重置密码、Excel导出 | `/api/admin/user/**` |
| **账单管理** | 全平台账单分页、多条件筛选、编辑修正、删除、全量导出 | `/api/admin/bill/**` |
| **分类管理** | 全局分类CRUD、系统内置/用户自定义分类展示 | `/api/admin/category/**` |
| **预算目标** | 查看/修正用户预算、查看/修正存钱目标进度 | `/api/admin/budget/**` |
| **AI配置** | 模型参数调整、Prompt模板管理、全平台AI分析记录查看 | `/api/admin/ai/**` |
| **操作日志** | 所有管理员操作留痕、时间筛选、关键词搜索、Excel导出 | `/api/admin/log/**` |
| **管理员账号** | 新增/编辑/删除管理员、角色分配、密码重置（仅超管） | `/api/admin/account/**` |
| **文件管理** | 存储概览、数据库一键备份、备份历史 | `/api/admin/file/**` |

---

## 三、项目结构

```
finance-admin-web/
├── package.json                  # 依赖配置
├── vite.config.js                # Vite配置+API代理
├── index.html                    # 入口HTML
├── README.md
└── src/
    ├── main.js                   # 应用入口
    ├── App.vue                   # 根组件
    ├── api/                      # 接口模块（9个文件）
    │   ├── request.js            # Axios封装(JWT+拦截)
    │   ├── auth.js               # 管理员登录
    │   ├── dashboard.js          # 看板统计
    │   ├── user.js               # 用户管理
    │   ├── bill.js               # 账单管理
    │   ├── category.js           # 分类管理
    │   ├── budget.js             # 预算目标
    │   ├── ai.js                 # AI配置
    │   ├── log.js                # 操作日志
    │   └── system.js             # 系统运维
    ├── assets/styles/            # 全局样式
    │   ├── variables.scss        # SCSS变量
    │   └── global.scss           # 全局样式(毛玻璃/粒子/流光)
    ├── layout/                   # 布局组件
    │   ├── MainLayout.vue        # 主容器
    │   ├── SideBar.vue           # 侧边栏导航
    │   └── HeaderBar.vue         # 顶栏(面包屑/主题/用户)
    ├── router/index.js           # 路由+权限守卫
    ├── store/                    # Pinia状态
    │   ├── index.js
    │   ├── user.js               # 管理员Token/角色
    │   └── app.js                # 主题/侧边栏
    ├── utils/                    # 工具
    │   ├── date.js               # 日期格式化
    │   └── excel.js              # Excel导出
    └── views/                    # 页面（9个）
        ├── login/LoginView.vue
        ├── dashboard/DashboardView.vue
        ├── user/UserListView.vue
        ├── bill/BillListView.vue
        ├── category/CategoryManage.vue
        ├── budget/BudgetManage.vue
        ├── ai/AiConfigView.vue
        ├── log/OperationLog.vue
        └── system/
            ├── AdminAccount.vue
            └── FileManager.vue
```

---

## 四、技术特性

### 视觉设计
- **毛玻璃流光**：登录页和应用卡片使用 backdrop-filter + 渐变流光边框
- **粒子动画**：全局背景Simplex Noise风格浮动粒子
- **深浅色主题**：一键切换暗黑模式，Preferences持久化
- **高级紫蓝色系**：`#6C5CE7 → #A29BFE → #55EFC4` 渐变配色

### 权限体系
- 超级管理员(SUPER_ADMIN)：全部权限，含管理员账号管理
- 运营管理员(OPERATOR)：用户/账单/分类/日志管理，不可操作系统运维
- 路由守卫 + Pinia状态 + 菜单动态渲染

### 数据安全
- Axios拦截器自动携带JWT Admin Token
- 401/403自动跳转登录页
- 所有删除操作二次确认弹窗
- 所有管理员操作自动记录至后端操作日志表

---

## 五、后端接口对照

全部31个管理员接口严格对照 `API接口文档说明书.md` 第11章：

- 认证(1): `POST /api/admin/login`
- 看板(1): `GET /api/admin/dashboard`
- 用户(5): page, status, reset-password, export, bills
- 账单(5): page, update, delete, statistics, export-all
- 分类(4): list, create, update, delete
- 预算目标(4): budget/{userId}, budget/{id}, save-target/{userId}, save-target/{id}
- AI(5): config, config(PUT), records, records/{id}, qdrant/reset
- 日志(2): log/page, log/export
- 管理员(5): account CRUD, reset-password
- 文件运维(4): file/overview, file/clean, database/backup, database/backup/log

---

## 六、打包部署

```bash
npm run build
```
生成的 `dist/` 目录可直接部署至Nginx或后端静态资源目录。

---

*文档版本：V1.0 | 2026-06-23 | 胡宪棋*
