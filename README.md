# 古籍分析 Agent 后端

基于 **SpringBoot 3.2.x** + **MyBatis-Plus** + **Redis** + **MySQL** 的 WebFlux 流式对话后端服务，集成 **Coze V3 API** 实现智能古籍分析功能。

## 特别说明

### 前后端分离仓库

本项目采用前后端分离架构，**前端**和**后端**分别托管于独立仓库，开发时需同时启动两个服务：

| 仓库 | 路径 | 启动命令 |
|------|------|---------|
| **前端** | `ancientbooks-pc-frontend/` | `npm run dev` |
| **后端** | `ancientbooks-pc-backend/` | `mvn spring-boot:run` |

**关联地址：**

- 后端 README：`../ancientbooks-pc-backend/README.md`
- 后端 API 文档：详见后端 README「接口说明」章节

> ⚠️ 前端依赖后端接口，**启动前端前必须先确认后端服务已正常运行**（MySQL + Redis + SpringBoot 均需就绪）。

---

### 后端项目进度

**v1.0** — 2026.9.6

| 模块 | 状态 | 说明 |
|------|------|------|
| **项目基础架构** | ✅ 完成 | SpringBoot 3.2.x + MyBatis-Plus + Redis + MySQL |
| **数据库设计** | ✅ 完成 | 用户表、对话历史表、古籍元数据表 |
| **用户认证模块** | ✅ 完成 | JWT登录/登出/刷新Token接口 |
| **JWT安全机制** | ✅ 完成 | Token生成/解析/黑名单/过期检查 |
| **Coze V3 API集成** | ✅ 完成 | 流式/非流式调用、请求/响应处理 |
| **古籍分析接口** | ✅ 完成 | 流式分析GET、非流式POST、会话管理 |
| **对话历史持久化** | ✅ 完成 | MySQL自动保存、Redis会话缓存 |
| **参数校验** | ✅ 完成 | JSR-380注解验证、手动校验 |
| **限流机制** | ⚠️ 部分完成 | 基础限流拦截器，Coze API限流待完善 |
| **跨域配置** | ⚠️ 临时方案 | 允许所有来源，生产环境需限制域名 |
| **安全加固** | ✅ 完成 | BCrypt加密、SQL注入防护、XSS防护 |
| **密码安全** | ✅ 完成 | BCrypt强度12、密码脱敏日志 |
| **环境配置** | ⏳ 待优化 | 硬编码配置，需迁移到环境变量 |
| **文档建设** | 🔄 进行中 | README/API文档/开发规范持续更新 |

**已知问题：**
- `EventSource` 无法携带自定义请求头，Token 通过 URL query 参数传递
- GET 请求 `content` 超过 8KB 会静默失败，超长古籍需改 POST 或前端截断
- CORS配置过于宽松（`*`），生产环境需限制具体域名
- Coze API缺少独立的限流机制，可能被恶意消耗额度
- 配置文件中的敏感信息（PAT Token、JWT Secret）硬编码，需迁移到环境变量

**下一步计划：**
- [ ] 完善Coze API限流（每小时最多100次/用户）
- [ ] 配置环境变量管理敏感信息
- [ ] 优化CORS配置（生产环境指定域名）
- [ ] 添加登录失败次数限制（防暴力破解）
- [ ] 实现Redis密码认证
- [ ] 提升BCrypt加密强度到12
- [ ] 修复IP获取逻辑（X-Forwarded-For多IP问题）

---

## 技术栈

- **SpringBoot 3.2.x** - 应用框架
- **MyBatis-Plus 3.5.7** - ORM 框架
- **MySQL 8.0** - 数据存储
- **Redis 6.0+** - 缓存与会话管理
- **Spring WebFlux** - 响应式 Web 框架
- **Spring Security** - 安全框架
- **JWT (JJWT)** - Token认证
- **Lombok** - 简化代码
- **Hutool** - 工具类库
- **FastJSON2** - JSON 处理

## 项目结构

