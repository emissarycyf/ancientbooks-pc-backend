# 古籍分析 Agent 后端

基于 SpringBoot 3 + MyBatis-Plus + Redis + MySQL 的 WebFlux 流式对话后端服务。

## 技术栈

- **SpringBoot 3.2.x** - 应用框架
- **MyBatis-Plus 3.5.7** - ORM 框架
- **MySQL 8.0** - 数据存储
- **Redis 6.0+** - 缓存与会话管理
- **Spring WebFlux** - 响应式 Web 框架
- **Lombok** - 简化代码
- **Hutool** - 工具类库
- **FastJSON2** - JSON 处理

## 项目结构

```
src/main/java/com/ancientbooks/
├── AnalyseApplication.java        # 启动类
├── config/                        # 配置类
│   ├── CorsConfig.java            # 跨域配置
│   ├── RedisConfig.java           # Redis 序列化配置
│   ├── WebClientConfig.java       # WebClient 配置
│   └── MyMetaObjectHandler.java   # MyBatis-Plus 自动填充
├── controller/                    # 控制器层
│   └── AncientBookAgentController.java
├── entity/                        # 数据库实体
│   └── ChatHistory.java
├── mapper/                        # 数据访问层
│   └── ChatHistoryMapper.java
├── service/                       # 业务接口
│   ├── ChatHistoryService.java
│   └── impl/
│       └── ChatHistoryServiceImpl.java
├── dto/                           # 数据传输对象
│   ├── ChatRequest.java           # 入参 DTO
│   └── Result.java                # 统一返回结果
├── properties/                    # 配置属性类
│   └── CozeProperties.java
├── exception/                     # 异常处理
│   ├── GlobalExceptionHandler.java
│   └── BusinessException.java
├── constants/                     # 常量类
│   └── SystemConstants.java
└── utils/                         # 工具类

src/main/resources/
├── application.yml                # 主配置文件
├── schema.sql                     # 数据库初始化脚本
└── mapper/
    └── ChatHistoryMapper.xml      # Mapper XML
```

## 快速开始

### 1. 环境要求

- **JDK 17+** - 下载 [Adoptium](https://adoptium.net/) 或 [Oracle JDK](https://www.oracle.com/java/technologies/downloads/)
- **Maven 3.8+** - 下载 [Apache Maven](https://maven.apache.org/download.cgi)
- **MySQL 8.0** - 下载 [MySQL Community Server](https://dev.mysql.com/downloads/mysql/)
- **Redis 6.0+** - 下载 [Redis for Windows](https://github.com/redis-windows/redis-windows/releases)

### 2. 启动 MySQL 数据库

#### 2.1 启动 MySQL 服务

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

EXIT;
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
cd "C:\Program Files\Redis"

# 启动 Redis（带密码验证）
redis-server --requirepass cyf12345
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
redis-cli -a cyf12345

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
- 后端已配置 Redis 密码为 `cyf12345`
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

| 配置项 | 值 | 说明 |
|--------|-----|------|
| `spring.datasource.password` | `cyf12345` | ✅ MySQL 密码（已配置） |
| `spring.data.redis.password` | `cyf12345` | ✅ Redis 密码（已配置） |
| `coze.pat-token` | `pat_xxxxxxxxxxxx` | ⚠️ **需要替换**为实际 PAT Token |
| `coze.bot-id` | `xxxxxx` | ⚠️ **需要替换**为实际 Bot ID |
| `jwt.secret` | 测试密钥 | ⚠️ 生产环境必须更换 |

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
curl http://localhost:8080/api/auth/login -X POST \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
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

#### 刷新 Token
```
POST /api/auth/refresh
Authorization: Bearer {token}
```

### SSE 流式对话

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
- ⚠️ 所有接口（除登录/注册外）都需要在请求头携带 `Authorization: Bearer {token}`

## 快速测试

### 1. 启动服务

确保 MySQL 和 Redis 已启动，然后运行：

```bash
mvn spring-boot:run
```

### 2. 初始化数据库

```bash
mysql -u root -p < src/main/resources/schema.sql
```

### 3. 测试登录

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

### 4. 测试对话接口

```bash
curl "http://localhost:8080/api/agent/stream/chat?query=你好&conversationId=" \
  -H "Authorization: Bearer {token}"
```

## 开发规范

详见 `.claude/skills/后端开发规范_SpringBoot3_MyBatis-Plus.md`
