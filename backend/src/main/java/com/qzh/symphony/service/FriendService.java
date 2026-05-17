package com.qzh.symphony.service;

import java.util.Map;

public interface FriendService {
    void sendRequest(String fromUserId, String toUserId);
    void handleRequest(String requestId, boolean accept);
}
