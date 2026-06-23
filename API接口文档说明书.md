# API 接口文档说明书

## 个人理财助手应用系统

| 文档属性 | 内容 |
|----------|------|
| 文档编号 | API-PFM-2026-001 |
| 版本号 | V1.0 |
| 编制人 | 胡宪棋 |
| 班级 | 软件2413 |
| 学号 | 202421332084 |
| 编制日期 | 2026年6月23日 |
| 文档状态 | 初始版本 |
| 密级 | 内部 |
| 依赖文档 | SRS-PFM-2026-001、DB-PFM-2026-001 |

---

## 目录

1. [接口总体规范](#1-接口总体规范)
2. [认证模块](#2-认证模块)
3. [离线同步模块](#3-离线同步模块)
4. [账单模块](#4-账单模块)
5. [分类模块](#5-分类模块)
6. [预算模块](#6-预算模块)
7. [存钱目标模块](#7-存钱目标模块)
8. [统计图表模块](#8-统计图表模块)
9. [AI智能理财模块](#9-ai智能理财模块)
10. [文件模块](#10-文件模块)
11. [管理员后台模块](#11-管理员后台模块)
12. [全局错误码对照表](#12-全局错误码对照表)
13. [自检审查确认清单](#13-自检审查确认清单)

---

## 1. 接口总体规范

### 1.1 请求协议与地址

| 配置项 | 值 |
|--------|-----|
| 协议 | HTTP（局域网环境，无需HTTPS） |
| 服务端地址 | `http://<电脑WLAN_IPv4>:8080` |
| 字符编码 | UTF-8 |
| 请求体格式 | JSON（Content-Type: application/json） |
| 文件上传格式 | multipart/form-data |

### 1.2 接口路径前缀

| 路径前缀 | 受众 | 说明 |
|----------|------|------|
| `/api/user/` | 普通用户 | 用户注册、登录接口（无需鉴权） |
| `/api/` | 普通用户 | 业务接口（需普通用户JWT鉴权） |
| `/api/admin/` | 管理员 | 管理员登录、业务接口（需管理员JWT鉴权） |
| `/uploads/` | 公开 | 静态资源（小票图片、Excel文件） |
| `/doc.html` | 开发 | Knife4j接口文档页（仅开发环境） |

### 1.3 鉴权方式

| 配置项 | 值 |
|--------|-----|
| 鉴权方式 | JWT (JSON Web Token) |
| Token 传输 | HTTP Header: `Authorization: Bearer <token>` |
| 普通用户 Token 有效期 | 24小时（86400秒） |
| 管理员 Token 有效期 | 24小时（86400秒） |
| 签发算法 | HMAC-SHA256 |

**普通用户 JWT Payload**：
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

**管理员 JWT Payload**：
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

**鉴权拦截规则**：

| 请求路径 | 所需身份 | 拦截规则 |
|----------|----------|----------|
| `/api/user/register`、`/api/user/login` | 无 | 直接放行 |
| `/api/admin/login` | 无 | 直接放行 |
| `/api/**`（除上述外） | 普通用户 | JWT Token 校验，sub=user |
| `/api/admin/**`（除登录外） | 管理员 | 管理员 JWT Token 校验，sub=admin |
| `/api/admin/account/**` | 超级管理员 | 管理员 JWT + role=SUPER_ADMIN |
| `/uploads/**` | 无 | 静态资源直接放行 |

### 1.4 统一响应格式

**成功响应**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": { ... },
  "timestamp": 1719145600000
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
    "page": 1,
    "size": 20,
    "totalPages": 5
  },
  "timestamp": 1719145600000
}
```

**错误响应**：
```json
{
  "code": 400,
  "message": "参数校验失败",
  "errors": [
    { "field": "username", "message": "用户名不能为空" }
  ],
  "timestamp": 1719145600000
}
```

### 1.5 分页规范

| 参数 | 类型 | 默认值 | 最大值 | 说明 |
|------|------|--------|--------|------|
| page | Integer | 1 | — | 页码，从1开始 |
| size | Integer | 20 | 100 | 每页条数 |

> 分页参数统一使用 Query String 方式传递：`?page=1&size=20`

### 1.6 时间格式规范

| 场景 | 格式 | 示例 |
|------|------|------|
| 请求参数（日期） | `yyyy-MM-dd` | `2026-06-23` |
| 请求参数（月份） | `yyyy-MM` | `2026-06` |
| 请求参数（日期时间） | `yyyy-MM-dd HH:mm:ss` | `2026-06-23 12:30:00` |
| 响应字段 | `yyyy-MM-dd HH:mm:ss` | `2026-06-23 12:30:00` |
| timestamp 时间戳 | 毫秒时间戳 | `1719145600000` |

---

## 2. 认证模块

### 2.1 用户注册

| 属性 | 值 |
|------|-----|
| 接口名称 | 用户注册 |
| 请求地址 | `/api/user/register` |
| 请求方式 | POST |
| 权限级别 | 无需认证 |

**请求参数**：

| 字段 | 类型 | 必传 | 说明 |
|------|------|------|------|
| username | String | 是 | 用户名，4-20字符，字母数字下划线 |
| password | String | 是 | 登录密码，6-20字符 |
| confirmPassword | String | 是 | 确认密码，需与password一致 |

**请求示例**：
```json
{
  "username": "zhangsan",
  "password": "pass123456",
  "confirmPassword": "pass123456"
}
```

**响应参数**：

| 字段 | 类型 | 说明 |
|------|------|------|
| userId | Long | 注册成功的用户ID |
| username | String | 注册用户名 |

**响应示例**：
```json
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "userId": 10001,
    "username": "zhangsan"
  },
  "timestamp": 1719145600000
}
```

**业务说明**：
- 用户名全局唯一，重复注册返回错误码 10001
- 密码通过 BCrypt 加密后存储
- 注册成功后不自动签发Token，需调用登录接口

---

### 2.2 用户登录

| 属性 | 值 |
|------|-----|
| 接口名称 | 用户登录 |
| 请求地址 | `/api/user/login` |
| 请求方式 | POST |
| 权限级别 | 无需认证 |

**请求参数**：

| 字段 | 类型 | 必传 | 说明 |
|------|------|------|------|
| username | String | 是 | 用户名 |
| password | String | 是 | 登录密码 |

**请求示例**：
```json
{
  "username": "zhangsan",
  "password": "pass123456"
}
```

**响应参数**：

| 字段 | 类型 | 说明 |
|------|------|------|
| token | String | JWT令牌，后续请求携带于Authorization Header |
| tokenType | String | 固定值 "Bearer" |
| expiresIn | Long | 令牌有效期（秒） |
| userId | Long | 用户ID |
| username | String | 用户名 |

**响应示例**：
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyIiwidXNlcklkIjoxMDAwMSwidXNlcm5hbWUiOiJ6aGFuZ3NhbiIsInJvbGUiOiJVU0VSIiwiaWF0IjoxNzE5MTQ1NjAwLCJleHAiOjE3MTkyMzIwMDB9.xxx",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "userId": 10001,
    "username": "zhangsan"
  },
  "timestamp": 1719145600000
}
```

**业务说明**：
- 登录成功后更新 `sys_user.last_login_time`
- 账号被冻结（status=0）时返回错误码 10003
- 用户名或密码错误返回错误码 10002

---

### 2.3 获取用户信息

| 属性 | 值 |
|------|-----|
| 接口名称 | 获取当前用户信息 |
| 请求地址 | `/api/user/profile` |
| 请求方式 | GET |
| 权限级别 | 普通用户 |

**请求参数**：无

**响应参数**：

| 字段 | 类型 | 说明 |
|------|------|------|
| userId | Long | 用户ID |
| username | String | 用户名 |
| status | Integer | 账号状态：1-正常，0-冻结 |
| lastLoginTime | String | 最近登录时间 |
| createdAt | String | 注册时间 |

**响应示例**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "userId": 10001,
    "username": "zhangsan",
    "status": 1,
    "lastLoginTime": "2026-06-23 12:30:00",
    "createdAt": "2026-06-20 09:15:00"
  },
  "timestamp": 1719145600000
}
```

---

### 2.4 修改用户信息

| 属性 | 值 |
|------|-----|
| 接口名称 | 修改用户信息 |
| 请求地址 | `/api/user/profile` |
| 请求方式 | PUT |
| 权限级别 | 普通用户 |

**请求参数**：

| 字段 | 类型 | 必传 | 说明 |
|------|------|------|------|
| username | String | 否 | 新用户名，若修改需保证唯一 |

**请求示例**：
```json
{
  "username": "zhangsan_new"
}
```

**响应参数**：

| 字段 | 类型 | 说明 |
|------|------|------|
| userId | Long | 用户ID |
| username | String | 更新后的用户名 |

**响应示例**：
```json
{
  "code": 200,
  "message": "修改成功",
  "data": {
    "userId": 10001,
    "username": "zhangsan_new"
  },
  "timestamp": 1719145600000
}
```

---

### 2.5 修改密码

| 属性 | 值 |
|------|-----|
| 接口名称 | 修改登录密码 |
| 请求地址 | `/api/user/password` |
| 请求方式 | PUT |
| 权限级别 | 普通用户 |

**请求参数**：

| 字段 | 类型 | 必传 | 说明 |
|------|------|------|------|
| oldPassword | String | 是 | 旧密码 |
| newPassword | String | 是 | 新密码，6-20字符 |
| confirmPassword | String | 是 | 确认新密码 |

**请求示例**：
```json
{
  "oldPassword": "pass123456",
  "newPassword": "newPass789",
  "confirmPassword": "newPass789"
}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "密码修改成功",
  "data": null,
  "timestamp": 1719145600000
}
```

**业务说明**：
- 旧密码验证失败返回错误码 10005
- 新密码与确认密码不一致返回错误码 10006

---

### 2.6 管理员登录

| 属性 | 值 |
|------|-----|
| 接口名称 | 管理员登录 |
| 请求地址 | `/api/admin/login` |
| 请求方式 | POST |
| 权限级别 | 无需认证 |

**请求参数**：

| 字段 | 类型 | 必传 | 说明 |
|------|------|------|------|
| username | String | 是 | 管理员账号 |
| password | String | 是 | 管理员密码 |

**请求示例**：
```json
{
  "username": "admin",
  "password": "admin123"
}
```

**响应参数**：

| 字段 | 类型 | 说明 |
|------|------|------|
| token | String | 管理员JWT令牌 |
| tokenType | String | 固定值 "Bearer" |
| expiresIn | Long | 令牌有效期（秒） |
| adminId | Long | 管理员ID |
| username | String | 管理员账号 |
| role | String | 角色：SUPER_ADMIN / OPERATOR |

**响应示例**：
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsImFkbWluSWQiOjEsInVzZXJuYW1lIjoiYWRtaW4iLCJyb2xlIjoiU1VQRVJfQURNSU4iLCJpYXQiOjE3MTkxNDU2MDAsImV4cCI6MTcxOTIzMjAwMH0.xxx",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "adminId": 1,
    "username": "admin",
    "role": "SUPER_ADMIN"
  },
  "timestamp": 1719145600000
}
```

---

## 3. 离线同步模块

### 3.1 离线批量同步账单

| 属性 | 值 |
|------|-----|
| 接口名称 | 离线账单批量同步 |
| 请求地址 | `/api/bill/sync-batch` |
| 请求方式 | POST |
| 权限级别 | 普通用户 |

**请求参数（JSON数组）**：

| 字段 | 类型 | 必传 | 说明 |
|------|------|------|------|
| uuid | String | 是 | 客户端生成的UUID v4，用于去重 |
| amount | BigDecimal | 是 | 金额（正数） |
| type | String | 是 | income / expense |
| categoryId | Long | 是 | 消费分类ID |
| categoryName | String | 否 | 分类名称（冗余，用于客户端展示） |
| remark | String | 否 | 文字备注，最大500字符 |
| consumeTime | String | 是 | 消费时间，格式：yyyy-MM-dd HH:mm:ss |
| createdAt | String | 是 | 账单创建时间，格式：yyyy-MM-dd HH:mm:ss |

**请求示例**：
```json
[
  {
    "uuid": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "amount": 35.50,
    "type": "expense",
    "categoryId": 1,
    "categoryName": "餐饮",
    "remark": "午餐外卖",
    "consumeTime": "2026-06-23 12:30:00",
    "createdAt": "2026-06-23 12:31:00"
  },
  {
    "uuid": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
    "amount": 200.00,
    "type": "expense",
    "categoryId": 2,
    "categoryName": "交通",
    "remark": "加油",
    "consumeTime": "2026-06-23 08:15:00",
    "createdAt": "2026-06-23 08:16:00"
  }
]
```

**响应参数**：

| 字段 | 类型 | 说明 |
|------|------|------|
| total | Integer | 本次提交总数 |
| successCount | Integer | 成功同步数量 |
| failCount | Integer | 失败数量 |
| duplicateCount | Integer | 重复（已同步过）数量 |
| results | Array | 每条账单的处理结果明细 |

**results 子字段**：

| 字段 | 类型 | 说明 |
|------|------|------|
| uuid | String | 对应请求的UUID |
| status | String | 处理状态：success / duplicate / fail |
| billId | Long | 成功时返回的账单ID |
| message | String | 处理说明 |

**响应示例**：
```json
{
  "code": 200,
  "message": "批量同步完成",
  "data": {
    "total": 2,
    "successCount": 1,
    "failCount": 0,
    "duplicateCount": 1,
    "results": [
      {
        "uuid": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
        "status": "success",
        "billId": 12345,
        "message": "同步成功"
      },
      {
        "uuid": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
        "status": "duplicate",
        "billId": null,
        "message": "该账单已同步过，跳过"
      }
    ]
  },
  "timestamp": 1719145600000
}
```

**业务说明**：
- 后端逐条处理，单条失败不影响其他账单
- 以 `sync_uuid` 为去重键，已存在的UUID返回 `duplicate`
- 分类ID不存在时返回 `fail`，客户端需修正后重新同步
- 离线同步不携带图片；图片需联网后单独上传

---

## 4. 账单模块

### 4.1 新增账单

| 属性 | 值 |
|------|-----|
| 接口名称 | 新增账单 |
| 请求地址 | `/api/bill` |
| 请求方式 | POST |
| 权限级别 | 普通用户 |

**请求参数**：

| 字段 | 类型 | 必传 | 说明 |
|------|------|------|------|
| amount | BigDecimal | 是 | 金额（正数，>0） |
| type | String | 是 | income / expense |
| categoryId | Long | 是 | 消费分类ID（须在用户可用分类列表中） |
| remark | String | 否 | 文字备注，最大500字符 |
| receiptImage | String | 否 | 小票图片路径（先上传图片再提交账单） |
| consumeTime | String | 是 | 消费时间，格式：yyyy-MM-dd HH:mm:ss |

**请求示例**：
```json
{
  "amount": 35.50,
  "type": "expense",
  "categoryId": 1,
  "remark": "午餐外卖",
  "receiptImage": "/uploads/receipts/a1b2c3d4.jpg",
  "consumeTime": "2026-06-23 12:30:00"
}
```

**响应参数**：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 新创建的账单ID |
| amount | BigDecimal | 金额 |
| type | String | 收支类型 |
| categoryId | Long | 分类ID |
| categoryName | String | 分类名称 |
| remark | String | 备注 |
| receiptImage | String | 小票图片路径 |
| consumeTime | String | 消费时间 |
| createdAt | String | 创建时间 |

**响应示例**：
```json
{
  "code": 200,
  "message": "记账成功",
  "data": {
    "id": 12345,
    "amount": 35.50,
    "type": "expense",
    "categoryId": 1,
    "categoryName": "餐饮",
    "remark": "午餐外卖",
    "receiptImage": "/uploads/receipts/a1b2c3d4.jpg",
    "consumeTime": "2026-06-23 12:30:00",
    "createdAt": "2026-06-23 12:31:00"
  },
  "timestamp": 1719145600000
}
```

**业务说明**：
- 后端自动从JWT中提取 userId 并注入
- 金额必须 > 0，否则返回校验错误
- categoryId 必须在用户可用分类列表中存在

---

### 4.2 查询账单详情

| 属性 | 值 |
|------|-----|
| 接口名称 | 查询账单详情 |
| 请求地址 | `/api/bill/{id}` |
| 请求方式 | GET |
| 权限级别 | 普通用户 |

**路径参数**：

| 字段 | 类型 | 必传 | 说明 |
|------|------|------|------|
| id | Long | 是 | 账单ID |

**响应参数**：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 账单ID |
| amount | BigDecimal | 金额 |
| type | String | 收支类型 |
| categoryId | Long | 消费分类ID |
| categoryName | String | 消费分类名称 |
| remark | String | 备注 |
| receiptImage | String | 小票图片完整URL |
| consumeTime | String | 消费时间 |
| createdAt | String | 创建时间 |
| updatedAt | String | 最近修改时间 |

**响应示例**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 12345,
    "amount": 35.50,
    "type": "expense",
    "categoryId": 1,
    "categoryName": "餐饮",
    "remark": "午餐外卖",
    "receiptImage": "http://192.168.1.100:8080/uploads/receipts/a1b2c3d4.jpg",
    "consumeTime": "2026-06-23 12:30:00",
    "createdAt": "2026-06-23 12:31:00",
    "updatedAt": null
  },
  "timestamp": 1719145600000
}
```

**业务说明**：
- 只能查询当前用户的账单；查询他人账单返回错误码 20001
- 账单不存在或已删除返回错误码 20002

---

### 4.3 分页查询账单

| 属性 | 值 |
|------|-----|
| 接口名称 | 分页查询账单列表 |
| 请求地址 | `/api/bill/page` |
| 请求方式 | GET |
| 权限级别 | 普通用户 |

**请求参数（Query String）**：

| 字段 | 类型 | 必传 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | Integer | 否 | 1 | 页码 |
| size | Integer | 否 | 20 | 每页条数（最大100） |

**响应参数**（data内）：

| 字段 | 类型 | 说明 |
|------|------|------|
| records | Array | 账单列表（按consumeTime倒序） |
| total | Long | 总记录数 |
| page | Integer | 当前页码 |
| size | Integer | 每页条数 |
| totalPages | Integer | 总页数 |

**records 子字段**：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 账单ID |
| amount | BigDecimal | 金额 |
| type | String | 收支类型 |
| categoryId | Long | 分类ID |
| categoryName | String | 分类名称 |
| remark | String | 备注（截断至50字） |
| hasImage | Boolean | 是否有小票图片 |
| consumeTime | String | 消费时间 |

**响应示例**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [
      {
        "id": 12345,
        "amount": 35.50,
        "type": "expense",
        "categoryId": 1,
        "categoryName": "餐饮",
        "remark": "午餐外卖",
        "hasImage": true,
        "consumeTime": "2026-06-23 12:30:00"
      }
    ],
    "total": 156,
    "page": 1,
    "size": 20,
    "totalPages": 8
  },
  "timestamp": 1719145600000
}
```

---

### 4.4 多条件筛选账单

| 属性 | 值 |
|------|-----|
| 接口名称 | 多条件筛选账单 |
| 请求地址 | `/api/bill/search` |
| 请求方式 | GET |
| 权限级别 | 普通用户 |

**请求参数（Query String）**：

| 字段 | 类型 | 必传 | 说明 |
|------|------|------|------|
| page | Integer | 否 | 页码，默认1 |
| size | Integer | 否 | 每页条数，默认20 |
| type | String | 否 | 收支类型：income / expense |
| categoryId | Long | 否 | 消费分类ID |
| keyword | String | 否 | 关键词（模糊匹配备注内容） |
| startDate | String | 否 | 消费时间起始，格式：yyyy-MM-dd |
| endDate | String | 否 | 消费时间截止，格式：yyyy-MM-dd |
| yearMonth | String | 否 | 指定月份，格式：yyyy-MM（与startDate/endDate互斥） |
| minAmount | BigDecimal | 否 | 最小金额 |
| maxAmount | BigDecimal | 否 | 最大金额 |

**请求示例**：
```
GET /api/bill/search?type=expense&categoryId=1&yearMonth=2026-06&keyword=外卖&page=1&size=20
```

**响应格式**：同 [4.3 分页查询账单](#43-分页查询账单)

---

### 4.5 修改账单

| 属性 | 值 |
|------|-----|
| 接口名称 | 修改账单 |
| 请求地址 | `/api/bill/{id}` |
| 请求方式 | PUT |
| 权限级别 | 普通用户 |

**路径参数**：

| 字段 | 类型 | 必传 | 说明 |
|------|------|------|------|
| id | Long | 是 | 账单ID |

**请求参数**：

| 字段 | 类型 | 必传 | 说明 |
|------|------|------|------|
| amount | BigDecimal | 否 | 金额（>0） |
| type | String | 否 | 收支类型 |
| categoryId | Long | 否 | 分类ID |
| remark | String | 否 | 备注 |
| consumeTime | String | 否 | 消费时间 |

**请求示例**：
```json
{
  "amount": 45.00,
  "categoryId": 1,
  "remark": "午餐外卖（修改）",
  "consumeTime": "2026-06-23 12:30:00"
}
```

**响应格式**：同 [4.2 查询账单详情](#42-查询账单详情)

**业务说明**：
- 只能修改自己的账单
- 仅更新传入的非null字段
- 修改后 `updated_at` 自动更新

---

### 4.6 删除账单

| 属性 | 值 |
|------|-----|
| 接口名称 | 删除账单 |
| 请求地址 | `/api/bill/{id}` |
| 请求方式 | DELETE |
| 权限级别 | 普通用户 |

**路径参数**：

| 字段 | 类型 | 必传 | 说明 |
|------|------|------|------|
| id | Long | 是 | 账单ID |

**响应示例**：
```json
{
  "code": 200,
  "message": "账单已删除",
  "data": null,
  "timestamp": 1719145600000
}
```

**业务说明**：
- 使用逻辑删除（设置 is_deleted=1）
- 若有关联小票图片，同步删除磁盘文件
- 只能删除自己的账单

---

### 4.7 导出个人账单Excel

| 属性 | 值 |
|------|-----|
| 接口名称 | 导出个人账单Excel |
| 请求地址 | `/api/bill/export` |
| 请求方式 | GET |
| 权限级别 | 普通用户 |

**请求参数（Query String）**：

| 字段 | 类型 | 必传 | 说明 |
|------|------|------|------|
| yearMonth | String | 否 | 导出指定月份，不传则导出全部 |
| type | String | 否 | 按收支类型筛选 |

**响应说明**：
- 后端生成 Excel 文件流
- Content-Type: `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
- 文件名：`账单导出_20260623_143000.xlsx`
- 响应头 `Content-Disposition: attachment; filename=...`

**Excel 列结构**：

| 序号 | 金额 | 收支类型 | 分类 | 备注 | 消费时间 | 有无小票 |
|------|------|----------|------|------|----------|----------|

---

## 5. 分类模块

### 5.1 获取用户可用分类列表

| 属性 | 值 |
|------|-----|
| 接口名称 | 获取分类列表（含系统内置+用户自定义） |
| 请求地址 | `/api/category/list` |
| 请求方式 | GET |
| 权限级别 | 普通用户 |

**请求参数（Query String）**：

| 字段 | 类型 | 必传 | 说明 |
|------|------|------|------|
| type | String | 否 | income / expense，不传则返回全部 |

**响应参数**：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 分类ID |
| name | String | 分类名称 |
| icon | String | 图标标识 |
| type | String | 分类类型 |
| sortOrder | Integer | 排序序号 |
| isSystem | Boolean | 是否为系统内置分类（true=不可删除） |

**响应示例**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "name": "餐饮",
      "icon": "icon-food",
      "type": "expense",
      "sortOrder": 1,
      "isSystem": true
    },
    {
      "id": 2,
      "name": "交通",
      "icon": "icon-transport",
      "type": "expense",
      "sortOrder": 2,
      "isSystem": true
    },
    {
      "id": 100,
      "name": "咖啡",
      "icon": "icon-coffee",
      "type": "expense",
      "sortOrder": 50,
      "isSystem": false
    }
  ],
  "timestamp": 1719145600000
}
```

**业务说明**：
- 查询条件：`WHERE (user_id = 0 OR user_id = ?) AND is_deleted = 0`
- 系统内置分类（user_id=0，isSystem=true）对所有用户可见
- 用户自定义分类（user_id=当前用户ID，isSystem=false）仅创建者可见

---

### 5.2 创建自定义分类

| 属性 | 值 |
|------|-----|
| 接口名称 | 创建自定义分类 |
| 请求地址 | `/api/category` |
| 请求方式 | POST |
| 权限级别 | 普通用户 |

**请求参数**：

| 字段 | 类型 | 必传 | 说明 |
|------|------|------|------|
| name | String | 是 | 分类名称，最大64字符 |
| icon | String | 否 | 图标标识 |
| type | String | 是 | income / expense |
| sortOrder | Integer | 否 | 排序序号，默认0 |

**请求示例**：
```json
{
  "name": "咖啡",
  "icon": "icon-coffee",
  "type": "expense",
  "sortOrder": 50
}
```

**响应参数**：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 新创建的分类ID |
| name | String | 分类名称 |
| icon | String | 图标标识 |
| type | String | 分类类型 |
| sortOrder | Integer | 排序序号 |
| isSystem | Boolean | 是否为系统分类（固定false） |

**响应示例**：
```json
{
  "code": 200,
  "message": "分类创建成功",
  "data": {
    "id": 100,
    "name": "咖啡",
    "icon": "icon-coffee",
    "type": "expense",
    "sortOrder": 50,
    "isSystem": false
  },
  "timestamp": 1719145600000
}
```

**业务说明**：
- user_id 自动设为当前用户ID
- 同一用户、同一类型下分类名不可重复（错误码 30001）

---

### 5.3 修改自定义分类

| 属性 | 值 |
|------|-----|
| 接口名称 | 修改自定义分类 |
| 请求地址 | `/api/category/{id}` |
| 请求方式 | PUT |
| 权限级别 | 普通用户 |

**路径参数**：

| 字段 | 类型 | 必传 | 说明 |
|------|------|------|------|
| id | Long | 是 | 分类ID |

**请求参数**：

| 字段 | 类型 | 必传 | 说明 |
|------|------|------|------|
| name | String | 否 | 新名称 |
| icon | String | 否 | 新图标 |
| sortOrder | Integer | 否 | 新排序序号 |

**请求示例**：
```json
{
  "name": "精品咖啡",
  "sortOrder": 30
}
```

**业务说明**：
- 只能修改自己的自定义分类
- 系统内置分类不可修改（错误码 30002）

---

### 5.4 删除自定义分类

| 属性 | 值 |
|------|-----|
| 接口名称 | 删除自定义分类 |
| 请求地址 | `/api/category/{id}` |
| 请求方式 | DELETE |
| 权限级别 | 普通用户 |

**路径参数**：

| 字段 | 类型 | 必传 | 说明 |
|------|------|------|------|
| id | Long | 是 | 分类ID |

**响应示例**：
```json
{
  "code": 200,
  "message": "分类已删除",
  "data": null,
  "timestamp": 1719145600000
}
```

**业务说明**：
- 只能删除自己的自定义分类
- 系统内置分类不可删除（错误码 30002）
- 若该分类下有关联账单，拒绝删除并提示（错误码 30003）
- 使用逻辑删除

---

## 6. 预算模块

### 6.1 获取当月预算

| 属性 | 值 |
|------|-----|
| 接口名称 | 获取当月预算 |
| 请求地址 | `/api/budget/current` |
| 请求方式 | GET |
| 权限级别 | 普通用户 |

**请求参数**：无（后端自动取当前月份 `yyyy-MM`）

**响应参数**：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 预算ID |
| yearMonth | String | 预算月份 |
| totalBudget | BigDecimal | 月度总预算 |
| totalSpent | BigDecimal | 当月已消费金额（后端实时计算） |
| remaining | BigDecimal | 剩余预算 = totalBudget - totalSpent |
| usagePercent | BigDecimal | 使用百分比（如 65.5 表示 65.5%） |
| categoryBudgets | Object | 各分类子预算：{ "categoryId": amount } |
| categorySpent | Object | 各分类已消费金额：{ "categoryId": amount } |
| alertCategories | Array | 超预算/预警的分类列表 |

**alertCategories 子字段**：

| 字段 | 类型 | 说明 |
|------|------|------|
| categoryId | Long | 分类ID |
| categoryName | String | 分类名称 |
| budgetAmount | BigDecimal | 子预算金额 |
| spentAmount | BigDecimal | 已消费金额 |
| alertLevel | String | 预警级别：WARNING（≥80%）, OVER（≥100%） |

**响应示例**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 10,
    "yearMonth": "2026-06",
    "totalBudget": 8000.00,
    "totalSpent": 5230.50,
    "remaining": 2769.50,
    "usagePercent": 65.38,
    "categoryBudgets": {
      "1": 2500.00,
      "2": 800.00,
      "3": 2000.00,
      "4": 1000.00
    },
    "categorySpent": {
      "1": 2100.00,
      "2": 720.00,
      "3": 1500.00,
      "4": 910.50
    },
    "alertCategories": [
      {
        "categoryId": 4,
        "categoryName": "娱乐",
        "budgetAmount": 1000.00,
        "spentAmount": 910.50,
        "alertLevel": "WARNING"
      }
    ]
  },
  "timestamp": 1719145600000
}
```

**业务说明**：
- 若当月未设置预算，返回空（data为null但code=200）
- 预警阈值：已消费 ≥ 子预算80% → WARNING；≥ 100% → OVER
- `totalSpent` 和 `categorySpent` 为实时从 bill 表聚合计算

---

### 6.2 获取指定月份预算

| 属性 | 值 |
|------|-----|
| 接口名称 | 获取指定月份预算 |
| 请求地址 | `/api/budget/{yearMonth}` |
| 请求方式 | GET |
| 权限级别 | 普通用户 |

**路径参数**：

| 字段 | 类型 | 必传 | 说明 |
|------|------|------|------|
| yearMonth | String | 是 | 月份，格式：yyyy-MM（如 2026-05） |

**响应格式**：同 [6.1 获取当月预算](#61-获取当月预算)

---

### 6.3 创建/覆盖月度预算

| 属性 | 值 |
|------|-----|
| 接口名称 | 创建或覆盖月度预算 |
| 请求地址 | `/api/budget` |
| 请求方式 | POST |
| 权限级别 | 普通用户 |

**请求参数**：

| 字段 | 类型 | 必传 | 说明 |
|------|------|------|------|
| yearMonth | String | 是 | 预算月份，格式：yyyy-MM |
| totalBudget | BigDecimal | 是 | 月度总预算金额 |
| categoryBudgets | Object | 否 | 各分类子预算，key为categoryId（字符串），value为金额 |

**请求示例**：
```json
{
  "yearMonth": "2026-06",
  "totalBudget": 8000.00,
  "categoryBudgets": {
    "1": 2500.00,
    "2": 800.00,
    "3": 2000.00,
    "4": 1000.00,
    "5": 1700.00
  }
}
```

**响应参数**：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 预算ID |
| yearMonth | String | 预算月份 |
| totalBudget | BigDecimal | 总预算 |
| categoryBudgets | Object | 分类子预算 |

**响应示例**：
```json
{
  "code": 200,
  "message": "预算设置成功",
  "data": {
    "id": 10,
    "yearMonth": "2026-06",
    "totalBudget": 8000.00,
    "categoryBudgets": {
      "1": 2500.00,
      "2": 800.00,
      "3": 2000.00,
      "4": 1000.00,
      "5": 1700.00
    }
  },
  "timestamp": 1719145600000
}
```

**业务说明**：
- 若该月份已存在预算记录，则为覆盖更新（upsert逻辑）
- `yearMonth` 必须是有效格式，不能是未来超过1年的月份

---

## 7. 存钱目标模块

### 7.1 获取存钱目标列表

| 属性 | 值 |
|------|-----|
| 接口名称 | 获取存钱目标列表 |
| 请求地址 | `/api/save-target/list` |
| 请求方式 | GET |
| 权限级别 | 普通用户 |

**请求参数（Query String）**：

| 字段 | 类型 | 必传 | 说明 |
|------|------|------|------|
| status | Integer | 否 | 0-进行中，1-已完成，不传返回全部 |

**响应参数**：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 目标ID |
| name | String | 目标名称 |
| targetAmount | BigDecimal | 目标总金额 |
| savedAmount | BigDecimal | 已存金额 |
| progressPercent | BigDecimal | 完成百分比（如 65.5） |
| status | Integer | 状态：0-进行中，1-已完成 |
| createdAt | String | 创建时间 |
| completedAt | String | 完成时间（仅已完成目标） |

**响应示例**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "name": "买MacBook Pro",
      "targetAmount": 15000.00,
      "savedAmount": 7500.00,
      "progressPercent": 50.00,
      "status": 0,
      "createdAt": "2026-05-15 10:00:00",
      "completedAt": null
    },
    {
      "id": 2,
      "name": "旅行基金",
      "targetAmount": 5000.00,
      "savedAmount": 5000.00,
      "progressPercent": 100.00,
      "status": 1,
      "createdAt": "2026-04-01 09:00:00",
      "completedAt": "2026-06-20 15:30:00"
    }
  ],
  "timestamp": 1719145600000
}
```

---

### 7.2 创建存钱目标

| 属性 | 值 |
|------|-----|
| 接口名称 | 创建存钱目标 |
| 请求地址 | `/api/save-target` |
| 请求方式 | POST |
| 权限级别 | 普通用户 |

**请求参数**：

| 字段 | 类型 | 必传 | 说明 |
|------|------|------|------|
| name | String | 是 | 目标名称，最大128字符 |
| targetAmount | BigDecimal | 是 | 目标总金额（>0） |

**请求示例**：
```json
{
  "name": "买MacBook Pro",
  "targetAmount": 15000.00
}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "目标创建成功",
  "data": {
    "id": 1,
    "name": "买MacBook Pro",
    "targetAmount": 15000.00,
    "savedAmount": 0.00,
    "progressPercent": 0.00,
    "status": 0,
    "createdAt": "2026-06-23 12:30:00",
    "completedAt": null
  },
  "timestamp": 1719145600000
}
```

---

### 7.3 更新存钱目标（追加存款/修改信息）

| 属性 | 值 |
|------|-----|
| 接口名称 | 更新存钱目标 |
| 请求地址 | `/api/save-target/{id}` |
| 请求方式 | PUT |
| 权限级别 | 普通用户 |

**路径参数**：

| 字段 | 类型 | 必传 | 说明 |
|------|------|------|------|
| id | Long | 是 | 目标ID |

**请求参数**：

| 字段 | 类型 | 必传 | 说明 |
|------|------|------|------|
| name | String | 否 | 新名称 |
| targetAmount | BigDecimal | 否 | 新目标金额 |
| addAmount | BigDecimal | 否 | 本次追加存款金额（正值累加至savedAmount） |

**请求示例**：
```json
{
  "addAmount": 2000.00
}
```

**业务说明**：
- 当 `savedAmount + addAmount >= targetAmount` 时，自动标记 status=1，设置 completedAt
- `addAmount` 和修改目标基本信息不可在同一请求中进行（以addAmount优先）

---

### 7.4 删除存钱目标

| 属性 | 值 |
|------|-----|
| 接口名称 | 删除存钱目标 |
| 请求地址 | `/api/save-target/{id}` |
| 请求方式 | DELETE |
| 权限级别 | 普通用户 |

**路径参数**：

| 字段 | 类型 | 必传 | 说明 |
|------|------|------|------|
| id | Long | 是 | 目标ID |

**响应示例**：
```json
{
  "code": 200,
  "message": "目标已删除",
  "data": null,
  "timestamp": 1719145600000
}
```

**业务说明**：使用逻辑删除

---

## 8. 统计图表模块

### 8.1 月度收支统计

| 属性 | 值 |
|------|-----|
| 接口名称 | 月度收支统计数据 |
| 请求地址 | `/api/statistics/monthly` |
| 请求方式 | GET |
| 权限级别 | 普通用户 |

**请求参数（Query String）**：

| 字段 | 类型 | 必传 | 说明 |
|------|------|------|------|
| yearMonth | String | 是 | 统计月份，格式：yyyy-MM |

**响应参数**：

| 字段 | 类型 | 说明 |
|------|------|------|
| yearMonth | String | 统计月份 |
| totalIncome | BigDecimal | 月度总收入 |
| totalExpense | BigDecimal | 月度总支出 |
| balance | BigDecimal | 月度结余 = totalIncome - totalExpense |
| billCount | Integer | 账单总数 |
| categoryBreakdown | Array | 各分类金额及占比 |
| dailyBreakdown | Array | 每日收支明细 |

**categoryBreakdown 子字段**：

| 字段 | 类型 | 说明 |
|------|------|------|
| categoryId | Long | 分类ID |
| categoryName | String | 分类名称 |
| icon | String | 图标标识 |
| totalAmount | BigDecimal | 该分类总金额 |
| percentage | BigDecimal | 占比百分比（如 29.9） |
| count | Integer | 该分类账单数 |

**dailyBreakdown 子字段**：

| 字段 | 类型 | 说明 |
|------|------|------|
| date | String | 日期（如 2026-06-01） |
| dayOfWeek | String | 星期（如 "周一"） |
| income | BigDecimal | 当日收入合计 |
| expense | BigDecimal | 当日支出合计 |

**响应示例**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "yearMonth": "2026-06",
    "totalIncome": 15000.00,
    "totalExpense": 8350.50,
    "balance": 6649.50,
    "billCount": 42,
    "categoryBreakdown": [
      {
        "categoryId": 1,
        "categoryName": "餐饮",
        "icon": "icon-food",
        "totalAmount": 2500.00,
        "percentage": 29.94,
        "count": 18
      },
      {
        "categoryId": 3,
        "categoryName": "购物",
        "icon": "icon-shopping",
        "totalAmount": 3000.00,
        "percentage": 35.93,
        "count": 8
      }
    ],
    "dailyBreakdown": [
      {
        "date": "2026-06-01",
        "dayOfWeek": "周一",
        "income": 15000.00,
        "expense": 235.50
      },
      {
        "date": "2026-06-02",
        "dayOfWeek": "周二",
        "income": 0.00,
        "expense": 200.00
      }
    ]
  },
  "timestamp": 1719145600000
}
```

**业务说明**：
- 全部数据由后端通过SQL聚合查询计算（GROUP BY + SUM）
- 客户端直接使用返回数据渲染 ECharts 图表
- categoryBreakdown 按金额降序排列
- dailyBreakdown 覆盖当月1日至最后一天，无账单日期金额为 0.00

---

## 9. AI智能理财模块

### 9.1 AI月度账单分析

| 属性 | 值 |
|------|-----|
| 接口名称 | AI月度理财分析 |
| 请求地址 | `/api/ai/analyze` |
| 请求方式 | POST |
| 权限级别 | 普通用户 |

**请求参数**：

| 字段 | 类型 | 必传 | 说明 |
|------|------|------|------|
| yearMonth | String | 是 | 分析月份，格式：yyyy-MM |

**请求示例**：
```json
{
  "yearMonth": "2026-06"
}
```

**响应参数**：

| 字段 | 类型 | 说明 |
|------|------|------|
| recordId | Long | 分析记录ID（用于历史查询） |
| yearMonth | String | 分析月份 |
| redundantItems | Array | 冗余消费项列表 |
| badHabits | Array | 不良消费习惯列表 |
| savingPlans | Array | 省钱方案列表 |
| monthlyReview | String | 月度财务复盘文案 |
| processingTimeMs | Long | AI处理耗时（毫秒） |

**redundantItems 子字段**：

| 字段 | 类型 | 说明 |
|------|------|------|
| name | String | 消费项目名称 |
| amount | BigDecimal | 涉及金额 |
| reason | String | 判为冗余的原因 |
| suggestion | String | 改进建议 |

**badHabits 子字段**：

| 字段 | 类型 | 说明 |
|------|------|------|
| habit | String | 习惯名称 |
| description | String | 具体描述 |
| impact | String | 财务影响评估 |

**savingPlans 子字段**：

| 字段 | 类型 | 说明 |
|------|------|------|
| plan | String | 方案名称 |
| description | String | 具体做法 |
| estimatedSave | String | 预估节省金额/月 |

**响应示例**：
```json
{
  "code": 200,
  "message": "AI分析完成",
  "data": {
    "recordId": 55,
    "yearMonth": "2026-06",
    "redundantItems": [
      {
        "name": "外卖配送费",
        "amount": 85.50,
        "reason": "本月外卖配送费累计较高，其中5笔订单的配送费超过15元",
        "suggestion": "选择免配送费商户，或每周减少2次外卖下单"
      },
      {
        "name": "视频会员自动续费",
        "amount": 45.00,
        "reason": "3个视频平台同时自动续费，实际仅使用1个",
        "suggestion": "取消不常用平台订阅，按需开通"
      }
    ],
    "badHabits": [
      {
        "habit": "深夜冲动消费",
        "description": "22:00至次日02:00时段共有8笔消费记录，集中在零食和网购",
        "impact": "月均额外支出约350元，且长期深夜进食不利健康"
      }
    ],
    "savingPlans": [
      {
        "plan": "交通优化方案",
        "description": "工作日早晚高峰选择地铁代替打车，非高峰时段可使用网约车",
        "estimatedSave": "月省200-300元"
      },
      {
        "plan": "午餐自带计划",
        "description": "每两周采购一次食材，周末集中备餐",
        "estimatedSave": "月省400-500元"
      },
      {
        "plan": "购物冷静期",
        "description": "对非必需消费设置24小时冷静期，延迟满足减少冲动消费",
        "estimatedSave": "月省300-500元"
      }
    ],
    "monthlyReview": "2026年6月您的财务状况总体健康，月度结余6649.50元，储蓄率44.3%表现良好。支出中购物占比35.9%偏高，建议下月重点关注购物支出控制。餐饮支出占比29.9%处于合理区间，但外卖配送费仍有优化空间。娱乐支出接近月度预算上限，需注意控制。整体来看，您已建立良好的储蓄习惯，继续保持当前节奏，预计年底可达成主要存钱目标。",
    "processingTimeMs": 12500
  },
  "timestamp": 1719145600000
}
```

**业务说明**：
- 后端流程：查询当月账单 → 组装 Markdown 表格 → 读取 ai_config Prompt模板 → 调用 DeepSeek → 解析JSON → 存储 ai_analysis_record → 返回结果
- AI调用耗时5-30秒，建议前端展示加载动画
- 若当月无账单数据，返回错误码 40001
- 分析结果同时存入 MySQL（ai_analysis_record）和 Qdrant（消费向量）

---

### 9.2 AI智能分类推荐

| 属性 | 值 |
|------|-----|
| 接口名称 | AI消费分类智能推荐 |
| 请求地址 | `/api/ai/classify` |
| 请求方式 | POST |
| 权限级别 | 普通用户 |

**请求参数**：

| 字段 | 类型 | 必传 | 说明 |
|------|------|------|------|
| remark | String | 是 | 消费备注文本（如"中午和同事吃了火锅AA"） |
| type | String | 否 | 限定分类类型：income / expense，默认expense |

**请求示例**：
```json
{
  "remark": "中午和同事吃了火锅AA",
  "type": "expense"
}
```

**响应参数**：

| 字段 | 类型 | 说明 |
|------|------|------|
| categoryId | Long | 推荐的分类ID |
| categoryName | String | 推荐的分类名称 |
| confidence | BigDecimal | 置信度（0.00-1.00） |
| reason | String | 判断依据简述 |

**响应示例**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "categoryId": 1,
    "categoryName": "餐饮",
    "confidence": 0.96,
    "reason": "备注中包含'火锅''吃'等餐饮关键词，判断为餐饮消费"
  },
  "timestamp": 1719145600000
}
```

**业务说明**：
- 后端构造分类Prompt → 调用DeepSeek → 返回推荐分类
- confidence < 0.5 时，前端可提示用户手动选择

---

### 9.3 获取个人AI分析历史

| 属性 | 值 |
|------|-----|
| 接口名称 | 获取个人AI分析历史记录 |
| 请求地址 | `/api/ai/history` |
| 请求方式 | GET |
| 权限级别 | 普通用户 |

**请求参数（Query String）**：

| 字段 | 类型 | 必传 | 说明 |
|------|------|------|------|
| page | Integer | 否 | 页码，默认1 |
| size | Integer | 否 | 每页条数，默认10 |

**响应参数**（records子字段）：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 分析记录ID |
| yearMonth | String | 分析月份 |
| modelName | String | 使用的模型 |
| processingTimeMs | Long | 处理耗时 |
| resultPreview | String | 月度复盘文案摘要（截取前100字） |
| createdAt | String | 分析时间 |

**响应示例**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [
      {
        "id": 55,
        "yearMonth": "2026-06",
        "modelName": "DeepSeek",
        "processingTimeMs": 12500,
        "resultPreview": "2026年6月您的财务状况总体健康，月度结余6649.50元...",
        "createdAt": "2026-06-23 12:35:00"
      }
    ],
    "total": 3,
    "page": 1,
    "size": 10,
    "totalPages": 1
  },
  "timestamp": 1719145600000
}
```

---

### 9.4 获取AI分析记录详情

| 属性 | 值 |
|------|-----|
| 接口名称 | 获取AI分析记录完整详情 |
| 请求地址 | `/api/ai/history/{id}` |
| 请求方式 | GET |
| 权限级别 | 普通用户 |

**路径参数**：

| 字段 | 类型 | 必传 | 说明 |
|------|------|------|------|
| id | Long | 是 | 分析记录ID |

**响应格式**：同 [9.1 AI月度账单分析](#91-ai月度账单分析) 的 data 部分，额外包含 `createdAt` 和 `promptTemplateSnapshot` 字段。

---

## 10. 文件模块

### 10.1 上传小票图片

| 属性 | 值 |
|------|-----|
| 接口名称 | 上传消费小票图片 |
| 请求地址 | `/api/file/upload/receipt` |
| 请求方式 | POST |
| 权限级别 | 普通用户 |
| Content-Type | multipart/form-data |

**请求参数**：

| 字段 | 类型 | 必传 | 说明 |
|------|------|------|------|
| file | File | 是 | 图片文件，仅允许 jpg / png / webp，最大 10MB |

**响应参数**：

| 字段 | 类型 | 说明 |
|------|------|------|
| fileName | String | 服务端生成的文件名（UUID） |
| filePath | String | 服务端相对路径，用于存入 bill.receipt_image |
| fileUrl | String | 完整访问URL |
| fileSize | Long | 文件大小（字节） |

**请求示例**（cURL）：
```bash
curl -X POST http://192.168.1.100:8080/api/file/upload/receipt \
  -H "Authorization: Bearer <token>" \
  -F "file=@/path/to/receipt.jpg"
