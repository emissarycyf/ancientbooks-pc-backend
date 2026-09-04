@# 后端开发规范（SpringBoot3 + MyBatis-Plus）

> 配套前端 Vue3 开发规范，确保前后端协作风格一致、接口契约清晰。

---

## 一、基础代码风格规范

| 规范项 | 要求 |
|--------|------|
| **语法** | 统一使用 Java 17 + SpringBoot 3.2.x；类名大驼峰（`UserService`），方法/变量名小驼峰（`getUserList`）；常量全大写下划线（`MAX_RETRY_COUNT`）。 |
| **异步** | 外部 HTTP 调用（如 Coze API）统一使用 `WebClient` + `Flux/Mono` 响应式编程；内部业务逻辑如需异步使用 `@Async` + `CompletableFuture`。 |
| **命名** | 见名知意，禁止 `a`、`b`、`temp`、`fn1`、`data1` 等无意义命名；Mapper 接口名与实体对应（`UserMapper`），Service 接口名 `XxxService`，实现类 `XxxServiceImpl`。 |
| **变量声明** | 优先使用 `final` 修饰不可变变量；集合初始化优先使用 `List.of()`、`Map.of()`（不可变）或 `new ArrayList<>()`；禁止魔法数字，提取为常量。 |
| **注释** | 复杂业务逻辑、算法、自定义校验必须加 `/** */` 文档注释；简单 Getter/Setter 无需注释；禁止无意义注释（如 `// 获取用户` 对应 `getUser()`）。 |
| **Lombok** | 实体类统一使用 `@Data`，但 DTO/VO 视情况使用 `@Getter @Setter` 避免过度生成；Builder 模式优先用 `@Builder`。 |

---

## 二、分层架构规范

严格按职责拆分，**禁止跨层调用**（如 Controller 直接调 Mapper）。

```
┌─────────────┐
│  Controller │  ← 只处理 HTTP 请求/响应，不做业务逻辑
├─────────────┤
│   Service   │  ← 封装业务逻辑，事务控制（@Transactional）
├─────────────┤
│   Mapper    │  ← 数据访问，MyBatis-Plus BaseMapper
├─────────────┤
│   Entity    │  ← 数据库映射，与表结构一一对应
└─────────────┘
```

| 层级 | 职责 | 禁止事项 |
|------|------|----------|
| **Controller** | 接收参数、调用 Service、返回 `Result<T>` | 禁止写业务逻辑、禁止直接操作数据库 |
| **Service** | 业务编排、事务管理、数据转换 | 禁止返回 Entity，必须返回 DTO/VO |
| **Mapper** | SQL 映射、复杂查询写 XML | 禁止在 Mapper 中写业务判断 |
| **Entity** | 与数据库表一一对应，仅含字段 + 注解 | 禁止在 Entity 中写业务方法 |
| **DTO** | 入参/出参数据传输对象，按接口隔离 | 禁止复用 Entity 作为接口入参 |

---

## 三、RESTful API 设计规范

| 规范项 | 要求 |
|--------|------|
| **URL 语义化** | 资源名词复数，禁止动词。如 `GET /api/users`（查列表）、`POST /api/users`（新增）、`PUT /api/users/{id}`（全量更新）、`PATCH /api/users/{id}`（局部更新）、`DELETE /api/users/{id}`（删除）。 |
| **HTTP 方法** | 严格对应：GET 查、POST 增、PUT/PATCH 改、DELETE 删；查询用 GET，提交用 POST/PUT。 |
| **路径风格** | 统一前缀 `/api`；版本控制 `/api/v1/users`；路径参数 `{id}` 必须为唯一标识。 |
| **响应格式** | 统一返回 `Result<T>` 包装，结构：`{ "code": 200, "msg": "success", "data": {} }`；禁止直接返回裸字符串/裸对象。 |
| **状态码** | 业务成功 `code=200`；参数错误 `code=400`；未授权 `code=401`；禁止 `code=500` 暴露给前端（全局异常处理拦截后包装）。 |
| **SSE 流式** | 流式接口路径以 `/stream/` 标识，如 `GET /api/agent/stream/chat`，`produces = MediaType.TEXT_EVENT_STREAM_VALUE`。 |

---

## 四、参数校验规范

| 规范项 | 要求 |
|--------|------|
| **入参校验** | 所有接口入参必须使用 JSR-380 注解（`@NotBlank`、`@NotNull`、`@Size`、`@Email` 等），配合 `@Valid` 在 Controller 层校验。 |
| **DTO 隔离** | 每个接口独立 DTO，禁止一个 DTO 到处复用；查询参数用 `XxxQueryDTO`，提交用 `XxxFormDTO`。 |
| **校验提示** | 提示语必须清晰，如 `@NotBlank(message = "提问内容不能为空")`；禁止前端传什么就报什么英文错误。 |
| **自定义校验** | 复杂校验逻辑写在 `Validator` 类中，通过 `@Constraint` 自定义注解，禁止写在 Controller/Service 里用 `if` 判断。 |
| **分组校验** | 新增/更新场景不同校验规则时，使用 `groups` 分组（如 `CreateGroup.class`、`UpdateGroup.class`）。 |

---

## 五、异常处理规范