```
src/main/java/com/ancientbooks/
├── AnalyseApplication.java              # 启动类
├── config/                              # 配置类
│   ├── CorsConfig.java                  # 跨域配置
│   ├── RedisConfig.java                 # Redis 序列化配置
│   ├── WebClientConfig.java             # WebClient 配置
│   ├── JacksonConfig.java               # Jackson配置
│   ├── SecurityConfig.java              # Spring Security配置
│   ├── JwtAuthenticationInterceptor.java # JWT认证拦截器
│   ├── RateLimitInterceptor.java        # 限流拦截器
│   ├── MybatisPlusConfig.java           # MyBatis-Plus配置
│   ├── MyMetaObjectHandler.java         # MyBatis-Plus自动填充
│   └── WebMvcConfig.java                # Web MVC配置（拦截器注册）
├── controller/                          # 控制器层
│   ├── AncientBookAgentController.java  # 古籍分析控制器
│   └── AuthController.java              # 认证控制器
├── entity/                              # 数据库实体
│   ├── User.java                        # 用户实体
│   └── ChatHistory.java                 # 对话历史实体
├── mapper/                              # 数据访问层
│   ├── UserMapper.java                  # 用户Mapper
│   └── ChatHistoryMapper.java           # 对话历史Mapper
├── service/                             # 业务接口
│   ├── UserService.java                 # 用户服务
│   ├── ChatHistoryService.java          # 对话历史服务
│   ├── CozeService.java                 # Coze API服务
│   └── impl/                            # 服务实现
│       ├── UserServiceImpl.java
│       ├── ChatHistoryServiceImpl.java
│       └── CozeServiceImpl.java
├── dto/                                 # 数据传输对象
│   ├── Result.java                      # 统一返回结果
│   ├── LoginRequest.java                # 登录请求
│   ├── LoginResponse.java               # 登录响应
│   ├── ChatRequest.java                 # 对话请求（旧版）
│   ├── AncientBookAnalysisRequest.java  # 古籍分析请求
│   ├── CozeV3ChatRequest.java           # Coze V3请求
│   ├── CozeApiResponse.java             # Coze响应
│   └── AdditionalMessage.java           # Coze附加消息
├── properties/                          # 配置属性类
│   └── CozeProperties.java              # Coze配置
├── exception/                           # 异常处理
│   ├── GlobalExceptionHandler.java      # 全局异常处理器
│   └── BusinessException.java           # 业务异常
├── constants/                           # 常量类
│   └── SystemConstants.java
├── security/                            # 安全相关
│   ├── JwtTokenProvider.java            # JWT Token提供者
│   └── UserPrincipal.java               # 用户主体
├── utils/                               # 工具类

src/main/resources/
├── application.yml                      # 主配置文件
├── schema.sql                           # 数据库初始化脚本
└── mapper/
    └── ChatHistoryMapper.xml            # Mapper XML

src/test/java/com/ancientbooks/
└── CozeApiTest.java                     # Coze API测试类
```

## 快速开始

### 1. 环境要求

