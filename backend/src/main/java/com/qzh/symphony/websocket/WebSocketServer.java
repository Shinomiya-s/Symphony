package com.qzh.symphony.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.qzh.symphony.DAO.PublicMessage;
import com.qzh.symphony.common.utils.JwtUtils;
import com.qzh.symphony.mapper.AccountMapper;
import com.qzh.symphony.mapper.PublicMessageMapper;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
@ServerEndpoint("/symphony")
public class WebSocketServer {
    private static JwtUtils jwtUtils;
    private static AccountMapper accountMapper;
    private static PublicMessageMapper publicMessageMapper;
    private static StringRedisTemplate redisTemplate;
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private static final String PUBLIC_MSG_KEY = "public:messages";
    private static final long MSG_TTL_HOURS = 24;

    private void saveToMysqlAsync(PublicMessage publicMessage) {
        new Thread(() -> publicMessageMapper.insert(publicMessage)).start();
    }

    @Autowired
    public void setRedisTemplate(StringRedisTemplate redisTemplate) {
        WebSocketServer.redisTemplate = redisTemplate;
    }
    @Autowired
    public void setJwtUtils(JwtUtils jwtUtils) {
        WebSocketServer.jwtUtils = jwtUtils;
    }
    @Autowired
    public void setAccountMapper(AccountMapper accountMapper) {
        WebSocketServer.accountMapper = accountMapper;
    }
    @Autowired
    public void setPublicMessageMapper(PublicMessageMapper publicMessageMapper) {WebSocketServer.publicMessageMapper = publicMessageMapper;}

    //key=userId, value=连接，一个用户只能有一个连接
    private static ConcurrentHashMap<String, Session> sessionMap = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session) throws IOException {
        try {
            String token=session.getRequestParameterMap().get("token").get(0);
            String userId=jwtUtils.getUserIdFromToken(token);
            //token无效直接踢出
            if(userId == null) {
                try {
                    session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "token无效"));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else{
                //同一用户新连接挤掉旧连接
                if(sessionMap.containsKey(userId))
                    try {
                        sessionMap.get(userId).close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "新连接已建立"));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                String username = accountMapper.selectById(userId).getUsername();
                session.getUserProperties().put("userId", userId);
                session.getUserProperties().put("username", username);
                sessionMap.put(userId,session);
                StringBuilder onlineList = new StringBuilder("在线：");
                Map<String, Object> joinMsg = new HashMap<>();
                joinMsg.put("messageType", "SYSTEM");
                joinMsg.put("content", username + "进来了");
                String joinJson = objectMapper.writeValueAsString(joinMsg);

                for (Map.Entry<String, Session> entry : sessionMap.entrySet()) {
                    entry.getValue().getBasicRemote().sendText(joinJson);
                    onlineList.append(entry.getValue().getUserProperties().get("username")).append(" ");
                }
                Map<String, Object> listMsg = new HashMap<>();
                listMsg.put("messageType", "SYSTEM");
                //只给自己发在线列表
                listMsg.put("content", onlineList.toString());
                String listJson = objectMapper.writeValueAsString(listMsg);

                session.getBasicRemote().sendText(listJson);
            }
        } catch (Exception e) {
            System.err.println("WebSocket连接建立失败: " + e.getMessage());
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, "服务器错误"));
            } catch (IOException ioException) {
                System.err.println("关闭连接失败: " + ioException.getMessage());
            }
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) throws IOException {
        try{
            String userId = (String) session.getUserProperties().get("userId");
            String username = (String) session.getUserProperties().get("username");
            if (userId == null || username == null) {
                System.err.println("用户信息为空，无法处理消息");
                return;
            }
            Map<String, Object> map = new HashMap<>();
            map.put("messageType", "CHAT");
            map.put("fromId", userId);
            map.put("fromName", username);
            map.put("content", message);
            map.put("createdAt", new Date());
            String json = objectMapper.writeValueAsString(map);
            PublicMessage publicMessage = new PublicMessage();
            publicMessage.setFromUserId(userId);
            publicMessage.setFromUsername(username);
            publicMessage.setContent(message);
            publicMessage.setCreatedAt(LocalDateTime.now());
            String msgJson = objectMapper.writeValueAsString(publicMessage);
            //存Redis
            redisTemplate.opsForList().rightPush(PUBLIC_MSG_KEY, msgJson);
            redisTemplate.expire(PUBLIC_MSG_KEY, MSG_TTL_HOURS, TimeUnit.HOURS);
            //异步存MySQL
            saveToMysqlAsync(publicMessage);
            //广播给所有在线用户（包括自己）
            for (Map.Entry<String, Session> entry : sessionMap.entrySet()) {
                entry.getValue().getBasicRemote().sendText(json);
            }
        } catch (Exception e) {
            System.err.println("处理消息失败: " + e.getMessage());
        }
    }

    @OnClose
    public void onClose(Session session) throws IOException {
        try {
            String userId = (String) session.getUserProperties().get("userId");
            String username = (String) session.getUserProperties().get("username");
            if (userId != null) {
                sessionMap.remove(userId);  // 清理连接
                if (username != null) {
                    Map<String, Object> leaveMsg = new HashMap<>();
                    leaveMsg.put("messageType", "SYSTEM");
                    leaveMsg.put("content", username + "离开了");
                    String leaveJson = objectMapper.writeValueAsString(leaveMsg);
                    for (Map.Entry<String, Session> entry : sessionMap.entrySet()) {
                        entry.getValue().getBasicRemote().sendText(leaveJson);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("关闭连接时出错: " + e.getMessage());
        }
    }


    @OnError
    public void onError(Session session, Throwable error) {
        try {
            String userId = (String) session.getUserProperties().get("userId");
            String username = (String) session.getUserProperties().get("username");

            // 记录错误信息
            if (error instanceof java.io.EOFException) {
                // EOFException 是正常的连接关闭，不需要记录详细堆栈
                System.out.println("WebSocket连接异常关闭: " + (username != null ? username : "未知用户"));
            } else {
                System.err.println("WebSocket错误 [用户: " + (username != null ? username : "未知") + "]: " + error.getMessage());
            }

            // 清理资源
            if (userId != null) {
                sessionMap.remove(userId);
            }

            // 尝试关闭session
            if (session.isOpen()) {
                session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, "连接错误"));
            }

        } catch (Exception e) {
            System.err.println("错误处理失败: " + e.getMessage());
        }
    }
}