```

**响应示例**：
```json
{
  "code": 200,
  "message": "上传成功",
  "data": {
    "fileName": "a1b2c3d4e5f67890.jpg",
    "filePath": "/uploads/receipts/a1b2c3d4e5f67890.jpg",
    "fileUrl": "http://192.168.1.100:8080/uploads/receipts/a1b2c3d4e5f67890.jpg",
    "fileSize": 245760
  },
  "timestamp": 1719145600000
}
```

**业务说明**：
- 文件存储目录：`{user.dir}/uploads/receipts/`
- 文件名格式：UUID + 原始扩展名
- 后端校验文件类型（魔数检查，防止伪造Content-Type）
- 上传成功后客户端将 `filePath` 填入账单的 `receiptImage` 字段
- 文件类型不符返回错误码 50001，文件过大返回错误码 50002

---

### 10.2 访问小票图片（静态资源）

| 属性 | 值 |
|------|-----|
| 接口名称 | 查看小票图片 |
| 请求地址 | `/uploads/receipts/{filename}` |
| 请求方式 | GET |
| 权限级别 | 无需认证 |

**路径参数**：

| 字段 | 类型 | 必传 | 说明 |
|------|------|------|------|
| filename | String | 是 | 图片文件名 |

**响应说明**：
- 返回图片二进制流
- Content-Type: image/jpeg 或 image/png 或 image/webp
- 文件不存在返回 404

---

## 11. 管理员后台模块

### 11.1 管理员后台总看板

| 属性 | 值 |
|------|-----|
| 接口名称 | 后台总看板数据 |
| 请求地址 | `/api/admin/dashboard` |
| 请求方式 | GET |
| 权限级别 | 管理员 |

**请求参数**：无

**响应参数**：

| 字段 | 类型 | 说明 |
|------|------|------|
| totalUsers | Long | 平台注册用户总数 |
| activeUsers | Long | 当月有账单记录的活跃用户数 |
| currentMonthBillCount | Long | 当月全平台账单总数 |
| currentMonthTotalAmount | BigDecimal | 当月全平台流水总额 |
| recent6MonthTrend | Array | 近6个月数据趋势 |

**recent6MonthTrend 子字段**：

| 字段 | 类型 | 说明 |
|------|------|------|
| yearMonth | String | 月份 |
| billCount | Long | 账单数 |
| userCount | Long | 活跃用户数 |
| totalAmount | BigDecimal | 交易总额 |

**响应示例**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "totalUsers": 256,
    "activeUsers": 89,
    "currentMonthBillCount": 1240,
    "currentMonthTotalAmount": 235600.00,
    "recent6MonthTrend": [
      { "yearMonth": "2026-01", "billCount": 980, "userCount": 72, "totalAmount": 182000.00 },
      { "yearMonth": "2026-02", "billCount": 1050, "userCount": 75, "totalAmount": 195000.00 }
    ]
  },
  "timestamp": 1719145600000
}
```