- **JDK 17+** - 下载 [Adoptium](https://adoptium.net/) 或 [Oracle JDK](https://www.oracle.com/java/technologies/downloads/)
- **Maven 3.8+** - 下载 [Apache Maven](https://maven.apache.org/download.cgi)
- **MySQL 8.0** - 下载 [MySQL Community Server](https://dev.mysql.com/downloads/mysql/)
- **Redis 6.0+** - 下载 [Redis for Windows](https://github.com/redis-windows/redis-windows/releases)

### 2. 启动 MySQL 数据库

#### 2.1 启动 MySQL 服务

以后每次启动项目，只需要确保 MySQL 服务在后台运行，然后直接运行你的 Java 后端代码就可以了，完全不需要再次执行这些建表命令。

**Windows:**
```powershell
# 方式一：通过服务管理器启动
net start MySQL80

# 方式二：通过系统托盘启动（如果已安装为服务）
```

**检查 MySQL 是否启动：**
```bash
mysql -u root -pcyf12345 -e "SELECT 1"
# 如果成功，应该返回：1
+---+
| 1 |
+---+
| 1 |
+---+
```

#### 2.2 创建数据库和表

**方式一：使用 MySQL 命令行**
```bash
# 连接到 MySQL
mysql -u root -pcyf12345

# 在 MySQL 命令行中执行
source D:/code/ancinetbooks-Analyse/ancientbooks-pc-backend/src/main/resources/schema.sql

# 或者手动执行以下 SQL
```

**方式二：手动执行 SQL**

```sql
-- 1. 创建数据库
CREATE DATABASE IF NOT EXISTS ancient_books_db
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE ancient_books_db;

-- 2. 创建对话历史表
CREATE TABLE chat_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id VARCHAR(64) DEFAULT 'anonymous' COMMENT '用户标识',
    conversation_id VARCHAR(128) COMMENT 'Coze 会话ID',
    user_query TEXT COMMENT '用户提问',
    ai_reply LONGTEXT COMMENT 'Agent 返回结果',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 0-正常 1-删除',
    INDEX idx_conversation_id (conversation_id),
    INDEX idx_user_id (user_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='古籍对话历史表';

-- 3. 创建古籍元数据表
CREATE TABLE ancient_book (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL COMMENT '书名',
    author VARCHAR(128) COMMENT '作者',
    dynasty VARCHAR(64) COMMENT '朝代',
    category VARCHAR(64) COMMENT '分类：经史子集',
    content TEXT COMMENT '内容摘要',
    file_path VARCHAR(512) COMMENT '文件路径',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='古籍元数据表';

-- 4. 创建用户表
CREATE TABLE IF NOT EXISTS `user` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    username VARCHAR(64) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(128) NOT NULL COMMENT '密码（BCrypt 加密）',
    email VARCHAR(128) COMMENT '邮箱',
    role VARCHAR(32) DEFAULT 'USER' COMMENT '角色：ADMIN、USER',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用 1-正常',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 0-正常 1-删除',
    INDEX idx_username (username),
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 5. 插入默认管理员账户（密码：admin123，BCrypt 加密）
INSERT INTO `user` (username, password, email, role, status)
VALUES ('admin', '$2a$10$YQYxhYqLq5YqLq5YqLq5YOHLq5YqLq5YqLq5YqLq5YqLq5YqLq5Yq', 'admin@ancientbooks.com', 'ADMIN', 1)
ON DUPLICATE KEY UPDATE id=id;
```

**验证数据库创建成功：**
```sql
-- 查看所有表
SHOW TABLES;

-- 查看用户表
SELECT id, username, email, role, status FROM user;

-- 应该看到 admin 用户
-- +----+----------+---------------------+-------+
-- | id | username | email               | role  |
-- +----+----------+---------------------+-------+
-- | 1  | admin    | admin@ancientbooks.com | ADMIN |
-- +----+----------+---------------------+-------+

```

### 3. 启动 Redis 缓存

#### 3.1 安装 Redis

如果还没有安装 Redis，请先安装：

1. 下载地址：https://github.com/redis-windows/redis-windows/releases
2. 下载 `Redis-x64-xxx.msi` 安装包
3. 双击安装，一路下一步即可

#### 3.2 启动 Redis

**方式一：命令行启动（开发环境推荐）**
```bash
# 进入 Redis 安装目录
cd "[C:\Program Files\Redis](D:\Redis\Redis-x64-5.0.14.1)"

# 启动 Redis（带密码验证）
redis-server --requirepass cyf12345
# 优先尝试下面这种
.\redis-server
```

**方式二：服务方式启动（生产环境推荐）**
```bash
# 安装为 Windows 服务
redis-server --service-install redis.windows.conf --requirepass cyf12345

# 启动服务
redis-server --service-start

# 停止服务
redis-server --service-stop

# 卸载服务
redis-server --service-uninstall
```

#### 3.3 测试 Redis 连接

```bash
# 启动 Redis 客户端
要切cd D:\Redis\Redis-x64-5.0.14.1
redis-cli -a cyf12345
redis-cli(改application.yml后，暂未尝试)
# 测试连接
PING
# 应该返回：PONG

# 测试设置键值
SET test:hello "Hello Redis"
# 应该返回：OK

# 测试获取值
GET test:hello
# 应该返回："Hello Redis"

# 退出
EXIT
```

**Redis 密码配置说明：**
- 后端暂未配置 Redis 密码，若设置则为 `cyf12345`
- 如果 Redis 没有设置密码，需要修改 `application.yml` 中的 `spring.data.redis.password` 为空字符串 `""`

### 4. 配置后端

编辑 `src/main/resources/application.yml`：

```yaml
server:
  port: 8080
  servlet:
    context-path: /api

spring:
  application:
    name: ancientbooks-analyse-pc-backend
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://127.0.0.1:3306/ancient_books_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
    username: root
    password: cyf12345  # ✅ MySQL 密码（已配置）
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
  data:
    redis:
      host: 127.0.0.1
      port: 6379
      password: cyf12345  # ✅ Redis 密码（已配置）
      database: 0
      timeout: 60000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: GMT+8
    default-property-inclusion: non_null

# MyBatis-Plus
mybatis-plus:
  mapper-locations: classpath*:mapper/**/*.xml
  type-aliases-package: com.ancientbooks.entity
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      id-type: auto
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

# Coze 扣子配置（需要替换为实际值）
coze:
  base-url: https://api.coze.cn/open_api/v2/chat
  pat-token: "pat_xxxxxxxxxxxx"  # ⚠️ 替换为实际的 PAT Token
  bot-id: "xxxxxx"               # ⚠️ 替换为实际的 Bot ID

# JWT 配置
jwt:
  secret: "your-256-bit-secret-key-change-in-production-12345678901234567890123456789012"
  expiration: 86400000  # 24小时（毫秒）
  blacklist-prefix: "jwt:blacklist:"

# 限流配置
rate-limit:
  enabled: true
  max-requests: 30  # 每分钟最大请求数
  window-seconds: 60
  prefix: "rate:limit:"

# 日志
logging:
  level:
    com.ancientbooks.mapper: debug
```

**重要配置说明：**
``
| 配置项 | 值 | 说明                            |
|--------|-----|---------------------------------|
| `spring.datasource.password` | `cyf12345` | ✅ MySQL 密码（已配置）         |
| `spring.data.redis.password` | `cyf12345` |  Redis 密码（暂未配置）         |
| `coze.pat-token` | `pat_xxxxxxxxxxxx` | ⚠️ **需要替换**为实际 PAT Token |
| `coze.bot-id` | `xxxxxx` | ⚠️ **需要替换**为实际 Bot ID    |
| `jwt.secret` | 测试密钥 | ⚠️ 生产环境必须更换             |

**获取 Coze 配置：**
1. 访问 https://www.coze.cn
2. 创建智能体 → 发布智能体
3. 获取 `bot_id` 和 `pat_token`（个人设置 → API 令牌）

### 5. 启动后端服务

**方式一：Maven 命令行启动（推荐）**
```bash
cd D:/code/ancinetbooks-Analyse/ancientbooks-pc-backend
mvn spring-boot:run
```

**方式二：IDEA 启动**
1. 打开 IDEA，导入项目
2. 运行 `AnalyseApplication.java`

**方式三：打包后运行**
```bash
# 打包
mvn clean package -DskipTests

# 运行 JAR
java -jar target/ancientbooks-analyse-pc-backend-1.0.0.jar
```

**预期成功输出：**
```
Started AnalyseApplication in 8.174 seconds
Tomcat started on port 8080 (http) with context path '/api'
```

**后端服务地址：** `http://localhost:8080/api`

**检查后端是否启动成功：**
```bash
# Linux
curl http://localhost:8080/api/auth/login -X POST \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
# powershell  

Invoke-WebRequest -Uri "http://localhost:8080/api/auth/login" -Method POST -ContentType "application/json" -Body '{"username":"admin","password":"admin123"}' -UseBasicParsing

```

**应该返回：**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "userId": 1,
    "username": "admin",
    "role": "ADMIN"
  }
}
```

### 6. 前端配置（可选）

如果还需要启动前端，请参考以下配置：

#### 6.1 安装依赖
```bash
cd D:/code/ancinetbooks-Analyse/ancientbooks-analyse-pc-frontend
npm install
```

#### 6.2 配置环境变量
**文件：`.env.development`**
```env
VITE_API_BASE_URL=http://127.0.0.1:8080/api
```

#### 6.3 配置 Axios（添加 Token）
**文件：`src/utils/request.js`**
```javascript
import axios from 'axios'

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 120000
})

