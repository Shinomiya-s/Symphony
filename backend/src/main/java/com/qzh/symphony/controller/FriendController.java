package com.qzh.symphony.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qzh.symphony.DAO.Account;
import com.qzh.symphony.DAO.FriendRequest;
import com.qzh.symphony.DAO.Friendship;
import com.qzh.symphony.common.Result;
import com.qzh.symphony.common.utils.ChatUtils;
import com.qzh.symphony.common.utils.JwtUtils;
import com.qzh.symphony.mapper.AccountMapper;
import com.qzh.symphony.mapper.FriendRequestMapper;
import com.qzh.symphony.mapper.FriendshipMapper;
import com.qzh.symphony.service.FriendService;
import com.qzh.symphony.service.impl.FriendServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/friends")
public class FriendController {
    @Autowired
    private FriendService friendService;
    @Autowired
    FriendshipMapper friendshipMapper;
    @Autowired
    private AccountMapper accountMapper;
    @Autowired
    private FriendRequestMapper friendRequestMapper;

    //发送好友申请
    @PostMapping("/request")
    public Result sendFriendRequest(@RequestBody Map<String, String> body){
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        String targetUsername = body.get("targetUsername");
        Account target = accountMapper.selectOne(
                new LambdaQueryWrapper<Account>()
                        .eq(Account::getUsername, targetUsername)
        );
        //先查用户是否存在
        if (target == null) return Result.error("用户不存在");
        if (userId.equals(target.getUserId())) {
            //防止用户自己加自己
            return Result.error("不能加自己为好友");
        }

        String friendshipId = ChatUtils.buildFriendShipId(userId, target.getUserId());
        //用friendshipId查,已经是好友就拒绝
        long count = friendshipMapper.selectCount(
                new LambdaQueryWrapper<Friendship>()
                        .eq(Friendship::getId, friendshipId)
        );
        if (count > 0) {
            return Result.error("你们已经是好友了");
        }

        friendService.sendRequest(userId, target.getUserId());
        return Result.success("success");
    }

    //处理好友申请
    @PostMapping("/request/handle")
    public Result handleFriendRequest(@RequestBody Map<String, Object> body){
        //得到request的id
        String requestId = (String) body.get("requestId");
        Boolean accept = (Boolean) body.get("accept");
        //校验这条申请确实是发给当前用户的
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
        FriendRequest req = friendRequestMapper.selectById(requestId);
        if (req == null) return Result.error("请求不存在");
        if (!req.getToUserId().equals(currentUserId)) return Result.error("无权操作");
        //再调用handleFriendRequest方法
        friendService.handleRequest(requestId, accept);
        return Result.success("success");
    }

    //查看待处理申请
    @GetMapping("/requests/pending")
    public Result pendingFriendRequest(){
        List<FriendRequest> friendships = friendRequestMapper.selectList(
                new LambdaQueryWrapper<FriendRequest>()
                        .eq(FriendRequest::getToUserId, SecurityContextHolder.getContext().getAuthentication().getName())
                        .eq(FriendRequest::getStatus,0) //0表示待处理
        );
        List<Map<String, Object>> result = friendships.stream().map(req -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", req.getId());
            map.put("fromUserId", req.getFromUserId());
            Account account = accountMapper.selectById(req.getFromUserId());
            map.put("fromUsername", account != null ? account.getUsername() : "未知用户");
            return map;
        }).collect(Collectors.toList());
        return Result.success(result);
    }
}