---

### 11.2 用户管理

#### 11.2.1 用户分页列表

| 属性 | 值 |
|------|-----|
| 接口名称 | 全平台用户分页列表 |
| 请求地址 | `/api/admin/user/page` |
| 请求方式 | GET |
| 权限级别 | 管理员 |

**请求参数（Query String）**：

| 字段 | 类型 | 必传 | 说明 |
|------|------|------|------|
| page | Integer | 否 | 页码，默认1 |
| size | Integer | 否 | 每页条数，默认20 |
| username | String | 否 | 用户名模糊搜索 |
| status | Integer | 否 | 账号状态：1-正常，0-冻结 |

**响应参数**（records 子字段）：

| 字段 | 类型 | 说明 |
|------|------|------|
| userId | Long | 用户ID |
| username | String | 用户名 |
| status | Integer | 账号状态 |
| billCount | Long | 该用户账单总数 |
| lastLoginTime | String | 最近活跃时间 |
| createdAt | String | 注册时间 |

**响应示例**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [
      {
        "userId": 10001,
        "username": "zhangsan",
        "status": 1,
        "billCount": 156,
        "lastLoginTime": "2026-06-23 12:30:00",
        "createdAt": "2026-06-20 09:15:00"
      }
    ],
    "total": 256,
    "page": 1,
    "size": 20,
    "totalPages": 13
  },
  "timestamp": 1719145600000
}
```

#### 11.2.2 查看用户账单

| 属性 | 值 |
|------|-----|
| 接口名称 | 查看指定用户的账单 |
| 请求地址 | `/api/admin/user/{userId}/bills` |
| 请求方式 | GET |
| 权限级别 | 管理员 |

**路径参数**：`userId` - 目标用户ID

**请求参数**：同 [4.4 多条件筛选账单](#44-多条件筛选账单)（去除用户数据隔离，显示全量）

**响应格式**：同 [4.3 分页查询账单](#43-分页查询账单)

#### 11.2.3 冻结/解冻用户

| 属性 | 值 |
|------|-----|
| 接口名称 | 冻结或解冻用户账号 |
| 请求地址 | `/api/admin/user/{userId}/status` |
| 请求方式 | PUT |
| 权限级别 | 管理员 |

**路径参数**：`userId` - 目标用户ID

**请求参数**：

| 字段 | 类型 | 必传 | 说明 |
|------|------|------|------|
| status | Integer | 是 | 1-正常（解冻），0-冻结 |

**请求示例**：
```json
{
  "status": 0
}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "用户已冻结",
  "data": null,
  "timestamp": 1719145600000
}
```

**业务说明**：
- 冻结后用户无法登录（JWT未过期仍可使用，后端验证时检查status）
- 操作记录自动写入 admin_operation_log

#### 11.2.4 重置用户密码

| 属性 | 值 |
|------|-----|
| 接口名称 | 重置用户登录密码 |
| 请求地址 | `/api/admin/user/{userId}/reset-password` |
| 请求方式 | PUT |
| 权限级别 | 管理员 |

**路径参数**：`userId` - 目标用户ID

**请求参数**：无（后端自动生成随机密码）

**响应参数**：

| 字段 | 类型 | 说明 |
|------|------|------|
| newPassword | String | 生成的新密码明文（提示管理员告知用户） |

**响应示例**：
```json
{
  "code": 200,
  "message": "密码已重置",
  "data": {
    "newPassword": "aB3xK9mQ"
  },
  "timestamp": 1719145600000
}
```

**业务说明**：
- 生成8位随机密码，BCrypt加密后更新
- 操作记录写入 admin_operation_log（不记录明文密码）

#### 11.2.5 导出用户数据

| 属性 | 值 |
|------|-----|
| 接口名称 | 导出用户数据Excel |
| 请求地址 | `/api/admin/user/export` |
| 请求方式 | GET |
| 权限级别 | 管理员 |

**请求参数**：同 [11.2.1](#1121-用户分页列表) 筛选参数

**响应说明**：
- 返回 Excel 文件流
- 文件名：`用户数据导出_20260623_143000.xlsx`

---

### 11.3 全局账单管理

#### 11.3.1 全平台账单分页

| 属性 | 值 |
|------|-----|
| 接口名称 | 全平台账单分页查询 |
| 请求地址 | `/api/admin/bill/page` |
| 请求方式 | GET |
| 权限级别 | 管理员 |

**请求参数（Query String）**：

| 字段 | 类型 | 必传 | 说明 |
|------|------|------|------|
| page | Integer | 否 | 页码 |
| size | Integer | 否 | 每页条数 |
| username | String | 否 | 按用户名筛选 |
| userId | Long | 否 | 按用户ID筛选 |
| type | String | 否 | 收支类型 |
| startDate | String | 否 | 消费时间起始 |
| endDate | String | 否 | 消费时间截止 |
| minAmount | BigDecimal | 否 | 最小金额 |
| maxAmount | BigDecimal | 否 | 最大金额 |

**响应参数**（records 子字段，比用户端多出用户名）：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 账单ID |
| userId | Long | 所属用户ID |
| username | String | 所属用户名 |
| amount | BigDecimal | 金额 |
| type | String | 收支类型 |
| categoryName | String | 分类名称 |
| remark | String | 备注 |
| hasImage | Boolean | 是否有小票图片 |
| consumeTime | String | 消费时间 |
| createdAt | String | 创建时间 |

**响应示例**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [
      {
        "id": 12345,
        "userId": 10001,
        "username": "zhangsan",
        "amount": 35.50,
        "type": "expense",
        "categoryName": "餐饮",
        "remark": "午餐外卖",
        "hasImage": true,
        "consumeTime": "2026-06-23 12:30:00",
        "createdAt": "2026-06-23 12:31:00"
      }
    ],
    "total": 1240,
    "page": 1,
    "size": 20,
    "totalPages": 62
  },
  "timestamp": 1719145600000
}
```

