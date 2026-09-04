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
