package com.qzh.symphony.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qzh.symphony.DAO.AiAssistant;
import com.qzh.symphony.DAO.AiMessage;
import com.qzh.symphony.mapper.AiAssistantMapper;
import com.qzh.symphony.mapper.AiMessageMapper;
import com.qzh.symphony.service.AiChatService;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AiChatServiceImpl implements AiChatService {
    @Autowired
    private AiAssistantMapper aiAssistantMapper;
    @Autowired
    private AiMessageMapper aiMessageMapper;

    @Value("${dify.api.url}")
    private String difyApiUrl;

    @Value("${dify.api.key}")
    private String difyApiKey;

    private final ObjectMapper mapper = new ObjectMapper();

    //存用户消息
    @Override
    public void saveUserMessage(String userId, String aiId, String content) {
        AiMessage msg = new AiMessage();
        msg.setUserId(userId);
        msg.setAiId(aiId);
        msg.setRole("user");
        msg.setContent(content);
        msg.setCreatedAt(LocalDateTime.now());
        aiMessageMapper.insert(msg);
        aiAssistantMapper.update(null, new LambdaUpdateWrapper<AiAssistant>()
                .eq(AiAssistant::getId, aiId)
                .setSql("dialog_count = dialog_count + 1")
        );
    }

    //存AI回复
    @Override
    public void saveAiMessage(String userId, String aiId, String content) {
        AiMessage msg = new AiMessage();
        msg.setUserId(userId);
        msg.setAiId(aiId);
        msg.setRole("assistant");
        msg.setContent(content);
        msg.setCreatedAt(LocalDateTime.now());
        aiMessageMapper.insert(msg);
    }

    //流式方法
    @Override
    @Async("aiTaskExecutor")
    public void streamAiResponse(String userId, String aiId, String userMessage, SseEmitter emitter) {
        StringBuilder fullResponse = new StringBuilder();

        try {
            AiAssistant ai = aiAssistantMapper.selectById(aiId);
            String singleType = (ai == null) ? "algorithmAssistant" : ai.getType().getValue();

            Map<String, Object> inputs = new HashMap<>();
            //构造请求
            inputs.put("chatMode", "single");
            inputs.put("singleType", singleType);
            inputs.put("prompt", ai.getPrompt());
            inputs.put("userId", userId);

            Map<String, Object> body = new HashMap<>();
            body.put("inputs", inputs);
            body.put("query", userMessage);
            body.put("response_mode", "streaming");
            body.put("user", userId);

            String bodyJson = mapper.writeValueAsString(body);

            URL url = new URL(difyApiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + difyApiKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "text/event-stream");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(120_000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(bodyJson.getBytes());
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data: ")) continue;

                    String jsonStr = line.substring(6).trim();
                    if (jsonStr.isEmpty()) continue;

                    JsonNode node = mapper.readTree(jsonStr);
                    String event = node.path("event").asText();

                    if ("node_finished".equals(event)) {
                        JsonNode data = node.path("data");
                        if ("llm".equals(data.path("node_type").asText())) {
                            String text = data.path("outputs").path("text").asText();
                            if (!text.isEmpty()) {
                                fullResponse.append(text);
                                Map<String, String> tokenData = new HashMap<>();
                                tokenData.put("token", text);
                                try {
                                    //捕获IOException=客户端已断开，停止推送
                                    emitter.send(SseEmitter.event().data(mapper.writeValueAsString(tokenData)));
                                } catch (IOException e) {
                                    System.out.println("客户端断开，停止推送: " + userId);
                                    return; //不报错，不存库，直接结束
                                }
                            }
                        }
                    } else if ("workflow_finished".equals(event)) {
                        break;
                    } else if ("error".equals(event)) {
                        String errMsg = node.path("message").asText("AI出错了");
                        try {
                            emitter.send(SseEmitter.event().name("error").data("{\"error\":\"" + errMsg + "\"}"));
                        } catch (IOException ignored) {}
                        break;
                    }
                }
            }

            //正常结束,通知前端done,存库
            try {
                emitter.send(SseEmitter.event().name("done").data("{\"done\":true}"));
                emitter.complete();
            } catch (java.io.IOException e) {
                System.out.println("推送done时客户端已断开: " + userId);
            }

        } catch (Exception e) {
            //只有真正的服务端错误才报错
            System.err.println("流式请求异常: " + e.getMessage());
            try {
                emitter.send(SseEmitter.event().name("error").data("{\"error\":\"AI暂时不可用\"}"));
                emitter.complete();
            } catch (Exception ignored) {}
        } finally {
            //无论正常结束还是客户端断开，只要拿到了回复就存库
            if (fullResponse.length() > 0) {
                saveAiMessage(userId, aiId, fullResponse.toString());
                System.out.println("AI回复已存库，长度: " + fullResponse.length());
            }
        }
    }
}