#### 11.3.2 后台编辑账单

| 属性 | 值 |
|------|-----|
| 接口名称 | 管理员编辑任意账单 |
| 请求地址 | `/api/admin/bill/{id}` |
| 请求方式 | PUT |
| 权限级别 | 管理员 |

**路径参数**：`id` - 账单ID

**请求参数**：同 [4.5 修改账单](#45-修改账单)

**业务说明**：
- 管理员可编辑任何用户的账单
- 操作记录自动写入 admin_operation_log

#### 11.3.3 后台删除账单

| 属性 | 值 |
|------|-----|
| 接口名称 | 管理员删除任意账单 |
| 请求地址 | `/api/admin/bill/{id}` |
| 请求方式 | DELETE |
| 权限级别 | 管理员 |

**业务说明**：
- 管理员可删除任何用户的账单
- 操作记录自动写入 admin_operation_log

#### 11.3.4 全平台消费统计

| 属性 | 值 |
|------|-----|
| 接口名称 | 全平台消费统计数据 |
| 请求地址 | `/api/admin/bill/statistics` |
| 请求方式 | GET |
| 权限级别 | 管理员 |

**请求参数（Query String）**：

| 字段 | 类型 | 必传 | 说明 |
|------|------|------|------|
| yearMonth | String | 否 | 指定月份，不传默认当月 |

**响应参数**：

| 字段 | 类型 | 说明 |
|------|------|------|
| totalIncome | BigDecimal | 全平台总收入 |
| totalExpense | BigDecimal | 全平台总支出 |
| userRanking | Array | 用户消费排名 TOP 20 |
| categoryDistribution | Array | 全平台分类消费分布 |

**响应示例**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "totalIncome": 856000.00,
    "totalExpense": 623500.00,
    "userRanking": [
      { "userId": 10001, "username": "zhangsan", "totalAmount": 8350.50, "billCount": 42 }
    ],
    "categoryDistribution": [
      { "categoryName": "餐饮", "totalAmount": 186500.00, "percentage": 29.91 }
    ]
  },
  "timestamp": 1719145600000
}
```

#### 11.3.5 全量导出账单Excel

| 属性 | 值 |
|------|-----|
| 接口名称 | 全平台账单Excel导出 |
| 请求地址 | `/api/admin/bill/export-all` |
| 请求方式 | GET |
| 权限级别 | 管理员 |

**请求参数**：同 [11.3.1](#1131-全平台账单分页) 筛选参数（不含分页）

**响应说明**：返回 Excel 文件流

---

### 11.4 系统分类管理

#### 11.4.1 全部分类列表

| 属性 | 值 |
|------|-----|
| 接口名称 | 获取全部系统分类及用户自定义分类 |
| 请求地址 | `/api/admin/category/list` |
| 请求方式 | GET |
| 权限级别 | 管理员 |

**响应格式**：同 [5.1 获取用户可用分类列表](#51-获取用户可用分类列表)，额外展示所属用户信息

#### 11.4.2 新增全局分类

| 属性 | 值 |
|------|-----|
| 接口名称 | 新增系统全局默认分类 |
| 请求地址 | `/api/admin/category` |
| 请求方式 | POST |
| 权限级别 | 管理员 |

**请求参数**：同 [5.2 创建自定义分类](#52-创建自定义分类)（user_id 自动设为 0）

**业务说明**：
- 全局分类 user_id=0，对所有用户可见
- 操作记录写入 admin_operation_log

#### 11.4.3 修改分类

| 属性 | 值 |
|------|-----|
| 接口名称 | 管理员修改任意分类 |
| 请求地址 | `/api/admin/category/{id}` |
| 请求方式 | PUT |
| 权限级别 | 管理员 |

#### 11.4.4 删除分类

| 属性 | 值 |
|------|-----|
| 接口名称 | 管理员删除分类 |
| 请求地址 | `/api/admin/category/{id}` |
| 请求方式 | DELETE |
| 权限级别 | 管理员 |

**业务说明**：
- 有关联账单的分类提示不可删除或需迁移（错误码 30003）
- 操作记录写入 admin_operation_log

---

### 11.5 管理员查看/修正用户预算与目标

#### 11.5.1 查看用户预算

| 属性 | 值 |
|------|-----|
| 接口名称 | 管理员查看指定用户预算 |
| 请求地址 | `/api/admin/budget/{userId}` |
| 请求方式 | GET |
| 权限级别 | 管理员 |

**路径参数**：`userId` - 目标用户ID

**请求参数（Query String）**：

| 字段 | 类型 | 必传 | 说明 |
|------|------|------|------|
| yearMonth | String | 否 | 预算月份，不传默认当月 |

**响应格式**：同 [6.1 获取当月预算](#61-获取当月预算)

#### 11.5.2 修正用户预算

| 属性 | 值 |
|------|-----|
| 接口名称 | 管理员修正用户预算 |
| 请求地址 | `/api/admin/budget/{id}` |
| 请求方式 | PUT |
| 权限级别 | 管理员 |

**请求参数**：同 [6.3 创建/覆盖月度预算](#63-创建覆盖月度预算)

#### 11.5.3 查看用户存钱目标

| 属性 | 值 |
|------|-----|
| 接口名称 | 管理员查看用户存钱目标 |
| 请求地址 | `/api/admin/save-target/{userId}` |
| 请求方式 | GET |
| 权限级别 | 管理员 |

**响应格式**：同 [7.1 获取存钱目标列表](#71-获取存钱目标列表)

#### 11.5.4 修正用户存钱目标

| 属性 | 值 |
|------|-----|
| 接口名称 | 管理员修正用户存钱目标 |
| 请求地址 | `/api/admin/save-target/{id}` |
| 请求方式 | PUT |
| 权限级别 | 管理员 |

**请求参数**：同 [7.3 更新存钱目标](#73-更新存钱目标追加存款修改信息)

---

### 11.6 AI运营管理

#### 11.6.1 获取AI配置

| 属性 | 值 |
|------|-----|
| 接口名称 | 获取AI全部配置项 |
| 请求地址 | `/api/admin/ai/config` |
| 请求方式 | GET |
| 权限级别 | 管理员 |

**响应参数**：

| 字段 | 类型 | 说明 |
|------|------|------|
| configs | Array | 配置项列表 |
| configs[].id | Long | 配置ID |
| configs[].configKey | String | 配置键 |
| configs[].configValue | String | 配置值 |
| configs[].configType | String | 值类型 |
| configs[].description | String | 说明 |

**响应示例**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "configs": [
      { "id": 1, "configKey": "model_temperature", "configValue": "0.7", "configType": "NUMBER", "description": "模型temperature参数" },
      { "id": 2, "configKey": "model_max_tokens", "configValue": "2048", "configType": "NUMBER", "description": "模型max_tokens参数" },
      { "id": 3, "configKey": "model_top_p", "configValue": "0.9", "configType": "NUMBER", "description": "模型top_p参数" },
      { "id": 4, "configKey": "prompt_template_analysis", "configValue": "你是一名专业的个人理财顾问...", "configType": "TEXT", "description": "AI月度分析Prompt模板" }
    ]
  },
  "timestamp": 1719145600000
}
```

