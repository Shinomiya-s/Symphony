package com.qzh.symphony.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qzh.symphony.DAO.AiMessage;
import com.qzh.symphony.common.Result;
import com.qzh.symphony.mapper.AiMessageMapper;
import com.qzh.symphony.service.AiChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat/ai")
public class AiChatController {

    @Autowired
    private AiChatService aiChatService;
    @Autowired
    AiMessageMapper aiMessageMapper;

    //发消息并接收,流式回复
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@RequestBody Map<String, String> req) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = (String) auth.getPrincipal();

        String aiId = req.get("aiId");
        String message = req.get("message");

        //超时时间120秒
        SseEmitter emitter = new SseEmitter(120_000L);

        //保存用户消息，然后异步流式调Dify
        aiChatService.saveUserMessage(userId, aiId, message);
        aiChatService.streamAiResponse(userId, aiId, message, emitter);

        return emitter;
    }

    //获取历史消息
    @GetMapping("/{aiId}/messages")
    public Result getMessages(@PathVariable String aiId) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        List<AiMessage> messages = aiMessageMapper.selectList(
                new LambdaQueryWrapper<AiMessage>()
                        .eq(AiMessage::getUserId, userId)
                        .eq(AiMessage::getAiId, aiId)
                        .orderByAsc(AiMessage::getCreatedAt)
        );
        return Result.success(messages);
    }
}