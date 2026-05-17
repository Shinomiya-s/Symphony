package com.qzh.symphony.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qzh.symphony.DAO.Account;
import com.qzh.symphony.common.Result;
import com.qzh.symphony.mapper.AccountMapper;
import com.qzh.symphony.service.AccountService;
import com.qzh.symphony.service.FriendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/api/user")
public class UserController {
    @Autowired
    private AccountService accountService;

    //按用户名查简介
    @GetMapping("/profile/{username}")
    public Result getUserProfile(@PathVariable String username) {
        String userId=SecurityContextHolder.getContext().getAuthentication().getName();
        return Result.success(accountService.getUserProfile(username,userId));
    }

    //按userId查账户
    @GetMapping("/id/{userId}")
    public Result getUserById(@PathVariable String userId) {
        Account account=accountService.getById(userId);
        return Result.success(account);
    }
}