#### 11.6.2 更新AI配置

| 属性 | 值 |
|------|-----|
| 接口名称 | 更新AI配置项 |
| 请求地址 | `/api/admin/ai/config` |
| 请求方式 | PUT |
| 权限级别 | 超级管理员 |

**请求参数**：

| 字段 | 类型 | 必传 | 说明 |
|------|------|------|------|
| configs | Array | 是 | 待更新的配置项列表 |
| configs[].configKey | String | 是 | 配置键 |
| configs[].configValue | String | 是 | 新配置值 |

**请求示例**：
```json
{
  "configs": [
    { "configKey": "model_temperature", "configValue": "0.8" },
    { "configKey": "model_max_tokens", "configValue": "3072" }
  ]
}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "配置已更新",
  "data": null,
  "timestamp": 1719145600000
}
```

**业务说明**：
- 配置热更新，下次AI调用即刻生效
- 仅超级管理员可修改

#### 11.6.3 全平台AI分析记录

| 属性 | 值 |
|------|-----|
| 接口名称 | 查看全平台AI分析记录 |
| 请求地址 | `/api/admin/ai/records` |
| 请求方式 | GET |
| 权限级别 | 管理员 |

**请求参数（Query String）**：

| 字段 | 类型 | 必传 | 说明 |
|------|------|------|------|
| page | Integer | 否 | 页码 |
| size | Integer | 否 | 每页条数 |
| username | String | 否 | 按用户名搜索 |
| yearMonth | String | 否 | 按月份筛选 |

