package com.qzh.symphony.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qzh.symphony.DAO.Friendship;
import com.qzh.symphony.DAO.FriendshipAi;
import com.qzh.symphony.DAO.PrivateMessage;
import com.qzh.symphony.common.Result;
import com.qzh.symphony.common.utils.ChatUtils;
import com.qzh.symphony.mapper.AccountMapper;
import com.qzh.symphony.mapper.FriendshipAiMapper;
import com.qzh.symphony.mapper.FriendshipMapper;
import com.qzh.symphony.mapper.PrivateMessageMapper;
import com.qzh.symphony.service.PrivateChatAiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/chat/private")
public class PrivateChatController {
    @Autowired
    FriendshipMapper friendshipMapper;
    @Autowired
    AccountMapper accountMapper;
    @Autowired
    PrivateMessageMapper privateMessageMapper;
    @Autowired
    FriendshipAiMapper friendshipAiMapper;
    @Autowired
    PrivateChatAiService privateChatAiService;

    //查询好友列表
    @GetMapping("/list")
    public Result friendshipList() {
        //从SecurityContext拿当前用户id
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        //查friendship表里所有包含当前用户的记录
        List<Friendship> friendships = friendshipMapper.selectList(
                new LambdaQueryWrapper<Friendship>()
                        .and(w -> w
                                .eq(Friendship::getUserId, currentUserId)
                                .or()
                                .eq(Friendship::getFriendId, currentUserId)
                        )
        );
        //把好友的用户信息返回
        List<Map<String,Object>> maps=new ArrayList<>();
        for (Friendship friendship:friendships) {
            Map<String, Object> map = new HashMap<>();
            String friendId = friendship.getUserId().equals(currentUserId)
                    ? friendship.getFriendId()
                    : friendship.getUserId();
            String friendName = accountMapper.selectById(friendId).getUsername();
            map.put("userId", friendId);
            map.put("username", friendName);
            map.put("isFriend", true);
            maps.add(map);
        }
        return Result.success(maps);
    }

    //旧接口,已弃用,查询聊天记录(三种消息)
    @GetMapping("/{targetUserId}/messages")
    public Result getMessages(@PathVariable String targetUserId) {
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
        String chatId = ChatUtils.buildFriendShipId(currentUserId, targetUserId);
        List<PrivateMessage> messages = privateMessageMapper.selectList(
                new LambdaQueryWrapper<PrivateMessage>()
                        .eq(PrivateMessage::getChatId, chatId)
                        .and(w -> w
                                .eq(PrivateMessage::getFromUserId, currentUserId)
                                .eq(PrivateMessage::getToUserId, targetUserId)
                                .or()
                                .eq(PrivateMessage::getFromUserId, targetUserId)
                                .eq(PrivateMessage::getToUserId, currentUserId)
                                .or()
                                .eq(PrivateMessage::getFromUserId, "AI")
                                .eq(PrivateMessage::getToUserId, currentUserId)
                        )
                        .orderByAsc(PrivateMessage::getCreatedAt)
        );
        return Result.success(messages);
    }

    //发送消息,数据双写,异步触发错题检测
    @PostMapping("/send")
    public Result sendMessage(@RequestBody Map<String, Object> body) throws IOException {
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
        String targetUserId = (String) body.get("targetUserId");
        String content = (String) body.get("message");
        Boolean aiEnabled = (Boolean) body.get("aiEnabled");

        // 创建消息对象
        PrivateMessage msg = new PrivateMessage();
        msg.setAiVisible(aiEnabled != null && aiEnabled ? 1 : 0);
        msg.setFromUserId(currentUserId);
        msg.setToUserId(targetUserId);
        msg.setContent(content);
        msg.setType(0);

        //双写
        privateChatAiService.sendMessage(msg);

        //AI错题检测
        privateChatAiService.detectMistakeAsync(currentUserId, targetUserId);

        return Result.success("success");
    }

    //手动触发AI总结，且只取 aiVisible=1（用户允许 AI 看到）的最近10条消息
    @PostMapping("/ai/summarize")
    public Result summarize(@RequestBody Map<String, Object> body) {
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
        String targetUserId = (String) body.get("targetUserId");
        String friendshipId = ChatUtils.buildFriendShipId(currentUserId, targetUserId);
        Friendship friendship = friendshipMapper.selectById(friendshipId);
        if (friendship == null) return Result.error("好友关系不存在");

        FriendshipAi friendshipAi = friendshipAiMapper.selectById(friendship.getId());
        if (friendshipAi == null) return Result.error("AI未初始化");

        //查最近10条记录
        List<PrivateMessage> history = privateMessageMapper.selectList(
                new LambdaQueryWrapper<PrivateMessage>()
                        .and(w -> w
                                .eq(PrivateMessage::getFromUserId, currentUserId)
                                .eq(PrivateMessage::getToUserId, targetUserId)
                                .or()
                                .eq(PrivateMessage::getFromUserId, targetUserId)
                                .eq(PrivateMessage::getToUserId, currentUserId)
                        )
                        .eq(PrivateMessage::getAiVisible, 1)
                        .orderByDesc(PrivateMessage::getCreatedAt)
                        .last("LIMIT 10")
        );

        privateChatAiService.callAiAsync(currentUserId, targetUserId, null, history, friendshipAi);
        return Result.success("success");
    }

    //查询AI开关状态
    @GetMapping("/ai-status")
    public Result getAiStatus(@RequestParam String targetUserId) {
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
        String friendshipId = ChatUtils.buildFriendShipId(currentUserId, targetUserId);
        Friendship friendship = friendshipMapper.selectById(friendshipId);
        if (friendship == null) return Result.error("好友关系不存在");

        Map<String, Object> result = new HashMap<>();
        result.put("aiEnabled", friendship.getAiEnabled() == null || friendship.getAiEnabled() == 1);
        return Result.success(result);
    }

    //切换AI开关并持久化
    @PostMapping("/ai-toggle")
    public Result toggleAi(@RequestBody Map<String, Object> body) {
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
        String targetUserId  = (String) body.get("targetUserId");
        Boolean enabled      = (Boolean) body.get("enabled");
        String friendshipId = ChatUtils.buildFriendShipId(currentUserId, targetUserId);
        Friendship friendship = friendshipMapper.selectById(friendshipId);
        if (friendship == null) return Result.error("好友关系不存在");

        friendship.setAiEnabled(enabled != null && enabled ? 1 : 0);
        friendshipMapper.updateById(friendship);
        return Result.success("success");
    }

    //Redis获取最近50条
    @GetMapping("/{targetUserId}/messages/recent")
    public Result getRecentMessages(@PathVariable String targetUserId) {
        try {
            String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
            List<PrivateMessage> messages = privateChatAiService.getRecentMessages(currentUserId, targetUserId);
            return Result.success(messages);
        } catch (Exception e) {
            return Result.error("获取消息失败: " + e.getMessage());
        }
    }

    //向上滚动加载更多,每次加载100条
    @GetMapping("/{targetUserId}/messages/more")
    public Result loadMoreMessages(
            @PathVariable String targetUserId,
            @RequestParam String beforeMessageId) {

        try {
            String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
            List<PrivateMessage> messages = privateChatAiService.loadMoreMessages(
                    currentUserId,
                    targetUserId,
                    beforeMessageId
            );
            return Result.success(messages);
        } catch (Exception e) {
            return Result.error("加载失败: " + e.getMessage());
        }
    }

}
