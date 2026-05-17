package com.qzh.symphony.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qzh.symphony.DAO.FriendshipAi;
import com.qzh.symphony.DAO.PrivateMessage;
import com.qzh.symphony.common.utils.DifyUtils;
import com.qzh.symphony.mapper.AccountMapper;
import com.qzh.symphony.mapper.PrivateMessageMapper;
import com.qzh.symphony.service.PrivateChatAiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.qzh.symphony.common.Constant.*;

@Service
public class PrivateChatAiServiceImpl implements PrivateChatAiService {
    @Autowired
    PrivateMessageMapper privateMessageMapper;
    @Autowired
    AccountMapper accountMapper;
    @Autowired
    StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Value("${dify.api.url}")
    private String difyApiUrl;
    @Value("${dify.api.key}")
    private String difyApiKey;
    @Autowired
    private DifyUtils difyUtils;

    //AI介入私聊,数据库双写
    @Override
    @Async("aiTaskExecutor")
    public void callAiAsync(String fromUserId, String toUserId, String message,
                            List<PrivateMessage> history, FriendshipAi friendshipAi) {
        try {
            String fromUsername = accountMapper.selectById(fromUserId).getUsername();
            String toUsername = accountMapper.selectById(toUserId).getUsername();

            //拼历史上下文
            StringBuilder context = new StringBuilder();
            List<PrivateMessage> reversed = new ArrayList<>(history);
            Collections.reverse(reversed);
            for (PrivateMessage h : reversed) {
                String sender = h.getFromUserId().equals(fromUserId) ? fromUsername : toUsername;
                context.append(sender).append(": ").append(h.getContent()).append("\n");
            }

            String prompt = "你是" + fromUsername + "和" + toUsername + "的学习助手，以下是他们的对话记录：\n" + context;
            String chatId = getConversationId(fromUserId, toUserId);
            Map<String, Object> inputs = new HashMap<>();
            inputs.put("chatMode", "dual_with_ai");
            inputs.put("prompt", prompt);
            inputs.put("userId", fromUserId);
            String aiReply = difyUtils.callDify(inputs, context.toString(), fromUserId);
            if (aiReply != null && !aiReply.isEmpty()) {
                LocalDateTime now = LocalDateTime.now();
                PrivateMessage aiMsg1 = new PrivateMessage();
                aiMsg1.setFromUserId("AI");
                aiMsg1.setToUserId(fromUserId);
                aiMsg1.setContent(aiReply.toString());
                aiMsg1.setType(1);
                aiMsg1.setAiVisible(1);
                aiMsg1.setCreatedAt(now);
                aiMsg1.setChatId(chatId);
                privateMessageMapper.insert(aiMsg1);
                String cacheKey = CACHE_KEY_PREFIX + chatId;
                stringRedisTemplate.opsForList().rightPush(cacheKey, messageToJson(aiMsg1));
                stringRedisTemplate.expire(cacheKey, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
                PrivateMessage aiMsg2 = new PrivateMessage();
                aiMsg2.setFromUserId("AI");
                aiMsg2.setToUserId(toUserId);
                aiMsg2.setContent(aiReply.toString());
                aiMsg2.setType(1);
                aiMsg2.setAiVisible(1);
                aiMsg2.setCreatedAt(now);
                aiMsg2.setChatId(chatId);
                privateMessageMapper.insert(aiMsg2);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("私聊AI调用失败: " + e.getMessage());
        }
    }

    //错题检测(每10条消息触发一次)
    @Async("aiTaskExecutor")
    public void detectMistakeAsync(String fromUserId, String toUserId) throws IOException {
        String pairKey = fromUserId.compareTo(toUserId) < 0
                ? fromUserId + "_" + toUserId
                : toUserId + "_" + fromUserId;

        //计数
        String countKey = "mistake:count:" + pairKey;
        Long count = stringRedisTemplate.opsForValue().increment(countKey);
        stringRedisTemplate.expire(countKey, 1, TimeUnit.DAYS);

        //没到10条直接return
        if (count == null || count % 10 != 0) return;

        // 触发后取最近20条消息喂给Dify分析（这两人之间，今天的）
        List<PrivateMessage> recentMessages = privateMessageMapper.selectList(
                Wrappers.<PrivateMessage>lambdaQuery()
                        .and(w -> w
                                .eq(PrivateMessage::getFromUserId, fromUserId)
                                .eq(PrivateMessage::getToUserId, toUserId))
                        .or(w -> w
                                .eq(PrivateMessage::getFromUserId, toUserId)
                                .eq(PrivateMessage::getToUserId, fromUserId))
                        .orderByDesc(PrivateMessage::getCreatedAt)
                        .last("LIMIT 20")
        );

        //调Dify
        String fromUsername = accountMapper.selectById(fromUserId).getUsername();
        String toUsername = accountMapper.selectById(toUserId).getUsername();
        StringBuilder context = new StringBuilder();
        List<PrivateMessage> reversed = new ArrayList<>(recentMessages);
        Collections.reverse(reversed);
        for (PrivateMessage msg : reversed) {
            String sender = msg.getFromUserId().equals(fromUserId) ? fromUsername : toUsername;
            context.append(sender).append(": ").append(msg.getContent()).append("\n");
        }
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("chatMode", "mistakeDetect");
        inputs.put("userId", fromUserId);
        String result = difyUtils.callDify(inputs, context.toString(), fromUserId);

        //解析Dify回复
        result = result.replaceAll("```json", "").replaceAll("```", "").trim();
        Map<String, Object> resultMap = mapper.readValue(result, new TypeReference<Map<String, Object>>() {});

        //false直接return
        if (!(boolean) resultMap.get("detected")) return;

        //true才存Redis
        String suggestJson = mapper.writeValueAsString(resultMap);
        stringRedisTemplate.opsForValue().set("mistake:suggest:" + fromUserId + ":" + pairKey, suggestJson, 30, TimeUnit.MINUTES);
        stringRedisTemplate.opsForValue().set("mistake:suggest:" + toUserId + ":" + pairKey, suggestJson, 30, TimeUnit.MINUTES);
    }

    //重复代码,与ChatUtils的方法一致
    private String getConversationId(String userId1, String userId2) {
        return userId1.compareTo(userId2) < 0
                ? userId1 + "_" + userId2
                : userId2 + "_" + userId1;
    }

    //Redis优先读消息
    @Override
    public List<PrivateMessage> getRecentMessages(String currentUserId, String targetUserId) {
        String conversationId = getConversationId(currentUserId, targetUserId);
        String cacheKey = CACHE_KEY_PREFIX + conversationId;
        try {
            //尝试从Redis获取最新50条
            List<String> cachedMessages = stringRedisTemplate.opsForList()
                    .range(cacheKey, -INITIAL_LOAD_SIZE, -1);
            if (cachedMessages != null && !cachedMessages.isEmpty()) {
                return parseMessages(cachedMessages);
            }
            //Redis未命中,从MySQL加载
            System.out.println("Redis缓存未命中,从MySQL加载: " + conversationId);
            List<PrivateMessage> messages = loadMessagesFromDB(currentUserId, targetUserId, null, INITIAL_LOAD_SIZE);
            //写入Redis缓存
            if (!messages.isEmpty()) {
                cacheMessagesToRedis(conversationId, messages);
            }
            return messages;
        } catch (Exception e) {
            e.printStackTrace();
            //降级:直接查数据库
            return loadMessagesFromDB(currentUserId, targetUserId, null, INITIAL_LOAD_SIZE);
        }
    }

    //加载历史消息
    @Override
    public List<PrivateMessage> loadMoreMessages(String currentUserId, String targetUserId, String beforeMessageId) {
        String conversationId = getConversationId(currentUserId, targetUserId);
        String cacheKey = CACHE_KEY_PREFIX + conversationId;
        String lockKey = LOCK_KEY_PREFIX + conversationId;
        try {
            //获取分布式锁,防并发
            Boolean locked = stringRedisTemplate.opsForValue()
                    .setIfAbsent(lockKey, "1", 5, TimeUnit.SECONDS);

            if (!locked) {
                System.out.println("获取锁失败,正在加载中: " + conversationId);
                return new ArrayList<>();
            }

            try {
                //从MySQL查询历史记录
                List<PrivateMessage> messages = loadMessagesFromDB(
                        currentUserId, targetUserId, beforeMessageId, MORE_LOAD_SIZE);

                if (messages.isEmpty()) {
                    return messages;
                }

                //插入到Redis列表头部
                List<String> jsonMessages = messages.stream()
                        .map(this::messageToJson)
                        .collect(Collectors.toList());

                if (!jsonMessages.isEmpty()) {
                    stringRedisTemplate.opsForList().leftPushAll(cacheKey, jsonMessages);
                }

                //限制缓存大小,超过500条就裁剪
                Long size = stringRedisTemplate.opsForList().size(cacheKey);
                if (size != null && size > MAX_CACHE_SIZE) {
                    stringRedisTemplate.opsForList().trim(cacheKey, -MAX_CACHE_SIZE, -1);
                }

                return messages;

            } finally {
                stringRedisTemplate.delete(lockKey);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    //发消息
    //同时双写数据库，超过500条裁剪，保持缓存不无限增长。
    @Override
    public void sendMessage(PrivateMessage message) {
        try {
            // 1. 写入MySQL
            privateMessageMapper.insert(message);

            // 2. 写入Redis
            String conversationId = getConversationId(message.getFromUserId(), message.getToUserId());
            String cacheKey = CACHE_KEY_PREFIX + conversationId;
            String jsonMessage = messageToJson(message);

            stringRedisTemplate.opsForList().rightPush(cacheKey, jsonMessage);
            stringRedisTemplate.expire(cacheKey, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);

            // 3. 限制缓存大小
            Long size = stringRedisTemplate.opsForList().size(cacheKey);
            if (size != null && size > MAX_CACHE_SIZE) {
                stringRedisTemplate.opsForList().trim(cacheKey, -MAX_CACHE_SIZE, -1);
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("消息发送失败", e);
        }
    }

    //辅助方法
    //从数据库加载消息
    private List<PrivateMessage> loadMessagesFromDB(String currentUserId, String targetUserId,
                                                    String beforeId, int limit) {
        LambdaQueryWrapper<PrivateMessage> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.and(w -> w
                .and(w1 -> w1
                        .eq(PrivateMessage::getFromUserId, currentUserId)
                        .eq(PrivateMessage::getToUserId, targetUserId)
                )
                .or(w2 -> w2
                        .eq(PrivateMessage::getFromUserId, targetUserId)
                        .eq(PrivateMessage::getToUserId, currentUserId)
                )
                .or(w3 -> w3
                        .eq(PrivateMessage::getFromUserId, "AI")
                        .eq(PrivateMessage::getToUserId, currentUserId)
                        .eq(PrivateMessage::getChatId, currentUserId.compareTo(targetUserId) < 0
                                ? currentUserId + "_" + targetUserId
                                : targetUserId + "_" + currentUserId)
                )
        );

        // 如果指定了beforeId,查询更早的消息
        if (beforeId != null) {
            PrivateMessage beforeMsg = privateMessageMapper.selectById(beforeId);
            if (beforeMsg != null) {
                queryWrapper.lt(PrivateMessage::getCreatedAt, beforeMsg.getCreatedAt());
            }
        }

        queryWrapper.orderByDesc(PrivateMessage::getCreatedAt)
                .last("LIMIT " + limit);

        List<PrivateMessage> messages = privateMessageMapper.selectList(queryWrapper);

        // 反转顺序(最早的在前)
        Collections.reverse(messages);

        return messages;
    }

    //批量缓存消息到redis
    private void cacheMessagesToRedis(String conversationId, List<PrivateMessage> messages) {
        String cacheKey = CACHE_KEY_PREFIX + conversationId;

        List<String> jsonMessages = messages.stream()
                .map(this::messageToJson)
                .collect(Collectors.toList());

        if (!jsonMessages.isEmpty()) {
            stringRedisTemplate.opsForList().rightPushAll(cacheKey, jsonMessages);
        }

        stringRedisTemplate.expire(cacheKey, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
    }

    //解析消息列表
    private List<PrivateMessage> parseMessages(List<String> jsonMessages) {
        return jsonMessages.stream()
                .map(this::jsonToMessage)
                .filter(msg -> msg != null)
                .collect(Collectors.toList());
    }

    //消息对象转JSON
    private String messageToJson(PrivateMessage message) {
        try {
            return mapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "{}";
        }
    }

    //JSON转消息对象
    private PrivateMessage jsonToMessage(String json) {
        try {
            return mapper.readValue(json, PrivateMessage.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }
}