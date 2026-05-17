package com.qzh.symphony.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AiChatService {
    void saveUserMessage(String userId, String aiId, String content);
    void saveAiMessage(String userId, String aiId, String content);
    void streamAiResponse(String userId, String aiId, String userMessage, SseEmitter emitter);
}