// 请求拦截器（自动添加 Token）
service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器（处理 401）
service.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default service
```

#### 6.4 启动前端
```bash
npm run dev
```

**前端地址：** `http://localhost:5173`

---

## 接口说明

### 认证接口

#### 用户登录
```
POST /api/auth/login
```

**请求体：**
```json
{
  "username": "admin",
  "password": "admin123"
}
```

**响应：**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "userId": 1,
    "username": "admin",
    "role": "ADMIN"
  }
}
```

#### 用户登出
```
POST /api/auth/logout
Authorization: Bearer {token}
```

**说明：** 将Token加入黑名单，立即失效

#### 刷新 Token
```
POST /api/auth/refresh
Authorization: Bearer {token}
```

**说明：** 生成新Token，旧Token自动失效

---

### 古籍分析接口（⭐ 核心功能）

#### 1. 流式古籍分析（推荐）

```
GET /api/agent/analyze/stream?content={分析内容}&userId={用户ID}&conversationId={会话ID}
```

**参数：**
- `content` (必填): 分析内容，如"讲解《道德经》第一篇"
- `userId` (可选): 用户ID，默认从认证获取
- `conversationId` (可选): 会话ID，用于多轮对话

**响应：**
- Content-Type: `text/event-stream`
- 格式: Coze SSE 原始事件流

**示例：**
```bash
curl -N "http://localhost:8080/api/agent/analyze/stream?content=讲解《道德经》第一篇&userId=admin"
```

#### 2. 非流式古籍分析（同步）

```
POST /api/agent/analyze
Content-Type: application/json

{
  "content": "讲解《道德经》第一篇",
  "userId": "admin",
  "conversationId": "可选会话ID"
}
```

**响应：**
```json
{
  "code": 200,
  "msg": "success",
  "data": "道可道，非常道；名可名，非常名。..."
}
```

#### 3. 获取会话ID

```
GET /api/agent/conversation/{userId}
```

**响应：**
```json
{
  "code": 200,
  "msg": "success",
  "data": "abc123xyz会话ID"
}
```

---

### 旧版对话接口（兼容保留）

#### SSE 流式对话

```
GET /api/agent/stream/chat?query={问题}&conversationId={会话ID}
Authorization: Bearer {token}
```

**参数：**
- `query` (必填): 用户提问内容，最多 5000 字
- `conversationId` (可选): 会话 ID，用于保持上下文

**响应：**
- Content-Type: `text/event-stream`
- 格式: Coze SSE 原始事件流

## 注意事项

- ⚠️ `pat_token` 等敏感信息**绝对不能**硬编码在代码中
- ⚠️ 生产环境必须使用环境变量或配置中心（Nacos/Apollo）
- ⚠️ 确保 Coze Agent 已发布，否则 API 会返回 401
- ⚠️ 首次运行前必须先初始化数据库
- ⚠️ JWT Secret 在生产环境必须更换为高强度随机字符串
- ⚠️ 限流配置默认开启（30次/分钟），可根据实际情况调整
- ⚠️ **所有接口（除登录/注册外）都需要在请求头携带 `Authorization: Bearer {token}`**
- ⚠️ 流式分析接口使用 GET 方法，参数通过 Query String 传递
- ⚠️ 非流式分析接口使用 POST 方法，参数通过 JSON Body 传递
- ⚠️ 多轮对话需要保存并传递 `conversationId` 参数

## 快速测试

### 1. 启动服务

确保 MySQL 和 Redis 已启动，然后运行：

```bash
mvn spring-boot:run
```

### 2. 初始化数据库

```bash
mysql -u root -pcyf12345
source D:/code/ancinetbooks-Analyse/ancientbooks-pc-backend/src/main/resources/schema.sql
```

### 3. 测试登录

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

### 4. 测试对话接口

#### 方式1：流式分析（推荐）
```bash
curl -N "http://localhost:8080/api/agent/analyze/stream?content=讲解《道德经》第一篇&userId=admin"
```

#### 方式2：非流式分析
```bash
curl -X POST "http://localhost:8080/api/agent/analyze" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {token}" \
  -d '{"userId":"admin","content":"讲解《道德经》第一篇"}'
```

#### 方式3：旧版流式对话（兼容）
```bash
curl "http://localhost:8080/api/agent/stream/chat?query=你好&conversationId=" \
  -H "Authorization: Bearer {token}"
