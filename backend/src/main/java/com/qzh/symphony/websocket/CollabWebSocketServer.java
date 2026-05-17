package com.qzh.symphony.websocket;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.websocket.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@ServerEndpoint("/ws/collab/{roomId}")
public class CollabWebSocketServer {
    //key是roomId，value是这个房间里所有在线用户的连接
    private static ConcurrentHashMap<String, CopyOnWriteArrayList<Session>> rooms = new ConcurrentHashMap<>();
    private static StringRedisTemplate stringRedisTemplate;

    @Autowired
    public void setStringRedisTemplate(StringRedisTemplate stringRedisTemplate) {
        CollabWebSocketServer.stringRedisTemplate = stringRedisTemplate;
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("roomId") String roomId) {
        if (rooms.containsKey(roomId)) {
            //把session加入房间
            rooms.get(roomId).add(session);
        } else{
            CopyOnWriteArrayList<Session> sessions = new CopyOnWriteArrayList<>();
            sessions.add(session);
            rooms.put(roomId, sessions);
        }
        //从URL参数拿userId存入session
        String userId=session.getRequestParameterMap().get("userId").get(0);
        session.getUserProperties().put("userId", userId);
        session.getUserProperties().put("roomId", roomId);
        //发送历史文档内容
        String history = stringRedisTemplate.opsForValue().get("collab:" + roomId);
        if (history != null) {
            try {
                session.getBasicRemote().sendText(history);
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }

    //广播给房间内的另一个人（不发给自己）
    @OnMessage
    public void onMessage(String message, Session session) {
        String roomId = (String) session.getUserProperties().get("roomId");
        CopyOnWriteArrayList<Session> sessions = rooms.get(roomId);
        for (Session s : sessions) {
            if (!s.equals(session)) {
                try {
                    s.getBasicRemote().sendText(message);
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
        }
        //存入Redis，覆盖旧内容
        stringRedisTemplate.opsForValue().set("collab:" + roomId, message);
    }

    @OnClose
    public void onClose(Session session) {
        String roomId = (String) session.getUserProperties().get("roomId");
        CopyOnWriteArrayList<Session> sessions = rooms.get(roomId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                rooms.remove(roomId);       //房间空了就清掉,防止rooms里堆积空列表
            }
        }
    }
}
