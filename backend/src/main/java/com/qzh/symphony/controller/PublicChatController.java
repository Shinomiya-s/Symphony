package com.qzh.symphony.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.qzh.symphony.DAO.PublicMessage;
import com.qzh.symphony.common.Result;
import com.qzh.symphony.mapper.PublicMessageMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
@RestController
@RequestMapping("/api/chat/public")
public class PublicChatController {
    @Autowired
    PublicMessageMapper publicMessageMapper;
    @Autowired
    StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    //查询公屏聊天记录,优先查redis,再查mysql,寻找最近100条
    @GetMapping("/messages")
    public Result getMessages() {
        String key = "public:messages";
        List<String> cached = redisTemplate.opsForList().range(key, 0, -1);
        if (cached != null && !cached.isEmpty()) {
            List<PublicMessage> messages = cached.stream()
                    .map(json -> {
                        try {
                            return objectMapper.readValue(json, PublicMessage.class);
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            return Result.success(messages);
        }
        //Redis没有，查MySQL并回填
        List<PublicMessage> messages = publicMessageMapper.selectList(
                new LambdaQueryWrapper<PublicMessage>()
                        .orderByAsc(PublicMessage::getCreatedAt)
                        .last("LIMIT 100")
        );
        messages.forEach(m -> {
            try {
                redisTemplate.opsForList().rightPush(key, objectMapper.writeValueAsString(m));
            } catch (Exception e) {}
        });
        redisTemplate.expire(key, 24, TimeUnit.HOURS);
        return Result.success(messages);
    }
}