```

---

## 开发规范与最佳实践

### 1. 代码规范

#### 1.1 Java 编码规范

**命名规范**：
- ✅ **类名**：大驼峰，如 `AncientBookAgentController`
- ✅ **方法名**：小驼峰，如 `analyzeStream()`
- ✅ **变量名**：小驼峰，如 `conversationId`
- ✅ **常量名**：全大写下划线分隔，如 `MAX_RETRY_COUNT`
- ✅ **包名**：全小写，如 `com.ancientbooks.service`

**代码格式**：
- ✅ 使用 4 个空格缩进
- ✅ 每行不超过 120 字符
- ✅ 方法之间空一行
- ✅ 使用 Lombok 简化代码（`@Data`、`@Slf4j`）

**示例**：
```java
@Service
@Slf4j
@RequiredArgsConstructor
public class CozeServiceImpl implements CozeService {
    
    private final CozeProperties cozeProperties;
    private final WebClient.Builder webClientBuilder;

    @Override
    public String streamChat(String userId, String content, String conversationId) {
        log.info("调用Coze API，userId: {}, content: {}", userId, content);
        // 业务逻辑
        return result;
    }
}
```

---

#### 1.2 Controller 规范

**RESTful 风格**：
```java
// ✅ 推荐：使用名词复数
GET    /api/agent/analyze/stream     // 流式分析
POST   /api/agent/analyze            // 非流式分析
GET    /api/agent/conversation/{id}  // 获取会话

// ❌ 避免：使用动词
GET    /api/agent/doAnalyze
POST   /api/agent/getAnalysis
```

**响应格式统一**：
```java
// ✅ 成功响应
return Result.success(data);

// ✅ 失败响应
return Result.error("错误信息");

// ✅ 带状态码的失败
return Result.error(400, "参数错误");
```

**参数校验**：
```java
// ✅ 使用 @Valid 进行参数校验
public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request)

// ✅ DTO 中使用 JSR-380 注解
@Data
public class LoginRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 20, message = "用户名长度4-20位")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 32, message = "密码长度8-32位")
    private String password;
}
```

---

#### 1.3 Service 规范

**接口与实现分离**：
```java
// ✅ 接口定义
public interface CozeService {
    String streamChat(String userId, String content, String conversationId);
    String chat(String userId, String content, String conversationId);
}

// ✅ 实现类
@Service
@RequiredArgsConstructor
public class CozeServiceImpl implements CozeService {
    // 实现逻辑
}
```

**事务管理**：
```java
// ✅ 需要事务的方法添加 @Transactional
@Transactional(rollbackFor = Exception.class)
public void saveChatHistory(String query, String reply) {
    // 数据库操作
}
```

**异常处理**：
```java
// ✅ 抛出业务异常，不要返回 null
if (user == null) {
    throw new BusinessException("用户不存在");
}

// ❌ 避免返回 null
if (user == null) {
    return null;  // 不推荐
}
```

---

#### 1.4 DTO 规范

**用途区分**：
- ✅ **Request DTO**：入参，如 `LoginRequest`、`AncientBookAnalysisRequest`
- ✅ **Response DTO**：出参，如 `LoginResponse`、`CozeApiResponse`
- ✅ **统一返回**：使用 `Result<T>` 包装所有响应

**字段规则**：
```java
// ✅ 使用包装类型（Integer、Long）而非基本类型（int、long）
private Integer status;  // 允许 null
private Long id;

// ❌ 避免使用基本类型
private int status;  // 默认值 0，可能导致歧义
```

---

### 2. 安全规范

#### 2.1 认证与授权

**必须使用认证的接口**：
```java
// ✅ 所有业务接口必须认证
@GetMapping("/analyze/stream")
public SseEmitter analyzeStream(Authentication authentication) {
    String userId = authentication.getName();
    // ...
}

// ❌ 禁止公开访问业务接口
@GetMapping("/analyze/stream")
public SseEmitter analyzeStream() {  // 危险！
    // ...
}
```

**Token 处理**：
```java
// ✅ 从 SecurityContext 获取用户信息
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
String userId = principal.getUserId().toString();

// ❌ 禁止从请求参数获取用户ID（容易被伪造）
String userId = request.getParameter("userId");  // 危险！
```

---

#### 2.2 输入验证

**所有外部输入必须验证**：
```java
// ✅ DTO 层验证
@Data
public class AncientBookAnalysisRequest {
    @NotBlank(message = "分析内容不能为空")
    @Size(max = 5000, message = "内容不超过5000字")
    @Pattern(regexp = "^[\\s\\S]*$", message = "非法字符")
    private String content;
}

// ✅ Service 层二次验证
public String streamChat(String userId, String content) {
    if (content.length() > 5000) {
        throw new BusinessException("内容过长");
    }
    // ...
}
```

**SQL 注入防护**：
```java
// ✅ 使用 MyBatis-Plus 或参数化查询
@Select("SELECT * FROM user WHERE username = #{username}")
User findByUsername(String username);

// ❌ 禁止字符串拼接
@Select("SELECT * FROM user WHERE username = '" + username + "'")  // SQL注入！
```

---

#### 2.3 敏感信息处理

**密码处理**：
```java
// ✅ 使用 BCrypt 加密
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);  // 强度12
}

// ❌ 禁止明文存储
// password = "admin123"  // 绝对禁止！
```

**日志脱敏**：
```java
// ✅ 脱敏处理
log.info("用户登录：{}", maskSensitiveInfo(username));
// 输出：用户登录：ad***

// ❌ 禁止明文记录敏感信息
log.info("用户登录：{}", username);  // 危险！
```

**密钥管理**：
```yaml
# ✅ 使用环境变量
coze:
  pat-token: ${COZE_PAT_TOKEN:}

