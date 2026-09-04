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

- JDK 17+
- Maven 3.8+
- MySQL 8.0
- Redis 6.0+

### 2. 数据库配置

执行 `src/main/resources/schema.sql` 初始化数据库：

```bash
mysql -u root -p < src/main/resources/schema.sql
```

### 3. 修改配置

编辑 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    username: root
    password: 你的mysql密码
  data:
    redis:
      password: 你的redis密码

coze:
  pat-token: "pat_xxxxxxxxxxxx"  # 替换为实际的 PAT Token
  bot-id: "xxxxxx"               # 替换为实际的 Bot ID
```

### 4. 启动服务

```bash
mvn spring-boot:run
```

或 IDEA 直接运行 `AnalyseApplication.java`

## 接口说明

### SSE 流式对话

```
GET /api/agent/stream/chat?query={问题}&conversationId={会话ID}
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

## 开发规范

详见 `.claude/skills/后端开发规范_SpringBoot3_MyBatis-Plus.md`
