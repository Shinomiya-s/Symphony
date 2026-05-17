package com.qzh.symphony.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qzh.symphony.DAO.Account;
import com.qzh.symphony.DAO.FriendRequest;
import com.qzh.symphony.DAO.Friendship;
import com.qzh.symphony.DAO.FriendshipAi;
import com.qzh.symphony.common.utils.ChatUtils;
import com.qzh.symphony.mapper.AccountMapper;
import com.qzh.symphony.mapper.FriendRequestMapper;
import com.qzh.symphony.mapper.FriendshipAiMapper;
import com.qzh.symphony.mapper.FriendshipMapper;
import com.qzh.symphony.service.FriendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class FriendServiceImpl implements FriendService {
    @Autowired
    FriendshipMapper friendshipMapper;
    @Autowired
    FriendRequestMapper friendRequestMapper;
    @Autowired
    FriendshipAiMapper friendshipAiMapper;

    //处理好友申请
    @Override
    public void handleRequest(String requestId, boolean accept) {
        FriendRequest friendRequest=friendRequestMapper.selectById(requestId);
        if(friendRequest==null) return;
        if(accept){
            String smallId = friendRequest.getFromUserId().compareTo(friendRequest.getToUserId()) < 0
                    ? friendRequest.getFromUserId() : friendRequest.getToUserId();
            String bigId = friendRequest.getFromUserId().compareTo(friendRequest.getToUserId()) < 0
                    ? friendRequest.getToUserId() : friendRequest.getFromUserId();
            String friendshipId = ChatUtils.buildFriendShipId(
                    friendRequest.getFromUserId(), friendRequest.getToUserId());
            Friendship friendship = new Friendship();
            //建立好友关系
            friendship.setId(friendshipId);
            friendship.setUserId(smallId);
            friendship.setFriendId(bigId);
            friendship.setAiEnabled(1);
            try {
                friendshipMapper.insert(friendship);
            } catch (org.springframework.dao.DuplicateKeyException e) {
                //已经是好友了，静默处理
                friendRequest.setStatus(1);
                friendRequestMapper.updateById(friendRequest);
                return;
            }
            friendRequest.setStatus(1);
            FriendshipAi friendshipAi = new FriendshipAi();
            //初始化
            friendshipAi.setFriendshipId(friendship.getId());
            friendshipAi.setAiName("symphony");
            friendshipAi.setCreatedAt(LocalDateTime.now());
            friendshipAiMapper.insert(friendshipAi);
            //同意则修改状态为1
            friendRequest.setStatus(1);
        }else{
            //拒绝则改状态
            friendRequest.setStatus(2);
        }friendRequestMapper.updateById(friendRequest);
    }

    //发送好友申请
    @Override
    public void sendRequest(String fromUserId, String toUserId) {
        String smallId = fromUserId.compareTo(toUserId) < 0 ? fromUserId : toUserId;
        String bigId = fromUserId.compareTo(toUserId) < 0 ? toUserId : fromUserId;
        //已经是好友了
        if (friendshipMapper.selectOne(
                new LambdaQueryWrapper<Friendship>()
                        .eq(Friendship::getUserId, smallId)
                        .eq(Friendship::getFriendId, bigId)
        ) != null) return;
        //已经有待处理的申请了
        if (friendRequestMapper.selectOne(
                new LambdaQueryWrapper<FriendRequest>()
                        .eq(FriendRequest::getStatus, 0)
                        .and(w -> w
                                .eq(FriendRequest::getFromUserId, smallId).eq(FriendRequest::getToUserId, bigId)
                                .or()
                                .eq(FriendRequest::getFromUserId, bigId).eq(FriendRequest::getToUserId, smallId)
                        )
        ) != null) return;
        FriendRequest request = new FriendRequest();
        request.setFromUserId(fromUserId);
        request.setToUserId(toUserId);
        request.setStatus(0);
        friendRequestMapper.insert(request);
    }
}
