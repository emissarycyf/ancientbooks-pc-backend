package com.ancientbooks.controller;

import com.ancientbooks.dto.AncientBookAnalysisRequest;
import com.ancientbooks.dto.ChatRequest;
import com.ancientbooks.dto.Result;
import com.ancientbooks.entity.ChatHistory;
import com.ancientbooks.properties.CozeProperties;
import com.ancientbooks.security.UserPrincipal;
import com.ancientbooks.service.ChatHistoryService;
import com.ancientbooks.service.CozeService;
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
    private final CozeService cozeService;
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
            log.error("SSE 发送失败: {}", e.getMessage(), e);
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
        log.error("Coze API 错误: {}", err.getMessage(), err);
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
            log.error("保存对话历史失败: {}", e.getMessage(), e);
        }
    }

    private String getCurrentUserId() {
        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getUserId().toString();
        }

        // 未登录时的兜底值
        return "anonymous";
    }

    // ==================== 古籍分析接口 ====================

    /**
     * 古籍分析接口 - 流式返回
     * 前端提交分析内容，后端调用Coze API并流式返回结果
     *
     * @param request 分析请求
     * @return SSE 流式响应
     */
    @GetMapping(value = "/analyze/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter analyzeStream(AncientBookAnalysisRequest request) {
        SseEmitter emitter = new SseEmitter(180_000L);  // 3分钟超时

        // 手动参数校验（必须放在方法内部，避免GlobalExceptionHandler返回JSON）
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            try {
                emitter.send(SseEmitter.event().name("error").data("分析内容不能为空"));
            } catch (Exception e) {
                log.error("发送错误事件失败: {}", e.getMessage(), e);
            }
            emitter.complete();
            return emitter;
        }

        String userId = request.getUserId() != null ? request.getUserId() : getCurrentUserId();
        String content = request.getContent().trim();
        String conversationId = request.getConversationId();

        log.info("收到古籍分析请求，userId: {}, conversationId: {}, content: {}", userId, conversationId, content);

        // 调用 Coze Service 进行流式对话
        try {
            cozeService.streamChat(userId, content, conversationId, new com.ancientbooks.service.StreamCallback() {
                @Override
                public void onNext(String chunk) {
                    try {
                        emitter.send(SseEmitter.event().data(chunk));
                    } catch (Exception e) {
                        log.error("SSE 发送失败: {}", e.getMessage(), e);
                        emitter.completeWithError(e);
                    }
                }

                @Override
                public void onError(String error) {
                    log.error("Coze API 调用失败: {}", error);
                    try {
                        emitter.send(SseEmitter.event().name("error").data("分析失败: " + error));
                    } catch (Exception e) {
                        log.error("发送错误事件失败: {}", e.getMessage(), e);
                    }
                    emitter.complete();
                }

                @Override
                public void onComplete(String convId, String fullReply) {
                    try {
                        // 发送完成标记
                        emitter.send(SseEmitter.event().data("[DONE]"));
                    } catch (Exception e) {
                        log.error("发送完成事件失败: {}", e.getMessage(), e);
                    }
                    emitter.complete();

                    // 保存对话历史
                    saveChatHistory(content, fullReply, convId, userId);

                    // 缓存会话ID
                    if (convId != null && !convId.isBlank()) {
                        redisTemplate.opsForValue().set(
                                "chat:conv:" + userId, convId, 30, TimeUnit.MINUTES);
                    }
                }
            });
        } catch (Exception e) {
            log.error("调用 Coze Service 失败: {}", e.getMessage(), e);
            try {
                emitter.send(SseEmitter.event().name("error").data("系统异常: " + e.getMessage()));
            } catch (Exception ex) {
                log.error("发送错误事件失败: {}", ex.getMessage(), ex);
            }
            emitter.complete();
        }

        return emitter;
    }

    /**
     * 古籍分析接口 - 非流式返回（同步）
     * 前端提交分析内容，后端调用Coze API并返回完整结果
     *
     * @param request 分析请求
     * @return 分析结果
     */
    @PostMapping("/analyze")
    public Result<String> analyze(@Valid @RequestBody AncientBookAnalysisRequest request) {
        String userId = request.getUserId() != null ? request.getUserId() : getCurrentUserId();
        String content = request.getContent();
        String conversationId = request.getConversationId();

        log.info("收到古籍分析请求（非流式），userId: {}, content: {}", userId, content);

        try {
            String result = cozeService.chat(userId, content, conversationId);

            // 保存对话历史（使用当前时间作为conversationId的简化处理）
            saveChatHistory(content, result, conversationId, userId);

            return Result.success(result);
        } catch (Exception e) {
            log.error("古籍分析失败: {}", e.getMessage(), e);
            return Result.error("分析失败: " + e.getMessage());
        }
    }

    /**
     * 获取当前会话ID
     *
     * @param userId 用户ID
     * @return 会话ID
     */
    @GetMapping("/conversation/{userId}")
    public Result<String> getConversationId(@PathVariable String userId) {
        String conversationId = (String) redisTemplate.opsForValue().get("chat:conv:" + userId);
        return Result.success(conversationId != null ? conversationId : "");
    }
}