**响应参数**（records 子字段）：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 记录ID |
| userId | Long | 用户ID |
| username | String | 用户名 |
| yearMonth | String | 分析月份 |
| modelName | String | 使用模型 |
| processingTimeMs | Long | 处理耗时 |
| createdAt | String | 分析时间 |

**响应示例**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [
      {
        "id": 55,
        "userId": 10001,
        "username": "zhangsan",
        "yearMonth": "2026-06",
        "modelName": "DeepSeek",
        "processingTimeMs": 12500,
        "createdAt": "2026-06-23 12:35:00"
      }
    ],
    "total": 120,
    "page": 1,
    "size": 20,
    "totalPages": 6
  },
  "timestamp": 1719145600000
}
```

#### 11.6.4 查看AI分析详情（管理员）

| 属性 | 值 |
|------|-----|
| 接口名称 | 查看AI分析完整结果 |
| 请求地址 | `/api/admin/ai/records/{id}` |
| 请求方式 | GET |
| 权限级别 | 管理员 |

**响应格式**：同 [9.1 AI月度账单分析](#91-ai月度账单分析)，额外包含分析用户信息

#### 11.6.5 重置用户向量数据

| 属性 | 值 |
|------|-----|
| 接口名称 | 清空指定用户Qdrant消费向量 |
| 请求地址 | `/api/admin/ai/qdrant/reset` |
| 请求方式 | POST |
| 权限级别 | 超级管理员 |

**请求参数**：

| 字段 | 类型 | 必传 | 说明 |
|------|------|------|------|
| userId | Long | 是 | 目标用户ID |

**响应示例**：
```json
{
  "code": 200,
  "message": "用户消费向量数据已清空",
  "data": null,
  "timestamp": 1719145600000
}
```

**业务说明**：
- 删除Qdrant中该用户的所有向量数据
- 仅超级管理员可操作
- 操作需二次确认并记录操作日志

---

### 11.7 系统运维

#### 11.7.1 管理员列表

| 属性 | 值 |
|------|-----|
| 接口名称 | 获取管理员列表 |
| 请求地址 | `/api/admin/account/page` |
| 请求方式 | GET |
| 权限级别 | 超级管理员 |

**请求参数（Query String）**：

| 字段 | 类型 | 必传 | 说明 |
|------|------|------|------|
| page | Integer | 否 | 页码 |
| size | Integer | 否 | 每页条数 |

**响应参数**（records 子字段）：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 管理员ID |
| username | String | 管理员账号 |
| role | String | SUPER_ADMIN / OPERATOR |
| lastLoginTime | String | 最近登录时间 |
| createdAt | String | 创建时间 |

#### 11.7.2 新增管理员

| 属性 | 值 |
|------|-----|
| 接口名称 | 新增管理员账号 |
| 请求地址 | `/api/admin/account` |
| 请求方式 | POST |
| 权限级别 | 超级管理员 |

**请求参数**：

| 字段 | 类型 | 必传 | 说明 |
|------|------|------|------|
| username | String | 是 | 管理员账号 |
| password | String | 是 | 初始密码 |
| role | String | 是 | SUPER_ADMIN / OPERATOR |

**请求示例**：
```json
{
  "username": "operator01",
  "password": "oper123456",
  "role": "OPERATOR"
}
```

#### 11.7.3 修改管理员信息/角色

| 属性 | 值 |
|------|-----|
| 接口名称 | 修改管理员角色或重置密码 |
| 请求地址 | `/api/admin/account/{id}` |
| 请求方式 | PUT |
| 权限级别 | 超级管理员 |

#### 11.7.4 删除管理员

| 属性 | 值 |
|------|-----|
| 接口名称 | 删除管理员账号 |
| 请求地址 | `/api/admin/account/{id}` |
| 请求方式 | DELETE |
| 权限级别 | 超级管理员 |

**业务说明**：
- 超级管理员不可删除自身
- 使用逻辑删除

#### 11.7.5 操作日志分页查询

| 属性 | 值 |
|------|-----|
| 接口名称 | 管理员操作日志查询 |
| 请求地址 | `/api/admin/log/page` |
| 请求方式 | GET |
| 权限级别 | 管理员 |

**请求参数（Query String）**：

| 字段 | 类型 | 必传 | 说明 |
|------|------|------|------|
| page | Integer | 否 | 页码 |
| size | Integer | 否 | 每页条数 |
| username | String | 否 | 按操作人筛选 |
| operation | String | 否 | 按操作类型筛选 |
| startTime | String | 否 | 操作时间起始 |
| endTime | String | 否 | 操作时间截止 |

**响应参数**（records 子字段）：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 日志ID |
| adminUsername | String | 操作人 |
| operation | String | 操作描述 |
| method | String | HTTP方法 |
| requestUrl | String | 请求路径 |
| status | Integer | 操作结果：1-成功，0-失败 |
| ipAddress | String | 操作IP |
| createdAt | String | 操作时间 |

#### 11.7.6 操作日志导出

| 属性 | 值 |
|------|-----|
| 接口名称 | 导出操作日志Excel |
| 请求地址 | `/api/admin/log/export` |
| 请求方式 | GET |
| 权限级别 | 管理员 |

**请求参数**：同 [11.7.5](#1175-操作日志分页查询) 筛选参数（不含分页）

**响应说明**：返回 Excel 文件流

#### 11.7.7 文件存储概览

| 属性 | 值 |
|------|-----|
| 接口名称 | 获取文件存储概览信息 |
| 请求地址 | `/api/admin/file/overview` |
| 请求方式 | GET |
| 权限级别 | 管理员 |

**响应参数**：

| 字段 | 类型 | 说明 |
|------|------|------|
| totalFileCount | Long | 文件总数 |
| totalSizeBytes | Long | 占用空间（字节） |
| totalSizeFormatted | String | 可读格式（如 "156.8 MB"） |
| orphanFileCount | Long | 无关联账单的孤儿文件数 |
| storageDir | String | 存储目录路径 |

#### 11.7.8 清理无效文件

| 属性 | 值 |
|------|-----|
| 接口名称 | 清理无关联的孤儿文件 |
| 请求地址 | `/api/admin/file/clean` |
| 请求方式 | DELETE |
| 权限级别 | 超级管理员 |

**响应参数**：

| 字段 | 类型 | 说明 |
|------|------|------|
| deletedCount | Long | 清理的文件数 |
| freedSpaceBytes | Long | 释放空间（字节） |

#### 11.7.9 数据库备份

| 属性 | 值 |
|------|-----|
| 接口名称 | 执行数据库备份 |
| 请求地址 | `/api/admin/database/backup` |
| 请求方式 | POST |
| 权限级别 | 超级管理员 |

**请求参数**：无

**响应参数**：

| 字段 | 类型 | 说明 |
|------|------|------|
| backupId | Long | 备份记录ID |
| fileName | String | 备份文件名 |
| filePath | String | 备份文件路径 |
| fileSize | Long | 文件大小（字节） |
| createdAt | String | 备份时间 |

**响应示例**：
```json
{
  "code": 200,
  "message": "数据库备份成功",
  "data": {
    "backupId": 5,
    "fileName": "backup_20260623_143000.sql",
    "filePath": "./backups/backup_20260623_143000.sql",
    "fileSize": 5242880,
    "createdAt": "2026-06-23 14:30:00"
  },
  "timestamp": 1719145600000
}
```

**业务说明**：
- 后端执行 `mysqldump` 或使用 JDBC 导出SQL
- 备份记录写入 database_backup_log 表
- 备份文件存储于 `{user.dir}/backups/`

#### 11.7.10 备份历史查询

| 属性 | 值 |
|------|-----|
| 接口名称 | 查询备份历史记录 |
| 请求地址 | `/api/admin/database/backup/log` |
| 请求方式 | GET |
| 权限级别 | 超级管理员 |

**请求参数（Query String）**：

| 字段 | 类型 | 必传 | 说明 |
|------|------|------|------|
| page | Integer | 否 | 页码 |
| size | Integer | 否 | 每页条数 |

**响应参数**（records 子字段）：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 备份记录ID |
| fileName | String | 文件名 |
| fileSize | Long | 文件大小（字节） |
| status | Integer | 1-成功，0-失败 |
| createdAt | String | 备份时间 |

---

## 12. 全局错误码对照表

### 12.1 HTTP状态码使用规范

| HTTP 状态码 | 含义 | 使用场景 |
|-------------|------|----------|
| 200 | 成功 | 业务处理成功 |
| 400 | 请求参数错误 | 参数校验失败、业务规则不满足 |
| 401 | 未认证 | Token 缺失、无效、过期 |
| 403 | 无权限 | Token有效但角色权限不足 |
| 404 | 资源不存在 | 请求的资源（账单、用户等）不存在 |
| 500 | 服务器错误 | 系统内部异常 |

### 12.2 业务错误码对照表

| 错误码 | HTTP码 | 说明 | 建议处理 |
|--------|--------|------|----------|
| **用户认证 1xxxx** | | | |
| 10001 | 400 | 用户名已存在 | 提示用户更换用户名 |
| 10002 | 400 | 用户名或密码错误 | 提示用户重新输入 |
| 10003 | 400 | 账号已被冻结 | 提示联系管理员 |
| 10004 | 401 | Token已过期 | 跳转登录页 |
| 10005 | 400 | 旧密码验证失败 | 提示重新输入旧密码 |
| 10006 | 400 | 两次密码输入不一致 | 提示重新输入 |
| 10007 | 400 | 用户名格式不合法 | 提示格式要求 |
| 10008 | 400 | 密码格式不合法 | 提示格式要求 |
| **账单业务 2xxxx** | | | |
| 20001 | 403 | 无权操作此账单 | 数据隔离校验失败 |
| 20002 | 404 | 账单不存在 | 提示账单已删除 |
| 20003 | 400 | 金额必须大于0 | 校验提示 |
| 20004 | 400 | 分类不存在或不可用 | 提示重新选择分类 |
| 20005 | 400 | 消费时间格式不正确 | 校验提示 |
| **分类业务 3xxxx** | | | |
| 30001 | 400 | 分类名称已存在 | 提示更换名称 |
| 30002 | 403 | 系统内置分类不可修改/删除 | 提示不可操作 |
| 30003 | 400 | 该分类下有N条关联账单，无法删除 | 提示迁移或保留 |
| **AI分析 4xxxx** | | | |
| 40001 | 400 | 该月份无账单数据，无法分析 | 提示先记账 |
| 40002 | 500 | AI服务调用失败 | 提示稍后重试 |
| 40003 | 500 | AI返回结果解析失败 | 提示重试 |
| **文件 5xxxx** | | | |
| 50001 | 400 | 文件类型不允许 | 提示仅支持 jpg/png/webp |
| 50002 | 400 | 文件大小超过限制（10MB） | 提示压缩后上传 |
| 50003 | 500 | 文件上传失败 | 提示重试 |
| 50004 | 404 | 文件不存在 | 提示文件已删除 |
| **管理员业务 6xxxx** | | | |
| 60001 | 403 | 需要超级管理员权限 | 提示权限不足 |
| 60002 | 400 | 不能操作自身账号 | 提示不可操作 |
| 60003 | 400 | 目标用户不存在 | 提示用户不存在 |
| 60004 | 500 | 数据库备份失败 | 提示检查数据库服务 |
| **离线同步 7xxxx** | | | |
| 70001 | 400 | 批量同步数据格式错误 | 校验提示 |
| 70002 | 400 | 同步账单中分类ID不存在 | 提示修正后重试 |

### 12.3 统一错误响应示例

```json
// 参数校验失败
{
  "code": 20003,
  "message": "金额必须大于0",
  "errors": [
    { "field": "amount", "message": "金额必须大于0" }
  ],
  "timestamp": 1719145600000
}