# ❌ 禁止硬编码
coze:
  pat-token: "pat_xxx"  // 危险！
```

---

### 3. 数据库规范

#### 3.1 命名规范

**表名**：
- ✅ 小写字母 + 下划线：`chat_history`、`ancient_book`
- ✅ 复数形式：`users`、`chat_histories`

**字段名**：
- ✅ 小写字母 + 下划线：`user_id`、`create_time`
- ✅ 使用 `id` 作为主键
- ✅ 统一使用 `deleted` 作为逻辑删除字段

**索引命名**：
```sql
-- ✅ 规范
CREATE INDEX idx_user_id ON chat_history(user_id);
CREATE INDEX idx_create_time ON chat_history(create_time);

-- ❌ 不规范
CREATE INDEX a ON chat_history(user_id);
```

---

#### 3.2 字段规范

**必须字段**：
```sql
-- ✅ 每个表必须包含以下字段
id               BIGINT PRIMARY KEY AUTO_INCREMENT
create_time      DATETIME DEFAULT CURRENT_TIMESTAMP
update_time      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
deleted          TINYINT DEFAULT 0  -- 逻辑删除
```

**字段类型**：
- ✅ 时间类型：`DATETIME`（不要用 TIMESTAMP）
- ✅ 金额类型：`DECIMAL(10,2)`
- ✅ 状态类型：`TINYINT`
- ✅ 文本类型：`TEXT`（长文本用 LONGTEXT）

---

#### 3.3 查询规范

**使用 MyBatis-Plus**：
```java
// ✅ Lambda 查询（类型安全）
List<User> users = lambdaQuery()
    .eq(User::getUsername, username)
    .eq(User::getStatus, 1)
    .list();

// ✅ 自定义 SQL 使用参数化
@Select("SELECT * FROM user WHERE username = #{username} AND deleted = 0")
User findByUsername(String username);

// ❌ 避免拼接 SQL
String sql = "SELECT * FROM user WHERE username = '" + username + "'";
```

**分页查询**：
```java
// ✅ 使用分页插件
IPage<User> page = lambdaQuery()
    .eq(User::getStatus, 1)
    .page(new Page<>(pageNum, pageSize));

// ❌ 避免一次性查询大量数据
List<User> allUsers = list();  // 数据量大时性能差
```

---

### 4. API 设计规范

#### 4.1 接口设计原则

**RESTful 风格**：
```
GET    /api/agent/analyze/stream     # 获取资源（流式）
POST   /api/agent/analyze            # 创建资源（非流式）
GET    /api/agent/conversation/{id}  # 根据ID查询
```

**统一响应格式**：
```json
{
  "code": 200,        // 状态码：200成功，400参数错误，401未认证，500服务器错误
  "msg": "success",   // 提示信息
  "data": {}          // 响应数据
}
```

**HTTP 状态码规范**：
| 状态码 | 说明 | 使用场景 |
|--------|------|----------|
| 200 | 成功 | 请求成功 |
| 201 | 已创建 | 资源创建成功 |
| 400 | 参数错误 | 参数校验失败 |
| 401 | 未认证 | Token 缺失或无效 |
| 403 | 无权限 | 权限不足 |
| 404 | 未找到 | 资源不存在 |
| 429 | 请求过多 | 限流 |
| 500 | 服务器错误 | 系统异常 |

---

#### 4.2 参数设计规范

**路径参数**：
```http
# ✅ 使用路径参数标识资源
GET /api/chat-history/{id}
DELETE /api/chat-history/{id}
```

**查询参数**：
```http
# ✅ 过滤、分页、排序用查询参数
GET /api/chat-history/list?userId=1&pageNum=1&pageSize=20
```

**请求体**：
```json
// ✅ POST/PUT 使用 JSON 请求体
{
  "userId": "1",
  "content": "讲解《道德经》"
}
```

---

#### 4.3 流式接口规范

**SSE 接口标准**：
```java
// ✅ 必须声明响应类型
@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)

// ✅ 设置合理的超时时间（3-5分钟）
SseEmitter emitter = new SseEmitter(180_000L);

// ✅ 正确关闭连接
emitter.complete();  // 正常关闭
emitter.completeWithError(e);  // 异常关闭
```

**SSE 事件格式**：
```
event: message
data: {"event":"conversation.message.delta","data":{"content":"增量内容"}}

event: error
data: {"error":"错误信息"}

event: message
data: [DONE]
```

---

### 5. 异常处理规范

#### 5.1 异常分类

**业务异常**：
```java
// ✅ 继承 BusinessException
public class UserNotFoundException extends BusinessException {
    public UserNotFoundException(String message) {
        super(404, message);
    }
}

