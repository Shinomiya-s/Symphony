package com.qzh.symphony.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.qzh.symphony.DAO.FriendshipAi;
import com.qzh.symphony.DAO.PrivateMessage;

import java.io.IOException;
import java.util.List;

public interface PrivateChatAiService {
    void callAiAsync(String fromUserId, String toUserId, String message,
                     List<PrivateMessage> history, FriendshipAi friendshipAi);
    void detectMistakeAsync(String fromUserId, String toUserId) throws IOException;
    //Redis分页
    List<PrivateMessage> getRecentMessages(String currentUserId, String targetUserId);
    List<PrivateMessage> loadMoreMessages(String currentUserId, String targetUserId, String beforeMessageId);
    void sendMessage(PrivateMessage message);
}