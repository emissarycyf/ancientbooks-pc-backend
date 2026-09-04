# 古籍分析 Agent 完整项目搭建（修正版 v2）

> 基于原文档全面修正，已修复参数校验、SSE解析、内存泄漏、XSS、自动填充等关键漏洞。  
> **本次额外修正**：前后端SSE消息格式对齐、前端变量作用域bug、接口分离、URL编码、冗余监听。

---

## 项目命名

- **后端**：`ancientbooks-analyse-pc-backend`（IDEA 开发）
- **前端**：`ancientbooks-analyse-pc-frontend`（Trae IDE 开发）

**技术栈**：Vue3 + Vite + Element-Plus | SpringBoot3 + MyBatis-Plus + Redis + MySQL | Coze 扣子 Agent

---

## 整体架构链路

```
Vue前端(Trae) → SpringBoot后端(IDEA)代理 → Coze网页版Agent
MySQL：存储用户、对话历史、古籍元数据
Redis：缓存conversation_id会话、限流、用户token、会话过期
Coze Agent：古籍解析、古文翻译、训诂、摘要、知识库检索
```

---

# 一、本地环境清单

1. JDK 17（SpringBoot3）
2. Maven 3.8+
3. Node.js 20 LTS（Vite）
4. MySQL 8.0
5. Redis 6.0+（推荐 GitHub 社区版 [redis-windows](https://github.com/redis-windows/redis-windows/releases)）
6. IDEA：打开 `ancientbooks-analyse-pc-backend`
7. Trae IDE：打开 `ancientbooks-analyse-pc-frontend`
8. 浏览器访问 [coze.cn](https://coze.cn)，注册登录，创建古籍分析 Agent

---

# 二、Coze 网页版：创建古籍分析 Agent

1. 进入 [coze.cn](https://coze.cn) → 新建 → **创建智能体 Agent**
   - Agent 名称：`古籍文献分析助手`
   - 简介：负责古籍文本翻译、训诂考证、段落摘要、背景解读、古籍知识库问答

2. **人设 & 提示词（直接复制）**

```
# 角色
你是专业古籍文献研究专家，精通经史子集、古文训诂、字词考据、历史背景考证。

## 能力
1. 古文白话翻译，逐句释义，生僻字注音；
2. 古籍段落摘要、主旨概括；
3. 考证文本出处、版本、时代背景；
4. 基于知识库内古籍PDF、文本资料回答问题；
5. 输出结构清晰，分章节、分点，引用原文片段。

## 约束
- 回答尽量严谨，给出史料来源；
- 如果知识库没有相关资料如实告知；
- 输出 markdown 格式，方便前端渲染。
```

3. **知识库**：上传古籍 PDF、txt 文本（四库节选、史书等），开启知识库检索。
4. **模型选择**：`Doubao-2.0-pro`，长文本能力强，适合古籍。
5. 右侧调试面板测试对话，确认 Agent 输出正常。
6. **发布智能体**（⚠️ 必须发布，API 才能调用）
7. 获取两个核心参数：
   - `bot_id`：智能体 ID
   - `pat_token`：个人访问令牌 PAT（右上角个人设置 → API 令牌）

> ⚠️ `pat_token` 只放在后端 `application.yml`，**绝对不能暴露给前端**。

---

# 三、后端 ancientbooks-analyse-pc-backend（IDEA）

## 步骤 1：初始化 SpringBoot 项目

IDEA → New Project → Spring Initializr

| 配置项 | 值 |
|--------|-----|
| Group | `com.ancientbooks` |
| Artifact | `ancientbooks-analyse-pc-backend` |
| Java | 17 |
| SpringBoot | 3.2.x |

**勾选依赖**：
- ✅ Spring Web
- ✅ Spring Data Redis
- ✅ MyBatis Framework
- ✅ MySQL Driver
- ✅ Lombok
- ✅ Spring Reactive Web（WebFlux，SSE 流式必须）
- ✅ **Validation（参数校验）**

---

## 步骤 2：pom.xml 补充依赖

```xml
<dependencies>
    <!-- SpringBoot Starter -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>

    <!-- MyBatis-Plus -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        <version>3.5.7</version>
    </dependency>

    <!-- MySQL -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Hutool 工具包 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.27</version>
    </dependency>

    <!-- JSON 处理 -->
    <dependency>
        <groupId>com.alibaba.fastjson2</groupId>
        <artifactId>fastjson2</artifactId>
        <version>2.0.51</version>
    </dependency>

    <!-- 测试 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## 步骤 3：完整 application.yml

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
    password: 你的mysql密码
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
      password: ""
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

# Coze 扣子配置
coze:
  base-url: https://api.coze.cn/open_api/v2/chat
  pat-token: "pat_xxxxxxxxxxxx"
  bot-id: "xxxxxx"

# 日志
logging:
  level:
    com.ancientbooks.mapper: debug
```

---

## 步骤 4：MySQL 初始化脚本

```sql
CREATE DATABASE IF NOT EXISTS ancient_books_db
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE ancient_books_db;

-- 对话历史表
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

-- 古籍元数据表（扩展用）
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
```

---

## 步骤 5：项目完整目录结构

```
ancientbooks-analyse-pc-backend/
├── src/main/java/com/ancientbooks/
│   ├── AncientBooksAnalysePcBackendApplication.java
│   ├── config/
│   │   ├── CorsConfig.java          # 跨域
│   │   ├── RedisConfig.java         # Redis序列化
│   │   ├── WebClientConfig.java     # WebFlux WebClient（含超时）
│   │   └── MyMetaObjectHandler.java # MyBatis-Plus自动填充
│   ├── controller/
│   │   └── AncientBookAgentController.java
│   ├── entity/
│   │   └── ChatHistory.java
│   ├── mapper/
│   │   └── ChatHistoryMapper.java
│   ├── service/
│   │   ├── ChatHistoryService.java
│   │   └── impl/
│   │       └── ChatHistoryServiceImpl.java
│   ├── dto/
│   │   ├── ChatRequest.java         # 入参DTO
│   │   └── Result.java             # 统一返回
│   ├── properties/
│   │   └── CozeProperties.java     # 配置属性类
│   └── exception/
│       └── GlobalExceptionHandler.java
├── src/main/resources/
│   ├── application.yml
│   └── mapper/
│       └── ChatHistoryMapper.xml
└── pom.xml
```

---

## 步骤 6：核心代码

### 6.1 统一返回结果 Result.java

```java
package com.ancientbooks.dto;

import lombok.Data;

@Data
public class Result<T> {
    private Integer code;
    private String msg;
    private T data;

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.setCode(200);
        r.setMsg("success");
        r.setData(data);
        return r;
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> error(String msg) {
        Result<T> r = new Result<>();
        r.setCode(500);
        r.setMsg(msg);
        return r;
    }

    public static <T> Result<T> error(Integer code, String msg) {
        Result<T> r = new Result<>();
        r.setCode(code);
        r.setMsg(msg);
        return r;
    }
}
```

### 6.2 入参 DTO ChatRequest.java（增加长度限制）

```java
package com.ancientbooks.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatRequest {
    @NotBlank(message = "提问内容不能为空")
    @Size(max = 5000, message = "单次输入不超过5000字")
    private String query;

    private String conversationId;
}
```

### 6.3 实体类 ChatHistory.java

```java
package com.ancientbooks.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("chat_history")
public class ChatHistory {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String userId;
    private String conversationId;
    private String userQuery;
    private String aiReply;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
```

### 6.4 Mapper 接口 ChatHistoryMapper.java

```java
package com.ancientbooks.mapper;

import com.ancientbooks.entity.ChatHistory;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatHistoryMapper extends BaseMapper<ChatHistory> {
}
```

### 6.5 Service 层 ChatHistoryService.java

```java
package com.ancientbooks.service;

import com.ancientbooks.entity.ChatHistory;
import com.baomidou.mybatisplus.extension.service.IService;

public interface ChatHistoryService extends IService<ChatHistory> {
}
```

```java
package com.ancientbooks.service.impl;

import com.ancientbooks.entity.ChatHistory;
import com.ancientbooks.mapper.ChatHistoryMapper;
import com.ancientbooks.service.ChatHistoryService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory> implements ChatHistoryService {
}
```

### 6.6 全局异常处理 GlobalExceptionHandler.java（补充校验异常）

```java
package com.ancientbooks.exception;

import com.ancientbooks.dto.Result;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BindException.class)
    public Result<String> handleValidation(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("参数校验失败");
        return Result.error(400, msg);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<String> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("参数校验失败");
        return Result.error(400, msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<String> handleConstraintViolation(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        return Result.error(400, msg);
    }

    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.error("系统繁忙，请稍后再试");
    }
}
```

### 6.7 Redis 配置 RedisConfig.java

```java
package com.ancientbooks.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        Jackson2JsonRedisSerializer<Object> jackson = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper om = new ObjectMapper();
        om.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        jackson.setObjectMapper(om);

        StringRedisSerializer string = new StringRedisSerializer();
        template.setKeySerializer(string);
        template.setHashKeySerializer(string);
        template.setValueSerializer(jackson);
        template.setHashValueSerializer(jackson);
        template.afterPropertiesSet();
        return template;
    }
}
```

### 6.8 WebClient 配置 WebClientConfig.java（增加超时）

```java
package com.ancientbooks.config;

import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .responseTimeout(Duration.ofSeconds(60));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));
    }
}
```

### 6.9 跨域配置 CorsConfig.java

```java
package com.ancientbooks.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // 生产环境建议指定具体域名，如 http://localhost:5173
        config.addAllowedOriginPattern("*");
        config.setAllowCredentials(false);  // 若前端不需要携带 cookie，设为 false 避免通配符冲突
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
```

### 6.10 MyBatis-Plus 自动填充配置 MyMetaObjectHandler.java

```java
package com.ancientbooks.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "deleted", Integer.class, 0);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
```

### 6.11 Coze 配置属性类 CozeProperties.java

```java
package com.ancientbooks.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "coze")
public class CozeProperties {
    private String baseUrl;
    private String patToken;
    private String botId;
}
```

### 6.12 Coze 古籍 Agent Controller（SSE 流式，完整修正版）

> **说明**：后端通过 `WebClient` 调用 Coze API，Coze 返回 `text/event-stream` 格式。  
> `bodyToFlux(String.class)` 会将每个 SSE 事件的 **data 字段内容**（已去除 `data: ` 前缀）作为 String 元素发出。  
> 后端直接将这些 JSON 字符串透传给前端，前端需按 Coze 格式解析。

```java
package com.ancientbooks.controller;

import com.ancientbooks.dto.ChatRequest;
import com.ancientbooks.dto.Result;
import com.ancientbooks.entity.ChatHistory;
import com.ancientbooks.properties.CozeProperties;
import com.ancientbooks.service.ChatHistoryService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
@Validated  // ← 必须加上，否则 @Valid 对非 @RequestBody 参数不生效
public class AncientBookAgentController {

    private final CozeProperties coze;
    private final WebClient.Builder webClientBuilder;
    private final ChatHistoryService chatHistoryService;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * SSE 流式对话接口
     * 透传 Coze 原始 SSE 事件给前端，前端按 Coze 格式解析
     */
    @GetMapping(value = "/stream/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@Valid ChatRequest request) {
        SseEmitter emitter = new SseEmitter(180_000L);  // 3分钟超时
        String query = request.getQuery();
        String conversationId = request.getConversationId();
        String userId = getCurrentUserId();  // 后续接入 JWT 时替换

        // 构建 Coze 请求体
        Map<String, Object> reqBody = new HashMap<>();
        reqBody.put("bot_id", coze.getBotId());
        reqBody.put("query", query);
        reqBody.put("conversation_id", conversationId == null ? "" : conversationId);
        reqBody.put("user", userId);
        reqBody.put("stream", true);

        StringBuilder fullReply = new StringBuilder();
        final String[] convIdHolder = {conversationId};

        Flux<String> flux = webClientBuilder.build()
                .post()
                .uri(coze.getBaseUrl())
                .header("Authorization", "Bearer " + coze.getPatToken())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(reqBody)
                .retrieve()
                .bodyToFlux(String.class);

        // 订阅并管理生命周期，防止内存泄漏
        Disposable disposable = flux.subscribe(
                chunk -> handleChunk(chunk, emitter, fullReply, convIdHolder),
                err -> handleError(err, emitter),
                () -> handleCompletion(emitter, query, fullReply.toString(), convIdHolder[0], userId)
        );

        // 客户端断开/超时/出错时取消订阅，释放资源
        emitter.onCompletion(() -> {
            if (!disposable.isDisposed()) disposable.dispose();
        });
        emitter.onTimeout(() -> {
            if (!disposable.isDisposed()) disposable.dispose();
            emitter.complete();
        });
        emitter.onError((e) -> {
            if (!disposable.isDisposed()) disposable.dispose();
        });

        return emitter;
    }

    private void handleChunk(String chunk, SseEmitter emitter, StringBuilder fullReply, String[] convIdHolder) {
        try {
            // 直接透传 Coze 的 SSE data 内容（WebClient 已去除 data: 前缀）
            emitter.send(SseEmitter.event().data(chunk));
            // 同时提取内容用于持久化
            extractContent(chunk, fullReply, convIdHolder);
        } catch (Exception e) {
            log.error("SSE 发送失败", e);
            emitter.completeWithError(e);
        }
    }

    /**
     * 解析 Coze SSE 返回格式，提取增量内容和 conversation_id
     * Coze 格式：{"event":"conversation.message.delta","data":{"content":"..."}}
     */
    private void extractContent(String chunk, StringBuilder fullReply, String[] convIdHolder) {
        // chunk 可能是单行 JSON，也可能是多行（以 \n 分隔）
        for (String line : chunk.split("\n")) {
            line = line.trim();
            if (line.isEmpty() || "[DONE]".equals(line)) continue;

            try {
                JsonNode root = objectMapper.readTree(line);
                String event = root.path("event").asText("");

                // 提取 conversation_id（可能在任意事件的 data 中）
                if (root.has("data") && root.get("data").has("conversation_id")) {
                    convIdHolder[0] = root.get("data").get("conversation_id").asText();
                }

                // 提取增量内容（conversation.message.delta 事件）
                if ("conversation.message.delta".equals(event) && root.has("data")) {
                    JsonNode msg = root.get("data");
                    if (msg.has("content")) {
                        fullReply.append(msg.get("content").asText());
                    }
                }
            } catch (Exception e) {
                log.warn("解析 Coze chunk 失败: {}", line, e);
            }
        }
    }

    private void handleError(Throwable err, SseEmitter emitter) {
        log.error("Coze API 错误", err);
        try {
            emitter.send(SseEmitter.event().name("error").data("Agent 调用失败: " + err.getMessage()));
        } catch (Exception ignored) {}
        emitter.completeWithError(err);
    }

    private void handleCompletion(SseEmitter emitter, String query, String reply, String convId, String userId) {
        try {
            // 使用默认事件名发送 [DONE]，前端 onmessage 可正常捕获
            emitter.send(SseEmitter.event().data("[DONE]"));
        } catch (Exception ignored) {}
        emitter.complete();

        // 保存对话记录到 MySQL
        saveChatHistory(query, reply, convId, userId);

        // 会话 ID 缓存 Redis，30 分钟过期
        if (convId != null && !convId.isBlank()) {
            redisTemplate.opsForValue().set(
                    "chat:conv:" + userId, convId, 30, TimeUnit.MINUTES);
        }
    }

    private void saveChatHistory(String query, String reply, String convId, String userId) {
        try {
            ChatHistory history = new ChatHistory();
            history.setUserId(userId);
            history.setUserQuery(query);
            history.setAiReply(reply);
            history.setConversationId(convId);
            chatHistoryService.save(history);
        } catch (Exception e) {
            log.error("保存对话历史失败", e);
        }
    }

    private String getCurrentUserId() {
        // TODO: 接入 JWT 后从 SecurityContext 获取真实用户ID
        return "anonymous";
    }
}
```

---

# 四、前端 ancientbooks-analyse-pc-frontend（Trae IDE）

## 步骤 1：初始化项目

```bash
npm create vite@latest . -- --template vue
npm install
```

## 步骤 2：安装依赖（新增 dompurify）

```bash
npm install element-plus axios vue-router@4 pinia marked highlight.js dompurify
```

## 步骤 3：环境变量 `.env.development`

```
VITE_API_BASE_URL=http://127.0.0.1:8080/api
```

## 步骤 4：main.js

```javascript
import { createApp } from 'vue'
import App from './App.vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import router from './router'
import { createPinia } from 'pinia'

const app = createApp(App)
app.use(ElementPlus)
app.use(router)
app.use(createPinia())
app.mount('#app')
```

## 步骤 5：axios 封装 src/utils/request.js

```javascript
import axios from 'axios'

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 120000
})

service.interceptors.response.use(
  res => res.data,
  err => {
    console.error('请求错误', err)
    return Promise.reject(err)
  }
)

export default service
```

## 步骤 6：SSE 接口封装 src/api/agentApi.js（新增，符合前端规范）

> **修正说明**：将 SSE 连接逻辑从页面组件中提取，按前端规范第六章要求统一放在 api/ 目录。

```javascript
const baseUrl = import.meta.env.VITE_API_BASE_URL

/**
 * 建立 SSE 流式对话连接
 * @param {string} query - 用户提问内容
 * @param {string} conversationId - 会话ID（可选）
 * @returns {EventSource} SSE 连接实例
 */
export const sendStreamChat = (query, conversationId = '') => {
  const url = `${baseUrl}/agent/stream/chat?query=${encodeURIComponent(query)}&conversationId=${encodeURIComponent(conversationId)}`
  return new EventSource(url)
}
```

## 步骤 7：路由 src/router/index.js

```javascript
import { createRouter, createWebHistory } from 'vue-router'
import AncientChat from '../views/chat/AncientChat.vue'

const routes = [
  { path: '/', component: AncientChat }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
```

## 步骤 8：古籍分析聊天页面（Markdown 渲染 + XSS 防护版 + SSE格式修正）

> **文件位置**：`src/views/chat/AncientChat.vue`（按前端规范按模块分子文件夹）  
> **关键修正**：
> 1. 变量 `data` 提取到 try 块外，修复作用域bug
> 2. JSON 解析适配 Coze 实际返回格式 `json.data.content`
> 3. 移除冗余的 `addEventListener('done')`
> 4. 使用 api/agentApi.js 封装 SSE 连接
> 5. conversationId 使用 encodeURIComponent 编码

```vue
<template>
  <div class="chat-container">
    <el-card class="chat-card">
      <template #header>
        <div class="card-header">
          <span>📜 古籍文献分析 Agent</span>
          <el-tag v-if="conversationId" type="success" size="small">会话中</el-tag>
        </div>
      </template>

      <div class="chat-history" ref="chatHistoryRef">
        <div v-for="(msg, index) in messages" :key="msg.id" :class="['msg-item', msg.role]">
          <div class="msg-avatar">{{ msg.role === 'user' ? '🧑' : '📚' }}</div>
          <div class="msg-content">
            <div v-if="msg.role === 'user'" class="user-text">{{ msg.content }}</div>
            <div v-else class="markdown-body" v-html="renderMarkdown(msg.content)"></div>
          </div>
        </div>
        <div v-if="loading" class="msg-item assistant">
          <div class="msg-avatar">📚</div>
          <div class="msg-content">
            <el-skeleton :rows="3" animated />
          </div>
        </div>
      </div>

      <div class="input-area">
        <el-input
          v-model="userInput"
          type="textarea"
          :rows="3"
          placeholder="粘贴古籍文本或输入问题，例如：翻译《论语》学而篇第一段"
          @keydown.enter.prevent="handleEnter"
        />
        <el-button
          type="primary"
          :loading="loading"
          @click="sendChat"
          style="margin-top: 12px"
        >
          {{ loading ? '分析中...' : '提交分析' }}
        </el-button>
        <el-button @click="clearChat" style="margin-top: 12px; margin-left: 8px">
          清空对话
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'
import DOMPurify from 'dompurify'
import { sendStreamChat } from '@/api/agentApi'

const userInput = ref('')
const messages = ref([])
const conversationId = ref('')
const loading = ref(false)
const chatHistoryRef = ref(null)

// 配置 marked（局部配置，避免全局污染）
const markedOptions = {
  highlight: (code, lang) => {
    if (lang && hljs.getLanguage(lang)) {
      return hljs.highlight(code, { language: lang }).value
    }
    return hljs.highlightAuto(code).value
  },
  breaks: true
}

const renderMarkdown = (text) => {
  const rawHtml = marked.parse(text || '', markedOptions)
  // XSS 防护：净化 HTML
  return DOMPurify.sanitize(rawHtml)
}

const scrollToBottom = () => {
  nextTick(() => {
    if (chatHistoryRef.value) {
      chatHistoryRef.value.scrollTop = chatHistoryRef.value.scrollHeight
    }
  })
}

const handleEnter = (e) => {
  if (!e.shiftKey) {
    sendChat()
  }
}

const sendChat = () => {
  const query = userInput.value.trim()
  if (!query || loading.value) return

  // 添加用户消息，绑定唯一 key
  messages.value.push({ role: 'user', content: query, id: Date.now() })
  userInput.value = ''
  loading.value = true
  scrollToBottom()

  // 创建 AI 消息占位
  const aiMsg = { role: 'assistant', content: '', id: Date.now() + 1 }
  messages.value.push(aiMsg)

  // 使用封装的 API 建立 SSE 连接
  const eventSource = sendStreamChat(query, conversationId.value)

  eventSource.onmessage = (event) => {
    // ✅ 修正：将 data 提取到 try 外部，避免 catch 块作用域错误
    const data = event.data

    if (data === '[DONE]') {
      eventSource.close()
      loading.value = false
      return
    }

    try {
      const json = JSON.parse(data)

      // ✅ 修正：适配 Coze SSE 实际返回格式
      // Coze 格式：{"event":"conversation.message.delta","data":{"content":"..."}}
      if (json.event === 'conversation.message.delta' && json.data?.content) {
        aiMsg.content += json.data.content
        scrollToBottom()
      }

      // ✅ 修正：提取 conversation_id（可能在 conversation.chat.created 或任意事件中）
      if (json.data?.conversation_id) {
        conversationId.value = json.data.conversation_id
      }
    } catch (e) {
      // 非 JSON 数据直接追加显示
      if (data && data !== '[DONE]') {
        aiMsg.content += data
        scrollToBottom()
      }
    }
  }

  eventSource.onerror = (err) => {
    console.error('SSE error', err)
    eventSource.close()
    loading.value = false
    if (!aiMsg.content) {
      aiMsg.content = '❌ 连接异常，请检查后端服务是否正常'
    }
  }
}

const clearChat = () => {
  messages.value = []
  conversationId.value = ''
}
</script>

<style scoped>
.chat-container {
  width: 90%;
  max-width: 1000px;
  margin: 30px auto;
}

.chat-card {
  min-height: 80vh;
  display: flex;
  flex-direction: column;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 18px;
  font-weight: bold;
}

.chat-history {
  flex: 1;
  max-height: 60vh;
  overflow-y: auto;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 16px;
}

.msg-item {
  display: flex;
  margin-bottom: 16px;
  gap: 12px;
}

.msg-item.user {
  flex-direction: row-reverse;
}

.msg-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.msg-content {
  max-width: 80%;
  padding: 12px 16px;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}

.msg-item.user .msg-content {
  background: #409eff;
  color: #fff;
}

.user-text {
  white-space: pre-wrap;
  word-break: break-word;
}

.markdown-body {
  line-height: 1.8;
}

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3) {
  margin-top: 16px;
  margin-bottom: 12px;
  color: #303133;
}

.markdown-body :deep(p) {
  margin-bottom: 8px;
}

.markdown-body :deep(code) {
  background: #f0f0f0;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: monospace;
}

.markdown-body :deep(pre) {
  background: #f6f8fa;
  padding: 12px;
  border-radius: 8px;
  overflow-x: auto;
}

.input-area {
  padding-top: 8px;
  border-top: 1px solid #e4e7ed;
}
</style>
```

## 步骤 9：App.vue

```vue
<template>
  <router-view />
</template>
```

## 步骤 10：启动前端

```bash
npm run dev
```

访问 `http://localhost:5173`

---

# 五、完整联调流程

1. **启动 MySQL**：创建数据库 `ancient_books_db`，执行初始化 SQL
2. **启动 Redis**：`redis-server.exe redis.conf`
3. **启动后端**：IDEA 运行主类，端口 `8080`
4. **启动前端**：`npm run dev`，端口 `5173`
5. **Coze 配置**：确认 `bot_id`、`pat_token` 正确，Agent 已发布
6. **测试链路**：前端输入古籍问题 → SpringBoot 代理 → Coze Agent → SSE 流式返回 → Markdown 渲染

---

# 六、关键修正说明（相比原版）

| 修正项 | 原问题 | 修正方案 |
|--------|--------|----------|
| **参数校验** | `@Valid` 对 GET 参数不生效 | Controller 加 `@Validated`，ExceptionHandler 补 `ConstraintViolationException` |
| **Coze SSE 解析** | 用字符串索引解析 JSON，极易出错 | 使用 Jackson 按事件类型解析，正确处理 `conversation.message.delta` |
| **自动填充** | 缺少 `MetaObjectHandler`，时间字段不填充 | 新增 `MyMetaObjectHandler` 组件 |
| **内存泄漏** | Flux 订阅无取消机制 | 用 `Disposable` 管理订阅，在 `onCompletion/onTimeout` 中释放 |
| **CORS 兼容** | `*` + `credentials=true` 冲突 | `allowCredentials=false` 或指定具体域名 |
| **XSS 安全** | `v-html` 直接渲染，无过滤 | 引入 `DOMPurify` 净化 HTML |
| **SSE [DONE]** | 用 `name("done")` 发送，前端 onmessage 捕获不到 | 统一用默认事件名发送 `[DONE]` |
| **配置注入** | `@Value` 与 `@RequiredArgsConstructor` 混用 | 使用 `@ConfigurationProperties` 封装配置 |
| **WebClient 超时** | 无超时配置，网络异常挂起 | 配置 `CONNECT_TIMEOUT` 和 `responseTimeout` |
| **Redis 缓存** | 仅注释，空实现 | 实际注入 `RedisTemplate` 写入会话缓存 |
| **输入长度** | 无限制，URL 可能超长 | `@Size(max=5000)` 限制输入长度 |
| **数据库连接池** | 未配置 HikariCP | 补充 `maximum-pool-size` 等参数 |
| **SSE 格式对齐** | 后端透传 Coze 格式，前端期望简化格式 | 前端解析逻辑适配 Coze 实际格式 `json.data.content` |
| **变量作用域** | `const data` 在 try 内，catch 访问报错 | 将 `data` 提取到 try 块外部 |
| **接口分离** | API 调用直接写在页面组件 | 提取到 `api/agentApi.js`，符合前端规范 |
| **URL 编码** | `conversationId` 未编码 | 使用 `encodeURIComponent` |
| **冗余监听** | `addEventListener('done')` 不会触发 | 移除冗余代码 |

---

# 七、高频踩坑 & 解决

| 问题 | 原因 | 解决 |
|------|------|------|
| Coze 返回 401 | `pat_token` 错误或过期 / Agent 未发布 | 检查 token 完整性，确认 Agent 已发布 |
| SSE 无流式输出 | 没引入 `spring-boot-starter-webflux` / `stream` 参数为 false | 确认依赖和 `stream: true` |
| 多轮对话丢失上下文 | 没保存 `conversation_id` | 每次请求带上上次返回的 `conversation_id`，并缓存到 Redis |
| 前端跨域报错 | 后端没配 CORS / 端口不一致 | 检查 `CorsConfig` 和 `VITE_API_BASE_URL` |
| Redis 连接失败 | Redis 没启动 / 密码错误 | 检查 Redis 服务状态和 `application.yml` 配置 |
| MyBatis-Plus 扫描不到 Mapper | 没加 `@Mapper` 或没配 `mapper-locations` | 检查注解和 YAML 配置 |
| Markdown 没渲染 | 没安装 `marked` / 样式没引入 | 检查依赖和 `v-html` 绑定 |
| 参数校验不生效 | Controller 没加 `@Validated` | 在类上加 `@Validated` 注解 |
| create_time 为 null | 没配置 `MetaObjectHandler` | 新增 `MyMetaObjectHandler` 并注册为 Bean |
| 关闭浏览器后后端继续运行 | SSE 订阅未取消 | 使用 `Disposable` 并在回调中 `dispose()` |
| 前端显示原始 JSON | 前后端 SSE 格式未对齐 | 确认前端按 Coze 格式 `json.data.content` 解析 |

---

# 八、后续扩展方向

1. **用户系统**：JWT 登录注册，区分多用户会话（替换 `getCurrentUserId()`）
2. **文件上传**：前端上传古籍 PDF/TXT，后端转存并对接 Coze 知识库
3. **古籍元数据管理**：朝代、作者、书目分类管理页面
4. **限流防刷**：Redis + Sentinel / Bucket4j 限制 Coze API 调用频率
5. **对话历史分页**：MySQL 分页查询，支持按时间筛选
6. **Docker 部署**：前后端 + MySQL + Redis 一键编排
7. **停止生成**：增加 `POST /agent/stop/{emitterId}` 接口，支持中断长文本生成
8. **健康检查**：引入 Spring Boot Actuator，增加 `/actuator/health`