// 使用
throw new UserNotFoundException("用户不存在");
```

**系统异常**：
```java
// ✅ 不捕获系统异常，让 GlobalExceptionHandler 处理
try {
    // 业务逻辑
} catch (BusinessException e) {
    throw e;  // 重新抛出业务异常
}
```

---

#### 5.2 全局异常处理

```java
// ✅ GlobalExceptionHandler 统一处理
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    public Result<String> handleBusinessException(BusinessException e) {
        log.warn("业务异常：{}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }
    
    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.error("系统繁忙，请稍后再试");
    }
}
```

---

### 6. 日志规范

#### 6.1 日志级别

| 级别 | 使用场景 | 示例 |
|------|---------|------|
| **ERROR** | 系统错误、异常 | 数据库连接失败、Coze API 调用失败 |
| **WARN** | 警告信息 | Token 过期、参数非法 |
| **INFO** | 重要业务日志 | 用户登录、订单创建 |
| **DEBUG** | 调试信息 | SQL 语句、参数详情（仅开发环境） |

---

#### 6.2 日志规范

**使用占位符**：
```java
// ✅ 使用 {} 占位符
log.info("用户登录：{}", userId);
log.error("Coze API 调用失败：{}", error.getMessage(), error);

// ❌ 避免字符串拼接
log.info("用户登录：" + userId);  // 性能差
```

**禁止打印敏感信息**：
```java
// ❌ 禁止
log.info("密码：{}", password);
log.info("Token：{}", token);

// ✅ 脱敏后打印
log.info("用户登录：{}", maskSensitiveInfo(username));
```

**必须包含上下文**：
```java
// ✅ 包含足够上下文
log.error("保存对话历史失败，userId: {}, conversationId: {}", 
    userId, conversationId, e);

// ❌ 信息不足
log.error("保存失败", e);  // 不知道是谁的对话历史
```

---

### 7. 测试规范

#### 7.1 单元测试

**测试覆盖率要求**：
- ✅ Service 层：≥ 80%
- ✅ Utils 工具类：≥ 90%
- ⚠️ Controller 层：≥ 60%
- ⚠️ Entity/DTO：可不测

**测试示例**：
```java
@SpringBootTest
class CozeServiceImplTest {
    
    @Autowired
    private CozeService cozeService;
    
    @Test
    void testStreamChat() {
        // Given
        String userId = "test_user";
        String content = "测试内容";
        
        // When
        String result = cozeService.chat(userId, content, null);
        
        // Then
        assertNotNull(result);
        assertTrue(result.length() > 0);
    }
}
```

---

#### 7.2 集成测试

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class AncientBookAgentControllerTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void testAnalyzeStream() {
        // 准备 Token
        String token = getLoginToken();
        
        // 调用接口
        ResponseEntity<String> response = restTemplate.exchange(
            "/api/agent/analyze/stream?content=测试",
            HttpMethod.GET,
            new HttpEntity<>(createHeaders(token)),
            String.class
        );
        
        // 验证
        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getHeaders().getContentType().toString().contains("text/event-stream"));
    }
}
```

---

### 8. Git 工作流

#### 8.1 分支管理

```
main/master      # 主分支，稳定可发布
   ↓
dev-cui         # 开发分支（当前分支）
   ↓
feature/*       # 功能分支，从 dev-cui 拉取
hotfix/*        # 热修复分支，从 main 拉取
```

**分支命名规范**：
- ✅ `feature/add-coze-v3-api`
- ✅ `hotfix/fix-jwt-blacklist`
- ✅ `bugfix/fix-conversation-id-leak`
- ❌ 避免：`test`、`new`、`update`

---

#### 8.2 提交信息规范

**格式**：
```
<type>(<scope>): <subject>

<body>

<footer>
```

**Type 类型**：
| 类型 | 说明 |
|------|------|
| `feat` | 新功能 |
| `fix` | 修复 Bug |
| `docs` | 文档更新 |
| `style` | 代码格式（不影响功能） |
| `refactor` | 重构 |
| `test` | 测试相关 |
| `chore` | 构建/工具相关 |

**示例**：
```
feat(coze): 添加Coze V3 API支持

- 新增CozeV3ChatRequest DTO
- 新增CozeService服务层
- 新增流式/非流式分析接口
- 更新CozeProperties配置类

Closes #123
```

---

#### 8.3 Code Review 规范

**必须审查的场景**：
- ✅ 所有提交到 `dev-cui` 的代码
- ✅ 所有合并到 `main` 的代码
- ✅ 涉及安全、认证、支付等关键逻辑

**审查清单**：
- [ ] 代码符合命名规范
- [ ] 没有硬编码密钥
- [ ] 输入验证完整
- [ ] 异常处理正确
- [ ] 日志不包含敏感信息
- [ ] SQL 使用参数化查询
- [ ] 单元测试通过
- [ ] 文档已更新

---

### 9. 数据库迁移规范

#### 9.1 版本管理

**使用 Flyway 或 Liquibase**：
```sql
-- 文件名：V1.2__add_user_table.sql
CREATE TABLE user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    -- ...
);
```

**禁止手动修改生产数据库**：
- ❌ 禁止直接在生产数据库执行 DDL/DML
- ✅ 必须通过迁移脚本更新
- ✅ 先在开发环境测试迁移脚本

---

### 10. 配置管理规范

#### 10.1 环境隔离

**配置文件**：
```
application.yml        # 公共配置
application-dev.yml    # 开发环境
application-test.yml   # 测试环境
application-prod.yml   # 生产环境
```

**敏感信息**：
```bash
# ✅ 使用环境变量
export COZE_PAT_TOKEN="xxx"
export JWT_SECRET="xxx"

# ✅ 使用配置中心（Nacos/Apollo）
# ❌ 禁止提交到代码仓库
```

---

### 11. 文档规范

#### 11.1 代码注释