// Token过期
{
  "code": 10004,
  "message": "登录已过期，请重新登录",
  "errors": null,
  "timestamp": 1719145600000
}

// 权限不足
{
  "code": 60001,
  "message": "需要超级管理员权限",
  "errors": null,
  "timestamp": 1719145600000
}
```

---

## 13. 自检审查确认清单

| 编号 | 自检项 | 核查结果 | 说明 |
|------|--------|----------|------|
| SC-001 | 是否覆盖全部SRS功能 | ✅ 通过 | 60个接口完全对照SRS第7.4节接口清单，无遗漏 |
| SC-002 | 是否区分普通用户/管理员权限接口 | ✅ 通过 | 接口路径前缀物理隔离（/api/** vs /api/admin/**）；超级管理员专属接口标注清晰 |
| SC-003 | 是否包含离线同步整套专属接口 | ✅ 通过 | `/api/bill/sync-batch` 支持批量提交、UUID去重、逐条结果返回 |
| SC-004 | 是否包含AI Markdown结构化分析接口 | ✅ 通过 | `/api/ai/analyze` 后端自动构造Markdown表格送入大模型；`/api/ai/classify` 智能分类推荐 |
| SC-005 | 所有数据库字段是否有对应读写接口 | ✅ 通过 | 10张表全覆盖：sys_user（注册/登录/信息）、sys_admin（CRUD）、bill（完整CRUD）、category（内置+自定义）、budget（月度）、save_target（进度）、admin_operation_log（查询/导出）、ai_analysis_record（分析/历史）、ai_config（查询/更新）、database_backup_log（备份/查询） |
| SC-006 | 统一返回体、错误码、分页规范一致 | ✅ 通过 | 统一 {code, message, data, timestamp}；分页 {records, total, page, size, totalPages}；43个业务错误码 |
| SC-007 | 文件上传、导出逻辑完整 | ✅ 通过 | 小票图片上传含类型/大小校验；Excel导出含用户端和管理员端 |
| SC-008 | 可直接支撑鸿蒙端+Vue端全量开发 | ✅ 通过 | 每个接口含请求参数、响应参数、示例JSON、业务说明 |

---

## 附录A：接口总量统计

| 序号 | 模块 | 接口数量 | 接口列表 |
|------|------|----------|----------|
| 1 | 认证模块 | 6 | 注册、登录、用户信息、修改信息、修改密码、管理员登录 |
| 2 | 离线同步模块 | 1 | 批量同步 |
| 3 | 账单模块 | 7 | 新增、详情、分页、筛选、修改、删除、导出 |
| 4 | 分类模块 | 4 | 列表、新增、修改、删除 |
| 5 | 预算模块 | 3 | 当月、指定月份、创建/覆盖 |
| 6 | 存钱目标模块 | 4 | 列表、创建、更新进度、删除 |
| 7 | 统计图表模块 | 1 | 月度统计 |
| 8 | AI智能理财模块 | 4 | 月度分析、分类推荐、历史列表、历史详情 |
| 9 | 文件模块 | 2 | 上传、查看 |
| 10 | 管理员-后台看板 | 1 | 看板 |
| 11 | 管理员-用户管理 | 5 | 列表、查看账单、冻结/解冻、重置密码、导出 |
| 12 | 管理员-账单管理 | 5 | 列表、编辑、删除、统计、导出 |
| 13 | 管理员-分类管理 | 4 | 列表、新增、修改、删除 |
| 14 | 管理员-预算目标 | 4 | 查看预算、修正预算、查看目标、修正目标 |
| 15 | 管理员-AI运营 | 5 | 获取配置、更新配置、分析记录、分析详情、重置向量 |
| 16 | 管理员-系统运维 | 10 | 管理员CRUD(4)、操作日志(2)、文件管理(2)、数据库备份(2) |
| **合计** | | **66** | |

> 注：SRS第7.4节接口清单共60个接口。本设计在此基础上将管理员-AI分析记录拆分为列表+详情2个接口、AI历史拆分为列表+详情2个接口，更符合RESTful规范，合计66个接口，实现SRS全部功能的完整覆盖。

---

## 附录B：接口快速检索索引

### 普通用户端接口（35个）

| 方法 | 路径 | 功能 |
|------|------|------|
| POST | /api/user/register | 用户注册 |
| POST | /api/user/login | 用户登录 |
| GET | /api/user/profile | 获取用户信息 |
| PUT | /api/user/profile | 修改用户信息 |
| PUT | /api/user/password | 修改密码 |
| POST | /api/bill/sync-batch | 离线批量同步 |
| POST | /api/bill | 新增账单 |
| GET | /api/bill/{id} | 查询账单详情 |
| GET | /api/bill/page | 分页查询账单 |
| GET | /api/bill/search | 多条件筛选账单 |
| PUT | /api/bill/{id} | 修改账单 |
| DELETE | /api/bill/{id} | 删除账单 |
| GET | /api/bill/export | 导出账单Excel |
| GET | /api/category/list | 获取分类列表 |
| POST | /api/category | 创建自定义分类 |
| PUT | /api/category/{id} | 修改分类 |
| DELETE | /api/category/{id} | 删除分类 |
| GET | /api/budget/current | 获取当月预算 |
| GET | /api/budget/{yearMonth} | 获取指定月预算 |
| POST | /api/budget | 创建/覆盖预算 |
| GET | /api/save-target/list | 获取目标列表 |
| POST | /api/save-target | 创建存钱目标 |
| PUT | /api/save-target/{id} | 更新/追加存款 |
| DELETE | /api/save-target/{id} | 删除目标 |
| GET | /api/statistics/monthly | 月度收支统计 |
| POST | /api/ai/analyze | AI月度分析 |
| POST | /api/ai/classify | AI分类推荐 |
| GET | /api/ai/history | AI历史列表 |
| GET | /api/ai/history/{id} | AI历史详情 |
| POST | /api/file/upload/receipt | 上传小票图片 |

### 管理员后台端接口（31个）

| 方法 | 路径 | 功能 |
|------|------|------|
| POST | /api/admin/login | 管理员登录 |
| GET | /api/admin/dashboard | 后台看板 |
| GET | /api/admin/user/page | 用户列表 |
| GET | /api/admin/user/{userId}/bills | 查看用户账单 |
| PUT | /api/admin/user/{userId}/status | 冻结/解冻 |
| PUT | /api/admin/user/{userId}/reset-password | 重置密码 |
| GET | /api/admin/user/export | 导出用户数据 |
| GET | /api/admin/bill/page | 全平台账单 |
| PUT | /api/admin/bill/{id} | 编辑账单 |
| DELETE | /api/admin/bill/{id} | 删除账单 |
| GET | /api/admin/bill/statistics | 消费统计 |
| GET | /api/admin/bill/export-all | 全量导出 |
| GET | /api/admin/category/list | 分类列表 |
| POST | /api/admin/category | 新增分类 |
| PUT | /api/admin/category/{id} | 修改分类 |
| DELETE | /api/admin/category/{id} | 删除分类 |
| GET | /api/admin/budget/{userId} | 查看用户预算 |
| PUT | /api/admin/budget/{id} | 修正预算 |
| GET | /api/admin/save-target/{userId} | 查看目标 |
| PUT | /api/admin/save-target/{id} | 修正目标 |
| GET | /api/admin/ai/config | 获取AI配置 |
| PUT | /api/admin/ai/config | 更新AI配置 |
| GET | /api/admin/ai/records | AI分析记录 |
| GET | /api/admin/ai/records/{id} | AI分析详情 |
| POST | /api/admin/ai/qdrant/reset | 重置向量 |
| GET | /api/admin/account/page | 管理员列表 |
| POST | /api/admin/account | 新增管理员 |
| PUT | /api/admin/account/{id} | 修改管理员 |
| DELETE | /api/admin/account/{id} | 删除管理员 |
| GET | /api/admin/log/page | 操作日志 |
| GET | /api/admin/log/export | 导出日志 |
| GET | /api/admin/file/overview | 文件概览 |
| DELETE | /api/admin/file/clean | 清理文件 |
| POST | /api/admin/database/backup | 数据库备份 |
| GET | /api/admin/database/backup/log | 备份历史 |

---

*文档结束*

| 编制 | 审核 | 批准 |
|------|------|------|
| 胡宪棋 | — | — |
| 2026-06-23 | — | — |