| 规范项 | 要求 |
|--------|------|
| **全局拦截** | 必须使用 `@RestControllerAdvice` + `@ExceptionHandler` 做统一异常拦截，返回标准化 `Result<T>`。 |
| **异常分类** | 业务异常（`BusinessException`）、参数异常（`ValidationException`）、系统异常（`Exception`）分别处理。 |
| **错误信息** | 生产环境禁止返回堆栈信息给前端；日志中记录完整堆栈；用户提示统一为 `"系统繁忙，请稍后再试"`。 |
| **日志级别** | 业务异常打 `WARN`，系统异常打 `ERROR`，调试信息打 `DEBUG`；禁止 `System.out.println`。 |
| **事务回滚** | Service 层抛异常时，`@Transactional` 默认回滚 `RuntimeException`，如需回滚受检异常需指定 `rollbackFor`。 |

---

## 六、数据库 & MyBatis-Plus 规范

| 规范项 | 要求 |
|--------|------|
| **表设计** | 主键 `BIGINT AUTO_INCREMENT`；必须含 `create_time`、`update_time`、`deleted`（逻辑删除）；字符集 `utf8mb4_unicode_ci`。 |
| **字段命名** | 下划线命名（`user_name`），实体类驼峰映射（`userName`），通过 `map-underscore-to-camel-case: true` 自动转换。 |
| **逻辑删除** | 统一使用 MyBatis-Plus `@TableLogic`，禁止物理删除业务数据；删除值 `1`，未删除值 `0`。 |
| **SQL 写法** | 简单 CRUD 用 MyBatis-Plus 内置方法（`save`、`update`、`page`）；复杂查询写 XML，禁止在 Java 中拼接 SQL 字符串。 |
| **分页查询** | 统一使用 `Page<T>` 对象，禁止手写 `LIMIT`；分页参数必须校验（`pageNum >= 1`，`pageSize <= 100`）。 |
| **关联查询** | 禁止 N+1 查询，复杂关联使用 `JOIN` 一次性查出；懒加载场景显式配置。 |
| **敏感字段** | 密码等敏感字段禁止在 DTO/VO 中暴露；查询时显式排除或单独处理。 |

---

## 七、日志规范

| 规范项 | 要求 |
|--------|------|
| **日志框架** | 统一使用 SLF4J + Logback，`private static final Logger log = LoggerFactory.getLogger(Xxx.class)` 或 Lombok `@Slf4j`。 |
| **日志级别** | `DEBUG`：调试信息、入参出参；`INFO`：业务流程关键节点；`WARN`：业务异常、可恢复错误；`ERROR`：系统异常、第三方接口失败。 |
| **关键日志** | 所有外部 API 调用（Coze、文件上传等）必须记录请求参数、响应状态、耗时；核心业务流程（登录、支付、对话）必须留痕。 |
| **禁止事项** | 禁止输出敏感信息（token、密码、身份证号）；禁止 `System.out.println`；禁止生产环境 `DEBUG` 级别全开。 |

---

## 八、接口安全 & 通用规范

| 规范项 | 要求 |
|--------|------|
| **密钥管理** | `pat_token`、数据库密码、Redis 密码等必须放在 `application.yml`，**禁止硬编码**；生产环境使用环境变量或配置中心（Nacos/Apollo）。 |
| **防重复提交** | 提交类接口（对话、保存）必须使用 Redis 分布式锁或 Token 机制防重；查询类接口使用 `loading` 状态控制。 |
| **限流** | Coze API 等外部调用必须加限流（Redis + Sentinel 或 Guava RateLimiter），防止刷爆配额。 |
| **跨域** | 统一在 `CorsConfig` 中配置，禁止在 Controller 中单独加 `@CrossOrigin`。 |
| **会话管理** | `conversation_id` 缓存 Redis，设置过期时间（如 30 分钟）；用户 token 使用 JWT，过期时间合理设置。 |
| **文件上传** | 限制文件类型、大小；上传路径禁止暴露绝对路径；文件名使用 UUID 重命名。 |

---

## 九、目录结构规范

```
src/main/java/com/ancientbooks/
├── config/              # 配置类（Cors、Redis、WebClient、拦截器等）
├── controller/          # 控制器层，按模块分子包（agent/、user/、book/）
├── service/             # 业务接口
│   └── impl/            # 业务实现
├── mapper/              # 数据访问层
├── entity/              # 数据库实体
├── dto/                 # 数据传输对象（request/、response/）
├── vo/                  # 视图对象（返回给前端的专用结构）
├── exception/           # 自定义异常 + 全局异常处理
├── enums/               # 枚举类（状态码、业务类型等）
├── utils/               # 工具类（日期、加密、文件等）
├── constants/           # 常量池
└── aspect/              # AOP 切面（日志、权限、限流）

src/main/resources/
├── application.yml      # 主配置
├── application-dev.yml  # 开发环境
├── application-prod.yml # 生产环境
└── mapper/              # XML 映射文件
```

---

## 十、禁止项

- ❌ **禁止 Controller 写业务逻辑**，只能做参数校验和结果包装。
- ❌ **禁止 Service 返回 Entity**，必须转换为 DTO/VO。
- ❌ **禁止在 Java 代码中拼接 SQL**，复杂查询写 XML。
- ❌ **禁止接口不做 try/catch**，所有外部调用必须捕获异常。
- ❌ **禁止魔法数字和硬编码字符串**，必须提取为常量。
- ❌ **禁止生产环境输出 DEBUG 日志和堆栈信息**。
- ❌ **禁止将 `pat_token`、数据库密码等敏感信息提交到 Git**。
- ❌ **禁止跨层调用**（Controller 直接调 Mapper）。
- ❌ **禁止裸返回**（接口必须包 `Result<T>`）。
- ❌ **禁止在 Entity/DTO 中写业务方法**。