**类注释**：
```java
/**
 * Coze API 服务实现类
 * 负责调用 Coze V3 Chat API 进行古籍分析
 *
 * @author 崔雨帆
 * @since 1.0.0
 */
@Service
public class CozeServiceImpl implements CozeService {
```

**方法注释**：
```java
/**
 * 流式调用 Coze API
 *
 * @param userId 用户ID
 * @param content 分析内容
 * @param conversationId 会话ID（可选）
 * @param callback 流式回调
 * @return 会话ID
 */
@Override
public String streamChat(String userId, String content, String conversationId) {
```

---

#### 11.2 API 文档

**使用 Swagger/OpenAPI**：
```java
@Operation(summary = "流式古籍分析")
@Parameter(name = "content", description = "分析内容", required = true)
@GetMapping(value = "/analyze/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter analyzeStream(AncientBookAnalysisRequest request) {
```

---

### 12. 性能规范

#### 12.1 数据库查询

**避免 N+1 查询**：
```java
// ✅ 使用联表查询或批量查询
List<ChatHistory> histories = chatHistoryMapper.selectWithUser(userId);

// ❌ 避免循环查询
for (ChatHistory history : histories) {
    User user = userMapper.selectById(history.getUserId());  // N+1！
}
```

**分页查询**：
```java
// ✅ 必须分页
IPage<ChatHistory> page = chatHistoryService.page(
    new Page<>(pageNum, pageSize),
    Wrappers.<ChatHistory>lambdaQuery().eq(USER_ID, userId)
);
```

---

#### 12.2 缓存规范

**合理使用缓存**：
```java
// ✅ 热点数据缓存（会话ID、用户信息）
redisTemplate.opsForValue().set("chat:conv:" + userId, convId, 30, TimeUnit.MINUTES);

// ✅ 设置过期时间，避免内存泄漏
redisTemplate.expire(key, 1, TimeUnit.HOURS);

// ❌ 禁止缓存大对象（>1MB）
redisTemplate.opsForValue().set("large_data", hugeObject);  // 危险！
```

---

#### 12.3 限流规范

**接口限流**：
```java
// ✅ 每个用户每分钟最多 30 次请求
@RateLimit(maxRequests = 30, windowSeconds = 60)
@GetMapping("/analyze/stream")
public SseEmitter analyzeStream() {
```

**Coze API 限流**：
```java
// ✅ 每个用户每小时最多调用 100 次 Coze API
Long count = redisTemplate.opsForValue().increment("coze:rate:" + userId);
if (count > 100) {
    throw new BusinessException("分析次数已达上限，请稍后再试");
}
```

---

### 13. 发布规范

#### 13.1 发布前检查清单

- [ ] 所有单元测试通过
- [ ] 集成测试通过
- [ ] 数据库迁移脚本已准备
- [ ] 配置文件已更新（环境变量）
- [ ] 安全漏洞已修复（无 P0/P1 级别问题）
- [ ] 性能测试通过（接口响应时间 < 2s）
- [ ] 文档已更新（README、API 文档）

---

#### 13.2 发布流程

```bash
# 1. 合并代码到 main 分支
git checkout main
git merge dev-cui

# 2. 更新版本号
mvn versions:set -DnewVersion=1.0.1

# 3. 打包
mvn clean package -DskipTests

# 4. 备份数据库
mysqldump -u root -p ancient_books_db > backup_$(date +%Y%m%d).sql

# 5. 执行数据库迁移
mysql -u root -p ancient_books_db < src/main/resources/db/migration/V1.2__xxx.sql

# 6. 部署
java -jar target/ancientbooks-analyse-pc-backend-1.0.1.jar

# 7. 验证
curl http://localhost:8080/api/health
```

---

### 14. 常见问题排查

#### 14.1 开发环境问题

**MySQL 连接失败**：
```bash
# 检查 MySQL 是否启动
net start MySQL80

# 测试连接
mysql -u root -pcyf12345 -e "SELECT 1"
```

**Redis 连接失败**：
```bash
# 检查 Redis 是否启动
redis-cli ping

# 检查配置
redis-cli config get requirepass
```

**端口被占用**：
```bash
# Windows 查看端口占用
netstat -ano | findstr :8080

# 杀死进程
taskkill /PID <进程ID> /F
```

---

#### 14.2 生产环境问题

**内存溢出**：
```bash
# 增加 JVM 堆内存
java -Xms512m -Xmx2048m -jar app.jar
```

**GC 频繁**：
```bash
# 查看 GC 日志
java -Xloggc:gc.log -XX:+PrintGCDetails -jar app.jar

# 分析日志
gcviewer gc.log
```

**慢查询**：
```sql
-- 开启慢查询日志
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 2;

-- 查看慢查询
SELECT * FROM mysql.slow_log;
```

---

### 15. 参考资源

**官方文档**：
- Spring Boot：https://spring.io/projects/spring-boot
- MyBatis-Plus：https://baomidou.com/
- JWT：https://jwt.io/
- Coze API：https://www.coze.cn/docs

**内部文档**：
- 后端开发规范：`.claude/skills/后端开发规范_SpringBoot3_MyBatis-Plus.md`
- 前端开发指南：`skills/frontend-dev-guide.md`
- 安全审查报告：`安全审查报告.md`

---

**规范制定**：崔雨帆
**最后更新**：2026-09-06
**版本**：v1.0


